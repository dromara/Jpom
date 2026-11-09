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

import "fmt"

// ValidationError is a request/state validation failure. The Java agent maps
// Spring Assert / JsonMessage.fail failures (IllegalArgumentException and
// IllegalStateException) to the JSON code 405, while unexpected exceptions map
// to code 500. Callers of this package must translate the error with
// HTTPCodeOf.
type ValidationError struct {
	Message string
}

// Error implements error.
func (e *ValidationError) Error() string { return e.Message }

// invalidf builds a validation error (JSON code 405).
func invalidf(format string, args ...any) error {
	return &ValidationError{Message: fmt.Sprintf(format, args...)}
}

// HTTPCodeOf returns the JSON envelope code matching the Java agent behaviour:
// 405 for validation failures, 500 for every other error.
func HTTPCodeOf(err error) int {
	if err == nil {
		return 200
	}
	if _, ok := err.(*ValidationError); ok {
		return 405
	}
	return 500
}
