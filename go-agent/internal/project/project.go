/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// Package project owns the project.json data file and the shared rules used to
// resolve a project directory/log directory. It is consumed by the project,
// project-file and script modules.
package project

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"

	"github.com/dromara/Jpom/go-agent/internal/store"
)

// FileName is the project data file inside <data>/.
const FileName = "project.json"

// DataFile is the whitelist data file inside <data>/.
const whitelistFileName = "whitelistDirectory.json"

// Manager reads/writes data/project.json.
type Manager struct {
	baseDir string
	file    *store.Store
	mu      sync.Mutex
}

// New creates a project manager for the agent base directory.
func New(baseDir string) *Manager {
	return &Manager{
		baseDir: baseDir,
		file:    store.New(filepath.Join(baseDir, "data", FileName)),
	}
}

// All returns every project keyed by id.
func (m *Manager) All() (map[string]map[string]any, error) {
	values := map[string]map[string]any{}
	if err := m.file.Read(&values); err != nil {
		return nil, err
	}
	return values, nil
}

// Get returns one project by id.
func (m *Manager) Get(id string) (map[string]any, bool, error) {
	all, err := m.All()
	if err != nil {
		return nil, false, err
	}
	value, ok := all[id]
	return value, ok, nil
}

// SaveAll replaces the whole project file (caller must hold semantics in mind).
func (m *Manager) SaveAll(values map[string]map[string]any) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	return m.file.Write(values)
}

// Mutate runs fn holding the project file lock; fn receives the decoded map and
// must return the map to persist.
func (m *Manager) Mutate(fn func(values map[string]map[string]any) error) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	values := map[string]map[string]any{}
	if err := m.file.Read(&values); err != nil {
		return err
	}
	if err := fn(values); err != nil {
		return err
	}
	return m.file.Write(values)
}

// WorkspaceID returns the project workspace (DEFAULT when absent).
func WorkspaceID(info map[string]any) string {
	if info == nil {
		return "DEFAULT"
	}
	if value, ok := info["workspaceId"].(string); ok && value != "" {
		return value
	}
	return "DEFAULT"
}

// String reads a string field from a project map.
func String(info map[string]any, key string) string {
	if info == nil {
		return ""
	}
	switch value := info[key].(type) {
	case string:
		return value
	case json.Number:
		return value.String()
	case float64:
		return fmt.Sprintf("%v", value)
	case bool:
		return fmt.Sprintf("%v", value)
	default:
		return ""
	}
}

// ProjectPath resolves <whitelistDirectory>/<lib>, the directory where the
// project's files live (same as Java NodeProjectInfoModel.allLib()).
func ProjectPath(info map[string]any) (string, error) {
	whitelistDirectory := strings.TrimSpace(String(info, "whitelistDirectory"))
	if whitelistDirectory == "" {
		return "", ValidationKey(keyRestoreAuthorize)
	}
	lib := String(info, "lib")
	if lib == "" {
		return "", ValidationKey(keyInvalidProjectPath)
	}
	return filepath.Join(whitelistDirectory, lib), nil
}

// LogDir resolves the directory holding the project console logs.
func (m *Manager) LogDir(info map[string]any) (string, error) {
	id := String(info, "id")
	if id == "" {
		return "", ValidationKey(keyProjectIDNotFound)
	}
	if custom := strings.TrimSpace(String(info, "logPath")); custom != "" {
		return filepath.Join(custom, id), nil
	}
	return filepath.Join(m.baseDir, "project-log", id), nil
}

// Whitelist is the content of data/whitelistDirectory.json.
type Whitelist struct {
	AllowEditSuffix []string `json:"allowEditSuffix"`
	Project         []string `json:"project"`
}

// ReadWhitelist loads the whitelist file.
func ReadWhitelist(baseDir string) (Whitelist, error) {
	whitelist := Whitelist{}
	content, err := os.ReadFile(filepath.Join(baseDir, "data", whitelistFileName))
	if err != nil {
		if os.IsNotExist(err) {
			return whitelist, nil
		}
		return whitelist, err
	}
	if len(content) == 0 {
		return whitelist, nil
	}
	err = json.Unmarshal(content, &whitelist)
	return whitelist, err
}

// WriteWhitelist replaces the whitelist file atomically.
func WriteWhitelist(baseDir string, whitelist Whitelist) error {
	path := filepath.Join(baseDir, "data", whitelistFileName)
	content, err := json.Marshal(whitelist)
	if err != nil {
		return err
	}
	dir := filepath.Dir(path)
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return err
	}
	tmp, err := os.CreateTemp(dir, ".whitelist-*")
	if err != nil {
		return err
	}
	defer func() { _ = os.Remove(tmp.Name()) }()
	if _, err := tmp.Write(content); err != nil {
		_ = tmp.Close()
		return err
	}
	if err := tmp.Close(); err != nil {
		return err
	}
	return os.Rename(tmp.Name(), path)
}

// CheckPath reports whether path is inside one of the authorized directories.
func (w Whitelist) CheckPath(path string) bool {
	target := filepath.Clean(path)
	for _, dir := range w.Project {
		cleaned := filepath.Clean(strings.TrimSpace(dir))
		if cleaned == "" {
			continue
		}
		if target == cleaned || strings.HasPrefix(target, cleaned+string(filepath.Separator)) {
			return true
		}
	}
	return false
}

// CheckSuffix reports whether the file extension is allowed to be edited.
func (w Whitelist) CheckSuffix(path string) bool {
	if len(w.AllowEditSuffix) == 0 {
		return false
	}
	ext := strings.TrimPrefix(strings.ToLower(filepath.Ext(path)), ".")
	for _, suffix := range w.AllowEditSuffix {
		if strings.EqualFold(strings.TrimPrefix(suffix, "."), ext) {
			return true
		}
	}
	return false
}
