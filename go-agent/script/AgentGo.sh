#!/usr/bin/env bash
#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#
# description: Jpom Go 插件端服务脚本（与 Java 的 Agent.sh 用法保持一致）
# 用法：AgentGo.sh {start|stop|restart|status|upgrade} [-s]
#   -s  与 Java 脚本兼容的静默模式（输出重定向到 logs/stdout.log）
#
# 安装布局：
#   <base>/bin/agent         Go 二进制
#   <base>/bin/AgentGo.sh    本脚本
#   <base>/conf/application.yml
#   <base>/data logs tmp project-log
#
set -euo pipefail

bin_abs_path=$(cd "$(dirname "$0")" && pwd)
base=$(cd "$bin_abs_path/.." && pwd)
binary="$base/bin/agent"
pidfile="$base/bin/agent.pid"
logs="$base/logs"
stdout="$logs/stdout.log"

mkdir -p "$logs" "$base/data" "$base/tmp" "$base/project-log"

mode="${1:-start}"

running_pid() {
  if [[ -f "$pidfile" ]]; then
    local pid
    pid=$(cat "$pidfile" 2>/dev/null || true)
    if [[ -n "${pid}" ]] && kill -0 "$pid" 2>/dev/null; then
      echo "$pid"
      return 0
    fi
  fi
  return 1
}

check_binary() {
  if [[ ! -x "$binary" ]]; then
    echo "找不到可执行文件: $binary（请确认 Go 插件端已解压到 $base）" >&2
    exit 1
  fi
}

start() {
  check_binary
  local pid
  if pid=$(running_pid); then
    echo "Jpom Go Agent 已在运行 (pid=$pid)"
    return 0
  fi
  nohup "$binary" -base "$base" >>"$stdout" 2>&1 </dev/null &
  echo $! >"$pidfile"
  sleep 0.5
  if pid=$(running_pid); then
    echo "Jpom Go Agent 启动成功 (pid=$pid)，日志: $stdout"
  else
    echo "Jpom Go Agent 启动失败，请查看 $stdout" >&2
    exit 1
  fi
}

stop() {
  local pid
  if ! pid=$(running_pid); then
    rm -f "$pidfile"
    echo "Jpom Go Agent 未运行"
    return 0
  fi
  kill "$pid" 2>/dev/null || true
  for _ in $(seq 1 100); do
    if ! kill -0 "$pid" 2>/dev/null; then
      break
    fi
    sleep 0.1
  done
  if kill -0 "$pid" 2>/dev/null; then
    echo "进程未在 10 秒内退出，强制结束 (pid=$pid)" >&2
    kill -9 "$pid" 2>/dev/null || true
  fi
  rm -f "$pidfile"
  echo "Jpom Go Agent 已停止"
}

status() {
  local pid
  if pid=$(running_pid); then
    echo "Jpom Go Agent 正在运行 (pid=$pid)"
    exit 0
  fi
  echo "Jpom Go Agent 未运行"
  exit 1
}

case "$mode" in
  start) start ;;
  stop) stop ;;
  restart) stop; start ;;
  status) status ;;
  upgrade) stop; start ;;
  *)
    echo "用法: $0 {start|stop|restart|status|upgrade} [-s]" >&2
    exit 2
    ;;
esac
