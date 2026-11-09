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
	"errors"
	"fmt"

	"jpom.io/go-agent/internal/i18n"
)

// ErrorKind 业务错误类型，决定 HTTP 响应业务码（对齐 Java BaseExceptionHandler）。
type ErrorKind int

const (
	// ErrValidation 参数/状态异常（Java Assert/IllegalArgument/IllegalState/ValidateException）→ code=405。
	ErrValidation ErrorKind = iota
	// ErrService 运行期异常（Java RuntimeException/Exception）→ code=500，msg 前缀“服务异常：”。
	ErrService
	// ErrBusiness 业务显式构造的 code=500（如日志备份目录不存在），msg 原样输出。
	ErrBusiness
)

// Error 带错误类型与 i18n key 的项目域错误。
//
// Key 非空时 Error() 返回 zh-CN 文案（用于日志），HTTP 响应按请求语言调用 Localized。
type Error struct {
	Kind   ErrorKind
	Msg    string
	Key    string
	Args   []any
	Suffix string // 拼接到文案末尾的动态内容（如 id/路径，Java 端字符串拼接）
}

// Error 实现 error（zh-CN 文案，便于日志排查）。
func (e *Error) Error() string {
	if e.Key != "" {
		if text := i18n.T("zh-CN", e.Key, e.Args...); text != "" {
			return text + e.Suffix
		}
	}
	return e.Msg
}

// Localized 返回指定语言的错误文案。
func (e *Error) Localized(lang string) string {
	if e.Key != "" {
		if text := i18n.T(lang, e.Key, e.Args...); text != "" {
			return text + e.Suffix
		}
	}
	return e.Msg
}

// Validationf 构造参数/状态异常（405），msg 使用 fmt 语义格式化。
func Validationf(format string, args ...any) error {
	return &Error{Kind: ErrValidation, Msg: fmt.Sprintf(format, args...)}
}

// ValidationKey 按 i18n key 构造参数/状态异常（405）。
func ValidationKey(key string, args ...any) error {
	return &Error{Kind: ErrValidation, Key: key, Args: args}
}

// ValidationKeySuffix 按 i18n key + 末尾拼接内容构造参数/状态异常（405）。
func ValidationKeySuffix(key, suffix string, args ...any) error {
	return &Error{Kind: ErrValidation, Key: key, Args: args, Suffix: suffix}
}

// Servicef 构造运行期异常（500+服务异常前缀）。
func Servicef(format string, args ...any) error {
	return &Error{Kind: ErrService, Msg: fmt.Sprintf(format, args...)}
}

// ServiceKey 按 i18n key 构造运行期异常（500）。
func ServiceKey(key string, args ...any) error {
	return &Error{Kind: ErrService, Key: key, Args: args}
}

// ServiceKeySuffix 按 i18n key + 末尾拼接内容构造运行期异常（500）。
func ServiceKeySuffix(key, suffix string, args ...any) error {
	return &Error{Kind: ErrService, Key: key, Args: args, Suffix: suffix}
}

// ServiceError 把底层错误包装成运行期异常（保持原始信息）。
func ServiceError(err error) error {
	if err == nil {
		return nil
	}
	var typed *Error
	if errors.As(err, &typed) {
		return err
	}
	return &Error{Kind: ErrService, Msg: err.Error()}
}

// Businessf 构造业务显式 500 错误（msg 原样输出，无“服务异常：”前缀）。
func Businessf(format string, args ...any) error {
	return &Error{Kind: ErrBusiness, Msg: fmt.Sprintf(format, args...)}
}

// BusinessKey 按 i18n key 构造业务显式 500 错误。
func BusinessKey(key string, args ...any) error {
	return &Error{Kind: ErrBusiness, Key: key, Args: args}
}

// BusinessKeySuffix 按 i18n key + 末尾拼接内容构造业务显式 500 错误。
func BusinessKeySuffix(key, suffix string, args ...any) error {
	return &Error{Kind: ErrBusiness, Key: key, Args: args, Suffix: suffix}
}

// KindOf 返回错误类型；未知错误按运行期异常处理（Java 兜底 500）。
func KindOf(err error) ErrorKind {
	var typed *Error
	if errors.As(err, &typed) {
		return typed.Kind
	}
	return ErrService
}

// LocalizeError 返回错误类型与按语言本地化后的文案。
func LocalizeError(lang string, err error) (ErrorKind, string) {
	if err == nil {
		return ErrService, ""
	}
	var typed *Error
	if errors.As(err, &typed) {
		return typed.Kind, typed.Localized(lang)
	}
	return ErrService, err.Error()
}
