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
# 触发 GitHub Actions release-package 工作流（构建发布包并上传 R2），等待完成，
# 再把产物下载回本地（供 release.sh 做完整性校验/启动冒烟）。
#
# 用法：
#   bash script/release-r2-ci.sh --channel release --version 2.11.14
#   bash script/release-r2-ci.sh --channel beta --version 2.11.14.1 --no-wait
#   bash script/release-r2-ci.sh --channel release --version 2.11.12 --ref dev --prefix ci-test   # POC，不写正式目录
#   bash script/release-r2-ci.sh --channel release --version 2.11.12 --prefix ci-test --download-only  # 只下载已上传的 POC 产物
#
# 可选参数：
#   --channel release|beta     发布渠道（必填）
#   --version VER              目标版本（必填）
#   --ref REF                  触发的 GitHub ref，默认 release=v<version> tag、beta=beta 分支
#   --prefix PREFIX            目标路径前缀（POC/灰度；空=正式目录）
#   --repo OWNER/REPO          GitHub 仓库，默认 dromara/Jpom
#   --workflow NAME            工作流文件名，默认 release-package.yml
#   --download-dir DIR         产物下载目录（默认工作区 jpom-parent）
#   --download-base URL        产物下载基址（默认 R2 r2.dev 公开域名；可用 R2_PUBLIC_BASE 覆盖）
#   --no-wait                  只触发，不等待完成（不下载）
#   --wait-timeout MIN         等待超时分钟数，默认 30
#   --deep-verify              让 CI 回验时下载全部对象逐字节比对
#   --download-only            不触发 workflow，直接从下载基址下载并校验（用于验证/重试）
#   --skip-download            只等待 CI，不下载产物
#   --skip-verify              下载后不做本地完整性校验
#   --dry-run                  只打印将要执行的动作，不触发任何远程写操作
#
# 环境变量：GH_REPO（默认 dromara/Jpom）、GH_TOKEN（gh 认证）
#-----------------------------------------------------------

set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
root_dir=$(cd "${script_dir}/.." && pwd)
parent_dir="${root_dir}/jpom-parent"

channel=""
version=""
ref=""
prefix=""
repo="${GH_REPO:-dromara/Jpom}"
workflow="release-package.yml"
download_dir="${parent_dir}"
download_base="${R2_PUBLIC_BASE:-https://pub-a46506c7e8ff43fabc4c1ce8e5faffcb.r2.dev}"
download_base="${download_base%/}"
wait=1
wait_timeout=30
deep_verify=0
download_only=0
skip_download=0
skip_verify=0
dry_run=0

log() { echo "[release-r2-ci] $*"; }
warn() { echo "[release-r2-ci][WARN] $*" >&2; }
die() {
	echo "[release-r2-ci][ERROR] $*" >&2
	exit 1
}

usage() {
	sed -n '/^# 用法：/,/^#-----------------------------------------------------------$/p' "$0" | sed 's/^# \{0,1\}//'
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
	--ref)
		ref="${2:?}"
		shift 2
		;;
	--prefix)
		prefix="${2:?}"
		shift 2
		;;
	--repo)
		repo="${2:?}"
		shift 2
		;;
	--workflow)
		workflow="${2:?}"
		shift 2
		;;
	--download-dir)
		download_dir="${2:?}"
		shift 2
		;;
	--download-base)
		download_base="${2:?}"
		download_base="${download_base%/}"
		shift 2
		;;
	--no-wait)
		wait=0
		shift
		;;
	--wait-timeout)
		wait_timeout="${2:?}"
		shift 2
		;;
	--deep-verify)
		deep_verify=1
		shift
		;;
	--download-only)
		download_only=1
		shift
		;;
	--skip-download)
		skip_download=1
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
		usage
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
[[ "$repo" =~ ^[^/[:space:]]+/[^/[:space:]]+$ ]] || die "--repo 需为 OWNER/REPO：${repo}"
[[ "$wait_timeout" =~ ^[0-9]+$ ]] || die "--wait-timeout 需为非负整数（分钟）：${wait_timeout}"
prefix="${prefix#/}"
prefix="${prefix%/}"
if [ -n "$prefix" ]; then
	[[ "$prefix" =~ ^[A-Za-z0-9._/-]+$ ]] || die "非法的 --prefix：${prefix}"
	case "${prefix%%/*}" in
	release | beta) die "--prefix 不能以 release/beta 开头（会写入正式目录）：${prefix}" ;;
	esac
fi
if [ -z "$ref" ]; then
	if [ "$channel" = "release" ]; then
		ref="v${version}"
	else
		ref="beta"
	fi
fi
remote_rel="${prefix:+${prefix}/}${channel}/${version}"
public_url="${download_base}/${remote_rel}"

need_cmd() { command -v "$1" >/dev/null 2>&1 || die "缺少命令：$1"; }

# ---------------------------------------------------------------------------
# 下载与校验（--download-only 或 CI 成功后的收尾）
# ---------------------------------------------------------------------------
download_artifacts() {
	local kinds=(server agent)
	local kind file sha
	local target="${download_dir}/modules"
	mkdir -p "$target"
	for kind in "${kinds[@]}"; do
		mkdir -p "${target}/${kind}/target"
		for ext in zip tar.gz; do
			file="${target}/${kind}/target/${kind}-${version}-release.${ext}"
			sha="${file}.sha1"
			log "下载 ${public_url}/${file##*/}"
			curl -fsSL --retry 3 --retry-delay 2 -o "$file" "${public_url}/${file##*/}" ||
				die "下载失败：${public_url}/${file##*/}"
			curl -fsSL --retry 3 --retry-delay 2 -o "$sha" "${public_url}/${file##*/}.sha1" ||
				die "下载失败：${public_url}/${file##*/}.sha1"
		done
		# 解压运行目录，供 release.sh 冒烟使用
		local run_dir="${target}/${kind}/target/${kind}-${version}-release"
		rm -rf "$run_dir"
		tar -xzf "${target}/${kind}/target/${kind}-${version}-release.tar.gz" -C "${target}/${kind}/target" ||
			die "解压失败：${kind}-${version}-release.tar.gz"
		[ -d "$run_dir" ] || die "解压后缺少运行目录：${run_dir}"
		log "OK  ${kind} 产物已下载并解压到 ${run_dir}"
	done
	if [ "$skip_verify" = "0" ]; then
		bash "${root_dir}/script/verify-release-artifacts.sh" \
			--parent-dir "$download_dir" --version "$version" --downloaded ||
			die "下载后的产物完整性校验失败"
	fi
}

# ---------------------------------------------------------------------------
# 等待 Gitee -> GitHub 镜像同步（与 release.sh docker 阶段同思路）
# ---------------------------------------------------------------------------
wait_ref_sync() {
	local kind local_sha deadline remote_info remote_sha remote_type
	if git -C "$parent_dir" rev-parse --verify --quiet "refs/tags/${ref}^{commit}" >/dev/null; then
		kind="tags"
	elif git -C "$parent_dir" rev-parse --verify --quiet "refs/heads/${ref}^{commit}" >/dev/null; then
		kind="heads"
	else
		die "无法判断 ref ${ref} 是 tag 还是分支（本地不存在）"
	fi
	local_sha=$(git -C "$parent_dir" rev-parse "${ref}^{commit}")
	log "等待 GitHub ${repo} 镜像同步：${ref}（${kind}）-> ${local_sha:0:12}…（最多 5 分钟）"
	deadline=$((SECONDS + 300))
	while [ "$SECONDS" -lt "$deadline" ]; do
		remote_info=$(gh api "repos/${repo}/git/ref/${kind}/${ref}" --jq '.object.sha + " " + .object.type' 2>/dev/null || true)
		if [ -n "$remote_info" ]; then
			remote_sha=${remote_info%% *}
			remote_type=${remote_info##* }
			if [ "$remote_type" = "tag" ]; then
				remote_sha=$(gh api "repos/${repo}/git/tags/${remote_sha}" --jq '.object.sha' 2>/dev/null || true)
			fi
			if [ -n "$remote_sha" ] && [ "$remote_sha" = "$local_sha" ]; then
				log "GitHub 镜像已同步"
				return 0
			fi
		fi
		sleep 10
	done
	die "GitHub ${repo} 上的 ${ref} 在 5 分钟内未同步到本地 commit ${local_sha}"
}

# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------
log "仓库：${repo}  工作流：${workflow}  ref：${ref}"
log "渠道：${channel}  版本：${version}  目标路径：${remote_rel}（下载基址 ${download_base}）"

if [ "$download_only" = "1" ]; then
	need_cmd curl
	download_artifacts
	log "仅下载模式完成：${public_url}"
	exit 0
fi

if [ "$dry_run" = "1" ]; then
	log "(dry-run) 将执行："
	log "  gh workflow run ${workflow} --repo ${repo} --ref ${ref} -f channel=${channel} -f version=${version} -f target_prefix=${prefix} -f deep_verify=$( [ "$deep_verify" = 1 ] && echo true || echo false )"
	log "  然后等待 run 完成并下载 ${public_url}/ 到 ${download_dir}"
	exit 0
fi

need_cmd gh
need_cmd curl
need_cmd git
gh auth status >/dev/null 2>&1 || die "gh 未登录 GitHub（或 GH_TOKEN 未设置）：--download-only 模式可跳过"

wait_ref_sync

dispatched_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
log "触发 GitHub Actions：${workflow} ref=${ref}"
if ! dispatch_output=$(gh workflow run "$workflow" --repo "$repo" --ref "$ref" \
	-f channel="$channel" -f version="$version" -f target_prefix="$prefix" \
	-f dry_run=false -f deep_verify="$( [ "$deep_verify" = 1 ] && echo true || echo false )" 2>&1); then
	die "触发 GitHub Actions 失败：${dispatch_output}
可能原因：工作流 ${workflow} 还没在该仓库运行过（workflow_dispatch 首次运行限制）；可先 push 一次 workflow 文件自检触发"
fi

find_run() {
	local timeout=$1 deadline info
	deadline=$((SECONDS + timeout))
	while [ "$SECONDS" -lt "$deadline" ]; do
		info=$(gh api "repos/${repo}/actions/workflows/${workflow}/runs?event=workflow_dispatch&per_page=20" 2>/dev/null |
			python3 -c '
import datetime
import json
import sys

since = datetime.datetime.strptime(sys.argv[1], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=datetime.timezone.utc)
since -= datetime.timedelta(seconds=120)
try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(0)
candidates = []
for run in data.get("workflow_runs") or []:
    try:
        created_at = datetime.datetime.strptime(run.get("created_at") or "", "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=datetime.timezone.utc)
    except Exception:
        continue
    if created_at < since:
        continue
    candidates.append((created_at, run.get("id"), run.get("html_url")))
if not candidates:
    sys.exit(0)
candidates.sort()
_, run_id, url = candidates[0]
print(f"{run_id} {url}")
' "$dispatched_at" || true)
		if [ -n "$info" ]; then
			printf '%s\n' "$info"
			return 0
		fi
		sleep 5
	done
	return 1
}

found=$(find_run 180) || die "已触发 GitHub Actions，但 3 分钟内未找到本次 run；请到 https://github.com/${repo}/actions/workflows/${workflow} 查看"
run_id=${found%% *}
run_url=${found#* }

if [ "$wait" = "0" ]; then
	log "已触发（--no-wait）：${run_url}"
	exit 0
fi

log "等待 run 完成（超时 ${wait_timeout} 分钟）：${run_url}"
deadline=$((SECONDS + wait_timeout * 60))
status=""
while [ "$SECONDS" -lt "$deadline" ]; do
	status=$(gh api "repos/${repo}/actions/runs/${run_id}" --jq '.status' 2>/dev/null || true)
	[ "$status" = "completed" ] && break
	sleep 15
done
conclusion=$(gh api "repos/${repo}/actions/runs/${run_id}" --jq '.conclusion' 2>/dev/null || true)
if [ "$status" != "completed" ]; then
	die "run ${run_id} 在 ${wait_timeout} 分钟内未完成（status=${status:-未知}）：${run_url}"
fi
if [ "$conclusion" != "success" ]; then
	die "GitHub Actions 打包/上传失败（conclusion=${conclusion}）：${run_url}
排查：gh run view ${run_id} --repo ${repo} --log-failed"
fi
log "GitHub Actions 成功：${run_url}"

if [ "$skip_download" = "1" ]; then
	log "已指定 --skip-download，跳过产物下载与本地校验"
	exit 0
fi
download_artifacts
log "完成：产物已下载到 ${download_dir}/modules/*/target/，下载基址 ${public_url}"
