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
	"strings"
	"time"

	"gopkg.in/yaml.v3"

	"jpom.io/go-agent/internal/app"
)

// UpgradeMessage 对应 Const.UPGRADE_MSG。
const UpgradeMessage = "升级(重启)中大约需要30秒～2分钟左右"

// SystemConfigContent 对应 /system/getConfig.json：返回配置原文与绝对路径。
func SystemConfigContent(a *app.App) (map[string]any, error) {
	content, err := os.ReadFile(a.ConfPath)
	if err != nil {
		return nil, fmt.Errorf("读取配置文件失败: %w", err)
	}
	absolute, err := filepath.Abs(a.ConfPath)
	if err != nil {
		absolute = a.ConfPath
	}
	return map[string]any{
		"content": string(content),
		"file":    absolute,
	}, nil
}

// SaveSystemConfig 对应 /system/save_config.json。
//
// 与 Java 端一致：先用 YAML 语法校验（tab 转两个空格后解析），校验通过后把原始
// 内容写回 conf/application.yml；restart 为 true 时延迟 2 秒重启进程（配置在重启
// 后生效，未重启时不会热加载）。
func SaveSystemConfig(a *app.App, content, restartParam string) (string, error) {
	if strings.TrimSpace(content) == "" {
		return "", &ParamError{Code: 405, Msg: "内容不能为空"}
	}
	validator := strings.ReplaceAll(content, "\t", "  ")
	var parsed map[string]any
	if err := yaml.Unmarshal([]byte(validator), &parsed); err != nil {
		return "", &ParamError{Code: 500, Msg: "内容格式错误，请检查修正:" + err.Error()}
	}
	info, err := os.Stat(a.ConfPath)
	mode := os.FileMode(0o644)
	if err == nil {
		mode = info.Mode().Perm()
	}
	if err := os.WriteFile(a.ConfPath, []byte(content), mode); err != nil {
		return "", fmt.Errorf("写入配置文件失败: %w", err)
	}
	if parseBool(restartParam) {
		a.RestartAfter(2 * time.Second)
		return UpgradeMessage, nil
	}
	return "修改成功", nil
}

func parseBool(value string) bool {
	switch strings.ToLower(strings.TrimSpace(value)) {
	case "true", "1", "yes", "y", "on":
		return true
	default:
		return false
	}
}

// ParamError 表示参数/状态校验失败（Java 端由参数拦截器或 Assert 抛出，body code
// 为 405），由 api 层转换成对应的响应码。
type ParamError struct {
	Code int
	Msg  string
}

func (e *ParamError) Error() string { return e.Msg }
