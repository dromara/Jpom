/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package i18n

import (
	"strconv"
	"strings"
	"testing"
)

// 与 Java messages_*.properties 抽样对照（逐字）。
func TestTFourLanguages(t *testing.T) {
	cases := map[string]string{
		ZhCN: "授权信息错误",
		ZhHK: "授權信息錯誤",
		ZhTW: "授權資訊錯誤",
		EnUS: "Authorization information error",
	}
	for lang, want := range cases {
		if got := T(lang, "i18n.auth_info_error.c184"); got != want {
			t.Errorf("T(%s) = %q, want %q", lang, got, want)
		}
	}
	if got := T("zh_CN", "i18n.upload_success.a769"); got != "上传成功" {
		t.Errorf("zh_CN alias = %q", got)
	}
	if got := T("zh-TW", "i18n.file_system_monitoring_exception.d4c0"); got != "檔案系統監控異常：" {
		t.Errorf("zh-TW = %q", got)
	}
}

func TestTUnknownFallback(t *testing.T) {
	// 未知语言归一化为 zh-CN。
	if got := T("fr-FR", "i18n.upload_success.a769"); got != "上传成功" {
		t.Errorf("fr = %q", got)
	}
	// 空语言同理。
	if got := T("", "i18n.save_succeeded.3b10"); got != "保存成功" {
		t.Errorf("empty = %q", got)
	}
	// 未知 key 原样返回，方便发现遗漏。
	if got := T(ZhCN, "i18n.not_exists.0000"); got != "i18n.not_exists.0000" {
		t.Errorf("missing key = %q", got)
	}
	if Lookup(ZhCN, "") != "" {
		t.Error("empty key should return empty string")
	}
	if Get("i18n.not_exists.0000") != nil {
		t.Error("missing key should return nil map")
	}
	entry := Get("i18n.auth_info_error.c184")
	if entry[ZhCN] != "授权信息错误" || entry[EnUS] != "Authorization information error" {
		t.Errorf("Get(key) = %v", entry)
	}
	if len(Keys()) < 1000 {
		t.Errorf("Keys() 过少: %d", len(Keys()))
	}
}

func TestPlaceholderReplacement(t *testing.T) {
	// 找一个带 {} 的 key 验证顺序替换。
	var key string
	for _, current := range Keys() {
		if strings.Contains(Lookup(ZhCN, current), "{}") {
			key = current
			break
		}
	}
	if key == "" {
		t.Skip("没有找到带占位符的文案")
	}
	placeholderCount := strings.Count(Lookup(ZhCN, key), "{}")
	args := make([]any, 0, placeholderCount)
	for index := 0; index < placeholderCount; index++ {
		args = append(args, "A"+strconv.Itoa(index))
	}
	text := T(ZhCN, key, args...)
	if strings.Contains(text, "{}") {
		t.Errorf("占位符未被替换: %q (key=%s)", text, key)
	}
	for index := range args {
		if !strings.Contains(text, "A"+strconv.Itoa(index)) {
			t.Errorf("参数 %d 未写入: %q (key=%s)", index, text, key)
		}
	}
	// 多参数不足时保留剩余 {}（与 Hutool StrUtil.format 一致）。
	if single := T(ZhCN, "i18n.file_merge_exception_details.e9d0", "only-one"); strings.Contains(single, "{}") {
		t.Logf("参数不足时保留占位符（符合预期）: %q", single)
	}
}

func TestBestLanguageAndResolve(t *testing.T) {
	cases := []struct {
		header string
		want   string
	}{
		{"zh-CN,zh;q=0.8,en;q=0.2", "zh-CN"},
		{"en-US,en;q=0.9,zh-CN;q=0.8", "en-US"},
		{"zh-HK", "zh-HK"},
		{"zh-TW,zh;q=0.9", "zh-TW"},
		{"", ""},
		{"en-GB;q=0.5", "en-GB"},
		// 权重相同取标签字典序最大（与 Java ComparatorChain 一致）。
		{"en-US,zh-CN", "zh-CN"},
	}
	for _, item := range cases {
		if got := BestLanguage(item.header); got != item.want {
			t.Errorf("BestLanguage(%q) = %q, want %q", item.header, got, item.want)
		}
	}
	if got := Resolve("en-US,en;q=0.9", "zh-CN"); got != EnUS {
		t.Errorf("Resolve = %q", got)
	}
	if got := Resolve("", "zh-TW"); got != ZhTW {
		t.Errorf("Resolve fallback = %q", got)
	}
	if got := Resolve("", ""); got != ZhCN {
		t.Errorf("Resolve default = %q", got)
	}
	t.Setenv(LanguageEnv, "en_US")
	if got := DefaultLang(); got != EnUS {
		t.Errorf("DefaultLang = %q", got)
	}
	if got := Resolve("", ""); got != EnUS {
		t.Errorf("Resolve env = %q", got)
	}
}

func TestNormalizeLang(t *testing.T) {
	cases := map[string]string{
		"zh":      ZhCN,
		"zh-cn":   ZhCN,
		"zh_CN":   ZhCN,
		"ZH-HK":   ZhHK,
		"zh-tw":   ZhTW,
		"zh-hant": ZhTW,
		"zh-hk":   ZhHK,
		"en":      EnUS,
		"en-US":   EnUS,
		"en_us":   EnUS,
		"fr":      ZhCN,
		"":        ZhCN,
	}
	for input, want := range cases {
		if got := NormalizeLang(input); got != want {
			t.Errorf("NormalizeLang(%q) = %q, want %q", input, got, want)
		}
	}
}

// TestEmbeddedMessagesIntegrity 校验嵌入数据完整（gen 脚本产出后 key 数以千计）。
func TestEmbeddedMessagesIntegrity(t *testing.T) {
	if len(Keys()) < 1000 {
		t.Fatalf("文案 key 过少: %d", len(Keys()))
	}
	if missing := MissingKeys(ZhCN); len(missing) > 0 {
		t.Errorf("zh-CN 缺失 %d 个 key: %v", len(missing), missing[:min(5, len(missing))])
	}
	if !Has("i18n.auth_info_error.c184") {
		t.Error("关键 key 缺失")
	}
	for _, lang := range Languages() {
		if len(MissingKeys(lang)) > 50 {
			t.Errorf("%s 缺失过多 key: %d", lang, len(MissingKeys(lang)))
		}
	}
}
