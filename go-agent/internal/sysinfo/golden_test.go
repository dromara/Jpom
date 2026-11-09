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
	"encoding/json"
	"os"
	"path/filepath"
	"reflect"
	"runtime"
	"testing"
)

// findGolden 在工作区 .tmp/go-agent-ref/golden 中查找真实 Java 响应样本；
// 仓库内/CI 环境没有该目录时跳过 golden 比对。
func findGolden(t *testing.T, name string) map[string]any {
	t.Helper()
	candidates := []string{
		filepath.Join("..", "..", "..", "..", "go-agent-ref", "golden", name),
		filepath.Join("..", "..", "..", "..", ".tmp", "go-agent-ref", "golden", name),
		filepath.Join("testdata", name),
	}
	for _, candidate := range candidates {
		content, err := os.ReadFile(candidate)
		if err != nil {
			continue
		}
		var value map[string]any
		if err := json.Unmarshal(content, &value); err != nil {
			t.Fatalf("解析 golden %s 失败: %v", candidate, err)
		}
		return value
	}
	t.Skipf("golden 样本不存在: %s", name)
	return nil
}

type shapeKind string

const (
	kindString shapeKind = "string"
	kindNumber shapeKind = "number"
	kindBool   shapeKind = "bool"
	kindArray  shapeKind = "array"
	kindObject shapeKind = "object"
	kindNull   shapeKind = "null"
)

func kindOf(value any) shapeKind {
	if value == nil {
		return kindNull
	}
	switch value.(type) {
	case string:
		return kindString
	case json.Number:
		return kindNumber
	case float64, float32, int, int64, int32, int16, int8, uint, uint64, uint32:
		return kindNumber
	case bool:
		return kindBool
	}
	reflected := reflect.ValueOf(value)
	switch reflected.Kind() {
	case reflect.Slice, reflect.Array:
		return kindArray
	case reflect.Map:
		return kindObject
	case reflect.Ptr, reflect.Interface:
		return kindOf(reflected.Elem().Interface())
	default:
		return kindNull
	}
}

// assertShape 递归比较 JSON 结构（字段名 + 类型），不比较具体值。
func assertShape(t *testing.T, path string, golden, actual any) {
	t.Helper()
	goldenKind := kindOf(golden)
	actualKind := kindOf(actual)
	if goldenKind != actualKind {
		t.Errorf("%s 类型不一致: golden=%s actual=%s", path, goldenKind, actualKind)
		return
	}
	switch goldenKind {
	case kindObject:
		goldenMap := golden.(map[string]any)
		actualMap, ok := actual.(map[string]any)
		if !ok {
			return
		}
		for key, goldenValue := range goldenMap {
			actualValue, exists := actualMap[key]
			if !exists {
				t.Errorf("%s.%s 缺失", path, key)
				continue
			}
			assertShape(t, path+"."+key, goldenValue, actualValue)
		}
	case kindArray:
		goldenArray := toAnySlice(golden)
		actualArray := toAnySlice(actual)
		if len(goldenArray) == 0 || len(actualArray) == 0 {
			return
		}
		assertShape(t, path+"[0]", goldenArray[0], actualArray[0])
	}
}

func toAnySlice(value any) []any {
	switch typed := value.(type) {
	case []any:
		return typed
	case []string:
		result := make([]any, len(typed))
		for index, item := range typed {
			result[index] = item
		}
		return result
	case []int:
		result := make([]any, len(typed))
		for index, item := range typed {
			result[index] = item
		}
		return result
	case []float64:
		result := make([]any, len(typed))
		for index, item := range typed {
			result[index] = item
		}
		return result
	case []map[string]any:
		result := make([]any, len(typed))
		for index, item := range typed {
			result[index] = item
		}
		return result
	default:
		return nil
	}
}

// TestGoldenStatInfoShape 用真实 Java 插件端 /get-stat-info 响应比对字段名与类型。
func TestGoldenStatInfoShape(t *testing.T) {
	if runtime.GOOS != "linux" {
		t.Skip("需要 /proc 与 /sys")
	}
	golden := findGolden(t, "stat-info.json")
	goldenData, _ := golden["data"].(map[string]any)
	if goldenData == nil {
		t.Skip("golden 无 data")
	}
	status, err := SimpleStatus(nil)
	if err != nil {
		t.Fatalf("SimpleStatus: %v", err)
	}
	systemInfo, err := SystemInfo()
	if err != nil {
		t.Fatalf("SystemInfo: %v", err)
	}
	a := newTestApp(t)
	actual := map[string]any{
		"simpleStatus": status,
		"systemSleep":  SystemSleep,
		"systemInfo":   systemInfo,
		"jpomInfo":     JpomInfo(a),
	}
	for _, key := range []string{"simpleStatus", "systemInfo", "jpomInfo"} {
		goldenValue, ok := goldenData[key]
		if !ok {
			t.Errorf("golden 缺少 %s", key)
			continue
		}
		if key == "simpleStatus" {
			// swapMemory/virtualMemory 与运行环境是否启用 swap 相关，允许本地缺失。
			if goldenStatus, ok := goldenValue.(map[string]any); ok {
				if actualStatus, ok := actual[key].(map[string]any); ok {
					for optional := range map[string]bool{"swapMemory": true, "virtualMemory": true} {
						if _, exists := actualStatus[optional]; !exists {
							delete(goldenStatus, optional)
						}
					}
				}
			}
		}
		assertShape(t, key, goldenValue, actual[key])
	}
	if goldenData["systemSleep"] != nil {
		assertShape(t, "systemSleep", goldenData["systemSleep"], actual["systemSleep"])
	}
}

// TestGoldenDiskNetworkShape 比对 /disk-info、/hw-disk--info、/network-interfaces、/processList。
func TestGoldenDiskNetworkShape(t *testing.T) {
	if runtime.GOOS != "linux" {
		t.Skip("需要 /sys")
	}
	cases := map[string]func() (any, error){
		"disk-info.json": func() (any, error) {
			list, err := FileStoresJSON()
			return list, err
		},
		"hw-disk-info.json": func() (any, error) {
			list, err := DiskStoresJSON()
			return list, err
		},
		"net-interfaces.json": func() (any, error) {
			list, err := NetworkInterfacesJSON()
			return list, err
		},
		"processList.json": func() (any, error) {
			list, err := ProcessListJSON("java", 5)
			return list, err
		},
	}
	for name, collect := range cases {
		golden := findGolden(t, name)
		goldenData, _ := golden["data"].([]any)
		if len(goldenData) == 0 {
			t.Logf("%s golden 为空,跳过", name)
			continue
		}
		actual, err := collect()
		if err != nil {
			t.Errorf("%s: %v", name, err)
			continue
		}
		actualList := toAnySlice(actual)
		if len(actualList) == 0 {
			t.Logf("%s 当前环境无数据,跳过元素比对", name)
			continue
		}
		assertShape(t, name+"[0]", goldenData[0], actualList[0])
	}
}

// TestGoldenCacheShape 比对 /system/cache 字段名与类型。
func TestGoldenCacheShape(t *testing.T) {
	golden := findGolden(t, "sys-cache.json")
	goldenData, _ := golden["data"].(map[string]any)
	if goldenData == nil {
		t.Skip("golden 无 data")
	}
	a := newTestApp(t)
	RefreshCache(a)
	actual := CacheJSON(a, "DEFAULT")
	optional := map[string]bool{"envVarKeys": true, "scriptLibraryTagMap": true}
	for key := range goldenData {
		actualValue, exists := actual[key]
		if !exists {
			if optional[key] {
				continue
			}
			t.Errorf("cache.%s 缺失", key)
			continue
		}
		assertShape(t, "cache."+key, goldenData[key], actualValue)
	}
}
