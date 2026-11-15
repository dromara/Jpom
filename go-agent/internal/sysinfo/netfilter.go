/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package sysinfo

import (
	"regexp"
	"strings"

	"github.com/dromara/Jpom/go-agent/internal/config"
)

// NetworkFilter 对应 jpom.monitor.network：按 Ant 风格通配符过滤网卡名称。
type NetworkFilter struct {
	excludes     []*regexp.Regexp
	containsOnly []*regexp.Regexp
}

// NewNetworkFilter 根据配置构造过滤器（nil 表示不过滤）。
func NewNetworkFilter(monitor *config.Monitor) *NetworkFilter {
	filter := &NetworkFilter{}
	if monitor == nil {
		return filter
	}
	for _, pattern := range monitor.Network.StatExcludeNames.List() {
		if compiled := antToRegexp(pattern); compiled != nil {
			filter.excludes = append(filter.excludes, compiled)
		}
	}
	for _, pattern := range monitor.Network.StatContainsOnlyNames.List() {
		if compiled := antToRegexp(pattern); compiled != nil {
			filter.containsOnly = append(filter.containsOnly, compiled)
		}
	}
	return filter
}

// Match 判断网卡是否参与统计（AntPathMatcher 语义）。
func (f *NetworkFilter) Match(name string) bool {
	for _, pattern := range f.excludes {
		if pattern.MatchString(name) {
			return false
		}
	}
	if len(f.containsOnly) == 0 {
		return true
	}
	for _, pattern := range f.containsOnly {
		if pattern.MatchString(name) {
			return true
		}
	}
	return false
}

// antToRegexp 把 Ant 风格通配符转换为正则表达式（* ? ** {a,b}）。
func antToRegexp(pattern string) *regexp.Regexp {
	pattern = strings.TrimSpace(pattern)
	if pattern == "" {
		return nil
	}
	var builder strings.Builder
	builder.WriteString("^")
	for i := 0; i < len(pattern); i++ {
		char := pattern[i]
		switch char {
		case '*':
			if i+1 < len(pattern) && pattern[i+1] == '*' {
				builder.WriteString(".*")
				i++
			} else {
				builder.WriteString("[^/]*")
			}
		case '?':
			builder.WriteString("[^/]")
		case '{':
			end := strings.IndexByte(pattern[i:], '}')
			if end < 0 {
				builder.WriteString(regexp.QuoteMeta(string(char)))
				continue
			}
			options := strings.Split(pattern[i+1:i+end], ",")
			builder.WriteString("(?:")
			for index, option := range options {
				if index > 0 {
					builder.WriteString("|")
				}
				builder.WriteString(regexp.QuoteMeta(strings.TrimSpace(option)))
			}
			builder.WriteString(")")
			i += end
		default:
			builder.WriteString(regexp.QuoteMeta(string(char)))
		}
	}
	builder.WriteString("$")
	compiled, err := regexp.Compile(builder.String())
	if err != nil {
		return nil
	}
	return compiled
}
