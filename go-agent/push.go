//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package main

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/config"
	"github.com/dromara/Jpom/go-agent/internal/jpomheader"
	"github.com/dromara/Jpom/go-agent/internal/netx"
	"github.com/dromara/Jpom/go-agent/internal/tunnel"
)

// statEnvelope is the agent JSON envelope ({code,msg,data}) returned by the
// local /get-stat-info endpoint.
type statEnvelope struct {
	Code int             `json:"code"`
	Msg  string          `json:"msg"`
	Data json.RawMessage `json:"data"`
}

// runPushLoop periodically collects the local node status and pushes it over the
// reverse tunnel (PUSH frame), so the server does not have to poll this agent.
//
// It reuses the existing /get-stat-info handler through a loopback HTTP call so
// the pushed payload is byte-for-byte the same structure the pull path returns.
func runPushLoop(ctx context.Context, application *app.App, client *tunnel.Client, settings config.Tunnel) {
	if !settings.PushOn() {
		application.Log().Info("reverse tunnel push disabled", "config", "jpom.tunnel.push-enabled=false")
		return
	}
	interval := time.Duration(settings.PushIntervalSeconds()) * time.Second
	statURL := "http://" + tunnel.LoopbackAddr(application.ListenAddr()) + "/get-stat-info"
	authorize := application.Auth.Authorize
	httpClient := netx.Client(application.UserAgent(), 30*time.Second)
	ticker := time.NewTicker(interval)
	defer ticker.Stop()
	var seq int64
	unsupportedLogged := false
	// 预热：启动时先异步采集一次，把指标库（CPU/内存）初始化成本提前付掉，
	// 否则「隧道刚连上」那一次采集可能要几十秒，节点在服务端会短暂显示未上报。
	go func() {
		if _, err := collectStatInfo(httpClient, statURL, authorize); err != nil {
			application.Log().Debug("warm up node stat collection failed", "error", err)
		}
	}()
	for {
		if client.Connected() && client.PushSupported() {
			unsupportedLogged = false
			seq++
			body, err := collectStatInfo(httpClient, statURL, authorize)
			if err != nil {
				application.Log().Warn("collect node stat for push failed", "error", err)
			} else if err := client.SendPush(tunnel.TopicNodeStat, seq, body); err != nil {
				application.Log().Warn("push node stat failed", "error", err)
			}
		} else if client.Connected() && !client.PushSupported() && !unsupportedLogged {
			// 老服务端不认识 PUSH 帧（收到会按非法帧断开），只在首次提示一次
			application.Log().Info("server does not advertise push support, node status push skipped",
				"hint", "upgrade server to enable push")
			unsupportedLogged = true
		}
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
		}
	}
}

func collectStatInfo(client *http.Client, statURL, authorize string) ([]byte, error) {
	request, err := http.NewRequest(http.MethodPost, statURL, nil)
	if err != nil {
		return nil, err
	}
	if authorize != "" {
		request.Header.Set(jpomheader.Authorize, authorize)
	}
	response, err := client.Do(request)
	if err != nil {
		return nil, err
	}
	defer func() {
		_ = response.Body.Close()
	}()
	raw, err := io.ReadAll(io.LimitReader(response.Body, 4<<20))
	if err != nil {
		return nil, err
	}
	var envelope statEnvelope
	if err := json.Unmarshal(raw, &envelope); err != nil {
		return nil, fmt.Errorf("decode stat info: %w", err)
	}
	if envelope.Code != 200 {
		return nil, fmt.Errorf("stat info returned code=%d msg=%s", envelope.Code, envelope.Msg)
	}
	if len(envelope.Data) == 0 {
		return nil, fmt.Errorf("stat info data is empty")
	}
	return envelope.Data, nil
}
