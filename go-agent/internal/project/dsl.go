/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package project

import (
	"encoding/json"
	"fmt"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"time"

	"gopkg.in/yaml.v3"
	"jpom.io/go-agent/internal/proc"
)

// DslExecutor DSL 流程执行器。脚本模块（script-ws）可通过 RegisterDslExecutor 注册，
// 由其执行 DSL 流程（带执行记录/日志等增强能力）；未注册时使用内置实现。
type DslExecutor interface {
	// Run 执行流程（sync 为 true 时等待执行结束）。
	Run(r *Runtime, info, original map[string]any, op string, sync bool) error
	// SyncRun 同步执行流程，返回退出码与带格式的日志行。
	SyncRun(r *Runtime, info, original map[string]any, op string) (int, []string, error)
}

var (
	dslMu       sync.RWMutex
	dslExecutor DslExecutor
)

// RegisterDslExecutor 注册 DSL 执行器。
func RegisterDslExecutor(executor DslExecutor) {
	dslMu.Lock()
	defer dslMu.Unlock()
	dslExecutor = executor
}

func currentDslExecutor() DslExecutor {
	dslMu.RLock()
	defer dslMu.RUnlock()
	return dslExecutor
}

// parseDslConfig 解析项目原始数据的 dslContent YAML（等价 Java DslYmlDto.build）。
func parseDslConfig(original map[string]any) (map[string]any, error) {
	content := GetString(original, "dslContent")
	if strings.TrimSpace(content) == "" {
		return nil, ValidationKey(keyDslInfoNotConfigured)
	}
	return parseDslContent(content)
}

// parseDslContent 解析 dsl YAML 内容。
func parseDslContent(content string) (map[string]any, error) {
	var config map[string]any
	if err := yaml.Unmarshal([]byte(content), &config); err != nil {
		return nil, Servicef("parse dsl config failed: %v", err)
	}
	return config, nil
}

// yamlMap 把任意值转为 map[string]any。
func yamlMap(value any) map[string]any {
	switch data := value.(type) {
	case map[string]any:
		return data
	case map[any]any:
		result := make(map[string]any, len(data))
		for key, item := range data {
			result[fmt.Sprint(key)] = item
		}
		return result
	default:
		return nil
	}
}

// dslProcess 读取 run.<op> 流程配置。
func dslProcess(config map[string]any, op string) map[string]any {
	if config == nil {
		return nil
	}
	run := yamlMap(config["run"])
	if run == nil {
		return nil
	}
	return yamlMap(run[op])
}

// dslHasProcess 判断是否配置了指定流程（等价 DslYmlDto.hasRunProcess）。
func dslHasProcess(config map[string]any, op string) bool {
	return dslProcess(config, op) != nil
}

// dslExecPath 读取 run.execPath。
func dslExecPath(config map[string]any) string {
	run := yamlMap(config["run"])
	return GetString(run, "execPath")
}

// dslFileChangeReload 读取 run.fileChangeReload。
func dslFileChangeReload(config map[string]any) bool {
	run := yamlMap(config["run"])
	return GetBool(run, "fileChangeReload")
}

// dslAutoBackToFile 读取 config.autoBackToFile（未配置返回 ok=false）。
func dslAutoBackToFile(config map[string]any) (bool, bool) {
	cfg := yamlMap(config["config"])
	if cfg == nil {
		return false, false
	}
	if _, ok := cfg["autoBackToFile"]; !ok {
		return false, false
	}
	return GetBool(cfg, "autoBackToFile"), true
}

// resolvedDslScript 解析后的流程脚本。
type resolvedDslScript struct {
	Kind string // file/script/library
	Data string // script/library 的脚本内容
	Path string // file 类型脚本路径
}

// resolveDslScript 解析流程脚本（等价 DslScriptServer.resolveProcessScript）。
//
// 返回值：响应 JSON（status/msg/type/scriptId）、脚本内容、错误消息。
func (r *Runtime) resolveDslScript(info map[string]any, process map[string]any) (map[string]any, resolvedDslScript, error) {
	lang := r.Lang()
	result := map[string]any{"status": false}
	if process == nil {
		err := ValidationKey(keyProcessNotExist)
		_, msg := LocalizeError(lang, err)
		result["msg"] = msg
		return result, resolvedDslScript{}, err
	}
	scriptID := GetString(process, "scriptId")
	if scriptID == "" {
		err := ValidationKey(keyScriptTemplateIDNeeded)
		_, msg := LocalizeError(lang, err)
		result["msg"] = msg
		return result, resolvedDslScript{}, err
	}
	if strings.HasPrefix(strings.ToLower(scriptID), "g@") {
		libraryID := scriptID[2:]
		content, err := os.ReadFile(filepath.Join(r.Options.BaseDir, "data", "global-script", libraryID+".json"))
		if err == nil {
			var library map[string]any
			if json.Unmarshal(content, &library) == nil {
				result["status"] = true
				result["type"] = "library"
				result["scriptId"] = libraryID
				return result, resolvedDslScript{Kind: "library", Data: GetString(library, "script")}, nil
			}
		}
		scriptErr := ValidationKeySuffix(keyScriptLibraryNotExist, libraryID)
		_, msg := LocalizeError(lang, scriptErr)
		result["msg"] = msg
		return result, resolvedDslScript{}, scriptErr
	}
	// 节点脚本
	if scriptContent, ok := r.loadNodeScript(scriptID); ok {
		result["status"] = true
		result["type"] = "script"
		result["scriptId"] = scriptID
		return result, resolvedDslScript{Kind: "script", Data: scriptContent}, nil
	}
	// 项目目录下的脚本文件
	lib, err := r.Manager.ResolveLibPath(info)
	if err == nil {
		scriptFile := filepath.Join(lib, scriptID)
		if stat, err := os.Stat(scriptFile); err == nil && !stat.IsDir() {
			result["status"] = true
			result["type"] = "file"
			result["scriptId"] = scriptID
			return result, resolvedDslScript{Kind: "file", Path: scriptFile}, nil
		}
	}
	scriptErr := ValidationKeySuffix(keyScriptTemplateNotExist, scriptID)
	_, msg := LocalizeError(lang, scriptErr)
	result["msg"] = msg
	return result, resolvedDslScript{}, scriptErr
}

// loadNodeScript 从 data/script.json 读取节点脚本内容。
func (r *Runtime) loadNodeScript(scriptID string) (string, bool) {
	content, err := os.ReadFile(filepath.Join(r.Options.BaseDir, "data", "script.json"))
	if err != nil || len(content) == 0 {
		return "", false
	}
	var all map[string]map[string]any
	if err := json.Unmarshal(content, &all); err != nil {
		return "", false
	}
	item, ok := all[scriptID]
	if !ok || item == nil {
		return "", false
	}
	return GetString(item, "context"), true
}

// DslProcessInfo 返回 DSL 流程信息（供 /manage/getProjectItem 使用）。
func (r *Runtime) DslProcessInfo(info map[string]any) (bool, []map[string]any, error) {
	original, err := r.Manager.ResolveModel(info)
	if err != nil {
		return false, nil, err
	}
	config, err := parseDslConfig(original)
	if err != nil {
		return false, nil, err
	}
	canReload := dslHasProcess(config, OpReload)
	list := make([]map[string]any, 0, 4)
	for _, op := range []string{OpStart, OpStop, OpRestart, OpReload} {
		result, _, _ := r.resolveDslScript(info, dslProcess(config, op))
		result["process"] = op
		list = append(list, result)
	}
	return canReload, list, nil
}

// dslStatus 执行 DSL 的 status 流程并解析运行状态。
func (r *Runtime) dslStatus(info, original map[string]any) (*OpResult, error) {
	lang := r.Lang()
	// mustDslConfig 在 Java 中位于 try 之外，配置缺失直接 405
	if _, err := parseDslConfig(original); err != nil {
		return nil, err
	}
	_, lines, err := r.dslSyncRun(info, original, OpStatus)
	if err != nil {
		kind, msg := LocalizeError(lang, err)
		if kind != ErrValidation {
			return nil, err
		}
		logFile, logErr := r.Manager.TouchLogFile(info)
		if logErr == nil {
			_ = appendLogLines(logFile, []string{msg}, r.ResolveLogCharset(info))
		}
		return OpResultOfMsg(msg), nil
	}
	logFile, logErr := r.Manager.TouchLogFile(info)
	if logErr == nil && len(lines) > 0 {
		_ = appendLogLines(logFile, lines, r.ResolveLogCharset(info))
	}
	if len(lines) == 0 {
		return OpResultSimple(false, proc.StoppedTag), nil
	}
	last := lines[len(lines)-1]
	segments := splitTrim(last, "-")
	value := last
	if len(segments) > 0 {
		value = segments[len(segments)-1]
	}
	return OpResultOfMsg(value), nil
}

// dslRun 执行 DSL 流程并写入控制台日志（等价 DslScriptServer.run）。
func (r *Runtime) dslRun(info, original map[string]any, op string, sync bool) error {
	if executor := currentDslExecutor(); executor != nil {
		return executor.Run(r, info, original, op, sync)
	}
	return r.dslRunBuiltin(info, original, op, sync)
}

// dslSyncRun 同步执行 DSL 流程（等价 DslScriptServer.syncRun）。
func (r *Runtime) dslSyncRun(info, original map[string]any, op string) (int, []string, error) {
	if executor := currentDslExecutor(); executor != nil {
		return executor.SyncRun(r, info, original, op)
	}
	return r.dslExecute(info, original, op, false)
}

// dslRunBuiltin 内置实现：异步执行并写入控制台日志。
func (r *Runtime) dslRunBuiltin(info, original map[string]any, op string, sync bool) error {
	_, _, err := r.dslExecute(info, original, op, true)
	return err
}

// dslExecute 执行 DSL 流程。
//
// writeLog 为 true 时把执行过程写入项目控制台日志（异步执行），返回带格式的日志行。
func (r *Runtime) dslExecute(info, original map[string]any, op string, writeLog bool) (int, []string, error) {
	config, err := parseDslConfig(original)
	if err != nil {
		return -1, nil, err
	}
	process := dslProcess(config, op)
	if process == nil {
		return -1, nil, ValidationKey(keyDslNotConfigured, op)
	}
	result, script, resolveErr := r.resolveDslScript(info, process)
	if resolveErr != nil {
		return -1, nil, resolveErr
	}
	if !GetBool(result, "status") && result["status"] != true {
		return -1, nil, ValidationKey(keyProcessNotExist)
	}
	logFile, err := r.Manager.TouchLogFile(info)
	if err != nil {
		return -1, nil, err
	}
	env, projectPath := r.dslEnvironment(info, process)
	env["PROJECT_LOG_FILE"] = logFile
	execPath := dslExecPath(config)
	env["JPOM_EXEC_PATH"] = execPath

	scriptFile := script.Path
	autoDelete := false
	if script.Kind != "file" {
		cacheDir := filepath.Join(r.Options.BaseDir, "data", "script_run_cache")
		if err := os.MkdirAll(cacheDir, 0o755); err != nil {
			return -1, nil, err
		}
		suffix := "sh"
		if runtime.GOOS == "windows" {
			suffix = "bat"
		}
		scriptFile = filepath.Join(cacheDir, fmt.Sprintf("%d.%s", time.Now().UnixNano(), suffix))
		content := strings.ReplaceAll(script.Data, "\n", lineSeparator())
		if err := os.WriteFile(scriptFile, []byte(content), 0o755); err != nil {
			return -1, nil, err
		}
		autoDelete = true
	}
	if autoDelete {
		defer func() { _ = os.Remove(scriptFile) }()
	}

	dir := filepath.Dir(scriptFile)
	if execPath != "" && projectPath != "" {
		if filepath.IsAbs(execPath) {
			dir = projectPath
		} else {
			dir = filepath.Join(projectPath, execPath)
		}
	}
	args := strings.Fields(GetString(process, "scriptArgs"))
	command := make([]string, 0, len(args)+2)
	if runtime.GOOS == "windows" {
		command = append(command, scriptFile)
	} else {
		command = append(command, "bash", scriptFile)
	}
	command = append(command, args...)

	cmd := exec.Command(command[0], command[1:]...)
	cmd.Dir = dir
	cmd.Env = proc.MergeEnv(env)
	var builder strings.Builder
	cmd.Stdout = &builder
	cmd.Stderr = &builder
	runErr := cmd.Run()
	code := 0
	if runErr != nil {
		if exitErr, ok := runErr.(*exec.ExitError); ok {
			code = exitErr.ExitCode()
		} else {
			code = -1
		}
	}
	outputText, decodeErr := proc.DecodeString(r.ConsoleCharset(), []byte(builder.String()))
	if decodeErr != nil {
		outputText = builder.String()
	}
	outputLines := splitLines(outputText)
	formatted := make([]string, 0, len(outputLines)+1)
	for _, line := range outputLines {
		formatted = append(formatted, formatDslLine(op, line))
	}
	lang := r.Lang()
	if writeLog {
		logLines := make([]string, 0, len(formatted)+8)
		logLines = append(logLines, "##################################################################################")
		keys := sortedKeys(env)
		for _, key := range keys {
			logLines = append(logLines, key+"="+env[key])
		}
		logLines = append(logLines, "##################################################################################")
		logLines = append(logLines, "[SYSTEM-INFO] "+tr(lang, keyStartExecuting, op))
		logLines = append(logLines, formatted...)
		logLines = append(logLines, "[SYSTEM-INFO] "+tr(lang, keyExecutionEndedDetail, op, code))
		_ = appendLogLines(logFile, logLines, r.ResolveLogCharset(info))
	} else {
		// 同步执行：首行插入退出码（与 Java DslScriptBuilder.syncExecute 一致）
		formatted = append([]string{formatDslLine(op, tr(lang, keyExitCode, code))}, formatted...)
	}
	if runErr != nil && code < 0 {
		return code, formatted, ServiceError(runErr)
	}
	return code, formatted, nil
}

// dslEnvironment 构建 DSL 执行环境变量（等价 DslScriptServer.environment）。
func (r *Runtime) dslEnvironment(info map[string]any, process map[string]any) (map[string]string, string) {
	env := loadWorkspaceEnv(r.Options.BaseDir, GetString(info, "workspaceId"))
	// 项目配置的 dslEnv（URL query 形式）
	if dslEnv := GetString(info, "dslEnv"); dslEnv != "" {
		if values, err := url.ParseQuery(dslEnv); err == nil {
			for key, items := range values {
				if len(items) > 0 {
					env[key] = items[len(items)-1]
				}
			}
		}
	}
	// 流程配置的 scriptEnv
	if scriptEnv := yamlMap(process["scriptEnv"]); scriptEnv != nil {
		for key, value := range scriptEnv {
			env[key] = fmt.Sprint(value)
		}
	}
	projectPath, _ := r.Manager.ResolveLibPath(info)
	env["PROJECT_ID"] = GetString(info, "id")
	env["PROJECT_NAME"] = GetString(info, "name")
	env["PROJECT_PATH"] = projectPath
	return env, projectPath
}

// formatDslLine 生成 DSL 日志行（等价 DslScriptBuilder.formatLine）。
func formatDslLine(action, line string) string {
	return fmt.Sprintf("%s [%s] - %s", time.Now().Format("2006-01-02 15:04:05.000"), action, line)
}

// appendLogLines 追加写入日志行。
func appendLogLines(path string, lines []string, charset string) error {
	file, err := os.OpenFile(path, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o644)
	if err != nil {
		return err
	}
	defer func() { _ = file.Close() }()
	var builder strings.Builder
	for _, line := range lines {
		builder.WriteString(line)
		builder.WriteString("\n")
	}
	data, err := proc.EncodeString(charset, builder.String())
	if err != nil {
		return err
	}
	_, err = file.Write(data)
	return err
}

// splitLines 按行拆分输出。
func splitLines(output string) []string {
	output = strings.ReplaceAll(output, "\r\n", "\n")
	if output == "" {
		return nil
	}
	lines := strings.Split(output, "\n")
	if last := len(lines) - 1; last >= 0 && lines[last] == "" {
		lines = lines[:last]
	}
	return lines
}

// lineSeparator 返回系统换行符。
func lineSeparator() string {
	if runtime.GOOS == "windows" {
		return "\r\n"
	}
	return "\n"
}

// sortedKeys 返回 map 的稳定排序 key。
func sortedKeys(values map[string]string) []string {
	keys := make([]string, 0, len(values))
	for key := range values {
		keys = append(keys, key)
	}
	sortStrings(keys)
	return keys
}

func sortStrings(values []string) {
	for i := 1; i < len(values); i++ {
		for j := i; j > 0 && values[j] < values[j-1]; j-- {
			values[j], values[j-1] = values[j-1], values[j]
		}
	}
}
