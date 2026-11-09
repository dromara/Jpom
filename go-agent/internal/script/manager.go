/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// Package script owns the agent script templates, the global script library,
// the asynchronous script executor and its execution logs.
//
// The on disk layout is identical to the Java agent:
//
//	<data>/script.json                     模板数据（以 id 为 key）
//	<data>/script_log.json                 执行记录（触发器执行记录）
//	<data>/global-script/<id>.json         脚本库
//	<data>/script/<id>/log/<executeId>.log 执行日志
//	<data>/script_run_cache/<uuid>.sh      执行时生成的临时脚本
//
// so a node can switch between the Java and the Go agent without migrating data.
package script

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"jpom.io/go-agent/internal/app"
)

const (
	// FileName is the script template data file inside <data>/.
	FileName = "script.json"
	// ExecLogFileName is the trigger execution log data file inside <data>/.
	ExecLogFileName = "script_log.json"
	// GlobalScriptDirName is the script library directory inside <data>/.
	GlobalScriptDirName = "global-script"
)

// Manager reads and writes the script domain data files.
type Manager struct {
	app *app.App

	mu          sync.Mutex
	scriptFile  string
	execLogFile string
}

// NewManager creates the script manager for the runtime.
func NewManager(a *app.App) *Manager {
	return &Manager{
		app:         a,
		scriptFile:  filepath.Join(a.DataDir, FileName),
		execLogFile: filepath.Join(a.DataDir, ExecLogFileName),
	}
}

// App returns the owning runtime.
func (m *Manager) App() *app.App { return m.app }

// List returns every script template in file order.
func (m *Manager) List() ([]map[string]any, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	records, err := readRecords(m.scriptFile)
	if err != nil {
		return nil, err
	}
	values := make([]map[string]any, 0, len(records))
	for _, record := range records {
		if record.Value["id"] == nil {
			record.Value["id"] = record.Key
		}
		values = append(values, record.Value)
	}
	return values, nil
}

// Get returns one script template by id.
func (m *Manager) Get(id string) (map[string]any, bool, error) {
	if strings.TrimSpace(id) == "" {
		return nil, false, nil
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	records, err := readRecords(m.scriptFile)
	if err != nil {
		return nil, false, err
	}
	for _, record := range records {
		if record.Key == id {
			if record.Value["id"] == nil {
				record.Value["id"] = record.Key
			}
			return record.Value, true, nil
		}
	}
	return nil, false, nil
}

// Add stores a new script template. The id must not exist yet.
func (m *Manager) Add(value map[string]any) error {
	id := stringField(value, "id")
	if id == "" {
		return fmt.Errorf("script id is required")
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	records, err := readRecords(m.scriptFile)
	if err != nil {
		return err
	}
	for _, record := range records {
		if record.Key == id {
			return fmt.Errorf("script id already exists")
		}
	}
	records = append(records, Record{Key: id, Value: value})
	return writeRecords(m.scriptFile, records, scriptFieldOrder)
}

// Update replaces an existing script template.
func (m *Manager) Update(value map[string]any) error {
	id := stringField(value, "id")
	if id == "" {
		return fmt.Errorf("script id is required")
	}
	return m.UpdateFields(id, value)
}

// UpdateFields merges fields into an existing script template. It mirrors the
// Java updateById: unknown/historical fields of the stored record are kept.
func (m *Manager) UpdateFields(id string, fields map[string]any) error {
	if strings.TrimSpace(id) == "" {
		return fmt.Errorf("script id is required")
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	records, err := readRecords(m.scriptFile)
	if err != nil {
		return err
	}
	for index, record := range records {
		if record.Key != id {
			continue
		}
		merged := cloneObject(record.Value)
		for key, value := range fields {
			if key == "id" {
				continue
			}
			merged[key] = value
		}
		merged["id"] = id
		records[index].Value = merged
		return writeRecords(m.scriptFile, records, scriptFieldOrder)
	}
	return fmt.Errorf("script does not exist: %s", id)
}

// Delete removes a script template and its execution log directory, mirroring
// NodeScriptServer.deleteItem.
func (m *Manager) Delete(id string) error {
	if strings.TrimSpace(id) == "" {
		return nil
	}
	m.mu.Lock()
	records, err := readRecords(m.scriptFile)
	if err != nil {
		m.mu.Unlock()
		return err
	}
	filtered := make([]Record, 0, len(records))
	for _, record := range records {
		if record.Key == id {
			continue
		}
		filtered = append(filtered, record)
	}
	writeErr := writeRecords(m.scriptFile, filtered, scriptFieldOrder)
	m.mu.Unlock()
	// 删除脚本同时删除执行日志目录（Java: FileUtil.del(scriptPath())）
	if err := os.RemoveAll(m.ScriptPath(id)); err != nil && !os.IsNotExist(err) {
		return err
	}
	return writeErr
}

// ScriptPath is <data>/script/<id>, the directory holding the execution logs.
func (m *Manager) ScriptPath(id string) string {
	return filepath.Join(m.app.ScriptDir, id)
}

// LogDir is <data>/script/<id>/log.
func (m *Manager) LogDir(id string) string {
	return filepath.Join(m.ScriptPath(id), "log")
}

// LogFile is <data>/script/<id>/log/<executeId>.log (NodeScriptModel.logFile).
func (m *Manager) LogFile(id, executeID string) string {
	return filepath.Join(m.LogDir(id), executeID+".log")
}

// DeleteLog removes one execution log file. It reports whether the file still
// exists after the deletion (Java CommandUtil.systemFastDel semantics).
func (m *Manager) DeleteLog(id, executeID string) (bool, error) {
	path := m.LogFile(id, executeID)
	if err := os.Remove(path); err != nil && !os.IsNotExist(err) {
		return true, err
	}
	_, statErr := os.Stat(path)
	return !os.IsNotExist(statErr), nil
}

// ListExecLogs returns script_log.json records in file order.
func (m *Manager) ListExecLogs() ([]map[string]any, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	records, err := readRecords(m.execLogFile)
	if err != nil {
		return nil, err
	}
	values := make([]map[string]any, 0, len(records))
	for _, record := range records {
		if record.Value["id"] == nil {
			record.Value["id"] = record.Key
		}
		values = append(values, record.Value)
	}
	return values, nil
}

// AddExecLog appends a script execution record.
func (m *Manager) AddExecLog(value map[string]any) error {
	id := stringField(value, "id")
	if id == "" {
		return fmt.Errorf("execution log id is required")
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	records, err := readRecords(m.execLogFile)
	if err != nil {
		return err
	}
	records = append(records, Record{Key: id, Value: value})
	return writeRecords(m.execLogFile, records, execLogFieldOrder)
}

// DeleteExecLog removes one script execution record.
func (m *Manager) DeleteExecLog(id string) error {
	if strings.TrimSpace(id) == "" {
		return nil
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	records, err := readRecords(m.execLogFile)
	if err != nil {
		return err
	}
	filtered := make([]Record, 0, len(records))
	for _, record := range records {
		if record.Key == id {
			continue
		}
		filtered = append(filtered, record)
	}
	return writeRecords(m.execLogFile, filtered, execLogFieldOrder)
}

// RunCacheDir returns <data>/script_run_cache, where the temporary execution
// scripts are written.
func (m *Manager) RunCacheDir() string { return m.app.ScriptRunCacheDir }

// ConsoleCharset returns jpom.system.console-charset (platform default when
// absent): the charset script content is written with and process output is read
// with in the Java agent (ExtConfigBean.getConsoleLogCharset).
func (m *Manager) ConsoleCharset() string {
	if m.app != nil && m.app.Config != nil {
		return ConsoleCharset(m.app.Config.Jpom.System.ConsoleCharset)
	}
	return PlatformCharset()
}

// NewExecutionID returns a 32 char hex id (Hutool fastSimpleUUID equivalent).
func NewExecutionID() string {
	buffer := make([]byte, 16)
	if _, err := rand.Read(buffer); err != nil {
		return fmt.Sprintf("%d", time.Now().UnixNano())
	}
	return hex.EncodeToString(buffer)
}

// WriteScriptFile resolves the script library references and writes the script
// content into the run cache directory. The returned path is executed with
// bash/cmd, so no executable bit is required.
func (m *Manager) WriteScriptFile(context string) (string, error) {
	replaced, err := m.ReferenceReplace(context)
	if err != nil {
		return "", err
	}
	// Java FileUtils.writeScript replaces LF with the platform line separator.
	lineSeparator := "\n"
	if os.PathSeparator == '\\' {
		lineSeparator = "\r\n"
	}
	content := strings.ReplaceAll(replaced, "\n", lineSeparator)
	if err := os.MkdirAll(m.RunCacheDir(), 0o755); err != nil {
		return "", err
	}
	suffix := "sh"
	if os.PathSeparator == '\\' {
		suffix = "bat"
	}
	encoded, err := EncodeText(content, m.ConsoleCharset())
	if err != nil {
		return "", err
	}
	path := filepath.Join(m.RunCacheDir(), NewExecutionID()+"."+suffix)
	if err := os.WriteFile(path, encoded, 0o644); err != nil {
		return "", err
	}
	return path, nil
}

// WriteRawScriptFile writes content (already resolved) into the run cache
// directory without resolving script library references, matching the Java
// DslScriptServer#initScriptFile(ScriptLibraryModel) behaviour.
func (m *Manager) WriteRawScriptFile(context string) (string, error) {
	lineSeparator := "\n"
	if os.PathSeparator == '\\' {
		lineSeparator = "\r\n"
	}
	content := strings.ReplaceAll(context, "\n", lineSeparator)
	if err := os.MkdirAll(m.RunCacheDir(), 0o755); err != nil {
		return "", err
	}
	suffix := "sh"
	if os.PathSeparator == '\\' {
		suffix = "bat"
	}
	encoded, err := EncodeText(content, m.ConsoleCharset())
	if err != nil {
		return "", err
	}
	path := filepath.Join(m.RunCacheDir(), NewExecutionID()+"."+suffix)
	if err := os.WriteFile(path, encoded, 0o644); err != nil {
		return "", err
	}
	return path, nil
}
