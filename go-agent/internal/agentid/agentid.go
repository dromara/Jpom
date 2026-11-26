//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

// Package agentid renders the outbound User-Agent shared with the Java agent.
//
// The literal template must stay identical to JpomUserAgent.TEMPLATE: both ends
// are pinned by the shared contract vectors in jpom-parent/contract/user-agent.tsv,
// and preflight-check.sh --ua fails when either side drifts.
package agentid

import (
	"fmt"
	"net/http"
	"os"
	"runtime"
	"strings"

	"github.com/dromara/Jpom/go-agent/internal/jpomheader"
)

const (
	// TypeAgent is the program type reported in the UA (Java Type.Agent enum name).
	TypeAgent = "Agent"
	// Unknown is the fallback used when a platform value is blank.
	Unknown = "Unknown"
	// HeaderName is the outbound identity header (single literal lives in jpomheader).
	HeaderName = jpomheader.UserAgent
	// Template mirrors JpomUserAgent.TEMPLATE; %s order is
	// osName, osVersion, arch, runtime, type, version.
	Template = "Mozilla/5.0 (%s %s; %s; %s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.0000.000 Safari/537.36 Jpom %s/%s"
	// unknownVersion is the fallback version (Java JpomManifest reports dev when unpacked).
	unknownVersion = "dev"
)

func fallback(value, def string) string {
	if strings.TrimSpace(value) == "" {
		return def
	}
	return value
}

// Build renders the UA from explicit platform values (contract vectors use this).
func Build(typeName, version, osName, osVersion, arch, runtimeVersion string) string {
	return fmt.Sprintf(Template,
		fallback(osName, Unknown),
		fallback(osVersion, "0"),
		fallback(arch, Unknown),
		fallback(runtimeVersion, Unknown),
		fallback(typeName, Unknown),
		fallback(version, unknownVersion))
}

// Host renders the UA for the running process.
func Host(version string) string {
	return Build(TypeAgent, version, OSName(), osVersion(), runtime.GOARCH, runtime.Version())
}

// OSName returns the Java compatible OS name ("Linux", "Windows", "Mac OS X").
// Container runs report "docker", exactly like the Java agent.
func OSName() string {
	if strings.TrimSpace(os.Getenv("JPOM_PKG")) != "" {
		return "docker"
	}
	switch runtime.GOOS {
	case "linux":
		return "Linux"
	case "windows":
		return "Windows"
	case "darwin":
		return "Mac OS X"
	default:
		return runtime.GOOS
	}
}

// osVersion is a best effort kernel release (Java reports the same segment).
func osVersion() string {
	if runtime.GOOS == "linux" {
		if raw, err := os.ReadFile("/proc/sys/kernel/osrelease"); err == nil {
			if value := strings.TrimSpace(string(raw)); value != "" {
				return value
			}
		}
	}
	return "0"
}

// Header builds request headers carrying the UA (websocket handshakes).
func Header(userAgent string) http.Header {
	if strings.TrimSpace(userAgent) == "" {
		return nil
	}
	return http.Header{HeaderName: {userAgent}}
}
