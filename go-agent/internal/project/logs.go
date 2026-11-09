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
	"io"
	"math"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"time"

	"jpom.io/go-agent/internal/proc"
)

// backLogMinSize 小于该大小的日志不做备份（等价 Java BACK_LOG_MIN_SIZE=100KB）。
const backLogMinSize = 100 * 1024

// LogSize 返回日志备份目录是否存在与当前日志文件可读大小（等价 /manage/log/logSize）。
func (r *Runtime) LogSize(info map[string]any) (bool, string, error) {
	logBack, err := r.Manager.ResolveLogBack(info)
	if err != nil {
		return false, "", err
	}
	logBackExists := false
	if stat, err := os.Stat(logBack); err == nil && stat.IsDir() {
		logBackExists = true
	}
	file, err := r.Manager.ResolveLogFile(info)
	if err != nil {
		return logBackExists, "", err
	}
	stat, err := os.Stat(file)
	if err != nil || stat.Size() <= 0 {
		return logBackExists, "", nil
	}
	return logBackExists, readableFileSize(stat.Size()), nil
}

// BackLog 清空项目控制台日志（兼容旧调用：异常返回错误文案）。
func (r *Runtime) BackLog(info map[string]any) string {
	original, err := r.Manager.ResolveModel(info)
	if err != nil {
		return err.Error()
	}
	msg, err := r.backLog(info, original)
	if err != nil {
		return err.Error()
	}
	return msg
}

// backLog 清空日志并按配置备份（等价 ProjectCommander.backLog）。
func (r *Runtime) backLog(info, original map[string]any) (string, error) {
	file, err := r.Manager.TouchLogFile(info)
	if err != nil {
		return "", ServiceError(err)
	}
	stat, err := os.Stat(file)
	if err != nil || stat.IsDir() {
		return "not exists", nil
	}
	if stat.Size() <= backLogMinSize {
		return "ok", nil
	}
	if r.resolveOpenLogBack(info, original) {
		backDir, err := r.Manager.ResolveLogBack(info)
		if err != nil {
			return "", err
		}
		backPath := filepath.Join(backDir, time.Now().Format("20060102150405")+".log")
		if err := copyFileContent(file, backPath); err != nil && r.Log != nil {
			r.Log.Warn("failed to backup project console log", "file", file, "error", err)
		}
	}
	if output := proc.EmptyLogFile(file); strings.TrimSpace(output) != "" && r.Log != nil {
		r.Log.Info("project console log truncated", "file", file, "result", output)
	}
	return "ok", nil
}

// ResetLog 重置日志：返回消息、业务码（200/201）与异常。
//
// 等价 ProjectLogBackController.resetLog：异常由调用方统一返回 code=500 重置日志失败。
func (r *Runtime) ResetLog(info map[string]any) (string, int, error) {
	original, err := r.Manager.ResolveModel(info)
	if err != nil {
		return "", 500, err
	}
	msg, err := r.backLog(info, original)
	if err != nil {
		return "", 500, err
	}
	if strings.Contains(msg, "ok") {
		return tr(r.Lang(), keyResetSuccess), 200, nil
	}
	return tr(r.Lang(), keyResetFailed) + msg, 201, nil
}

// resolveOpenLogBack 是否开启控制台日志备份（等价 AbstractProjectCommander.resolveOpenLogBack）。
func (r *Runtime) resolveOpenLogBack(info, original map[string]any) bool {
	if GetString(original, "runMode") == RunModeDsl {
		if config, err := parseDslConfig(original); err == nil {
			if value, ok := dslAutoBackToFile(config); ok {
				return value
			}
		}
	}
	return r.Options.ProjectLog.AutoBackupToFile
}

// LogBackList 返回日志备份列表（等价 /manage/log/logBack）。
func (r *Runtime) LogBackList(info map[string]any) (map[string]any, error) {
	original, err := r.Manager.ResolveModel(info)
	if err != nil {
		return nil, err
	}
	logFile, err := r.Manager.TouchLogFile(info)
	if err != nil {
		return nil, ServiceError(err)
	}
	logBack, err := r.Manager.ResolveLogBack(info)
	if err != nil {
		return nil, err
	}
	result := map[string]any{}
	if stat, err := os.Stat(logBack); err == nil && stat.IsDir() {
		entries, err := os.ReadDir(logBack)
		if err == nil {
			disableScanDir := GetBool(original, "disableScanDir")
			files := make([]map[string]any, 0, len(entries))
			for _, entry := range entries {
				item, err := fileToJSON(filepath.Join(logBack, entry.Name()), disableScanDir)
				if err == nil {
					files = append(files, item)
				}
			}
			sort.Slice(files, func(i, j int) bool {
				return GetString(files[i], "modifyTimeLong") > GetString(files[j], "modifyTimeLong")
			})
			result["array"] = files
		}
	}
	result["id"] = GetString(info, "id")
	if abs, err := filepath.Abs(logFile); err == nil {
		result["logPath"] = abs
	} else {
		result["logPath"] = logFile
	}
	if abs, err := filepath.Abs(logBack); err == nil {
		result["logBackPath"] = abs
	} else {
		result["logBackPath"] = logBack
	}
	return result, nil
}

// fileToJSON 文件信息（等价 Java FileUtils.fileToJson；long 字段输出字符串）。
func fileToJSON(path string, disableScanDir bool) (map[string]any, error) {
	stat, err := os.Stat(path)
	if err != nil {
		return nil, err
	}
	directory := stat.IsDir()
	item := map[string]any{
		"isDirectory":    directory,
		"filename":       stat.Name(),
		"modifyTimeLong": strconv.FormatInt(stat.ModTime().UnixMilli(), 10),
	}
	if !directory || !disableScanDir {
		size, err := dirSize(path)
		if err != nil {
			size = stat.Size()
		}
		item["fileSizeLong"] = strconv.FormatInt(size, 10)
	}
	return item, nil
}

// dirSize 计算文件/目录大小（等价 FileUtil.size）。
func dirSize(path string) (int64, error) {
	stat, err := os.Stat(path)
	if err != nil {
		return 0, err
	}
	if !stat.IsDir() {
		return stat.Size(), nil
	}
	var total int64
	err = filepath.Walk(path, func(_ string, info os.FileInfo, err error) error {
		if err != nil {
			return nil
		}
		if !info.IsDir() {
			total += info.Size()
		}
		return nil
	})
	return total, err
}

// LogBackDelete 删除指定日志备份文件/目录（等价 /manage/log/logBack_delete）。
func (r *Runtime) LogBackDelete(info map[string]any, name string) error {
	if strings.TrimSpace(name) == "" {
		return ValidationKey(keyNoFileFound)
	}
	logBack, err := r.Manager.ResolveLogBack(info)
	if err != nil {
		return err
	}
	if stat, err := os.Stat(logBack); err != nil || !stat.IsDir() {
		return BusinessKey(keyNoCorrespondingFolder)
	}
	target, err := safeJoin(logBack, name)
	if err != nil {
		return err
	}
	if _, err := os.Lstat(target); err != nil {
		return BusinessKey(keyNoCorrespondingFile)
	}
	// Java 删除后不再检查结果，直接返回成功
	proc.SystemFastDel(target)
	return nil
}

// LogBackDownloadFile 解析备份文件下载路径（等价 /manage/log/logBack_download）。
func (r *Runtime) LogBackDownloadFile(info map[string]any, name string) (string, error) {
	if strings.TrimSpace(name) == "" {
		return "", ValidationKey(keyCorrespondingFileNeeded)
	}
	logBack, err := r.Manager.ResolveLogBack(info)
	if err != nil {
		// Java 在 try 中调用，异常被捕获后返回 400 下载失败
		return "", ServiceError(err)
	}
	if stat, err := os.Stat(logBack); err != nil || !stat.IsDir() {
		return "", BusinessKeySuffix(keyNoCorrespondingFileColon, logBack)
	}
	target, err := safeJoin(logBack, name)
	if err != nil {
		return "", ServiceError(err)
	}
	stat, err := os.Stat(target)
	if err != nil || stat.IsDir() {
		return "", ServiceError(Validationf(tr(r.Lang(), keyFileNotFound)))
	}
	return target, nil
}

// ExportLogFile 返回控制台日志文件绝对路径（等价 /manage/log/export）。
func (r *Runtime) ExportLogFile(info map[string]any) (string, error) {
	file, err := r.Manager.ResolveLogFile(info)
	if err != nil {
		return "", err
	}
	if stat, err := os.Stat(file); err != nil || stat.IsDir() {
		return "", BusinessKeySuffix(keyLogFileNotFound, file)
	}
	return filepath.Abs(file)
}

// safeJoin 安全拼接子路径，拒绝绝对路径与 .. 越界（含符号链接逃逸）。
func safeJoin(baseDir, name string) (string, error) {
	sub := strings.TrimSpace(name)
	sub = strings.TrimPrefix(sub, "/")
	sub = strings.TrimPrefix(sub, "\\")
	if sub == "" || filepath.IsAbs(sub) {
		return "", ValidationKey(keyProjectPathPromotion)
	}
	target := filepath.Join(baseDir, sub)
	if !isSubPath(baseDir, target) {
		return "", ValidationKey(keyProjectPathPromotion)
	}
	// 防符号链接逃逸：存在时比较真实路径
	if realBase, err := filepath.EvalSymlinks(baseDir); err == nil {
		if realTarget, err := filepath.EvalSymlinks(target); err == nil {
			if !isSubPath(realBase, realTarget) {
				return "", ValidationKey(keyProjectPathPromotion)
			}
		}
	}
	return target, nil
}

// isSubPath 判断 child 是否位于 parent 目录内（含相等）。
func isSubPath(parent, child string) bool {
	parent = filepath.Clean(parent)
	child = filepath.Clean(child)
	if parent == child {
		return true
	}
	relative, err := filepath.Rel(parent, child)
	if err != nil {
		return false
	}
	if relative == ".." || strings.HasPrefix(relative, ".."+string(filepath.Separator)) {
		return false
	}
	return true
}

// copyFileContent 复制文件内容（等价 FileUtil.copy）。
func copyFileContent(src, target string) error {
	if err := os.MkdirAll(filepath.Dir(target), 0o755); err != nil {
		return err
	}
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer func() { _ = in.Close() }()
	out, err := os.OpenFile(target, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0o644)
	if err != nil {
		return err
	}
	defer func() { _ = out.Close() }()
	_, err = io.Copy(out, in)
	return err
}

// readableFileSize 格式化文件大小（等价 hutool DataSizeUtil.format：按 1024 进制、最多 2 位小数）。
func readableFileSize(size int64) string {
	if size <= 0 {
		return "0"
	}
	units := []string{"B", "KB", "MB", "GB", "TB", "PB", "EB"}
	index := int(math.Log10(float64(size)) / math.Log10(1024))
	if index < 0 {
		index = 0
	}
	if index > len(units)-1 {
		index = len(units) - 1
	}
	value := float64(size) / math.Pow(1024, float64(index))
	text := strconv.FormatFloat(value, 'f', 2, 64)
	text = strings.TrimRight(strings.TrimRight(text, "0"), ".")
	return text + " " + units[index]
}
