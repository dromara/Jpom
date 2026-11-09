/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package api

import (
	"net/http"

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/httpx"
)

// registerWS owns the websocket endpoints: /console, /script_run,
// /free-script-run, /system_log and /node_update. They are registered as public
// routes because the handshake authorization travels in the query string
// (Jpom-Agent-Authorize), not in a header.
func registerWS(r *httpx.Router, a *app.App) {
	manager := runtimeFor(a).ws
	r.Public(http.MethodGet, "/console", manager.HandleConsole)
	r.Public(http.MethodGet, "/script_run", manager.HandleScriptRun)
	r.Public(http.MethodGet, "/free-script-run", manager.HandleFreeScriptRun)
	r.Public(http.MethodGet, "/system_log", manager.HandleSystemLog)
	r.Public(http.MethodGet, "/node_update", manager.HandleNodeUpdate)
}
