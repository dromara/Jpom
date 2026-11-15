/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package project

import (
	"github.com/dromara/Jpom/go-agent/internal/proc"
)

// ConsoleCharset 返回子进程/脚本输出解码字符集（jpom.system.console-charset，空则系统默认）。
//
// 等价 Java ExtConfigBean.getConsoleLogCharset()：未配置时使用系统默认字符集。
func (r *Runtime) ConsoleCharset() string {
	return proc.NormalizeCharset(r.Options.ConsoleCharset, proc.DefaultSystemCharset())
}

// ResolveLogCharset 解析项目控制台日志文件字符集（等价 ProjectInfoService.resolveLogCharset）。
//
// 优先级：项目数据 logCharset（软链项目取被软链项目）→ jpom.project.log.file-charset
// → 系统默认（Windows GBK，其他 UTF-8）。
func (r *Runtime) ResolveLogCharset(info map[string]any) string {
	defaultCharset := r.Options.ProjectLog.FileCharset
	if original, err := r.Manager.ResolveModel(info); err == nil {
		if custom := GetString(original, "logCharset"); custom != "" {
			return proc.NormalizeCharset(custom, defaultCharset)
		}
	}
	return proc.NormalizeCharset(defaultCharset, proc.DefaultSystemCharset())
}

// TailLogLines 读取项目控制台日志最后 n 行（按项目日志字符集解码），供 WS /console 复用。
func (r *Runtime) TailLogLines(info map[string]any, n int) ([]string, error) {
	file, err := r.Manager.ResolveLogFile(info)
	if err != nil {
		return nil, err
	}
	return proc.TailLinesCharset(file, n, r.ResolveLogCharset(info))
}

// ReadLogLinesFrom 从第 startLine 行开始读取项目控制台日志（按项目日志字符集解码），
// 返回日志行、下次起始行号、文件总行数；供 WS /console 轮询复用。
func (r *Runtime) ReadLogLinesFrom(info map[string]any, startLine, max int) ([]string, int, int, error) {
	file, err := r.Manager.ResolveLogFile(info)
	if err != nil {
		return nil, startLine, 0, err
	}
	return proc.ReadLinesFromCharset(file, startLine, max, r.ResolveLogCharset(info))
}

// AppendLogLines 按项目日志字符集追加日志行（供 WS/脚本域写控制台日志复用）。
func (r *Runtime) AppendLogLines(info map[string]any, lines []string) error {
	file, err := r.Manager.TouchLogFile(info)
	if err != nil {
		return err
	}
	return appendLogLines(file, lines, r.ResolveLogCharset(info))
}
