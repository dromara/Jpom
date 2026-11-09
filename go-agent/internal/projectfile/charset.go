/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package projectfile

import (
	"bytes"
	"encoding/binary"
	"fmt"
	"regexp"
	"strings"
	"unicode/utf16"
)

// editSuffixRule is one entry of whitelist allowEditSuffix. Java supports the
// form "suffix@charset" (the charset is optional) and regular expressions.
type editSuffixRule struct {
	// pattern is the part before the optional "@charset".
	pattern string
	// charset is the Java charset name, empty means the platform default.
	charset string
}

// parseEditSuffix splits allowEditSuffix into rules, same semantics as
// AgentWhitelist#parserFileSuffixMap.
func parseEditSuffix(suffixes []string) []editSuffixRule {
	rules := make([]editSuffixRule, 0, len(suffixes))
	for _, item := range suffixes {
		item = strings.TrimSpace(item)
		if item == "" {
			continue
		}
		rule := editSuffixRule{}
		if index := strings.IndexByte(item, '@'); index >= 0 {
			rule.pattern = item[:index]
			rule.charset = item[index+1:]
		} else {
			rule.pattern = item
		}
		if rule.pattern == "" {
			continue
		}
		rules = append(rules, rule)
	}
	return rules
}

// editCharset returns the charset configured for filename, or nil when the file
// is not editable (AgentWhitelist#parserFileSuffixMap).
func editCharset(suffixes []string, filename string) *editSuffixRule {
	rules := parseEditSuffix(suffixes)
	// "*" applies to every file.
	for index := range rules {
		if rules[index].pattern == "*" {
			return &rules[index]
		}
	}
	for index := range rules {
		rule := &rules[index]
		lowerName := strings.ToLower(filename)
		lowerPattern := strings.ToLower(rule.pattern)
		if strings.HasSuffix(lowerName, lowerPattern) || strings.HasSuffix(lowerName, "."+lowerPattern) {
			return rule
		}
		if matched, err := regexp.MatchString(rule.pattern, filename); err == nil && matched {
			return rule
		}
	}
	return nil
}

// checkSilentFileSuffix mirrors AgentWhitelist#checkSilentFileSuffix.
func checkSilentFileSuffix(suffixes []string, filename string) bool {
	if len(suffixes) == 0 {
		return false
	}
	return editCharset(suffixes, filename) != nil
}

// checkFileSuffix mirrors AgentWhitelist#checkFileSuffix and returns the
// charset name to use when reading or writing the file.
func checkFileSuffix(suffixes []string, filename string) (string, error) {
	if len(suffixes) == 0 {
		return "", invalidf("没有配置可允许编辑的后缀")
	}
	rule := editCharset(suffixes, filename)
	if rule == nil {
		return "", invalidf("不允许编辑的文件后缀")
	}
	return rule.charset, nil
}

// normalizeCharset maps the Java charset names used by Jpom to a canonical form.
func normalizeCharset(name string) string {
	value := strings.ToUpper(strings.TrimSpace(name))
	value = strings.ReplaceAll(value, "_", "-")
	switch value {
	case "", "DEFAULT", "UTF8", "UTF-8":
		return "UTF-8"
	case "GBK", "GB2312", "GB18030", "CP936":
		return "GBK"
	case "UTF16", "UTF-16":
		return "UTF-16"
	case "UTF16LE", "UTF-16LE", "UNICODELITTLE":
		return "UTF-16LE"
	case "UTF16BE", "UTF-16BE", "UNICODEBIG":
		return "UTF-16BE"
	case "ASCII", "US-ASCII":
		return "US-ASCII"
	case "ISO8859-1", "ISO-8859-1", "LATIN1":
		return "ISO-8859-1"
	default:
		return value
	}
}

// decodeText decodes file bytes with the configured charset. GBK family codecs
// are provided by charset_gbk.go; when unavailable a clear error is returned.
func decodeText(data []byte, charset string) (string, error) {
	switch normalizeCharset(charset) {
	case "UTF-8":
		// Files written by the Java agent are UTF-8 by default; be forgiving
		// about a missing/incorrect byte sequence like the Java decoder.
		return strings.ToValidUTF8(string(data), "�"), nil
	case "GBK":
		return decodeGBK(data)
	case "ISO-8859-1":
		runes := make([]rune, len(data))
		for index, value := range data {
			runes[index] = rune(value)
		}
		return string(runes), nil
	case "US-ASCII":
		runes := make([]rune, len(data))
		for index, value := range data {
			runes[index] = rune(value & 0x7f)
		}
		return string(runes), nil
	case "UTF-16":
		return decodeUTF16(data, binary.BigEndian)
	case "UTF-16LE":
		return decodeUTF16(data, binary.LittleEndian)
	case "UTF-16BE":
		return decodeUTF16(data, binary.BigEndian)
	default:
		return "", fmt.Errorf("不支持的编码格式:%s", charset)
	}
}

// encodeText encodes text with the configured charset.
func encodeText(text string, charset string) ([]byte, error) {
	switch normalizeCharset(charset) {
	case "UTF-8":
		return []byte(text), nil
	case "GBK":
		return encodeGBK(text)
	case "ISO-8859-1":
		buf := make([]byte, 0, len(text))
		for _, value := range text {
			if value > 0xff {
				return nil, fmt.Errorf("字符 %q 不能使用 ISO-8859-1 编码", value)
			}
			buf = append(buf, byte(value))
		}
		return buf, nil
	case "US-ASCII":
		buf := make([]byte, 0, len(text))
		for _, value := range text {
			if value > 0x7f {
				return nil, fmt.Errorf("字符 %q 不能使用 US-ASCII 编码", value)
			}
			buf = append(buf, byte(value))
		}
		return buf, nil
	case "UTF-16", "UTF-16BE":
		return encodeUTF16(text, binary.BigEndian, true)
	case "UTF-16LE":
		return encodeUTF16(text, binary.LittleEndian, true)
	default:
		return nil, fmt.Errorf("不支持的编码格式:%s", charset)
	}
}

func decodeUTF16(data []byte, order binary.ByteOrder) (string, error) {
	if bytes.HasPrefix(data, []byte{0xfe, 0xff}) {
		data = data[2:]
		order = binary.BigEndian
	} else if bytes.HasPrefix(data, []byte{0xff, 0xfe}) {
		data = data[2:]
		order = binary.LittleEndian
	}
	if len(data)%2 != 0 {
		data = data[:len(data)-1]
	}
	units := make([]uint16, len(data)/2)
	for index := range units {
		units[index] = order.Uint16(data[index*2:])
	}
	return string(utf16.Decode(units)), nil
}

func encodeUTF16(text string, order binary.ByteOrder, bom bool) ([]byte, error) {
	units := utf16.Encode([]rune(text))
	buf := make([]byte, 0, len(units)*2+2)
	if bom {
		if order == binary.BigEndian {
			buf = append(buf, 0xfe, 0xff)
		} else {
			buf = append(buf, 0xff, 0xfe)
		}
	}
	for _, unit := range units {
		var pair [2]byte
		order.PutUint16(pair[:], unit)
		buf = append(buf, pair[0], pair[1])
	}
	return buf, nil
}

// decodeGBK and encodeGBK are swapped in by charset_gbk.go; without it Jpom
// configurations that require GBK report a clear error instead of corrupting
// files.
var (
	gbkDecoder func([]byte) (string, error)
	gbkEncoder func(string) ([]byte, error)
)

func decodeGBK(data []byte) (string, error) {
	if gbkDecoder == nil {
		return "", fmt.Errorf("暂不支持 GBK 编码,请将 allowEditSuffix 配置为 UTF-8 或安装 GBK 支持")
	}
	return gbkDecoder(data)
}

func encodeGBK(text string) ([]byte, error) {
	if gbkEncoder == nil {
		return nil, fmt.Errorf("暂不支持 GBK 编码,请将 allowEditSuffix 配置为 UTF-8 或安装 GBK 支持")
	}
	return gbkEncoder(text)
}
