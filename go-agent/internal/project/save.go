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
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"sync"

	"jpom.io/go-agent/internal/proc"
)

var whitelistMu sync.Mutex

// urlHTTPPattern 校验 WebHooks 地址（等价 hutool RegexPool.URL_HTTP）。
var urlHTTPPattern = regexp.MustCompile("^https?://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]")

// SaveParams 保存项目请求参数。
type SaveParams struct {
	Values      map[string]any // 请求参数（form/JSON）
	Preview     bool           // 是否预检查
	OutGiving   bool           // 是否为节点分发添加
	WorkspaceID string         // 请求头 workspaceId
	UserName    string         // 请求头 Jpom-Server-UserName
}

// SaveProject 新建/修改项目（等价 ManageEditProjectController.saveProject）。
func (r *Runtime) SaveProject(params SaveParams) (string, error) {
	info := Clone(params.Values)
	delete(info, "previewData")
	dropNilValues(info)
	coerceProjectValues(info)
	if err := r.checkSaveParameter(info, params.Preview, params.OutGiving); err != nil {
		return "", err
	}
	// 自动创建日志文件并校验日志路径不是目录
	logFile, err := r.Manager.TouchLogFile(info)
	if err != nil {
		return "", err
	}
	if stat, err := os.Stat(logFile); err == nil && stat.IsDir() {
		return "", fmt.Errorf("项目log是一个已经存在的文件夹")
	}
	// WebHooks 地址校验
	if token := tokenValue(info); token != "" && !urlHTTPPattern.MatchString(token) {
		return "", fmt.Errorf("WebHooks 地址不合法")
	}
	// dsl 配置校验
	if err := checkDslYml(info); err != nil {
		return "", err
	}
	return r.save(info, params)
}

// checkDslYml 校验 DSL 配置（等价 ManageEditProjectController.checkDslYml）。
func checkDslYml(info map[string]any) error {
	if GetString(info, "runMode") != RunModeDsl {
		return nil
	}
	content := GetString(info, "dslContent")
	if strings.TrimSpace(content) == "" {
		return fmt.Errorf("请配置 dsl 内容")
	}
	config, err := parseDslContent(content)
	if err != nil {
		return err
	}
	if !dslHasProcess(config, OpStatus) {
		return fmt.Errorf("没有配置 run.status")
	}
	return nil
}

// checkSaveParameter 保存前参数校验（等价 ManageEditProjectController.checkParameter）。
func (r *Runtime) checkSaveParameter(info map[string]any, preview, outGiving bool) error {
	id := GetString(info, "id")
	if err := ValidateProjectID(id); err != nil {
		return err
	}
	runMode := ParseRunMode(GetString(info, "runMode"))
	if runMode == "" {
		return fmt.Errorf("请选择运行模式")
	}
	info["runMode"] = runMode
	switch runMode {
	case RunModeClassPath, RunModeJavaExtDirsCp:
		if GetString(info, "mainClass") == "" {
			return fmt.Errorf("ClassPath、JavaExtDirsCp 模式 MainClass必填")
		}
		if runMode == RunModeJavaExtDirsCp && GetString(info, "javaExtDirsCp") == "" {
			return fmt.Errorf("JavaExtDirsCp 模式 javaExtDirsCp必填")
		}
	case RunModeJar, RunModeJarWar:
		info["mainClass"] = ""
	case RunModeLink:
		linkID := GetString(info, "linkId")
		if linkID == "" {
			return fmt.Errorf("Link 模式 LinkId必填")
		}
		target, ok, err := r.Manager.Get(linkID)
		if err != nil {
			return err
		}
		if !ok || target == nil {
			return fmt.Errorf("被软链的项目已经不存在啦，%s", linkID)
		}
		targetMode := GetString(target, "runMode")
		if targetMode == RunModeFile || targetMode == RunModeLink {
			return fmt.Errorf("软链项目模式错误")
		}
	}
	info["outGivingProject"] = outGiving
	if runMode == RunModeLink {
		return nil
	}
	if !preview {
		whitelistDirectory := GetString(info, "whitelistDirectory")
		if whitelistDirectory == "" {
			return fmt.Errorf("恢复授权数据异常或者没有选择授权目录")
		}
		if !r.CheckProjectDirectory(whitelistDirectory) {
			if !outGiving {
				return fmt.Errorf("请选择正确的项目路径,或者还没有配置授权")
			}
			if err := r.AddProjectWhiteList(whitelistDirectory); err != nil {
				return err
			}
		}
		if logPath := GetString(info, "logPath"); logPath != "" {
			if !r.CheckProjectDirectory(logPath) {
				if !outGiving {
					return fmt.Errorf("请填写的项目日志存储路径,或者还没有配置授权")
				}
				if err := r.AddProjectWhiteList(logPath); err != nil {
					return err
				}
			}
		}
	}
	lib := GetString(info, "lib")
	if lib == "" || lib == "/" || containsChinese(lib) {
		return fmt.Errorf("项目路径不能为空,不能是顶级目录,不能包含中文")
	}
	if err := checkSlip(lib); err != nil {
		return err
	}
	return nil
}

// containsChinese 判断是否包含中文字符。
func containsChinese(value string) bool {
	for _, r := range value {
		if r >= 0x4E00 && r <= 0x9FFF {
			return true
		}
	}
	return false
}

// checkSlip 判断目录是否有越级问题（等价 Java FileUtils.checkSlip）。
func checkSlip(lib string) error {
	tempDir := os.TempDir()
	target := filepath.Join(tempDir, lib)
	if !isSubPath(tempDir, target) {
		return fmt.Errorf("项目路径存在提升目录问题")
	}
	return nil
}

// save 保存项目（等价 ManageEditProjectController.save）。
func (r *Runtime) save(info map[string]any, params SaveParams) (string, error) {
	if err := r.checkProjectPath(info); err != nil {
		return "", err
	}
	id := GetString(info, "id")
	existing, ok, err := r.Manager.Get(id)
	if err != nil {
		return "", err
	}
	workspaceID := params.WorkspaceID
	info["workspaceId"] = workspaceID
	runMode := GetString(info, "runMode")
	if !ok || existing == nil {
		if runMode != RunModeFile && runMode != RunModeDsl {
			if r.IsRun(info) {
				return "", fmt.Errorf("当前项目id已经被正在运行的程序占用")
			}
		}
		if params.Preview {
			return "检查通过", nil
		}
		if err := r.Manager.AddItem(info, params.UserName); err != nil {
			return "", err
		}
		return "新增成功！", nil
	}
	if params.Preview {
		return "检查通过", nil
	}
	if ws := GetString(existing, "workspaceId"); ws != "" && ws != workspaceID {
		return "", fmt.Errorf("当前项目已经被其他工作空间关联,请检查确认后再操作或者使用孤独数据修正")
	}
	if runMode != RunModeLink {
		if err := r.moveTo(existing, info); err != nil {
			return "", err
		}
	}
	err = r.Manager.UpdateByID(id, params.UserName, func(item map[string]any) {
		// 只覆盖请求中出现的字段（Spring/Java 对未传字段保持 null 不覆盖）
		copyPresent(item, info, "name", "group", "nodeId", "autoStart", "disableScanDir",
			"jvm", "args", "dslContent", "dslEnv", "logCharset")
		item["mainClass"] = GetString(info, "mainClass")
		item["token"] = tokenValue(info)
		item["javaExtDirsCp"] = GetString(info, "javaExtDirsCp")
		item["outGivingProject"] = GetBool(info, "outGivingProject")
		item["runMode"] = runMode
		item["workspaceId"] = workspaceID
		if runMode != RunModeLink {
			item["lib"] = GetString(info, "lib")
			item["whitelistDirectory"] = GetString(info, "whitelistDirectory")
			item["logPath"] = GetString(info, "logPath")
		}
	})
	if err != nil {
		return "", err
	}
	return "修改成功", nil
}

// dropNilValues 去掉请求中值为 null 的字段（Java 端 null 不会写入数据文件）。
func dropNilValues(info map[string]any) {
	for key, value := range info {
		if value == nil {
			delete(info, key)
		}
	}
}

// coerceProjectValues 按 Java 模型字段类型转换请求参数（Spring MVC 数据绑定语义）。
//
// form 提交的值都是字符串，Spring 绑定到 Boolean 字段后会变成布尔值；这里显式转换，
// 保证回写的 project.json 与 Java 端类型一致。空字符串视为未传（Spring 绑定为 null）。
func coerceProjectValues(info map[string]any) {
	for _, key := range []string{"autoStart", "disableScanDir"} {
		raw, ok := info[key]
		if !ok {
			continue
		}
		text := strings.TrimSpace(GetString(info, key))
		if raw == nil || text == "" {
			delete(info, key)
			continue
		}
		info[key] = convertToBool(text)
	}
}

// convertToBool 按 Spring/hutool Boolean 绑定语义解析（true/1/yes/y/on 为真）。
func convertToBool(text string) bool {
	switch strings.ToLower(strings.TrimSpace(text)) {
	case "true", "1", "yes", "y", "on", "是", "真":
		return true
	default:
		return false
	}
}

// copyPresent 只拷贝源数据中出现且非 nil 的字段。
func copyPresent(target, source map[string]any, keys ...string) {
	for _, key := range keys {
		if HasKey(source, key) {
			target[key] = source[key]
		}
	}
}

// checkProjectPath 检查项目路径（等价 ManageEditProjectController.checkPath）。
func (r *Runtime) checkProjectPath(info map[string]any) error {
	id := GetString(info, "id")
	if strings.Contains(id, " ") {
		return fmt.Errorf("项目Id不能包含空格")
	}
	if GetString(info, "runMode") == RunModeLink {
		return nil
	}
	lib, err := AllLib(info)
	if err != nil {
		return err
	}
	if strings.Contains(lib, " ") {
		return fmt.Errorf("项目路径不能包含空格")
	}
	if stat, err := os.Stat(lib); err == nil && !stat.IsDir() {
		return fmt.Errorf("项目路径是一个已经存在的文件")
	}
	list, err := r.Manager.List()
	if err != nil {
		return err
	}
	for _, item := range list {
		if GetString(item, "runMode") == RunModeLink {
			continue
		}
		if GetString(item, "id") == id {
			continue
		}
		otherLib, err := r.Manager.ResolveLibFile(item)
		if err != nil {
			continue
		}
		if pathEquals(otherLib, lib) || isSubPath(otherLib, lib) || isSubPath(lib, otherLib) {
			return fmt.Errorf("项目路径和【%s】项目冲突:%s", GetString(item, "name"), otherLib)
		}
	}
	return nil
}

// moveTo 修改项目路径时移动目录与日志（等价 ManageEditProjectController.moveTo）。
func (r *Runtime) moveTo(old, news map[string]any) error {
	oldLib, err := r.Manager.ResolveLibFile(old)
	if err != nil {
		return err
	}
	newLib, err := AllLib(news)
	if err != nil {
		return err
	}
	if !pathEquals(oldLib, newLib) {
		if r.IsRun(old) {
			return fmt.Errorf("正在运行的项目不能修改路径")
		}
		if _, err := os.Stat(oldLib); err == nil {
			if err := tempMoveContent(oldLib, newLib); err != nil {
				return err
			}
		}
	}
	oldLog, err := r.Manager.TouchLogFile(old)
	if err != nil {
		return err
	}
	newLog, err := r.Manager.TouchLogFile(news)
	if err != nil {
		return err
	}
	if pathEquals(oldLog, newLog) {
		return nil
	}
	if r.IsRun(old) {
		return fmt.Errorf("正在运行的项目不能修改路径")
	}
	if _, err := os.Stat(oldLog); err == nil {
		if err := os.MkdirAll(filepath.Dir(newLog), 0o755); err != nil {
			return err
		}
		if err := os.Rename(oldLog, newLog); err != nil {
			if copyErr := copyFileContent(oldLog, newLog); copyErr != nil {
				return copyErr
			}
			_ = os.Remove(oldLog)
		}
	}
	oldLogBack, err := r.Manager.ResolveLogBack(old)
	if err != nil {
		return nil
	}
	if stat, err := os.Stat(oldLogBack); err == nil && stat.IsDir() {
		newLogBack, err := r.Manager.ResolveLogBack(news)
		if err != nil {
			return err
		}
		if err := tempMoveContent(oldLogBack, newLogBack); err != nil {
			return err
		}
	}
	return nil
}

// tempMoveContent 安全移动文件夹内容（等价 Java FileUtils.tempMoveContent）。
func tempMoveContent(src, target string) error {
	if isSubPath(target, src) {
		tempDir, err := os.MkdirTemp("", "jpom-mv-")
		if err != nil {
			return err
		}
		defer func() { _ = os.RemoveAll(tempDir) }()
		if err := moveContentTo(src, tempDir); err != nil {
			return err
		}
		if err := os.MkdirAll(target, 0o755); err != nil {
			return err
		}
		return moveContentTo(tempDir, target)
	}
	if err := os.MkdirAll(target, 0o755); err != nil {
		return err
	}
	if err := moveContentTo(src, target); err != nil {
		return err
	}
	return os.RemoveAll(src)
}

// moveContentTo 把 src 目录下的内容移动到 target 目录。
func moveContentTo(src, target string) error {
	entries, err := os.ReadDir(src)
	if err != nil {
		return err
	}
	for _, entry := range entries {
		from := filepath.Join(src, entry.Name())
		to := filepath.Join(target, entry.Name())
		if err := os.Rename(from, to); err != nil {
			if copyErr := copyPath(from, to); copyErr != nil {
				return copyErr
			}
			if delErr := os.RemoveAll(from); delErr != nil {
				return delErr
			}
		}
	}
	return nil
}

// copyPath 递归复制文件/目录（跨设备移动时的兜底）。
func copyPath(src, target string) error {
	stat, err := os.Stat(src)
	if err != nil {
		return err
	}
	if !stat.IsDir() {
		return copyFileContent(src, target)
	}
	entries, err := os.ReadDir(src)
	if err != nil {
		return err
	}
	if err := os.MkdirAll(target, 0o755); err != nil {
		return err
	}
	for _, entry := range entries {
		if err := copyPath(filepath.Join(src, entry.Name()), filepath.Join(target, entry.Name())); err != nil {
			return err
		}
	}
	return nil
}

// DeleteProject 删除项目（等价 ManageEditProjectController.deleteProject）。
func (r *Runtime) DeleteProject(id, thorough string) (string, error) {
	info, ok, err := r.Manager.Get(id)
	if err != nil {
		return "", err
	}
	if !ok || info == nil {
		return "项目不存在", nil
	}
	if r.IsRun(info) {
		return "", fmt.Errorf("不能删除正在运行的项目")
	}
	list, err := r.Manager.List()
	if err != nil {
		return "", err
	}
	for _, item := range list {
		if GetString(item, "runMode") != RunModeLink {
			continue
		}
		if GetString(item, "linkId") == id {
			return "", fmt.Errorf("项目被%s软链中", GetString(item, "name"))
		}
	}
	if strings.TrimSpace(thorough) != "" {
		if logBack, err := r.Manager.ResolveLogBack(info); err == nil {
			if proc.SystemFastDel(logBack) {
				return "", fmt.Errorf("删除日志文件失败:%s", logBack)
			}
		}
		if logFile, err := r.Manager.ResolveLogFile(info); err == nil {
			if proc.SystemFastDel(logFile) {
				return "", fmt.Errorf("删除日志文件失败:%s", logFile)
			}
		}
		if GetString(info, "runMode") != RunModeLink {
			if lib, err := r.Manager.ResolveLibFile(info); err == nil {
				if proc.SystemFastDel(lib) {
					return "", fmt.Errorf("删除项目文件失败:%s", lib)
				}
			}
		}
	}
	if err := r.Manager.DeleteItem(id); err != nil {
		return "", err
	}
	return "删除成功！", nil
}

// ReleaseOutGiving 释放节点分发（等价 ManageEditProjectController.releaseOutGiving）。
func (r *Runtime) ReleaseOutGiving(id, userName string) (string, error) {
	_, ok, err := r.Manager.Get(id)
	if err != nil {
		return "", err
	}
	if ok {
		if err := r.Manager.UpdateByID(id, userName, func(item map[string]any) {
			item["outGivingProject"] = false
		}); err != nil {
			return "", err
		}
	}
	return "释放成功", nil
}

// ChangeWorkspaceID 修改项目工作空间与节点（等价 /manage/change-workspace-id）。
func (r *Runtime) ChangeWorkspaceID(id, newWorkspaceID, newNodeID, userName string) (string, error) {
	if strings.TrimSpace(newWorkspaceID) == "" {
		return "", fmt.Errorf("请选择要修改的工作空间")
	}
	_, ok, err := r.Manager.Get(id)
	if err != nil {
		return "", err
	}
	if ok {
		if err := r.Manager.UpdateByID(id, userName, func(item map[string]any) {
			item["workspaceId"] = newWorkspaceID
			item["nodeId"] = newNodeID
		}); err != nil {
			return "", err
		}
	}
	return "修改成功", nil
}

// CheckProjectDirectory 判断路径是否命中项目授权目录（等价 AgentWhitelist.checkPath）。
func (r *Runtime) CheckProjectDirectory(path string) bool {
	if strings.TrimSpace(path) == "" {
		return false
	}
	whitelist, err := ReadWhitelist(r.Options.BaseDir)
	if err != nil {
		return false
	}
	for _, item := range whitelist.Project {
		if pathEquals(item, path) {
			return true
		}
	}
	return false
}

// AddProjectWhiteList 追加项目授权目录（等价 WhitelistDirectoryService.addProjectWhiteList）。
func (r *Runtime) AddProjectWhiteList(item string) error {
	validated, err := r.validateWhitelistPath(item)
	if err != nil {
		return err
	}
	whitelistMu.Lock()
	defer whitelistMu.Unlock()
	whitelist, err := ReadWhitelist(r.Options.BaseDir)
	if err != nil {
		return err
	}
	for _, exists := range whitelist.Project {
		if pathEquals(exists, validated) {
			return nil
		}
	}
	whitelist.Project = append(whitelist.Project, validated)
	return WriteWhitelist(r.Options.BaseDir, whitelist)
}

// validateWhitelistPath 校验授权目录（等价 AgentWhitelist.covertToArray）。
func (r *Runtime) validateWhitelistPath(value string) (string, error) {
	cleaned := filepath.Clean(strings.TrimSpace(value))
	if !filepath.IsAbs(cleaned) {
		return "", fmt.Errorf("需要配置绝对路径%s", cleaned)
	}
	if filepath.Dir(cleaned) == cleaned {
		return "", fmt.Errorf("不能配置根路径%s", cleaned)
	}
	agentDataPath := canonicalPath(filepath.Join(r.Options.BaseDir, "data"))
	if strings.HasPrefix(agentDataPath, cleaned) {
		return "", fmt.Errorf("项目路径授权不能包含 jpom 数据目录")
	}
	return cleaned, nil
}

// canonicalPath 返回规范化绝对路径（等价 FileUtil.pathEquals 的 canonicalPath 逻辑）。
func canonicalPath(path string) string {
	absolute, err := filepath.Abs(path)
	if err != nil {
		absolute = filepath.Clean(path)
	}
	if resolved, err := filepath.EvalSymlinks(absolute); err == nil {
		return resolved
	}
	return absolute
}

// pathEquals 判断两个路径是否相同（等价 Java FileUtil.pathEquals）。
func pathEquals(left, right string) bool {
	return canonicalPath(left) == canonicalPath(right)
}
