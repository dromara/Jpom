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

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/i18n"
)

// UpgradeMessage 返回升级提示的 i18n key（api 层按请求语言渲染）。
const UpgradeMessage = "i18n.upgrade_duration_message.bab4"

// SystemConfigContent 对应 /system/getConfig.json：返回配置原文与绝对路径。
func SystemConfigContent(a *app.App) (map[string]any, error) {
	content, err := os.ReadFile(a.ConfPath)
	if err != nil {
		return nil, fmt.Errorf("read config file failed: %w", err)
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
	// @ValidatorItem(msg=内容不能为空)：缺参与空串都返回 code 400（Java 2.11.13.1 实测）。
	if strings.TrimSpace(content) == "" {
		return "", paramErr(400, "i18n.content_cannot_be_empty.9f0d")
	}
	validator := strings.ReplaceAll(content, "	", "  ")
	var parsed map[string]any
	if err := yaml.Unmarshal([]byte(validator), &parsed); err != nil {
		return "", paramErrAppend(500, "i18n.content_format_error_with_detail.c846", err.Error())
	}
	info, err := os.Stat(a.ConfPath)
	mode := os.FileMode(0o644)
	if err == nil {
		mode = info.Mode().Perm()
	}
	if err := os.WriteFile(a.ConfPath, []byte(content), mode); err != nil {
		return "", fmt.Errorf("write config file failed: %w", err)
	}
	if parseBool(restartParam) {
		a.RestartAfter(2 * time.Second)
		return UpgradeMessage, nil
	}
	return "i18n.modify_success.69be", nil
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
//
// Key 是 i18n key：api 层会按请求语言渲染；Msg 保存 zh-CN 渲染结果，便于日志/单测
// 直接使用。Key 为空时回退使用 Msg（兼容非 i18n 的动态文案）。
type ParamError struct {
	Code int
	Key  string
	Args []any
	// Suffix 是 Java 端「i18n 文案 + 具体值」拼接场景的尾部内容（4 语言都追加）。
	Suffix string
	Msg    string
}

func (e *ParamError) Error() string { return e.Msg }

// paramErr 构造带 i18n key 的参数错误（Msg 用 zh-CN 渲染，供日志/单测使用）。
func paramErr(code int, key string, args ...any) *ParamError {
	return &ParamError{Code: code, Key: key, Args: args, Msg: i18n.T(i18n.LangZhCn, key, args...)}
}

// paramErrAppend 对应 Java 端「i18n 文案 + 具体值」的拼接（如
// Assert.state(..., i18n.get(x) + value)），值在所有语言下都追加到文案末尾。
func paramErrAppend(code int, key, suffix string) *ParamError {
	return &ParamError{Code: code, Key: key, Suffix: suffix, Msg: i18n.T(i18n.LangZhCn, key) + suffix}
}
