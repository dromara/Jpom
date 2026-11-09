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
	"os"
	"strconv"
	"strings"

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/httpx"
	"jpom.io/go-agent/internal/proc"
	"jpom.io/go-agent/internal/project"
	"jpom.io/go-agent/internal/sysinfo"
)

// 项目域错误/提示文案 key（与 Java i18n 完全一致）。
const (
	keyIncorrectProjectID     = "i18n.incorrect_project_id.5f70"
	keyProjectIDNotExist      = "i18n.project_id_does_not_exist.6b9b"
	keyInfoToRetrieveNotFound = "i18n.info_to_retrieve_not_found.96d7"
	keyMissingOperationParam  = "i18n.missing_operation_parameter.b16b"
	keyNoProjectFound         = "i18n.no_project_found.ef5e"
	keyOperationNotSupported  = "i18n.current_operation_not_supported.3aec"
	keySelectOperationType    = "i18n.select_operation_type.63c6"
	keyOperationSucceeded     = "i18n.operation_succeeded.3313"
	keyOperationFailedDetails = "i18n.operation_failed_with_details.7280"
	keyGetProjectInfoFailure  = "i18n.get_project_info_failure.ddff"
	keyNoFileFound            = "i18n.no_file_found.7d40"
	keyDeleteSuccess          = "i18n.delete_success.0007"
	keyDownloadFailedRetry    = "i18n.download_failed_retry.c113"
	keyResetLogFailure        = "i18n.reset_log_failure.b3d3"
	keyServiceException       = "i18n.service_exception.3821"
	keyCorrespondingFileNeed  = "i18n.corresponding_file_required.57b3"
	keyProjectNotExist        = "i18n.project_does_not_exist.3029"
)

// registerProject 注册 /manage/* 项目生命周期、状态与日志端点。
func registerProject(r *httpx.Router, a *app.App) {
	runtime := project.NewRuntime(a.BaseDir, a.Config, a.Log())
	// 项目后台任务：autoStart 项目启动
	runtime.StartBackgroundTasks()
	// 控制台日志自动备份注册到 sysinfo 统一调度器（/system/cache.taskList 与 Java 一致）
	if cron := runtime.Options.ProjectLog.AutoBackupConsoleCron; strings.TrimSpace(cron) != "" {
		if err := sysinfo.RegisterCronTask("auto_back_log", cron, runtime.AutoBackupConsoleLogs); err != nil {
			a.Log().Warn("failed to register auto_back_log cron", "error", err)
		}
	}

	r.Handle(http.MethodPost, "/manage/getProjectItem", func(c *httpx.Context) { projectGetItem(c, runtime) })
	r.Handle(http.MethodPost, "/manage/getProjectInfo", func(c *httpx.Context) { projectGetInfo(c, runtime) })
	r.Handle(http.MethodPost, "/manage/saveProject", func(c *httpx.Context) { projectSave(c, runtime) })
	r.Handle(http.MethodPost, "/manage/deleteProject", func(c *httpx.Context) { projectDelete(c, runtime) })
	r.Handle(http.MethodPost, "/manage/getProjectStatus", func(c *httpx.Context) { projectStatus(c, runtime) })
	r.Handle(http.MethodPost, "/manage/getProjectPort", func(c *httpx.Context) { projectPort(c, runtime) })
	r.Handle(http.MethodPost, "/manage/operate", func(c *httpx.Context) { projectOperate(c, runtime) })
	r.Handle(http.MethodPost, "/manage/releaseOutGiving", func(c *httpx.Context) { projectReleaseOutGiving(c, runtime) })
	r.Handle(http.MethodPost, "/manage/change-workspace-id", func(c *httpx.Context) { projectChangeWorkspaceID(c, runtime) })

	r.Handle(http.MethodPost, "/manage/log/logSize", func(c *httpx.Context) { projectLogSize(c, runtime) })
	r.Handle(http.MethodPost, "/manage/log/resetLog", func(c *httpx.Context) { projectResetLog(c, runtime) })
	r.Handle(http.MethodPost, "/manage/log/logBack", func(c *httpx.Context) { projectLogBack(c, runtime) })
	r.Handle(http.MethodPost, "/manage/log/logBack_delete", func(c *httpx.Context) { projectLogBackDelete(c, runtime) })
	r.Handle(http.MethodGet, "/manage/log/logBack_download", func(c *httpx.Context) { projectLogBackDownload(c, runtime) })
	r.Handle(http.MethodGet, "/manage/log/export", func(c *httpx.Context) { projectLogExport(c, runtime) })
}

// projectForm 统一读取 form-urlencoded / JSON 请求参数。
type projectForm struct {
	values  map[string]any
	present map[string]bool
}

func readProjectForm(c *httpx.Context) *projectForm {
	form := &projectForm{values: map[string]any{}, present: map[string]bool{}}
	contentType := strings.ToLower(c.R.Header.Get("Content-Type"))
	if strings.Contains(contentType, "application/json") {
		var body map[string]any
		if err := c.BindJSON(&body); err == nil && body != nil {
			for key, value := range body {
				form.values[key] = value
				form.present[key] = true
			}
		}
		return form
	}
	_ = c.R.ParseForm()
	for key, values := range c.R.Form {
		if len(values) == 0 {
			continue
		}
		form.values[key] = values[len(values)-1]
		form.present[key] = true
	}
	return form
}

func (f *projectForm) string(key string) string {
	value, ok := f.values[key]
	if !ok || value == nil {
		return ""
	}
	switch data := value.(type) {
	case string:
		return data
	case bool:
		return strconv.FormatBool(data)
	case json.Number:
		return data.String()
	case float64:
		return strconv.FormatFloat(data, 'f', -1, 64)
	default:
		return fmt.Sprint(data)
	}
}

// boolValue 按 Java Boolean.parseBoolean 语义解析（忽略大小写，只有 true 为真）。
func (f *projectForm) boolValue(key string) bool {
	return strings.EqualFold(strings.TrimSpace(f.string(key)), "true")
}

// convertBoolValue 按 hutool Convert.toBool 语义解析（true/1/yes/y/on 为真）。
func (f *projectForm) convertBoolValue(key string) bool {
	switch strings.ToLower(strings.TrimSpace(f.string(key))) {
	case "true", "1", "yes", "y", "on":
		return true
	default:
		return false
	}
}

// projectWorkspace 返回 workspaceId 请求头原值（Java 端不做 trim/默认值）。
func projectWorkspace(c *httpx.Context) string {
	return c.R.Header.Get("workspaceId")
}

// projectUser 解析服务端操作人：URL 解码，空值返回 "-"（等价 BaseAgentController.getUserName）。
func projectUser(c *httpx.Context) string {
	raw := c.R.Header.Get("Jpom-Server-UserName")
	if raw == "" {
		return "-"
	}
	if decoded, err := url.PathUnescape(raw); err == nil {
		return decoded
	}
	return raw
}

// respondOK 输出成功响应；data 为 nil 时不输出 data 字段（Jackson NON_NULL 语义）。
func respondOK(c *httpx.Context, msg string, data any) {
	respondCode(c, 200, msg, data)
}

// respondCode 输出 JsonMessage；data 为 nil 时省略 data 字段。
func respondCode(c *httpx.Context, code int, msg string, data any) {
	c.W.Header().Set("Content-Type", "application/json;charset=UTF-8")
	c.W.WriteHeader(http.StatusOK)
	payload := map[string]any{"code": code, "msg": msg}
	if data != nil {
		payload["data"] = data
	}
	encoder := json.NewEncoder(c.W)
	encoder.SetEscapeHTML(false)
	_ = encoder.Encode(payload)
}

// respondProjectError 按 Java BaseExceptionHandler 语义输出项目域错误：
// 参数/状态异常 → 405 原文；业务显式 500 → 原文；运行期异常 → 500“服务异常：”+信息。
func respondProjectError(c *httpx.Context, err error) {
	if err == nil {
		return
	}
	kind, msg := project.LocalizeError(c.Lang, err)
	switch kind {
	case project.ErrValidation:
		respondCode(c, 405, msg, nil)
	case project.ErrBusiness:
		respondCode(c, 500, msg, nil)
	default:
		respondCode(c, 500, c.T(keyServiceException)+msg, nil)
	}
}

// failValidation 输出参数/状态异常（405）。
func failValidation(c *httpx.Context, err error) {
	respondProjectError(c, err)
}

// failParam 输出 Spring @ValidatorItem 参数校验失败（code=400，Java 实测语义）。
func failParam(c *httpx.Context, msg string) {
	respondCode(c, 400, msg, nil)
}

// lookupProject 读取项目，不存在时返回 nil。
func lookupProject(rt *project.Runtime, id string) map[string]any {
	info, ok, err := rt.Manager.Get(id)
	if err != nil || !ok {
		return nil
	}
	return info
}

// projectGetItem 获取单个项目信息（等价 ProjectListController.getProjectItem）。
func projectGetItem(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	info := lookupProject(rt, form.string("id"))
	if info == nil {
		respondOK(c, "", nil)
		return
	}
	result := project.Clone(info)
	runMode := project.GetString(info, "runMode")
	if runMode != project.RunModeDsl && runMode != project.RunModeFile {
		command, err := rt.RunCommandE(info)
		if err != nil {
			respondProjectError(c, err)
			return
		}
		if command != "" {
			result["runCommand"] = command
		}
	}
	if runMode == project.RunModeDsl {
		canReload, list, err := rt.DslProcessInfo(info)
		if err != nil {
			respondProjectError(c, err)
			return
		}
		result["canReload"] = canReload
		result["dslProcessInfo"] = list
	}
	respondOK(c, "", result)
}

// projectGetInfo 获取全部项目信息（等价 ProjectListController.getProjectInfo）。
func projectGetInfo(c *httpx.Context, runtime *project.Runtime) {
	list, err := runtime.Manager.List()
	if err != nil {
		respondProjectError(c, project.ServiceError(err))
		return
	}
	respondOK(c, "", list)
}

// projectSave 保存项目（等价 ManageEditProjectController.saveProject）。
func projectSave(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	msg, err := rt.SaveProject(project.SaveParams{
		Values:      form.values,
		Preview:     form.convertBoolValue("previewData"),
		OutGiving:   form.boolValue("outGivingProject"),
		WorkspaceID: projectWorkspace(c),
		UserName:    projectUser(c),
	})
	if err != nil {
		respondProjectError(c, err)
		return
	}
	respondOK(c, msg, nil)
}

// projectDelete 删除项目（等价 ManageEditProjectController.deleteProject）。
func projectDelete(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	// Java deleteProject 未使用 @ValidatorItem：缺 id 时按“项目不存在”返回成功
	msg, err := rt.DeleteProject(form.string("id"), form.string("thorough"))
	if err != nil {
		respondProjectError(c, err)
		return
	}
	respondOK(c, msg, nil)
}

// projectStatus 获取项目进程状态（等价 ProjectStatusController.getProjectStatus）。
func projectStatus(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	id := form.string("id")
	if strings.TrimSpace(id) == "" {
		// @ValidatorItem 校验失败 -> code=400（自定义文案优先）
		failParam(c, c.T(keyIncorrectProjectID))
		return
	}
	info := lookupProject(rt, id)
	if info == nil {
		failValidation(c, project.ValidationKey(keyProjectIDNotExist))
		return
	}
	// Java 端 getProjectStatus 捕获内部异常并返回空对象
	result, err := rt.StatusE(info)
	if err != nil {
		c.App.Log().Warn("get project status failed", "id", id, "error", err)
		respondOK(c, "", map[string]any{})
		return
	}
	data := map[string]any{}
	if result.Pid != nil {
		data["pId"] = *result.Pid
	}
	if len(result.Pids) > 0 {
		data["pIds"] = result.Pids
	}
	if result.StatusMsg != nil {
		data["statusMsg"] = *result.StatusMsg
	}
	respondOK(c, "", data)
}

// projectPort 获取项目运行端口（等价 ProjectStatusController.getProjectPort）。
func projectPort(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	idsText := form.string("ids")
	if strings.TrimSpace(idsText) == "" {
		failValidation(c, project.ValidationKey(keyInfoToRetrieveNotFound))
		return
	}
	var ids []string
	if err := json.Unmarshal([]byte(idsText), &ids); err != nil {
		respondProjectError(c, project.Servicef("invalid ids json: %v", err))
		return
	}
	result := map[string]any{}
	for _, id := range ids {
		item := map[string]any{}
		info := lookupProject(rt, id)
		if info == nil {
			item["error"] = c.T(keyProjectNotExist) + ":" + id
			result[id] = item
			continue
		}
		item["name"] = project.GetString(info, "name")
		status, err := rt.StatusE(info)
		if err != nil {
			_, msg := project.LocalizeError(c.Lang, err)
			item["error"] = msg
			result[id] = item
			continue
		}
		pid := 0
		if status.Pid != nil {
			item["pid"] = *status.Pid
			pid = *status.Pid
		}
		if len(status.Pids) > 0 {
			item["pids"] = status.Pids
		}
		if status.StatusMsg != nil {
			item["statusMsg"] = *status.StatusMsg
		}
		if status.Ports != nil && *status.Ports != "" {
			item["port"] = *status.Ports
		} else {
			item["port"] = proc.MainPort(pid)
		}
		result[id] = item
	}
	respondOK(c, "", result)
}

// projectOperate 执行项目命令（等价 ProjectStatusController.operate）。
func projectOperate(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	id := form.string("id")
	if strings.TrimSpace(id) == "" {
		// @ValidatorItem 校验失败 -> code=400（自定义文案优先）
		failParam(c, c.T(keyIncorrectProjectID))
		return
	}
	op := strings.TrimSpace(form.string("opt"))
	if op == "" {
		failParam(c, c.T(keyMissingOperationParam))
		return
	}
	info := lookupProject(rt, id)
	if info == nil {
		failValidation(c, project.ValidationKey(keyNoProjectFound))
		return
	}
	// Java：EnumUtil.fromStringQuietly 解析失败 -> Assert「请选择操作类型」；
	// 枚举存在但 canOpt=false -> Assert「不支持当前操作：<opt>」
	if !project.ValidOp(op) {
		failValidation(c, project.ValidationKey(keySelectOperationType))
		return
	}
	if !project.CanOpt(op) {
		failValidation(c, project.ValidationKeySuffix(keyOperationNotSupported, op))
		return
	}
	result, err := rt.Operate(op, info)
	if err != nil {
		respondProjectError(c, err)
		return
	}
	code := 200
	msg := c.T(keyOperationSucceeded)
	if !result.Success {
		code = 201
		msg = c.T(keyOperationFailedDetails) + result.MsgStr()
	}
	respondCode(c, code, msg, result.JSON())
}

// projectReleaseOutGiving 释放节点分发（等价 /manage/releaseOutGiving）。
func projectReleaseOutGiving(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	msg, err := rt.ReleaseOutGiving(form.string("id"), projectUser(c))
	if err != nil {
		respondProjectError(c, err)
		return
	}
	respondOK(c, msg, nil)
}

// projectChangeWorkspaceID 修改项目工作空间（等价 /manage/change-workspace-id）。
func projectChangeWorkspaceID(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	msg, err := rt.ChangeWorkspaceID(form.string("id"), form.string("newWorkspaceId"), form.string("newNodeId"), projectUser(c))
	if err != nil {
		respondProjectError(c, err)
		return
	}
	respondOK(c, msg, nil)
}

// requireProject 读取项目，不存在时写 405 错误响应。
func requireProject(c *httpx.Context, rt *project.Runtime, id string) map[string]any {
	info := lookupProject(rt, id)
	if info == nil {
		respondProjectError(c, project.ValidationKeySuffix(keyGetProjectInfoFailure, id))
		return nil
	}
	return info
}

// projectLogSize 查看控制台日志大小（等价 ProjectLogBackController.logSize）。
func projectLogSize(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	info := requireProject(c, rt, form.string("id"))
	if info == nil {
		return
	}
	logBack, size, err := rt.LogSize(info)
	if err != nil {
		respondProjectError(c, err)
		return
	}
	data := map[string]any{"logBack": logBack}
	if size != "" {
		data["logSize"] = size
	}
	respondOK(c, "", data)
}

// projectResetLog 重置控制台日志（等价 ProjectLogBackController.resetLog）。
func projectResetLog(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	info := requireProject(c, rt, form.string("id"))
	if info == nil {
		return
	}
	msg, code, err := rt.ResetLog(info)
	if err != nil {
		// Java resetLog 捕获所有异常并返回 code=500 重置日志失败
		c.App.Log().Warn("reset project log failed", "id", form.string("id"), "error", err)
		respondCode(c, 500, c.T(keyResetLogFailure), nil)
		return
	}
	respondCode(c, code, msg, nil)
}

// projectLogBack 查看控制台日志备份列表（等价 ProjectLogBackController.console）。
func projectLogBack(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	info := requireProject(c, rt, form.string("id"))
	if info == nil {
		return
	}
	data, err := rt.LogBackList(info)
	if err != nil {
		respondProjectError(c, err)
		return
	}
	respondOK(c, "", data)
}

// projectLogBackDelete 删除日志备份（等价 ProjectLogBackController.clear）。
func projectLogBackDelete(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	info := requireProject(c, rt, form.string("id"))
	if info == nil {
		return
	}
	name := form.string("name")
	if strings.TrimSpace(name) == "" {
		failValidation(c, project.ValidationKey(keyNoFileFound))
		return
	}
	if err := rt.LogBackDelete(info, name); err != nil {
		respondProjectError(c, err)
		return
	}
	respondOK(c, c.T(keyDeleteSuccess), nil)
}

// projectLogBackDownload 下载日志备份（等价 ProjectLogBackController.download）。
func projectLogBackDownload(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	info := requireProject(c, rt, form.string("id"))
	if info == nil {
		return
	}
	key := form.string("key")
	if strings.TrimSpace(key) == "" {
		failValidation(c, project.ValidationKey(keyCorrespondingFileNeed))
		return
	}
	file, err := rt.LogBackDownloadFile(info, key)
	if err != nil {
		kind, msg := project.LocalizeError(c.Lang, err)
		if kind == project.ErrBusiness {
			writeRawJSON(c, 400, msg)
		} else {
			writeRawJSON(c, 400, c.T(keyDownloadFailedRetry)+msg)
		}
		return
	}
	serveFile(c, file)
}

// projectLogExport 导出控制台日志（等价 ProjectLogBackController.export）。
func projectLogExport(c *httpx.Context, runtime *project.Runtime) {
	rt := runtime.WithLang(c.Lang)
	form := readProjectForm(c)
	info := requireProject(c, rt, form.string("id"))
	if info == nil {
		return
	}
	file, err := rt.ExportLogFile(info)
	if err != nil {
		kind, msg := project.LocalizeError(c.Lang, err)
		if kind == project.ErrBusiness {
			writeRawJSON(c, 400, msg)
			return
		}
		respondProjectError(c, err)
		return
	}
	serveFile(c, file)
}

// writeRawJSON 输出裸 JSON（下载失败时的 Java ServletUtil.write 行为）。
func writeRawJSON(c *httpx.Context, code int, msg string) {
	c.W.Header().Set("Content-Type", "application/json;charset=UTF-8")
	c.W.WriteHeader(http.StatusOK)
	encoder := json.NewEncoder(c.W)
	encoder.SetEscapeHTML(false)
	_ = encoder.Encode(map[string]any{"code": code, "msg": msg})
}

// serveFile 以附件形式输出文件。
func serveFile(c *httpx.Context, path string) {
	file, err := os.Open(path)
	if err != nil {
		writeRawJSON(c, 400, c.T(keyDownloadFailedRetry)+err.Error())
		return
	}
	defer func() { _ = file.Close() }()
	stat, err := file.Stat()
	if err != nil || stat.IsDir() {
		writeRawJSON(c, 400, c.T(keyDownloadFailedRetry))
		return
	}
	c.W.Header().Set("Content-Type", "application/octet-stream")
	c.W.Header().Set("Content-Disposition", httpx.ContentDisposition(stat.Name()))
	http.ServeContent(c.W, c.R, stat.Name(), stat.ModTime(), file)
}
