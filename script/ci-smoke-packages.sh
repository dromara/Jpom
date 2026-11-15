#!/bin/bash
#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

#-----------------------------------------------------------
# Jpom 发布包启动冒烟（GitHub Actions release-package.yml 调用，也可本地运行）
#
# 目的：把“下载产物后解压启动校验”从本机迁到 CI——直接解压构建好的 server/agent
#       tar.gz，用临时目录作为 jpom.path、用高位端口启动，轮询 HTTP 接口确认可启动、
#       可访问；随后停止进程并清理临时目录，不污染工作区；失败时打印并保留日志。
#
# 用法：
#   bash script/ci-smoke-packages.sh --parent-dir . --version 2.11.14
#   bash script/ci-smoke-packages.sh --parent-dir . --version 2.11.14 --keep --work-dir .tmp/ci-smoke
#
# 参数：
#   --parent-dir DIR        jpom-parent 仓库根（默认：脚本上级目录；传入含 jpom-parent/ 的工作区根时自动下钻）
#   --version V             目标版本（默认读取 pom.xml）
#   --server-port PORT      服务端端口（默认 21222）
#   --agent-port PORT       插件端端口（默认 21223）
#   --timeout SECONDS       单端 HTTP 就绪超时（默认 180）
#   --java PATH             java 可执行文件（默认 $JAVA；也为空时用 PATH 中的 java）
#   --work-dir DIR          解压/数据临时目录（默认 mktemp -d；--keep 时建议显式指定）
#   --log-dir DIR           失败时复制日志的目录（默认 ${TMPDIR:-/tmp}/jpom-ci-smoke-logs）
#   --skip-server           跳过服务端
#   --skip-agent            跳过插件端
#   --keep                  结束后保留临时目录与数据（排障用，CI 不要开）
#   -h | --help             显示用法
#
# 环境变量：
#   JAVA                    java 路径（bin 脚本原生支持）
#   USR_JVM_SIZE            覆盖 bin 脚本默认 JVM 大小（默认 -Xms128m -Xmx512m，加快 CI 启动）
#   TMPDIR                  临时目录根（默认 /tmp）
#
# 说明：
#   - 依赖 tar、curl、java；只解压 tar.gz（tar.gz 为默认发布格式，过渡期 zip 不参与冒烟）
#   - 启动命令：bin/<Kind>.sh start -s --server.port=<port> --jpom.path=<临时数据目录>
#     -s 让 bin 脚本启动后立即返回（否则会 tail -f 阻塞）；jpom.path 指向临时目录，避免写入仓库
#   - 停止时按 bin/<kind>.pid 记录的进程号 kill，不用 bin 脚本的 stop，避免误杀同机其它 Jpom 实例
#   - 冒烟内容：服务端 /check-system、首页 HTML、/rand-code（登录验证码接口）；插件端首页
#   - 退出码：0 通过；非 0 失败（失败会打印两端日志并复制到 --log-dir）
#-----------------------------------------------------------

set -euo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

parent_dir=""
version=""
server_port=21222
agent_port=21223
http_timeout=180
java_bin="${JAVA:-}"
work_dir=""
log_dir="${TMPDIR:-/tmp}/jpom-ci-smoke-logs"
skip_server=0
skip_agent=0
keep=0

log() { echo "[ci-smoke] $*"; }
warn() { echo "[ci-smoke][WARN] $*" >&2; }
die() {
	echo "[ci-smoke][ERROR] $*" >&2
	exit 1
}

usage() {
	awk '/^# 用法：/{f=1} f{print} f&&/^#----/{exit}' "${BASH_SOURCE[0]}" | awk '{sub(/^# ?/, ""); print}'
}

while [ $# -gt 0 ]; do
	case "$1" in
	--parent-dir)
		parent_dir="${2:?--parent-dir 需要参数}"
		shift 2
		;;
	--version)
		version="${2:?--version 需要参数}"
		shift 2
		;;
	--server-port)
		server_port="${2:?--server-port 需要参数}"
		shift 2
		;;
	--agent-port)
		agent_port="${2:?--agent-port 需要参数}"
		shift 2
		;;
	--timeout)
		http_timeout="${2:?--timeout 需要参数}"
		shift 2
		;;
	--java)
		java_bin="${2:?--java 需要参数}"
		shift 2
		;;
	--work-dir)
		work_dir="${2:?--work-dir 需要参数}"
		shift 2
		;;
	--log-dir)
		log_dir="${2:?--log-dir 需要参数}"
		shift 2
		;;
	--skip-server)
		skip_server=1
		shift
		;;
	--skip-agent)
		skip_agent=1
		shift
		;;
	--keep)
		keep=1
		shift
		;;
	-h | --help)
		usage
		exit 0
		;;
	*)
		die "未知参数：$1（--help 查看用法）"
		;;
	esac
done

parent_dir="${parent_dir:-${script_dir}/..}"
[ -d "$parent_dir" ] || die "parent-dir 不存在：$parent_dir"
parent_dir=$(cd "$parent_dir" && pwd)
# 兼容“工作区根（含 jpom-parent/ 子目录）”与“jpom-parent 仓库根”两种布局
if [ -d "${parent_dir}/jpom-parent/modules" ]; then
	parent_dir="${parent_dir}/jpom-parent"
fi
[ -d "${parent_dir}/modules" ] || die "不是 jpom-parent 仓库根（缺少 modules/）：$parent_dir"

if [ -z "$version" ]; then
	command -v python3 >/dev/null 2>&1 || die "未指定 --version 且没有 python3，无法读取 pom 版本"
	version=$(python3 - "${parent_dir}/pom.xml" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
ns = "{http://maven.apache.org/POM/4.0.0}"
node = root.find(ns + "version")
print((node.text or "").strip() if node is not None else "")
PY
	)
fi
[ -n "$version" ] || die "无法确定版本号，请指定 --version"

for tool in tar curl; do
	command -v "$tool" >/dev/null 2>&1 || die "缺少命令：$tool"
done
if [ -z "$java_bin" ]; then
	java_bin=$(command -v java || true)
fi
[ -n "$java_bin" ] && [ -x "$java_bin" ] || die "未找到可执行的 java（可用 --java 或 JAVA 指定；CI 使用 JDK8）"
export JAVA="$java_bin"
# bin 脚本支持 USR_JVM_SIZE；CI 用小堆加快启动（默认是 -Xms1g -Xmx2g）
if [ -z "${USR_JVM_SIZE:-}" ]; then
	USR_JVM_SIZE="-Xms128m -Xmx512m"
fi
export USR_JVM_SIZE

if [ -z "$work_dir" ]; then
	work_dir=$(mktemp -d "${TMPDIR:-/tmp}/jpom-ci-smoke.XXXXXX")
else
	mkdir -p "$work_dir"
	work_dir=$(cd "$work_dir" && pwd)
fi
mkdir -p "$log_dir"
log "版本：${version}  工作目录：${work_dir}  java：${java_bin}"

run_id="$(date +%Y%m%d-%H%M%S)-$$"
server_root=""
agent_root=""
server_pid=""
agent_pid=""
started_server=0
started_agent=0

sha1_of() {
	if command -v sha1sum >/dev/null 2>&1; then
		sha1sum "$1" | awk '{print $1}'
	else
		shasum -a 1 "$1" | awk '{print $1}'
	fi
}

verify_pkg() {
	local pkg=$1 expected actual
	[ -s "$pkg" ] || die "缺少发布包：$pkg（请先构建 tar.gz）"
	if [ -s "${pkg}.sha1" ]; then
		expected=$(tr -d ' \r\n' <"${pkg}.sha1")
		actual=$(sha1_of "$pkg")
		[ "$actual" = "$expected" ] || die "sha1 不一致：${pkg}（期望 ${expected}，实际 ${actual}）"
		log "OK  sha1 校验通过：$(basename "$pkg")"
	fi
}

# 解压并输出运行根目录（tar.gz 根通常直接是 bin/conf/lib；兼容带顶层目录的包）
extract_pkg() {
	local pkg=$1 dest=$2 script_name=$3 found
	mkdir -p "$dest"
	tar -xzf "$pkg" -C "$dest" || die "解压失败：$pkg"
	if [ -f "${dest}/bin/${script_name}" ]; then
		echo "$dest"
		return 0
	fi
	found=$(find "$dest" -maxdepth 3 -type f -name "$script_name" -path '*/bin/*' 2>/dev/null | head -n 1)
	[ -n "$found" ] || die "解压后未找到 bin/${script_name}：$pkg"
	echo "$(cd "$(dirname "$found")/.." && pwd)"
}

port_in_use() {
	local port=$1 rc=0
	curl -s -o /dev/null -m 1 "http://127.0.0.1:${port}/" >/dev/null 2>&1 || rc=$?
	# curl 退出码 7 = 连接被拒绝（端口空闲）；其它（有 HTTP 响应/超时）按占用处理
	[ "$rc" -ne 7 ]
}

read_pid() {
	local pidfile=$1 i
	for i in $(seq 1 20); do
		if [ -s "$pidfile" ]; then
			cat "$pidfile"
			return 0
		fi
		sleep 0.2
	done
	return 1
}

stop_pid() {
	local name=$1 pid=$2 i
	[ -n "$pid" ] || return 0
	kill -0 "$pid" 2>/dev/null || {
		log "${name} 已退出"
		return 0
	}
	log "停止 ${name}（pid=${pid}）"
	kill "$pid" 2>/dev/null || true
	for i in $(seq 1 30); do
		kill -0 "$pid" 2>/dev/null || {
			log "${name} 已停止"
			return 0
		}
		sleep 1
	done
	warn "${name} 未在 30s 内退出，强制 kill -9"
	kill -9 "$pid" 2>/dev/null || true
	return 0
}

wait_http() {
	local name=$1 url=$2 pattern=$3 pid=$4
	local deadline body_file code
	deadline=$(( $(date +%s) + http_timeout ))
	body_file="${work_dir}/http-${name}.txt"
	: >"$body_file"
	while [ "$(date +%s)" -lt "$deadline" ]; do
		if ! kill -0 "$pid" 2>/dev/null; then
			echo "[ci-smoke][ERROR] 进程已退出（${name}）" >&2
			return 1
		fi
		code=$(curl -s -o "$body_file" -w '%{http_code}' -m 5 "$url" 2>/dev/null || true)
		if [ "$code" = "200" ] && grep -qE "$pattern" "$body_file" 2>/dev/null; then
			return 0
		fi
		sleep 2
	done
	return 1
}

dump_logs() {
	local kind root f
	log_dir="${log_dir}/${run_id}"
	mkdir -p "$log_dir"
	for kind in server agent; do
		if [ "$kind" = "server" ]; then
			root="$server_root"
		else
			root="$agent_root"
		fi
		[ -n "$root" ] || continue
		[ -d "${root}/logs" ] || continue
		cp -a "${root}/logs" "${log_dir}/${kind}-logs" 2>/dev/null || true
		log "---- ${kind} 日志（${root}/logs）----"
		for f in "${root}/logs"/*.log "${work_dir}/${kind}-start.log"; do
			[ -f "$f" ] || continue
			echo "" >&2
			echo "### $f" >&2
			tail -n 80 "$f" >&2 || true
		done
	done
	log "日志目录：${log_dir}"
}

cleanup() {
	local status=$?
	trap - EXIT
	if [ "$started_agent" -eq 1 ]; then
		stop_pid "agent" "$agent_pid"
	fi
	if [ "$started_server" -eq 1 ]; then
		stop_pid "server" "$server_pid"
	fi
	if [ "$status" -ne 0 ]; then
		dump_logs
	fi
	if [ "$keep" -eq 1 ]; then
		log "已保留临时目录：${work_dir}"
	else
		rm -rf "$work_dir"
	fi
	exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT TERM

start_component() {
	local kind=$1 root=$2 port=$3 data_dir=$4 script_name bin_pidfile smoke_pidfile pid start_log
	if [ "$kind" = "server" ]; then
		script_name="Server.sh"
	else
		script_name="Agent.sh"
	fi
	if port_in_use "$port"; then
		die "端口 ${port} 已被占用，请用 --server-port/--agent-port 指定空闲端口"
	fi
	log "启动 ${kind}：端口 ${port}，数据目录 ${data_dir}"
	start_log="${work_dir}/${kind}-start.log"
	if ! (
		cd "$root" && bash "bin/${script_name}" start -s "--server.port=${port}" "--jpom.path=${data_dir}"
	) >"$start_log" 2>&1; then
		cat "$start_log" >&2 || true
		die "${kind} 启动脚本返回失败（bin/${script_name} start）"
	fi
	bin_pidfile="${root}/bin/${kind}.pid"
	pid=$(read_pid "$bin_pidfile") || die "${kind} 未生成 pid 文件：${bin_pidfile}"
	case "$pid" in
	'' | *[!0-9]*)
		die "${kind} 的 pid 文件内容非法：${pid}"
		;;
	esac
	smoke_pidfile="${work_dir}/${kind}.smoke.pid"
	echo "$pid" >"$smoke_pidfile"
	log "${kind} 已启动：pid=${pid}"
}

if [ "$skip_server" -eq 0 ]; then
	server_pkg="${parent_dir}/modules/server/target/server-${version}-release.tar.gz"
	verify_pkg "$server_pkg"
	server_root=$(extract_pkg "$server_pkg" "${work_dir}/server" "Server.sh")
	mkdir -p "${work_dir}/data-server"
	start_component "server" "$server_root" "$server_port" "${work_dir}/data-server"
	server_pid=$(cat "${work_dir}/server.smoke.pid")
	started_server=1
	if wait_http "server-check-system" "http://127.0.0.1:${server_port}/check-system" '"code"' "$server_pid"; then
		log "OK server /check-system 可访问"
	else
		die "server /check-system 未在 ${http_timeout}s 内就绪"
	fi
	if wait_http "server-index" "http://127.0.0.1:${server_port}/" 'id="app"' "$server_pid"; then
		log "OK server 首页可访问（含前端挂载点 id=app）"
	else
		die "server 首页未在 ${http_timeout}s 内就绪（前端产物可能缺失）"
	fi
	if wait_http "server-rand-code" "http://127.0.0.1:${server_port}/rand-code" '"code"' "$server_pid"; then
		log "OK server /rand-code 可访问（登录验证码接口）"
	else
		die "server /rand-code 未在 ${http_timeout}s 内就绪"
	fi
fi

if [ "$skip_agent" -eq 0 ]; then
	agent_pkg="${parent_dir}/modules/agent/target/agent-${version}-release.tar.gz"
	verify_pkg "$agent_pkg"
	agent_root=$(extract_pkg "$agent_pkg" "${work_dir}/agent" "Agent.sh")
	mkdir -p "${work_dir}/data-agent"
	start_component "agent" "$agent_root" "$agent_port" "${work_dir}/data-agent"
	agent_pid=$(cat "${work_dir}/agent.smoke.pid")
	started_agent=1
	if wait_http "agent-index" "http://127.0.0.1:${agent_port}/" 'Jpom-Agent' "$agent_pid"; then
		log "OK agent 首页可访问（返回 Jpom-Agent 提示）"
	else
		die "agent 首页未在 ${http_timeout}s 内就绪"
	fi
fi

log "冒烟通过：server=${server_port} agent=${agent_port}（进程将在退出时停止）"
