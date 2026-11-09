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
	"bufio"
	"io"
	"os"
	"strings"
)

// TailLines 读取文件最后 n 行（用于控制台日志首次读取）。
// n <= 0 时返回空；文件不存在返回错误。
func TailLines(path string, n int) ([]string, error) {
	if n <= 0 {
		return nil, nil
	}
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer func() { _ = file.Close() }()
	info, err := file.Stat()
	if err != nil {
		return nil, err
	}
	size := info.Size()
	if size == 0 {
		return nil, nil
	}
	// 从文件末尾按块向前读取，直到凑够 n 行或读到文件头
	const blockSize = int64(64 * 1024)
	offset := size
	buffer := make([]byte, 0, blockSize)
	for offset > 0 {
		readSize := blockSize
		if offset < readSize {
			readSize = offset
		}
		offset -= readSize
		chunk := make([]byte, readSize)
		if _, err := file.ReadAt(chunk, offset); err != nil && err != io.EOF {
			return nil, err
		}
		buffer = append(chunk, buffer...)
		if strings.Count(string(buffer), "\n") > n {
			break
		}
	}
	lines := splitOutput(string(buffer))
	if len(lines) > n {
		lines = lines[len(lines)-n:]
	}
	return lines, nil
}

// ReadLinesFrom 从第 startLine 行（1 基）开始读取，最多返回 max 行。
// 返回值依次为：日志行、下次读取的起始行号、文件总行数。
// 等价 Java FileUtils.readLogFile（内部使用 1000 行的 LimitQueue）。
func ReadLinesFrom(path string, startLine, max int) ([]string, int, int, error) {
	if startLine < 1 {
		startLine = 1
	}
	if max <= 0 {
		max = 1000
	}
	file, err := os.Open(path)
	if err != nil {
		return nil, startLine, 0, err
	}
	defer func() { _ = file.Close() }()
	queue := make([]string, 0, max)
	lineNo := 0
	scanner := bufio.NewScanner(file)
	scanner.Buffer(make([]byte, 64*1024), 1024*1024)
	for scanner.Scan() {
		lineNo++
		if lineNo < startLine {
			continue
		}
		if len(queue) == max {
			queue = queue[1:]
		}
		queue = append(queue, scanner.Text())
	}
	if err := scanner.Err(); err != nil {
		return queue, lineNo + 1, lineNo, err
	}
	return queue, lineNo + 1, lineNo, nil
}
