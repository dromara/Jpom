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
# 解析 R2 打包发布元数据（GitHub Actions release-package 工作流调用，也可本地调试）
#
# 输出（写入 $GITHUB_OUTPUT；未设置时打印到 stdout）：
#   channel / version / target_prefix / remote_rel / public_base
#   server_pkg / agent_pkg / merged_pkg
#   dry_run / deep_verify
#
# 环境变量：
#   INPUT_CHANNEL        auto|release|beta（默认 auto）
#   INPUT_VERSION        版本号（空则自动推导）
#   INPUT_TARGET_PREFIX  目标路径前缀（POC/灰度用，如 ci-test；空=正式 release|beta 目录）
#   INPUT_DRY_RUN        true|false（默认 false；只构建不上传）
#   INPUT_DEEP_VERIFY    true|false（默认 false）
#   GITHUB_EVENT_NAME    workflow_dispatch|push
#   GITHUB_REF           refs/tags/v2.11.13 / refs/heads/dev
#   GITHUB_REF_NAME      v2.11.13 / dev
#   GITHUB_OUTPUT        输出文件（GitHub Actions 提供）
#   JPOM_DOWNLOAD_BASE   下载/回验基址（默认 R2 r2.dev 公开域名）
#   REPO_DIR             仓库根目录（默认脚本上级目录）
#-----------------------------------------------------------

set -euo pipefail

repo_dir="${REPO_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
# 兼容两种布局：工作区根（含 jpom-parent/ 子目录，如本地调试）与 jpom-parent 仓库根（如 GitHub Actions checkout）
if [ -d "${repo_dir}/jpom-parent" ]; then
	parent_dir="${repo_dir}/jpom-parent"
else
	parent_dir="${repo_dir}"
fi
event="${GITHUB_EVENT_NAME:-workflow_dispatch}"
ref="${GITHUB_REF:-refs/heads/dev}"
ref_name="${GITHUB_REF_NAME:-${ref#refs/}}"
ref_name="${ref_name#refs/}"

channel_input="${INPUT_CHANNEL:-auto}"
version_input="${INPUT_VERSION:-}"
target_prefix="${INPUT_TARGET_PREFIX:-}"
dry_run="${INPUT_DRY_RUN:-false}"
deep_verify="${INPUT_DEEP_VERIFY:-false}"
public_base="${JPOM_DOWNLOAD_BASE:-https://pub-a46506c7e8ff43fabc4c1ce8e5faffcb.r2.dev}"
public_base="${public_base%/}"

log() { echo "[release-ci-meta] $*" >&2; }
die() {
	echo "[release-ci-meta][ERROR] $*" >&2
	exit 1
}

emit() {
	if [ -n "${GITHUB_OUTPUT:-}" ]; then
		printf '%s=%s\n' "$1" "$2" >>"$GITHUB_OUTPUT"
	else
		printf '%s=%s\n' "$1" "$2"
	fi
}

# 1. 渠道
channel="$channel_input"
if [ "$channel" = "auto" ]; then
	case "$ref" in
	refs/tags/v*) channel="release" ;;
	refs/heads/beta) channel="beta" ;;
	*) channel="release" ;;
	esac
fi
case "$channel" in
release | beta) ;;
*) die "不支持的 channel：${channel}（可选 auto/release/beta）" ;;
esac

# 2. 版本
version="$version_input"
if [ -z "$version" ]; then
	case "$ref" in
	refs/tags/*) version="${ref_name#v}" ;;
	*)
		tag_file="${parent_dir}/script/tag.${channel}.txt"
		[ -f "$tag_file" ] || die "缺少版本号来源文件：${tag_file}"
		version=$(tr -d ' \t\r\n' <"$tag_file")
		;;
	esac
fi
[ -n "$version" ] || die "无法推导版本号，请通过 version 输入或 tag 文件指定"
case "$channel" in
release)
	[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "release 版本号格式不正确：${version}（期望 x.y.z）"
	;;
beta)
	[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "beta 版本号格式不正确：${version}（期望 x.y.z.N）"
	;;
esac

# 3. pom.xml 版本必须与目标版本一致（发版前应先跑 replaceVersion.sh 并提交）
pom_version=$(python3 - "$parent_dir/pom.xml" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
ns = "{http://maven.apache.org/POM/4.0.0}"
node = root.find(ns + "version")
print((node.text or "").strip() if node is not None else "")
PY
)
[ -n "$pom_version" ] || die "无法从 jpom-parent/pom.xml 读取项目版本"
if [ "$pom_version" != "$version" ]; then
	die "版本不一致：jpom-parent/pom.xml 项目版本=${pom_version}，期望 ${version}（请先执行 replaceVersion.sh 并提交/打 tag）"
fi

# 4. 目标路径前缀（POC/灰度），禁止把 POC 前缀写成正式目录的同义路径
target_prefix="${target_prefix#/}"
target_prefix="${target_prefix%/}"
if [ -n "$target_prefix" ]; then
	[[ "$target_prefix" =~ ^[A-Za-z0-9._/-]+$ ]] || die "非法的 target_prefix：${target_prefix}"
	case "${target_prefix%%/*}" in
	release | beta) die "target_prefix 不能以 release/beta 开头（会写入正式目录）：${target_prefix}" ;;
	esac
	log "[WARN] 使用 target_prefix=${target_prefix}，上传到 ${target_prefix}/${channel}/${version}/（非正式目录，验证后请清理）"
fi

# 5. 布尔与包名
case "$dry_run" in
true | false) ;;
*) die "INPUT_DRY_RUN 只能是 true/false：${dry_run}" ;;
esac
case "$deep_verify" in
true | false) ;;
*) die "INPUT_DEEP_VERIFY 只能是 true/false：${deep_verify}" ;;
esac

remote_rel="${target_prefix:+${target_prefix}/}${channel}/${version}"
server_pkg="server-${version}-release"
agent_pkg="agent-${version}-release"
merged_pkg="jpom-${version}.zip"

# 分支 push 仅做构建自检（不写 R2）
self_test=0
case "$ref" in
refs/heads/*)
	if [ "$event" = "push" ]; then
		self_test=1
		dry_run="true"
		log "[WARN] 分支 push 触发，强制 dry_run=true（只构建/校验，不上传 R2）"
	fi
	;;
esac

emit channel "$channel"
emit version "$version"
emit target_prefix "$target_prefix"
emit remote_rel "$remote_rel"
emit public_base "$public_base"
emit server_pkg "$server_pkg"
emit agent_pkg "$agent_pkg"
emit merged_pkg "$merged_pkg"
emit dry_run "$dry_run"
emit self_test "$self_test"
emit deep_verify "$deep_verify"

log "解析完成：channel=${channel} version=${version} prefix=${target_prefix:-<无>} remote_rel=${remote_rel} dry_run=${dry_run} self_test=${self_test}"
