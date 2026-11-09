/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// Package ws implements the five agent WebSocket endpoints exposed to the Jpom
// server: /console, /script_run, /free-script-run, /system_log and /node_update.
//
// The server is the websocket *client* and proxies browser frames verbatim, so
// every frame written here must stay byte/format compatible with the Java agent
// (see go-agent/DESIGN.md).
package ws

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/websocket"

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/httpx"
	"jpom.io/go-agent/internal/i18n"
	"jpom.io/go-agent/internal/project"
	"jpom.io/go-agent/internal/script"
)

const (
	// authorizeParam is the handshake query parameter carrying the agent
	// authorization digest (same value as the HTTP header).
	authorizeParam = "Jpom-Agent-Authorize"
	// socketMsgTag marks control frames consumed by the browser
	// (Const.SOCKET_MSG_TAG).
	socketMsgTag = "JPOM_MSG"
	// closeCannotAccept is the close code used when the authorization fails
	// (CloseCodes.CANNOT_ACCEPT).
	closeCannotAccept = 1003
	// closeNormal is a normal closure.
	closeNormal = 1000
	// defaultTextLimit mirrors the 5MB @OnMessage limits.
	defaultTextLimit = 5 * 1024 * 1024
	// consoleIdleTimeout mirrors session.setMaxIdleTimeout(180_000).
	consoleIdleTimeout = 180 * time.Second
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  8192,
	WriteBufferSize: 8192,
	// The server connects directly; the handshake authorization is the query
	// parameter checked in authorized().
	CheckOrigin: func(_ *http.Request) bool { return true },
}

// Session is one websocket connection with a write lock (the Java agent
// serializes every send on the session object; gorilla/websocket requires the
// same).
type Session struct {
	ID        string
	Lang      string
	OptUser   string
	Workspace string

	conn *websocket.Conn

	mu     sync.Mutex
	closed bool
}

func newSession(conn *websocket.Conn) *Session {
	return &Session{ID: randomID(), conn: conn}
}

func randomID() string {
	buffer := make([]byte, 16)
	if _, err := rand.Read(buffer); err != nil {
		return time.Now().Format("150405.000000000")
	}
	return hex.EncodeToString(buffer)
}

// Send writes one text frame. Empty messages are skipped (SocketSessionUtil).
func (s *Session) Send(message string) error {
	if message == "" {
		return nil
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.closed {
		return errSessionClosed
	}
	return s.conn.WriteMessage(websocket.TextMessage, []byte(message))
}

// SendJSON writes the JSON encoding of value.
func (s *Session) SendJSON(value any) error {
	encoded, err := json.Marshal(value)
	if err != nil {
		return err
	}
	return s.Send(string(encoded))
}

// Read reads the next frame.
func (s *Session) Read() (int, []byte, error) {
	return s.conn.ReadMessage()
}

// SetReadLimit sets the maximum frame size.
func (s *Session) SetReadLimit(limit int64) { s.conn.SetReadLimit(limit) }

// SetReadDeadline sets the read deadline (console idle timeout).
func (s *Session) SetReadDeadline(deadline time.Time) error {
	return s.conn.SetReadDeadline(deadline)
}

// CloseWithReason sends a close frame with code/reason and closes the socket.
func (s *Session) CloseWithReason(code int, reason string) {
	s.mu.Lock()
	if s.closed {
		s.mu.Unlock()
		return
	}
	s.closed = true
	_ = s.conn.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(code, reason))
	s.mu.Unlock()
	_ = s.conn.Close()
}

// Close closes the socket normally.
func (s *Session) Close() { s.CloseWithReason(closeNormal, "") }

var errSessionClosed = websocket.ErrCloseSent

// Manager holds the websocket endpoint dependencies.
type Manager struct {
	App      *app.App
	Scripts  *script.Manager
	Executor *script.Executor
	Tail     *TailManager
	// Projects resolves project data/log paths for the console.
	Projects *project.Manager
	// ProjectRuntime runs the project lifecycle commands for the console.
	ProjectRuntime *project.Runtime
	// Console provides the project console operations (start/stop/status...).
	Console ConsoleBackend
	// Upgrade installs the uploaded agent package and restarts the process.
	Upgrade UpgradeHandler
}

// NewManager creates the websocket manager.
func NewManager(instance *app.App, scripts *script.Manager, executor *script.Executor) *Manager {
	initReadLine := instance.Config.Jpom.InitReadLine
	projectRuntime := project.NewRuntime(instance.BaseDir, instance.Config, instance.Log())
	return &Manager{
		App:            instance,
		Scripts:        scripts,
		Executor:       executor,
		Tail:           NewTailManager(initReadLine),
		Projects:       projectRuntime.Manager,
		ProjectRuntime: projectRuntime,
		Console:        NewDefaultConsoleBackend(projectRuntime),
	}
}

// authorized checks the handshake query authorization and fills the session
// metadata (optUser / lang / workspaceId); it closes the session with the Java
// close code when the authorization fails.
func (m *Manager) authorized(session *Session, request *http.Request) bool {
	session.OptUser = query(request, "optUser")
	session.Lang = query(request, "lang")
	if session.Lang == "" && m.App.Config != nil {
		session.Lang = m.App.Config.Jpom.System.Lang
	}
	session.Workspace = query(request, "workspaceId")
	if m.App.CheckAuthorize(query(request, authorizeParam)) {
		return true
	}
	m.App.Log().Warn("websocket authorization failed", "path", request.URL.Path, "remote", request.RemoteAddr)
	session.CloseWithReason(closeCannotAccept, m.translate(session, "i18n.auth_info_error.c184"))
	return false
}

// sessionLang resolves the language of one websocket session: the handshake
// lang parameter, then jpom.system.lang, then the agent default.
func (m *Manager) sessionLang(session *Session) string {
	if session != nil && session.Lang != "" {
		return session.Lang
	}
	if m.App.Config != nil && m.App.Config.Jpom.System.Lang != "" {
		return m.App.Config.Jpom.System.Lang
	}
	return i18n.DefaultLang()
}

// translate renders a client facing websocket message for the session language.
func (m *Manager) translate(session *Session, key string, args ...any) string {
	return i18n.T(m.sessionLang(session), key, args...)
}

// translateDefault renders a message using the configured agent language
// (used outside a session context).
func (m *Manager) translateDefault(key string, args ...any) string {
	lang := ""
	if m.App.Config != nil {
		lang = m.App.Config.Jpom.System.Lang
	}
	return i18n.T(lang, key, args...)
}

// query returns the first query value.
func query(request *http.Request, name string) string {
	values := request.URL.Query()[name]
	if len(values) == 0 {
		return ""
	}
	return strings.TrimSpace(values[0])
}

// upgrade upgrades the HTTP request to a websocket session.
func upgrade(c *httpx.Context) (*Session, error) {
	conn, err := upgrader.Upgrade(c.W, c.R, nil)
	if err != nil {
		return nil, err
	}
	return newSession(conn), nil
}

// controlFrame builds the console/script control frame: the request object with
// code/msg/data/JPOM_MSG merged in (Java: reqJson.putAll(resultData.toJson()) +
// JPOM_MSG). Only non-nil values are added, mirroring fastjson2 (null omitted).
func controlFrame(request map[string]any, code int, message string, data any) map[string]any {
	frame := make(map[string]any, len(request)+4)
	for key, value := range request {
		frame[key] = value
	}
	frame["code"] = code
	if message != "" {
		frame["msg"] = message
	}
	if data != nil {
		frame["data"] = data
	}
	frame[socketMsgTag] = socketMsgTag
	return frame
}

// jsonMessage is the {"code":n,"msg":"..."} frame used by the Java
// JsonMessage.getString helper (null fields omitted).
func jsonMessage(code int, message string) map[string]any {
	return map[string]any{"code": code, "msg": message}
}

// closeFrame sends a {"code":500,"msg":"..."} frame and closes the session.
func closeWithMessage(session *Session, message string) {
	_ = session.SendJSON(jsonMessage(500, message))
	session.Close()
}

// decodeLine decodes raw log bytes with the configured charset
// (Java AgentFileTailWatcher charset / console charset).
func decodeLine(data []byte, charset string) string { return script.DecodeText(data, charset) }

// bodyString reads a string field from a decoded generic JSON object.
func bodyString(value map[string]any, key string) string {
	if value == nil {
		return ""
	}
	if item, ok := value[key].(string); ok {
		return item
	}
	return ""
}

// requestString reads a JSON value the way fastjson2 JSONObject#getString does:
// booleans/numbers become their string form, missing/null becomes "".
func requestString(value map[string]any, key string) string {
	if value == nil {
		return ""
	}
	item, ok := value[key]
	if !ok || item == nil {
		return ""
	}
	switch typed := item.(type) {
	case string:
		return typed
	case bool:
		if typed {
			return "true"
		}
		return "false"
	case json.Number:
		return typed.String()
	case float64:
		return strconv.FormatFloat(typed, 'f', -1, 64)
	default:
		return ""
	}
}

// bodyBool reads a boolean field from a decoded generic JSON object.
func bodyBool(value map[string]any, key string) bool {
	if value == nil {
		return false
	}
	switch item := value[key].(type) {
	case bool:
		return item
	case string:
		return item == "true" || item == "1"
	case float64:
		return item != 0
	case json.Number:
		return item.String() != "0" && item.String() != ""
	}
	return false
}

// bodyInt reads an integer field from a decoded generic JSON object.
func bodyInt(value map[string]any, key string) int {
	if value == nil {
		return 0
	}
	switch item := value[key].(type) {
	case float64:
		return int(item)
	case json.Number:
		if parsed, err := item.Int64(); err == nil {
			return int(parsed)
		}
	case int:
		return item
	case int64:
		return int(item)
	case string:
		parsed := json.Number(item)
		if number, err := parsed.Int64(); err == nil {
			return int(number)
		}
	}
	return 0
}

// decodeObject decodes a JSON text frame keeping numbers as json.Number.
func decodeObject(data []byte) (map[string]any, error) {
	value := map[string]any{}
	decoder := json.NewDecoder(strings.NewReader(string(data)))
	decoder.UseNumber()
	if err := decoder.Decode(&value); err != nil {
		return nil, err
	}
	return value, nil
}
