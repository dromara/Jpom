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
# 解析 R2 打包发布元数据（GitHub Actions release-package 工作流调用，也可本地调试）
#
# 输出（写入 $GITHUB_OUTPUT；未设置时打印到 stdout）：
#   channel / version / target_prefix / remote_rel / public_base
#   server_pkg / agent_pkg / merged_pkg（jpom-<version>.tar.gz）/ legacy_zip
#   dry_run / deep_verify
#
# 环境变量：
#   INPUT_CHANNEL        auto|release|beta（默认 auto）
#   INPUT_VERSION        版本号（空则自动推导）
#   INPUT_TARGET_PREFIX  目标路径前缀（POC/灰度用，如 ci-test；空=正式 release|beta 目录）
#   INPUT_DRY_RUN        true|false（默认 false；只构建不上传）
#   INPUT_DEEP_VERIFY    true|false（默认 false）
#   INPUT_LEGACY_ZIP     true|false（默认 true；过渡期同时构建/上传旧客户端在线升级用的 zip）
#   JPOM_LEGACY_ZIP      INPUT_LEGACY_ZIP 的兜底来源（GitHub 仓库变量，默认 true）
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
# 过渡期（2.12.0 前）默认同时产出 zip，兼容旧客户端在线升级；2.12.0 起可关闭
legacy_zip_input="${INPUT_LEGACY_ZIP:-${JPOM_LEGACY_ZIP:-true}}"
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

# 1. 渠道初值（auto 时先按 ref 推导：tag / beta 分支 / 其它；dev 分支自检会在下面按 pom 版本修正）
channel="$channel_input"
if [ "$channel" = "auto" ]; then
	case "$ref" in
	refs/tags/v*) channel="release" ;;
	refs/heads/beta) channel="beta" ;;
	*) channel="release" ;;
	esac
fi

# 2. 分支 push 仅做构建自检（不写 R2，强制 dry_run）
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

# 3. 读取 pom 版本（用于版本推导与一致性校验）
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

# 4. 版本：显式输入 > tag > tag 文件
#    分支自检（dry_run）时若 tag 文件版本与 pom 实际版本不一致（dev 常态：pom 是下一个 beta 版本），
#    按 pom 版本构建自检产物，避免“版本不一致”误判导致 workflow 自检必然失败。
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
	if [ "$self_test" = "1" ] && [ "$version" != "$pom_version" ]; then
		log "[WARN] 分支自检：tag.${channel}.txt 版本 ${version} 与 pom 版本 ${pom_version} 不一致，按 pom 版本构建（dry_run，不上传）"
		version="$pom_version"
		if [ "$channel_input" = "auto" ]; then
			if [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
				channel="beta"
			elif [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
				channel="release"
			fi
			log "[WARN] 分支自检：按 pom 版本将自检渠道调整为 ${channel}"
		fi
	fi
fi
[ -n "$version" ] || die "无法推导版本号，请通过 version 输入或 tag 文件指定"
case "$channel" in
release)
	[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "release 版本号格式不正确：${version}（期望 x.y.z）"
	;;
beta)
	[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "beta 版本号格式不正确：${version}（期望 x.y.z.N）"
	;;
*) die "不支持的 channel：${channel}（可选 auto/release/beta）" ;;
esac

# 5. pom 版本必须与目标版本一致（正式发布强校验，防止忘替换版本就发版；
#    分支 push 的 dry-run 自检仅告警，不阻断 workflow 自检）
if [ "$pom_version" != "$version" ]; then
	if [ "$self_test" = "1" ]; then
		log "[WARN] 分支自检：pom 版本 ${pom_version} 与构建版本 ${version} 不一致（dry_run 仅自检，忽略）"
	else
		die "版本不一致：jpom-parent/pom.xml 项目版本=${pom_version}，期望 ${version}（请先执行 replaceVersion.sh 并提交/打 tag）"
	fi
fi

# 6. 目标路径前缀（POC/灰度），禁止把 POC 前缀写成正式目录的同义路径
target_prefix="${target_prefix#/}"
target_prefix="${target_prefix%/}"
if [ -n "$target_prefix" ]; then
	[[ "$target_prefix" =~ ^[A-Za-z0-9._/-]+$ ]] || die "非法的 target_prefix：${target_prefix}"
	case "${target_prefix%%/*}" in
	release | beta) die "target_prefix 不能以 release/beta 开头（会写入正式目录）：${target_prefix}" ;;
	esac
	log "[WARN] 使用 target_prefix=${target_prefix}，上传到 ${target_prefix}/${channel}/${version}/（非正式目录，验证后请清理）"
fi

# 7. 布尔与包名
case "$dry_run" in
true | false) ;;
*) die "INPUT_DRY_RUN 只能是 true/false：${dry_run}" ;;
esac
case "$deep_verify" in
true | false) ;;
*) die "INPUT_DEEP_VERIFY 只能是 true/false：${deep_verify}" ;;
esac
case "$legacy_zip_input" in
true | 1 | yes | on) legacy_zip="true" ;;
false | 0 | no | off | "") legacy_zip="false" ;;
*) die "INPUT_LEGACY_ZIP/JPOM_LEGACY_ZIP 只能是 true/false：${legacy_zip_input}" ;;
esac

remote_rel="${target_prefix:+${target_prefix}/}${channel}/${version}"
server_pkg="server-${version}-release"
agent_pkg="agent-${version}-release"
# 合并包固定 tar.gz（内含 server/agent 的 tar.gz+sha1，legacy 过渡期再含 zip）
merged_pkg="jpom-${version}.tar.gz"
# Go 插件端产物（发布包 + 在线升级容器），与 Java 产物同版本目录
go_dist_rel="go-agent/dist"
go_upgrade_pkg="agent-go-${version}-upgrade.jar"


emit channel "$channel"
emit version "$version"
emit target_prefix "$target_prefix"
emit remote_rel "$remote_rel"
emit public_base "$public_base"
emit server_pkg "$server_pkg"
emit agent_pkg "$agent_pkg"
emit merged_pkg "$merged_pkg"
emit legacy_zip "$legacy_zip"
emit go_dist_rel "$go_dist_rel"
emit go_upgrade_pkg "$go_upgrade_pkg"
emit dry_run "$dry_run"
emit self_test "$self_test"
emit deep_verify "$deep_verify"

log "解析完成：channel=${channel} version=${version} prefix=${target_prefix:-<无>} remote_rel=${remote_rel} legacy_zip=${legacy_zip} dry_run=${dry_run} self_test=${self_test}"
