/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// Package api wires every agent endpoint. Each functional area lives in its own
// file (system.go, project.go, projectfile.go, script.go, ws.go) and owns its
// routes, so parallel development does not conflict.
package api

import (
	"net/http"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/httpx"
)

// RegisterAll builds the HTTP handler tree.
func RegisterAll(a *app.App) http.Handler {
	r := httpx.New(a)
	// The Java agent exposes an unauthenticated index page (and its root path)
	// through a method-less @RequestMapping: GET/POST/HEAD 等常见方法都必须可用。
	r.SetIndex(index)

	registerSystem(r, a)
	registerProject(r, a)
	registerProjectFile(r, a)
	registerScript(r, a)
	registerWS(r, a)
	return r
}

func index(c *httpx.Context) {
	c.W.Header().Set("Content-Type", "text/plain;charset=UTF-8")
	_, _ = c.W.Write([]byte("Jpom-Agent,Can't access directly,Please configure it to JPOM server"))
}

func notImplemented(c *httpx.Context) {
	c.Fail(httpx.ErrNotImplemented.Error())
}
