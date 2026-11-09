/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package sysinfo

import (
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"sync"

	"jpom.io/go-agent/internal/app"
)

// workspaceEnvVarFile 是 data/workspace_env_var.json 的结构：workspaceId -> 变量集。
type workspaceEnvVarFile map[string]*workspaceEnvItem

type workspaceEnvItem struct {
	ID      string                        `json:"id,omitempty"`
	Name    string                        `json:"name,omitempty"`
	VarData map[string]*workspaceEnvValue `json:"varData,omitempty"`
}

type workspaceEnvValue struct {
	Name        string `json:"name,omitempty"`
	Value       string `json:"value,omitempty"`
	Description string `json:"description,omitempty"`
	Privacy     *int   `json:"privacy,omitempty"`
}

// Java 端对 workspace_env 的写操作是 synchronized，这里保持同样的串行语义。
var workspaceEnvMu sync.Mutex

func workspaceEnvPath(a *app.App) string {
	return filepath.Join(a.DataDir, "workspace_env_var.json")
}

func readWorkspaceEnvFile(a *app.App) (workspaceEnvVarFile, error) {
	values := workspaceEnvVarFile{}
	content, err := os.ReadFile(workspaceEnvPath(a))
	if err != nil {
		if os.IsNotExist(err) {
			return values, nil
		}
		return nil, err
	}
	if len(content) == 0 {
		return values, nil
	}
	if err := jsonUnmarshal(content, &values); err != nil {
		return nil, fmt.Errorf("parse workspace_env_var.json failed: %w", err)
	}
	return values, nil
}

func writeWorkspaceEnvFile(a *app.App, values workspaceEnvVarFile) error {
	return writeJSONAtomic(workspaceEnvPath(a), values)
}

// UpdateWorkspaceEnv 对应 /system/workspace_env/update。
func UpdateWorkspaceEnv(a *app.App, workspaceID, name, value, description string, privacy *int) error {
	// @ValidatorItem 校验失败 -> code 400（自定义文案优先）
	if strings.TrimSpace(name) == "" {
		return paramErr(400, "i18n.name_required.856d")
	}
	if strings.TrimSpace(value) == "" {
		return paramErr(400, "i18n.value.2e81")
	}
	if strings.TrimSpace(description) == "" {
		return paramErr(400, "i18n.description.615e")
	}
	workspaceEnvMu.Lock()
	defer workspaceEnvMu.Unlock()
	values, err := readWorkspaceEnvFile(a)
	if err != nil {
		return err
	}
	item, ok := values[workspaceID]
	if !ok || item == nil {
		item = &workspaceEnvItem{ID: workspaceID, Name: workspaceID, VarData: map[string]*workspaceEnvValue{}}
		values[workspaceID] = item
	}
	if item.VarData == nil {
		item.VarData = map[string]*workspaceEnvValue{}
	}
	item.VarData[name] = &workspaceEnvValue{Name: name, Value: value, Description: description, Privacy: privacy}
	return writeWorkspaceEnvFile(a, values)
}

// DeleteWorkspaceEnv 对应 /system/workspace_env/delete。
func DeleteWorkspaceEnv(a *app.App, workspaceID, name string) error {
	workspaceEnvMu.Lock()
	defer workspaceEnvMu.Unlock()
	values, err := readWorkspaceEnvFile(a)
	if err != nil {
		return err
	}
	if item, ok := values[workspaceID]; ok && item != nil {
		delete(item.VarData, name)
		if err := writeWorkspaceEnvFile(a, values); err != nil {
			return err
		}
	}
	return nil
}

// WorkspaceEnvKeys 返回指定工作空间的环境变量名（排序后保持稳定）。
func WorkspaceEnvKeys(a *app.App, workspaceID string) []string {
	values, err := readWorkspaceEnvFile(a)
	if err != nil {
		return nil
	}
	item, ok := values[workspaceID]
	if !ok || item == nil || len(item.VarData) == 0 {
		return nil
	}
	keys := make([]string, 0, len(item.VarData))
	for key := range item.VarData {
		keys = append(keys, key)
	}
	sort.Strings(keys)
	return keys
}

// WorkspaceEnv 返回指定工作空间的环境变量（供项目/脚本执行时注入）。
func WorkspaceEnv(a *app.App, workspaceID string) map[string]string {
	values, err := readWorkspaceEnvFile(a)
	if err != nil {
		return nil
	}
	item, ok := values[workspaceID]
	if !ok || item == nil {
		return nil
	}
	result := make(map[string]string, len(item.VarData))
	for key, value := range item.VarData {
		if value == nil {
			continue
		}
		result[key] = value.Value
	}
	return result
}
