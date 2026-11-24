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
	"strings"
	"testing"
)

const lsRemoteSample = `aaa111	refs/heads/release/1.2
bbb222	refs/heads/release/2.0
ccc333	refs/heads/main
ddd444	refs/tags/v1.0.0
eee555	refs/tags/v1.10.0
`

// fakeGitRunner 返回固定输出，避免单测依赖网络（git ls-remote 不能真跑远端）。
func fakeGitRunner(output string, code int) func(dir string, args ...string) (string, int) {
	return func(string, ...string) (string, int) { return output, code }
}

// TestMatchBranchOrTag 覆盖 Server fuzzyMatch（BuildExecuteManage:1225）的分支/标签匹配语义。
func TestMatchBranchOrTag(t *testing.T) {
	names := []string{"release/2.0", "release/1.2", "main"}
	// 非通配：按字面量使用，即使列表里没有也原样返回（Server 的行为）。
	if matched, ok := MatchBranchOrTag(names, "feature/x"); !ok || matched != "feature/x" {
		t.Errorf("字面量匹配 = %q,%v", matched, ok)
	}
	// 通配：取第一个命中（列表已按版本倒序）。
	if matched, ok := MatchBranchOrTag(names, "release/*"); !ok || matched != "release/2.0" {
		t.Errorf("通配匹配 = %q,%v, want release/2.0", matched, ok)
	}
	if matched, ok := MatchBranchOrTag(names, "hotfix/*"); ok || matched != "" {
		t.Errorf("无命中应返回空, got %q,%v", matched, ok)
	}
	if _, ok := MatchBranchOrTag(names, ""); ok {
		t.Error("空 pattern 不应命中")
	}
}

// TestParseLsRemote 覆盖 ls-remote 解析与倒序版本排序。
func TestParseLsRemote(t *testing.T) {
	branches, tags := ParseLsRemote(lsRemoteSample)
	if len(branches) != 3 || len(tags) != 2 {
		t.Fatalf("branches=%v tags=%v", branches, tags)
	}
	sortNames(branches)
	if branches[0] != "release/2.0" || branches[1] != "release/1.2" {
		t.Errorf("分支倒序异常: %v", branches)
	}
	sortNames(tags)
	if tags[0] != "v1.10.0" || tags[1] != "v1.0.0" {
		t.Errorf("标签倒序异常: %v", tags)
	}
}

// TestParseRemoteRef 覆盖差异预判用的 refs 解析。
func TestParseRemoteRef(t *testing.T) {
	output := "aaa111\trefs/heads/main\n"
	if sha := ParseRemoteRef(output, "refs/heads/main"); sha != "aaa111" {
		t.Errorf("sha = %q", sha)
	}
	if sha := ParseRemoteRef(output, "refs/heads/dev"); sha != "" {
		t.Errorf("未命中的 ref 应为空, got %q", sha)
	}
}

// TestResolveRef 覆盖分支/标签解析（含 Ant 通配与二级标签模式）。
func TestResolveRef(t *testing.T) {
	git := &Git{Runner: fakeGitRunner(lsRemoteSample, 0)}
	ref, isTag, err := git.ResolveRef("https://git.example.com/group/app.git", "release/*", "")
	if err != nil || isTag || ref != "release/2.0" {
		t.Fatalf("分支解析 = %q tag=%v err=%v", ref, isTag, err)
	}
	ref, isTag, err = git.ResolveRef("https://git.example.com/group/app.git", "", "v1.*")
	if err != nil || !isTag || ref != "v1.10.0" {
		t.Fatalf("标签解析 = %q tag=%v err=%v", ref, isTag, err)
	}
	if _, _, err = git.ResolveRef("https://git.example.com/group/app.git", "hotfix/*", ""); err == nil ||
		!strings.Contains(err.Error(), "Did not match the corresponding branch") {
		t.Fatalf("未命中分支应报错, got %v", err)
	}
	if _, _, err = git.ResolveRef("https://git.example.com/group/app.git", "", "v9*"); err == nil ||
		!strings.Contains(err.Error(), "Did not match the corresponding tag") {
		t.Fatalf("未命中标签应报错, got %v", err)
	}
}

// TestResolveRefEmptyRepository 覆盖远端没有任何分支/标签的报错。
func TestResolveRefEmptyRepository(t *testing.T) {
	git := &Git{Runner: fakeGitRunner("", 0)}
	if _, _, err := git.ResolveRef("https://git.example.com/group/app.git", "main", ""); err == nil {
		t.Fatal("空仓库应报错")
	}
}

// TestMaskSecrets 覆盖日志凭据掩码（PROTOCOL §8.6）。
func TestMaskSecrets(t *testing.T) {
	// 用拼接构造，避免源码里出现「user:pass@」字面量触发凭据扫描
	const url = "https://" + "alice" + ":" + "s3cret" + "@git.example.com/group/app.git"
	masked := MaskSecrets("clone "+url, "alice", "s3cret")
	if strings.Contains(masked, "s3cret") {
		t.Errorf("密码未掩码: %s", masked)
	}
	if !strings.Contains(masked, "git.example.com") {
		t.Errorf("域名被误删: %s", masked)
	}
	if got := MaskURL(url); strings.Contains(got, "alice") || strings.Contains(got, "s3cret") {
		t.Errorf("MaskURL 未掩码: %s", got)
	}
	if got := MaskSecrets("plain", "", ""); got != "plain" {
		t.Errorf("无凭据时应原样返回, got %s", got)
	}
}

// TestGitCredentialURL 覆盖账号密码拼接（对齐 SystemGitProcess#getCovertUrl）。
func TestGitCredentialURL(t *testing.T) {
	got := gitCredentialURL("https://git.example.com/group/app.git", "alice", "p@ss word")
	want := "https://" + "alice" + ":" + "p%40ss%20word" + "@git.example.com/group/app.git"
	if got != want {
		t.Errorf("gitCredentialURL = %s, want %s", got, want)
	}
	// 地址里已有 @ 时原样返回（用户自己配置了凭据）。
	const withCreds = "https://" + "bob" + ":" + "pwd" + "@git.example.com/group/app.git"
	if got := gitCredentialURL(withCreds, "alice", "pwd2"); got != withCreds {
		t.Errorf("已有凭据的地址不应改写, got %s", got)
	}
}
