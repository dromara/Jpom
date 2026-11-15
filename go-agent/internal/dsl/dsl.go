/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// Package dsl parses the project dslContent YAML and executes the configured
// run processes (start/status/stop/restart/reload), mirroring the Java agent:
//
//	DslYmlDto            -> Content
//	DslScriptServer      -> Engine (resolve + execute)
//	DslScriptBuilder     -> run/syncRun
//
// The execution engine is registered into the project domain through
// project.RegisterDslExecutor so /manage/operate and the console websocket use
// it (the project package keeps a built-in fallback).
package dsl

import (
	"bufio"
	"bytes"
	"fmt"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strings"
	"sync"
	"time"

	"gopkg.in/yaml.v3"

	"github.com/dromara/Jpom/go-agent/internal/i18n"
	"github.com/dromara/Jpom/go-agent/internal/project"
	"github.com/dromara/Jpom/go-agent/internal/script"
)

// Process is one DSL run process (DslYmlDto.BaseProcess).
type Process struct {
	ScriptID   string
	ScriptArgs string
	ScriptEnv  map[string]string
}

// Run is the "run" node of the DSL configuration.
type Run struct {
	Start            *Process
	Status           *Process
	Stop             *Process
	Restart          *Process
	Reload           *Process
	FileChangeReload bool
	ExecPath         string
}

// FileConfig is the "file" node (backup configuration, used by the file domain).
type FileConfig struct {
	BackupCount  int
	BackupSuffix []string
	BackupPath   string
	DiffBackup   bool
}

// Config is the "config" node.
type Config struct {
	AutoBackToFile bool
}

// Content is the parsed dslContent YAML (DslYmlDto).
type Content struct {
	Description string
	Run         Run
	File        FileConfig
	Config      Config
}

// processNames is the ConsoleCommandOp subset supported by DSL.
var processNames = []string{"start", "status", "stop", "restart", "reload"}

// Parse parses the dslContent YAML. Unknown keys are ignored, camelCase /
// snake_case / kebab-case spellings are all accepted (the Java agent binds the
// bean field names, historical documents also use snake_case).
func Parse(content string) (*Content, error) {
	if strings.TrimSpace(content) == "" {
		return nil, fmt.Errorf("dsl content is empty")
	}
	raw := map[string]any{}
	if err := yaml.Unmarshal([]byte(content), &raw); err != nil {
		return nil, fmt.Errorf("parse dsl content failed: %w", err)
	}
	parsed := &Content{Description: pickString(raw, "description")}
	if runRaw := pickMap(raw, "run"); runRaw != nil {
		parsed.Run.ExecPath = pickString(runRaw, "execPath")
		parsed.Run.FileChangeReload = pickBool(runRaw, "fileChangeReload")
		for _, name := range processNames {
			if processRaw := pickMap(runRaw, name); processRaw != nil {
				process := &Process{
					ScriptID:   pickString(processRaw, "scriptId"),
					ScriptArgs: pickString(processRaw, "scriptArgs"),
					ScriptEnv:  pickStringMap(processRaw, "scriptEnv"),
				}
				parsed.setProcess(name, process)
			}
		}
	}
	if fileRaw := pickMap(raw, "file"); fileRaw != nil {
		parsed.File.BackupCount = pickInt(fileRaw, "backupCount")
		parsed.File.BackupPath = pickString(fileRaw, "backupPath")
		parsed.File.DiffBackup = pickBool(fileRaw, "diffBackup")
		parsed.File.BackupSuffix = pickStringSlice(fileRaw, "backupSuffix")
	}
	if configRaw := pickMap(raw, "config"); configRaw != nil {
		parsed.Config.AutoBackToFile = pickBool(configRaw, "autoBackToFile")
	}
	return parsed, nil
}

func (c *Content) setProcess(name string, process *Process) {
	switch name {
	case "start":
		c.Run.Start = process
	case "status":
		c.Run.Status = process
	case "stop":
		c.Run.Stop = process
	case "restart":
		c.Run.Restart = process
	case "reload":
		c.Run.Reload = process
	}
}

// HasProcess reports whether op is configured.
func (c *Content) HasProcess(op string) bool {
	return c.Process(op) != nil
}

// Process returns the configured process of op (nil when absent).
func (c *Content) Process(op string) *Process {
	if c == nil {
		return nil
	}
	switch op {
	case "start":
		return c.Run.Start
	case "status":
		return c.Run.Status
	case "stop":
		return c.Run.Stop
	case "restart":
		return c.Run.Restart
	case "reload":
		return c.Run.Reload
	default:
		return nil
	}
}

// Resolved is the executable resolved from a process scriptId: a project file,
// a node script or a script library entry (DslScriptServer.resolveProcessScript).
type Resolved struct {
	// Type is "file", "script" or "library".
	Type     string
	ScriptID string
	Path     string
	Temp     bool
	Args     string
	Env      map[string]string
	ExecPath string
}

// Engine resolves and executes DSL processes. It reads node scripts and script
// libraries through the script manager.
type Engine struct {
	scripts *script.Manager
	lang    string
}

// NewEngine creates a DSL engine (default language from jpom.system.lang).
func NewEngine(scripts *script.Manager) *Engine {
	return &Engine{scripts: scripts, lang: i18n.DefaultLang()}
}

var registerOnce sync.Once

// Register wires the engine into the project domain (the project package calls
// it for /manage/operate DSL processes). It is safe to call multiple times.
func Register(scripts *script.Manager, lang ...string) {
	registerOnce.Do(func() {
		engine := NewEngine(scripts)
		if len(lang) > 0 && strings.TrimSpace(lang[0]) != "" {
			engine.lang = lang[0]
		}
		project.RegisterDslExecutor(engine)
	})
}

// Resolve resolves the process scriptId into an executable file:
//   - G@("tag") style library reference (G@tag accepted) -> global-script/<tag>.json
//   - node script id -> data/script.json entry
//   - otherwise a project file relative to the project lib directory
func (e *Engine) Resolve(original map[string]any, process *Process) (*Resolved, error) {
	if process == nil {
		return nil, fmt.Errorf("%s", i18n.T(e.lang, "i18n.process_does_not_exist.4e39"))
	}
	scriptID := strings.TrimSpace(process.ScriptID)
	if scriptID == "" {
		return nil, fmt.Errorf("%s", i18n.T(e.lang, "i18n.script_template_id_required.f339"))
	}
	resolved := &Resolved{ScriptID: scriptID, Args: process.ScriptArgs}
	resolved.Env = process.ScriptEnv
	upper := strings.ToUpper(scriptID)
	if strings.HasPrefix(upper, "G@") {
		tag := strings.TrimPrefix(scriptID, scriptID[:2])
		library, exists, err := e.scripts.LibraryGet(tag)
		if err != nil {
			return nil, err
		}
		if !exists {
			return nil, fmt.Errorf("%s%s", i18n.T(e.lang, "i18n.missing_script_library_message.be9a"), tag)
		}
		path, err := e.scripts.WriteRawScriptFile(stringField(library, "script"))
		if err != nil {
			return nil, err
		}
		resolved.Type = "library"
		resolved.Path = path
		resolved.Temp = true
		return resolved, nil
	}
	item, exists, err := e.scripts.Get(scriptID)
	if err != nil {
		return nil, err
	}
	if exists {
		path, err := e.scripts.WriteScriptFile(stringField(item, "context"))
		if err != nil {
			return nil, err
		}
		resolved.Type = "script"
		resolved.Path = path
		resolved.Temp = true
		return resolved, nil
	}
	libDir, err := project.ProjectPath(original)
	if err != nil {
		return nil, err
	}
	file := filepath.Join(libDir, scriptID)
	info, err := os.Stat(file)
	if err != nil || info.IsDir() {
		return nil, fmt.Errorf("%s%s", i18n.T(e.lang, "i18n.script_template_not_exist.e05f"), scriptID)
	}
	resolved.Type = "file"
	resolved.Path = file
	return resolved, nil
}

// syncResult is the outcome of one DSL execution.
type syncResult struct {
	exitCode int
	lines    []string
	err      error
}

// Run implements project.DslExecutor: runs the configured process and appends
// the console output to the project log file (DslScriptServer#run).
func (e *Engine) Run(runtime *project.Runtime, info, original map[string]any, op string, sync bool) error {
	content := e.parse(original)
	if content == nil {
		return fmt.Errorf("%s", i18n.T(e.lang, "i18n.dsl_info_not_configured.3487"))
	}
	process := content.Process(op)
	if process == nil {
		return fmt.Errorf("%s", i18n.T(e.lang, "i18n.dsl_not_configured.8a57", op))
	}
	resolved, err := e.Resolve(original, process)
	if err != nil {
		return err
	}
	resolved.ExecPath = content.Run.ExecPath
	defer removeTemp(resolved)
	logFile, err := runtime.Manager.TouchLogFile(info)
	if err != nil {
		return err
	}
	environment, err := e.environment(original, resolved, logFile, runtime)
	if err != nil {
		return err
	}
	result := e.execute(resolved, environment, op)
	if result.err != nil {
		return result.err
	}
	lines := make([]string, 0, len(result.lines)+3)
	lines = append(lines, "[SYSTEM-INFO] "+i18n.T(e.lang, "i18n.start_executing.f0b9", op))
	lines = append(lines, result.lines...)
	lines = append(lines, "[SYSTEM-INFO] "+i18n.T(e.lang, "i18n.execution_ended_with_detail.8f93", op, result.exitCode))
	return appendLogLines(logFile, lines)
}

// SyncRun implements project.DslExecutor: executes the process synchronously and
// returns the exit code plus the formatted console lines.
func (e *Engine) SyncRun(runtime *project.Runtime, info, original map[string]any, op string) (int, []string, error) {
	content := e.parse(original)
	if content == nil {
		return 0, nil, fmt.Errorf("%s", i18n.T(e.lang, "i18n.dsl_info_not_configured.3487"))
	}
	process := content.Process(op)
	if process == nil {
		return 0, nil, fmt.Errorf("%s", i18n.T(e.lang, "i18n.dsl_not_configured.8a57", op))
	}
	resolved, err := e.Resolve(original, process)
	if err != nil {
		return 0, nil, err
	}
	resolved.ExecPath = content.Run.ExecPath
	defer removeTemp(resolved)
	logFile, _ := runtime.Manager.ResolveLogFile(info)
	environment, err := e.environment(original, resolved, logFile, runtime)
	if err != nil {
		return 0, nil, err
	}
	result := e.execute(resolved, environment, op)
	if result.err != nil {
		return result.exitCode, result.lines, result.err
	}
	lines := make([]string, 0, len(result.lines)+1)
	lines = append(lines, formatLine(op, i18n.T(e.lang, "i18n.exit_code.3b54", result.exitCode)))
	lines = append(lines, result.lines...)
	return result.exitCode, lines, nil
}

// parse parses the project dslContent, returning nil when absent/invalid.
func (e *Engine) parse(original map[string]any) *Content {
	content, err := Parse(project.GetString(original, "dslContent"))
	if err != nil {
		return nil
	}
	return content
}

// environment builds the process environment: workspace variables, project
// dslEnv, process scriptEnv and the Jpom DSL variables
// (DslScriptServer#environment).
func (e *Engine) environment(original map[string]any, resolved *Resolved, logFile string, runtime *project.Runtime) (*script.EnvBuilder, error) {
	workspaceID := project.GetString(original, "workspaceId")
	if workspaceID == "" {
		workspaceID = "DEFAULT"
	}
	builder, err := e.scripts.WorkspaceEnv(workspaceID)
	if err != nil {
		return nil, err
	}
	if dslEnv := project.GetString(original, "dslEnv"); dslEnv != "" {
		values, err := url.ParseQuery(dslEnv)
		if err == nil {
			flat := make(map[string]string, len(values))
			for key, items := range values {
				if len(items) > 0 {
					flat[key] = items[0]
				} else {
					flat[key] = ""
				}
			}
			builder.PutStr(flat)
		}
	}
	builder.PutStr(resolved.Env)
	libPath, _ := project.ProjectPath(original)
	builder.Put("PROJECT_ID", project.GetString(original, "id"))
	builder.Put("PROJECT_NAME", project.GetString(original, "name"))
	builder.Put("PROJECT_PATH", libPath)
	builder.Put("PROJECT_LOG_FILE", logFile)
	builder.Put("JPOM_EXEC_PATH", resolved.ExecPath)
	return builder, nil
}

// execute runs the resolved script and returns the formatted output lines.
func (e *Engine) execute(resolved *Resolved, environment *script.EnvBuilder, op string) syncResult {
	command := script.BuildCommand(resolved.Path, resolved.Args)
	cmd := exec.Command(command[0], command[1:]...)
	cmd.Dir = workingDirectory(resolved, environment)
	cmd.Env = script.MergeEnvironment(environment.Environment())
	readEnd, writeEnd, err := os.Pipe()
	if err != nil {
		return syncResult{exitCode: -100, err: err}
	}
	cmd.Stdout = writeEnd
	cmd.Stderr = writeEnd
	if err := cmd.Start(); err != nil {
		_ = readEnd.Close()
		_ = writeEnd.Close()
		return syncResult{exitCode: -100, lines: []string{formatLine(op, i18n.T(e.lang, "i18n.general_execution_exception.62e9")+err.Error())}}
	}
	_ = writeEnd.Close()
	reader := bufio.NewReaderSize(readEnd, 64*1024)
	lines := make([]string, 0, 64)
	for {
		line, readErr := reader.ReadString('\n')
		if len(line) > 0 {
			// DSL 输出按 console-charset 解码（Windows 默认 GBK）
			lines = append(lines, formatLine(op, script.DecodeLine(trimLineBytes([]byte(line)), e.scripts.ConsoleCharset())))
		}
		if readErr != nil {
			break
		}
	}
	_ = readEnd.Close()
	waitErr := cmd.Wait()
	return syncResult{exitCode: processExitCode(waitErr), lines: lines}
}

// workingDirectory resolves the process working directory
// (DslScriptBuilder#init): JPOM_EXEC_PATH is relative to PROJECT_PATH.
func workingDirectory(resolved *Resolved, environment *script.EnvBuilder) string {
	execPath, hasExecPath := environment.Get("JPOM_EXEC_PATH")
	projectPath, hasProjectPath := environment.Get("PROJECT_PATH")
	if hasExecPath && execPath != "" && hasProjectPath && projectPath != "" {
		if filepath.IsAbs(execPath) {
			return projectPath
		}
		return filepath.Join(projectPath, execPath)
	}
	return filepath.Dir(resolved.Path)
}

// formatLine formats one process output line as
// "yyyy-MM-dd HH:mm:ss.SSS [action] - line" (DslScriptBuilder#formatLine).
func formatLine(action, line string) string {
	return time.Now().Format("2006-01-02 15:04:05.000") + " [" + action + "] - " + line
}

// appendLogLines appends lines to the project console log file.
func appendLogLines(path string, lines []string) error {
	if path == "" || len(lines) == 0 {
		return nil
	}
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return err
	}
	file, err := os.OpenFile(path, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o644)
	if err != nil {
		return err
	}
	defer func() { _ = file.Close() }()
	for _, line := range lines {
		if _, err := file.WriteString(line + "\n"); err != nil {
			return err
		}
	}
	return nil
}

func removeTemp(resolved *Resolved) {
	if resolved != nil && resolved.Temp && resolved.Path != "" {
		_ = os.Remove(resolved.Path)
	}
}

func trimLineEnding(line string) string {
	line = strings.TrimSuffix(line, "\n")
	return strings.TrimSuffix(line, "\r")
}

// trimLineBytes strips the trailing CR/LF from a raw byte line before decoding.
func trimLineBytes(line []byte) []byte {
	line = bytes.TrimSuffix(line, []byte("\n"))
	return bytes.TrimSuffix(line, []byte("\r"))
}

func processExitCode(err error) int {
	if err == nil {
		return 0
	}
	if exitError, ok := err.(*exec.ExitError); ok {
		return exitError.ExitCode()
	}
	return -1
}

// ---- YAML helpers (tolerant key lookup) ----

func normalizeKey(key string) string {
	replacer := strings.NewReplacer("_", "", "-", "", " ", "")
	return strings.ToLower(replacer.Replace(key))
}

func pickMap(value map[string]any, key string) map[string]any {
	if value == nil {
		return nil
	}
	target := normalizeKey(key)
	for rawKey, item := range value {
		if normalizeKey(rawKey) != target {
			continue
		}
		if typed, ok := item.(map[string]any); ok {
			return typed
		}
		return nil
	}
	return nil
}

func pick(value map[string]any, key string) (any, bool) {
	target := normalizeKey(key)
	for rawKey, item := range value {
		if normalizeKey(rawKey) == target {
			return item, true
		}
	}
	return nil, false
}

func pickString(value map[string]any, key string) string {
	item, ok := pick(value, key)
	if !ok || item == nil {
		return ""
	}
	if text, ok := item.(string); ok {
		return text
	}
	return fmt.Sprint(item)
}

func pickBool(value map[string]any, key string) bool {
	item, ok := pick(value, key)
	if !ok || item == nil {
		return false
	}
	if flag, ok := item.(bool); ok {
		return flag
	}
	if text, ok := item.(string); ok {
		return text == "true" || text == "1"
	}
	return false
}

func pickInt(value map[string]any, key string) int {
	item, ok := pick(value, key)
	if !ok || item == nil {
		return 0
	}
	switch typed := item.(type) {
	case int:
		return typed
	case int64:
		return int(typed)
	case float64:
		return int(typed)
	case string:
		parsed := 0
		_, _ = fmt.Sscanf(typed, "%d", &parsed)
		return parsed
	}
	return 0
}

func pickStringMap(value map[string]any, key string) map[string]string {
	nested := pickMap(value, key)
	if nested == nil {
		return nil
	}
	result := make(map[string]string, len(nested))
	names := make([]string, 0, len(nested))
	for name := range nested {
		names = append(names, name)
	}
	sort.Strings(names)
	for _, name := range names {
		if nested[name] == nil {
			result[name] = ""
			continue
		}
		result[name] = fmt.Sprint(nested[name])
	}
	return result
}

func pickStringSlice(value map[string]any, key string) []string {
	item, ok := pick(value, key)
	if !ok || item == nil {
		return nil
	}
	items, ok := item.([]any)
	if !ok {
		return nil
	}
	result := make([]string, 0, len(items))
	for _, entry := range items {
		result = append(result, fmt.Sprint(entry))
	}
	return result
}

func stringField(value map[string]any, key string) string {
	if value == nil {
		return ""
	}
	if text, ok := value[key].(string); ok {
		return text
	}
	return ""
}
