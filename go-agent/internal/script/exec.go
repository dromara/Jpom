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
	"bufio"
	"bytes"
	"encoding/json"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"sync"

	"jpom.io/go-agent/internal/i18n"
)

const (
	systemInfoPrefix    = "[SYSTEM-INFO] "
	systemErrorPrefix   = "[SYSTEM-ERROR] "
	systemWarningPrefix = "[SYSTEM-WARNING] "
)

// LineListener receives one console line of a running execution.
type LineListener func(line string) error

// Execution is one running script execution (data/script/<id>/log/<executeId>.log).
type Execution struct {
	ID         string
	ScriptID   string
	ScriptName string

	logFile    string
	scriptFile string
	env        *EnvBuilder
	lang       string
	charset    string

	pipe     *os.File
	writeEnd *os.File

	mu        sync.Mutex
	log       *os.File
	listeners map[string]LineListener
	cmd       *exec.Cmd
	finished  bool
	stopped   bool
}

// Append writes one log line and forwards it to every attached session.
func (e *Execution) append(line string) {
	e.mu.Lock()
	writer := e.log
	if e.finished {
		e.mu.Unlock()
		return
	}
	if writer != nil {
		if _, err := writer.WriteString(line + "\n"); err != nil {
			_ = err
		}
	}
	listeners := make(map[string]LineListener, len(e.listeners))
	for id, listener := range e.listeners {
		listeners[id] = listener
	}
	e.mu.Unlock()
	for id, listener := range listeners {
		if err := listener(line); err != nil {
			e.removeListener(id)
		}
	}
}

// info writes a plain console line.
func (e *Execution) info(line string) { e.append(line) }

// system writes a [SYSTEM-INFO] line.
func (e *Execution) system(message string) { e.append(systemInfoPrefix + message) }

// systemError writes a [SYSTEM-ERROR] line.
func (e *Execution) systemError(message string) { e.append(systemErrorPrefix + message) }

// addListener registers a console session; the listener receives subsequent
// lines. Existing log lines are returned by the Executor before registering.
func (e *Execution) addListener(id string, listener LineListener) {
	e.mu.Lock()
	defer e.mu.Unlock()
	e.listeners[id] = listener
}

// removeListener removes a console session.
func (e *Execution) removeListener(id string) {
	e.mu.Lock()
	defer e.mu.Unlock()
	delete(e.listeners, id)
}

// end sends message to every attached session and removes them (Java
// BaseRunScript#end / NodeScriptProcessBuilder#end).
func (e *Execution) end(message string) {
	e.mu.Lock()
	listeners := e.listeners
	e.listeners = make(map[string]LineListener)
	e.mu.Unlock()
	for id, listener := range listeners {
		if err := listener(message); err != nil {
			_ = id
		}
	}
}

// kill terminates the process of the execution.
func (e *Execution) kill() {
	e.mu.Lock()
	e.stopped = true
	cmd := e.cmd
	e.mu.Unlock()
	if cmd != nil && cmd.Process != nil {
		_ = cmd.Process.Kill()
	}
}

// Executor runs script templates asynchronously and caches the running
// executions by execution id (NodeScriptProcessBuilder static cache).
type Executor struct {
	manager *Manager

	mu      sync.Mutex
	running map[string]*Execution
}

// NewExecutor creates the script executor.
func NewExecutor(manager *Manager) *Executor {
	return &Executor{manager: manager, running: make(map[string]*Execution)}
}

// Manager returns the owning script manager.
func (e *Executor) Manager() *Manager { return e.manager }

// Start begins a new execution. args is the effective argument JSON (the caller
// applies the defArgs fallback), params are the trigger environment variables and
// lang selects the localized console messages (zh-CN default, en-US supported).
func (e *Executor) Start(item map[string]any, executeID, args string, params map[string]string, lang string) error {
	_, err := e.getOrCreate(item, executeID, args, params, lang)
	return err
}

// Attach starts the execution when it is not running yet and streams the
// existing log lines to the listener, mirroring
// NodeScriptProcessBuilder.addWatcher.
func (e *Executor) Attach(item map[string]any, executeID, args string, sessionID string, send LineListener, lang string) error {
	execution, err := e.getOrCreate(item, executeID, args, nil, lang)
	if err != nil {
		return err
	}
	lines, err := ReadLogLines(execution.logFile)
	if err != nil {
		return err
	}
	execution.addListener(sessionID, send)
	for _, line := range lines {
		if err := send(line); err != nil {
			execution.removeListener(sessionID)
			return err
		}
	}
	return nil
}

// Detach removes a console session from every execution.
func (e *Executor) Detach(sessionID string) {
	e.mu.Lock()
	executions := make([]*Execution, 0, len(e.running))
	for _, execution := range e.running {
		executions = append(executions, execution)
	}
	e.mu.Unlock()
	for _, execution := range executions {
		execution.removeListener(sessionID)
	}
}

// Stop stops a running execution by id; it reports whether the execution was
// running.
func (e *Executor) Stop(executeID string) bool {
	e.mu.Lock()
	execution := e.running[executeID]
	e.mu.Unlock()
	if execution == nil {
		return false
	}
	execution.end(i18n.T(execution.lang, "i18n.stop_running.1d4e"))
	execution.kill()
	return true
}

// IsRun reports whether executeID is still running.
func (e *Executor) IsRun(executeID string) bool {
	e.mu.Lock()
	defer e.mu.Unlock()
	_, ok := e.running[executeID]
	return ok
}

// IsScriptRunning reports whether any execution of scriptID is running.
func (e *Executor) IsScriptRunning(scriptID string) bool {
	e.mu.Lock()
	defer e.mu.Unlock()
	for _, execution := range e.running {
		if execution.ScriptID == scriptID {
			return true
		}
	}
	return false
}

// CloseAll terminates every running execution (agent shutdown).
func (e *Executor) CloseAll() {
	e.mu.Lock()
	executions := make([]*Execution, 0, len(e.running))
	for _, execution := range e.running {
		executions = append(executions, execution)
	}
	e.mu.Unlock()
	for _, execution := range executions {
		execution.kill()
	}
}

// getOrCreate returns the running execution, starting it when needed.
func (e *Executor) getOrCreate(item map[string]any, executeID, args string, params map[string]string, lang string) (*Execution, error) {
	e.mu.Lock()
	if execution, ok := e.running[executeID]; ok {
		e.mu.Unlock()
		return execution, nil
	}
	e.mu.Unlock()
	execution, err := e.newExecution(item, executeID, args, params, lang)
	if err != nil {
		return nil, err
	}
	e.mu.Lock()
	if existing, ok := e.running[executeID]; ok {
		e.mu.Unlock()
		_ = os.Remove(execution.scriptFile)
		_ = execution.log.Close()
		return existing, nil
	}
	e.running[executeID] = execution
	e.mu.Unlock()
	go e.run(execution)
	return execution, nil
}

// newExecution prepares the temporary script file, the log file and the command
// line, mirroring NodeScriptProcessBuilder's constructor.
func (e *Executor) newExecution(item map[string]any, executeID, args string, params map[string]string, lang string) (*Execution, error) {
	scriptID := stringField(item, "id")
	scriptPath, err := e.manager.WriteScriptFile(stringField(item, "context"))
	if err != nil {
		return nil, err
	}
	logFile := e.manager.LogFile(scriptID, executeID)
	if err := os.MkdirAll(filepath.Dir(logFile), 0o755); err != nil {
		_ = os.Remove(scriptPath)
		return nil, err
	}
	log, err := os.OpenFile(logFile, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o644)
	if err != nil {
		_ = os.Remove(scriptPath)
		return nil, err
	}
	workspaceID := stringField(item, "workspaceId")
	if workspaceID == "" {
		workspaceID = "DEFAULT"
	}
	environment, err := e.manager.WorkspaceEnv(workspaceID)
	if err != nil {
		_ = log.Close()
		_ = os.Remove(scriptPath)
		return nil, err
	}
	environment.PutStr(params)
	command := buildCommand(scriptPath, args)
	execution := &Execution{
		ID:         executeID,
		ScriptID:   scriptID,
		ScriptName: stringField(item, "name"),
		logFile:    logFile,
		scriptFile: scriptPath,
		env:        environment,
		lang:       lang,
		charset:    e.manager.ConsoleCharset(),
		log:        log,
		listeners:  make(map[string]LineListener),
	}
	cmd := exec.Command(command[0], command[1:]...)
	cmd.Dir = filepath.Dir(scriptPath)
	cmd.Env = mergeEnvironment(environment.Environment())
	readEnd, writeEnd, err := os.Pipe()
	if err != nil {
		_ = log.Close()
		_ = os.Remove(scriptPath)
		return nil, err
	}
	cmd.Stdout = writeEnd
	cmd.Stderr = writeEnd
	execution.cmd = cmd
	execution.pipe = readEnd
	execution.writeEnd = writeEnd
	return execution, nil
}

// buildCommand builds the process command line: the script path followed by the
// parsed arguments, executed with bash on unix (CommandUtil.paddingPrefix).
func buildCommand(scriptPath, args string) []string {
	arguments := ToCommandList(args)
	command := make([]string, 0, len(arguments)+2)
	if runtime.GOOS == "windows" {
		command = append(command, "cmd", "/c", scriptPath)
	} else {
		command = append(command, "bash", scriptPath)
	}
	command = append(command, arguments...)
	return command
}

// mergeEnvironment overlays values on top of the inherited process environment.
func mergeEnvironment(values map[string]string) []string {
	inherited := os.Environ()
	result := make([]string, 0, len(inherited)+len(values))
	replaced := make(map[string]string, len(values))
	for name, value := range values {
		replaced[strings.ToUpper(name)] = value
	}
	for _, item := range inherited {
		name, _, found := strings.Cut(item, "=")
		if found {
			if _, ok := replaced[strings.ToUpper(name)]; ok {
				continue
			}
		}
		result = append(result, item)
	}
	for name, value := range values {
		result = append(result, name+"="+value)
	}
	return result
}

// run executes the process, writes the console log and notifies the sessions.
func (e *Executor) run(execution *Execution) {
	defer e.finish(execution)
	// 输出环境变量（隐私变量以 ****** 显示）
	execution.env.EachStr(execution.info)
	if err := execution.cmd.Start(); err != nil {
		message := i18n.T(execution.lang, "i18n.general_execution_exception.62e9") + err.Error()
		execution.systemError(message)
		execution.end(message)
		return
	}
	// 父进程关闭写端，子进程退出后读端才会 EOF
	_ = execution.writeEnd.Close()
	reader := bufio.NewReaderSize(execution.pipe, 64*1024)
	for {
		line, err := reader.ReadString('\n')
		if len(line) > 0 {
			// 进程输出按 console-charset 解码（Windows 默认 GBK）
			execution.info(DecodeLine(trimLineBytes([]byte(line)), execution.charset))
		}
		if err != nil {
			break
		}
	}
	_ = execution.pipe.Close()
	waitErr := execution.cmd.Wait()
	exitCode := processExitCode(waitErr)
	execution.system(i18n.T(execution.lang, "i18n.execution_ended.b793", exitCode))
	// 通知前端执行结束（JsonMessage + JPOM_MSG + op）
	completion := map[string]any{
		"code":     200,
		"msg":      i18n.T(execution.lang, "i18n.execution_completed.24a1") + strconv.Itoa(exitCode),
		"JPOM_MSG": "JPOM_MSG",
		"op":       "stop",
	}
	encoded, err := json.Marshal(completion)
	if err != nil {
		execution.end(i18n.T(execution.lang, "i18n.execution_completed.24a1"))
		return
	}
	execution.end(string(encoded))
}

// finish cleans the execution cache and the temporary script file.
func (e *Executor) finish(execution *Execution) {
	execution.mu.Lock()
	execution.finished = true
	writer := execution.log
	execution.log = nil
	execution.mu.Unlock()
	if writer != nil {
		_ = writer.Close()
	}
	_ = os.Remove(execution.scriptFile)
	e.mu.Lock()
	if current, ok := e.running[execution.ID]; ok && current == execution {
		delete(e.running, execution.ID)
	}
	e.mu.Unlock()
}

// BuildCommand is the exported form of buildCommand, shared with the DSL and
// free-script runners.
func BuildCommand(scriptPath, args string) []string { return buildCommand(scriptPath, args) }

// MergeEnvironment is the exported form of mergeEnvironment.
func MergeEnvironment(values map[string]string) []string { return mergeEnvironment(values) }

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

// ReadLogLines reads every line of an execution log file.
func ReadLogLines(path string) ([]string, error) {
	content, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, nil
		}
		return nil, err
	}
	trimmed := strings.TrimSuffix(string(content), "\n")
	if trimmed == "" {
		return nil, nil
	}
	return strings.Split(trimmed, "\n"), nil
}

// ReadLogFile mirrors FileUtils.readLogFile: it returns the current line count,
// the requested start line and at most 1000 lines beginning at fromLine (1
// based). The line count is the total number of lines in the file.
func ReadLogFile(path string, fromLine int) (total int, lines []string, err error) {
	content, err := os.ReadFile(path)
	if err != nil {
		return 0, nil, err
	}
	text := string(content)
	if text == "" {
		return 0, []string{}, nil
	}
	text = strings.TrimSuffix(text, "\n")
	all := strings.Split(text, "\n")
	total = len(all)
	if fromLine < 0 {
		fromLine = 1
	}
	if fromLine == 0 {
		fromLine = 1
	}
	start := fromLine - 1
	if start >= total {
		return total, []string{}, nil
	}
	lines = all[start:]
	if len(lines) > 1000 {
		lines = lines[len(lines)-1000:]
	}
	return total, lines, nil
}
