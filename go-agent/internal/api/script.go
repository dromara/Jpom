/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package api

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/dsl"
	"github.com/dromara/Jpom/go-agent/internal/httpx"
	"github.com/dromara/Jpom/go-agent/internal/script"
	"github.com/dromara/Jpom/go-agent/internal/sysinfo"
	"github.com/dromara/Jpom/go-agent/internal/ws"
)

// scriptRuntime holds the process wide script domain singletons. The api
// package is created once per agent process; ws.go reuses the same instance for
// the /script_run and /console websockets.
type scriptRuntime struct {
	app         *app.App
	manager     *script.Manager
	executor    *script.Executor
	ws          *ws.Manager
	matchSecond bool
	lang        string
}

// scriptCronRegistry adapts the unified agent cron scheduler
// (sysinfo.CronManager) so autoExecCron tasks show up in /system/cache.taskList.
type scriptCronRegistry struct{}

func (scriptCronRegistry) Register(id, expression string, fn func()) error {
	return sysinfo.RegisterCronTask(id, expression, fn)
}

func (scriptCronRegistry) Remove(id string) error { return sysinfo.RemoveCronTask(id) }

var scriptRuntimes sync.Map

// runtimeFor returns (and lazily creates) the script runtime of one App.
func runtimeFor(a *app.App) *scriptRuntime {
	if value, ok := scriptRuntimes.Load(a); ok {
		return value.(*scriptRuntime)
	}
	manager := script.NewManager(a)
	executor := script.NewExecutor(manager)
	lang := ""
	matchSecond := false
	if a.Config != nil {
		lang = a.Config.Jpom.System.Lang
		matchSecond = a.Config.Jpom.System.TimerMatchSecond
	}
	runtime := &scriptRuntime{
		app:         a,
		manager:     manager,
		executor:    executor,
		ws:          ws.NewManager(a, manager, executor),
		matchSecond: matchSecond,
		lang:        lang,
	}
	actual, loaded := scriptRuntimes.LoadOrStore(a, runtime)
	if loaded {
		return actual.(*scriptRuntime)
	}
	return runtime
}

// nowText is the Java yyyy-MM-dd HH:mm:ss timestamp.
func nowText() string { return time.Now().Format("2006-01-02 15:04:05") }

// nowMillis is the Java SystemClock.now() value.
func nowMillis() int64 { return time.Now().UnixMilli() }

// serverUser returns the decoded Jpom-Server-UserName header or "-".
func serverUser(c *httpx.Context) string {
	name := c.ServerUser
	if name == "" {
		return "-"
	}
	if decoded, err := url.QueryUnescape(name); err == nil {
		name = decoded
	}
	if name == "" {
		return "-"
	}
	return name
}

// scriptBool parses a form boolean with the Java Convert.toBool default
// (private to this file: other api files define their own form helpers).
func scriptBool(c *httpx.Context, key string, fallback bool) bool {
	value := strings.TrimSpace(c.Form(key))
	if value == "" {
		return fallback
	}
	parsed, err := strconv.ParseBool(value)
	if err != nil {
		return fallback
	}
	return parsed
}

// validCron performs a light validation so obviously broken expressions are
// rejected like the Java agent does with Hutool CronPattern. A leading "!" means
// "do not schedule" and is accepted as-is.
func validCron(value string) bool {
	if strings.HasPrefix(value, "!") {
		return true
	}
	fields := strings.Fields(value)
	if len(fields) < 5 || len(fields) > 7 {
		return false
	}
	for _, field := range fields {
		for _, char := range field {
			switch {
			case char >= '0' && char <= '9':
			case char >= 'A' && char <= 'Z':
			case char >= 'a' && char <= 'z':
			case char == '*' || char == '/' || char == ',' || char == '-' || char == '?' || char == '#' || char == 'L' || char == 'W':
			default:
				return false
			}
		}
	}
	return true
}

// checkCron validates the cron expression (StringUtil.checkCron).
func checkCron(value string) (string, bool) {
	trimmed := strings.TrimSpace(value)
	if trimmed == "" {
		return "", true
	}
	if !validCron(trimmed) {
		return "", false
	}
	return trimmed, true
}

// codeBusinessError is the agent business error code: the Java agent maps
// IllegalArgumentException/IllegalStateException (Assert.*) to code 405.
const codeBusinessError = 405

// scriptFailBusiness writes an Assert-style business error (code 405).
func scriptFailBusiness(c *httpx.Context, message string) {
	c.JSONCode(codeBusinessError, message, nil)
}

// scriptFailParam writes a Spring @ValidatorItem validation failure (code 400;
// custom message first, default 参数验证失败).
func scriptFailParam(c *httpx.Context, message string) {
	if strings.TrimSpace(message) == "" {
		message = c.T("i18n.parameter_validation_failed.f0a1")
	}
	c.JSONCode(400, message, nil)
}

// scriptFailFormat writes a request body parsing failure
// (HttpMessageNotReadableException -> HTTP 417, i18n.incorrect_parameter_format).
func scriptFailFormat(c *httpx.Context) {
	c.JSONCode(417, c.T("i18n.incorrect_parameter_format.9efb"), nil)
}

// scriptFailInternal writes a runtime error (code 500, msg=服务异常：<message>).
func scriptFailInternal(c *httpx.Context, err error) {
	c.App.Log().Error("script handler error", "path", c.R.URL.Path, "error", err)
	c.JSONCode(500, c.T("i18n.service_exception.3821")+err.Error(), nil)
}

// scriptSaveFields copies the editable NodeScriptModel fields of a form request.
func scriptSaveFields(c *httpx.Context) map[string]any {
	fields := map[string]any{}
	if c.HasForm("name") {
		fields["name"] = c.Form("name")
	}
	if c.HasForm("context") {
		fields["context"] = c.Form("context")
	}
	if c.HasForm("description") {
		fields["description"] = c.Form("description")
	}
	if c.HasForm("defArgs") {
		fields["defArgs"] = script.ConvertToParam(c.Form("defArgs"))
	}
	return fields
}

// registerScript owns /script/* and /script-library/* endpoints.
func registerScript(r *httpx.Router, a *app.App) {
	rt := runtimeFor(a)
	// 把增强 DSL 执行器注册到项目域（/manage/operate 的 DSL 流程会走这里），
	// 语言使用代理配置语言（DslExecutor 接口无请求级 lang）。
	dsl.Register(rt.manager, rt.lang)
	// autoExecCron 统一注册到 sysinfo 调度器（等价 Java NodeScriptServer ICron#queryStartingList）
	rt.manager.RegisterScriptCrons(scriptCronRegistry{}, rt.executor, rt.matchSecond, rt.lang)

	r.Handle(http.MethodPost, "/script/list.json", func(c *httpx.Context) {
		list, err := rt.manager.List()
		if err != nil {
			scriptFailInternal(c, err)
			return
		}
		for _, item := range list {
			normalizeScriptItem(item)
		}
		c.OK(list)
	})

	r.Handle(http.MethodPost, "/script/item.json", func(c *httpx.Context) {
		item, exists, err := rt.manager.Get(c.Form("id"))
		if err != nil {
			scriptFailInternal(c, err)
			return
		}
		if !exists {
			c.OK(nil)
			return
		}
		normalizeScriptItem(item)
		c.OK(item)
	})

	r.Handle(http.MethodPost, "/script/save.json", func(c *httpx.Context) {
		context := c.Form("context")
		if strings.TrimSpace(context) == "" {
			scriptFailBusiness(c, c.T("i18n.content_is_empty.3122"))
			return
		}
		autoExecCron, cronOK := checkCron(c.Form("autoExecCron"))
		if !cronOK {
			scriptFailBusiness(c, c.T("i18n.cron_expression_format_error.6dcd"))
			return
		}
		global := scriptBool(c, "global", false)
		workspaceID := strings.TrimSpace(c.R.Header.Get("workspaceId"))
		if global {
			workspaceID = "GLOBAL"
		}
		nodeID := c.Form("nodeId")
		id := c.Form("id")
		existing, exists, err := rt.manager.Get(id)
		if err != nil {
			scriptFailInternal(c, err)
			return
		}
		switch strings.ToLower(strings.TrimSpace(c.Form("type"))) {
		case "add":
			if exists {
				scriptFailBusiness(c, c.T("i18n.id_already_exists.6208"))
				return
			}
			item := scriptSaveFields(c)
			item["id"] = script.NewExecutionID()
			// Java add 分支不调用 setAutoExecCron/setDefArgs，缺参时字段为 null
			// （Jackson NON_NULL 省略）；仅请求里出现过的字段才写入。
			if c.HasForm("autoExecCron") {
				item["autoExecCron"] = autoExecCron
			}
			item["workspaceId"] = workspaceID
			if c.HasForm("nodeId") {
				item["nodeId"] = nodeID
			}
			item["createTime"] = nowText()
			// Java ScriptController.add 不写 lastRunUser（NodeScriptModel getter 在
			// 读取时回退 "-"），Go 不在落盘数据里写该字段（P3-2）。
			user := serverUser(c)
			if user != "-" {
				item["createUser"] = user
				item["modifyUser"] = user
			}
			if err := rt.manager.Add(item); err != nil {
				scriptFailBusiness(c, err.Error())
				return
			}
			rt.manager.UpsertScriptCron(scriptCronRegistry{}, rt.executor, stringField(item, "id"), rt.matchSecond, rt.lang)
			c.OKMsg(c.T("i18n.addition_succeeded.3fda"), nil)
		case "sync":
			item := map[string]any{}
			if exists {
				item = existing
			} else {
				item["id"] = id
				if c.HasForm("nodeId") {
					item["nodeId"] = nodeID
				}
			}
			if exists && !isGlobal(existing) && global && c.HasForm("nodeId") {
				item["nodeId"] = nodeID
			}
			item["scriptType"] = "server-sync"
			item["workspaceId"] = workspaceID
			for key, value := range scriptSaveFields(c) {
				item[key] = value
			}
			// Java sync 分支始终调用 eModel.setDefArgs(nodeScriptModel.getDefArgs())：
			// 缺参时 convertToParam(null) -> "[]"（列表接口因此输出 []）。
			item["defArgs"] = script.ConvertToParam(c.Form("defArgs"))
			item["autoExecCron"] = autoExecCron
			item["modifyTime"] = nowText()
			if user := serverUser(c); user != "-" {
				item["modifyUser"] = user
			}
			if exists {
				if err := rt.manager.UpdateFields(id, item); err != nil {
					scriptFailBusiness(c, err.Error())
					return
				}
			} else {
				item["id"] = id
				item["createTime"] = nowText()
				if user := serverUser(c); user != "-" {
					item["createUser"] = user
				}
				if err := rt.manager.Add(item); err != nil {
					scriptFailBusiness(c, err.Error())
					return
				}
			}
			rt.manager.UpsertScriptCron(scriptCronRegistry{}, rt.executor, id, rt.matchSecond, rt.lang)
			c.OKMsg(c.T("i18n.modify_success.69be"), nil)
		default:
			if !exists {
				scriptFailBusiness(c, c.T("i18n.no_data.1ac0"))
				return
			}
			item := scriptSaveFields(c)
			// Java edit 分支始终 setDefArgs(...)：缺参 -> "[]"；autoExecCron 缺参 -> ""
			item["defArgs"] = script.ConvertToParam(c.Form("defArgs"))
			item["autoExecCron"] = autoExecCron
			item["modifyTime"] = nowText()
			if user := serverUser(c); user != "-" {
				item["modifyUser"] = user
			}
			if err := rt.manager.UpdateFields(id, item); err != nil {
				scriptFailBusiness(c, err.Error())
				return
			}
			rt.manager.UpsertScriptCron(scriptCronRegistry{}, rt.executor, id, rt.matchSecond, rt.lang)
			c.OKMsg(c.T("i18n.modify_success.69be"), nil)
		}
	})

	r.Handle(http.MethodPost, "/script/del.json", func(c *httpx.Context) {
		id := c.Form("id")
		if err := rt.manager.Delete(id); err != nil {
			scriptFailInternal(c, err)
			return
		}
		rt.manager.UpsertScriptCron(scriptCronRegistry{}, rt.executor, id, rt.matchSecond, rt.lang)
		c.OKMsg(c.T("i18n.delete_success.0007"), nil)
	})

	r.Handle(http.MethodPost, "/script/log", func(c *httpx.Context) {
		id := c.Form("id")
		executeID := c.Form("executeId")
		if strings.TrimSpace(id) == "" {
			// @ValidatorItem(msg=log_id_missing) -> code 400
			scriptFailParam(c, c.T("i18n.log_id_missing.3500"))
			return
		}
		if strings.TrimSpace(executeID) == "" {
			// @ValidatorItem(msg=missing_execution_id) -> code 400
			scriptFailParam(c, c.T("i18n.missing_execution_id.14d0"))
			return
		}
		// @ValidatorItem(POSITIVE_INTEGER, msg=incorrect_line_number)：缺省/非数字/
		// 负数都按 NOT_EMPTY/POSITIVE_INTEGER 校验失败 -> 400；实测 line=0 合法。
		lineText := strings.TrimSpace(c.Form("line"))
		line := 0
		if lineText != "" {
			parsed, err := strconv.Atoi(lineText)
			if err != nil || parsed < 0 {
				scriptFailParam(c, c.T("i18n.incorrect_line_number.5877"))
				return
			}
			line = parsed
		} else {
			scriptFailParam(c, c.T("i18n.incorrect_line_number.5877"))
			return
		}
		item, exists, err := rt.manager.Get(id)
		if err != nil {
			scriptFailInternal(c, err)
			return
		}
		_ = item
		if !exists {
			scriptFailBusiness(c, c.T("i18n.no_data_found.4ffb"))
			return
		}
		logFile := rt.manager.LogFile(id, executeID)
		total, lines, err := script.ReadLogFile(logFile, line)
		if err != nil {
			scriptFailBusiness(c, c.T("i18n.log_file_error.473b"))
			return
		}
		// 运行中判断
		c.OK(map[string]any{
			"line":      total + 1,
			"getLine":   line,
			"dataLines": lines,
			"run":       rt.executor.IsRun(executeID),
		})
	})

	r.Handle(http.MethodPost, "/script/del_log", func(c *httpx.Context) {
		id := c.Form("id")
		executeID := c.Form("executeId")
		if strings.TrimSpace(id) == "" {
			scriptFailParam(c, c.T("i18n.log_id_missing.3500"))
			return
		}
		if strings.TrimSpace(executeID) == "" {
			scriptFailParam(c, c.T("i18n.missing_execution_id.14d0"))
			return
		}
		_, exists, err := rt.manager.Get(id)
		if err != nil {
			scriptFailInternal(c, err)
			return
		}
		if !exists {
			c.OKMsg(c.T("i18n.script_template_not_exist.1d5b"), nil)
			return
		}
		stillExists, err := rt.manager.DeleteLog(id, executeID)
		if err != nil {
			scriptFailInternal(c, err)
			return
		}
		if stillExists {
			scriptFailBusiness(c, c.T("i18n.delete_log_file_failure.bf0b"))
			return
		}
		c.OKMsg(c.T("i18n.delete_success.0007"), nil)
	})

	r.Handle(http.MethodPost, "/script/exec", func(c *httpx.Context) {
		id := c.Form("id")
		if strings.TrimSpace(id) == "" {
			// exec(@ValidatorItem String id) 未配置文案 -> 400 参数验证失败
			scriptFailParam(c, "")
			return
		}
		item, exists, err := rt.manager.Get(id)
		if err != nil {
			scriptFailInternal(c, err)
			return
		}
		if !exists {
			scriptFailBusiness(c, c.T("i18n.script_not_exist.b180"))
			return
		}
		args := c.Form("args")
		if strings.TrimSpace(args) == "" {
			args = stringField(item, "defArgs")
		}
		params, parseErr := script.TriggerParamMap(c.Form("params"))
		if parseErr != nil {
			// Java: JSONObject.parseObject(params) 抛 JSONException -> 500 服务异常
			scriptFailInternal(c, parseErr)
			return
		}
		executeID := script.NewExecutionID()
		record := map[string]any{
			"id":               executeID,
			"createTimeMillis": nowMillis(),
			"scriptId":         id,
			"scriptName":       stringField(item, "name"),
			"modifyUser":       serverUser(c),
			"workspaceId":      workspaceField(item),
			"triggerExecType":  2,
		}
		if err := rt.manager.AddExecLog(record); err != nil {
			scriptFailInternal(c, err)
			return
		}
		if err := rt.executor.Start(item, executeID, args, params, c.Lang); err != nil {
			scriptFailBusiness(c, err.Error())
			return
		}
		c.OKMsg(c.T("i18n.start_execution.00d7"), executeID)
	})

	r.Handle(http.MethodPost, "/script/pull_exec_log", func(c *httpx.Context) {
		text := strings.TrimSpace(c.Form("pullCount"))
		if text == "" {
			// @ValidatorItem int pullCount 缺省 -> NOT_EMPTY 校验失败 -> 400 参数验证失败
			scriptFailParam(c, "")
			return
		}
		pullCount, err := strconv.Atoi(text)
		if err != nil {
			// 非数字：@ValidatorItem 只校验非空，随后 Spring 绑定失败 -> 运行期 500
			scriptFailInternal(c, err)
			return
		}
		if pullCount <= 0 {
			// Assert.state(pullCount > 0, "pull count error") -> 405
			scriptFailBusiness(c, "pull count error")
			return
		}
		list, err := rt.manager.ListExecLogs()
		if err != nil {
			scriptFailInternal(c, err)
			return
		}
		if len(list) > pullCount {
			list = list[:pullCount]
		}
		for _, record := range list {
			if value, ok := record["createTimeMillis"]; ok && value != nil {
				record["createTimeMillis"] = jsonNumberString(value)
			}
		}
		c.OK(list)
	})

	r.Handle(http.MethodPost, "/script/del_exec_log", func(c *httpx.Context) {
		var body struct {
			IDs []string `json:"ids"`
		}
		if err := c.BindJSON(&body); err != nil {
			// @RequestBody 解析失败 -> 417 incorrect_parameter_format
			scriptFailFormat(c)
			return
		}
		for _, id := range body.IDs {
			if err := rt.manager.DeleteExecLog(id); err != nil {
				scriptFailInternal(c, err)
				return
			}
		}
		c.OKMsg(c.T("i18n.delete_success.0007"), nil)
	})

	r.Handle(http.MethodPost, "/script/change-workspace-id", func(c *httpx.Context) {
		id := c.Form("id")
		if strings.TrimSpace(id) == "" {
			scriptFailParam(c, "")
			return
		}
		newWorkspaceID := c.Form("newWorkspaceId")
		if strings.TrimSpace(newWorkspaceID) == "" {
			scriptFailBusiness(c, c.T("i18n.select_workspace_to_modify.ac87"))
			return
		}
		_, exists, err := rt.manager.Get(id)
		if err != nil {
			scriptFailInternal(c, err)
			return
		}
		if !exists {
			scriptFailBusiness(c, c.T("i18n.script_info_not_found.bd8d"))
			return
		}
		fields := map[string]any{
			"workspaceId": newWorkspaceID,
			"nodeId":      c.Form("newNodeId"),
			"modifyTime":  nowText(),
		}
		if user := serverUser(c); user != "-" {
			fields["modifyUser"] = user
		}
		if err := rt.manager.UpdateFields(id, fields); err != nil {
			scriptFailBusiness(c, err.Error())
			return
		}
		c.OKMsg(c.T("i18n.modify_success.69be"), nil)
	})

	// ---- 脚本库 ----

	r.Handle(http.MethodPost, "/script-library/list", func(c *httpx.Context) {
		list, err := rt.manager.LibraryList()
		if err != nil {
			scriptFailInternal(c, err)
			return
		}
		c.OK(list)
	})

	r.Handle(http.MethodPost, "/script-library/get", func(c *httpx.Context) {
		if strings.TrimSpace(c.Form("id")) == "" {
			// @ValidatorItem String id -> 400 参数验证失败
			scriptFailParam(c, "")
			return
		}
		item, exists, err := rt.manager.LibraryGet(c.Form("id"))
		if err != nil {
			scriptFailInternal(c, err)
			return
		}
		if !exists {
			scriptFailBusiness(c, c.T("i18n.missing_script_message.af89"))
			return
		}
		c.OK(item)
	})

	r.Handle(http.MethodPost, "/script-library/save", func(c *httpx.Context) {
		id := strings.TrimSpace(c.Form("id"))
		if id == "" {
			// @ValidatorItem String id -> 400 参数验证失败
			scriptFailParam(c, "")
			return
		}
		content := c.Form("script")
		if strings.TrimSpace(content) == "" {
			// @ValidatorItem(msg=script_content_cannot_be_empty) -> 400
			scriptFailParam(c, c.T("i18n.script_content_cannot_be_empty.49be"))
			return
		}
		if err := rt.manager.LibrarySave(id, content, c.Form("description"), c.Form("version")); err != nil {
			scriptFailInternal(c, err)
			return
		}
		c.OKMsg(c.T("i18n.save_succeeded.3b10"), nil)
	})

	r.Handle(http.MethodPost, "/script-library/del", func(c *httpx.Context) {
		if strings.TrimSpace(c.Form("id")) == "" {
			// @ValidatorItem String id -> 400 参数验证失败
			scriptFailParam(c, "")
			return
		}
		if err := rt.manager.LibraryDelete(c.Form("id")); err != nil {
			scriptFailInternal(c, err)
			return
		}
		c.OKMsg(c.T("i18n.delete_success.0007"), nil)
	})
}

func isGlobal(item map[string]any) bool {
	return stringField(item, "workspaceId") == "GLOBAL"
}

func workspaceField(item map[string]any) string {
	workspaceID := stringField(item, "workspaceId")
	if workspaceID == "" {
		return "DEFAULT"
	}
	return workspaceID
}

// stringField reads a string field from a decoded script record.
func stringField(item map[string]any, key string) string {
	if item == nil {
		return ""
	}
	if value, ok := item[key].(string); ok {
		return value
	}
	return ""
}

// normalizeScriptItem applies the Java getter defaults: NodeScriptModel's
// lastRunUser getter returns "-" when the field is empty.
func normalizeScriptItem(item map[string]any) {
	if item == nil {
		return
	}
	if strings.TrimSpace(stringField(item, "lastRunUser")) == "" {
		item["lastRunUser"] = "-"
	}
}

// jsonNumberString renders a stored number (json.Number/int64/float64) as the
// string form used by the HTTP Jackson serializer for long fields.
func jsonNumberString(value any) string {
	switch item := value.(type) {
	case json.Number:
		return item.String()
	case string:
		return item
	case int64:
		return strconv.FormatInt(item, 10)
	case int:
		return strconv.Itoa(item)
	case float64:
		return strconv.FormatInt(int64(item), 10)
	default:
		return fmt.Sprint(item)
	}
}
