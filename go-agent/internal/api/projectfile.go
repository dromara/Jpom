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
	"io"
	"mime"
	"mime/multipart"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/httpx"
	"jpom.io/go-agent/internal/project"
	"jpom.io/go-agent/internal/projectfile"
)

// registerProjectFile owns /manage/file/* and /manage/file2/* endpoints.
func registerProjectFile(r *httpx.Router, a *app.App) {
	service := projectfile.New(a)
	wireProjectFileHooks(project.NewRuntime(a.BaseDir, a.Config, a.Log()))

	// POST /manage/file/getFileList
	r.Handle(http.MethodPost, "/manage/file/getFileList", func(c *httpx.Context) {
		files, err := service.GetFileList(c.Form("id"), c.Form("path"))
		if err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("查询成功", files)
	})

	r.Handle(http.MethodPost, "/manage/file/diff_file", func(c *httpx.Context) {
		var request projectfile.DiffFileRequest
		if err := c.BindJSON(&request); err != nil {
			c.JSONCode(417, "传入的参数格式不正确", nil)
			return
		}
		result, err := service.DiffFile(request)
		if err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("", result)
	})

	r.Handle(http.MethodPost, "/manage/file/batch_delete", func(c *httpx.Context) {
		var request projectfile.DiffFileRequest
		if err := c.BindJSON(&request); err != nil {
			c.JSONCode(417, "传入的参数格式不正确", nil)
			return
		}
		if err := service.BatchDelete(request); err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("删除成功", nil)
	})

	r.Handle(http.MethodPost, "/manage/file/upload-sharding", func(c *httpx.Context) {
		filename, body, err := fileMultipartUpload(c)
		if err != nil {
			failProjectFile(c, err)
			return
		}
		if body != nil {
			defer func() { _ = body.Close() }()
		}
		options := projectfile.ShardingChunkOptions{
			SliceID:    c.Form("sliceId"),
			FileSumMD5: c.Form("fileSumMd5"),
			TotalSlice: c.Form("totalSlice"),
			NowSlice:   c.Form("nowSlice"),
		}
		if err := service.SaveShardingChunk(filename, body, options); err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("上传成功", nil)
	})

	r.Handle(http.MethodPost, "/manage/file2/upload-sharding", func(c *httpx.Context) {
		filename, body, err := fileMultipartUpload(c)
		if err != nil {
			failProjectFile(c, err)
			return
		}
		if body != nil {
			defer func() { _ = body.Close() }()
		}
		options := projectfile.ShardingChunkOptions{
			SliceID:    c.Form("sliceId"),
			FileSumMD5: c.Form("fileSumMd5"),
			TotalSlice: c.Form("totalSlice"),
			NowSlice:   c.Form("nowSlice"),
		}
		if err := service.SaveShardingChunk(filename, body, options); err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("上传成功", nil)
	})

	r.Handle(http.MethodPost, "/manage/file/sharding-merge", func(c *httpx.Context) {
		// Validate the project and the destination before merging: a rejected
		// request must not consume the uploaded chunks (D1).
		if err := service.PreflightProjectUpload(c.Form("id"), c.Form("levelName")); err != nil {
			failProjectFile(c, err)
			return
		}
		merged, err := service.MergeSharding(c.Form("sliceId"), c.Form("fileSumMd5"), c.Form("totalSlice"))
		if err != nil {
			failProjectFile(c, err)
			return
		}
		options := projectfile.UploadOptions{
			Type:            c.Form("type"),
			LevelName:       c.Form("levelName"),
			StripComponents: fileFormInt(c.Form("stripComponents"), 0),
			After:           c.Form("after"),
			CloseFirst:      fileFormBool(c.Form("closeFirst")),
			ClearType:       c.Form("clearType"),
		}
		result, err := service.UploadProjectFile(c.Form("id"), merged, options)
		if err != nil {
			failProjectFile(c, err)
			return
		}
		if result.Restarted {
			code := 200
			if !result.Success {
				code = 405
			}
			c.JSONCode(code, "上传成功并重启", result.Data)
			return
		}
		c.OKMsg("上传成功", nil)
	})

	r.Handle(http.MethodPost, "/manage/file2/sharding-merge", func(c *httpx.Context) {
		// The Java controller validates the required "path" parameter before the
		// merge runs (D1); do the same so a bad request keeps the chunks.
		if _, err := projectfile.ValidateUploadPath(c.Form("path")); err != nil {
			failProjectFile(c, err)
			return
		}
		merged, err := service.MergeSharding(c.Form("sliceId"), c.Form("fileSumMd5"), c.Form("totalSlice"))
		if err != nil {
			failProjectFile(c, err)
			return
		}
		if err := service.UploadToPath(c.Form("path"), merged, c.Form("type"), fileFormInt(c.Form("stripComponents"), 0)); err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("上传成功", nil)
	})

	r.Handle(http.MethodPost, "/manage/file/deleteFile", func(c *httpx.Context) {
		message, err := service.DeleteFile(c.Form("id"), projectfile.DeleteOptions{
			Filename:  c.Form("filename"),
			Type:      c.Form("type"),
			LevelName: c.Form("levelName"),
		})
		if err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg(message, nil)
	})

	r.Handle(http.MethodPost, "/manage/file/read_file", func(c *httpx.Context) {
		content, err := service.ReadFile(c.Form("id"), c.Form("filePath"), c.Form("filename"))
		if err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("", content)
	})

	r.Handle(http.MethodPost, "/manage/file/copy", func(c *httpx.Context) {
		if err := service.Copy(c.Form("id"), c.Form("filePath"), c.Form("filename")); err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("复制成功", nil)
	})

	r.Handle(http.MethodPost, "/manage/file/compress", func(c *httpx.Context) {
		if err := service.CompressFileFolder(c.Form("id"), c.Form("filePath"), c.Form("filename"), c.Form("type")); err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("压缩成功", nil)
	})

	r.Handle(http.MethodPost, "/manage/file/update_config_file", func(c *httpx.Context) {
		if err := service.UpdateConfigFile(c.Form("id"), c.Form("filePath"), c.Form("filename"), c.Form("fileText")); err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("文件写入成功", nil)
	})

	r.Handle(http.MethodGet, "/manage/file/download", func(c *httpx.Context) {
		target, err := service.DownloadPath(c.Form("id"), c.Form("levelName"), c.Form("filename"))
		if err != nil {
			failProjectFile(c, err)
			return
		}
		serveFileDownload(c, target, func(message string) {
			writeFileRawJSONError(c, 400, message)
		}, func(message string) {
			// The OS error may contain local paths: log it, but only return the
			// Java message prefix to the caller (D8).
			c.App.Log().Warn("download project file failed", "path", target, "error", message)
			writeFileRawJSONError(c, 400, "下载失败。请刷新页面后重试")
		})
	})

	r.Handle(http.MethodPost, "/manage/file/remote_download", func(c *httpx.Context) {
		message, err := service.RemoteDownload(c.Form("id"), c.Form("url"), c.Form("levelName"),
			c.Form("unzip"), fileFormInt(c.Form("stripComponents"), 0))
		if err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg(message, nil)
	})

	r.Handle(http.MethodPost, "/manage/file/new_file_folder.json", func(c *httpx.Context) {
		if err := service.NewFileFolder(c.Form("id"), c.Form("levelName"), c.Form("filename"), c.Form("unFolder")); err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("操作成功", nil)
	})

	r.Handle(http.MethodPost, "/manage/file/rename.json", func(c *httpx.Context) {
		if err := service.Rename(c.Form("id"), c.Form("levelName"), c.Form("filename"), c.Form("newname")); err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("操作成功", nil)
	})

	// file backup endpoints
	r.Handle(http.MethodPost, "/manage/file/list-backup", func(c *httpx.Context) {
		result, err := service.ListBackup(c.Form("id"))
		if err != nil {
			failProjectFile(c, err)
			return
		}
		if result == nil {
			// Jackson omits a null data field; a typed nil pointer in an any
			// value would still be encoded as null.
			c.OKMsg("查询成功", nil)
			return
		}
		c.OKMsg("查询成功", result)
	})

	r.Handle(http.MethodPost, "/manage/file/backup-item-files", func(c *httpx.Context) {
		files, err := service.BackupItemFiles(c.Form("id"), c.Form("path"), c.Form("backupId"))
		if err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("查询成功", files)
	})

	r.Handle(http.MethodGet, "/manage/file/backup-download", func(c *httpx.Context) {
		target, err := service.BackupDownloadPath(c.Form("id"), c.Form("backupId"), c.Form("levelName"), c.Form("filename"))
		if err != nil {
			failProjectFile(c, err)
			return
		}
		serveFileDownload(c, target, func(message string) {
			writeFileRawTextError(c, message)
		}, func(message string) {
			writeFileRawTextError(c, "下载文件异常:"+message)
		})
	})

	r.Handle(http.MethodPost, "/manage/file/backup-delete", func(c *httpx.Context) {
		if err := service.BackupDelete(c.Form("id"), c.Form("backupId"), c.Form("levelName"), c.Form("filename")); err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("删除成功", nil)
	})

	r.Handle(http.MethodPost, "/manage/file/backup-recover", func(c *httpx.Context) {
		if err := service.BackupRecover(c.Form("id"), c.Form("backupId"), c.Form("type"), c.Form("levelName"), c.Form("filename")); err != nil {
			failProjectFile(c, err)
			return
		}
		c.OKMsg("还原成功", nil)
	})
}

// wireProjectFileHooks injects the project lifecycle and webhook capabilities
// required by the file endpoints. They live in the project domain so this keeps
// the file module free of process management code.
func wireProjectFileHooks(runtime *project.Runtime) {
	projectfile.Hooks.StopProject = func(info map[string]any) error {
		result, err := runtime.Operate(project.OpStop, info)
		if err != nil {
			return err
		}
		if result == nil || !result.Success {
			return fmt.Errorf("%s", projectResultMessage(result))
		}
		return nil
	}
	projectfile.Hooks.RestartProject = func(info map[string]any) (bool, string, any) {
		result, err := runtime.Operate(project.OpRestart, info)
		if err != nil {
			return false, err.Error(), nil
		}
		if result == nil {
			return false, "", nil
		}
		return result.Success, result.MsgStr(), result.JSON()
	}
	projectfile.Hooks.IsProjectRunning = func(info map[string]any) bool {
		return runtime.IsRun(info)
	}
	projectfile.Hooks.Webhook = func(info map[string]any, eventType string, values map[string]any) {
		runtime.WebhookFileChange(info, values)
	}
}

// projectResultMessage renders the Java CommandOpResult message string.
func projectResultMessage(result *project.OpResult) string {
	if result == nil {
		return ""
	}
	return result.MsgStr()
}

// failProjectFile writes the JSON envelope with the code the Java agent uses:
// 405 for validation failures (Spring Assert / JsonMessage.fail), 500 for
// unexpected exceptions.
func failProjectFile(c *httpx.Context, err error) {
	c.JSONCode(projectfile.HTTPCodeOf(err), err.Error(), nil)
}

// fileMultipartUpload returns the original name and the content of the "file"
// multipart part. A missing part is not an error here: the service reports the
// same "没有上传文件" message as the Java agent.
func fileMultipartUpload(c *httpx.Context) (string, multipart.File, error) {
	if err := c.R.ParseMultipartForm(32 << 20); err != nil {
		return "", nil, nil
	}
	file, header, err := c.R.FormFile("file")
	if err != nil {
		return "", nil, nil
	}
	return header.Filename, file, nil
}

// formInt parses an optional integer form field.
func fileFormInt(value string, defaultValue int) int {
	value = strings.TrimSpace(value)
	if value == "" {
		return defaultValue
	}
	parsed, err := strconv.Atoi(value)
	if err != nil {
		return defaultValue
	}
	return parsed
}

// formBool parses an optional boolean form field the way BooleanUtil.toBoolean
// does.
func fileFormBool(value string) bool {
	switch strings.ToLower(strings.TrimSpace(value)) {
	case "true", "1", "yes", "y", "on":
		return true
	default:
		return false
	}
}

// serveDownload streams an existing file (ServletUtil#write(response, file)).
func serveFileDownload(c *httpx.Context, target string, dirHandler, errHandler func(string)) {
	stat, err := os.Stat(target)
	if err != nil {
		errHandler(err.Error())
		return
	}
	if stat.IsDir() {
		dirHandler("暂不支持下载文件夹")
		return
	}
	contentType := mime.TypeByExtension(filepath.Ext(target))
	if contentType == "" {
		contentType = "application/octet-stream"
	}
	// Java FileUtil.getMimeType returns the bare media type (no charset
	// parameter); the server forwards it to the browser as is.
	if index := strings.IndexByte(contentType, ';'); index >= 0 {
		contentType = strings.TrimSpace(contentType[:index])
	}
	c.W.Header().Set("Content-Type", contentType)
	encoded := strings.ReplaceAll(url.QueryEscape(filepath.Base(target)), "+", "%20")
	encoded = strings.ReplaceAll(encoded, "%2A", "*")
	c.W.Header().Set("Content-Disposition", "attachment;filename=\""+encoded+"\"")
	c.W.Header().Set("Content-Length", strconv.FormatInt(stat.Size(), 10))
	c.W.WriteHeader(http.StatusOK)
	file, err := os.Open(target)
	if err != nil {
		return
	}
	defer func() { _ = file.Close() }()
	_, _ = io.Copy(c.W, file)
}

// writeRawJSONError writes a bare JsonMessage string, used by the download
// endpoints which the Java agent does not wrap in the normal envelope.
func writeFileRawJSONError(c *httpx.Context, code int, message string) {
	c.W.Header().Set("Content-Type", "application/json;charset=UTF-8")
	c.W.WriteHeader(http.StatusOK)
	_, _ = fmt.Fprintf(c.W, "{\"code\":%d,\"msg\":%s}", code, fileJSONString(message))
}

// writeRawTextError writes a plain text error, used by backup-download which the
// Java agent returns as text/html.
func writeFileRawTextError(c *httpx.Context, message string) {
	c.W.Header().Set("Content-Type", "text/html;charset=UTF-8")
	c.W.WriteHeader(http.StatusOK)
	_, _ = c.W.Write([]byte(message))
}

// jsonString encodes s as a JSON string literal.
func fileJSONString(value string) string {
	encoded, err := json.Marshal(value)
	if err != nil {
		return "\"\""
	}
	return string(encoded)
}
