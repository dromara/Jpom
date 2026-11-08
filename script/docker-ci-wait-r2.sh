#!/bin/bash
#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED,
# INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

#-----------------------------------------------------------
# 等待发布包在下载中心（Cloudflare R2）就绪
#
# DockerfileRelease/DockerfileBeta 会从 d2.jpom.download 下载对应版本的发布包再构建镜像，
# 因此发版流程中 GitHub Actions 必须等 R2 上传完成（release.sh 的 docker 阶段在 upload 之后）。
#
# 环境变量：
#   CHANNEL        release|beta（必填）
#   VERSION        版本号（必填）
#   IMAGES         server,agent / server / agent（默认 server,agent）
#   WAIT_MINUTES   最长等待分钟数（默认 30；0 表示只检查一次）
#   RETRY_INTERVAL 重试间隔秒数（默认 30）
#   R2_BASE        下载中心地址（默认 https://d2.jpom.download）
#-----------------------------------------------------------

set -euo pipefail

channel="${CHANNEL:?缺少 CHANNEL 环境变量（release|beta）}"
version="${VERSION:?缺少 VERSION 环境变量}"
images="${IMAGES:-server,agent}"
wait_minutes="${WAIT_MINUTES:-30}"
retry_interval="${RETRY_INTERVAL:-30}"
r2_base="${R2_BASE:-https://d2.jpom.download}"

case "$channel" in
release | beta) ;;
*)
	echo "[docker-ci-wait-r2][ERROR] 不支持的 CHANNEL：${channel}" >&2
	exit 1
	;;
esac
[[ "$wait_minutes" =~ ^[0-9]+$ ]] || {
	echo "[docker-ci-wait-r2][ERROR] WAIT_MINUTES 必须是非负整数：${wait_minutes}" >&2
	exit 1
}

log() { echo "[docker-ci-wait-r2] $*"; }
die() {
	echo "[docker-ci-wait-r2][ERROR] $*" >&2
	exit 1
}

has_image() {
	local needle=$1 image
	IFS=',' read -r -a image_list <<<"$images"
	for image in "${image_list[@]}"; do
		if [ "$image" = "$needle" ]; then
			return 0
		fi
	done
	return 1
}

# HEAD 请求校验对象存在且非空
check_package() {
	local url=$1
	curl -fsSIL --max-time 20 --retry 2 --retry-delay 2 "$url" 2>/dev/null |
		tr -d '\r' | awk 'tolower($1)=="content-length:" { if ($2+0 > 0) found=1 } END { exit(found?0:1) }'
}

# 校验 sha1 文件内容形如 40 位十六进制
check_sha1() {
	local url=$1
	curl -fsSL --max-time 20 --retry 2 --retry-delay 2 "$url" 2>/dev/null |
		tr -d ' \t\r\n' | grep -Eq '^[0-9a-fA-F]{40}$'
}

check_once() {
	local image pkg pkg_url sha1_url missing=0
	IFS=',' read -r -a image_list <<<"$images"
	for image in "${image_list[@]}"; do
		case "$image" in
		server | agent) ;;
		*)
			die "不支持的镜像：${image}（可选 server/agent）"
			;;
		esac
		pkg="${image}-${version}-release.tar.gz"
		pkg_url="${r2_base}/${channel}/${version}/${pkg}"
		sha1_url="${pkg_url}.sha1"
		if check_package "$pkg_url"; then
			log "存在：${pkg_url}"
		else
			log "缺失：${pkg_url}"
			missing=1
		fi
		if check_sha1 "$sha1_url"; then
			log "存在：${sha1_url}"
		else
			log "缺失或内容异常：${sha1_url}"
			missing=1
		fi
	done
	return "$missing"
}

start=$(date +%s)
deadline=$((start + wait_minutes * 60))
attempt=1
log "开始等待 R2 发布包：channel=${channel} version=${version} images=${images}（最长 ${wait_minutes} 分钟）"
while :; do
	if check_once; then
		log "发布包已就绪（第 ${attempt} 次检查）"
		exit 0
	fi
	now=$(date +%s)
	if [ "$now" -ge "$deadline" ]; then
		die "等待超时（${wait_minutes} 分钟）：发布包仍未就绪。请确认 release.sh 的 upload 阶段是否完成，以及 d2.jpom.download 路径是否正确：${r2_base}/${channel}/${version}/"
	fi
	log "第 ${attempt} 次检查未就绪，${retry_interval}s 后重试（剩余 $(((deadline - now) / 60 + 1)) 分钟）"
	sleep "$retry_interval"
	attempt=$((attempt + 1))
done
