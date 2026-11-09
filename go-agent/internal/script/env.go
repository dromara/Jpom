/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package script

import (
	"path/filepath"
	"sort"
	"strings"
)

// privacyPlaceholder is the mask printed instead of a privacy environment
// variable value (Const.PRIVACY_PLACEHOLDER).
const privacyPlaceholder = "******"

// privacyVariableKeywords marks environment variables whose value must not be
// written to the console log (Const.PRIVACY_VARIABLE_KEYWORDS).
var privacyVariableKeywords = []string{"pwd", "pass", "password", "token"}

// EnvItem is one environment variable with its display metadata, mirroring
// cn.keepbx EnvironmentMapBuilder.Item.
type EnvItem struct {
	Value   string
	Privacy bool
	System  bool
}

// EnvBuilder builds the environment variables of a script execution. It keeps
// insertion order so the console banner is stable.
type EnvBuilder struct {
	order []string
	items map[string]EnvItem
}

// NewEnvBuilder creates an empty builder.
func NewEnvBuilder() *EnvBuilder {
	return &EnvBuilder{items: make(map[string]EnvItem, 16)}
}

// Put adds or replaces a normal environment variable.
func (b *EnvBuilder) Put(name, value string) *EnvBuilder {
	if _, exists := b.items[name]; !exists {
		b.order = append(b.order, name)
	}
	b.items[name] = EnvItem{Value: value}
	return b
}

// PutSystem adds a system variable; privacy is derived from its name.
func (b *EnvBuilder) PutSystem(name, value string) *EnvBuilder {
	privacy := false
	lower := strings.ToLower(name)
	for _, keyword := range privacyVariableKeywords {
		if strings.Contains(lower, keyword) {
			privacy = true
			break
		}
	}
	if _, exists := b.items[name]; !exists {
		b.order = append(b.order, name)
	}
	b.items[name] = EnvItem{Value: value, Privacy: privacy, System: true}
	return b
}

// PutStr adds every entry of map (nil is ignored).
func (b *EnvBuilder) PutStr(values map[string]string) *EnvBuilder {
	keys := make([]string, 0, len(values))
	for key := range values {
		keys = append(keys, key)
	}
	sort.Strings(keys)
	for _, key := range keys {
		b.Put(key, values[key])
	}
	return b
}

// Get returns the value of name ("" when absent), mirroring
// EnvironmentMapBuilder#get.
func (b *EnvBuilder) Get(name string) (string, bool) {
	item, ok := b.items[name]
	if !ok {
		return "", false
	}
	return item.Value, true
}

// Environment returns the values applied to the process environment. Null
// values are skipped, exactly like the Java implementation.
func (b *EnvBuilder) Environment() map[string]string {
	values := make(map[string]string, len(b.items))
	for name, item := range b.items {
		values[name] = item.Value
	}
	return values
}

// EachStr calls fn with the banner and the key=value listing printed at the
// beginning of every script execution log
// (EnvironmentMapBuilder#eachStr).
func (b *EnvBuilder) EachStr(fn func(string)) {
	if len(b.order) == 0 {
		return
	}
	banner := strings.Repeat("#", 82)
	fn(banner)
	for _, name := range b.order {
		item := b.items[name]
		value := item.Value
		if item.Privacy {
			value = privacyPlaceholder
		}
		fn(name + "=" + value)
	}
	fn(banner)
}

// WorkspaceEnvVar is one environment variable entry of a workspace.
type WorkspaceEnvVar struct {
	Value string
	// Privacy defaults to true when the field is missing (Java compatibility
	// note: older data has no privacy field and is treated as privacy).
	Privacy bool
}

// workspaceEnvVarFile is the schema of <data>/workspace_env_var.json.
type workspaceEnvVarFile map[string]struct {
	VarData map[string]struct {
		Name    string `json:"name"`
		Value   string `json:"value"`
		Privacy *int   `json:"privacy"`
	} `json:"varData"`
}

// WorkspaceEnv returns the environment builder of a workspace
// (AgentWorkspaceEnvVarService#getEnv): all variables are system/privacy
// variables when the privacy flag is missing or 1.
func (m *Manager) WorkspaceEnv(workspaceID string) (*EnvBuilder, error) {
	builder := NewEnvBuilder()
	if strings.TrimSpace(workspaceID) == "" {
		return builder, nil
	}
	var file workspaceEnvVarFile
	path := filepath.Join(m.app.DataDir, "workspace_env_var.json")
	if err := readObject(path, &file); err != nil {
		return builder, err
	}
	workspace, ok := file[workspaceID]
	if !ok {
		return builder, nil
	}
	names := make([]string, 0, len(workspace.VarData))
	for key := range workspace.VarData {
		names = append(names, key)
	}
	sort.Strings(names)
	for _, key := range names {
		item := workspace.VarData[key]
		name := item.Name
		if name == "" {
			name = key
		}
		privacy := item.Privacy == nil || *item.Privacy == 1
		if _, exists := builder.items[name]; !exists {
			builder.order = append(builder.order, name)
		}
		builder.items[name] = EnvItem{Value: item.Value, Privacy: privacy, System: true}
	}
	return builder, nil
}
