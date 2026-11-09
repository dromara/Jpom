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
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"path"
	"path/filepath"
	"strings"
	"time"
)

// UploadOptions are the form fields of /manage/file/sharding-merge.
type UploadOptions struct {
	// Type is "unzip" when the merged file must be extracted instead of moved.
	Type string
	// LevelName is the destination directory relative to the project lib.
	LevelName string
	// StripComponents drops leading path levels while extracting.
	StripComponents int
	// After requests a project restart after the upload (restart/1/2/3).
	After string
	// CloseFirst stops the project before replacing its files.
	CloseFirst bool
	// ClearType "clear" empties the destination before writing.
	ClearType string
}

// UploadResult describes the optional project side effect of an upload.
type UploadResult struct {
	Restarted bool
	Success   bool
	Data      any
}

// isRestartAfter mirrors ProjectFileControl#saveProjectFileAfter.
func isRestartAfter(after string) bool {
	value := strings.TrimSpace(after)
	if value == "" {
		return false
	}
	if strings.EqualFold(value, "restart") {
		return true
	}
	code := 0
	if _, err := fmt.Sscanf(value, "%d", &code); err != nil {
		return false
	}
	return code >= 1 && code <= 3
}

// UploadProjectFile places a merged upload into the project directory, mirroring
// ProjectFileControl#upload.
func (s *Service) UploadProjectFile(id, mergedPath string, options UploadOptions) (UploadResult, error) {
	// The merged sharding file is a temporary artefact: the Java agent deletes it
	// after moving/extracting it.
	defer func() { _ = os.RemoveAll(mergedPath) }()
	info, err := s.Project(id)
	if err != nil {
		return UploadResult{}, err
	}
	_, target, err := s.LibAndTarget(info, options.LevelName)
	if err != nil {
		return UploadResult{}, err
	}
	backupID, err := s.Backup(info)
	if err != nil {
		return UploadResult{}, err
	}
	defer s.CheckDiff(info, backupID)
	// saveProjectFileBefore
	if options.CloseFirst {
		if err := s.stopProjectBefore(info); err != nil {
			return UploadResult{}, invalidf("关闭项目失败：%v", err)
		}
	}
	if strings.EqualFold(strings.TrimSpace(options.ClearType), "clear") {
		if err := cleanDir(target); err != nil {
			s.Log().Warn("clean project directory failed", "path", target, "error", err)
		}
	}
	fileName := filepath.Base(mergedPath)
	if strings.EqualFold(strings.TrimSpace(options.Type), "unzip") {
		if err := s.Uncompress(mergedPath, target, options.StripComponents); err != nil {
			return UploadResult{}, err
		}
	} else {
		if _, err := moveIntoDir(mergedPath, target); err != nil {
			return UploadResult{}, err
		}
	}
	s.webhook(info, "fileChange", map[string]any{
		"changeEvent": "upload",
		"levelName":   options.LevelName,
		"fileType":    options.Type,
		"fileName":    fileName,
	})
	result := UploadResult{}
	if isRestartAfter(options.After) {
		success, data, restarted := s.restartProjectAfter(info)
		result = UploadResult{Restarted: restarted, Success: success, Data: data}
	}
	return result, nil
}

// PreflightProjectUpload validates the project id and the destination directory
// before the sharded upload is merged. The Java agent validates the request in
// spring before the merge, so a rejected request must not destroy the uploaded
// chunks (otherwise a retry reports "分片丢失").
func (s *Service) PreflightProjectUpload(id, levelName string) error {
	info, err := s.Project(id)
	if err != nil {
		return err
	}
	_, _, err = s.LibAndTarget(info, levelName)
	return err
}

// ValidateUploadPath validates the absolute destination directory used by the
// file2 sharding-merge endpoint, mirroring the @ValidatorItem check of the Java
// controller which runs before the merge.
func ValidateUploadPath(destinationPath string) (string, error) {
	destinationPath = strings.TrimSpace(destinationPath)
	if destinationPath == "" || strings.IndexByte(destinationPath, 0) >= 0 {
		return "", invalidf("文件目录缺失")
	}
	if !filepath.IsAbs(destinationPath) {
		return "", invalidf("文件目录缺失")
	}
	abs, err := filepath.Abs(destinationPath)
	if err != nil {
		return "", invalidf("文件目录缺失")
	}
	return abs, nil
}

// UploadToPath places a merged upload into an absolute destination directory,
// used by /manage/file2/sharding-merge (the file release feature). The server
// validates the release path against the server side authorization before
// calling the agent, so it is accepted as an absolute directory.
func (s *Service) UploadToPath(destinationPath, mergedPath, uploadType string, stripComponents int) error {
	defer func() { _ = os.RemoveAll(mergedPath) }()
	abs, err := ValidateUploadPath(destinationPath)
	if err != nil {
		return err
	}
	if strings.EqualFold(strings.TrimSpace(uploadType), "unzip") {
		return s.Uncompress(mergedPath, abs, stripComponents)
	}
	if _, err := moveIntoDir(mergedPath, abs); err != nil {
		return err
	}
	return nil
}

// DeleteOptions are the fields of /manage/file/deleteFile.
type DeleteOptions struct {
	Filename  string
	Type      string
	LevelName string
}

// DeleteFile removes one file or clears a directory, mirroring
// ProjectFileControl#deleteFile.
func (s *Service) DeleteFile(id string, options DeleteOptions) (string, error) {
	info, err := s.Project(id)
	if err != nil {
		return "", err
	}
	_, dir, err := s.LibAndTarget(info, options.LevelName)
	if err != nil {
		return "", err
	}
	backupID, err := s.Backup(info)
	if err != nil {
		return "", err
	}
	defer s.CheckDiff(info, backupID)
	if strings.EqualFold(strings.TrimSpace(options.Type), "clear") {
		if cleanErr := cleanDir(dir); cleanErr == nil {
			s.webhook(info, "fileChange", map[string]any{
				"changeEvent": "delete",
				"levelName":   options.LevelName,
				"deleteType":  options.Type,
				"fileName":    options.Filename,
			})
			return "清除成功", nil
		}
		if s.projectRunning(info) {
			return "", invalidf("文件被占用，请先停止项目")
		}
		return "", fmt.Errorf("删除失败：%s", dir)
	}
	if strings.TrimSpace(options.Filename) == "" {
		return "", invalidf("请选择要删除的文件")
	}
	target, err := secureJoin(dir, options.Filename)
	if err != nil {
		return "", err
	}
	if err := os.RemoveAll(target); err != nil {
		return "", fmt.Errorf("删除失败")
	}
	s.webhook(info, "fileChange", map[string]any{
		"changeEvent": "delete",
		"levelName":   options.LevelName,
		"deleteType":  options.Type,
		"fileName":    options.Filename,
	})
	return "删除成功", nil
}

// BatchDelete removes the files listed in a DiffFileVo request, mirroring
// ProjectFileControl#batchDelete.
func (s *Service) BatchDelete(request DiffFileRequest) error {
	info, err := s.Project(request.ID)
	if err != nil {
		return err
	}
	lib, dir, err := s.LibAndTarget(info, request.Dir)
	if err != nil {
		return err
	}
	if len(request.Data) == 0 {
		return errComparisonDataMissing
	}
	backupID, err := s.Backup(info)
	if err != nil {
		return err
	}
	defer s.CheckDiff(info, backupID)
	for _, item := range request.Data {
		target, err := secureJoin(lib, request.Dir, item.Name)
		if err != nil {
			return err
		}
		if err := os.RemoveAll(target); err != nil {
			return fmt.Errorf("删除失败：%s", target)
		}
	}
	_ = dir
	s.webhook(info, "fileChange", map[string]any{
		"changeEvent": "batch-delete",
		"levelName":   request.Dir,
	})
	return nil
}

// ReadFile returns the text content of a project file, mirroring
// ProjectFileControl#readFile.
func (s *Service) ReadFile(id, filePath, filename string) (string, error) {
	info, err := s.Project(id)
	if err != nil {
		return "", err
	}
	charset, err := checkFileSuffix(s.Whitelist().AllowEditSuffix, filename)
	if err != nil {
		return "", err
	}
	_, target, err := s.LibAndTarget(info, filePath, filename)
	if err != nil {
		return "", err
	}
	content, err := os.ReadFile(target)
	if err != nil {
		// The Java agent lets hutool/FileReader throw, which Spring's generic
		// handler renders as "服务异常：File not exist: <path>" (code 500).
		s.Log().Warn("read project file failed", "path", target, "error", err)
		return "", fmt.Errorf("服务异常：File not exist: %s", target)
	}
	return decodeText(content, charset)
}

// UpdateConfigFile writes the text content of a project file, mirroring
// ProjectFileControl#updateConfigFile.
func (s *Service) UpdateConfigFile(id, filePath, filename, fileText string) error {
	info, err := s.Project(id)
	if err != nil {
		return err
	}
	charset, err := checkFileSuffix(s.Whitelist().AllowEditSuffix, filename)
	if err != nil {
		return err
	}
	_, target, err := s.LibAndTarget(info, filePath, filename)
	if err != nil {
		return err
	}
	backupID, err := s.Backup(info)
	if err != nil {
		return err
	}
	defer s.CheckDiff(info, backupID)
	content, err := encodeText(fileText, charset)
	if err != nil {
		return err
	}
	if err := os.MkdirAll(filepath.Dir(target), 0o755); err != nil {
		return err
	}
	if err := os.WriteFile(target, content, 0o644); err != nil {
		return fmt.Errorf("文件写入失败:%w", err)
	}
	s.webhook(info, "fileChange", map[string]any{
		"changeEvent": "edit",
		"levelName":   filePath,
		"fileName":    filename,
	})
	return nil
}

// Copy duplicates a file or directory with a non conflicting name, mirroring
// ProjectFileControl#copy.
func (s *Service) Copy(id, filePath, filename string) error {
	info, err := s.Project(id)
	if err != nil {
		return err
	}
	lib, source, err := s.LibAndTarget(info, filePath, filename)
	if err != nil {
		return err
	}
	base := mainName(filename)
	extension := extName(filename)
	counter := 1
	var target string
	for {
		var name string
		if extension != "" {
			name = fmt.Sprintf("%s(%d).%s", base, counter, extension)
		} else {
			name = fmt.Sprintf("%s(%d)", base, counter)
		}
		candidate, err := secureJoin(lib, filePath, name)
		if err != nil {
			return err
		}
		if !exists(candidate) {
			target = candidate
			break
		}
		counter++
		if counter > 10000 {
			return fmt.Errorf("复制失败,无法生成新的文件名")
		}
	}
	if isDir(source) {
		return copyContent(source, target, false)
	}
	return copyFile(source, target, false)
}

// CompressFileFolder compresses one project directory into a new zip/tar/tar.gz
// archive next to it, mirroring ProjectFileControl#compress.
func (s *Service) CompressFileFolder(id, filePath, filename, archiveType string) error {
	info, err := s.Project(id)
	if err != nil {
		return err
	}
	lib, source, err := s.LibAndTarget(info, filePath, filename)
	if err != nil {
		return err
	}
	if !isDir(source) {
		return invalidf("请选择文件夹进行压缩")
	}
	var extension, kind string
	switch archiveType {
	case "zip":
		extension, kind = ".zip", "zip"
	case "tar":
		extension, kind = ".tar", "tar"
	case "tar.gz":
		extension, kind = ".tar.gz", "tar.gz"
	default:
		return invalidf("不支持的压缩类型,%s", archiveType)
	}
	base := mainName(filename)
	counter := 0
	var target string
	for {
		var name string
		if counter == 0 {
			name = base + extension
		} else {
			name = fmt.Sprintf("%s(%d)%s", base, counter, extension)
		}
		candidate, err := secureJoin(lib, filePath, name)
		if err != nil {
			return err
		}
		if !exists(candidate) {
			target = candidate
			break
		}
		counter++
		if counter > 10000 {
			return fmt.Errorf("压缩失败,无法生成新的文件名")
		}
	}
	return Compress(source, target, kind)
}

// NewFileFolder creates a new file or directory inside the project, mirroring
// ProjectFileControl#newFileFolder. unFolder uses the same inverted semantics as
// Convert.toBool: unFolder=true creates a file, unset/false a directory.
func (s *Service) NewFileFolder(id, levelName, filename, unFolder string) error {
	info, err := s.Project(id)
	if err != nil {
		return err
	}
	_, target, err := s.LibAndTarget(info, levelName, filename)
	if err != nil {
		return err
	}
	if exists(target) {
		return invalidf("文件夹或者文件已存在")
	}
	folder := !toBool(unFolder, false)
	if folder {
		if err := os.MkdirAll(target, 0o755); err != nil {
			return err
		}
	} else {
		if err := os.MkdirAll(filepath.Dir(target), 0o755); err != nil {
			return err
		}
		file, err := os.OpenFile(target, os.O_CREATE|os.O_WRONLY, 0o644)
		if err != nil {
			return err
		}
		if err := file.Close(); err != nil {
			return err
		}
	}
	s.webhook(info, "fileChange", map[string]any{
		"changeEvent": "newFileOrFolder",
		"levelName":   levelName,
		"fileName":    filename,
		"folder":      folder,
	})
	return nil
}

// Rename renames a file or directory inside the project, mirroring
// ProjectFileControl#rename.
func (s *Service) Rename(id, levelName, filename, newName string) error {
	info, err := s.Project(id)
	if err != nil {
		return err
	}
	lib, source, err := s.LibAndTarget(info, levelName, filename)
	if err != nil {
		return err
	}
	target, err := secureJoin(lib, levelName, newName)
	if err != nil {
		return err
	}
	if !exists(source) {
		return invalidf("文件不存在")
	}
	if exists(target) {
		return invalidf("文件名已经存在拉")
	}
	if err := os.MkdirAll(filepath.Dir(target), 0o755); err != nil {
		return err
	}
	if err := os.Rename(source, target); err != nil {
		return err
	}
	s.webhook(info, "fileChange", map[string]any{
		"changeEvent": "rename",
		"levelName":   levelName,
		"fileName":    filename,
		"newname":     newName,
	})
	return nil
}

// DownloadPath resolves a project file for /manage/file/download.
func (s *Service) DownloadPath(id, levelName, filename string) (string, error) {
	if strings.TrimSpace(filename) == "" {
		return "", invalidf("请选择文件")
	}
	info, err := s.Project(id)
	if err != nil {
		return "", err
	}
	_, target, err := s.LibAndTarget(info, levelName, filename)
	if err != nil {
		return "", err
	}
	return target, nil
}

// RemoteDownload downloads a remote URL into the project, mirroring
// ProjectFileControl#remoteDownload.
func (s *Service) RemoteDownload(id, rawURL, levelName, unzip string, stripComponents int) (string, error) {
	rawURL = strings.TrimSpace(rawURL)
	if rawURL == "" {
		return "", invalidf("请输入正确的远程地址")
	}
	parsed, err := url.Parse(rawURL)
	if err != nil || (parsed.Scheme != "http" && parsed.Scheme != "https") || parsed.Host == "" {
		return "", invalidf("请输入正确的远程地址")
	}
	info, err := s.Project(id)
	if err != nil {
		return "", err
	}
	dateDir, err := s.shardingDateDir()
	if err != nil {
		return "", fmt.Errorf("下载远程文件失败：%w", err)
	}
	tempDir := filepath.Join(dateDir, randomHex(16))
	if err := os.MkdirAll(tempDir, 0o755); err != nil {
		return "", fmt.Errorf("下载远程文件失败：%w", err)
	}
	downloadFile, size, err := downloadRemote(rawURL, tempDir)
	if err != nil {
		return "", fmt.Errorf("下载远程文件失败：%w", err)
	}
	fileSize := readableSize(size)
	backupID, err := s.Backup(info)
	if err != nil {
		return "", err
	}
	defer s.CheckDiff(info, backupID)
	_, target, err := s.LibAndTarget(info, levelName)
	if err != nil {
		return "", err
	}
	if toBool(unzip, false) {
		if err := s.Uncompress(downloadFile, target, stripComponents); err != nil {
			return "", fmt.Errorf("下载远程文件失败：%w", err)
		}
		if err := os.RemoveAll(downloadFile); err != nil {
			s.Log().Error("delete downloaded temp file failed", "path", downloadFile, "error", err)
		}
	} else {
		if err := os.MkdirAll(target, 0o755); err != nil {
			return "", fmt.Errorf("下载远程文件失败：%w", err)
		}
		if _, err := moveIntoDir(downloadFile, target); err != nil {
			return "", fmt.Errorf("下载远程文件失败：%w", err)
		}
	}
	s.webhook(info, "fileChange", map[string]any{
		"changeEvent": "remoteDownload",
		"levelName":   levelName,
		// The Java agent reports the destination directory name here (it calls
		// file.getName() on the target dir), not the downloaded file name.
		"fileName": filepath.Base(target),
		"url":      rawURL,
	})
	return "下载成功文件大小：" + fileSize, nil
}

// downloadRemote downloads rawURL into destDir and returns the local path and
// size. The file name honours Content-Disposition, then the URL path, like
// hutool HttpResponse#completeFileNameFromHeader.
func downloadRemote(rawURL, destDir string) (string, int64, error) {
	client := &http.Client{Timeout: 10 * time.Minute}
	request, err := http.NewRequest(http.MethodGet, rawURL, nil)
	if err != nil {
		return "", 0, err
	}
	response, err := client.Do(request)
	if err != nil {
		return "", 0, err
	}
	defer func() { _ = response.Body.Close() }()
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return "", 0, fmt.Errorf("HTTP 状态码 %d", response.StatusCode)
	}
	name := replyFileName(response)
	if name == "" {
		name = "download"
	}
	target := filepath.Join(destDir, name)
	out, err := os.OpenFile(target, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, 0o644)
	if err != nil {
		return "", 0, err
	}
	size, err := io.Copy(out, response.Body)
	if err != nil {
		_ = out.Close()
		return "", 0, err
	}
	if err := out.Close(); err != nil {
		return "", 0, err
	}
	return target, size, nil
}

// replyFileName resolves the remote file name from Content-Disposition, falling
// back to the URL path.
func replyFileName(response *http.Response) string {
	if disposition := response.Header.Get("Content-Disposition"); disposition != "" {
		if name := parseDispositionFileName(disposition); name != "" {
			return sanitizeRemoteName(name)
		}
	}
	if response.Request != nil && response.Request.URL != nil {
		urlPath := response.Request.URL.Path
		name := path.Base(urlPath)
		name = strings.TrimSpace(name)
		if name != "" && name != "." && name != "/" {
			if decoded, err := url.PathUnescape(name); err == nil {
				name = decoded
			}
			return sanitizeRemoteName(name)
		}
	}
	return ""
}

// parseDispositionFileName extracts filename / filename* from a
// Content-Disposition header.
func parseDispositionFileName(disposition string) string {
	parts := strings.Split(disposition, ";")
	for _, part := range parts {
		part = strings.TrimSpace(part)
		lower := strings.ToLower(part)
		if !strings.HasPrefix(lower, "filename") {
			continue
		}
		index := strings.IndexByte(part, '=')
		if index < 0 {
			continue
		}
		value := strings.Trim(strings.TrimSpace(part[index+1:]), "\"")
		if strings.HasPrefix(lower, "filename*") {
			// RFC 5987: charset'lang'value
			sections := strings.SplitN(value, "'", 3)
			if len(sections) == 3 {
				value = sections[2]
			}
			if decoded, err := url.PathUnescape(value); err == nil {
				value = decoded
			}
		}
		value = strings.TrimSpace(value)
		if value != "" {
			return value
		}
	}
	return ""
}

// sanitizeRemoteName reduces a remote supplied name to a safe base name.
func sanitizeRemoteName(name string) string {
	name = strings.ReplaceAll(name, "\\", "/")
	name = path.Base(name)
	switch name {
	case "", ".", "..", "/":
		return ""
	}
	return name
}

// toBool converts an optional form value to a boolean with a default,
// mirroring cn.hutool.core.convert.Convert#toBool(Object, Boolean).
func toBool(value string, defaultValue bool) bool {
	value = strings.TrimSpace(strings.ToLower(value))
	if value == "" {
		return defaultValue
	}
	switch value {
	case "true", "1", "yes", "y", "on":
		return true
	default:
		return false
	}
}
