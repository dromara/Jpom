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
	"jpom.io/go-agent/internal/i18n"
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
	app     *app.App
	mux     *http.ServeMux
	public  map[string]struct{}
	methods map[string]map[string]struct{}
	// index 是 "/"、"/index"、"/index.html" 的处理器。Java 的 @RequestMapping
	// 不限制请求方法，因此这里对任意 HTTP 方法都返回该页面。
	index Handler
}

// New creates an empty router.
func New(a *app.App) *Router {
	r := &Router{
		app:     a,
		mux:     http.NewServeMux(),
		public:  map[string]struct{}{},
		methods: map[string]map[string]struct{}{},
	}
	r.mux.HandleFunc("/", func(w http.ResponseWriter, req *http.Request) {
		c := newContext(r.app, w, req)
		path := req.URL.Path
		// index 页与 Java 一致不限请求方法（GET/POST/HEAD 等）。
		if r.index != nil && isIndexPath(path) {
			r.index(c)
			return
		}
		methods, exists := r.methods[path]
		if !exists {
			// 与 Java 2.11.13.1 实测一致：HTTP 200 + code=500，msg 为 i18n
			// no_resource_found，data 为请求路径（Java 404 error controller 语义）。
			c.JSONCode(500, c.T("i18n.no_resource_found.dc22"), path)
			return
		}
		// 方法不匹配必须在鉴权之前返回 405：Spring 在映射阶段即抛
		// HttpRequestMethodNotSupportedException，不会执行鉴权拦截器。
		if _, allowed := methods[req.Method]; !allowed {
			c.JSONCode(405, c.T("i18n.unsupported_request_method.45d7"),
				fmt.Sprintf("Request method '%s' not supported", req.Method))
			return
		}
		// 路径与方法都已注册却未命中 mux（理论上不可达），按资源不存在处理。
		c.JSONCode(500, c.T("i18n.no_resource_found.dc22"), path)
	})
	return r
}

// SetIndex registers the index page handler. The Java agent maps "/", "/index"
// and "/index.html" without a method constraint, so the router serves this
// handler for every HTTP method (POST/HEAD included).
func (r *Router) SetIndex(handler Handler) { r.index = handler }

// isIndexPath reports whether path is one of the Java agent index mappings.
func isIndexPath(path string) bool {
	return path == "/" || path == "/index" || path == "/index.html"
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
	// 记录路径 -> 方法集合（用于方法不匹配时返回 405 而不是 500）。
	path := pattern
	if path == "/{$}" {
		path = "/"
	}
	if r.methods[path] == nil {
		r.methods[path] = map[string]struct{}{}
	}
	r.methods[path][method] = struct{}{}
	r.mux.HandleFunc(key, func(w http.ResponseWriter, req *http.Request) {
		c := newContext(r.app, w, req)
		defer func() {
			if recovered := recover(); recovered != nil {
				r.app.Log().Error("handler panic", "method", req.Method, "path", req.URL.Path, "panic", recovered, "stack", string(debug.Stack()))
				// panic 是服务异常，必须返回 code=500（不得伪装成 code=200 成功）。
				c.JSONCode(500, c.T("i18n.service_exception.3821")+fmt.Sprint(recovered), nil)
			}
		}()
		r.app.Log().Debug("request", "method", req.Method, "path", req.URL.Path, "remote", req.RemoteAddr, "workspace", req.Header.Get("workspaceId"))
		if !public {
			authorize := req.Header.Get("Jpom-Agent-Authorize")
			if !r.app.CheckAuthorize(authorize) {
				r.app.Log().Warn("agent authorization failed", "path", req.URL.Path, "remote", req.RemoteAddr)
				c.JSONCode(900, c.T("i18n.auth_info_error.c184"), nil)
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
	// 语言优先级与 Java I18nMessageUtil 一致：Accept-Language → jpom.system.lang
	// → JPOM_LANG → zh-CN。
	lang := i18n.Resolve(r.Header.Get("Accept-Language"), a.Config.Jpom.System.Lang)
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

// T 返回当前请求语言下的文案（Java 端 i18n key + {} 占位符）。
// 面向客户端的 msg/提示都应通过它输出。
func (c *Context) T(key string, args ...any) string { return i18n.T(c.Lang, key, args...) }

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

// Require reads a form/query parameter and enforces the Java @ValidatorItem
// semantics: missing or blank -> HTTP 200 + body code=400 with the given i18n
// key. The boolean result reports whether the handler may continue.
//
// 用法（N1 契约）：value, ok := c.Require("type", "i18n.type_error.395f")；
// 业务 Assert 仍用 c.FailCode(405, ...)，不要混用。
func (c *Context) Require(name, key string) (string, bool) {
	value := strings.TrimSpace(c.Form(name))
	if value == "" {
		c.FailCode(400, c.T(key))
		return "", false
	}
	return value, true
}

// RequireRaw 与 Require 相同，但返回未 trim 的原始值（内容/路径/变量值等
// 需要保留原始空白的参数使用）。
func (c *Context) RequireRaw(name, key string) (string, bool) {
	value := c.Form(name)
	if strings.TrimSpace(value) == "" {
		c.FailCode(400, c.T(key))
		return "", false
	}
	return value, true
}

// WriteFileHeaders writes common download headers.
func (c *Context) WriteFileHeaders(filename, contentType string) {
	if contentType == "" {
		contentType = "application/octet-stream"
	}
	c.W.Header().Set("Content-Type", contentType)
	c.W.Header().Set("Content-Disposition", ContentDisposition(filename))
}

// ContentDisposition renders the download header exactly like Hutool 5.8.34
// (ServletUtil#write(HttpServletResponse, File)):
//
//	attachment;filename="<percent-encoded>";filename*=UTF-8''<percent-encoded>
//
// Both filename parameters carry the RFC3986 percent-encoded UTF-8 name
// (Hutool URLUtil.encodeAll), so non-ASCII names decode correctly in browsers
// while ASCII names stay readable.
func ContentDisposition(filename string) string {
	encoded := EncodeRFC3986(filename)
	return "attachment;filename=\"" + encoded + "\";filename*=UTF-8''" + encoded
}

// EncodeRFC3986 percent-encodes every UTF-8 byte except the RFC3986 unreserved
// characters (A-Z a-z 0-9 - . _ ~), matching Hutool URLUtil.encodeAll.
func EncodeRFC3986(value string) string {
	var builder strings.Builder
	builder.Grow(len(value))
	for index := 0; index < len(value); index++ {
		char := value[index]
		switch {
		case char >= 'A' && char <= 'Z':
		case char >= 'a' && char <= 'z':
		case char >= '0' && char <= '9':
		case char == '-' || char == '.' || char == '_' || char == '~':
		default:
			builder.WriteByte('%')
			builder.WriteString(fmt.Sprintf("%02X", char))
			continue
		}
		builder.WriteByte(char)
	}
	return builder.String()
}

// multipartFormMemory caps how much of a multipart form is kept in memory; the
// remainder spills to temporary files (same idea as Spring's multipart resolver).
const multipartFormMemory = 32 << 20

// MaxUploadFileSize returns spring.servlet.multipart.max-file-size.
func (c *Context) MaxUploadFileSize() int64 { return c.App.Config.MaxFileSize() }

// MaxUploadRequestSize returns spring.servlet.multipart.max-request-size.
func (c *Context) MaxUploadRequestSize() int64 { return c.App.Config.MaxRequestSize() }

// ParseMultipartForm parses a multipart/form-data request while enforcing the
// Spring multipart limits (spring.servlet.multipart.max-file-size /
// max-request-size). Non-multipart requests are treated as "no file": their
// form is parsed and the handlers answer 没有上传文件 like the Java agent.
//
// When a limit is exceeded it writes the Java BaseExceptionHandler response
// (code 406, i18n.file_too_large.9994) and returns false; the caller must
// return immediately.
func (c *Context) ParseMultipartForm() bool {
	contentType := strings.ToLower(c.R.Header.Get("Content-Type"))
	if !strings.HasPrefix(contentType, "multipart/") {
		_ = c.R.ParseForm()
		return true
	}
	requestLimit := c.MaxUploadRequestSize()
	if requestLimit <= 0 {
		requestLimit = 20 << 20
	}
	if c.R.ContentLength > requestLimit {
		c.FailCode(http.StatusNotAcceptable, c.T("i18n.file_too_large.9994"))
		return false
	}
	c.R.Body = http.MaxBytesReader(c.W, c.R.Body, requestLimit)
	if err := c.R.ParseMultipartForm(multipartFormMemory); err != nil {
		var maxBytesError *http.MaxBytesError
		if errors.As(err, &maxBytesError) || strings.Contains(err.Error(), "request body too large") {
			c.FailCode(http.StatusNotAcceptable, c.T("i18n.file_too_large.9994"))
			return false
		}
		// 其它解析失败保持 Java 行为：交给后续「没有上传文件」判断。
		return true
	}
	fileLimit := c.MaxUploadFileSize()
	if form := c.R.MultipartForm; form != nil && fileLimit > 0 {
		for _, headers := range form.File {
			for _, header := range headers {
				if header.Size > fileLimit {
					c.FailCode(http.StatusNotAcceptable, c.T("i18n.file_too_large.9994"))
					return false
				}
			}
		}
	}
	return true
}

// ErrNotImplemented is returned by stub handlers until the owning module lands.
var ErrNotImplemented = errors.New("not implemented")
