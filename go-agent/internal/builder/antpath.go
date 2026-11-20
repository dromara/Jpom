//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package builder

import (
	"regexp"
	"strings"
	"sync"
)

// 构建执行器（builder）的 Ant 通配实现。
//
// 目录名必须是 internal/builder 而不是 internal/build：go-agent/.gitignore 第 1 行
// 是 `build/`（匹配任意层级同名目录），放在 internal/build 下的代码永远进不了仓库。
//
// 语义基准是 Server 端 org.springframework.util.AntPathMatcher（默认配置：
// trimTokens=true、caseSensitive=true）以及 org.dromara.jpom.util.AntPathUtil 的用法：
//
//   - AntPathUtil#antPathMatcher(File,String)：pattern 先 normalize 成 "/"+match，
//     遍历工作目录把每个文件/目录的相对路径 normalize 成 "/"+subPath 后调用
//     ANT_PATH_MATCHER.match(pattern, path)（全匹配）。
//   - BuildExecuteManage#packageFile 的二级匹配用
//     ANT_PATH_MATCHER.matchStart(antSubMatch+"**", path)，
//     并额外用 ANT_PATH_MATCHER.match(antSubMatch, "/a/b/") 逐级定位。
//
// 因此两端必须**逐条一致**：本文件的实现由 go-agent/internal/builder/testdata/antpath_parity.tsv
// 与 Java 输出对齐（基线由维护者用 Spring AntPathMatcher 生成，生成器不随仓库发布），
// 任何语义改动都必须重新生成基线。

// AntMatch 等价于 AntPathMatcher#match(pattern, path)（全匹配）。
func AntMatch(pattern, path string) bool {
	return antDoMatch(pattern, path, true)
}

// AntMatchStart 等价于 AntPathMatcher#matchStart(pattern, path)（前缀匹配）。
func AntMatchStart(pattern, path string) bool {
	return antDoMatch(pattern, path, false)
}

// AntIsPattern 等价于 AntPathMatcher#isPattern：含 * 或 ?，或配对的 {var}。
func AntIsPattern(value string) bool {
	if value == "" {
		return false
	}
	uriVar := false
	for _, c := range value {
		switch c {
		case '*', '?':
			return true
		case '{':
			uriVar = true
		case '}':
			if uriVar {
				return true
			}
		}
	}
	return false
}

func antDoMatch(pattern, path string, fullMatch bool) bool {
	// Java: path.startsWith(sep) != pattern.startsWith(sep) -> false
	if strings.HasPrefix(path, "/") != strings.HasPrefix(pattern, "/") {
		return false
	}
	// 末尾 "/{*var}" 的 path 变量语法在 Jpom 里不会被用到，这里不实现（保持显式）。
	pattDirs := antTokenize(pattern)
	pathDirs := antTokenize(path)

	pattIdxStart, pattIdxEnd := 0, len(pattDirs)-1
	pathIdxStart, pathIdxEnd := 0, len(pathDirs)-1

	// 匹配第一个 ** 之前的部分
	for pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd {
		patDir := pattDirs[pattIdxStart]
		if patDir == "**" {
			break
		}
		if !antMatchSegment(patDir, pathDirs[pathIdxStart]) {
			return false
		}
		pattIdxStart++
		pathIdxStart++
	}
	if pathIdxStart > pathIdxEnd {
		// 路径已耗尽：剩余 pattern 只能是 * 或 **
		if pattIdxStart > pattIdxEnd {
			return strings.HasSuffix(pattern, "/") == strings.HasSuffix(path, "/")
		}
		if !fullMatch {
			return true
		}
		if pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart] == "*" && strings.HasSuffix(path, "/") {
			return true
		}
		for i := pattIdxStart; i <= pattIdxEnd; i++ {
			if pattDirs[i] != "**" {
				return false
			}
		}
		return true
	}
	if pattIdxStart > pattIdxEnd {
		// 路径还有剩余但 pattern 已耗尽
		return false
	}
	if !fullMatch && pattDirs[pattIdxStart] == "**" {
		// pattern 以 ** 开始、非全匹配
		return true
	}
	// 从尾部反向对齐到最后一个 **（Spring doMatch 的 "up to last '**'" 段，缺了这段
	// /a/**/b 这类模式会被误判）。末尾段还要求两边的结尾分隔符一致。
	for pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd {
		pattDir := pattDirs[pattIdxEnd]
		if pattDir == "**" {
			break
		}
		if !antMatchSegment(pattDir, pathDirs[pathIdxEnd]) {
			return false
		}
		// 注意：Spring 6.x 在反向段里对"末尾段 + 结尾分隔符不一致"返回 false，
		// 但 5.3.x（Spring Boot 2.7.18 的运行时）没有这一条：/a/**/b 能匹配 /a/b/。
		// 这里以 5.3.39 为准（parity fixture 由该版本生成），不要按 6.x 源码"修正"。
		pattIdxEnd--
		pathIdxEnd--
	}
	if pathIdxStart > pathIdxEnd {
		// 路径已耗尽
		for i := pattIdxStart; i <= pattIdxEnd; i++ {
			if pattDirs[i] != "**" {
				return false
			}
		}
		return true
	}
	// 逐个 ** 段对齐
	for pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd {
		patIdxTmp := -1
		for i := pattIdxStart + 1; i <= pattIdxEnd; i++ {
			if pattDirs[i] == "**" {
				patIdxTmp = i
				break
			}
		}
		if patIdxTmp == pattIdxStart+1 {
			// '**/**' 等价于 '**'
			pattIdxStart++
			continue
		}
		if patIdxTmp < 0 {
			// 理论上不可达（反向段结束后 pattIdxEnd 必为 **），防御性返回
			return false
		}
		patLength := patIdxTmp - pattIdxStart - 1
		strLength := pathIdxEnd - pathIdxStart + 1
		foundIdx := -1
		for i := 0; i <= strLength-patLength; i++ {
			matched := true
			for j := 0; j < patLength; j++ {
				if !antMatchSegment(pattDirs[pattIdxStart+j+1], pathDirs[pathIdxStart+i+j]) {
					matched = false
					break
				}
			}
			if matched {
				foundIdx = pathIdxStart + i
				break
			}
		}
		if foundIdx == -1 {
			return false
		}
		pattIdxStart = patIdxTmp
		pathIdxStart = foundIdx + patLength
	}
	for i := pattIdxStart; i <= pattIdxEnd; i++ {
		if pattDirs[i] != "**" {
			return false
		}
	}
	return true
}

// antTokenize 等价于 Spring 的 tokenizeToStringArray(path, "/", trimTokens=true, ignoreEmptyTokens=true)。
func antTokenize(path string) []string {
	parts := strings.Split(path, "/")
	result := make([]string, 0, len(parts))
	for _, part := range parts {
		part = strings.TrimSpace(part)
		if part == "" {
			continue
		}
		result = append(result, part)
	}
	return result
}

var antSegmentCache sync.Map // map[string]*regexp.Regexp

func antMatchSegment(pattern, segment string) bool {
	if !strings.ContainsAny(pattern, "*?{") {
		return pattern == segment
	}
	if v, ok := antSegmentCache.Load(pattern); ok {
		return v.(*regexp.Regexp).MatchString(segment)
	}
	re := regexp.MustCompile(antSegmentToRegexp(pattern))
	antSegmentCache.Store(pattern, re)
	return re.MatchString(segment)
}

// antSegmentToRegexp 把单段 Ant 模式翻译成 Go 正则，对应 Spring AntPathStringMatcher：
//   - '?' -> '.'（任意单字符）
//   - '*' -> '.*'（段内任意字符序列，'**' 同 '.*'）
//   - '{名称}' -> '.*'；'{名称:regex}' -> 内联正则（Java 与 Go 语法一致的部分）
//   - 其它字符按字面量
func antSegmentToRegexp(pattern string) string {
	var b strings.Builder
	// Java 侧用 Pattern.DOTALL 编译段正则，Go 需要显式 (?s) 才能让 '.' 跨行
	b.WriteString("(?s)^")
	runes := []rune(pattern)
	for i := 0; i < len(runes); i++ {
		switch runes[i] {
		case '?':
			b.WriteString(".")
		case '*':
			b.WriteString(".*")
		case '{':
			end := antFindBraceEnd(runes, i)
			if end < 0 {
				b.WriteString(regexp.QuoteMeta("{"))
				continue
			}
			inner := string(runes[i+1 : end])
			if idx := strings.Index(inner, ":"); idx >= 0 {
				inner = inner[idx+1:]
			} else {
				inner = ".*"
			}
			if _, err := regexp.Compile(inner); err != nil {
				// 内联正则不兼容（Java 专有语法）时降级为 .*，并应由 parity 测试暴露差异
				inner = ".*"
			}
			b.WriteString("(" + inner + ")")
			i = end
		default:
			b.WriteString(regexp.QuoteMeta(string(runes[i])))
		}
	}
	b.WriteString("$")
	return b.String()
}

// antFindBraceEnd 找到与 runes[start]=='{' 配对的 '}'，支持嵌套 {a{b}}。
func antFindBraceEnd(runes []rune, start int) int {
	depth := 0
	for i := start; i < len(runes); i++ {
		switch runes[i] {
		case '{':
			depth++
		case '}':
			depth--
			if depth == 0 {
				return i
			}
		}
	}
	return -1
}
