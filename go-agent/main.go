/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package main

import (
	"context"
	"flag"
	"fmt"
	"net"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/api"
	"github.com/dromara/Jpom/go-agent/internal/app"
)

var (
	// version is injected at build time (-X main.version=2.x.y).
	version = "dev"
	// buildTime is injected at build time.
	buildTime = ""
)

func main() {
	var (
		confPath    string
		baseDir     string
		showVersion bool
	)
	flag.StringVar(&confPath, "conf", "", "path to application.yml (default: <base>/conf/application.yml)")
	flag.StringVar(&baseDir, "base", "", "base directory (default: auto detect from executable, or jpom.path)")
	flag.BoolVar(&showVersion, "version", false, "print version and exit")
	flag.Parse()

	if showVersion {
		fmt.Println(version)
		return
	}

	application, err := app.New(confPath, baseDir, version, buildTime)
	if err != nil {
		fmt.Fprintf(os.Stderr, "jpom agent start failed: %v\n", err)
		os.Exit(1)
	}
	defer application.Close()

	handler := api.RegisterAll(application)
	srv := &http.Server{
		Addr:              application.ListenAddr(),
		Handler:           handler,
		ReadHeaderTimeout: 30 * time.Second,
	}

	// 先成功监听再写 PID 文件（CONTRACT §4）：启停脚本以 bin/agent.pid 为准，
	// 只有服务真正可接受连接时才认为启动成功。
	listener, err := net.Listen("tcp", application.ListenAddr())
	if err != nil {
		application.Log().Error("jpom go agent listen failed", "listen", application.ListenAddr(), "error", err)
		os.Exit(1)
	}
	pidWritten := false
	if err := application.WritePidFile(); err != nil {
		application.Log().Warn("write pid file failed", "path", application.PidFilePath(), "error", err)
	} else {
		pidWritten = true
		application.Log().Info("pid file written", "path", application.PidFilePath(), "pid", application.Pid)
	}
	// 正常退出（SIGINT/SIGTERM/服务错误）时删除 PID 文件；自升级重启走
	// syscall.Exec（Unix）或 os.Exit(0)（Windows），不会执行该 defer。
	if pidWritten {
		defer application.RemovePidFile()
	}

	application.Log().Info("jpom go agent starting",
		"version", version, "base", application.BaseDir, "listen", application.ListenAddr())

	errCh := make(chan error, 1)
	go func() {
		if err := srv.Serve(listener); err != nil && err != http.ErrServerClosed {
			errCh <- err
		}
	}()

	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
	select {
	case sig := <-sigCh:
		application.Log().Info("shutdown signal received", "signal", sig.String())
	case err := <-errCh:
		application.Log().Error("http server exited", "error", err)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		application.Log().Error("http server shutdown", "error", err)
	}
}
