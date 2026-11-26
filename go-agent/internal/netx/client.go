//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

// Package netx is the single assembly point for go-agent outbound HTTP clients.
//
// Every outbound request must go through Client: the Jpom User-Agent is injected
// here instead of being repeated at each call site (Java parity, see agentid).
package netx

import (
	"net/http"
	"strings"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/agentid"
)

// Client returns an outbound HTTP client carrying the Jpom User-Agent.
func Client(userAgent string, timeout time.Duration) *http.Client {
	return &http.Client{
		Timeout:   timeout,
		Transport: &userAgentTransport{base: http.DefaultTransport, userAgent: userAgent},
	}
}

type userAgentTransport struct {
	base      http.RoundTripper
	userAgent string
}

func (t *userAgentTransport) RoundTrip(req *http.Request) (*http.Response, error) {
	if strings.TrimSpace(t.userAgent) == "" || req.Header.Get(agentid.HeaderName) != "" {
		return t.base.RoundTrip(req)
	}
	clone := req.Clone(req.Context())
	clone.Header.Set(agentid.HeaderName, t.userAgent)
	return t.base.RoundTrip(clone)
}
