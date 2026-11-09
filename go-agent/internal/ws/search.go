/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package ws

import (
	"os"
	"regexp"
	"strings"
)

// lineTuple is one numbered log line.
type lineTuple struct {
	no   int
	text string
}

// searchProjectLog performs the console log search
// (AgentWebSocketConsoleHandle#searchLog / FileSearchUtil#searchList): the
// matched lines are sent one by one, then a summary line.
func (m *Manager) searchProjectLog(session *Session, project map[string]any, request map[string]any, charset string) {
	libDir, err := m.projectLibDir(project)
	if err != nil {
		_ = session.Send(m.translate(session, "i18n.command_execution_failed_details.77ed"))
		_ = session.Send(err.Error())
		return
	}
	file, ok := resolveSubFile(libDir, bodyString(request, "logFile"))
	if !ok {
		_ = session.Send(m.translate(session, "i18n.file_not_found.d952"))
		return
	}
	keyword := bodyString(request, "keyword")
	beforeCount := bodyInt(request, "beforeCount")
	afterCount := bodyInt(request, "afterCount")
	head := bodyInt(request, "head")
	tail := bodyInt(request, "tail")
	first := bodyBool(request, "first")
	go func() {
		lines, total, err := readSearchLines(file, head, tail, first, charset)
		if err != nil {
			_ = session.Send(m.translate(session, "i18n.command_execution_failed_details.77ed"))
			_ = session.Send(err.Error())
			return
		}
		matcher := buildMatcher(keyword)
		seen := make(map[int]struct{}, len(lines))
		shown := 0
		echo := func(tuple lineTuple) {
			if _, ok := seen[tuple.no]; ok {
				return
			}
			seen[tuple.no] = struct{}{}
			shown++
			_ = session.Send(tuple.text)
		}
		hitIndex := 0
		beforeQueue := make([]lineTuple, 0, beforeCount)
		for _, tuple := range lines {
			if matcher(tuple.text) {
				for _, before := range beforeQueue {
					echo(before)
				}
				echo(tuple)
				hitIndex = tuple.no
			}
			if hitIndex > 0 && tuple.no > hitIndex && tuple.no <= hitIndex+afterCount {
				echo(tuple)
			}
			if beforeCount > 0 {
				if len(beforeQueue) >= beforeCount {
					beforeQueue = beforeQueue[:beforeCount]
				}
				// 头部插入（最近的在前），与 Hutool LimitQueue.offerFirst 一致
				beforeQueue = append([]lineTuple{tuple}, beforeQueue...)
				if len(beforeQueue) > beforeCount {
					beforeQueue = beforeQueue[:beforeCount]
				}
			}
		}
		_ = session.Send(m.translate(session, "i18n.search_result_display.d2c3", total, shown))
	}()
}

// buildMatcher builds the keyword matcher: empty keyword matches everything,
// otherwise a case insensitive contains OR a regular expression.
func buildMatcher(keyword string) func(string) bool {
	if keyword == "" {
		return func(string) bool { return true }
	}
	lower := strings.ToLower(keyword)
	expression, err := regexp.Compile(keyword)
	if err != nil {
		expression = nil
	}
	return func(line string) bool {
		if strings.Contains(strings.ToLower(line), lower) {
			return true
		}
		if expression != nil {
			return expression.MatchString(line)
		}
		return false
	}
}

// readSearchLines reads the line range selected by head/tail/first
// (FileSearchUtil#calculate + readLastLine/readRangeLine).
func readSearchLines(path string, head, tail int, first bool, charset string) ([]lineTuple, int, error) {
	content, err := os.ReadFile(path)
	if err != nil {
		return nil, 0, err
	}
	text := strings.TrimSuffix(string(content), "\n")
	all := make([]string, 0, 32)
	if text != "" {
		all = strings.Split(text, "\n")
	}
	total := len(all)
	if total == 0 {
		return nil, 0, nil
	}
	var start, end int
	if head > 0 {
		if first {
			start = 1
			end = head
			if tail < head {
				end = tail
			}
		} else {
			start = head - tail
			if start < 1 {
				start = 1
			}
			end = head
		}
	} else {
		if first {
			start = tail
			end = total
		} else {
			start = total - tail + 1
			end = total
		}
	}
	if start < 1 {
		start = 1
	}
	if end > total {
		end = total
	}
	if start > end {
		return nil, total, nil
	}
	lines := make([]lineTuple, 0, end-start+1)
	for no := start; no <= end; no++ {
		line := strings.TrimSuffix(all[no-1], "\r")
		lines = append(lines, lineTuple{no: no, text: decodeLine([]byte(line), charset)})
	}
	return lines, total, nil
}
