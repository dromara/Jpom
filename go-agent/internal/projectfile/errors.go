/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package projectfile

import (
	"jpom.io/go-agent/internal/i18n"
)

// LocalizedError is an error carrying a Java i18n key so the HTTP layer can
// render the message in the request language. The Java agent localizes every
// user facing message with I18nMessageUtil; code=405 maps to Spring Assert /
// JsonMessage.fail validation errors, code=500 to unexpected exceptions.
//
// Error() renders the zh-CN text, which keeps logs and unit tests readable
// without an HTTP request context.
type LocalizedError struct {
	key    string
	args   []any
	suffix string
	code   int
	text   string
}

// Error implements error.
func (e *LocalizedError) Error() string { return e.text }

// HTTPCode returns the JSON envelope code (405 validation / 500 failure).
func (e *LocalizedError) HTTPCode() int { return e.code }

// I18nKey returns the message key rendered by the HTTP layer.
func (e *LocalizedError) I18nKey() string { return e.key }

// I18nArgs returns the placeholder arguments of the message key.
func (e *LocalizedError) I18nArgs() []any { return e.args }

// I18nSuffix returns text appended after the localized message, used where the
// Java agent concatenates raw values (ids, paths, original exception messages).
func (e *LocalizedError) I18nSuffix() string { return e.suffix }

// localized builds an error from a message key plus an optional suffix.
func localized(code int, key, suffix string, args ...any) error {
	text := suffix
	if key != "" {
		text = i18n.T(i18n.LangZhCn, key, args...) + suffix
	}
	return &LocalizedError{key: key, args: args, suffix: suffix, code: code, text: text}
}

// invalidf builds a 405 validation error from an existing message key with {}
// placeholders (Java Assert / JsonMessage.fail semantics).
func invalidf(key string, args ...any) error {
	return localized(405, key, "", args...)
}

// invalidParamf builds a 400 parameter validation error, matching the Java
// @ValidatorItem interceptor (ParameterInterceptor, default code 400).
func invalidParamf(key string, args ...any) error {
	return localized(400, key, "", args...)
}

// invalidParamMessage builds a 400 parameter validation error without a key.
func invalidParamMessage(message string) error {
	return &LocalizedError{code: 400, text: message}
}

// invalidSuffix builds a 405 validation error from a key plus a raw suffix.
func invalidSuffix(key, suffix string) error {
	return localized(405, key, suffix)
}

// invalidMessage builds a 405 validation error without an i18n key, for Go only
// guards such as path traversal protection (Java throws an English
// IllegalArgumentException for those and the message is not localized).
func invalidMessage(message string) error {
	return &LocalizedError{code: 405, text: message}
}

// failSuffix builds a 500 error from a key plus a raw suffix.
func failSuffix(key, suffix string) error {
	return localized(500, key, suffix)
}

// failMessage builds a 500 error without an i18n key (Go only IO failures and
// guards, where the Java agent would surface a raw runtime exception message).
func failMessage(message string) error {
	return &LocalizedError{code: 500, text: message}
}

// failKey builds a 500 error from a message key.
func failKey(key string, args ...any) error {
	return localized(500, key, "", args...)
}

// Message is a localized success message returned by service methods that the
// Java agent reports with I18nMessageUtil (the HTTP layer renders it with the
// request language).
type Message struct {
	// Key is the i18n message key.
	Key string
	// Args are the {} placeholder arguments of Key.
	Args []any
	// Suffix is raw text appended after the localized message (file size,
	// user supplied names...).
	Suffix string
}

// Text renders the message in lang.
func (m Message) Text(lang string) string {
	if m.Key == "" {
		return m.Suffix
	}
	return i18n.T(lang, m.Key, m.Args...) + m.Suffix
}

// HTTPCodeOf returns the JSON envelope code matching the Java agent behaviour:
// 405 for validation failures, 500 for every other error.
func HTTPCodeOf(err error) int {
	if err == nil {
		return 200
	}
	if localizedErr, ok := err.(*LocalizedError); ok {
		return localizedErr.code
	}
	return 500
}
