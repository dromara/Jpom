/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// Package app owns the agent runtime: directories, manifest, authorization and logging.
package app

import (
	"crypto/rand"
	"crypto/sha1"
	"crypto/subtle"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"log/slog"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"time"

	"jpom.io/go-agent/internal/config"
	"jpom.io/go-agent/internal/i18n"
)

const (
	dirData           = "data"
	dirLogs           = "logs"
	dirConf           = "conf"
	dirTmp            = "tmp"
	dirProjectLog     = "project-log"
	dirScript         = "script"
	dirGlobalScript   = "global-script"
	dirScriptRunCache = "script_run_cache"
	dirTemp           = "temp"

	fileInstall         = "INSTALL.json"
	fileAgentAuthorize  = "agent_authorize.json"
	fileProject         = "project.json"
	fileScript          = "script.json"
	fileWhitelist       = "whitelistDirectory.json"
	fileWorkspaceEnvVar = "workspace_env_var.json"
	fileRemoteVersion   = "remote_version.json"
)

// Auth is the effective agent authorization (name + password + sha1 digest).
type Auth struct {
	Name      string
	Pwd       string
	Authorize string
}

// App is the process wide agent state.
type App struct {
	Version   string
	BuildTime string

	BaseDir  string
	ConfPath string
	Config   *config.Config

	DataDir           string
	LogsDir           string
	TmpDir            string
	ProjectLogDir     string
	ConfDir           string
	ScriptDir         string
	GlobalScriptDir   string
	ScriptRunCacheDir string
	TempDir           string
	Executable        string
	// RunDir is the directory holding the executable (Java's lib/ or bin/).
	RunDir string

	Auth Auth

	Pid         int
	RandomID    string
	InstallID   string
	InstallTime string
	StartAt     time.Time

	logger  *slog.Logger
	logFile *os.File
}

type installInfo struct {
	InstallID   string `json:"installId"`
	InstallTime string `json:"installTime"`
	Desc        string `json:"desc"`
}

type authorizeInfo struct {
	AgentName string `json:"agentName"`
	AgentPwd  string `json:"agentPwd"`
}

// New builds the runtime, loads configuration and prepares the data directory.
func New(confPath, baseDir, version, buildTime string) (*App, error) {
	autoBase, exe, err := detectBaseDir()
	if err != nil {
		return nil, err
	}
	if strings.TrimSpace(baseDir) != "" {
		absBase, err := filepath.Abs(baseDir)
		if err != nil {
			return nil, err
		}
		autoBase = absBase
	}
	conf := confPath
	if strings.TrimSpace(conf) == "" {
		conf = filepath.Join(autoBase, dirConf, "application.yml")
	}
	cfg, err := config.Load(conf)
	if err != nil {
		return nil, err
	}
	base := autoBase
	if strings.TrimSpace(cfg.Jpom.Path) != "" {
		absPath, err := filepath.Abs(cfg.Jpom.Path)
		if err != nil {
			return nil, err
		}
		base = absPath
	}
	a := &App{
		Version:    version,
		BuildTime:  buildTime,
		BaseDir:    base,
		ConfPath:   conf,
		Config:     cfg,
		Executable: exe,
		RunDir:     filepath.Dir(exe),
		StartAt:    time.Now(),
		Pid:        os.Getpid(),
	}
	a.DataDir = filepath.Join(base, dirData)
	a.LogsDir = filepath.Join(base, dirLogs)
	// 与 Java 端一致：优先使用 JPOM_LOG 环境变量（logback 配置会写入该变量）。
	if logPath := strings.TrimSpace(os.Getenv("JPOM_LOG")); logPath != "" {
		a.LogsDir = logPath
	}
	a.TmpDir = filepath.Join(base, dirTmp)
	a.ProjectLogDir = filepath.Join(base, dirProjectLog)
	a.ConfDir = filepath.Join(base, dirConf)
	a.ScriptDir = filepath.Join(a.DataDir, dirScript)
	a.GlobalScriptDir = filepath.Join(a.DataDir, dirGlobalScript)
	a.ScriptRunCacheDir = filepath.Join(a.DataDir, dirScriptRunCache)
	a.TempDir = filepath.Join(a.DataDir, dirTemp)
	for _, dir := range []string{a.DataDir, a.LogsDir, a.TmpDir, a.ProjectLogDir, a.ConfDir, a.ScriptDir, a.GlobalScriptDir, a.ScriptRunCacheDir, a.TempDir} {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			return nil, fmt.Errorf("create directory failed %s: %w", dir, err)
		}
	}
	a.RandomID = randomHex(16)
	if err := a.initLog(); err != nil {
		return nil, err
	}
	if err := a.initAuthorize(); err != nil {
		return nil, err
	}
	if err := a.initInstall(); err != nil {
		return nil, err
	}
	a.Log().Info("jpom agent data ready",
		"base", a.BaseDir, "data", a.DataDir, "installId", a.InstallID)
	return a, nil
}

// detectBaseDir mirrors the Java agent layout: the executable lives in bin/ or
// lib/ and the agent home is its parent.
func detectBaseDir() (string, string, error) {
	exe, err := os.Executable()
	if err != nil {
		return "", "", err
	}
	if resolved, err := filepath.EvalSymlinks(exe); err == nil {
		exe = resolved
	}
	exeDir := filepath.Dir(exe)
	base := exeDir
	switch strings.ToLower(filepath.Base(exeDir)) {
	case "bin", "lib":
		base = filepath.Dir(exeDir)
	}
	// go run puts the binary in a temp dir; fall back to the working directory.
	if strings.Contains(exeDir, string(filepath.Separator)+"go-build") || strings.HasPrefix(exeDir, os.TempDir()) {
		if wd, err := os.Getwd(); err == nil {
			base = wd
		}
	}
	return base, exe, nil
}

func (a *App) initLog() error {
	logPath := filepath.Join(a.LogsDir, "agent.log")
	f, err := os.OpenFile(logPath, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o644)
	if err != nil {
		return fmt.Errorf("open log file failed %s: %w", logPath, err)
	}
	a.logFile = f
	writer := io.MultiWriter(os.Stdout, f)
	level := slog.LevelInfo
	if strings.EqualFold(os.Getenv("JPOM_DEBUG"), "true") || strings.EqualFold(os.Getenv("JPOM_DEBUG"), "1") {
		level = slog.LevelDebug
	}
	handler := slog.NewTextHandler(writer, &slog.HandlerOptions{Level: level})
	a.logger = slog.New(handler)
	return nil
}

// Log returns the process logger.
func (a *App) Log() *slog.Logger {
	if a.logger == nil {
		return slog.Default()
	}
	return a.logger
}

// Close releases resources held by the runtime.
func (a *App) Close() {
	if a.logFile != nil {
		_ = a.logFile.Close()
	}
}

func (a *App) initAuthorize() error {
	cfgAuth := a.Config.Jpom.Authorize
	name := strings.TrimSpace(cfgAuth.AgentName)
	if name == "" {
		return fmt.Errorf("agent name must not be empty (jpom.authorize.agent-name)")
	}
	if strings.TrimSpace(cfgAuth.Authorize) != "" {
		a.Auth = Auth{Name: name, Authorize: cfgAuth.Authorize}
		a.Log().Warn("using authorize string from config file (jpom.authorize.authorize), it will not be regenerated")
		return nil
	}
	authorizeFile := filepath.Join(a.DataDir, fileAgentAuthorize)
	if strings.TrimSpace(cfgAuth.AgentPwd) != "" {
		_ = os.Remove(authorizeFile)
		a.Log().Info("using custom authorization from config file", "account", name)
		a.Auth = Auth{Name: name, Pwd: cfgAuth.AgentPwd, Authorize: sha1Hex(name + "@" + cfgAuth.AgentPwd)}
		return nil
	}
	if content, err := os.ReadFile(authorizeFile); err == nil {
		var info authorizeInfo
		if err := json.Unmarshal(content, &info); err == nil {
			if info.AgentName != "" && info.AgentName != name {
				return fmt.Errorf("existing agent name %s does not match configured agent name %s", info.AgentName, name)
			}
			if info.AgentPwd != "" {
				_ = os.Chmod(authorizeFile, 0o600)
				a.Auth = Auth{Name: name, Pwd: info.AgentPwd, Authorize: sha1Hex(name + "@" + info.AgentPwd)}
				a.Log().Info("loaded authorized account", "account", name, "file", authorizeFile)
				return nil
			}
		}
	}
	pwd := randomString(10)
	content, _ := json.Marshal(authorizeInfo{AgentName: name, AgentPwd: pwd})
	if err := os.WriteFile(authorizeFile, content, 0o600); err != nil {
		return fmt.Errorf("write authorize file failed: %w", err)
	}
	_ = os.Chmod(authorizeFile, 0o600)
	a.Auth = Auth{Name: name, Pwd: pwd, Authorize: sha1Hex(name + "@" + pwd)}
	a.Log().Info("generated authorization account and wrote it to file", "account", name, "file", authorizeFile)
	return nil
}

func (a *App) initInstall() error {
	installFile := filepath.Join(a.DataDir, fileInstall)
	if content, err := os.ReadFile(installFile); err == nil {
		var info installInfo
		if err := json.Unmarshal(content, &info); err == nil && info.InstallID != "" {
			a.InstallID = info.InstallID
			a.InstallTime = info.InstallTime
			return nil
		}
	}
	info := installInfo{
		InstallID:   randomHex(16),
		InstallTime: time.Now().Format("2006-01-02 15:04:05"),
		Desc:        i18n.T(i18n.LangZhCn, "i18n.go_install_file_desc"),
	}
	content, _ := json.Marshal(info)
	if err := os.WriteFile(installFile, content, 0o644); err != nil {
		return fmt.Errorf("write install info failed: %w", err)
	}
	a.InstallID = info.InstallID
	a.InstallTime = info.InstallTime
	return nil
}

// CheckAuthorize validates the Jpom-Agent-Authorize header value.
func (a *App) CheckAuthorize(value string) bool {
	if value == "" || a.Auth.Authorize == "" {
		return false
	}
	return subtle.ConstantTimeCompare([]byte(value), []byte(a.Auth.Authorize)) == 1
}

// ListenAddr returns the HTTP listen address.
func (a *App) ListenAddr() string {
	address := strings.TrimSpace(a.Config.Server.Address)
	return fmt.Sprintf("%s:%d", address, a.Config.Server.Port)
}

// OsName matches the value reported by the Java agent manifest.
func (a *App) OsName() string {
	switch runtime.GOOS {
	case "linux":
		return "Linux"
	case "windows":
		return "Windows"
	case "darwin":
		return "Mac OS X"
	default:
		return runtime.GOOS
	}
}

// Manifest returns the /info manifest object, byte compatible with the Java agent.
//
// The Java agent serializes it through Jackson where every long becomes a
// string (pid/upTime), the package timestamp is "yyyy-MM-dd HH:mm:ss" and
// debug means "version == dev".
func (a *App) Manifest() map[string]any {
	return map[string]any{
		"version":   a.Version,
		"timeStamp": a.packageTime(),
		"pid":       fmt.Sprintf("%d", a.Pid),
		"type":      "Agent",
		"port":      a.Config.Server.Port,
		"randomId":  a.RandomID,
		"dataPath":  a.DataDir,
		"jarFile":   a.Executable,
		"osName":    a.OsName(),
		"installId": a.InstallID,
		"debug":     a.Version == "dev",
		"upTime":    fmt.Sprintf("%d", int64(time.Since(a.StartAt).Seconds())),
		// engine 是 Go 版新增字段（G6）：Server 据此判断节点升级通道；
		// Java 旧服务端/旧 Agent 反序列化时忽略未知字段，保持字节兼容。
		"engine": "go",
	}
}

// packageTime mirrors JpomManifest#getTimeStamp: the package build time in the
// local "yyyy-MM-dd HH:mm:ss" format, or the start time for debug builds.
func (a *App) packageTime() string {
	if strings.TrimSpace(a.BuildTime) != "" {
		for _, layout := range []string{time.RFC3339Nano, time.RFC3339, "2006-01-02T15:04:05Z", "2006-01-02 15:04:05"} {
			if parsed, err := time.Parse(layout, a.BuildTime); err == nil {
				return parsed.Local().Format("2006-01-02 15:04:05")
			}
		}
		return a.BuildTime
	}
	return a.StartAt.Format("2006-01-02 15:04:05")
}

func sha1Hex(value string) string {
	sum := sha1.Sum([]byte(value))
	return hex.EncodeToString(sum[:])
}

func randomHex(bytesLen int) string {
	buf := make([]byte, bytesLen)
	if _, err := rand.Read(buf); err != nil {
		return fmt.Sprintf("%d", time.Now().UnixNano())
	}
	return hex.EncodeToString(buf)
}

func randomString(length int) string {
	const alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
	buf := make([]byte, length)
	random := make([]byte, length)
	if _, err := rand.Read(random); err != nil {
		return fmt.Sprintf("%d", time.Now().UnixNano())
	}
	for i := range buf {
		buf[i] = alphabet[int(random[i])%len(alphabet)]
	}
	return string(buf)
}
