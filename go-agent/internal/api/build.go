//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package api

import (
	"errors"
	"io"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"
	"sync"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/builder"
	"github.com/dromara/Jpom/go-agent/internal/httpx"
)

// buildManagers 为每个 App 缓存一个构建执行器（与 scriptRuntime 同套路）：
// api 与 ws 两个注册点必须共用同一个实例，否则 /build-log 订阅不到 /build/start 的 job。
var buildManagers sync.Map

// buildManagerFor 返回（并惰性创建）一个 App 的构建执行器。
func buildManagerFor(a *app.App) *builder.Manager {
	if value, ok := buildManagers.Load(a); ok {
		return value.(*builder.Manager)
	}
	created := builder.NewManager(a)
	actual, loaded := buildManagers.LoadOrStore(a, created)
	if loaded {
		return actual.(*builder.Manager)
	}
	return created
}

// registerBuild 注册构建执行域端点（PROTOCOL §1）。鉴权/工作空间头沿用 httpx，
// 隧道模式自动成立。
func registerBuild(r *httpx.Router, a *app.App) {
	manager := buildManagerFor(a)
	r.Handle(http.MethodPost, "/build/start", func(c *httpx.Context) { handleBuildStart(c, manager) })
	r.Handle(http.MethodPost, "/build/status", func(c *httpx.Context) { handleBuildStatus(c, manager) })
	r.Handle(http.MethodPost, "/build/cancel", func(c *httpx.Context) { handleBuildCancel(c, manager) })
	r.Handle(http.MethodPost, "/build/source-exists", func(c *httpx.Context) { handleBuildSourceExists(c, manager) })
	r.Handle(http.MethodGet, "/build/download", func(c *httpx.Context) { handleBuildDownload(c, manager) })
}

// handleBuildStart 对应 POST /build/start。
//
// 业务码：200 已受理、409 并发已满（Server 排队重试）、501 节点不支持该 job、500 其它错误
// （PROTOCOL §2，HTTP 仍为 200，沿用 agent 的 httpx 约定）。
func handleBuildStart(c *httpx.Context, manager *builder.Manager) {
	spec := &builder.Spec{}
	if err := c.BindJSON(spec); err != nil {
		c.FailCode(405, c.T("i18n.parameter_validation_failed.f0a1")+" "+err.Error())
		return
	}
	job, err := manager.Start(spec)
	if err != nil {
		switch {
		case errors.Is(err, builder.ErrBusy):
			c.FailCode(http.StatusConflict, c.T("i18n.build_thread_pool_rejected_task.3bad", spec.JobID))
		case errors.Is(err, builder.ErrUnsupported):
			c.FailCode(http.StatusNotImplemented, c.T("i18n.unsupported_prefix.4f8c")+err.Error())
		default:
			c.Fail(err.Error())
		}
		return
	}
	c.OK(map[string]any{
		"jobId":     job.JobID(),
		"sourceDir": job.SourceDir(),
		"accepted":  true,
	})
}

// handleBuildStatus 对应 POST /build/status；未知 jobId 返回 state=lost，
// 让 Server 直接把构建判失败并写明原因（PROTOCOL §4）。
func handleBuildStatus(c *httpx.Context, manager *builder.Manager) {
	jobID := buildJobID(c)
	status, err := manager.Status(jobID)
	if err != nil {
		c.OK(&builder.Status{JobID: jobID, State: builder.StateLost})
		return
	}
	c.OK(status)
}

// handleBuildCancel 对应 POST /build/cancel：对进程组发 SIGTERM → 5s → SIGKILL，
// 幂等（未知 jobId 也返回成功，PROTOCOL §5）。
func handleBuildCancel(c *httpx.Context, manager *builder.Manager) {
	jobID := buildJobID(c)
	manager.Cancel(jobID)
	c.OKMsg(c.T("i18n.cancel_success.285f"), map[string]any{"jobId": jobID})
}

// handleBuildDownload 对应 GET /build/download?jobId=&name=zip（流式 zip）。
// 未就绪返回 404（PROTOCOL §6），Server 只在收到 result 帧后拉取。
func handleBuildDownload(c *httpx.Context, manager *builder.Manager) {
	// 反向隧道下参数是表单 body（GET 也带 body），Go 的 FormValue 不会解析 GET body，
	// 因此这里同时读 query 与 body（2026-11-19 真机验证：只读 Form 会一直 404）。
	params := downloadParams(c)
	jobID := strings.TrimSpace(params.Get("jobId"))
	path, ready := manager.ZipPath(jobID)
	if !ready {
		// 兼容：jobId 丢失时用构建 id + 构建号直接定位产物 zip
		buildModelID := strings.TrimSpace(params.Get("buildModelId"))
		buildNumberID, _ := strconv.Atoi(strings.TrimSpace(params.Get("buildNumberId")))
		if buildModelID != "" && buildNumberID > 0 {
			path, ready = manager.ZipPathByBuild(buildModelID, buildNumberID)
		}
	}
	if !ready {
		c.W.Header().Set("Content-Type", "application/json;charset=UTF-8")
		c.W.WriteHeader(http.StatusNotFound)
		_, _ = c.W.Write([]byte(`{"code":404,"msg":"build product is not ready","jobId":"` + jobID + `"}`))
		return
	}
	file, err := os.Open(path)
	if err != nil {
		c.W.WriteHeader(http.StatusNotFound)
		return
	}
	defer func() { _ = file.Close() }()
	info, err := file.Stat()
	if err != nil {
		c.W.WriteHeader(http.StatusNotFound)
		return
	}
	c.W.Header().Set("Content-Type", "application/zip")
	c.W.Header().Set("Content-Length", strconv.FormatInt(info.Size(), 10))
	c.W.Header().Set("Content-Disposition", httpx.ContentDisposition("zip"))
	c.W.WriteHeader(http.StatusOK)
	if _, err := io.Copy(c.W, file); err != nil {
		c.App.Log().Warn("stream build product failed", "jobId", jobID, "error", err)
	}
}

// downloadParams 汇总下载参数：query + 表单 body（隧道转发走 body）。
func downloadParams(c *httpx.Context) url.Values {
	values := url.Values{}
	if query := c.R.URL.Query(); len(query) > 0 {
		for key, items := range query {
			values[key] = append(values[key], items...)
		}
	}
	if strings.TrimSpace(values.Get("jobId")) == "" && c.R.Body != nil {
		if body, err := io.ReadAll(io.LimitReader(c.R.Body, 1<<20)); err == nil && len(body) > 0 {
			if form, err := url.ParseQuery(string(body)); err == nil {
				for key, items := range form {
					values[key] = append(values[key], items...)
				}
			}
		}
	}
	return values
}

// handleBuildSourceExists 对应 POST /build/source-exists：批量探测节点侧源码目录是否存在，
// 供 Server 修正构建列表的 sourceDirExist（远程构建不能用 Server 本地路径判断）。
func handleBuildSourceExists(c *httpx.Context, manager *builder.Manager) {
	body := struct {
		BuildModelIDs []string `json:"buildModelIds"`
		IDs           []string `json:"ids"`
		BuildModelID  string   `json:"buildModelId"`
	}{}
	_ = c.BindJSON(&body)
	ids := body.BuildModelIDs
	if len(ids) == 0 {
		ids = body.IDs
	}
	if len(ids) == 0 && strings.TrimSpace(body.BuildModelID) != "" {
		ids = []string{body.BuildModelID}
	}
	if len(ids) == 0 {
		ids = strings.Split(c.Form("buildModelIds"), ",")
	}
	c.OK(manager.SourceExists(ids))
}

// buildJobID 读取 jobId：优先 JSON body（Server 用 DataContentType.JSON），退回 form/query。
func buildJobID(c *httpx.Context) string {
	if value := strings.TrimSpace(c.Form("jobId")); value != "" {
		return value
	}
	body := struct {
		JobID string `json:"jobId"`
	}{}
	_ = c.BindJSON(&body)
	return strings.TrimSpace(body.JobID)
}
