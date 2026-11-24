//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package sysinfo

import (
	"errors"
	"testing"
	"time"
)

// 在线刷新首次失败、第二次成功时应继续动作（不再直接 405）。
func TestResolveRemoteInfoRetriesThenSucceeds(t *testing.T) {
	attempts := 0
	fetch := func() (map[string]any, error) {
		attempts++
		if attempts < 2 {
			return nil, errors.New("SSLException: Connection reset")
		}
		return map[string]any{"tagName": "v2.11.14.7"}, nil
	}
	value, err := resolveRemoteInfo(fetch, func() map[string]any { return nil },
		func(time.Duration) {}, func(string) {})
	if err != nil {
		t.Fatalf("expected success after retry, got error: %v", err)
	}
	if toString(value["tagName"]) != "v2.11.14.7" {
		t.Fatalf("unexpected tagName: %v", value["tagName"])
	}
	if attempts != 2 {
		t.Fatalf("expected 2 fetch attempts, got %d", attempts)
	}
}

// 连续失败时回退本机缓存，并恰好尝试 3 次。
func TestResolveRemoteInfoFallsBackToCache(t *testing.T) {
	attempts := 0
	fetch := func() (map[string]any, error) {
		attempts++
		return nil, errors.New("SSLException: Connection reset")
	}
	value, err := resolveRemoteInfo(fetch, func() map[string]any {
		return map[string]any{"tagName": "v2.11.14.6"}
	}, func(time.Duration) {}, func(string) {})
	if err != nil {
		t.Fatalf("expected cache fallback, got error: %v", err)
	}
	if toString(value["tagName"]) != "v2.11.14.6" {
		t.Fatalf("unexpected fallback tagName: %v", value["tagName"])
	}
	if attempts != remoteVersionFetchAttempts {
		t.Fatalf("expected %d fetch attempts, got %d", remoteVersionFetchAttempts, attempts)
	}
}

// 既无在线信息又无缓存时返回最后一次错误（由调用方转成可读文案）。
func TestResolveRemoteInfoFailsWithoutCache(t *testing.T) {
	fetchErr := errors.New("SSLException: Connection reset")
	_, err := resolveRemoteInfo(func() (map[string]any, error) { return nil, fetchErr },
		func() map[string]any { return nil }, func(time.Duration) {}, func(string) {})
	if !errors.Is(err, fetchErr) {
		t.Fatalf("expected last fetch error, got %v", err)
	}
}

// 返回内容缺少 tagName 时视为不可用（与 Java 端一致）。
func TestResolveRemoteInfoTreatsEmptyTagNameAsFailure(t *testing.T) {
	_, err := resolveRemoteInfo(func() (map[string]any, error) {
		return map[string]any{"agentUrl": "https://example.com/agent.zip"}, nil
	}, func() map[string]any { return nil }, func(time.Duration) {}, func(string) {})
	if !errors.Is(err, errNoRemoteVersionInfo) {
		t.Fatalf("expected errNoRemoteVersionInfo, got %v", err)
	}
}
