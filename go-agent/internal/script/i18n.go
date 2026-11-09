/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package script

import "strings"

// ConsoleMessages holds the localized script console messages. The Java agent
// resolves them through i18n (Accept-Language / jpom.system.lang); the Go agent
// only needs zh-CN and en-US, the two languages used by the server UI.
type ConsoleMessages struct {
	// ExecutionEnded is i18n execution_ended.b793 ("执行结束:{}" / "End of execution: {}").
	ExecutionEnded string
	// ExecutionCompleted is i18n execution_completed.24a1 ("执行完毕:" / "Execution completed:").
	ExecutionCompleted string
	// StopRunning is i18n stop_running.1d4e ("停止运行" / "stop running").
	StopRunning string
	// ExecutionException is i18n execution_exception.b0d5 ("执行异常" / "execution exception").
	ExecutionException string
	// GeneralExecutionException is i18n general_execution_exception.62e9 ("执行异常：" / "Execution exception:").
	GeneralExecutionException string
}

// IsEnglish reports whether the language tag selects the en-US messages.
func IsEnglish(lang string) bool {
	return strings.HasPrefix(strings.ToLower(strings.TrimSpace(lang)), "en")
}

// MessagesFor resolves the console messages of one language tag.
func MessagesFor(lang string) ConsoleMessages {
	if IsEnglish(lang) {
		return ConsoleMessages{
			ExecutionEnded:            "End of execution: %d",
			ExecutionCompleted:        "Execution completed:%d",
			StopRunning:               "stop running",
			ExecutionException:        "execution exception",
			GeneralExecutionException: "Execution exception:",
		}
	}
	return ConsoleMessages{
		ExecutionEnded:            "执行结束:%d",
		ExecutionCompleted:        "执行完毕:%d",
		StopRunning:               "停止运行",
		ExecutionException:        "执行异常",
		GeneralExecutionException: "执行异常：",
	}
}
