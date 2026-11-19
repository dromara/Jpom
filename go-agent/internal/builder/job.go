//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package builder

import (
	"errors"
	"fmt"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/i18n"
)

// job 状态（PROTOCOL §4；interrupted 是落盘态，对外一律报 lost）。
const (
	StateRunning     = "running"
	StateSuccess     = "success"
	StateFailed      = "failed"
	StateCancelled   = "cancelled"
	StateTimeout     = "timeout"
	StateSkipped     = "skipped"
	StateInterrupted = "interrupted"
	StateLost        = "lost"
)

var (
	// errCancelled 任务被取消。
	errCancelled = errors.New("build cancelled")
	// errTimeout 任务超时被看门狗终止。
	errTimeout = errors.New("build timeout")
	// errSkipped 源码无变更，跳过本次构建。
	errSkipped = errors.New("build skipped")
)

// Spec 是 /build/start 的 job（PROTOCOL §2）。
type Spec struct {
	JobID             string            `json:"jobId"`
	BuildModelID      string            `json:"buildModelId"`
	BuildNumberID     flexInt           `json:"buildNumberId"`
	CacheBuild        *flexBool         `json:"cacheBuild"`
	TimeoutSec        flexInt           `json:"timeoutSec"`
	Repo              RepoSpec          `json:"repo"`
	Env               map[string]string `json:"env"`
	EnvMaskedKeys     []string          `json:"envMaskedKeys"`
	Script            string            `json:"script"`
	ShellSuffix       string            `json:"shellSuffix"`
	Strict            flexBool          `json:"strict"`
	ResultDirFile     string            `json:"resultDirFile"`
	ExcludeReleaseAnt string            `json:"excludeReleaseAnt"`
	ReleaseHideFile   flexBool          `json:"releaseHideFile"`
}

// cacheEnabled 缺省 true（语义与 Server 一致：只有显式 false 才删除缓存）。
func (s Spec) cacheEnabled() bool { return s.CacheBuild == nil || bool(*s.CacheBuild) }

// timeoutSeconds 返回 job 超时秒数，未配置时兜底 2 小时。
func (s Spec) timeoutSeconds() int {
	if s.TimeoutSec > 0 {
		return int(s.TimeoutSec)
	}
	return DefaultTimeoutSeconds
}

// shell 返回脚本后缀（sh/bat），未知值按 sh 处理。
func (s Spec) shell() string {
	if strings.EqualFold(strings.TrimSpace(s.ShellSuffix), "bat") {
		return "bat"
	}
	return "sh"
}

// Status 是 /build/status 的响应体（PROTOCOL §4）。
type Status struct {
	JobID          string `json:"jobId"`
	State          string `json:"state"`
	Stage          string `json:"stage,omitempty"`
	ExitCode       int    `json:"exitCode"`
	Message        string `json:"message,omitempty"`
	SourceDirExist bool   `json:"sourceDirExist"`
	ResultReady    bool   `json:"resultReady"`
	ResultSize     int64  `json:"resultSize,omitempty"`
	ResultSha1     string `json:"resultSha1,omitempty"`
	LastSeq        int    `json:"lastSeq"`
	LogFile        string `json:"logFile,omitempty"`
}

// Job 是一个构建 job 的运行态：三阶段（pull → executeCommand → packageFile）+ 事件流。
type Job struct {
	spec    Spec
	manager *Manager
	log     *JobLog

	env       map[string]string
	envMasked map[string]struct{}
	sourceDir string
	zipPath   string

	startedAt time.Time

	mu          sync.Mutex
	state       string
	stage       string
	exitCode    int
	message     string
	branch      string
	commitID    string
	pgid        int
	cmdPID      int
	cancelled   bool
	timedOut    bool
	finished    bool
	resultReady bool
	resultSize  int64
	resultSha1  string
	tempFiles   []string
}

// newJob 创建一个 job：准备日志文件、源码/产物目录与环境变量副本。
func newJob(manager *Manager, spec *Spec) (*Job, error) {
	logFile := manager.logFile(spec.BuildModelID, int(spec.BuildNumberID))
	jobLog, err := NewJobLog(logFile, manager.charset)
	if err != nil {
		return nil, err
	}
	env := make(map[string]string, len(spec.Env)+4)
	for key, value := range spec.Env {
		env[key] = value
	}
	masked := make(map[string]struct{}, len(spec.EnvMaskedKeys))
	for _, key := range spec.EnvMaskedKeys {
		masked[strings.TrimSpace(key)] = struct{}{}
	}
	job := &Job{
		spec:      *spec,
		manager:   manager,
		log:       jobLog,
		env:       env,
		envMasked: masked,
		sourceDir: manager.sourceDir(spec.BuildModelID),
		startedAt: time.Now(),
		state:     StateRunning,
		stage:     StagePull,
	}
	return job, nil
}

// JobID 返回 job 标识。
func (j *Job) JobID() string { return j.spec.JobID }

// SourceDir 返回节点上的源码目录（/build/start 响应回传）。
func (j *Job) SourceDir() string { return j.sourceDir }

// status 返回当前状态快照。
func (j *Job) status() *Status {
	j.mu.Lock()
	state := j.state
	stage := j.stage
	exitCode := j.exitCode
	message := j.message
	resultReady := j.resultReady
	resultSize := j.resultSize
	resultSha1 := j.resultSha1
	finished := j.finished
	cancelled := j.cancelled
	timedOut := j.timedOut
	j.mu.Unlock()
	if state == StateInterrupted {
		state = StateLost
	}
	if finished && (cancelled || timedOut) {
		// 进程被 kill 后 stage 退出码可能为 -1，终态以取消/超时为准。
		if cancelled {
			state = StateCancelled
		}
		if timedOut {
			state = StateTimeout
		}
	}
	return &Status{
		JobID:          j.spec.JobID,
		State:          state,
		Stage:          stage,
		ExitCode:       exitCode,
		Message:        message,
		SourceDirExist: dirExists(j.sourceDir),
		ResultReady:    resultReady,
		ResultSize:     resultSize,
		ResultSha1:     resultSha1,
		LastSeq:        j.log.Seq(),
		LogFile:        j.log.Path(),
	}
}

// terminal 报告 job 是否已进入终态。
func (j *Job) terminal() bool {
	j.mu.Lock()
	defer j.mu.Unlock()
	return j.finished
}

// Cancel 取消 job：对进程组发 SIGTERM，5s 后 SIGKILL（PROTOCOL §5）。幂等。
func (j *Job) Cancel() bool {
	j.mu.Lock()
	if j.finished {
		j.mu.Unlock()
		return false
	}
	j.cancelled = true
	pid := j.cmdPID
	j.mu.Unlock()
	if pid > 0 {
		go j.terminate(pid)
	}
	return true
}

// markTimeout 由看门狗触发：标记超时并杀掉整棵进程树。
func (j *Job) markTimeout() {
	j.mu.Lock()
	if j.finished {
		j.mu.Unlock()
		return
	}
	j.timedOut = true
	pid := j.cmdPID
	j.mu.Unlock()
	if pid > 0 {
		go j.terminate(pid)
	}
}

// terminate 先礼后兵：SIGTERM → 5s → SIGKILL（Windows 为 taskkill /T[/F]）。
// 5s 后先确认进程组还在再补刀，避免 pgid 被复用后误杀无关进程。
func (j *Job) terminate(pid int) {
	if pid <= 0 {
		return
	}
	_ = procTerminate(pid)
	timer := time.NewTimer(5 * time.Second)
	defer timer.Stop()
	<-timer.C
	if procGroupAliveFn(pid) {
		_ = procKill(pid)
	}
}

// run 执行三阶段；panic 也必须有确定终态，不留「僵尸构建中」。
func (j *Job) run() {
	defer func() {
		if recovered := recover(); recovered != nil {
			j.fail(fmt.Errorf("build job panic: %v", recovered))
		}
		j.finish()
	}()
	timeout := j.spec.timeoutSeconds()
	watchdog := time.AfterFunc(time.Duration(timeout)*time.Second, j.markTimeout)
	defer watchdog.Stop()
	if err := j.pullStage(); err != nil {
		j.fail(err)
		return
	}
	if err := j.commandStage(); err != nil {
		j.fail(err)
		return
	}
	if err := j.packageStage(); err != nil {
		j.fail(err)
		return
	}
	j.succeed()
}

// pullStage：拉取代码（含差异预判）。
func (j *Job) pullStage() error {
	j.setStage(StagePull)
	j.log.StageStart(StagePull)
	err := j.pull()
	if err != nil {
		code := 1
		if errors.Is(err, errCancelled) || errors.Is(err, errSkipped) {
			code = 0
		}
		j.log.StageEnd(StagePull, code, j.mask(err.Error()), "", "")
		return err
	}
	j.mu.Lock()
	branch, commitID := j.branch, j.commitID
	j.mu.Unlock()
	j.log.StageEnd(StagePull, 0, "", branch, commitID)
	return nil
}

// commandStage：执行构建脚本。
func (j *Job) commandStage() error {
	j.setStage(StageExecuteCommand)
	j.log.StageStart(StageExecuteCommand)
	exitCode, err := j.executeCommand()
	if err != nil {
		code := exitCode
		if errors.Is(err, errCancelled) || errors.Is(err, errTimeout) {
			// 被杀掉的进程退出码不可信，统一记 -1 便于 Server 区分。
			code = -1
		} else if code == 0 {
			code = 1
		}
		j.log.StageEnd(StageExecuteCommand, code, j.mask(err.Error()), "", "")
		return err
	}
	j.log.StageEnd(StageExecuteCommand, exitCode, "", "", "")
	return nil
}

// packageStage：收集产物并打包 zip。
func (j *Job) packageStage() error {
	j.setStage(StagePackageFile)
	j.log.StageStart(StagePackageFile)
	size, sha1, resultDir, err := j.packageResult()
	if err != nil {
		j.log.StageEnd(StagePackageFile, 1, j.mask(err.Error()), "", "")
		return err
	}
	j.log.StageEnd(StagePackageFile, 0, "", "", "")
	j.log.Result(size, sha1, resultDir)
	return nil
}

// packageResult 收集产物 + 打 zip，返回大小、sha1 与结果目录（供 result 帧使用）。
func (j *Job) packageResult() (int64, string, string, error) {
	if err := j.checkCancelled(); err != nil {
		return 0, "", "", err
	}
	pack, err := Collect(j.sourceDir, PackOptions{
		ResultDirFile:     j.spec.ResultDirFile,
		ExcludeReleaseAnt: j.spec.ExcludeReleaseAnt,
		ReleaseHideFile:   bool(j.spec.ReleaseHideFile),
	})
	if err != nil {
		return 0, "", "", err
	}
	resultDir := "/"
	if pack.Action.Type == PathOriginal {
		resultDir = pack.Action.Path
	} else {
		j.log.Log(StagePackageFile, j.t("i18n.fuzzy_match_files.139d", pack.Action.Path, pack.Matched))
		if pack.Action.AntSubMatch() != "" {
			j.log.Log(StagePackageFile, j.t("i18n.secondary_directory_match.0aec", pack.Action.AntSubMatch(), len(pack.Entries), pack.Action.Mode))
		}
	}
	if len(pack.Entries) == 0 {
		return 0, "", resultDir, fmt.Errorf("%w: %s", ErrNoMatch, j.spec.ResultDirFile)
	}
	if pack.Excluded > 0 {
		j.log.Log(StagePackageFile, j.t("i18n.cumulative_filter_files.448d", j.spec.ExcludeReleaseAnt, pack.Excluded))
	}
	zipPath := j.manager.zipFile(j.spec.BuildModelID, int(j.spec.BuildNumberID))
	size, sha1, err := Zip(pack.Entries, zipPath)
	if err != nil {
		return 0, "", resultDir, err
	}
	j.mu.Lock()
	j.zipPath = zipPath
	j.resultReady = true
	j.resultSize = size
	j.resultSha1 = sha1
	j.mu.Unlock()
	return size, sha1, resultDir, nil
}

// pull 实现代码拉取（PROTOCOL §8.1/§8.2）。
func (j *Job) pull() error {
	if err := j.checkCancelled(); err != nil {
		return err
	}
	if !j.spec.cacheEnabled() {
		j.log.Log(StagePull, j.t("i18n.delete_build_cache.c7f3"))
		if err := os.RemoveAll(j.sourceDir); err != nil {
			return err
		}
	}
	if err := os.MkdirAll(j.sourceDir, 0o755); err != nil {
		return err
	}
	repo := j.spec.Repo
	repoType := strings.ToLower(strings.TrimSpace(repo.Type))
	if repoType == "" {
		repoType = "git"
	}
	if repoType != "git" {
		return fmt.Errorf("%w: repo type %s", ErrUnsupported, repo.Type)
	}
	url := gitCredentialURL(repo.URL, repo.Username, repo.Password)
	git := j.git()
	ref, isTag, err := git.ResolveRef(url, repo.BranchPattern, repo.TagPattern)
	if err != nil {
		return err
	}
	if err := j.checkCancelled(); err != nil {
		return err
	}
	// 差异预判：远端 HEAD 与上次构建提交相同则跳过，省掉一次全量拉取。
	if bool(repo.CheckDiff) && strings.TrimSpace(repo.LastCommitID) != "" {
		remote, remoteErr := git.RemoteRef(url, ref, isTag)
		if remoteErr == nil && remote != "" && strings.EqualFold(remote, strings.TrimSpace(repo.LastCommitID)) {
			message := j.t("i18n.no_changes_in_repository_code_with_details.fd9f", remote, "")
			j.log.Log(StagePull, message)
			return fmt.Errorf("%w: %s", errSkipped, message)
		}
	}
	commitID, commitMsg, err := git.Prepare(j.sourceDir, url, ref, isTag, int(repo.CloneDepth))
	if err != nil {
		return err
	}
	j.mu.Lock()
	j.branch = ref
	j.commitID = commitID
	j.mu.Unlock()
	if strings.TrimSpace(commitMsg) != "" {
		for _, line := range strings.Split(strings.TrimRight(commitMsg, "\n"), "\n") {
			j.log.Log(StagePull, j.mask(line))
		}
	}
	// 节点侧重写/追加的环境变量（PROTOCOL §2：agent 可追加并通过 env 帧回传）。
	j.env["BUILD_SOURCE_FILE"] = j.sourceDir
	j.env["BUILD_COMMIT_ID"] = commitID
	if isTag {
		j.env["BUILD_TAG_NAME"] = ref
	} else {
		j.env["BUILD_BRANCH_NAME"] = ref
	}
	j.log.Env(j.maskedEnv())
	return nil
}

// executeCommand 写临时脚本并执行（PROTOCOL §8.3）。
func (j *Job) executeCommand() (int, error) {
	if err := j.checkCancelled(); err != nil {
		return -1, err
	}
	script := j.spec.Script
	if strings.TrimSpace(script) == "" {
		return -1, errors.New(j.t("i18n.no_command_to_execute.340b"))
	}
	suffix := j.spec.shell()
	scriptFile, err := j.writeScript(suffix, script)
	if err != nil {
		return -1, err
	}
	exitCode, err := j.runProcess(scriptFile, suffix)
	j.log.Log(StageExecuteCommand, j.t("i18n.script_exit_code.716e", exitCode))
	if err != nil {
		return exitCode, err
	}
	if bool(j.spec.Strict) && exitCode != 0 {
		return exitCode, errors.New(j.t("i18n.command_non_zero_exit_code.a6e1", exitCode))
	}
	return exitCode, nil
}

// writeScript 把脚本内容写进临时文件（后缀按 shellSuffix），退出时清理。
func (j *Job) writeScript(suffix, script string) (string, error) {
	if err := os.MkdirAll(j.manager.tmpDir, 0o755); err != nil {
		return "", err
	}
	file, err := os.CreateTemp(j.manager.tmpDir, "build-*."+suffix)
	if err != nil {
		return "", err
	}
	name := file.Name()
	if _, err := file.WriteString(script); err != nil {
		_ = file.Close()
		_ = os.Remove(name)
		return "", err
	}
	if err := file.Close(); err != nil {
		_ = os.Remove(name)
		return "", err
	}
	_ = os.Chmod(name, 0o755)
	j.mu.Lock()
	j.tempFiles = append(j.tempFiles, name)
	j.mu.Unlock()
	return name, nil
}

// git 构造拉取器：日志归属 pull 阶段，凭据在日志里掩码，进程纳入取消范围。
func (j *Job) git() *Git {
	return &Git{Log: j.log, Stage: StagePull, Mask: j.mask, OnStart: j.trackProcess}
}

// trackProcess 登记当前子进程（git / 构建脚本）的进程组：取消与超时据此杀掉整棵树。
func (j *Job) trackProcess(pid int) {
	j.mu.Lock()
	j.cmdPID = pid
	if pid > 0 {
		j.pgid = pid
	}
	j.mu.Unlock()
}

// mask 统一日志/状态文案的凭据掩码。
func (j *Job) mask(text string) string {
	text = MaskSecrets(text, j.spec.Repo.Username, j.spec.Repo.Password)
	return MaskURL(text)
}

// maskedEnv 返回回传给 Server 的环境变量（envMaskedKeys 一律 ********）。
func (j *Job) maskedEnv() map[string]string {
	data := make(map[string]string, len(j.env))
	for key, value := range j.env {
		if _, masked := j.envMasked[key]; masked {
			data[key] = "********"
			continue
		}
		data[key] = value
	}
	return data
}

// checkCancelled 在阶段边界检查取消状态（拉取/打包期间没有子进程也需要能停）。
func (j *Job) checkCancelled() error {
	j.mu.Lock()
	defer j.mu.Unlock()
	if j.timedOut {
		return errTimeout
	}
	if j.cancelled {
		return errCancelled
	}
	return nil
}

// setStage 记录当前阶段。
func (j *Job) setStage(stage string) {
	j.mu.Lock()
	j.stage = stage
	j.mu.Unlock()
	j.persist()
}

// succeed 标记成功。
func (j *Job) succeed() {
	j.mu.Lock()
	j.state = StateSuccess
	j.exitCode = 0
	j.mu.Unlock()
}

// fail 按错误类型落终态。
func (j *Job) fail(err error) {
	switch {
	case errors.Is(err, errCancelled):
		j.setState(StateCancelled, -1, j.t("i18n.cancel_success.285f"))
	case errors.Is(err, errTimeout):
		j.setState(StateTimeout, -1, fmt.Sprintf("build job timeout after %d seconds", j.spec.timeoutSeconds()))
	case errors.Is(err, errSkipped):
		j.setState(StateSkipped, 0, strings.TrimPrefix(err.Error(), errSkipped.Error()+": "))
	default:
		j.setState(StateFailed, -1, j.mask(err.Error()))
	}
}

func (j *Job) setState(state string, exitCode int, message string) {
	j.mu.Lock()
	j.state = state
	j.exitCode = exitCode
	j.message = message
	j.mu.Unlock()
}

// finish 收尾：补齐缺省终态、写 exit 帧、落盘、释放并发名额、清理临时脚本。
func (j *Job) finish() {
	j.mu.Lock()
	if j.finished {
		j.mu.Unlock()
		return
	}
	state := j.state
	exitCode := j.exitCode
	message := j.message
	if state != StateSuccess {
		if j.cancelled {
			state = StateCancelled
			exitCode = -1
		} else if j.timedOut {
			state = StateTimeout
			exitCode = -1
		} else if state == StateRunning {
			state = StateFailed
			exitCode = -1
			if message == "" {
				message = "build job finished without state"
			}
		}
	}
	j.state = state
	j.exitCode = exitCode
	j.message = message
	j.finished = true
	tempFiles := j.tempFiles
	j.mu.Unlock()

	j.log.Exit(exitCode, state, message)
	j.persist()
	for _, file := range tempFiles {
		_ = os.Remove(file)
	}
	j.manager.release(j)
}

// persist 把 job 状态写入 data/build/jobs/<jobId>.json（重启恢复用）。
func (j *Job) persist() {
	j.mu.Lock()
	record := j.recordLocked()
	finished := j.finished
	j.mu.Unlock()
	if finished {
		record.FinishedAt = time.Now().Format(time.RFC3339)
	}
	j.manager.writeRecord(record)
}

// recordSnapshot 返回当前 job 的持久化记录（Manager.release 用）。
func (j *Job) recordSnapshot() jobRecord {
	j.mu.Lock()
	defer j.mu.Unlock()
	return j.recordLocked()
}

// recordLocked 组装落盘记录（调用方必须已持有 j.mu）。
func (j *Job) recordLocked() jobRecord {
	return jobRecord{
		JobID:         j.spec.JobID,
		BuildModelID:  j.spec.BuildModelID,
		BuildNumberID: int(j.spec.BuildNumberID),
		State:         j.state,
		Stage:         j.stage,
		Pid:           j.cmdPID,
		Pgid:          j.pgid,
		ExitCode:      j.exitCode,
		Message:       j.message,
		StartedAt:     j.startedAt.Format(time.RFC3339),
		LogFile:       j.log.Path(),
		SourceDir:     j.sourceDir,
		ZipPath:       j.zipPath,
		ResultReady:   j.resultReady,
		ResultSize:    j.resultSize,
		ResultSha1:    j.resultSha1,
		LastSeq:       j.log.Seq(),
	}
}

// t 渲染当前语言的文案（复用 Java 端已有 i18n key）。
func (j *Job) t(key string, args ...any) string {
	return i18n.T(j.manager.lang, key, args...)
}

func dirExists(path string) bool {
	info, err := os.Stat(path)
	return err == nil && info.IsDir()
}
