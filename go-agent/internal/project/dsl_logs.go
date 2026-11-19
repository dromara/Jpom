//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package project

import (
	"crypto/md5"
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/dromara/Jpom/go-agent/internal/proc"
)

// LogFileItem 是可查看的项目运行日志文件（控制台日志下拉列表中的一项）。
//
// 等价 Java ProjectLogFileItem：客户端只回传 Key，由插件端解析真实路径，
// 避免把任意文件路径暴露成客户端可控参数。
type LogFileItem struct {
	Key      string `json:"key"`
	Name     string `json:"name"`
	Path     string `json:"path"`
	Charset  string `json:"charset"`
	Exists   bool   `json:"exists"`
	Size     int64  `json:"size"`
	SizeText string `json:"sizeText"`
}

// dslLogFileConfig 是 DSL 配置 config.log 中的一项（等价 Java DslYmlDto.LogFile）。
type dslLogFileConfig struct {
	Name    string
	Path    string
	Charset string
}

// parseDslLogFiles 读取 config.log 列表。
func parseDslLogFiles(config map[string]any) []dslLogFileConfig {
	cfg := yamlMap(config["config"])
	if cfg == nil {
		return nil
	}
	raw, ok := cfg["log"]
	if !ok || raw == nil {
		return nil
	}
	list, ok := raw.([]any)
	if !ok {
		return nil
	}
	result := make([]dslLogFileConfig, 0, len(list))
	for _, entry := range list {
		item := yamlMap(entry)
		if item == nil {
			continue
		}
		result = append(result, dslLogFileConfig{
			Name:    GetString(item, "name"),
			Path:    GetString(item, "path"),
			Charset: GetString(item, "charset"),
		})
	}
	return result
}

// DslLogFiles 返回 DSL 项目配置的可查看日志文件；非 DSL 或未配置返回 nil。
func (r *Runtime) DslLogFiles(info map[string]any) []LogFileItem {
	original, err := r.Manager.ResolveModel(info)
	if err != nil {
		return nil
	}
	if GetString(original, "runMode") != RunModeDsl {
		return nil
	}
	config, err := parseDslConfig(original)
	if err != nil {
		return nil
	}
	entries := parseDslLogFiles(config)
	if len(entries) == 0 {
		return nil
	}
	libDir, _ := r.Manager.ResolveLibPath(info)
	defaultCharset := r.ResolveLogCharset(info)
	variables := map[string]string{
		"PROJECT_ID":   GetString(info, "id"),
		"PROJECT_NAME": GetString(info, "name"),
		"PROJECT_PATH": libDir,
	}
	result := make([]LogFileItem, 0, len(entries))
	for _, entry := range entries {
		file, ok := resolveDslLogPath(libDir, entry.Path, variables)
		if !ok {
			continue
		}
		name := strings.TrimSpace(entry.Name)
		if name == "" {
			name = filepath.Base(file)
		}
		item := LogFileItem{
			// 标识用配置内容的摘要：中文名称经过表单转发可能被转码，标识只使用 ASCII
			Key:     dslLogKey(entry),
			Name:    name,
			Path:    file,
			Charset: proc.NormalizeCharset(entry.Charset, defaultCharset),
		}
		if stat, err := os.Stat(file); err == nil && !stat.IsDir() {
			item.Exists = true
			item.Size = stat.Size()
			item.SizeText = readableFileSize(stat.Size())
		}
		result = append(result, item)
	}
	return result
}

// dslLogKey 计算日志标识（等价 Java DslProjectLogFileProvider.logKey）：md5(name + "\n" + path)。
func dslLogKey(entry dslLogFileConfig) string {
	sum := md5.Sum([]byte(entry.Name + "\n" + entry.Path))
	return fmt.Sprintf("%x", sum)
}

// ResolveDslLogFile 按标识解析 DSL 日志文件（控制台切换日志使用）。
func (r *Runtime) ResolveDslLogFile(info map[string]any, key string) (LogFileItem, bool) {
	if strings.TrimSpace(key) == "" {
		return LogFileItem{}, false
	}
	for _, item := range r.DslLogFiles(info) {
		if item.Key == key {
			return item, true
		}
	}
	return LogFileItem{}, false
}

// resolveDslLogPath 解析日志文件路径：支持 ${PROJECT_PATH} 等变量；绝对路径直接使用；
// 相对路径限制在项目目录内（等价 Java DslProjectLogFileProvider.resolveFile）。
func resolveDslLogPath(libDir, path string, variables map[string]string) (string, bool) {
	resolved := strings.TrimSpace(path)
	if resolved == "" {
		return "", false
	}
	for key, value := range variables {
		resolved = strings.ReplaceAll(resolved, "${"+key+"}", value)
	}
	resolved = strings.TrimSpace(resolved)
	if resolved == "" {
		return "", false
	}
	if filepath.IsAbs(resolved) {
		return filepath.Clean(resolved), true
	}
	if libDir == "" {
		return "", false
	}
	return resolveUnderBase(libDir, resolved)
}

// resolveUnderBase 把相对路径限制在 base 目录内（等价 Java FileUtils.resolveSubFile）。
func resolveUnderBase(base, relative string) (string, bool) {
	relative = strings.TrimLeft(strings.TrimSpace(relative), "/\\")
	if relative == "" {
		return "", false
	}
	baseAbs, err := filepath.Abs(base)
	if err != nil {
		return "", false
	}
	targetAbs, err := filepath.Abs(filepath.Join(baseAbs, relative))
	if err != nil {
		return "", false
	}
	if targetAbs != baseAbs && !strings.HasPrefix(targetAbs, baseAbs+string(filepath.Separator)) {
		return "", false
	}
	return targetAbs, true
}
