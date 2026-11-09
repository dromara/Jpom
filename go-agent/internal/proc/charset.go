/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package proc

import (
	"fmt"
	"os"
	"runtime"
	"strings"

	"golang.org/x/text/encoding"
	"golang.org/x/text/encoding/charmap"
	"golang.org/x/text/encoding/simplifiedchinese"
	"golang.org/x/text/encoding/traditionalchinese"
	"golang.org/x/text/encoding/unicode"
)

// charsetUTF8 表示无需转换（Go 字符串本身即 UTF-8）。
const charsetUTF8 = "UTF-8"

// DefaultSystemCharset 返回 Java CharsetUtil.systemCharset 等价默认值：
// Windows 为 GBK，其他系统为 UTF-8（与 Java ProjectLogConfig 默认一致）。
func DefaultSystemCharset() string {
	if runtime.GOOS == "windows" {
		return "GBK"
	}
	return charsetUTF8
}

// NormalizeCharset 归一化字符集名称；无法识别时返回 fallback（空则返回系统默认）。
//
// 与 Java CharsetUtil.parse(value, defaultCharset) 语义一致：非法名称回退默认值。
func NormalizeCharset(name, fallback string) string {
	if strings.TrimSpace(fallback) == "" {
		fallback = DefaultSystemCharset()
	}
	value := strings.ToUpper(strings.TrimSpace(name))
	value = strings.ReplaceAll(value, "_", "-")
	switch value {
	case "":
		return strings.ToUpper(strings.TrimSpace(fallback))
	case "UTF-8", "UTF8":
		return charsetUTF8
	case "GBK", "GB2312", "GB-2312", "CP936", "MS936", "WINDOWS-936", "EUC-CN":
		return "GBK"
	case "GB18030", "GB-18030":
		return "GB18030"
	case "BIG5", "BIG-5", "CP950", "WINDOWS-950":
		return "BIG5"
	case "UTF-16", "UTF16":
		return "UTF-16"
	case "UTF-16LE", "UTF16LE":
		return "UTF-16LE"
	case "UTF-16BE", "UTF16BE":
		return "UTF-16BE"
	case "ISO-8859-1", "ISO8859-1", "LATIN1", "ISO-LATIN-1":
		return "ISO-8859-1"
	case "WINDOWS-1252", "CP1252":
		return "WINDOWS-1252"
	case "US-ASCII", "ASCII":
		return "US-ASCII"
	default:
		return NormalizeCharset(fallback, "")
	}
}

// charsetEncoding 返回字符集对应的 x/text 编码器；UTF-8 返回 nil（无需转换）。
func charsetEncoding(name string) encoding.Encoding {
	switch NormalizeCharset(name, "") {
	case charsetUTF8:
		return nil
	case "GBK":
		return simplifiedchinese.GBK
	case "GB18030":
		return simplifiedchinese.GB18030
	case "BIG5":
		return traditionalchinese.Big5
	case "UTF-16":
		return unicode.UTF16(unicode.BigEndian, unicode.UseBOM)
	case "UTF-16LE":
		return unicode.UTF16(unicode.LittleEndian, unicode.IgnoreBOM)
	case "UTF-16BE":
		return unicode.UTF16(unicode.BigEndian, unicode.IgnoreBOM)
	case "ISO-8859-1", "US-ASCII":
		return charmap.ISO8859_1
	case "WINDOWS-1252":
		return charmap.Windows1252
	default:
		return nil
	}
}

// EncodeString 把 UTF-8 文本编码为指定字符集字节（Java String#getBytes(charset)）。
func EncodeString(charset, text string) ([]byte, error) {
	encodingImpl := charsetEncoding(charset)
	if encodingImpl == nil {
		return []byte(text), nil
	}
	data, err := encodingImpl.NewEncoder().Bytes([]byte(text))
	if err != nil {
		return nil, fmt.Errorf("encode text with charset %s failed: %w", NormalizeCharset(charset, ""), err)
	}
	return data, nil
}

// DecodeBytes 把指定字符集字节解码为 UTF-8 文本（Java new String(bytes, charset)）。
func DecodeBytes(charset string, data []byte) ([]byte, error) {
	encodingImpl := charsetEncoding(charset)
	if encodingImpl == nil {
		return data, nil
	}
	decoded, err := encodingImpl.NewDecoder().Bytes(data)
	if err != nil {
		return nil, fmt.Errorf("decode with charset %s failed: %w", NormalizeCharset(charset, ""), err)
	}
	return decoded, nil
}

// DecodeString 把指定字符集字节解码为字符串。
func DecodeString(charset string, data []byte) (string, error) {
	decoded, err := DecodeBytes(charset, data)
	if err != nil {
		return "", err
	}
	return string(decoded), nil
}

// ReadAllWithCharset 读取整个文件并按字符集解码为文本。
func ReadAllWithCharset(path, charset string) (string, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return "", err
	}
	return DecodeString(charset, data)
}

// TailLinesCharset 读取文件最后 n 行，并按字符集解码（等价 WS 控制台日志初始读取）。
func TailLinesCharset(path string, n int, charset string) ([]string, error) {
	content, err := ReadAllWithCharset(path, charset)
	if err != nil {
		return nil, err
	}
	lines := SplitLines(content)
	if n > 0 && len(lines) > n {
		lines = lines[len(lines)-n:]
	}
	return lines, nil
}

// ReadLinesFromCharset 从第 startLine 行（1 基）开始读取并最多保留 max 行，
// 返回日志行、下次起始行号、总行数（等价 Java FileUtils.readLogFile 的 LimitQueue 语义）。
func ReadLinesFromCharset(path string, startLine, max int, charset string) ([]string, int, int, error) {
	if startLine < 1 {
		startLine = 1
	}
	if max <= 0 {
		max = 1000
	}
	content, err := ReadAllWithCharset(path, charset)
	if err != nil {
		return nil, startLine, 0, err
	}
	lines := SplitLines(content)
	queue := make([]string, 0, max)
	for index, line := range lines {
		lineNo := index + 1
		if lineNo < startLine {
			continue
		}
		if len(queue) == max {
			queue = queue[1:]
		}
		queue = append(queue, line)
	}
	total := len(lines)
	return queue, total + 1, total, nil
}

// SplitLines 按行拆分文本（保留空行、去掉行尾 CR）。
func SplitLines(content string) []string {
	content = strings.ReplaceAll(content, "\r\n", "\n")
	content = strings.ReplaceAll(content, "\r", "\n")
	if content == "" {
		return nil
	}
	lines := strings.Split(content, "\n")
	if last := len(lines) - 1; last >= 0 && lines[last] == "" {
		lines = lines[:last]
	}
	return lines
}
