/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package ws

import (
	"bufio"
	"bytes"
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strings"
	"sync"

	"github.com/gorilla/websocket"

	"jpom.io/go-agent/internal/httpx"
	"jpom.io/go-agent/internal/script"
)

// freeScriptProcess is one /free-script-run execution
// (AgentFreeWebSocketScriptHandle.ScriptProcess).
type freeScriptProcess struct {
	mu         sync.Mutex
	cmd        *exec.Cmd
	pipe       *os.File
	writeEnd   *os.File
	scriptFile string
	env        *script.EnvBuilder
	charset    string
	manager    *Manager
	done       bool
}

// close kills the process and removes the temporary script file. It is
// idempotent (socket close and the "close" command may race).
func (p *freeScriptProcess) close() {
	p.mu.Lock()
	if p.done {
		p.mu.Unlock()
		return
	}
	p.done = true
	cmd := p.cmd
	pipe := p.pipe
	writeEnd := p.writeEnd
	scriptFile := p.scriptFile
	p.mu.Unlock()
	if cmd != nil && cmd.Process != nil {
		_ = cmd.Process.Kill()
	}
	if writeEnd != nil {
		_ = writeEnd.Close()
	}
	if pipe != nil {
		_ = pipe.Close()
	}
	if scriptFile != "" {
		_ = os.Remove(scriptFile)
	}
}

// markDone marks the process finished without killing it.
func (p *freeScriptProcess) markDone() {
	p.mu.Lock()
	p.done = true
	p.mu.Unlock()
}

// HandleFreeScriptRun serves /free-script-run
// (AgentFreeWebSocketScriptHandle): arbitrary script content execution streamed
// line by line.
func (m *Manager) HandleFreeScriptRun(c *httpx.Context) {
	session, err := upgrade(c)
	if err != nil {
		return
	}
	defer session.Close()
	if !m.authorized(session, c.R) {
		return
	}
	session.SetReadLimit(defaultTextLimit)
	_ = session.Send(m.translate(session, "i18n.connection_successful.b331"))

	var (
		currentMu sync.Mutex
		current   *freeScriptProcess
	)
	setCurrent := func(process *freeScriptProcess) {
		currentMu.Lock()
		current = process
		currentMu.Unlock()
	}
	getCurrent := func() *freeScriptProcess {
		currentMu.Lock()
		defer currentMu.Unlock()
		return current
	}
	defer func() {
		if process := getCurrent(); process != nil {
			process.close()
		}
	}()

	for {
		messageType, data, err := session.Read()
		if err != nil {
			return
		}
		if messageType != websocket.TextMessage {
			continue
		}
		request, err := decodeObject(data)
		if err != nil {
			continue
		}
		if bodyString(request, "type") == "close" {
			if process := getCurrent(); process != nil {
				process.close()
				setCurrent(nil)
			}
			session.Close()
			return
		}
		if getCurrent() != nil {
			_ = session.SendJSON(jsonMessage(500, m.translate(session, "i18n.do_not_reopen.f86a")))
			continue
		}
		path := bodyString(request, "path")
		tag := bodyString(request, "tag")
		content := bodyString(request, "content")
		if path == "" || tag == "" || content == "" {
			_ = session.SendJSON(jsonMessage(500, m.translate(session, "i18n.incorrect_parameter.02ce")))
			continue
		}
		environment, ok := request["environment"].(map[string]any)
		if !ok {
			_ = session.SendJSON(jsonMessage(500, m.translate(session, "i18n.environment_variables_not_found.dbd4")))
			continue
		}
		process, err := m.prepareFreeScript(content, path, environment)
		if err != nil {
			_ = session.Send(m.translate(session, "i18n.general_execution_exception.62e9") + err.Error())
			_ = session.Send(m.translate(session, "i18n.execution_ended.b793", -1))
			_ = session.Send("JPOM_SYSTEM_TAG:" + tag)
			continue
		}
		setCurrent(process)
		go process.run(session, tag)
	}
}

// prepareFreeScript writes the script file and builds the command/environment.
func (m *Manager) prepareFreeScript(content, path string, environment map[string]any) (*freeScriptProcess, error) {
	scriptFile, err := m.Scripts.WriteRawScriptFile(content)
	if err != nil {
		return nil, err
	}
	builder := script.NewEnvBuilder()
	names := make([]string, 0, len(environment))
	for name := range environment {
		names = append(names, name)
	}
	sort.Strings(names)
	for _, name := range names {
		item, ok := environment[name].(map[string]any)
		if !ok {
			continue
		}
		value := bodyString(item, "value")
		if bodyBool(item, "privacy") {
			builder.PutSystem(name, value)
		} else {
			builder.Put(name, value)
		}
	}
	command := script.BuildCommand(scriptFile, "")
	cmd := exec.Command(command[0], command[1:]...)
	directory := path
	if !filepath.IsAbs(directory) {
		if absolute, err := filepath.Abs(directory); err == nil {
			directory = absolute
		}
	}
	if err := os.MkdirAll(directory, 0o755); err != nil {
		_ = os.Remove(scriptFile)
		return nil, err
	}
	cmd.Dir = directory
	cmd.Env = script.MergeEnvironment(builder.Environment())
	readEnd, writeEnd, err := os.Pipe()
	if err != nil {
		_ = os.Remove(scriptFile)
		return nil, err
	}
	cmd.Stdout = writeEnd
	cmd.Stderr = writeEnd
	return &freeScriptProcess{cmd: cmd, pipe: readEnd, writeEnd: writeEnd, scriptFile: scriptFile, env: builder, charset: m.Scripts.ConsoleCharset(), manager: m}, nil
}

// run executes the process and streams the console to the session.
func (p *freeScriptProcess) run(session *Session, tag string) {
	defer p.close()
	p.env.EachStr(func(line string) { _ = session.Send(line) })
	if err := p.cmd.Start(); err != nil {
		_ = session.Send(p.manager.translate(session, "i18n.general_execution_exception.62e9") + err.Error())
		_ = session.Send(p.manager.translate(session, "i18n.execution_ended.b793", -1))
		_ = session.Send("JPOM_SYSTEM_TAG:" + tag)
		return
	}
	_ = p.writeEnd.Close()
	reader := bufio.NewReaderSize(p.pipe, 64*1024)
	for {
		line, err := reader.ReadString('\n')
		if len(line) > 0 {
			_ = session.Send(script.DecodeLine(trimLineBytes([]byte(line)), p.charset))
		}
		if err != nil {
			break
		}
	}
	_ = p.pipe.Close()
	waitErr := p.cmd.Wait()
	_ = session.Send(p.manager.translate(session, "i18n.execution_ended.b793", processExitCode(waitErr)))
	// Server 收到该标记后关闭会话
	_ = session.Send("JPOM_SYSTEM_TAG:" + tag)
	p.markDone()
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
