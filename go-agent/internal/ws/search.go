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
	"bufio"
	"bytes"
	"io"
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
// matched lines are sent one by one, then a summary line whose two counts are
// exactly the Java FileSearchUtil values (interval line count, echoed count).
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
		// 流式按行读取（Java FileSearchUtil 只保留区间行），避免整文件读入内存。
		lines, err := scanSearchLines(file, head, tail, first, charset)
		if err != nil {
			_ = session.Send(m.translate(session, "i18n.command_execution_failed_details.77ed"))
			_ = session.Send(err.Error())
			return
		}
		shown := matchSearchLines(lines, keyword, beforeCount, afterCount, func(tuple lineTuple) {
			_ = session.Send(tuple.text)
		})
		// Java: search_result_display(区间行数, 命中显示行数)。
		_ = session.Send(m.translate(session, "i18n.search_result_display.d2c3", len(lines), shown))
	}()
}

// matchSearchLines echoes the matched lines plus the configured before/after
// context and returns the number of distinct echoed lines (Java cacheLineNum).
func matchSearchLines(lines []lineTuple, keyword string, beforeCount, afterCount int, echo func(lineTuple)) int {
	if beforeCount < 0 {
		beforeCount = 0
	}
	if afterCount < 0 {
		afterCount = 0
	}
	matcher := buildMatcher(keyword)
	seen := make(map[int]struct{}, len(lines))
	shown := 0
	emit := func(tuple lineTuple) {
		if _, ok := seen[tuple.no]; ok {
			return
		}
		seen[tuple.no] = struct{}{}
		shown++
		echo(tuple)
	}
	hitIndex := 0
	beforeQueue := make([]lineTuple, 0, beforeCount)
	for _, tuple := range lines {
		if matcher(tuple.text) {
			for _, before := range beforeQueue {
				emit(before)
			}
			emit(tuple)
			hitIndex = tuple.no
		}
		if hitIndex > 0 && tuple.no > hitIndex && tuple.no <= hitIndex+afterCount {
			emit(tuple)
		}
		if beforeCount > 0 {
			// 头部插入（最近的在前），与 Hutool LimitQueue.offerFirst 一致。
			beforeQueue = append([]lineTuple{tuple}, beforeQueue...)
			if len(beforeQueue) > beforeCount {
				beforeQueue = beforeQueue[:beforeCount]
			}
		}
	}
	return shown
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

// searchRange is the [start, end] line window selected by head/tail/first.
type searchRange struct {
	start int
	end   int
	last  int // last > 0: keep only the last N lines (readLastLine semantics)
}

// calculateSearchRange mirrors FileSearchUtil#calculate:
//
//	head > 0 && first  -> [min(tail, head), head]
//	head > 0 && !first -> [max(head-tail, 1), head]
//	head <= 0 && first -> [tail, +inf)
//	head <= 0 && !first-> last tail lines
func calculateSearchRange(head, tail int, first bool) searchRange {
	if head > 0 {
		if first {
			start := tail
			if start > head {
				start = head
			}
			return searchRange{start: start, end: head}
		}
		start := head - tail
		if start < 1 {
			start = 1
		}
		return searchRange{start: start, end: head}
	}
	if first {
		return searchRange{start: tail, end: -1}
	}
	return searchRange{last: tail}
}

// scanSearchLines reads the selected line range from the file line by line,
// keeping only the window in memory (no os.ReadFile of the whole log).
func scanSearchLines(path string, head, tail int, first bool, charset string) ([]lineTuple, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer func() { _ = file.Close() }()
	window := calculateSearchRange(head, tail, first)
	if window.start < 1 {
		window.start = 1
	}
	if window.last < 0 {
		window.last = 0
	}
	reader := bufio.NewReaderSize(file, 64*1024)
	lines := make([]lineTuple, 0, 32)
	lineNo := 0
	for {
		raw, readErr := reader.ReadBytes('\n')
		if len(raw) > 0 {
			lineNo++
			raw = bytes.TrimSuffix(raw, []byte("\n"))
			raw = bytes.TrimSuffix(raw, []byte("\r"))
			tuple := lineTuple{no: lineNo, text: decodeLine(raw, charset)}
			switch {
			case window.last > 0:
				// readLastLine：环形保留最后 N 行。
				lines = append(lines, tuple)
				if len(lines) > window.last {
					lines = lines[1:]
				}
			case lineNo >= window.start && (window.end < 0 || lineNo <= window.end):
				lines = append(lines, tuple)
			case window.end >= 0 && lineNo > window.end:
				return lines, nil
			}
		}
		if readErr != nil {
			if readErr == io.EOF {
				return lines, nil
			}
			return nil, readErr
		}
	}
}
