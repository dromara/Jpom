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
	"encoding/json"
	"testing"
)

// TestSignMatchesJavaVector 用同一份密钥/机器标识/时间戳/随机数锁定与 Java 端一致的 HMAC 结果，
// 避免两端各改一处后握手签名对不上。
func TestSignMatchesJavaVector(t *testing.T) {
	const expected = "3b0aec6b355848101988e9933cad81359446f849cff0dc5d672b0a8d181d9a87"
	got := Sign("tunnel-secret", "machine-1", 1800000000, "nonce")
	if got != expected {
		t.Fatalf("sign = %s, want %s", got, expected)
	}
}

// TestSplitStreamNeverSetsFin 锁定双向流协议：流上的每条消息都不能带 FIN，
// 否则对端（Go 插件端 onBinary / 服务端 ServerStream）会把“消息结束”当成“方向结束”并关闭本机连接，
// 2026-11-20 /node_update 的 getVersion 就是这样一发出就把流关掉的。
func TestSplitStreamNeverSetsFin(t *testing.T) {
	payload := bytes.Repeat([]byte("x"), MaxDataFrameSize+16)
	frames, err := SplitStream("sid-stream", payload)
	if err != nil {
		t.Fatalf("SplitStream error: %v", err)
	}
	if len(frames) < 2 {
		t.Fatalf("frames = %d, want at least 2", len(frames))
	}
	total := 0
	for i, raw := range frames {
		sid, chunk, fin, err := DecodeData(raw)
		if err != nil {
			t.Fatalf("decode frame %d: %v", i, err)
		}
		if sid != "sid-stream" {
			t.Fatalf("frame %d sid = %s", i, sid)
		}
		if fin {
			t.Fatalf("frame %d of a stream message must not carry FIN", i)
		}
		total += len(chunk)
	}
	if total != len(payload) {
		t.Fatalf("payload reassembled = %d, want %d", total, len(payload))
	}
	// HTTP 请求体仍按旧语义在最后一帧带 FIN
	bodyFrames, err := Split("sid-req", []byte("body"))
	if err != nil {
		t.Fatalf("Split error: %v", err)
	}
	_, _, fin, err := DecodeData(bodyFrames[len(bodyFrames)-1])
	if err != nil || !fin {
		t.Fatalf("request body last frame must carry FIN, err=%v fin=%v", err, fin)
	}
}

func TestDataFrameRoundTrip(t *testing.T) {
	payload := []byte("hello 插件端")
	raw, err := EncodeData("s-42", payload, true)
	if err != nil {
		t.Fatalf("encode: %v", err)
	}
	sid, decoded, fin, err := DecodeData(raw)
	if err != nil {
		t.Fatalf("decode: %v", err)
	}
	if sid != "s-42" || !fin || !bytes.Equal(payload, decoded) {
		t.Fatalf("round trip mismatch: sid=%s fin=%v payload=%q", sid, fin, decoded)
	}
}

func TestDecodeDataRejectsBrokenFrames(t *testing.T) {
	if _, _, _, err := DecodeData([]byte{0x02, 1, 'a', 0}); err == nil {
		t.Fatal("wrong magic must fail")
	}
	if _, _, _, err := DecodeData([]byte{0x01}); err == nil {
		t.Fatal("short frame must fail")
	}
	if _, err := EncodeData("", nil, true); err == nil {
		t.Fatal("empty sid must fail")
	}
	longSid := "0123456789012345678901234"
	if len(longSid) <= MaxSidLength {
		t.Fatalf("test sid must exceed limit: %d", len(longSid))
	}
	if _, err := EncodeData(longSid, nil, true); err == nil {
		t.Fatal("long sid must fail")
	}
	tooBig := make([]byte, MaxDataFrameSize+1)
	if _, err := EncodeData("s-1", tooBig, true); err == nil {
		t.Fatal("oversized payload must fail")
	}
}

func TestSplitAndReassemble(t *testing.T) {
	total := MaxDataFrameSize*2 + 1024
	body := make([]byte, total)
	for i := range body {
		body[i] = byte(i % 251)
	}
	frames, err := Split("s-9", body)
	if err != nil {
		t.Fatalf("split: %v", err)
	}
	if len(frames) != 3 {
		t.Fatalf("frames = %d, want 3", len(frames))
	}
	rebuilt := make([]byte, 0, total)
	for index, frame := range frames {
		sid, payload, fin, err := DecodeData(frame)
		if err != nil {
			t.Fatalf("decode frame %d: %v", index, err)
		}
		if sid != "s-9" {
			t.Fatalf("sid = %s", sid)
		}
		wantFin := index == len(frames)-1
		if fin != wantFin {
			t.Fatalf("frame %d fin = %v, want %v", index, fin, wantFin)
		}
		rebuilt = append(rebuilt, payload...)
	}
	if !bytes.Equal(body, rebuilt) {
		t.Fatal("reassembled body mismatch")
	}
}

// TestPushFrameRoundTrip 锁定 PUSH 帧字段名与 Java 端一致（topic/seq/body），
// 否则 Go 插件端推送会被 Java 服务端丢弃或解析为空。
func TestPushFrameRoundTrip(t *testing.T) {
	frame := NewPush(TopicNodeStat, 7, `{"simpleStatus":{"cpu":1.5}}`)
	raw, err := EncodeControl(frame)
	if err != nil {
		t.Fatalf("encode push: %v", err)
	}
	var fields map[string]any
	if err := json.Unmarshal(raw, &fields); err != nil {
		t.Fatalf("decode json: %v", err)
	}
	if fields["type"] != TypePush {
		t.Fatalf("type = %v, want %s", fields["type"], TypePush)
	}
	if fields["topic"] != TopicNodeStat {
		t.Fatalf("topic = %v, want %s", fields["topic"], TopicNodeStat)
	}
	if fields["seq"] != float64(7) {
		t.Fatalf("seq = %v, want 7", fields["seq"])
	}
	if fields["body"] != `{"simpleStatus":{"cpu":1.5}}` {
		t.Fatalf("body = %v", fields["body"])
	}
	if _, ok := fields["sid"]; ok {
		t.Fatal("push frame must not carry sid")
	}

	decoded, err := DecodeControl(raw)
	if err != nil {
		t.Fatalf("decode control: %v", err)
	}
	if decoded.Type != TypePush || decoded.Topic != TopicNodeStat || decoded.Seq != 7 {
		t.Fatalf("decoded push mismatch: %+v", decoded)
	}
}

// TestHelloAckPushCapability 锁定能力位解析：老服务端（不声明 push）必须解析成 nil，
// 插件端据此不发送 PUSH 帧，否则老服务端会把未知帧当非法帧断开连接。
func TestHelloAckPushCapability(t *testing.T) {
	withCapability, err := DecodeControl([]byte(`{"type":"HELLO_ACK","sessionId":"s-1","push":true}`))
	if err != nil {
		t.Fatalf("decode ack: %v", err)
	}
	if withCapability.Push == nil || !*withCapability.Push {
		t.Fatalf("push capability = %v, want true", withCapability.Push)
	}

	legacy, err := DecodeControl([]byte(`{"type":"HELLO_ACK","sessionId":"s-1"}`))
	if err != nil {
		t.Fatalf("decode legacy ack: %v", err)
	}
	if legacy.Push != nil {
		t.Fatalf("legacy ack must not advertise push, got %v", *legacy.Push)
	}
}

func TestNormalizeURL(t *testing.T) {
	cases := map[string]string{
		"ws://127.0.0.1:2122":            "ws://127.0.0.1:2122/agent/tunnel",
		"wss://jpom.example.com":         "wss://jpom.example.com/agent/tunnel",
		"http://127.0.0.1:2122":          "ws://127.0.0.1:2122/agent/tunnel",
		"https://jpom.example.com":       "wss://jpom.example.com/agent/tunnel",
		"127.0.0.1:2122":                 "ws://127.0.0.1:2122/agent/tunnel",
		"ws://127.0.0.1:2122/img/tunnel": "ws://127.0.0.1:2122/img/tunnel",
	}
	for input, want := range cases {
		got, err := NormalizeURL(input)
		if err != nil {
			t.Fatalf("normalize %q: %v", input, err)
		}
		if got != want {
			t.Fatalf("normalize %q = %q, want %q", input, got, want)
		}
	}
	if _, err := NormalizeURL(""); err == nil {
		t.Fatal("empty url must fail")
	}
}

func TestLoopbackAddr(t *testing.T) {
	cases := map[string]string{
		":2123":           "127.0.0.1:2123",
		"0.0.0.0:2123":    "127.0.0.1:2123",
		"127.0.0.1:2123":  "127.0.0.1:2123",
		"agent-host:2123": "agent-host:2123",
	}
	for input, want := range cases {
		if got := LoopbackAddr(input); got != want {
			t.Fatalf("loopback %q = %q, want %q", input, got, want)
		}
	}
}
