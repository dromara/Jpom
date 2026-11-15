/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package projectfile

// Hooks are the integration points owned by the project module (project-ops).
//
// The Java file controllers stop/restart the project before/after replacing
// files, check whether a running project occupies a file and fire the project
// webhooks after every change. Those capabilities live in the project/proc
// domain, so they are injected here once that package is available. A nil hook
// is a no-op: the file operation still succeeds, only the project side effect is
// skipped (and logged).
type ProjectHooks struct {
	// StopProject stops a running project (closeFirst=true on upload).
	StopProject func(info map[string]any) error
	// RestartProject restarts the project after an upload. It returns whether
	// the restart succeeded and the payload of the Java CommandOpResult.
	RestartProject func(info map[string]any) (success bool, msg string, data any)
	// IsProjectRunning reports whether the project process is running.
	IsProjectRunning func(info map[string]any) bool
	// Webhook dispatches the async project webhook (type + key/value pairs).
	Webhook func(info map[string]any, eventType string, values map[string]any)
}

// Hooks is the process wide project integration point set. It must be
// configured before the HTTP server starts.
var Hooks ProjectHooks

// webhook triggers the project webhook when a hook is installed.
func (s *Service) webhook(info map[string]any, eventType string, values map[string]any) {
	if Hooks.Webhook == nil {
		return
	}
	Hooks.Webhook(info, eventType, values)
}

// stopProjectBefore calls the stop hook used by closeFirst uploads.
func (s *Service) stopProjectBefore(info map[string]any) error {
	if Hooks.StopProject == nil {
		s.Log().Warn("closeFirst requested but no project commander is installed", "project", info["id"])
		return nil
	}
	return Hooks.StopProject(info)
}

// restartProjectAfter calls the restart hook used by the after parameter.
func (s *Service) restartProjectAfter(info map[string]any) (bool, any, bool) {
	if Hooks.RestartProject == nil {
		s.Log().Warn("after restart requested but no project commander is installed", "project", info["id"])
		return false, nil, false
	}
	success, _, data := Hooks.RestartProject(info)
	return success, data, true
}

// projectRunning reports whether the project is running, false when no hook is
// installed.
func (s *Service) projectRunning(info map[string]any) bool {
	if Hooks.IsProjectRunning == nil {
		return false
	}
	return Hooks.IsProjectRunning(info)
}
