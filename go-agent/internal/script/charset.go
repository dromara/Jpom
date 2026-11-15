/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package script

import (
	"strings"

	"github.com/dromara/Jpom/go-agent/internal/proc"
)

// Charset names supported by the agent (the Java agent relies on the JVM
// charset registry; the agent only needs the encodings used by Jpom users).
const (
	CharsetUTF8   = "UTF-8"
	CharsetGBK    = "GBK"
	CharsetGB1803 = "GB18030"
	CharsetLatin1 = "ISO-8859-1"
)

// PlatformCharset mirrors Hutool CharsetUtil.systemCharset(): the JVM default
// charset, GBK on Windows and UTF-8 everywhere else.
func PlatformCharset() string { return proc.DefaultSystemCharset() }

// NormalizeCharset maps a Java charset name to the canonical name supported by
// the agent (the charset registry lives in internal/proc, shared with the
// project log domain). It reports false for empty/unknown values so the caller
// can apply the Java fallback chain.
func NormalizeCharset(name string) (string, bool) {
	cleaned := strings.TrimSpace(name)
	if cleaned == "" {
		return "", false
	}
	upper := strings.ToUpper(cleaned)
	upper = strings.ReplaceAll(upper, "_", "-")
	switch upper {
	case "UTF-8", "UTF8", "UNICODE-1-1-UTF-8", "GBK", "CP936", "GB2312", "GB-2312", "EUC-CN", "MS936", "WINDOWS-936",
		"GB18030", "GB-18030", "BIG5", "BIG-5", "CP950", "WINDOWS-950",
		"UTF-16", "UTF16", "UTF-16LE", "UTF16LE", "UTF-16BE", "UTF16BE",
		"ISO-8859-1", "ISO8859-1", "LATIN1", "ISO-LATIN-1",
		"WINDOWS-1252", "CP1252", "US-ASCII", "ASCII":
		return proc.NormalizeCharset(cleaned, CharsetUTF8), true
	}
	return "", false
}

// ResolveCharset resolves a configured charset with a fallback charset
// (Java ObjectUtil.defaultIfNull(charset, fallback) semantics).
func ResolveCharset(configured, fallback string) string {
	if resolved, ok := NormalizeCharset(configured); ok {
		return resolved
	}
	if resolved, ok := NormalizeCharset(fallback); ok {
		return resolved
	}
	return CharsetUTF8
}

// ConsoleCharset resolves jpom.system.console-charset
// (ExtConfigBean.getConsoleLogCharset: platform default when absent).
func ConsoleCharset(configured string) string {
	return ResolveCharset(configured, PlatformCharset())
}

// SystemLogCharset resolves jpom.system.log-charset
// (BaseSystemConfig.getLogCharset: UTF-8 when absent).
func SystemLogCharset(configured string) string {
	return ResolveCharset(configured, CharsetUTF8)
}

// ProjectLogCharset resolves the project console log charset
// (ProjectInfoService.resolveLogCharset: project logCharset, then
// jpom.project.log.file-charset, then Java's Windows GBK / other UTF-8 default).
func ProjectLogCharset(projectCharset, configured string) string {
	if resolved, ok := NormalizeCharset(projectCharset); ok {
		return resolved
	}
	if resolved, ok := NormalizeCharset(configured); ok {
		return resolved
	}
	return PlatformCharset()
}

// DecodeText converts bytes encoded with charset into a UTF-8 Go string.
// Invalid sequences are replaced with the Unicode replacement character, matching
// the JVM decoder behaviour (CharsetDecoder default REPLACE action).
func DecodeText(data []byte, charset string) string {
	normalized, _ := NormalizeCharset(charset)
	if normalized == "" || normalized == CharsetUTF8 {
		return string(data)
	}
	decoded, err := proc.DecodeString(normalized, data)
	if err != nil {
		// 非法字节序列：与 JVM decoder REPLACE 语义接近的兜底（不 panic）
		return string(data)
	}
	return decoded
}

// DecodeLine decodes one log line (newline already stripped).
func DecodeLine(data []byte, charset string) string { return DecodeText(data, charset) }

// EncodeText converts a UTF-8 Go string into bytes of charset (used when writing
// the temporary script files: the Java agent writes them with the console
// charset). Unmappable runes become '?' like the JVM encoder replacement.
func EncodeText(text string, charset string) ([]byte, error) {
	normalized, _ := NormalizeCharset(charset)
	if normalized == "" || normalized == CharsetUTF8 {
		return []byte(text), nil
	}
	encoded, err := proc.EncodeString(normalized, text)
	if err == nil {
		return encoded, nil
	}
	// 逐字符编码，无法映射的字符按 JVM 行为替换为 '?'
	var buffer []byte
	for _, current := range text {
		single, singleErr := proc.EncodeString(normalized, string(current))
		if singleErr != nil {
			buffer = append(buffer, '?')
			continue
		}
		buffer = append(buffer, single...)
	}
	return buffer, nil
}
