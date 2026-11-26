//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package agentid

import (
	"bufio"
	"os"
	"runtime"
	"strings"
	"testing"
)

const contractPath = "../../../contract/user-agent.tsv"

// TestContractVector pins the Go implementation to the shared Java/Go contract:
// type, version, osName, osVersion, arch, runtime, expect.
func TestContractVector(t *testing.T) {
	file, err := os.Open(contractPath)
	if err != nil {
		t.Fatalf("open contract %s: %v", contractPath, err)
	}
	defer func() { _ = file.Close() }()
	checked := 0
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		if strings.TrimSpace(line) == "" || strings.HasPrefix(line, "#") {
			continue
		}
		parts := strings.Split(line, "\t")
		if len(parts) != 7 {
			t.Fatalf("contract row must have 7 columns: %q", line)
		}
		actual := Build(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5])
		if actual != parts[6] {
			t.Fatalf("UA drift with contract row %q\n  got  %s\n  want %s", line, actual, parts[6])
		}
		checked++
	}
	if err := scanner.Err(); err != nil {
		t.Fatalf("scan contract: %v", err)
	}
	if checked < 5 {
		t.Fatalf("contract vectors must not be empty, got %d rows", checked)
	}
}

func TestHostUsesAgentType(t *testing.T) {
	ua := Host("2.11.15.3")
	if !strings.HasPrefix(ua, "Mozilla/5.0 (") {
		t.Fatalf("unexpected UA: %s", ua)
	}
	if !strings.HasSuffix(ua, " Jpom Agent/2.11.15.3") {
		t.Fatalf("unexpected UA suffix: %s", ua)
	}
	if !strings.Contains(ua, "; "+runtime.GOARCH+"; ") {
		t.Fatalf("arch segment missing: %s", ua)
	}
	if !strings.Contains(ua, runtime.Version()) {
		t.Fatalf("runtime segment missing: %s", ua)
	}
}

func TestHeaderForDial(t *testing.T) {
	if got := Header(" "); got != nil {
		t.Fatalf("blank UA must not produce headers, got %v", got)
	}
	headers := Header("ua-value")
	if headers.Get(HeaderName) != "ua-value" {
		t.Fatalf("header = %v", headers)
	}
}
