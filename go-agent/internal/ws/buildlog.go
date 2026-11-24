//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package ws

import (
	"strconv"
	"sync"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/builder"
	"github.com/dromara/Jpom/go-agent/internal/httpx"
	"github.com/dromara/Jpom/go-agent/internal/i18n"
)

// buildLogPingInterval 心跳间隔（PROTOCOL §3：每 10s 一次，Server 20s 无帧转 /build/status）。
const buildLogPingInterval = 10 * time.Second

// HandleBuildLog 暴露 WS /build-log?jobId=..&fromSeq=..（PROTOCOL §3）。
//
// 约定与 agent 既有 WS 一致：Server 是 websocket 客户端，握手鉴权走查询参数
// （Jpom-Agent-Authorize），因此注册为 public 路由。这是**只读流**，收到的文本帧一律忽略
// （只用于探测对端关闭）。
func HandleBuildLog(a *app.App, builds *builder.Manager) httpx.Handler {
	return func(c *httpx.Context) {
		session, err := upgrade(c)
		if err != nil {
			return
		}
		lang := query(c.R, "lang")
		if lang == "" && a.Config != nil {
			lang = a.Config.Jpom.System.Lang
		}
		if !a.CheckAuthorize(query(c.R, authorizeParam)) {
			a.Log().Warn("build log websocket authorization failed", "remote", c.R.RemoteAddr)
			session.CloseWithReason(closeCannotAccept, i18n.T(lang, "i18n.auth_info_error.c184"))
			return
		}
		jobID := query(c.R, "jobId")
		fromSeq, _ := strconv.Atoi(query(c.R, "fromSeq"))
		backlog, live, cancel, err := builds.Subscribe(jobID, fromSeq)
		if err != nil {
			// 未知 job：按 lost 终态收尾，Server 直接把构建判失败。
			_ = session.SendJSON(builder.Event{
				Type:    builder.TypeExit,
				State:   builder.StateLost,
				Message: i18n.T(lang, "i18n.task_not_exist.47e9"),
			})
			session.Close()
			return
		}
		defer cancel()

		exited := false
		for _, event := range backlog {
			if sendErr := session.SendJSON(event); sendErr != nil {
				return
			}
			if event.Type == builder.TypeExit {
				exited = true
			}
		}
		if exited {
			session.Close()
			return
		}

		done := make(chan struct{})
		var once sync.Once
		stop := func() { once.Do(func() { close(done) }) }
		defer stop()
		go func() {
			for {
				if _, _, readErr := session.Read(); readErr != nil {
					stop()
					return
				}
			}
		}()

		ticker := time.NewTicker(buildLogPingInterval)
		defer ticker.Stop()
		for {
			select {
			case event, ok := <-live:
				if !ok {
					return
				}
				if sendErr := session.SendJSON(event); sendErr != nil {
					return
				}
				if event.Type == builder.TypeExit {
					session.Close()
					return
				}
			case <-ticker.C:
				if sendErr := session.SendJSON(builder.Event{Type: builder.TypePing}); sendErr != nil {
					return
				}
			case <-done:
				return
			}
		}
	}
}
