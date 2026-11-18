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
		":2123":            "127.0.0.1:2123",
		"0.0.0.0:2123":     "127.0.0.1:2123",
		"127.0.0.1:2123":   "127.0.0.1:2123",
		"agent-host:2123": "agent-host:2123",
	}
	for input, want := range cases {
		if got := LoopbackAddr(input); got != want {
			t.Fatalf("loopback %q = %q, want %q", input, got, want)
		}
	}
}
