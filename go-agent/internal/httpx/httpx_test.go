/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package httpx

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/config"
)

func testApp() *app.App {
	return &app.App{
		Config: config.Default(),
		Auth:   app.Auth{Authorize: "authorized"},
	}
}

func TestAuthorizationAndEnvelope(t *testing.T) {
	r := New(testApp())
	r.Handle(http.MethodPost, "/info", func(c *Context) { c.OK(map[string]any{"ok": true}) })
	r.Public(http.MethodGet, "/{$}", func(c *Context) { _, _ = c.W.Write([]byte("index")) })

	// public route does not require the header
	rec := httptest.NewRecorder()
	r.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/", nil))
	if rec.Body.String() != "index" {
		t.Fatalf("public index = %q", rec.Body.String())
	}

	// missing authorization -> body code 900, HTTP 200
	rec = httptest.NewRecorder()
	r.ServeHTTP(rec, httptest.NewRequest(http.MethodPost, "/info", nil))
	var msg Message
	if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
		t.Fatalf("decode: %v", err)
	}
	if msg.Code != 900 || rec.Code != http.StatusOK {
		t.Fatalf("auth failure = %d %+v", rec.Code, msg)
	}

	// valid authorization -> success envelope
	rec = httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/info", nil)
	req.Header.Set("Jpom-Agent-Authorize", "authorized")
	req.Header.Set("workspaceId", "WS1")
	r.ServeHTTP(rec, req)
	if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
		t.Fatalf("decode: %v", err)
	}
	if msg.Code != 200 {
		t.Fatalf("success envelope = %+v", msg)
	}

	// unknown route -> JSON 500 body with the request path in data (Java fallback page)
	rec = httptest.NewRecorder()
	r.ServeHTTP(rec, httptest.NewRequest(http.MethodPost, "/nope", nil))
	if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
		t.Fatalf("unknown route must still be JSON: %v", err)
	}
	if msg.Code != 500 {
		t.Fatalf("unknown route code = %d", msg.Code)
	}
	if msg.Data != "/nope" {
		t.Fatalf("unknown route data = %v", msg.Data)
	}
}

func TestI18nByAcceptLanguage(t *testing.T) {
	a := testApp()
	r := New(a)
	r.Handle(http.MethodPost, "/echo", func(c *Context) { c.OKMsg(c.T("i18n.upload_success.a769"), nil) })

	cases := map[string]string{
		"en-US": "Authorization information error",
		"zh-CN": "授权信息错误",
		"zh-TW": "授權資訊錯誤",
		"zh-HK": "授權信息錯誤",
	}
	for lang, want := range cases {
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/echo", nil)
		req.Header.Set("Accept-Language", lang)
		r.ServeHTTP(rec, req)
		var msg Message
		if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
			t.Fatalf("decode %s: %v", lang, err)
		}
		if msg.Code != 900 {
			t.Fatalf("%s code = %d", lang, msg.Code)
		}
		if msg.Msg != want {
			t.Errorf("%s auth msg = %q, want %q", lang, msg.Msg, want)
		}
	}

	// 带权重的 Accept-Language 与授权通过后的业务文案。
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/echo", nil)
	req.Header.Set("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8")
	req.Header.Set("Jpom-Agent-Authorize", "authorized")
	r.ServeHTTP(rec, req)
	var msg Message
	if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
		t.Fatal(err)
	}
	if msg.Code != 200 || msg.Msg != "Upload successful" {
		t.Errorf("en message = %d %q", msg.Code, msg.Msg)
	}

	// 未带 Accept-Language 时使用配置语言（默认 zh-CN）。
	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodPost, "/echo", nil)
	req.Header.Set("Jpom-Agent-Authorize", "authorized")
	r.ServeHTTP(rec, req)
	if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
		t.Fatal(err)
	}
	if msg.Msg != "上传成功" {
		t.Errorf("default lang msg = %q", msg.Msg)
	}
}

func TestRouterMethodNotSupported(t *testing.T) {
	r := New(testApp())
	r.Handle(http.MethodPost, "/info", func(c *Context) { c.OK(nil) })

	// 路径存在但方法不支持：code=405「不被支持的请求方式」+ Spring 异常 message。
	rec := httptest.NewRecorder()
	request := httptest.NewRequest(http.MethodGet, "/info", nil)
	request.Header.Set("Jpom-Agent-Authorize", "authorized")
	r.ServeHTTP(rec, request)
	var msg Message
	if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
		t.Fatal(err)
	}
	if msg.Code != 405 || msg.Msg != "不被支持的请求方式" {
		t.Fatalf("method not supported = %+v", msg)
	}
	if detail, _ := msg.Data.(string); detail != "Request method 'GET' not supported" {
		t.Errorf("detail = %v", msg.Data)
	}

	// 未授权时仍返回 900。
	rec = httptest.NewRecorder()
	unauthorized := httptest.NewRequest(http.MethodGet, "/info", nil)
	unauthorized.Header.Set("Accept-Language", "en-US")
	r.ServeHTTP(rec, unauthorized)
	if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
		t.Fatal(err)
	}
	if msg.Code != 900 {
		t.Fatalf("unauthorized method mismatch = %+v", msg)
	}

	// 未知路径仍是 500 no_resource_found。
	rec = httptest.NewRecorder()
	r.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/nope", nil))
	if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
		t.Fatal(err)
	}
	if msg.Code != 500 {
		t.Fatalf("unknown path = %+v", msg)
	}
}
