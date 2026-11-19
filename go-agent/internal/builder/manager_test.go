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
	"archive/zip"
	"errors"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"testing"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/config"
)

// newTestApp 构造一个只依赖临时目录的 App（Sysinfo/Manager 单测同款）。
func newTestApp(t *testing.T, maxConcurrency int) *app.App {
	t.Helper()
	base := t.TempDir()
	cfg := config.Default()
	if maxConcurrency > 0 {
		cfg.Jpom.Build.MaxConcurrency = maxConcurrency
	}
	a := &app.App{
		Version:    "dev",
		BaseDir:    base,
		Config:     cfg,
		DataDir:    filepath.Join(base, "data"),
		TmpDir:     filepath.Join(base, "tmp"),
		Pid:        4242,
		StartAt:    time.Now(),
		RunDir:     base,
		Executable: filepath.Join(base, "bin", "agent"),
	}
	for _, dir := range []string{a.DataDir, a.TmpDir} {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			t.Fatalf("mkdir %s: %v", dir, err)
		}
	}
	return a
}

func newTestManager(t *testing.T, maxConcurrency int) (*Manager, *app.App) {
	t.Helper()
	a := newTestApp(t, maxConcurrency)
	return NewManager(a), a
}

// TestManagerBusyAndUnsupported 覆盖并发闸门（409）与不支持类型（501）的判定。
func TestManagerBusyAndUnsupported(t *testing.T) {
	manager, _ := newTestManager(t, 1)
	if manager.MaxConcurrency() != 1 {
		t.Fatalf("MaxConcurrency = %d", manager.MaxConcurrency())
	}
	// 占满并发名额后必须拒绝新 job（Server 侧据此排队重试）。
	manager.mu.Lock()
	manager.running = manager.maxConcurrency
	manager.mu.Unlock()
	if _, err := manager.Start(&Spec{JobID: "job-busy", Repo: RepoSpec{Type: "git", URL: "/nonexistent"}}); !errors.Is(err, ErrBusy) {
		t.Fatalf("并发已满应返回 ErrBusy, got %v", err)
	}
	manager.mu.Lock()
	manager.running = 0
	manager.mu.Unlock()
	// svn 未下沉到插件端：start 直接 501。
	if _, err := manager.Start(&Spec{JobID: "job-svn", Repo: RepoSpec{Type: "svn"}}); !errors.Is(err, ErrUnsupported) {
		t.Fatalf("svn 应返回 ErrUnsupported, got %v", err)
	}
	if _, err := manager.Start(&Spec{JobID: ""}); err == nil {
		t.Fatal("缺少 jobId 应报错")
	}
}

// TestManagerRecoverMarksLost 覆盖重启恢复：running → interrupted，对外报 lost（PROTOCOL §9）。
func TestManagerRecoverMarksLost(t *testing.T) {
	manager, a := newTestManager(t, 1)
	record := jobRecord{
		JobID:        "job-restart",
		BuildModelID: "model-1",
		State:        StateRunning,
		Stage:        StageExecuteCommand,
		SourceDir:    filepath.Join(a.DataDir, "build", "model-1", "source"),
	}
	manager.writeRecord(record)

	// 重新创建 Manager 等价于插件端进程重启。
	restarted := NewManager(a)
	status, err := restarted.Status("job-restart")
	if err != nil {
		t.Fatalf("Status: %v", err)
	}
	if status.State != StateLost {
		t.Fatalf("重启后状态 = %s, want lost", status.State)
	}
	loaded, err := readRecord(restarted.jobFile("job-restart"))
	if err != nil {
		t.Fatalf("readRecord: %v", err)
	}
	if loaded.State != StateInterrupted {
		t.Fatalf("落盘状态 = %s, want interrupted", loaded.State)
	}
	if _, err := restarted.Status("job-unknown"); !errors.Is(err, ErrJobNotFound) {
		t.Fatalf("未知 job 应返回 ErrJobNotFound, got %v", err)
	}
}

// TestManagerSourceExists 覆盖 /build/source-exists 的判定。
func TestManagerSourceExists(t *testing.T) {
	manager, a := newTestManager(t, 1)
	if err := os.MkdirAll(filepath.Join(a.DataDir, "build", "model-1", "source"), 0o755); err != nil {
		t.Fatal(err)
	}
	result := manager.SourceExists([]string{"model-1", "model-2", " "})
	if !result["model-1"] {
		t.Errorf("model-1 应存在: %+v", result)
	}
	if result["model-2"] {
		t.Errorf("model-2 不应存在: %+v", result)
	}
	if _, ok := result[""]; ok {
		t.Errorf("空 id 不应出现在结果里: %+v", result)
	}
}

// requireGit 在没有 git 的环境跳过依赖 git 的用例。
func requireGit(t *testing.T) {
	t.Helper()
	if _, err := exec.LookPath("git"); err != nil {
		t.Skip("本机没有 git，跳过依赖 git 的用例")
	}
}

// initLocalRepo 建一个本地仓库（不依赖网络），返回仓库路径与当前分支。
func initLocalRepo(t *testing.T) (string, string) {
	t.Helper()
	dir := filepath.Join(t.TempDir(), "repo")
	if err := os.MkdirAll(dir, 0o755); err != nil {
		t.Fatal(err)
	}
	runGitTest(t, dir, "init")
	runGitTest(t, dir, "config", "user.email", "test@example.com")
	runGitTest(t, dir, "config", "user.name", "tester")
	if err := os.WriteFile(filepath.Join(dir, "README.md"), []byte("hello"), 0o644); err != nil {
		t.Fatal(err)
	}
	runGitTest(t, dir, "add", ".")
	runGitTest(t, dir, "commit", "-m", "init")
	branch := strings.TrimSpace(gitOutput(t, dir, "rev-parse", "--abbrev-ref", "HEAD"))
	return dir, branch
}

func runGitTest(t *testing.T, dir string, args ...string) {
	t.Helper()
	command := exec.Command("git", args...)
	command.Dir = dir
	if output, err := command.CombinedOutput(); err != nil {
		t.Fatalf("git %v: %v (%s)", args, err, output)
	}
}

func gitOutput(t *testing.T, dir string, args ...string) string {
	t.Helper()
	command := exec.Command("git", args...)
	command.Dir = dir
	output, err := command.Output()
	if err != nil {
		t.Fatalf("git %v: %v", args, err)
	}
	return string(output)
}

// waitTerminal 轮询状态直到 job 进入终态。
func waitTerminal(t *testing.T, manager *Manager, jobID string, timeout time.Duration) *Status {
	t.Helper()
	deadline := time.Now().Add(timeout)
	for {
		status, err := manager.Status(jobID)
		if err != nil {
			t.Fatalf("Status(%s): %v", jobID, err)
		}
		switch status.State {
		case StateSuccess, StateFailed, StateCancelled, StateTimeout, StateSkipped, StateLost:
			return status
		}
		if time.Now().After(deadline) {
			t.Fatalf("job %s 未在 %s 内结束, state=%s stage=%s", jobID, timeout, status.State, status.Stage)
		}
		time.Sleep(50 * time.Millisecond)
	}
}

// TestJobEndToEndWithLocalGit 走完 pull → executeCommand → packageFile（本地仓库，不依赖网络）。
func TestJobEndToEndWithLocalGit(t *testing.T) {
	requireGit(t)
	if runtime.GOOS == "windows" {
		t.Skip("脚本用例按 sh 编写")
	}
	repo, branch := initLocalRepo(t)
	manager, _ := newTestManager(t, 1)
	spec := &Spec{
		JobID:         "job-e2e",
		BuildModelID:  "model-e2e",
		BuildNumberID: 7,
		Repo:          RepoSpec{Type: "git", URL: repo, BranchPattern: branch, CloneDepth: 1},
		Script:        "echo building && mkdir -p dist && echo hello > dist/index.html",
		ShellSuffix:   "sh",
		Strict:        true,
		ResultDirFile: "dist",
	}
	job, err := manager.Start(spec)
	if err != nil {
		t.Fatalf("Start: %v", err)
	}
	status := waitTerminal(t, manager, job.JobID(), 90*time.Second)
	if status.State != StateSuccess {
		logContent, _ := os.ReadFile(status.LogFile)
		t.Fatalf("state=%s message=%s\nlog:\n%s", status.State, status.Message, string(logContent))
	}
	if !status.ResultReady || status.ResultSize <= 0 || len(status.ResultSha1) != 40 {
		t.Fatalf("产物元信息异常: %+v", status)
	}
	path, ok := manager.ZipPath(job.JobID())
	if !ok {
		t.Fatal("产物 zip 未就绪")
	}
	reader, err := zip.OpenReader(path)
	if err != nil {
		t.Fatalf("open zip: %v", err)
	}
	defer func() { _ = reader.Close() }()
	names := make([]string, 0, len(reader.File))
	for _, file := range reader.File {
		names = append(names, file.Name)
	}
	assertNames(t, names, "dist/index.html")

	// 事件流：必须包含 result 与 exit 帧，且 exit 的 state 为 success。
	backlog, _, cancel, err := manager.Subscribe(job.JobID(), 0)
	if err != nil {
		t.Fatalf("Subscribe: %v", err)
	}
	defer cancel()
	var hasResult, hasExit bool
	for _, event := range backlog {
		switch event.Type {
		case TypeResult:
			hasResult = event.Sha1 == status.ResultSha1 && event.Size == status.ResultSize
		case TypeExit:
			hasExit = event.State == StateSuccess
		}
	}
	if !hasResult || !hasExit {
		t.Fatalf("事件流缺少 result/exit 帧: %+v", backlog)
	}
	// 日志文件里有脚本输出。
	logContent, err := os.ReadFile(status.LogFile)
	if err != nil {
		t.Fatalf("读取日志: %v", err)
	}
	if !strings.Contains(string(logContent), "building") {
		t.Errorf("日志文件缺少脚本输出: %s", string(logContent))
	}
}

// TestJobSkippedWhenNoChanges 覆盖差异预判：checkDiff=true 且 lastCommitId 与远端一致 → skipped。
func TestJobSkippedWhenNoChanges(t *testing.T) {
	requireGit(t)
	repo, branch := initLocalRepo(t)
	commit := strings.TrimSpace(gitOutput(t, repo, "rev-parse", "HEAD"))
	manager, _ := newTestManager(t, 1)
	spec := &Spec{
		JobID:         "job-skip",
		BuildModelID:  "model-skip",
		BuildNumberID: 1,
		Repo: RepoSpec{
			Type:          "git",
			URL:           repo,
			BranchPattern: branch,
			CheckDiff:     true,
			LastCommitID:  commit,
		},
		Script:      "echo should-not-run",
		ShellSuffix: "sh",
		Strict:      true,
	}
	if _, err := manager.Start(spec); err != nil {
		t.Fatalf("Start: %v", err)
	}
	status := waitTerminal(t, manager, "job-skip", 60*time.Second)
	if status.State != StateSkipped {
		logContent, _ := os.ReadFile(status.LogFile)
		t.Fatalf("state=%s message=%s log=%s", status.State, status.Message, string(logContent))
	}
	if status.ResultReady {
		t.Error("skipped 不应产出 zip")
	}
	backlog, _, cancel, err := manager.Subscribe("job-skip", 0)
	if err != nil {
		t.Fatalf("Subscribe: %v", err)
	}
	defer cancel()
	exited := false
	for _, event := range backlog {
		if event.Type == TypeExit {
			exited = event.State == StateSkipped && event.Code != nil && *event.Code == 0
		}
	}
	if !exited {
		t.Fatalf("skipped 的 exit 帧应为 code=0: %+v", backlog)
	}
}

// TestJobTimeoutWatchdog 覆盖 timeoutSec 看门狗：到点杀掉进程组并置 timeout。
func TestJobTimeoutWatchdog(t *testing.T) {
	requireGit(t)
	if runtime.GOOS == "windows" {
		t.Skip("脚本用例按 sh 编写")
	}
	repo, branch := initLocalRepo(t)
	manager, _ := newTestManager(t, 1)
	spec := &Spec{
		JobID:         "job-timeout",
		BuildModelID:  "model-timeout",
		BuildNumberID: 1,
		TimeoutSec:    2,
		Repo:          RepoSpec{Type: "git", URL: repo, BranchPattern: branch},
		Script:        "echo started && sleep 60",
		ShellSuffix:   "sh",
	}
	started := time.Now()
	if _, err := manager.Start(spec); err != nil {
		t.Fatalf("Start: %v", err)
	}
	status := waitTerminal(t, manager, "job-timeout", 120*time.Second)
	if status.State != StateTimeout {
		logContent, _ := os.ReadFile(status.LogFile)
		t.Fatalf("state=%s message=%s log=%s", status.State, status.Message, string(logContent))
	}
	if elapsed := time.Since(started); elapsed > 60*time.Second {
		t.Errorf("看门狗没有及时生效: %s", elapsed)
	}
}
