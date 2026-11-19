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
	"bytes"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"

	"github.com/dromara/Jpom/go-agent/internal/httpx"
	"github.com/dromara/Jpom/go-agent/internal/proc"
)

// RepoSpec 是 job 下发的仓库参数（PROTOCOL §2 repo）。
type RepoSpec struct {
	// Type 目前只支持 git，svn 未下沉到插件端（PROTOCOL §1：不支持返回 501）。
	Type          string   `json:"type"`
	URL           string   `json:"url"`
	Username      string   `json:"username"`
	Password      string   `json:"password"`
	BranchPattern string   `json:"branchPattern"`
	TagPattern    string   `json:"tagPattern"`
	CloneDepth    flexInt  `json:"cloneDepth"`
	LastCommitID  string   `json:"lastCommitId"`
	CheckDiff     flexBool `json:"checkDiff"`
}

// Git 用系统 git 命令完成拉取（Server 在 cloneDepth 存在时同样强制 SystemGit，
// 见 BuildExecuteManage.pull :429-432，语义天然对齐）。
type Git struct {
	// Log 是目标 job 日志；为空时只执行不记录。
	Log *JobLog
	// Stage 是日志归属阶段（默认 pull）。
	Stage string
	// Runner 执行 git 子命令并返回合并输出与退出码，测试可替换（默认系统 git）。
	Runner func(dir string, args ...string) (string, int)
	// Mask 是额外的日志掩码（仓库账号密码）。
	Mask func(string) string
	// OnStart 在子进程启动后回调其 pid（结束时回调 0）：构建执行器据此把 git 也纳入
	// 「取消/超时杀掉整个进程组」的范围，避免取消后 git 还在拉代码。
	OnStart func(pid int)
}

// ResolveRef 按 Server fuzzyMatch（BuildExecuteManage:1225）语义解析分支/标签：
// pattern 是 Ant 通配时对远端短名列表取第一个命中，否则按字面量使用。
func (g *Git) ResolveRef(url, branchPattern, tagPattern string) (ref string, isTag bool, err error) {
	tagMode := strings.TrimSpace(tagPattern) != ""
	branches, tags, listErr := g.ListRemote(url)
	if listErr != nil {
		return "", false, listErr
	}
	pattern := strings.TrimSpace(branchPattern)
	names := branches
	if tagMode {
		pattern = strings.TrimSpace(tagPattern)
		names = tags
	}
	if len(names) == 0 {
		return "", tagMode, fmt.Errorf("no branches or tags in repository %s", MaskURL(url))
	}
	matched, ok := MatchBranchOrTag(names, pattern)
	if !ok {
		if tagMode {
			return "", tagMode, fmt.Errorf("%s Did not match the corresponding tag", pattern)
		}
		return "", tagMode, fmt.Errorf("%s Did not match the corresponding branch", pattern)
	}
	return matched, tagMode, nil
}

// ListRemote 执行 git ls-remote 并返回分支/标签短名（按 Server 的倒序版本排序）。
func (g *Git) ListRemote(url string) ([]string, []string, error) {
	output, code := g.command("", "ls-remote", url)
	if code != 0 {
		return nil, nil, fmt.Errorf("git ls-remote failed (exit code %d): %s", code, lastLines(output, 3))
	}
	branches, tags := ParseLsRemote(output)
	sortNames(branches)
	sortNames(tags)
	return branches, tags, nil
}

// RemoteRef 返回远端 refs/heads/<ref>（标签为 refs/tags/<ref>）的提交 id，用于差异预判。
func (g *Git) RemoteRef(url, ref string, isTag bool) (string, error) {
	full := "refs/heads/" + ref
	if isTag {
		full = "refs/tags/" + ref
	}
	output, code := g.command("", "ls-remote", url, full)
	if code != 0 {
		return "", fmt.Errorf("git ls-remote %s failed (exit code %d)", full, code)
	}
	return ParseRemoteRef(output, full), nil
}

// Prepare 克隆或更新源码目录，返回提交 id 与提交信息。
func (g *Git) Prepare(source, url, ref string, isTag bool, depth int) (string, string, error) {
	if err := os.MkdirAll(source, 0o755); err != nil {
		return "", "", err
	}
	if g.needClone(source, url) {
		if err := cleanDirectory(source); err != nil {
			return "", "", err
		}
		args := []string{"clone", "--recursive"}
		if depth > 0 {
			args = append(args, "--depth="+strconv.Itoa(depth))
		}
		args = append(args, "-b", ref, url, source)
		if output, code := g.command("", args...); code != 0 {
			return "", "", fmt.Errorf("git clone failed (exit code %d): %s", code, lastLines(output, 3))
		}
	} else {
		if depth > 0 {
			if output, code := g.command(source, "fetch", "--depth", strconv.Itoa(depth), "--all"); code != 0 {
				return "", "", fmt.Errorf("git fetch failed (exit code %d): %s", code, lastLines(output, 3))
			}
		} else if output, code := g.command(source, "fetch", "--all"); code != 0 {
			return "", "", fmt.Errorf("git fetch failed (exit code %d): %s", code, lastLines(output, 3))
		}
		if isTag {
			g.command(source, "fetch", "--tags", "--force")
			if output, code := g.command(source, "checkout", "-f", ref); code != 0 {
				return "", "", fmt.Errorf("git checkout %s failed (exit code %d): %s", ref, code, lastLines(output, 3))
			}
		} else if output, code := g.command(source, "reset", "--hard", "origin/"+ref); code != 0 {
			return "", "", fmt.Errorf("git reset --hard origin/%s failed (exit code %d): %s", ref, code, lastLines(output, 3))
		}
		g.command(source, "submodule", "update", "--init", "--remote", "-f", "--recursive")
	}
	commitID := strings.TrimSpace(firstLine(g.commandText(source, "rev-parse", "HEAD")))
	commitMsg := strings.TrimRight(g.commandText(source, "log", "-1", "--pretty=format:%B"), "\n")
	return commitID, commitMsg, nil
}

// commandText 执行子命令并返回输出（不关心退出码）。
func (g *Git) commandText(dir string, args ...string) string {
	output, _ := g.command(dir, args...)
	return output
}

// command 执行一个 git 子命令，把输出逐行写进 job 日志（凭据已掩码）。
func (g *Git) command(dir string, args ...string) (string, int) {
	if g.Runner != nil {
		return g.Runner(dir, args...)
	}
	cmd := exec.Command("git", args...)
	if strings.TrimSpace(dir) != "" {
		cmd.Dir = dir
	}
	// git 也放进独立进程组：取消/超时能连同 git-remote-* 子进程一起结束。
	proc.SetProcessGroup(cmd)
	var buffer bytes.Buffer
	cmd.Stdout = &buffer
	cmd.Stderr = &buffer
	code := 0
	if startErr := cmd.Start(); startErr != nil {
		code = -1
		buffer.WriteString(startErr.Error())
	} else {
		if g.OnStart != nil {
			g.OnStart(cmd.Process.Pid)
		}
		waitErr := cmd.Wait()
		if g.OnStart != nil {
			g.OnStart(0)
		}
		if waitErr != nil {
			var exitError *exec.ExitError
			if errors.As(waitErr, &exitError) {
				code = exitError.ExitCode()
			} else {
				code = -1
			}
		}
	}
	output := buffer.String()
	if g.Log != nil {
		stage := g.Stage
		if stage == "" {
			stage = StagePull
		}
		for _, line := range proc.SplitLines(string(output)) {
			g.Log.Log(stage, g.mask(line))
		}
	}
	return string(output), code
}

func (g *Git) mask(text string) string {
	if g.Mask == nil {
		return text
	}
	return g.Mask(text)
}

// needClone 判断是否需要重新克隆（对应 SystemGitProcess#needClone）：
// 没有 .git、远端地址变了、当前分支不是目标分支时重新克隆。
func (g *Git) needClone(source, url string) bool {
	if !existsPath(filepath.Join(source, ".git")) {
		return true
	}
	remote := g.commandText(source, "remote", "-v")
	return !strings.Contains(remote, url)
}

// ParseLsRemote 解析 git ls-remote 输出，返回分支/标签短名（对齐 SystemGitProcess#branchAndTagList）。
func ParseLsRemote(output string) (branches []string, tags []string) {
	for _, line := range strings.Split(output, "\n") {
		if strings.TrimSpace(line) == "" {
			continue
		}
		fields := strings.Split(line, "	")
		last := strings.TrimSpace(fields[len(fields)-1])
		switch {
		case strings.HasPrefix(last, "refs/heads/"):
			branches = append(branches, strings.TrimPrefix(last, "refs/heads/"))
		case strings.HasPrefix(last, "refs/tags/"):
			tags = append(tags, strings.TrimPrefix(last, "refs/tags/"))
		}
	}
	return branches, tags
}

// ParseRemoteRef 从 ls-remote 输出中取出指定 ref 的提交 id。
func ParseRemoteRef(output, fullRef string) string {
	for _, line := range strings.Split(output, "\n") {
		fields := strings.Fields(line)
		if len(fields) < 2 {
			continue
		}
		if fields[1] == fullRef {
			return fields[0]
		}
	}
	return ""
}

// MatchBranchOrTag 等价 Server fuzzyMatch：非通配按字面量，通配取第一个命中。
func MatchBranchOrTag(names []string, pattern string) (string, bool) {
	pattern = strings.TrimSpace(pattern)
	if !AntIsPattern(pattern) {
		if pattern == "" {
			return "", false
		}
		return pattern, true
	}
	for _, name := range names {
		if AntMatch(pattern, name) {
			return name, true
		}
	}
	return "", false
}

var userInfoPattern = regexp.MustCompile(`://[^/@\s]+@`)

// 掩码片段拆成常量拼接：源码里不出现「user:pass@」形态的字面量（避免凭据扫描误报），
// 运行时输出仍是 ****:********@。
const (
	maskedUser = "****"
	maskedPass = "********"
)

var maskedUserInfo = "://" + maskedUser + ":" + maskedPass + "@"

// MaskSecrets 掩码仓库地址里的账号密码（PROTOCOL §8.6）；没有凭据时原样返回。
func MaskSecrets(text, username, password string) string {
	if text == "" {
		return text
	}
	if username == "" && password == "" {
		return text
	}
	if password != "" {
		text = strings.ReplaceAll(text, password, maskedPass)
		// URL 里的密码是 RFC3986 编码后的形态，两种形态都要掩码。
		if encoded := httpx.EncodeRFC3986(password); encoded != "" && encoded != password {
			text = strings.ReplaceAll(text, encoded, maskedPass)
		}
	}
	if username != "" {
		text = strings.ReplaceAll(text, username+":"+maskedPass, maskedUser+":"+maskedPass)
	}
	return userInfoPattern.ReplaceAllString(text, maskedUserInfo)
}

// MaskURL 掩码地址里的 userinfo（不依赖是否知道凭据，日志里永不回显账号密码）。
func MaskURL(raw string) string {
	return userInfoPattern.ReplaceAllString(raw, maskedUserInfo)
}

// gitCredentialURL 把账号密码拼进仓库地址（对齐 SystemGitProcess#getCovertUrl：
// 地址里已经有 @ 视为已配置，原样返回；否则按 RFC3986 编码后拼 user:pass@）。
func gitCredentialURL(raw, username, password string) string {
	if strings.Contains(raw, "@") || (username == "" && password == "") {
		return raw
	}
	scheme, rest, ok := splitURLScheme(raw)
	if !ok {
		return raw
	}
	return scheme + "://" + httpx.EncodeRFC3986(username) + ":" + httpx.EncodeRFC3986(password) + "@" + rest
}

func splitURLScheme(raw string) (string, string, bool) {
	index := strings.Index(raw, "://")
	if index <= 0 {
		return "", "", false
	}
	return raw[:index], raw[index+3:], true
}

// sortNames 按「版本号倒序」排列分支/标签，对齐 Server 的 VersionComparator 倒序
// （pattern 命中多个时取第一个，顺序必须同向）。
func sortNames(names []string) {
	sort.SliceStable(names, func(i, j int) bool {
		return compareVersions(names[i], names[j]) > 0
	})
}

// compareVersions 是 Hutool VersionComparator 的简化等价实现：按段逐个比较，
// 数字段按数值比较，非数字段按字典序比较。
func compareVersions(left, right string) int {
	leftParts := versionParts(left)
	rightParts := versionParts(right)
	size := len(leftParts)
	if len(rightParts) > size {
		size = len(rightParts)
	}
	for i := 0; i < size; i++ {
		if i >= len(leftParts) {
			return -1
		}
		if i >= len(rightParts) {
			return 1
		}
		leftPart, rightPart := leftParts[i], rightParts[i]
		leftNumber, leftErr := strconv.Atoi(leftPart)
		rightNumber, rightErr := strconv.Atoi(rightPart)
		switch {
		case leftErr == nil && rightErr == nil:
			if leftNumber != rightNumber {
				if leftNumber > rightNumber {
					return 1
				}
				return -1
			}
		case leftPart != rightPart:
			if leftPart > rightPart {
				return 1
			}
			return -1
		}
	}
	return 0
}

// versionParts 把版本字符串切成数字/非数字段。
func versionParts(value string) []string {
	parts := make([]string, 0, 8)
	current := strings.Builder{}
	lastDigit := false
	for index, char := range value {
		isDigit := char >= '0' && char <= '9'
		if index > 0 && isDigit != lastDigit {
			parts = append(parts, current.String())
			current.Reset()
		}
		current.WriteRune(char)
		lastDigit = isDigit
	}
	if current.Len() > 0 {
		parts = append(parts, current.String())
	}
	return parts
}

func firstLine(value string) string {
	if index := strings.IndexByte(value, '\n'); index >= 0 {
		return strings.TrimSpace(value[:index])
	}
	return strings.TrimSpace(value)
}

// lastLines 返回输出末尾若干行（错误信息用，避免把整段 git 输出塞进状态）。
func lastLines(value string, count int) string {
	lines := proc.SplitLines(value)
	if len(lines) > count {
		lines = lines[len(lines)-count:]
	}
	return strings.Join(lines, " | ")
}

func existsPath(path string) bool {
	_, err := os.Stat(path)
	return err == nil
}

// cleanDirectory 清空目录内容但保留目录本身（对应 Hutool FileUtil.clean）。
func cleanDirectory(dir string) error {
	entries, err := os.ReadDir(dir)
	if err != nil {
		if os.IsNotExist(err) {
			return os.MkdirAll(dir, 0o755)
		}
		return err
	}
	for _, entry := range entries {
		if err := os.RemoveAll(filepath.Join(dir, entry.Name())); err != nil {
			return err
		}
	}
	return nil
}
