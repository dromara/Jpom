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
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"

	"jpom.io/go-agent/internal/app"
)

// LogTreeData 对应 LogManageController.log_data.json（DirTreeUtil 目录树）。
func LogTreeData(a *app.App) ([]map[string]any, error) {
	return readLogTree(a.LogsDir, a.LogsDir)
}

func readLogTree(dir, root string) ([]map[string]any, error) {
	entries, err := os.ReadDir(dir)
	if err != nil {
		if os.IsNotExist(err) {
			return []map[string]any{}, nil
		}
		return nil, err
	}
	type item struct {
		name    string
		modTime time.Time
		isDir   bool
	}
	items := make([]item, 0, len(entries))
	for _, entry := range entries {
		info, err := entry.Info()
		if err != nil {
			continue
		}
		items = append(items, item{name: entry.Name(), modTime: info.ModTime(), isDir: entry.IsDir()})
	}
	// 与 DirTreeUtil 一致：按修改时间倒序。
	sort.SliceStable(items, func(i, j int) bool { return items[i].modTime.After(items[j].modTime) })
	result := make([]map[string]any, 0, len(items))
	for _, current := range items {
		full := filepath.Join(dir, current.name)
		relative, err := filepath.Rel(root, full)
		if err != nil {
			relative = current.name
		}
		value := map[string]any{
			"title": current.name,
			"path":  filepath.ToSlash(relative),
		}
		if current.isDir {
			children, err := readLogTree(full, root)
			if err != nil {
				return nil, err
			}
			value["children"] = children
		}
		result = append(result, value)
	}
	return result, nil
}

// resolveLogPath 解析日志目录下的相对路径，并防止路径穿越。
func resolveLogPath(a *app.App, relative string) (string, error) {
	if strings.TrimSpace(relative) == "" {
		return "", paramErr(400, "i18n.parameter_error_path_error.f482")
	}
	root, err := filepath.Abs(a.LogsDir)
	if err != nil {
		return "", err
	}
	target, err := filepath.Abs(filepath.Join(root, relative))
	if err != nil {
		return "", err
	}
	if target != root && !strings.HasPrefix(target, root+string(filepath.Separator)) {
		return "", paramErr(405, "i18n.parameter_error_path_error.f482")
	}
	return target, nil
}

// DeleteLog 对应 LogManageController.log_del.json。
func DeleteLog(a *app.App, relative string) error {
	target, err := resolveLogPath(a, relative)
	if err != nil {
		return err
	}
	info, err := os.Stat(target)
	if err != nil {
		return paramErr(405, "i18n.delete_failure.acf0")
	}
	if time.Since(info.ModTime()) <= 24*time.Hour {
		return paramErr(405, "i18n.cannot_delete_recent_logs.ee19")
	}
	if err := os.RemoveAll(target); err != nil {
		return paramErr(500, "i18n.delete_failure.acf0")
	}
	// 清理空目录（与 FileUtil.cleanEmpty 一致）。
	parent := filepath.Dir(target)
	root, _ := filepath.Abs(a.LogsDir)
	for parent != root && strings.HasPrefix(parent, root) {
		entries, err := os.ReadDir(parent)
		if err != nil || len(entries) > 0 {
			break
		}
		if err := os.Remove(parent); err != nil {
			break
		}
		parent = filepath.Dir(parent)
	}
	return nil
}

// LogDownloadPath 对应 LogManageController.log_download。
func LogDownloadPath(a *app.App, relative string) (string, error) {
	target, err := resolveLogPath(a, relative)
	if err != nil {
		return "", err
	}
	info, err := os.Stat(target)
	if err != nil || info.IsDir() {
		return "", fmt.Errorf("log file does not exist")
	}
	return target, nil
}
