//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package projectfile

import "testing"

// 回归：一条 allowEditSuffix 里塞了多个 "suffix@charset"（历史脚本/旧前端）时，
// 必须与 Java AgentWhitelist#parserFileSuffixMap 一致：pattern 取第一个 @ 之前，
// charset 取最后一个 @ 之后，不能把 "UTF-8,.txt@UTF-8" 当成字体名而报错。
func TestParseEditSuffixMultipleAt(t *testing.T) {
	rules := parseEditSuffix([]string{".conf@UTF-8,.txt@UTF-8"})
	if len(rules) != 1 {
		t.Fatalf("rules = %d, want 1", len(rules))
	}
	if rules[0].pattern != ".conf" {
		t.Errorf("pattern = %q, want .conf", rules[0].pattern)
	}
	if rules[0].charset != "UTF-8" {
		t.Errorf("charset = %q, want UTF-8", rules[0].charset)
	}
	charset, err := checkFileSuffix([]string{".conf@UTF-8,.txt@UTF-8"}, "app.conf")
	if err != nil {
		t.Fatalf("checkFileSuffix error: %v", err)
	}
	if normalizeCharset(charset) != "UTF-8" {
		t.Errorf("charset = %q, want UTF-8", charset)
	}
}
