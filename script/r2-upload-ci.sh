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
# GitHub Actions 专用：把已构建的 Jpom 发布包上传到 Cloudflare R2 并回验。
#
# 与根仓 script/deploy-release-r2.sh 的区别：
#   - 本脚本在 jpom-parent 仓库内运行（CI checkout 的是 jpom-parent），路径以本仓库根为基准
#   - rclone 通过环境变量配置 R2 S3（RCLONE_CONFIG_R2_*），不依赖 ~/.config/rclone/rclone.conf
#   - 支持 --prefix（POC/灰度前缀，避免写正式 release|beta 目录）
#
# 用法：
#   bash script/r2-upload-ci.sh --channel release --version 2.11.14 [--prefix ci-test] [--deep-verify] [--dry-run]
#
# 环境变量：
#   R2_BUCKET         默认 jpom-releases-apac
#   R2_PUBLIC_BASE    回验基址，默认 R2 r2.dev 公开域名（d2 对 GitHub runner 有 Cloudflare challenge）
#   R2_REMOTE         rclone remote 名称，默认 r2（由 RCLONE_CONFIG_R2_* 环境变量提供）
#   R2_CHUNK_SIZE / R2_UPLOAD_CONCURRENCY / R2_TRANSFERS  上传调优
#-----------------------------------------------------------

set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
repo_dir=$(cd "${script_dir}/.." && pwd)

channel=""
version=""
prefix=""
deep_verify=0
skip_verify=0
dry_run=0

R2_REMOTE="${R2_REMOTE:-r2}"
R2_BUCKET="${R2_BUCKET:-jpom-releases-apac}"
R2_PUBLIC_BASE="${R2_PUBLIC_BASE:-https://pub-a46506c7e8ff43fabc4c1ce8e5faffcb.r2.dev}"
R2_PUBLIC_BASE="${R2_PUBLIC_BASE%/}"
R2_CHUNK_SIZE="${R2_CHUNK_SIZE:-16M}"
R2_UPLOAD_CONCURRENCY="${R2_UPLOAD_CONCURRENCY:-8}"
R2_TRANSFERS="${R2_TRANSFERS:-4}"

log() { echo "[r2-upload-ci] $*"; }
die() {
	echo "[r2-upload-ci][ERROR] $*" >&2
	exit 1
}

while [ $# -gt 0 ]; do
	case "$1" in
	--channel)
		channel="${2:?}"
		shift 2
		;;
	--version)
		version="${2:?}"
		shift 2
		;;
	--prefix)
		prefix="${2:?}"
		shift 2
		;;
	--repo-dir)
		repo_dir="${2:?}"
		shift 2
		;;
	--deep-verify)
		deep_verify=1
		shift
		;;
	--skip-verify)
		skip_verify=1
		shift
		;;
	--dry-run)
		dry_run=1
		shift
		;;
	-h | --help)
		sed -n '/^# 用法：/,/^#----/p' "$0" | sed 's/^# \{0,1\}//'
		exit 0
		;;
	*)
		die "不支持的参数 $1"
		;;
	esac
done

[ -n "$channel" ] || die "缺少 --channel（release|beta）"
[ -n "$version" ] || die "缺少 --version"
case "$channel" in
release) [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "release 版本号格式不正确：${version}" ;;
beta) [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "beta 版本号格式不正确：${version}" ;;
*) die "不支持的 channel：${channel}" ;;
esac
repo_dir=$(cd "$repo_dir" && pwd)
prefix="${prefix#/}"
prefix="${prefix%/}"
if [ -n "$prefix" ]; then
	[[ "$prefix" =~ ^[A-Za-z0-9._/-]+$ ]] || die "非法的 --prefix：${prefix}"
	case "${prefix%%/*}" in
	release | beta) die "--prefix 不能以 release/beta 开头（会写入正式目录）：${prefix}" ;;
	esac
fi

modules_dir="${repo_dir}/modules"
sources=()
for kind in agent server; do
	for ext in tar.gz zip; do
		sources+=("${modules_dir}/${kind}/target/${kind}-${version}-release.${ext}")
		sources+=("${modules_dir}/${kind}/target/${kind}-${version}-release.${ext}.sha1")
	done
done

remote_rel="${prefix:+${prefix}/}${channel}/${version}"
remote_dir="${R2_REMOTE}:${R2_BUCKET}/${remote_rel}/"
public_url="${R2_PUBLIC_BASE}/${remote_rel}"

log "渠道：${channel}  版本：${version}  目标：${remote_dir}  回验：${public_url}/"
for src in "${sources[@]}"; do
	[ -s "$src" ] || die "缺少产物：${src}"
done

if [ "$dry_run" = "0" ]; then
	command -v rclone >/dev/null 2>&1 || die "缺少 rclone"
	command -v zip >/dev/null 2>&1 || die "缺少 zip"
	command -v curl >/dev/null 2>&1 || die "缺少 curl"
fi

sha1_of() {
	if command -v sha1sum >/dev/null 2>&1; then
		sha1sum "$1" | awk '{print $1}'
	else
		shasum -a 1 "$1" | awk '{print $1}'
	fi
}
file_size() { stat -c '%s' "$1" 2>/dev/null || stat -f '%z' "$1" 2>/dev/null; }

stage_dir=$(mktemp -d)
trap 'rm -rf "${stage_dir}"' EXIT
merged_zip="${stage_dir}/jpom-${version}.zip"
log "生成合并包 ${merged_zip##*/}"
zip -j -q "$merged_zip" \
	"${modules_dir}/server/target/server-${version}-release.zip" \
	"${modules_dir}/agent/target/agent-${version}-release.zip"

upload_file() {
	local src=$1 name dest
	name=$(basename "$src")
	dest="${remote_dir}${name}"
	if [ "$dry_run" = "1" ]; then
		log "(dry-run) rclone copyto ${src} ${dest}"
		return 0
	fi
	local started_at elapsed
	started_at=$(date +%s)
	log "上传 ${name}（$(file_size "$src") bytes）"
	rclone copyto "$src" "$dest" \
		--retries 3 --low-level-retries 10 \
		--s3-chunk-size "$R2_CHUNK_SIZE" \
		--s3-upload-concurrency "$R2_UPLOAD_CONCURRENCY" \
		--transfers "$R2_TRANSFERS" \
		--stats 15s --stats-one-line
	elapsed=$(( $(date +%s) - started_at ))
	[ "$elapsed" -gt 0 ] || elapsed=1
	log "  耗时 ${elapsed}s"
}

for src in "${sources[@]}"; do
	upload_file "$src"
done
upload_file "$merged_zip"

if [ "$dry_run" = "1" ]; then
	log "dry-run 结束（未上传）"
	exit 0
fi

if [ "$skip_verify" = "1" ]; then
	log "已指定 --skip-verify，跳过回验"
	exit 0
fi

log "开始回验（${public_url}/）"
fail=0
uploaded=("${sources[@]}" "$merged_zip")
for src in "${uploaded[@]}"; do
	name=$(basename "$src")
	local_size=$(file_size "$src")
	remote_size=$(rclone lsl "${remote_dir}${name}" 2>/dev/null | awk 'NR==1 {print $1}' || true)
	if [ "$local_size" = "$remote_size" ]; then
		log "OK   ${name}  size=${local_size}"
	else
		log "FAIL ${name}  local_size=${local_size} remote_size=${remote_size:-<无>}"
		fail=1
	fi
done
cache_buster="verify=$(date +%s)-${RANDOM}"
for src in "${sources[@]}"; do
	case "$src" in
	*.sha1) continue ;;
	esac
	name=$(basename "$src")
	local_sha1=$(sha1_of "$src")
	remote_sha1=$(curl -fsSL --retry 3 --retry-delay 2 "${public_url}/${name}.sha1?${cache_buster}" 2>/dev/null | tr -d ' \r\n' || true)
	if [ "$local_sha1" = "$remote_sha1" ]; then
		log "OK   ${name}  sha1=${local_sha1}"
	else
		log "FAIL ${name}  local=${local_sha1} remote=${remote_sha1:-<无法获取>}"
		fail=1
	fi
done
if [ "$deep_verify" = "1" ]; then
	for src in "${uploaded[@]}"; do
		name=$(basename "$src")
		deep_file="${stage_dir}/deep-${name}"
		log "deep-verify 下载 ${name}"
		if ! curl -fsSL --retry 3 --retry-delay 2 -o "$deep_file" "${public_url}/${name}?${cache_buster}"; then
			log "FAIL ${name}  deep-verify 下载失败"
			fail=1
			continue
		fi
		if [ "$(sha1_of "$src")" = "$(sha1_of "$deep_file")" ]; then
			log "OK   ${name}  deep-verify sha1 一致"
		else
			log "FAIL ${name}  deep-verify sha1 不一致"
			fail=1
		fi
	done
fi
[ "$fail" = "0" ] || die "R2 上传回验失败"
log "上传并回验完成：${public_url}/"
