//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

// Package tunnel implements the agent initiated reverse tunnel to the Jpom
// server. The frame layout and control frame names are byte compatible with the
// Java implementation (modules/agent-transport/agent-transport-tunnel), so the
// same server can serve Java and Go agents.
package tunnel

import (
	"encoding/json"
	"errors"
	"fmt"
)

const (
	// EndpointPath is the server side WebSocket endpoint.
	EndpointPath = "/agent/tunnel"
	// DataFrameMagic is the first byte of every binary data frame.
	DataFrameMagic = 0x01
	// MaxDataFrameSize is the payload limit of a single data frame (256 KiB).
	MaxDataFrameSize = 256 * 1024
	// MaxSidLength is the maximum sid length in bytes.
	MaxSidLength = 24
	// DefaultHeartbeatSeconds mirrors TunnelConst.DEFAULT_HEARTBEAT_SECONDS.
	DefaultHeartbeatSeconds = 5
	// MaxMissedPong mirrors TunnelConst.DEFAULT_MAX_MISSED_PONG.
	MaxMissedPong = 3
	// FlagFin marks the end of one direction of a sid.
	FlagFin = 0x01
)

// Frame type names, identical to the Java TunnelFrameType enum.
const (
	TypeHello      = "HELLO"
	TypeHelloAck   = "HELLO_ACK"
	TypeReject     = "REJECT"
	TypePing       = "PING"
	TypePong       = "PONG"
	TypeReq        = "REQ"
	TypeReqBodyEnd = "REQ_BODY_END"
	TypeRes        = "RES"
	TypeEnd        = "END"
	TypeAbort      = "ABORT"
	TypeOpen       = "OPEN"
	TypeOpenAck    = "OPEN_ACK"
	TypeClose      = "CLOSE"
	TypeAck        = "ACK"
	// TypePush is an agent -> server one way push (node status, metrics).
	TypePush = "PUSH"
)

// TopicNodeStat mirrors TunnelConst.TOPIC_NODE_STAT: the pushed body is the
// same JSON shape as the /get-stat-info data field.
const TopicNodeStat = "node-stat"

// Frame is one control frame (JSON text frame on the tunnel).
type Frame struct {
	Type string `json:"type"`
	Sid  string `json:"sid,omitempty"`

	// HELLO
	MachineID    string `json:"machineId,omitempty"`
	Key          string `json:"key,omitempty"`
	AgentVersion string `json:"agentVersion,omitempty"`
	OS           string `json:"os,omitempty"`
	Arch         string `json:"arch,omitempty"`
	// AuthUser/AuthToken 为 HELLO 可选字段：服务端自动注册机器资产时补齐通讯账号与授权摘要。
	// 旧服务端会忽略未知字段、旧插件端不会发送，双向兼容。
	AuthUser  string `json:"authUser,omitempty"`
	AuthToken string `json:"authToken,omitempty"`
	Ts        int64  `json:"ts,omitempty"`
	Nonce     string `json:"nonce,omitempty"`
	Sign      string `json:"sign,omitempty"`

	// HELLO_ACK
	SessionID      string `json:"sessionId,omitempty"`
	Heartbeat      int    `json:"heartbeat,omitempty"`
	MaxConcurrency int    `json:"maxConcurrency,omitempty"`
	ServerVersion  string `json:"serverVersion,omitempty"`

	// REJECT / ABORT / CLOSE
	Code int    `json:"code,omitempty"`
	Msg  string `json:"msg,omitempty"`

	// REQ
	Method      string            `json:"method,omitempty"`
	Path        string            `json:"path,omitempty"`
	Headers     map[string]string `json:"headers,omitempty"`
	ContentType string            `json:"contentType,omitempty"`
	Len         int64             `json:"len,omitempty"`
	Timeout     int               `json:"timeout,omitempty"`

	// OPEN
	Query map[string]string `json:"query,omitempty"`

	// OPEN_ACK
	Ok *bool `json:"ok,omitempty"`

	// RES
	Status int `json:"status,omitempty"`

	// ACK
	N int64 `json:"n,omitempty"`

	// PUSH
	Topic string `json:"topic,omitempty"`
	Seq   int64  `json:"seq,omitempty"`
	Body  string `json:"body,omitempty"`

	// HELLO_ACK capability: server supports agent -> server PUSH frames.
	// An older server does not know PUSH and closes the connection on an
	// unknown control frame, so the agent only pushes when this is true.
	Push *bool `json:"push,omitempty"`
}

// NewPush builds an agent -> server push frame.
func NewPush(topic string, seq int64, body string) *Frame {
	return &Frame{Type: TypePush, Topic: topic, Seq: seq, Body: body}
}

// EncodeControl marshals a control frame.
func EncodeControl(frame *Frame) ([]byte, error) {
	if frame == nil || frame.Type == "" {
		return nil, errors.New("control frame type must not be empty")
	}
	return json.Marshal(frame)
}

// DecodeControl parses a control frame.
func DecodeControl(text []byte) (*Frame, error) {
	if len(text) == 0 {
		return nil, errors.New("empty control frame")
	}
	var frame Frame
	if err := json.Unmarshal(text, &frame); err != nil {
		return nil, fmt.Errorf("decode control frame: %w", err)
	}
	if frame.Type == "" {
		return nil, errors.New("control frame without type")
	}
	return &frame, nil
}

// EncodeData builds a binary data frame:
//
//	[magic][sidLen][sid][flags][payload]
func EncodeData(sid string, payload []byte, fin bool) ([]byte, error) {
	if sid == "" {
		return nil, errors.New("data frame sid must not be empty")
	}
	if len(sid) > MaxSidLength {
		return nil, fmt.Errorf("data frame sid too long: %d", len(sid))
	}
	if len(payload) > MaxDataFrameSize {
		return nil, fmt.Errorf("data frame payload too large: %d", len(payload))
	}
	frame := make([]byte, 3+len(sid)+len(payload))
	frame[0] = DataFrameMagic
	frame[1] = byte(len(sid))
	copy(frame[2:], sid)
	if fin {
		frame[2+len(sid)] = FlagFin
	}
	copy(frame[3+len(sid):], payload)
	return frame, nil
}

// DecodeData parses a binary data frame.
func DecodeData(raw []byte) (sid string, payload []byte, fin bool, err error) {
	if len(raw) < 3 {
		return "", nil, false, fmt.Errorf("data frame too short: %d", len(raw))
	}
	if raw[0] != DataFrameMagic {
		return "", nil, false, fmt.Errorf("unexpected data frame magic: %d", raw[0])
	}
	sidLen := int(raw[1])
	if sidLen <= 0 || sidLen > MaxSidLength {
		return "", nil, false, fmt.Errorf("invalid data frame sid length: %d", sidLen)
	}
	if len(raw) < 3+sidLen {
		return "", nil, false, fmt.Errorf("data frame too short: %d", len(raw))
	}
	sid = string(raw[2 : 2+sidLen])
	flags := raw[2+sidLen]
	payload = raw[3+sidLen:]
	if len(payload) > MaxDataFrameSize {
		return "", nil, false, fmt.Errorf("data frame payload too large: %d", len(payload))
	}
	return sid, payload, flags&FlagFin != 0, nil
}

// SplitStream cuts one duplex-stream message into data frames that never carry
// FIN. FIN means "this direction is finished": the peer closes its local
// websocket as soon as it sees FIN (see onBinary), so per-message writes on a
// stream must not set it — otherwise the very first getVersion message would
// tear the /node_update stream down before the agent can answer.
func SplitStream(sid string, payload []byte) ([][]byte, error) {
	if len(payload) == 0 {
		return nil, nil
	}
	var frames [][]byte
	for offset := 0; offset < len(payload); {
		end := offset + MaxDataFrameSize
		if end > len(payload) {
			end = len(payload)
		}
		frame, err := EncodeData(sid, payload[offset:end], false)
		if err != nil {
			return nil, err
		}
		frames = append(frames, frame)
		offset = end
	}
	return frames, nil
}

// Split cuts body into data frames, the last one carries FIN.
func Split(sid string, body []byte) ([][]byte, error) {
	if len(body) == 0 {
		frame, err := EncodeData(sid, nil, true)
		if err != nil {
			return nil, err
		}
		return [][]byte{frame}, nil
	}
	var frames [][]byte
	for offset := 0; offset < len(body); {
		end := offset + MaxDataFrameSize
		if end > len(body) {
			end = len(body)
		}
		frame, err := EncodeData(sid, body[offset:end], end >= len(body))
		if err != nil {
			return nil, err
		}
		frames = append(frames, frame)
		offset = end
	}
	return frames, nil
}
