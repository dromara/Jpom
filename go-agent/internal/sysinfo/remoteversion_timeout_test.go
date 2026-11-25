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
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"
)

// 未配置 JPOM_REMOTE_VERSION_TIMEOUT 时使用默认 10 秒（对应 Java RemoteVersion.timeoutMillis）。
func TestResolveRemoteVersionTimeoutDefault(t *testing.T) {
	unsetEnv(t, remoteVersionTimeoutEnv)
	timeout, warning := resolveRemoteVersionTimeout()
	if timeout != remoteVersionDefaultTimeout {
		t.Fatalf("expected default %v, got %v", remoteVersionDefaultTimeout, timeout)
	}
	if warning != "" {
		t.Fatalf("expected no warning for missing env, got %q", warning)
	}
}

// 配置合法秒数时按配置生效，前后空格忽略。
func TestResolveRemoteVersionTimeoutFromEnv(t *testing.T) {
	for _, raw := range []string{"3", " 3 ", "0003"} {
		t.Setenv(remoteVersionTimeoutEnv, raw)
		timeout, warning := resolveRemoteVersionTimeout()
		if timeout != 3*time.Second {
			t.Fatalf("env %q: expected 3s, got %v", raw, timeout)
		}
		if warning != "" {
			t.Fatalf("env %q: expected no warning, got %q", raw, warning)
		}
	}
}

// 非法值与小于等于 0 时回落默认值并给出告警文案（对应 Java 端 log.warn）。
func TestResolveRemoteVersionTimeoutInvalidFallsBack(t *testing.T) {
	for _, raw := range []string{"abc", "0", "-5", "1.5"} {
		t.Setenv(remoteVersionTimeoutEnv, raw)
		timeout, warning := resolveRemoteVersionTimeout()
		if timeout != remoteVersionDefaultTimeout {
			t.Fatalf("env %q: expected default %v, got %v", raw, remoteVersionDefaultTimeout, timeout)
		}
		if warning == "" {
			t.Fatalf("env %q: expected a warning", raw)
		}
	}
}

// 超时必须真正作用到版本信息请求上：服务端慢于超时即失败。
func TestLoadTransitURLHonorsTimeout(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(600 * time.Millisecond)
		_, _ = w.Write([]byte(`{"tagName":"v9.9.9","agentUrl":"http://x/a.zip","serverUrl":"http://x/s.zip"}`))
	}))
	defer server.Close()

	if _, err := loadTransitURL(server.URL, 100*time.Millisecond); err == nil {
		t.Fatal("expected timeout error for slow response")
	}
	value, err := loadTransitURL(server.URL, 5*time.Second)
	if err != nil {
		t.Fatalf("expected success with generous timeout, got %v", err)
	}
	if toString(value["tagName"]) != "v9.9.9" {
		t.Fatalf("unexpected tagName: %v", value["tagName"])
	}
}

// changelog 请求同样使用配置的超时，且失败不影响版本信息本身（对应 Java 端只告警）。
func TestFetchTextHonorsTimeout(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(600 * time.Millisecond)
		_, _ = w.Write([]byte("changelog"))
	}))
	defer server.Close()

	if _, err := fetchText(server.URL, 100*time.Millisecond); err == nil {
		t.Fatal("expected timeout error for slow changelog")
	}
	body, err := fetchText(server.URL, 5*time.Second)
	if err != nil {
		t.Fatalf("expected success with generous timeout, got %v", err)
	}
	if body != "changelog" {
		t.Fatalf("unexpected body: %q", body)
	}
}

func unsetEnv(t *testing.T, name string) {
	t.Helper()
	previous, existed := os.LookupEnv(name)
	if err := os.Unsetenv(name); err != nil {
		t.Fatalf("unset %s: %v", name, err)
	}
	t.Cleanup(func() {
		if existed {
			_ = os.Setenv(name, previous)
			return
		}
		_ = os.Unsetenv(name)
	})
}
