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
	"bufio"
	"errors"
	"os"
	"os/exec"
	"runtime"

	"github.com/dromara/Jpom/go-agent/internal/proc"
)

// procTerminate / procKill 是进程组结束动作（Unix: kill(-pgid) 先 SIGTERM 后 SIGKILL；
// Windows: taskkill /T 与 /T /F）。抽成变量便于单测替换。
var (
	procTerminate    = proc.TerminateProcessGroup
	procKill         = proc.KillProcessGroup
	procGroupAliveFn = proc.ProcessGroupAlive
)

// shellCommand 构造「执行脚本文件」的命令：
// unix 用 sh -c <脚本路径>（与 Server 的 CommandUtil.execWaitFor 同语义），
// windows 用 cmd /c <脚本路径>。
func shellCommand(scriptFile string) []string {
	if runtime.GOOS == "windows" {
		return []string{"cmd", "/c", scriptFile}
	}
	return []string{"sh", "-c", proc.ShellQuote(scriptFile)}
}

// runProcess 执行脚本文件并把 stdout+stderr 逐行写进 job 日志，返回退出码。
//
// 进程以新进程组启动（proc.SetProcessGroup），取消/超时由 Cancel/markTimeout 对整个
// 进程组发信号，避免只杀掉 sh 而留下 mvn/子进程占满打包机。
func (j *Job) runProcess(scriptFile, suffix string) (int, error) {
	command := shellCommand(scriptFile)
	cmd := exec.Command(command[0], command[1:]...)
	cmd.Dir = j.sourceDir
	cmd.Env = proc.MergeEnv(j.env)
	proc.SetProcessGroup(cmd)
	readEnd, writeEnd, err := os.Pipe()
	if err != nil {
		return -1, err
	}
	cmd.Stdout = writeEnd
	cmd.Stderr = writeEnd
	if err := cmd.Start(); err != nil {
		_ = readEnd.Close()
		_ = writeEnd.Close()
		return -1, err
	}
	_ = writeEnd.Close()
	j.trackProcess(cmd.Process.Pid)
	j.streamOutput(readEnd)
	_ = readEnd.Close()
	waitErr := cmd.Wait()
	j.trackProcess(0)
	exitCode := 0
	if waitErr != nil {
		var exitError *exec.ExitError
		if errors.As(waitErr, &exitError) {
			exitCode = exitError.ExitCode()
		} else {
			exitCode = -1
		}
	}
	// 被取消/超时杀掉时，退出码不可信，统一按语义错误返回。
	if err := j.checkCancelled(); err != nil {
		return exitCode, err
	}
	if waitErr != nil && exitCode < 0 {
		return exitCode, waitErr
	}
	return exitCode, nil
}

// streamOutput 逐行读取子进程输出并落日志：单行超过 MaxLineBytes 的部分直接丢弃，
// 避免构建输出（进度条、压缩包列表）把内存顶爆。
func (j *Job) streamOutput(readEnd *os.File) {
	reader := bufio.NewReaderSize(readEnd, 64*1024)
	line := make([]byte, 0, 4096)
	for {
		chunk, err := reader.ReadSlice('\n')
		if len(chunk) > 0 {
			if len(line) < MaxLineBytes {
				line = append(line, chunk...)
			}
		}
		if err == bufio.ErrBufferFull {
			continue
		}
		if len(line) > 0 {
			j.log.LogBytes(StageExecuteCommand, line)
			line = line[:0]
		}
		if err != nil {
			return
		}
	}
}
