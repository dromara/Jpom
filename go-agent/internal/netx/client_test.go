//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package netx

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/agentid"
)

func TestClientInjectsUserAgent(t *testing.T) {
	seen := make(chan string, 2)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		seen <- r.Header.Get(agentid.HeaderName)
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	response, err := Client("ua-from-netx", 5*time.Second).Get(server.URL)
	if err != nil {
		t.Fatalf("get: %v", err)
	}
	_ = response.Body.Close()
	if got := <-seen; got != "ua-from-netx" {
		t.Fatalf("User-Agent = %q, want ua-from-netx", got)
	}

	// 调用方显式设置的 UA 优先（服务端转发/上游组件可覆盖）
	request, err := http.NewRequest(http.MethodGet, server.URL, nil)
	if err != nil {
		t.Fatalf("new request: %v", err)
	}
	request.Header.Set(agentid.HeaderName, "explicit-ua")
	response, err = Client("ua-from-netx", 5*time.Second).Do(request)
	if err != nil {
		t.Fatalf("do: %v", err)
	}
	_ = response.Body.Close()
	if got := <-seen; got != "explicit-ua" {
		t.Fatalf("User-Agent = %q, want explicit-ua", got)
	}
}
