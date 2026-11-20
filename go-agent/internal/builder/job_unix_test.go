//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

//go:build !windows

package builder

import (
	"errors"
	"syscall"
	"testing"
	"time"
)

// TestCancelKillsProcessGroup 覆盖「取消要杀掉整个进程组」：
// sh + 后台子进程都必须在 SIGTERM 后消失（只杀 sh 会留下 mvn/sleep 占满打包机）。
func TestCancelKillsProcessGroup(t *testing.T) {
	manager, _ := newTestManager(t, 1)
	spec := &Spec{
		JobID:        "job-cancel",
		BuildModelID: "model-cancel",
		Script:       "sleep 60 & echo started; wait",
		ShellSuffix:  "sh",
	}
	job, err := newJob(manager, spec)
	if err != nil {
		t.Fatalf("newJob: %v", err)
	}
	job.sourceDir = t.TempDir()
	scriptFile, err := job.writeScript("sh", spec.Script)
	if err != nil {
		t.Fatalf("writeScript: %v", err)
	}
	finished := make(chan error, 1)
	go func() {
		_, runErr := job.runProcess(scriptFile, "sh")
		finished <- runErr
	}()

	pgid := waitProcessGroup(t, job, 10*time.Second)
	if err := syscall.Kill(-pgid, 0); err != nil {
		t.Fatalf("进程组未建立（pgid=%d）: %v", pgid, err)
	}
	if !job.Cancel() {
		t.Fatal("Cancel 应返回 true")
	}
	// 幂等：重复取消不应 panic 也不应报错。
	job.Cancel()

	select {
	case runErr := <-finished:
		if !errors.Is(runErr, errCancelled) {
			t.Fatalf("runProcess 应返回 errCancelled, got %v", runErr)
		}
	case <-time.After(20 * time.Second):
		t.Fatal("进程未在取消后退出")
	}

	deadline := time.Now().Add(10 * time.Second)
	for {
		killErr := syscall.Kill(-pgid, 0)
		if errors.Is(killErr, syscall.ESRCH) {
			return
		}
		if time.Now().After(deadline) {
			t.Fatalf("进程组仍然存在（pgid=%d, err=%v）", pgid, killErr)
		}
		time.Sleep(50 * time.Millisecond)
	}
}

// waitProcessGroup 等待子进程启动并返回进程组 id。
func waitProcessGroup(t *testing.T, job *Job, timeout time.Duration) int {
	t.Helper()
	deadline := time.Now().Add(timeout)
	for {
		job.mu.Lock()
		pgid := job.pgid
		job.mu.Unlock()
		if pgid > 0 {
			return pgid
		}
		if time.Now().After(deadline) {
			t.Fatal("子进程没有启动")
		}
		time.Sleep(20 * time.Millisecond)
	}
}
