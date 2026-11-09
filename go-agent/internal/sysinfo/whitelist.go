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
	"regexp"
	"sort"
	"strings"

	"jpom.io/go-agent/internal/app"
)

// whitelistFile 与 data/whitelistDirectory.json 的 schema 一致（同
// internal/project.Whitelist 以及 Java AgentWhitelist）。这里直接读写文件，避免与
// project 包产生循环依赖；字段与写入格式保持一致即可互相替换。
type whitelistFile struct {
	Project         []string `json:"project,omitempty"`
	AllowEditSuffix []string `json:"allowEditSuffix,omitempty"`
}

func whitelistPath(a *app.App) string {
	return filepath.Join(a.BaseDir, "data", "whitelistDirectory.json")
}

// ReadWhitelist 读取白名单文件（不存在或内容为空时返回零值）。
func ReadWhitelist(a *app.App) whitelistFile {
	value := whitelistFile{}
	content, err := os.ReadFile(whitelistPath(a))
	if err != nil || len(content) == 0 {
		return value
	}
	if err := jsonUnmarshal(content, &value); err != nil {
		a.Log().Warn("读取白名单失败", "error", err)
	}
	return value
}

func writeWhitelist(a *app.App, value whitelistFile) error {
	return writeJSONAtomic(whitelistPath(a), value)
}

// WhitelistData 对应 /system/whitelistDirectory_data。
func WhitelistData(a *app.App) map[string]any {
	whitelist := ReadWhitelist(a)
	result := map[string]any{}
	if whitelist.Project != nil {
		result["project"] = whitelist.Project
	}
	if whitelist.AllowEditSuffix != nil {
		result["allowEditSuffix"] = whitelist.AllowEditSuffix
	}
	return result
}

// SaveWhitelist 对应 /system/whitelistDirectory_submit。
func SaveWhitelist(a *app.App, projectValue, allowEditSuffixValue string) error {
	projects, err := parseLineList(projectValue, true, "项目路径授权不能为空")
	if err != nil {
		return err
	}
	normalized, err := covertToArray(a, projects)
	if err != nil {
		return err
	}
	if conflict := findStartsWith(normalized); conflict != "" {
		return &ParamError{Code: 405, Msg: "授权目录中不能存在包含关系：" + conflict}
	}
	suffixes, err := parseLineList(allowEditSuffixValue, false, "允许编辑的文件后缀不能为空")
	if err != nil {
		return err
	}
	for _, suffix := range suffixes {
		parts := strings.Split(suffix, "@")
		if len(parts) > 1 {
			charset := strings.TrimSpace(parts[len(parts)-1])
			if !isValidCharsetName(charset) {
				return &ParamError{Code: 405, Msg: "配置的字符编码格式不合法：" + suffix}
			}
		}
	}
	return writeWhitelist(a, whitelistFile{Project: normalized, AllowEditSuffix: suffixes})
}

// parseLineList 对应 AgentWhitelist.parseToList：按换行拆分为去空白的列表。
func parseLineList(value string, required bool, errorMsg string) ([]string, error) {
	if strings.TrimSpace(value) == "" {
		if required {
			return nil, &ParamError{Code: 405, Msg: errorMsg}
		}
		return nil, nil
	}
	list := make([]string, 0, 4)
	for _, line := range strings.Split(value, "\n") {
		line = strings.TrimSpace(line)
		if line != "" {
			list = append(list, line)
		}
	}
	if len(list) == 0 {
		return nil, &ParamError{Code: 405, Msg: errorMsg}
	}
	return list, nil
}

// covertToArray 对应 AgentWhitelist.covertToArray：绝对路径、非根路径、不能包含
// Jpom 数据目录，并去重。
func covertToArray(a *app.App, list []string) ([]string, error) {
	seen := map[string]struct{}{}
	result := make([]string, 0, len(list))
	base := filepath.Clean(a.BaseDir)
	for _, item := range list {
		normalized := filepath.Clean(item)
		if !filepath.IsAbs(normalized) {
			return nil, &ParamError{Code: 405, Msg: "需要配置绝对路径：" + normalized}
		}
		if parent := filepath.Dir(normalized); parent == normalized {
			return nil, &ParamError{Code: 405, Msg: "不能配置根路径：" + normalized}
		}
		if strings.HasPrefix(base, normalized) {
			return nil, &ParamError{Code: 405, Msg: "项目路径授权不能位于Jpom目录下"}
		}
		if _, ok := seen[normalized]; ok {
			continue
		}
		seen[normalized] = struct{}{}
		result = append(result, normalized)
	}
	return result, nil
}

// findStartsWith 对应 AgentWhitelist.findStartsWith：授权目录不能存在包含关系。
func findStartsWith(list []string) string {
	for index := range list {
		for other := range list {
			if index == other {
				continue
			}
			if isSubPath(list[other], list[index]) {
				return list[index]
			}
		}
	}
	return ""
}

func isSubPath(child, parent string) bool {
	relative, err := filepath.Rel(parent, child)
	if err != nil {
		return false
	}
	if relative == "." {
		return false
	}
	return !strings.HasPrefix(relative, "..")
}

var charsetPattern = regexp.MustCompile("^[A-Za-z][A-Za-z0-9_.:-]*$")

// isValidCharsetName 近似 Java CharsetUtil.charset 的名称校验（非法名称直接报错）。
func isValidCharsetName(name string) bool {
	return charsetPattern.MatchString(strings.TrimSpace(name))
}

// WhitelistProjectDirs 返回排序后的授权目录（供其他模块判断路径）。
func WhitelistProjectDirs(a *app.App) []string {
	dirs := append([]string{}, ReadWhitelist(a).Project...)
	sort.Strings(dirs)
	return dirs
}
