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
	"bytes"
	"encoding/json"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
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

	// 方法不匹配必须在鉴权之前返回 405（未授权也不应返回 900）。
	rec = httptest.NewRecorder()
	unauthorized := httptest.NewRequest(http.MethodGet, "/info", nil)
	unauthorized.Header.Set("Accept-Language", "en-US")
	r.ServeHTTP(rec, unauthorized)
	if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
		t.Fatal(err)
	}
	if msg.Code != 405 {
		t.Fatalf("method mismatch must be 405 before authorization = %+v", msg)
	}
	if detail, _ := msg.Data.(string); detail != "Request method 'GET' not supported" {
		t.Errorf("unauthorized detail = %v", msg.Data)
	}

	// 未知路径仍是 500 no_resource_found（Java 404 error controller 语义）。
	rec = httptest.NewRecorder()
	r.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/nope", nil))
	if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
		t.Fatal(err)
	}
	if msg.Code != 500 {
		t.Fatalf("unknown path = %+v", msg)
	}
}

// TestPanicHandlerReturns500 校验 handler panic 被 recover 后返回 code=500
// 「服务异常」，而不是伪装成 code=200 成功（契约 §5.1）。
func TestPanicHandlerReturns500(t *testing.T) {
	r := New(testApp())
	r.Handle(http.MethodPost, "/panic", func(c *Context) { panic("boom") })
	rec := httptest.NewRecorder()
	request := httptest.NewRequest(http.MethodPost, "/panic", nil)
	request.Header.Set("Jpom-Agent-Authorize", "authorized")
	r.ServeHTTP(rec, request)
	var msg Message
	if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
		t.Fatalf("decode: %v body=%s", err, rec.Body.String())
	}
	if msg.Code != 500 {
		t.Fatalf("panic must answer code=500, got %+v", msg)
	}
	if !strings.Contains(msg.Msg, "服务异常") {
		t.Errorf("panic msg = %q", msg.Msg)
	}
}

// TestIndexServedForAnyMethod 校验 index 页（/、/index、/index.html）与 Java 一致
// 不限请求方法（GET/POST/HEAD 等），且无需鉴权。
func TestIndexServedForAnyMethod(t *testing.T) {
	r := New(testApp())
	r.SetIndex(func(c *Context) {
		c.W.Header().Set("Content-Type", "text/plain;charset=UTF-8")
		_, _ = c.W.Write([]byte("index-page"))
	})
	for _, path := range []string{"/", "/index", "/index.html"} {
		for _, method := range []string{http.MethodGet, http.MethodPost, http.MethodHead, http.MethodPut} {
			rec := httptest.NewRecorder()
			r.ServeHTTP(rec, httptest.NewRequest(method, path, nil))
			if rec.Body.String() != "index-page" {
				t.Errorf("%s %s: body = %q", method, path, rec.Body.String())
			}
		}
	}
}

// TestContentDispositionHeader 校验下载头与 Hutool 一致（ASCII 与中文）。
func TestContentDispositionHeader(t *testing.T) {
	cases := map[string]string{
		"report.txt": `attachment;filename="report.txt";filename*=UTF-8''report.txt`,
		"中文报告.txt":   `attachment;filename="%E4%B8%AD%E6%96%87%E6%8A%A5%E5%91%8A.txt";filename*=UTF-8''%E4%B8%AD%E6%96%87%E6%8A%A5%E5%91%8A.txt`,
		"a b+c.txt":  `attachment;filename="a%20b%2Bc.txt";filename*=UTF-8''a%20b%2Bc.txt`,
	}
	for name, want := range cases {
		if got := ContentDisposition(name); got != want {
			t.Errorf("ContentDisposition(%q) = %q, want %q", name, got, want)
		}
	}

	// WriteFileHeaders 必须统一走同一 helper。
	c := &Context{W: httptest.NewRecorder(), App: testApp()}
	c.WriteFileHeaders("中文.txt", "text/plain")
	if got := c.W.Header().Get("Content-Disposition"); got != ContentDisposition("中文.txt") {
		t.Errorf("WriteFileHeaders disposition = %q", got)
	}
	if got := c.W.Header().Get("Content-Type"); got != "text/plain" {
		t.Errorf("content type = %q", got)
	}
}

// TestServeFileChineseName 校验文件流式下载时中文名可被浏览器解析。
func TestServeFileChineseName(t *testing.T) {
	dir := t.TempDir()
	file := filepath.Join(dir, "中文报告.txt")
	if err := os.WriteFile(file, []byte("hello"), 0o644); err != nil {
		t.Fatal(err)
	}
	rec := httptest.NewRecorder()
	c := &Context{W: rec, R: httptest.NewRequest(http.MethodGet, "/download", nil), App: testApp()}
	if err := c.ServeFile(file, ""); err != nil {
		t.Fatalf("ServeFile: %v", err)
	}
	if got := rec.Header().Get("Content-Disposition"); got != ContentDisposition("中文报告.txt") {
		t.Errorf("ServeFile disposition = %q", got)
	}
	if rec.Body.String() != "hello" {
		t.Errorf("ServeFile body = %q", rec.Body.String())
	}
}

func buildMultipartBody(t *testing.T, fields map[string]string, fileName string, content []byte) (string, *bytes.Buffer) {
	t.Helper()
	var buffer bytes.Buffer
	writer := multipart.NewWriter(&buffer)
	for key, value := range fields {
		if err := writer.WriteField(key, value); err != nil {
			t.Fatal(err)
		}
	}
	if fileName != "" {
		part, err := writer.CreateFormFile("file", fileName)
		if err != nil {
			t.Fatal(err)
		}
		if _, err := part.Write(content); err != nil {
			t.Fatal(err)
		}
	}
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}
	return writer.FormDataContentType(), &buffer
}

// TestMultipartLimits 校验 spring.servlet.multipart.max-file-size /
// max-request-size 生效且超限返回 code=406（i18n.file_too_large.9994）。
func TestMultipartLimits(t *testing.T) {
	a := testApp()
	a.Config.Spring.Servlet.Multipart.MaxFileSize = "10B"
	a.Config.Spring.Servlet.Multipart.MaxRequestSize = "1KB"
	r := New(a)
	r.Handle(http.MethodPost, "/upload", func(c *Context) {
		if !c.ParseMultipartForm() {
			return
		}
		c.OK(map[string]any{"ok": true})
	})
	do := func(contentType string, body *bytes.Buffer) Message {
		request := httptest.NewRequest(http.MethodPost, "/upload", body)
		request.Header.Set("Content-Type", contentType)
		request.Header.Set("Jpom-Agent-Authorize", "authorized")
		rec := httptest.NewRecorder()
		r.ServeHTTP(rec, request)
		var msg Message
		if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
			t.Fatalf("decode: %v body=%s", err, rec.Body.String())
		}
		return msg
	}

	// 限制内：正常成功。
	contentType, body := buildMultipartBody(t, map[string]string{"sliceId": "s1"}, "chunk.bin", []byte("12345"))
	if msg := do(contentType, body); msg.Code != 200 {
		t.Fatalf("within limits = %+v", msg)
	}
	// 单文件超过 max-file-size。
	contentType, body = buildMultipartBody(t, nil, "chunk.bin", []byte("12345678901234567890"))
	if msg := do(contentType, body); msg.Code != 406 || !strings.Contains(msg.Msg, "上传文件太大") {
		t.Fatalf("file too large = %+v", msg)
	}
	// 整个请求超过 max-request-size。
	contentType, body = buildMultipartBody(t, map[string]string{"content": strings.Repeat("x", 2048)}, "", nil)
	if msg := do(contentType, body); msg.Code != 406 || !strings.Contains(msg.Msg, "上传文件太大") {
		t.Fatalf("request too large = %+v", msg)
	}
	// 非 multipart 请求不触发大小限制。
	request := httptest.NewRequest(http.MethodPost, "/upload", strings.NewReader("a=1"))
	request.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	request.Header.Set("Jpom-Agent-Authorize", "authorized")
	rec := httptest.NewRecorder()
	r.ServeHTTP(rec, request)
	var msg Message
	if err := json.Unmarshal(rec.Body.Bytes(), &msg); err != nil {
		t.Fatal(err)
	}
	if msg.Code != 200 {
		t.Fatalf("non multipart = %+v", msg)
	}
}
