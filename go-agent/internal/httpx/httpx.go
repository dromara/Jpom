/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// Package httpx contains the HTTP plumbing shared by all agent endpoints:
// routing, the JSON response envelope and the authorization middleware.
package httpx

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"mime"
	"net/http"
	"os"
	"path/filepath"
	"runtime/debug"
	"strconv"
	"strings"

	"jpom.io/go-agent/internal/app"
)

// Message is the response envelope understood by the Jpom server
// (cn.keepbx.jpom.model.JsonMessage).
type Message struct {
	Code int    `json:"code"`
	Msg  string `json:"msg"`
	Data any    `json:"data,omitempty"`
}

// Handler is an agent HTTP handler.
type Handler func(*Context)

// Router registers endpoints and applies authorization.
type Router struct {
	app    *app.App
	mux    *http.ServeMux
	public map[string]struct{}
}

// New creates an empty router.
func New(a *app.App) *Router {
	r := &Router{app: a, mux: http.NewServeMux(), public: map[string]struct{}{}}
	r.mux.HandleFunc("/", func(w http.ResponseWriter, req *http.Request) {
		c := newContext(r.app, w, req)
		// 与 Java 兜底页面一致：code=500，data 为请求路径，文案按语言。
		msg := "No corresponding resource found."
		if strings.HasPrefix(strings.ToLower(c.Lang), "zh") {
			msg = "没有找到对应的资源"
		}
		c.JSONCode(500, msg, req.URL.Path)
	})
	return r
}

// Handle registers an endpoint that requires the Jpom-Agent-Authorize header.
func (r *Router) Handle(method, pattern string, handler Handler) {
	r.register(method, pattern, handler, false)
}

// Public registers an endpoint that does not require the authorization header.
func (r *Router) Public(method, pattern string, handler Handler) {
	r.register(method, pattern, handler, true)
}

func (r *Router) register(method, pattern string, handler Handler, public bool) {
	key := method + " " + pattern
	if public {
		r.public[key] = struct{}{}
	}
	r.mux.HandleFunc(key, func(w http.ResponseWriter, req *http.Request) {
		c := newContext(r.app, w, req)
		defer func() {
			if recovered := recover(); recovered != nil {
				r.app.Log().Error("handler panic", "method", req.Method, "path", req.URL.Path, "panic", recovered, "stack", string(debug.Stack()))
				c.JSONCode(http.StatusInternalServerError, "程序异常: "+fmt.Sprint(recovered), nil)
			}
		}()
		r.app.Log().Debug("request", "method", req.Method, "path", req.URL.Path, "remote", req.RemoteAddr, "workspace", req.Header.Get("workspaceId"))
		if !public {
			authorize := req.Header.Get("Jpom-Agent-Authorize")
			if !r.app.CheckAuthorize(authorize) {
				r.app.Log().Warn("agent authorization failed", "path", req.URL.Path, "remote", req.RemoteAddr)
				c.JSONCode(900, "授权信息错误", nil)
				return
			}
		}
		handler(c)
	})
}

// ServeHTTP implements http.Handler.
func (r *Router) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	// 先按 transport-encryption 头解密参数（Java 端由 DecryptionFilter 完成）。
	decryptRequest(req)
	// 服务端拼接的 agent URL 带结尾斜杠（如 /info/）。Spring MVC 同时匹配
	// /info 与 /info/，net/http 不会，这里统一归一化。
	if len(req.URL.Path) > 1 && strings.HasSuffix(req.URL.Path, "/") {
		trimmed := strings.TrimRight(req.URL.Path, "/")
		if trimmed == "" {
			trimmed = "/"
		}
		req.URL.Path = trimmed
	}
	r.mux.ServeHTTP(w, req)
}

// Context wraps the request/response pair with Jpom helpers.
type Context struct {
	W          http.ResponseWriter
	R          *http.Request
	App        *app.App
	Workspace  string
	Lang       string
	ServerUser string
}

func newContext(a *app.App, w http.ResponseWriter, r *http.Request) *Context {
	workspace := strings.TrimSpace(r.Header.Get("workspaceId"))
	if workspace == "" {
		workspace = "DEFAULT"
	}
	lang := strings.TrimSpace(r.Header.Get("Accept-Language"))
	if lang == "" {
		lang = a.Config.Jpom.System.Lang
	}
	return &Context{
		W:          w,
		R:          r,
		App:        a,
		Workspace:  workspace,
		Lang:       lang,
		ServerUser: r.Header.Get("Jpom-Server-UserName"),
	}
}

// Method returns the HTTP method.
func (c *Context) Method() string { return c.R.Method }

// Form returns a query/form parameter.
func (c *Context) Form(key string) string { return c.R.FormValue(key) }

// HasForm reports whether the parameter is present.
func (c *Context) HasForm(key string) bool {
	_, ok := c.R.Form[key]
	return ok
}

// FormDefault returns the parameter or fallback.
func (c *Context) FormDefault(key, fallback string) string {
	if value := strings.TrimSpace(c.Form(key)); value != "" {
		return value
	}
	return fallback
}

// BindJSON decodes a JSON request body into dst.
func (c *Context) BindJSON(dst any) error {
	defer func() { _, _ = io.Copy(io.Discard, c.R.Body) }()
	decoder := json.NewDecoder(c.R.Body)
	decoder.UseNumber()
	return decoder.Decode(dst)
}

// OK writes a success envelope.
func (c *Context) OK(data any) { c.JSONCode(200, "", data) }

// OKMsg writes a success envelope with a message.
func (c *Context) OKMsg(msg string, data any) { c.JSONCode(200, msg, data) }

// Fail writes an error envelope with the default error code used by the Java agent.
func (c *Context) Fail(msg string) { c.JSONCode(500, msg, nil) }

// FailCode writes an error envelope with an explicit code. The Java agent uses
// 405 for parameter/state violations and 500 for runtime errors.
func (c *Context) FailCode(code int, msg string) { c.JSONCode(code, msg, nil) }

// FormInt reads an integer parameter, falling back to def.
func (c *Context) FormInt(key string, def int) int {
	value := strings.TrimSpace(c.Form(key))
	if value == "" {
		return def
	}
	parsed, err := strconv.Atoi(value)
	if err != nil {
		return def
	}
	return parsed
}

// ServeFile streams a local file with a download disposition.
func (c *Context) ServeFile(path, filename string) error {
	file, err := os.Open(path)
	if err != nil {
		return err
	}
	defer func() { _ = file.Close() }()
	if filename == "" {
		filename = filepath.Base(path)
	}
	c.WriteFileHeaders(filename, mimeTypeByName(filename))
	_, err = io.Copy(c.W, file)
	return err
}

func mimeTypeByName(name string) string {
	if guessed := mime.TypeByExtension(filepath.Ext(name)); guessed != "" {
		return guessed
	}
	return "application/octet-stream"
}

// Failf writes a formatted error envelope.
func (c *Context) Failf(format string, args ...any) { c.Fail(fmt.Sprintf(format, args...)) }

// Error logs err and writes its message as error envelope.
func (c *Context) Error(err error) {
	if err == nil {
		return
	}
	c.App.Log().Error("handler error", "method", c.R.Method, "path", c.R.URL.Path, "error", err)
	c.Fail(err.Error())
}

// JSONCode writes an arbitrary envelope and always returns HTTP 200, matching
// the Java agent where errors are encoded in the body code.
//
// The body is encoded into memory first so Content-Length is set explicitly;
// the Spring/Tomcat agent always sends a known length and some Jpom client code
// paths are happier without chunked responses for large payloads (/info).
func (c *Context) JSONCode(code int, msg string, data any) {
	var buffer bytes.Buffer
	encoder := json.NewEncoder(&buffer)
	encoder.SetEscapeHTML(false)
	if err := encoder.Encode(Message{Code: code, Msg: msg, Data: data}); err != nil {
		c.App.Log().Error("encode response failed", "error", err)
		http.Error(c.W, "encode response failed", http.StatusInternalServerError)
		return
	}
	c.W.Header().Set("Content-Type", "application/json;charset=UTF-8")
	c.W.Header().Set("Content-Length", strconv.Itoa(buffer.Len()))
	c.W.WriteHeader(http.StatusOK)
	if _, err := c.W.Write(buffer.Bytes()); err != nil {
		c.App.Log().Debug("write response failed", "error", err)
	}
}

// WriteFileHeaders writes common download headers.
func (c *Context) WriteFileHeaders(filename, contentType string) {
	if contentType == "" {
		contentType = "application/octet-stream"
	}
	c.W.Header().Set("Content-Type", contentType)
	c.W.Header().Set("Content-Disposition", fmt.Sprintf("attachment; filename=%q", filename))
}

// ErrNotImplemented is returned by stub handlers until the owning module lands.
var ErrNotImplemented = errors.New("接口尚未实现")
