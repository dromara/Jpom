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
