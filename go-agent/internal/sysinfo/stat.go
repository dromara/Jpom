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
	"os"
	"path/filepath"
	"runtime"
	"strconv"

	"jpom.io/go-agent/internal/app"
)

// readDataMap 读取 data/<file> 的 JSON 对象（不存在或损坏时返回空 map）。
func readDataMap(a *app.App, file string) map[string]map[string]any {
	content, err := os.ReadFile(filepath.Join(a.DataDir, file))
	if err != nil || len(content) == 0 {
		return map[string]map[string]any{}
	}
	values := map[string]map[string]any{}
	if err := jsonUnmarshal(content, &values); err != nil {
		return map[string]map[string]any{}
	}
	return values
}

func workspaceID(info map[string]any) string {
	if value, ok := info["workspaceId"].(string); ok && value != "" {
		return value
	}
	return "DEFAULT"
}

// JpomInfo 对应 IndexController.getJpomInfo（/get-stat-info 的 jpomInfo 字段）。
//
// Go 端无 JVM，javaVersion 返回 Go 运行时版本，totalMemory/freeMemory 返回进程
// 内存统计（字段名与类型保持与 Java 端一致：long 序列化为字符串）。
func JpomInfo(a *app.App) map[string]any {
	projects := readDataMap(a, "project.json")
	scripts := readDataMap(a, "script.json")
	workspaceStat := map[string]map[string]int{}
	for _, info := range projects {
		workspace := workspaceID(info)
		if workspaceStat[workspace] == nil {
			workspaceStat[workspace] = map[string]int{"projectCount": 0, "scriptCount": 0}
		}
		workspaceStat[workspace]["projectCount"]++
	}
	for _, info := range scripts {
		workspace := workspaceID(info)
		if workspaceStat[workspace] == nil {
			workspaceStat[workspace] = map[string]int{"projectCount": 0, "scriptCount": 0}
		}
		workspaceStat[workspace]["scriptCount"]++
	}
	var memStats runtime.MemStats
	runtime.ReadMemStats(&memStats)
	totalMemory := memStats.Sys
	freeMemory := memStats.Sys - memStats.HeapAlloc
	return map[string]any{
		"javaVirtualCount": JavaProcessCount(),
		"jpomManifest":     a.Manifest(),
		"javaVersion":      runtime.Version(),
		"totalMemory":      strconv.FormatUint(totalMemory, 10),
		"freeMemory":       strconv.FormatUint(freeMemory, 10),
		"projectCount":     len(projects),
		"scriptCount":      len(scripts),
		"workspaceStat":    workspaceStat,
	}
}
