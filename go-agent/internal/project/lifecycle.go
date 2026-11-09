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
	"archive/zip"
	"bufio"
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"time"

	"jpom.io/go-agent/internal/proc"
)

// ProjectLogOptions 项目控制台日志配置（jpom.project.log）。
type ProjectLogOptions struct {
	AutoBackupConsoleCron string
	AutoBackupSize        int64
	AutoBackupToFile      bool
	SaveDays              int
	FileCharset           string
}

// Options 项目域运行时配置（来自 conf/application.yml）。
type Options struct {
	BaseDir                 string
	StatusWaitTime          int // 秒
	StatusDetectionInterval int // 毫秒
	FileBackupCount         int
	FileBackupSuffix        []string
	CommandUseSudo          bool
	ProjectLog              ProjectLogOptions
}

// asyncExec 项目启动命令异步执行入口（单测可替换，生产为 proc.AsyncExe）。
var asyncExec = proc.AsyncExe

// runtimeGOOSWindows 便于测试/可读性：当前系统是否为 Windows。
func runtimeGOOSWindows() bool {
	return runtime.GOOS == "windows"
}

// Runtime 项目域运行时：project.json 读写 + 启动/停止/状态/日志。
type Runtime struct {
	Manager *Manager
	Options Options
	Log     *slog.Logger
}

// OpResult 命令操作结果（对应 Java CommandOpResult，JSON 字段/省略规则一致）。
type OpResult struct {
	Success   bool
	Pid       *int
	Pids      []int
	Ports     *string
	StatusMsg *string
	Msgs      []string
}

// OpResultSimple 构造只带 success/msgs 的结果（等价 CommandOpResult.of(boolean, ...)）。
func OpResultSimple(success bool, msgs ...string) *OpResult {
	result := &OpResult{Success: success}
	for _, msg := range msgs {
		if msg != "" {
			result.Msgs = append(result.Msgs, msg)
		}
	}
	return result
}

// OpResultOfMsg 解析状态字符串（等价 CommandOpResult.of(String)）。
//
// 状态格式：running:<pid>[,<pid>...][:<ports>]；其他字符串视为未运行。
func OpResultOfMsg(msg string) *OpResult {
	result := &OpResult{}
	var pids []int
	var ports *string
	if strings.HasPrefix(msg, proc.RunningTag) {
		parts := splitTrim(msg, ":")
		if len(parts) > 1 {
			for _, item := range strings.Split(parts[1], ",") {
				if pid, err := strconv.Atoi(strings.TrimSpace(item)); err == nil {
					pids = append(pids, pid)
				}
			}
		}
		if len(parts) > 2 {
			value := parts[2]
			ports = &value
		}
	}
	mainPid := 0
	if len(pids) > 0 {
		mainPid = pids[0]
	}
	result.Success = mainPid > 0
	if len(pids) > 1 {
		result.Pids = pids
	}
	result.Pid = &mainPid
	result.Ports = ports
	value := msg
	result.StatusMsg = &value
	result.Append(msg)
	return result
}

// Append 追加消息。
func (o *OpResult) Append(msgs ...string) *OpResult {
	for _, msg := range msgs {
		if msg != "" {
			o.Msgs = append(o.Msgs, msg)
		}
	}
	return o
}

// MsgStr 消息拼接（等价 Java CommandOpResult.msgStr，逗号分隔）。
func (o *OpResult) MsgStr() string {
	return strings.Join(o.Msgs, ",")
}

// JSON 转为响应数据，省略 null 字段（Jackson NON_NULL 语义）。
func (o *OpResult) JSON() map[string]any {
	data := map[string]any{"success": o.Success}
	if o.Pid != nil {
		data["pid"] = *o.Pid
	}
	if len(o.Pids) > 0 {
		data["pids"] = o.Pids
	}
	if o.Ports != nil {
		data["ports"] = *o.Ports
	}
	if o.StatusMsg != nil {
		data["statusMsg"] = *o.StatusMsg
	}
	msgs := o.Msgs
	if msgs == nil {
		msgs = []string{}
	}
	data["msgs"] = msgs
	return data
}

// splitTrim 按分隔符切分并去掉空项（等价 StrUtil.splitTrim）。
func splitTrim(value, sep string) []string {
	parts := strings.Split(value, sep)
	result := make([]string, 0, len(parts))
	for _, part := range parts {
		part = strings.TrimSpace(part)
		if part != "" {
			result = append(result, part)
		}
	}
	return result
}

// Status 查询项目状态（等价 ProjectCommander.status）。
func (r *Runtime) Status(info map[string]any) *OpResult {
	original, err := r.Manager.ResolveModel(info)
	if err != nil {
		return OpResultSimple(false, err.Error())
	}
	return r.status(info, original)
}

// status 查询项目状态。
func (r *Runtime) status(info, original map[string]any) *OpResult {
	runMode := GetString(original, "runMode")
	if runMode == RunModeFile {
		return OpResultSimple(false, "file 类型项目没有运行状态")
	}
	if runMode == RunModeDsl {
		return r.dslStatus(info, original)
	}
	return OpResultOfMsg(proc.Status(GetString(info, "id")))
}

// IsRun 项目是否正在运行（等价 ProjectCommander.isRun）。
func (r *Runtime) IsRun(info map[string]any) bool {
	return r.Status(info).Success
}

// Operate 执行项目操作（对应 ProjectStatusController.operate / ConsoleCommandOp）。
func (r *Runtime) Operate(op string, info map[string]any) (*OpResult, error) {
	original, err := r.Manager.ResolveModel(info)
	if err != nil {
		return nil, err
	}
	switch op {
	case OpStart:
		return r.start(info, original, false), nil
	case OpStop:
		return r.stop(info, original, false), nil
	case OpRestart:
		return r.restart(info, original), nil
	case OpStatus:
		return r.status(info, original), nil
	case OpReload:
		return r.reload(info, original)
	default:
		return nil, fmt.Errorf("%s error", op)
	}
}

// NoOpResult 表示占位结果（保留给调用方）。
func NoOpResult() *OpResult { return OpResultSimple(true) }

// checkStart 启动前检查（等价 AbstractProjectCommander.checkStart）。
func (r *Runtime) checkStart(info, original map[string]any) string {
	status := r.status(info, original)
	if status.Success {
		pid := 0
		if status.Pid != nil {
			pid = *status.Pid
		}
		return fmt.Sprintf("当前程序正在运行中，不能重复启动,PID:%d", pid)
	}
	lib, err := r.Manager.ResolveLibFile(info)
	if err != nil {
		return err.Error()
	}
	entries, err := os.ReadDir(lib)
	if err != nil || len(entries) == 0 {
		return "项目目录没有任何文件,请先到项目文件管理中上传文件"
	}
	runMode := GetString(original, "runMode")
	switch runMode {
	case RunModeDsl:
		if _, err := parseDslConfig(original); err != nil {
			return err.Error()
		}
	case RunModeClassPath, RunModeJavaExtDirsCp:
		// Java 会校验 mainClass 可加载；Go 端无法加载 class，交由 java 进程启动报错
		if GetString(original, "mainClass") == "" {
			return "ClassPath、JavaExtDirsCp 模式 MainClass必填"
		}
		if _, err := resolveJavaPath(runtimeGOOSWindows()); err != nil {
			return err.Error()
		}
	case RunModeJar, RunModeJarWar:
		if _, err := resolveJavaPath(runtimeGOOSWindows()); err != nil {
			return err.Error()
		}
		files := listJars(runMode, lib)
		if len(files) == 0 {
			return fmt.Sprintf("一级目录没有%s包,请先到文件管理中上传程序的%s", runMode, runMode)
		}
		if msg := checkJar(files[0]); msg != "" {
			return msg
		}
	default:
		return "当前项目类型不支持启动"
	}
	// 备份日志
	r.backLog(info, original)
	return ""
}

// checkJar 校验 jar/war 的 MANIFEST Main-Class（等价 AbstractProjectCommander.checkJar）。
func checkJar(jarFile string) string {
	reader, err := zip.OpenReader(jarFile)
	if err != nil {
		return jarFile + "解析异常:" + err.Error()
	}
	defer func() { _ = reader.Close() }()
	for _, file := range reader.File {
		if !strings.EqualFold(file.Name, "META-INF/MANIFEST.MF") {
			continue
		}
		handle, err := file.Open()
		if err != nil {
			return jarFile + "解析异常:" + err.Error()
		}
		scanner := bufio.NewScanner(handle)
		for scanner.Scan() {
			line := strings.TrimSpace(scanner.Text())
			if strings.HasPrefix(strings.ToLower(line), "main-class:") {
				mainClass := strings.TrimSpace(line[len("main-class:"):])
				if mainClass == "" {
					continue
				}
				_ = handle.Close()
				return ""
			}
		}
		_ = handle.Close()
		return jarFile + "中没有找到对应的MainClass属性"
	}
	return jarFile + "中没有找到对应的MainClass属性"
}

// start 启动项目（等价 AbstractProjectCommander.start）。
func (r *Runtime) start(info, original map[string]any, sync bool) *OpResult {
	if msg := r.checkStart(info, original); msg != "" {
		return OpResultSimple(false, msg)
	}
	runMode := GetString(original, "runMode")
	if runMode == RunModeDsl {
		if err := r.dslRun(info, original, OpStart, sync); err != nil {
			return OpResultSimple(false, err.Error())
		}
	} else if err := r.execStartCommand(info, original); err != nil {
		return OpResultSimple(false, err.Error())
	}
	r.loopCheckRun(info, original, true)
	status := r.status(info, original)
	r.AsyncWebHooks(info, "start", map[string]any{"result": status.JSON()})
	return status
}

// execStartCommand 构建并异步执行项目启动命令。
//
// 注意：项目启动命令与 Java 一致**不加 sudo**（Java 只有 kill -9 兜底使用 hopeUseSudo=true），
// 否则 sudo secure_path 可能不含 java 导致 nohup: java not found。
func (r *Runtime) execStartCommand(info, original map[string]any) error {
	command := r.buildRunCommand(info, original)
	if command == "" {
		return fmt.Errorf("没有需要执行的命令")
	}
	lib, err := r.Manager.ResolveLibFile(info)
	if err != nil {
		return err
	}
	env := loadWorkspaceEnv(r.Options.BaseDir, GetString(info, "workspaceId"))
	if err := asyncExec(lib, env, command, false); err != nil {
		if r.Log != nil {
			r.Log.Error("项目启动命令执行失败", "id", GetString(info, "id"), "error", err)
		}
		return err
	}
	return nil
}

// stop 停止项目（等价 AbstractProjectCommander.stop）。
func (r *Runtime) stop(info, original map[string]any, sync bool) *OpResult {
	nodeRunMode := GetString(info, "runMode")
	if nodeRunMode == RunModeFile {
		return OpResultSimple(true, "file 类型项目没有 stop")
	}
	beforeStopMsg := r.WebHooks(info, "beforeStop", nil)
	status := r.status(info, original)
	r.AsyncWebHooks(info, "stop", map[string]any{"result": status.JSON()})
	if !status.Success {
		return OpResultSimple(true).Append(status.Msgs...).Append(beforeStopMsg)
	}
	if status.Pid != nil {
		proc.ClearPortCache(*status.Pid)
	}
	if nodeRunMode == RunModeDsl {
		if err := r.dslRun(info, original, OpStop, sync); err != nil {
			return OpResultSimple(false, err.Error()).Append(status.Msgs...)
		}
		stopped := r.loopCheckRun(info, original, false)
		result := OpResultSimple(stopped)
		if stopped {
			result.Append("stop done")
		} else {
			result.Append("stop done,but unsuccessful")
		}
		return result.Append(status.Msgs...).Append(beforeStopMsg)
	}
	pid := 0
	if status.Pid != nil {
		pid = *status.Pid
	}
	return r.stopJava(info, original, pid).Append(status.Msgs...).Append(beforeStopMsg)
}

// stopJava 结束 java 进程（等价 BaseUnixProjectCommander.stopJava）。
func (r *Runtime) stopJava(info, original map[string]any, pid int) *OpResult {
	lib, err := r.Manager.ResolveLibFile(original)
	if err != nil {
		lib = r.Options.BaseDir
	}
	msgs := make([]string, 0, 4)
	msgs = append(msgs, proc.Kill(lib, pid))
	success := false
	if r.loopCheckRun(info, original, false) {
		success = true
	} else {
		msgs = append(msgs, "Kill not completed, test kill -9")
		_ = asyncExec(lib, nil, fmt.Sprintf("kill -9 %d", pid), r.Options.CommandUseSudo)
		if r.loopCheckRunWith(info, original, 5, false) {
			success = true
		} else {
			msgs = append(msgs, "Kill -9 not completed, kill -9 failed ")
		}
	}
	result := OpResultOfMsg(proc.Status(GetString(info, "id")))
	result.Success = success
	return result.Append(msgs...)
}

// restart 重启项目（等价 AbstractProjectCommander.restart）。
func (r *Runtime) restart(info, original map[string]any) *OpResult {
	runMode := GetString(original, "runMode")
	if runMode == RunModeFile {
		return OpResultSimple(true, "file 类型项目没有 restart")
	}
	r.AsyncWebHooks(info, "beforeRestart", nil)
	if runMode == RunModeDsl {
		if config, err := parseDslConfig(original); err == nil && dslHasProcess(config, OpRestart) {
			if err := r.dslRun(info, original, OpRestart, false); err != nil {
				return OpResultSimple(false, err.Error())
			}
			if r.loopCheckRun(info, original, true) {
				result := r.status(info, original)
				r.AsyncWebHooks(info, "restart", map[string]any{"result": result.JSON()})
				return result
			}
			result := OpResultSimple(false, "restart done,but unsuccessful")
			r.AsyncWebHooks(info, "restart", map[string]any{"result": result.JSON()})
			return result
		}
	}
	var stopMsg *OpResult
	if r.IsRun(info) {
		stopMsg = r.stop(info, original, true)
	}
	startMsg := r.start(info, original, false)
	if stopMsg != nil {
		startMsg.Append(stopMsg.Msgs...)
	}
	return startMsg
}

// reload 重新加载 DSL 项目（等价 AbstractProjectCommander.reload）。
func (r *Runtime) reload(info, original map[string]any) (*OpResult, error) {
	if GetString(original, "runMode") != RunModeDsl {
		return nil, fmt.Errorf("非 DSL 项目不支持此操作")
	}
	code, lines, err := r.dslSyncRun(info, original, OpReload)
	var result *OpResult
	if err != nil {
		result = OpResultSimple(false, err.Error())
	} else {
		result = OpResultSimple(code == 0)
		result.Append(lines...)
	}
	r.AsyncWebHooks(info, "reload", map[string]any{"result": result.JSON()})
	// 缓存最后一次 reload 结果（等价 NodeProjectInfoModel.lastReloadResult）
	_ = r.Manager.UpdateByID(GetString(info, "id"), "-", func(item map[string]any) {
		item["lastReloadResult"] = result.JSON()
	})
	return result, nil
}

// loopCheckRun 阻塞检查项目状态，返回是否达到期望状态（等价 AbstractProjectCommander.loopCheckRun）。
func (r *Runtime) loopCheckRun(info, original map[string]any, expected bool) bool {
	waitTime := r.Options.StatusWaitTime
	return r.loopCheckRunWith(info, original, waitTime, expected)
}

// loopCheckRunWith 指定等待秒数阻塞检查项目状态。
func (r *Runtime) loopCheckRunWith(info, original map[string]any, waitTime int, expected bool) bool {
	if waitTime < 1 {
		waitTime = 1
	}
	interval := r.Options.StatusDetectionInterval
	if interval < 1 {
		interval = 1
	}
	loopCount := waitTime * 1000 / 500
	count := 0
	for {
		if r.IsRun(info) == expected {
			return true
		}
		time.Sleep(time.Duration(interval) * time.Millisecond)
		if count > loopCount {
			break
		}
		count++
	}
	return false
}

// AutoStartProjects 启动配置了 autoStart 的项目（等价 AgentStartInit.autoStartProject）。
func (r *Runtime) AutoStartProjects() {
	list, err := r.Manager.List()
	if err != nil {
		return
	}
	go func() {
		for _, info := range list {
			if !GetBool(info, "autoStart") {
				continue
			}
			func() {
				defer func() {
					if recovered := recover(); recovered != nil && r.Log != nil {
						r.Log.Warn("自动启动项目异常", "id", GetString(info, "id"), "panic", recovered)
					}
				}()
				if r.IsRun(info) {
					return
				}
				if _, err := r.Operate(OpStart, info); err != nil && r.Log != nil {
					r.Log.Warn("自动启动项目失败", "id", GetString(info, "id"), "error", err.Error())
				}
			}()
		}
	}()
}

// StartBackgroundTasks 启动项目域后台任务（当前仅 autoStart 项目）。
//
// 控制台日志自动备份（auto_back_log）由 api 层注册到 sysinfo 统一 cron 调度器，
// 以便 /system/cache.taskList 与 Java 端一致。
func (r *Runtime) StartBackgroundTasks() {
	r.AutoStartProjects()
}

// AutoBackupConsoleLogs 控制台日志自动备份与过期清理（等价 AgentStartInit.checkProject）。
//
// 由 api 层注册到 sysinfo 的 auto_back_log cron 任务。
func (r *Runtime) AutoBackupConsoleLogs() {
	r.autoBackupConsoleLogs()
}

// autoBackupConsoleLogs 控制台日志自动备份与过期清理（等价 AgentStartInit.checkProject）。
func (r *Runtime) autoBackupConsoleLogs() {
	list, err := r.Manager.List()
	if err != nil {
		return
	}
	autoSize := r.Options.ProjectLog.AutoBackupSize
	if autoSize <= 0 {
		autoSize = 50 * 1024 * 1024
	}
	saveDays := r.Options.ProjectLog.SaveDays
	if saveDays < 0 {
		saveDays = 0
	}
	for _, info := range list {
		file, err := r.Manager.ResolveLogFile(info)
		if err != nil {
			continue
		}
		if stat, err := os.Stat(file); err == nil && stat.Size() > autoSize {
			func() {
				defer func() { _ = recover() }()
				r.BackLog(info)
			}()
		}
		logBack, err := r.Manager.ResolveLogBack(info)
		if err != nil {
			continue
		}
		_ = filepath.Walk(logBack, func(path string, info os.FileInfo, err error) error {
			if err != nil || info == nil || info.IsDir() {
				return nil
			}
			if time.Since(info.ModTime()) > time.Duration(saveDays+1)*24*time.Hour {
				_ = os.Remove(path)
			}
			return nil
		})
	}
}

// cronMatchMinute 判断当前时间是否匹配 Quartz 六段 cron（秒 分 时 日 月 周）。
// 这里只按「分/时」匹配，秒级调度由每分钟的 ticker 保证。
func cronMatchMinute(expression string, now time.Time) bool {
	fields := strings.Fields(expression)
	if len(fields) < 6 {
		return false
	}
	return matchCronField(now.Minute(), fields[1]) && matchCronField(now.Hour(), fields[2])
}

// matchCronField 支持 *、*/n、a-b、a,b 与具体数字。
func matchCronField(value int, expression string) bool {
	for _, item := range strings.Split(expression, ",") {
		item = strings.TrimSpace(item)
		if item == "" {
			continue
		}
		if item == "*" || item == "?" {
			return true
		}
		if strings.HasPrefix(item, "*/") {
			step, err := strconv.Atoi(strings.TrimPrefix(item, "*/"))
			if err == nil && step > 0 && value%step == 0 {
				return true
			}
			continue
		}
		if strings.Contains(item, "-") {
			parts := strings.SplitN(item, "-", 2)
			start, err1 := strconv.Atoi(strings.TrimSpace(parts[0]))
			end, err2 := strconv.Atoi(strings.TrimSpace(parts[1]))
			if err1 == nil && err2 == nil && value >= start && value <= end {
				return true
			}
			continue
		}
		if number, err := strconv.Atoi(item); err == nil && number == value {
			return true
		}
	}
	return false
}
