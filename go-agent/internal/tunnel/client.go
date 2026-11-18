//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package tunnel

import (
	"bytes"
	"context"
	"encoding/base64"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"github.com/gorilla/websocket"
)

const (
	authorizeHeader = "Jpom-Agent-Authorize"
	workspaceHeader = "workspaceId"
	encryptHeader   = "transport-encryption"

	initialRetry = 3 * time.Second
	maxRetry     = 60 * time.Second
	writeTimeout = 30 * time.Second
	chunkSize    = 32 * 1024
)

// fatal reject codes: configuration problem, retrying is pointless.
var fatalCodes = map[int]bool{4001: true, 4002: true, 4003: true}

// Options configures the agent side tunnel client.
type Options struct {
	ServerURL      string
	MachineID      string
	Key            string
	AgentVersion   string
	OS             string
	Arch           string
	LocalHTTPBase  string
	LocalWSBase    string
	Authorize      string
	MaxConcurrency int
	Timeout        time.Duration
	Logger         *slog.Logger
}

func (o Options) validate() error {
	if strings.TrimSpace(o.ServerURL) == "" {
		return fmt.Errorf("tunnel server url is empty")
	}
	if strings.TrimSpace(o.MachineID) == "" {
		return fmt.Errorf("tunnel machine id is empty")
	}
	if strings.TrimSpace(o.Key) == "" {
		return fmt.Errorf("tunnel key is empty")
	}
	return nil
}

type requestState struct {
	frame *Frame
	body  bytes.Buffer
}

type streamState struct {
	sid     string
	conn    *websocket.Conn
	writeMu sync.Mutex
	closed  atomic.Bool
}

// Client keeps an outbound WebSocket connection to the Jpom server and forwards
// server requests to the local agent HTTP/WS endpoints.
type Client struct {
	opts     Options
	log      *slog.Logger
	dialer   *websocket.Dialer
	http     *http.Client
	connMu   sync.RWMutex
	conn     *websocket.Conn
	writeMu  sync.Mutex
	requests sync.Map // sid -> *requestState
	streams  sync.Map // sid -> *streamState
	sem      chan struct{}

	heartbeat int32
	connected atomic.Bool
	// pushSupported mirrors HELLO_ACK.push: never push to a server that did not
	// advertise PUSH support (an older server closes the tunnel on unknown frames).
	pushSupported atomic.Bool
	stopped       atomic.Bool
	stop          chan struct{}
	wg            sync.WaitGroup
}

// New creates a tunnel client.
func New(opts Options) (*Client, error) {
	if err := opts.validate(); err != nil {
		return nil, err
	}
	if opts.MaxConcurrency <= 0 {
		opts.MaxConcurrency = 16
	}
	if opts.Timeout <= 0 {
		opts.Timeout = 30 * time.Second
	}
	logger := opts.Logger
	if logger == nil {
		logger = slog.Default()
	}
	return &Client{
		opts:   opts,
		log:    logger,
		dialer: websocket.DefaultDialer,
		http:   &http.Client{Timeout: opts.Timeout},
		sem:    make(chan struct{}, opts.MaxConcurrency),
		stop:   make(chan struct{}),
	}, nil
}

// Start begins the connect/reconnect loop in background.
func (c *Client) Start() {
	c.wg.Add(1)
	go func() {
		defer c.wg.Done()
		backoff := initialRetry
		for !c.stopped.Load() {
			started := time.Now()
			err := c.serve()
			if c.stopped.Load() {
				return
			}
			if err != nil {
				c.log.Warn("tunnel disconnected", "error", err)
			}
			if time.Since(started) > 30*time.Second {
				backoff = initialRetry
			}
			c.log.Info("tunnel reconnect scheduled", "after", backoff.String())
			select {
			case <-c.stop:
				return
			case <-time.After(backoff):
			}
			if backoff < maxRetry {
				backoff *= 2
				if backoff > maxRetry {
					backoff = maxRetry
				}
			}
		}
	}()
}

// Stop closes the connection and waits for the background loop.
func (c *Client) Stop() {
	if c.stopped.Swap(true) {
		return
	}
	close(c.stop)
	c.closeConn(websocket.CloseNormalClosure, "client stopped")
	c.wg.Wait()
}

// Connected reports whether the handshake completed and the socket is alive.
func (c *Client) Connected() bool {
	return c.connected.Load()
}

func (c *Client) serve() error {
	target, err := NormalizeURL(c.opts.ServerURL)
	if err != nil {
		return err
	}
	dialer := *c.dialer
	dialer.HandshakeTimeout = c.opts.Timeout
	conn, resp, err := dialer.Dial(target, nil)
	if err != nil {
		if resp != nil {
			return fmt.Errorf("dial tunnel %s: %w (http %d)", target, err, resp.StatusCode)
		}
		return fmt.Errorf("dial tunnel %s: %w", target, err)
	}
	defer func() {
		_ = conn.Close()
	}()

	c.connMu.Lock()
	c.conn = conn
	c.connMu.Unlock()
	// 每条新连接都要重新等 HELLO_ACK 声明能力位，不能沿用上一条连接的结果
	c.pushSupported.Store(false)

	if err := c.handshake(conn); err != nil {
		return err
	}
	c.connected.Store(true)
	c.log.Info("tunnel connected", "server", target, "machine", c.opts.MachineID)
	defer func() {
		c.connected.Store(false)
		c.failAllPending()
	}()

	return c.readLoop(conn)
}

func (c *Client) handshake(conn *websocket.Conn) error {
	ts := time.Now().Unix()
	nonce := Nonce()
	hello := &Frame{
		Type:         TypeHello,
		MachineID:    c.opts.MachineID,
		Key:          c.opts.Key,
		AgentVersion: c.opts.AgentVersion,
		OS:           c.opts.OS,
		Arch:         c.opts.Arch,
		Ts:           ts,
		Nonce:        nonce,
		Sign:         Sign(c.opts.Key, c.opts.MachineID, ts, nonce),
	}
	payload, err := EncodeControl(hello)
	if err != nil {
		return err
	}
	if err := c.writeText(conn, payload); err != nil {
		return err
	}
	_ = conn.SetReadDeadline(time.Now().Add(c.opts.Timeout))
	for {
		_, data, err := conn.ReadMessage()
		if err != nil {
			return fmt.Errorf("wait hello ack: %w", err)
		}
		frame, err := DecodeControl(data)
		if err != nil {
			return err
		}
		switch frame.Type {
		case TypeHelloAck:
			_ = conn.SetReadDeadline(time.Time{})
			c.pushSupported.Store(frame.Push != nil && *frame.Push)
			if frame.Heartbeat > 0 {
				atomic.StoreInt32(&c.heartbeat, int32(frame.Heartbeat))
			}
			return nil
		case TypeReject:
			c.connected.Store(false)
			msg := frame.Msg
			if msg == "" {
				msg = "handshake rejected"
			}
			if fatalCodes[frame.Code] {
				c.stopped.Store(true)
			}
			return fmt.Errorf("handshake rejected: %s", msg)
		case TypePing:
			_ = c.writeControl(conn, &Frame{Type: TypePong})
		}
	}
}

func (c *Client) readLoop(conn *websocket.Conn) error {
	timeout := time.Duration(c.currentHeartbeat()) * time.Second * (MaxMissedPong + 1)
	for {
		if c.stopped.Load() {
			return nil
		}
		_ = conn.SetReadDeadline(time.Now().Add(timeout))
		messageType, data, err := conn.ReadMessage()
		if err != nil {
			return err
		}
		switch messageType {
		case websocket.TextMessage:
			c.onText(conn, data)
		case websocket.BinaryMessage:
			c.onBinary(data)
		}
	}
}

func (c *Client) currentHeartbeat() int {
	value := atomic.LoadInt32(&c.heartbeat)
	if value <= 0 {
		return DefaultHeartbeatSeconds
	}
	return int(value)
}

func (c *Client) onText(conn *websocket.Conn, data []byte) {
	frame, err := DecodeControl(data)
	if err != nil {
		c.log.Warn("invalid control frame", "error", err, "raw", string(data))
		return
	}
	switch frame.Type {
	case TypePing:
		_ = c.writeControl(conn, &Frame{Type: TypePong})
	case TypePong:
	case TypeReq:
		state := &requestState{frame: frame}
		c.requests.Store(frame.Sid, state)
	case TypeReqBodyEnd:
		c.dispatchRequest(frame.Sid)
	case TypeAbort:
		if _, ok := c.requests.LoadAndDelete(frame.Sid); ok {
			c.log.Debug("request aborted by server", "sid", frame.Sid, "code", frame.Code)
		}
		if value, ok := c.streams.LoadAndDelete(frame.Sid); ok {
			value.(*streamState).closeLocal(frame.Code)
		}
	case TypeOpen:
		c.handleOpen(frame)
	case TypeClose, TypeEnd:
		if value, ok := c.streams.LoadAndDelete(frame.Sid); ok {
			value.(*streamState).closeLocal(frame.Code)
		}
	}
}

func (c *Client) onBinary(data []byte) {
	sid, payload, fin, err := DecodeData(data)
	if err != nil {
		c.log.Warn("invalid data frame", "error", err)
		return
	}
	if value, ok := c.streams.Load(sid); ok {
		stream := value.(*streamState)
		if len(payload) > 0 {
			stream.writeMu.Lock()
			_ = stream.conn.WriteMessage(websocket.TextMessage, payload)
			stream.writeMu.Unlock()
		}
		if fin {
			_ = stream.conn.Close()
		}
		return
	}
	value, ok := c.requests.Load(sid)
	if !ok {
		return
	}
	state := value.(*requestState)
	if len(payload) > 0 {
		state.body.Write(payload)
	}
	if fin {
		c.dispatchRequest(sid)
	}
}

func (c *Client) dispatchRequest(sid string) {
	value, ok := c.requests.LoadAndDelete(sid)
	if !ok {
		return
	}
	state := value.(*requestState)
	body := state.body.Bytes()
	c.wg.Add(1)
	go func() {
		defer c.wg.Done()
		c.sem <- struct{}{}
		defer func() { <-c.sem }()
		c.executeRequest(state.frame, body)
	}()
}

func (c *Client) executeRequest(frame *Frame, body []byte) {
	fail := func(code int, msg string) {
		_ = c.writeControl(c.conn, &Frame{Type: TypeAbort, Sid: frame.Sid, Code: code, Msg: msg})
	}
	target := strings.TrimSuffix(c.opts.LocalHTTPBase, "/") + frame.Path
	if !strings.HasPrefix(frame.Path, "/") {
		target = strings.TrimSuffix(c.opts.LocalHTTPBase, "/") + "/" + frame.Path
	}
	method := frame.Method
	if method == "" {
		method = http.MethodPost
	}
	ctx, cancel := context.WithTimeout(context.Background(), c.opts.Timeout)
	defer cancel()
	var reader io.Reader
	if len(body) > 0 && method != http.MethodGet {
		reader = bytes.NewReader(body)
	}
	request, err := http.NewRequestWithContext(ctx, method, target, reader)
	if err != nil {
		fail(400, err.Error())
		return
	}
	for name, value := range frame.Headers {
		switch strings.ToLower(name) {
		case "host", "connection", "content-length", "transfer-encoding", "accept-encoding", "keep-alive", "upgrade":
			continue
		}
		request.Header.Set(name, value)
	}
	if frame.ContentType != "" {
		request.Header.Set("Content-Type", frame.ContentType)
	}
	if c.opts.Authorize != "" {
		request.Header.Set(authorizeHeader, c.opts.Authorize)
	}
	response, err := c.http.Do(request)
	if err != nil {
		fail(502, err.Error())
		return
	}
	defer func() {
		_ = response.Body.Close()
	}()
	headers := map[string]string{}
	for name, values := range response.Header {
		switch strings.ToLower(name) {
		case "content-length", "transfer-encoding", "connection", "keep-alive", "server", "date":
			continue
		}
		if len(values) > 0 {
			headers[name] = values[0]
		}
	}
	if err := c.writeControl(c.conn, &Frame{Type: TypeRes, Sid: frame.Sid, Status: response.StatusCode, Headers: headers}); err != nil {
		return
	}
	buffer := make([]byte, chunkSize)
	for {
		read, readErr := response.Body.Read(buffer)
		if read > 0 {
			chunk := make([]byte, read)
			copy(chunk, buffer[:read])
			if err := c.writeBody(frame.Sid, chunk); err != nil {
				return
			}
		}
		if readErr == io.EOF {
			break
		}
		if readErr != nil {
			fail(502, readErr.Error())
			return
		}
	}
	_ = c.writeBodyFrame(frame.Sid, nil, true)
	_ = c.writeControl(c.conn, &Frame{Type: TypeEnd, Sid: frame.Sid})
}

func (c *Client) handleOpen(frame *Frame) {
	query := url.Values{}
	for key, value := range frame.Query {
		query.Set(key, value)
	}
	if c.opts.Authorize != "" {
		query.Set(authorizeHeader, c.opts.Authorize)
	}
	target := strings.TrimSuffix(c.opts.LocalWSBase, "/") + frame.Path
	if len(query) > 0 {
		target += "?" + query.Encode()
	}
	conn, resp, err := websocket.DefaultDialer.Dial(target, nil)
	if err != nil {
		status := 0
		if resp != nil {
			status = resp.StatusCode
		}
		ok := false
		_ = c.writeControl(c.conn, &Frame{Type: TypeOpenAck, Sid: frame.Sid, Ok: &ok, Code: status, Msg: err.Error()})
		return
	}
	stream := &streamState{sid: frame.Sid, conn: conn}
	c.streams.Store(frame.Sid, stream)
	ok := true
	if err := c.writeControl(c.conn, &Frame{Type: TypeOpenAck, Sid: frame.Sid, Ok: &ok}); err != nil {
		_ = conn.Close()
		c.streams.Delete(frame.Sid)
		return
	}
	c.wg.Add(1)
	go func() {
		defer c.wg.Done()
		for {
			messageType, data, err := conn.ReadMessage()
			if err != nil {
				if !stream.closed.Load() {
					_ = c.writeControl(c.conn, &Frame{Type: TypeClose, Sid: frame.Sid, Code: websocket.CloseNormalClosure})
				}
				c.streams.Delete(frame.Sid)
				return
			}
			if messageType != websocket.TextMessage && messageType != websocket.BinaryMessage {
				continue
			}
			_ = c.writeBody(frame.Sid, data)
		}
	}()
}

func (s *streamState) closeLocal(code int) {
	if s.closed.Swap(true) {
		return
	}
	if code == 0 {
		code = websocket.CloseNormalClosure
	}
	_ = s.conn.WriteControl(websocket.CloseMessage, websocket.FormatCloseMessage(code, ""), time.Now().Add(writeTimeout))
	_ = s.conn.Close()
}

func (c *Client) failAllPending() {
	c.requests.Range(func(key, value any) bool {
		c.requests.Delete(key)
		return true
	})
	c.streams.Range(func(key, value any) bool {
		value.(*streamState).closeLocal(websocket.CloseAbnormalClosure)
		c.streams.Delete(key)
		return true
	})
}

func (c *Client) closeConn(code int, reason string) {
	c.connMu.RLock()
	conn := c.conn
	c.connMu.RUnlock()
	if conn == nil {
		return
	}
	_ = conn.WriteControl(websocket.CloseMessage, websocket.FormatCloseMessage(code, reason), time.Now().Add(writeTimeout))
	_ = conn.Close()
}

// PushSupported reports whether the server advertised PUSH support in HELLO_ACK.
func (c *Client) PushSupported() bool {
	return c.pushSupported.Load()
}

// SendPush pushes one one-way message to the server. It returns an error when
// the tunnel is offline; callers retry on the next tick instead of buffering.
func (c *Client) SendPush(topic string, seq int64, body []byte) error {
	if !c.PushSupported() {
		return fmt.Errorf("server does not support push")
	}
	if !c.Connected() {
		return fmt.Errorf("tunnel not connected")
	}
	c.connMu.RLock()
	conn := c.conn
	c.connMu.RUnlock()
	if conn == nil {
		return fmt.Errorf("tunnel not connected")
	}
	return c.writeControl(conn, NewPush(topic, seq, string(body)))
}

func (c *Client) writeControl(conn *websocket.Conn, frame *Frame) error {
	if conn == nil {
		return fmt.Errorf("tunnel not connected")
	}
	payload, err := EncodeControl(frame)
	if err != nil {
		return err
	}
	return c.writeText(conn, payload)
}

func (c *Client) writeText(conn *websocket.Conn, payload []byte) error {
	c.writeMu.Lock()
	defer c.writeMu.Unlock()
	_ = conn.SetWriteDeadline(time.Now().Add(writeTimeout))
	return conn.WriteMessage(websocket.TextMessage, payload)
}

func (c *Client) writeBody(sid string, payload []byte) error {
	frames, err := Split(sid, payload)
	if err != nil {
		return err
	}
	for _, frame := range frames {
		if err := c.writeBodyFrameBytes(frame); err != nil {
			return err
		}
	}
	return nil
}

func (c *Client) writeBodyFrame(sid string, payload []byte, fin bool) error {
	frame, err := EncodeData(sid, payload, fin)
	if err != nil {
		return err
	}
	return c.writeBodyFrameBytes(frame)
}

func (c *Client) writeBodyFrameBytes(frame []byte) error {
	c.connMu.RLock()
	conn := c.conn
	c.connMu.RUnlock()
	if conn == nil {
		return fmt.Errorf("tunnel not connected")
	}
	c.writeMu.Lock()
	defer c.writeMu.Unlock()
	_ = conn.SetWriteDeadline(time.Now().Add(writeTimeout))
	return conn.WriteMessage(websocket.BinaryMessage, frame)
}

// NormalizeURL converts http(s)/bare hosts into a ws(s) tunnel URL, appending
// the endpoint path when the URL carries none.
func NormalizeURL(raw string) (string, error) {
	value := strings.TrimSpace(raw)
	if value == "" {
		return "", fmt.Errorf("tunnel server url is empty")
	}
	switch {
	case strings.HasPrefix(value, "http://"):
		value = "ws://" + strings.TrimPrefix(value, "http://")
	case strings.HasPrefix(value, "https://"):
		value = "wss://" + strings.TrimPrefix(value, "https://")
	case !strings.HasPrefix(value, "ws://") && !strings.HasPrefix(value, "wss://"):
		value = "ws://" + value
	}
	parsed, err := url.Parse(value)
	if err != nil {
		return "", fmt.Errorf("invalid tunnel server url %q: %w", raw, err)
	}
	if parsed.Host == "" {
		return "", fmt.Errorf("invalid tunnel server url %q: missing host", raw)
	}
	if parsed.Path == "" || parsed.Path == "/" {
		parsed.Path = EndpointPath
	}
	return parsed.String(), nil
}

// LoopbackAddr rewrites a listen address such as ":2123" or "0.0.0.0:2123"
// into an address that can be dialed from the same machine.
func LoopbackAddr(listen string) string {
	value := strings.TrimSpace(listen)
	if value == "" {
		return "127.0.0.1:2123"
	}
	index := strings.LastIndex(value, ":")
	if index < 0 {
		return "127.0.0.1:" + value
	}
	host := value[:index]
	port := value[index+1:]
	if host == "" || host == "0.0.0.0" || host == "::" || host == "[::]" {
		host = "127.0.0.1"
	}
	return host + ":" + port
}

// BasicAuthHeader is kept for compatibility with older tooling that passed
// credentials as a query parameter; the tunnel itself uses HELLO signing.
func BasicAuthHeader(name, pwd string) string {
	return base64.StdEncoding.EncodeToString([]byte(name + ":" + pwd))
}
