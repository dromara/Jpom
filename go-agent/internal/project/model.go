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
	"encoding/json"
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"time"

	"jpom.io/go-agent/internal/config"
)

// RunMode 取值与 Java org.dromara.jpom.model.RunMode 枚举名一致（数据文件里存字符串）。
const (
	RunModeClassPath     = "ClassPath"
	RunModeJar           = "Jar"
	RunModeJarWar        = "JarWar"
	RunModeJavaExtDirsCp = "JavaExtDirsCp"
	RunModeFile          = "File"
	RunModeDsl           = "Dsl"
	RunModeLink          = "Link"
)

// ConsoleCommandOp 取值与 Java org.dromara.jpom.socket.ConsoleCommandOp 一致。
const (
	OpStart   = "start"
	OpStop    = "stop"
	OpRestart = "restart"
	OpStatus  = "status"
	OpReload  = "reload"
	OpShowlog = "showlog"
	OpHeart   = "heart"
)

// CanOpt 是否支持手动操作（Java ConsoleCommandOp.isCanOpt）。
func CanOpt(op string) bool {
	switch op {
	case OpStart, OpStop, OpRestart, OpReload:
		return true
	default:
		return false
	}
}

// ParseRunMode 校验运行模式（空或未知返回空字符串）。
func ParseRunMode(value string) string {
	switch value {
	case RunModeClassPath, RunModeJar, RunModeJarWar, RunModeJavaExtDirsCp, RunModeFile, RunModeDsl, RunModeLink:
		return value
	default:
		return ""
	}
}

// Now 返回 Java DateUtil.now() 同款时间字符串。
func Now() string {
	return time.Now().Format("2006-01-02 15:04:05")
}

// GetString 读取字符串字段（数字/布尔也转成字符串，保持与 Java 转换一致）。
func GetString(info map[string]any, key string) string {
	if info == nil {
		return ""
	}
	switch value := info[key].(type) {
	case string:
		return value
	case json.Number:
		return value.String()
	case float64:
		return strconv.FormatFloat(value, 'f', -1, 64)
	case bool:
		return strconv.FormatBool(value)
	case int:
		return strconv.Itoa(value)
	case int64:
		return strconv.FormatInt(value, 10)
	default:
		return ""
	}
}

// GetBool 读取布尔字段。
func GetBool(info map[string]any, key string) bool {
	if info == nil {
		return false
	}
	switch value := info[key].(type) {
	case bool:
		return value
	case string:
		return strings.EqualFold(value, "true")
	case float64:
		return value != 0
	default:
		return false
	}
}

// HasKey 判断字段是否存在且不为 nil。
func HasKey(info map[string]any, key string) bool {
	if info == nil {
		return false
	}
	value, ok := info[key]
	return ok && value != nil
}

// IsEmpty 空字符串判断。
func IsEmpty(value string) bool { return strings.TrimSpace(value) == "" }

// EmptyToDefault 空字符串返回默认值（等价 Java StrUtil.emptyToDefault）。
func EmptyToDefault(value, fallback string) string {
	if value == "" {
		return fallback
	}
	return value
}

// Clone 浅拷贝项目数据。
func Clone(info map[string]any) map[string]any {
	out := make(map[string]any, len(info))
	for key, value := range info {
		out[key] = value
	}
	return out
}

// ResolveModel 解析项目原始数据：软链项目返回被软链的项目（等价 ProjectInfoService.resolveModel）。
func (m *Manager) ResolveModel(info map[string]any) (map[string]any, error) {
	if info == nil {
		return nil, fmt.Errorf("项目信息不能为空")
	}
	if GetString(info, "runMode") != RunModeLink {
		return info, nil
	}
	linkID := GetString(info, "linkId")
	target, ok, err := m.Get(linkID)
	if err != nil {
		return nil, err
	}
	if !ok || target == nil {
		return nil, fmt.Errorf("软链项目已经不存在啦%s", linkID)
	}
	return target, nil
}

// AllLib 返回项目目录（<whitelistDirectory>/<lib>），等价 Java NodeProjectInfoModel.allLib。
func AllLib(info map[string]any) (string, error) {
	return ProjectPath(info)
}

// ResolveLibPath 解析项目目录（软链项目返回被软链项目目录，等价 ProjectInfoService.resolveLibPath）。
func (m *Manager) ResolveLibPath(info map[string]any) (string, error) {
	original, err := m.ResolveModel(info)
	if err != nil {
		return "", err
	}
	return AllLib(original)
}

// ResolveLibFile 同 ResolveLibPath。
func (m *Manager) ResolveLibFile(info map[string]any) (string, error) {
	return m.ResolveLibPath(info)
}

// ResolveLogDir 解析项目控制台日志目录（等价 ProjectInfoService.resolveLogPath）。
//
// 日志目录取「被软链项目」的 logPath，目录名固定是当前项目 id。
func (m *Manager) ResolveLogDir(info map[string]any) (string, error) {
	id := GetString(info, "id")
	if id == "" {
		return "", fmt.Errorf("没有找到项目id")
	}
	original, err := m.ResolveModel(info)
	if err != nil {
		return "", err
	}
	if custom := GetString(original, "logPath"); custom != "" {
		return filepath.Join(custom, id), nil
	}
	return filepath.Join(m.baseDir, "project-log", id), nil
}

// ResolveLogFile 解析项目控制台日志文件（<logDir>/<id>.log）。
func (m *Manager) ResolveLogFile(info map[string]any) (string, error) {
	dir, err := m.ResolveLogDir(info)
	if err != nil {
		return "", err
	}
	return filepath.Join(dir, GetString(info, "id")+".log"), nil
}

// ResolveLogBack 解析日志备份目录（<logDir>/back）。
func (m *Manager) ResolveLogBack(info map[string]any) (string, error) {
	dir, err := m.ResolveLogDir(info)
	if err != nil {
		return "", err
	}
	return filepath.Join(dir, "back"), nil
}

// TouchLogFile 创建日志文件（含父目录），等价 Java resolveAbsoluteLogFile 的 FileUtil.touch。
func (m *Manager) TouchLogFile(info map[string]any) (string, error) {
	file, err := m.ResolveLogFile(info)
	if err != nil {
		return "", err
	}
	if err := os.MkdirAll(filepath.Dir(file), 0o755); err != nil {
		return "", err
	}
	if _, err := os.Stat(file); os.IsNotExist(err) {
		f, err := os.OpenFile(file, os.O_CREATE|os.O_WRONLY, 0o644)
		if err != nil {
			return "", err
		}
		_ = f.Close()
	}
	return file, nil
}

// UpdateByID 按 id 修改项目（等价 Java BaseOperService.updateById：只覆盖非 nil 字段）。
func (m *Manager) UpdateByID(id, userName string, patch func(info map[string]any)) error {
	return m.Mutate(func(values map[string]map[string]any) error {
		item, ok := values[id]
		if !ok || item == nil {
			return fmt.Errorf("数据不存在%s", id)
		}
		patch(item)
		item["modifyTime"] = Now()
		item["modifyUser"] = userName
		return nil
	})
}

// AddItem 新增项目（等价 BaseWorkspaceOptService.addItem）。
func (m *Manager) AddItem(info map[string]any, userName string) error {
	id := GetString(info, "id")
	if id == "" {
		return fmt.Errorf("项目id不能为空")
	}
	return m.Mutate(func(values map[string]map[string]any) error {
		if _, ok := values[id]; ok {
			return fmt.Errorf("数据id已经存在：%s", id)
		}
		info["createTime"] = Now()
		if userName != "-" && userName != "" {
			info["createUser"] = userName
			info["modifyUser"] = userName
		}
		values[id] = info
		return nil
	})
}

// DeleteItem 删除项目数据。
func (m *Manager) DeleteItem(id string) error {
	return m.Mutate(func(values map[string]map[string]any) error {
		delete(values, id)
		return nil
	})
}

// List 返回所有项目（按创建时间、id 排序，近似 Java JSONObject 的文件顺序）。
func (m *Manager) List() ([]map[string]any, error) {
	all, err := m.All()
	if err != nil {
		return nil, err
	}
	ids := make([]string, 0, len(all))
	for id := range all {
		ids = append(ids, id)
	}
	sort.Slice(ids, func(i, j int) bool {
		left, right := all[ids[i]], all[ids[j]]
		leftTime := GetString(left, "createTime")
		rightTime := GetString(right, "createTime")
		if leftTime == rightTime {
			return ids[i] < ids[j]
		}
		if leftTime == "" {
			return false
		}
		if rightTime == "" {
			return true
		}
		return leftTime < rightTime
	})
	result := make([]map[string]any, 0, len(ids))
	for _, id := range ids {
		result = append(result, all[id])
	}
	return result, nil
}

// ValidateProjectID 校验项目 id（等价 Java Validator.validateGeneral + SYSTEM_ID 检查）。
func ValidateProjectID(id string) error {
	checkID := strings.ReplaceAll(id, "-", "_")
	if len(checkID) < 2 || len(checkID) > 50 {
		return fmt.Errorf("项目id 长度范围2-50（英文字母 、数字和下划线）")
	}
	if !projectIDPattern.MatchString(checkID) {
		return fmt.Errorf("项目id 长度范围2-50（英文字母 、数字和下划线）")
	}
	if id == "system" {
		return fmt.Errorf("项目id system 关键词被系统占用")
	}
	return nil
}

var projectIDPattern = regexp.MustCompile("^[A-Za-z0-9_]+$")

// tokenValue 解析 webhook token：历史数据中的 no 视为空（等价 NodeProjectInfoModel.token）。
func tokenValue(info map[string]any) string {
	value := GetString(info, "token")
	if strings.EqualFold(value, "no") {
		return ""
	}
	return value
}

// ParseDataSize 解析 50MB/1KB 这类配置（Spring DataSize 语义，1KB=1024B）。
func ParseDataSize(value string) int64 {
	text := strings.TrimSpace(strings.ToUpper(value))
	if text == "" {
		return 0
	}
	units := []struct {
		suffix string
		scale  int64
	}{
		{"TB", 1 << 40}, {"GB", 1 << 30}, {"MB", 1 << 20}, {"KB", 1 << 10}, {"B", 1},
	}
	for _, unit := range units {
		if strings.HasSuffix(text, unit.suffix) {
			number := strings.TrimSpace(strings.TrimSuffix(text, unit.suffix))
			size, err := strconv.ParseFloat(number, 64)
			if err != nil {
				return 0
			}
			return int64(size * float64(unit.scale))
		}
	}
	number, err := strconv.ParseInt(text, 10, 64)
	if err != nil {
		return 0
	}
	return number
}

// loadWorkspaceEnv 读取 data/workspace_env_var.json 中指定工作空间的环境变量。
// 与 Java AgentWorkspaceEnvVarService#getEnv 一致：取 varData[*].value，值不能为空。
func loadWorkspaceEnv(baseDir, workspaceID string) map[string]string {
	env := map[string]string{}
	content, err := os.ReadFile(filepath.Join(baseDir, "data", "workspace_env_var.json"))
	if err != nil || len(content) == 0 {
		return env
	}
	var all map[string]map[string]any
	if err := json.Unmarshal(content, &all); err != nil {
		return env
	}
	item, ok := all[workspaceID]
	if !ok || item == nil {
		return env
	}
	varData, ok := item["varData"].(map[string]any)
	if !ok || varData == nil {
		return env
	}
	for _, value := range varData {
		entry, ok := value.(map[string]any)
		if !ok || entry == nil {
			continue
		}
		name := GetString(entry, "name")
		if name == "" {
			continue
		}
		env[name] = GetString(entry, "value")
	}
	return env
}

// NewRuntime 创建项目域运行时。
func NewRuntime(baseDir string, cfg *config.Config, logger *slog.Logger) *Runtime {
	runtime := &Runtime{
		Manager: New(baseDir),
		Log:     logger,
	}
	runtime.Options.BaseDir = baseDir
	if cfg != nil {
		runtime.Options.StatusWaitTime = cfg.Jpom.Project.StatusWaitTime
		runtime.Options.StatusDetectionInterval = cfg.Jpom.Project.StatusDetectionInterval
		runtime.Options.FileBackupCount = cfg.Jpom.Project.FileBackupCount
		runtime.Options.FileBackupSuffix = cfg.Jpom.Project.FileBackupSuffix
		runtime.Options.CommandUseSudo = cfg.Jpom.System.CommandUseSudo
		runtime.Options.ProjectLog = ProjectLogOptions{
			AutoBackupConsoleCron: cfg.Jpom.Project.Log.AutoBackupConsoleCron,
			AutoBackupSize:        ParseDataSize(cfg.Jpom.Project.Log.AutoBackupSize),
			AutoBackupToFile:      cfg.Jpom.Project.Log.AutoBackupToFile,
			SaveDays:              cfg.Jpom.Project.Log.SaveDays,
			FileCharset:           cfg.Jpom.Project.Log.FileCharset,
		}
	}
	return runtime
}
