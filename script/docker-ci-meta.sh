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
# 解析 Docker 镜像发布元数据（GitHub Actions 工作流调用，也可本地调试）
#
# 输出（写入 $GITHUB_OUTPUT；未设置时打印到 stdout）：
#   channel / version / images / images_json / platforms
#   server_dockerfile / agent_dockerfile
#   server_image / agent_image / server_tags / agent_tags
#   dry_run / wait_r2_minutes / r2_base
#
# 环境变量（工作流传入）：
#   INPUT_CHANNEL            auto|release|beta（默认 auto）
#   INPUT_VERSION            版本号（空则自动推导）
#   INPUT_IMAGES             both|server|agent（默认 both）
#   INPUT_PLATFORMS          默认 linux/amd64,linux/arm64
#   INPUT_TAGS_OVERRIDE      逗号分隔的自定义 tag（空=自动规则）
#   INPUT_DRY_RUN            true|false（默认 false）
#   INPUT_WAIT_R2_MINUTES    等待 R2 产物就绪的最长分钟数（默认 30）
#   GITHUB_EVENT_NAME        workflow_dispatch|push
#   GITHUB_REF               refs/tags/v2.11.13 / refs/heads/dev
#   GITHUB_REF_NAME          v2.11.13 / dev
#   GITHUB_OUTPUT            输出文件（GitHub Actions 提供）
#   DOCKERHUB_NAMESPACE      默认 jpomdocker
#   DOCKERHUB_SERVER_REPO    默认 jpom
#   DOCKERHUB_AGENT_REPO     默认 jpom-agent
#   JPOM_DOWNLOAD_BASE       发布包下载基址（默认 https://d2.jpom.download；
#                            GitHub Actions 使用 r2.dev 公开域名，见 workflow 中 JPOM_DOWNLOAD_BASE）
#   REPO_DIR                 仓库根目录（默认脚本上级目录）
#-----------------------------------------------------------

set -euo pipefail

repo_dir="${REPO_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
event="${GITHUB_EVENT_NAME:-workflow_dispatch}"
ref="${GITHUB_REF:-refs/heads/dev}"
ref_name="${GITHUB_REF_NAME:-${ref#refs/}}"
ref_name="${ref_name#refs/}"

channel_input="${INPUT_CHANNEL:-auto}"
version_input="${INPUT_VERSION:-}"
images_input="${INPUT_IMAGES:-both}"
platforms_input="${INPUT_PLATFORMS:-linux/amd64,linux/arm64}"
tags_override="${INPUT_TAGS_OVERRIDE:-}"
dry_run_input="${INPUT_DRY_RUN:-false}"
wait_r2_minutes="${INPUT_WAIT_R2_MINUTES:-30}"
r2_base="${JPOM_DOWNLOAD_BASE:-${R2_BASE:-https://d2.jpom.download}}"
dockerhub_namespace="${DOCKERHUB_NAMESPACE:-jpomdocker}"
dockerhub_server_repo="${DOCKERHUB_SERVER_REPO:-jpom}"
dockerhub_agent_repo="${DOCKERHUB_AGENT_REPO:-jpom-agent}"

log() { echo "[docker-ci-meta] $*" >&2; }
die() {
	echo "[docker-ci-meta][ERROR] $*" >&2
	exit 1
}

emit() {
	if [ -n "${GITHUB_OUTPUT:-}" ]; then
		printf '%s=%s\n' "$1" "$2" >>"$GITHUB_OUTPUT"
	else
		printf '%s=%s\n' "$1" "$2"
	fi
}

emit_multiline() {
	local key=$1 value=$2
	if [ -n "${GITHUB_OUTPUT:-}" ]; then
		{
			printf '%s<<__JPOM_DOCKER_CI_EOF__\n' "$key"
			printf '%s\n' "$value"
			printf '__JPOM_DOCKER_CI_EOF__\n'
		} >>"$GITHUB_OUTPUT"
	else
		printf '%s<<__JPOM_DOCKER_CI_EOF__\n' "$key"
		printf '%s\n' "$value"
		printf '__JPOM_DOCKER_CI_EOF__\n'
	fi
}

# ---------------------------------------------------------------------------
# 1. 解析发布渠道
# ---------------------------------------------------------------------------
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
log "event=${event} ref=${ref} channel=${channel}"

# 分支 push 只做构建自检（不推送镜像、不登录 Docker Hub）
self_test=0
dry_run="$dry_run_input"
case "$ref" in
refs/heads/*)
	if [ "$event" = "push" ]; then
		self_test=1
		dry_run="true"
		log "[WARN] 分支 push 触发，强制 dry_run=true（只构建、不登录、不推送）"
	fi
	;;
esac
case "$dry_run" in
true | false) ;;
*) die "INPUT_DRY_RUN 只能是 true/false：${dry_run}" ;;
esac

# ---------------------------------------------------------------------------
# 2. 解析版本号
# ---------------------------------------------------------------------------
version="$version_input"
if [ -z "$version" ]; then
	case "$ref" in
	refs/tags/*)
		version="${ref_name#v}"
		;;
	*)
		tag_file="${repo_dir}/script/tag.${channel}.txt"
		[ -f "$tag_file" ] || die "缺少版本号来源文件：${tag_file}"
		version=$(tr -d ' \t\r\n' <"$tag_file")
		;;
	esac
fi

if [ -z "$version" ]; then
	die "无法推导版本号，请通过 version 输入或 script/tag.<channel>.txt 指定"
fi

case "$channel" in
release)
	[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ||
		die "release 版本号格式不正确：${version}（期望 x.y.z）"
	;;
beta)
	[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] ||
		die "beta 版本号格式不正确：${version}（期望 x.y.z.N）"
	;;
esac
log "version=${version}"

# ---------------------------------------------------------------------------
# 3. 选择 Dockerfile 并校验版本号已替换
# ---------------------------------------------------------------------------
server_dockerfile="modules/server/DockerfileRelease"
agent_dockerfile="modules/agent/DockerfileRelease"
if [ "$channel" = "beta" ]; then
	server_dockerfile="modules/server/DockerfileBeta"
	agent_dockerfile="modules/agent/DockerfileBeta"
fi

check_dockerfile_version() {
	local file=$1
	local abs="${repo_dir}/${file}"
	[ -f "$abs" ] || die "缺少 Dockerfile：${file}"
	local actual
	actual=$(sed -n -E 's/^ENV[[:space:]]+JPOM_PKG_VERSION[[:space:]=]+([^[:space:]]+).*/\1/p' "$abs" | head -n 1)
	[ -n "$actual" ] || die "${file} 中未找到 ENV JPOM_PKG_VERSION"
	if [ "$actual" != "$version" ]; then
		die "版本号不一致：${file} 中 JPOM_PKG_VERSION=${actual}，期望 ${version}（请先执行 replaceVersion.sh 或检查触发 ref）"
	fi
	log "Dockerfile 版本校验通过：${file} -> ${actual}"
}

check_dockerfile_version "$server_dockerfile"
check_dockerfile_version "$agent_dockerfile"

# ---------------------------------------------------------------------------
# 4. 镜像与目标平台
# ---------------------------------------------------------------------------
images=""
case "$images_input" in
both) images="server,agent" ;;
server) images="server" ;;
agent) images="agent" ;;
*) die "不支持的 images：${images_input}（可选 both/server/agent）" ;;
esac

platforms=$(echo "$platforms_input" | tr -d ' \t')
[ -n "$platforms" ] || die "platforms 不能为空"
IFS=',' read -r -a platform_list <<<"$platforms"
for platform in "${platform_list[@]}"; do
	if ! [[ "$platform" =~ ^linux/[a-z0-9]+(/[a-z0-9]+)?$ ]]; then
		die "不支持的平台：${platform}（期望 linux/<arch>，如 linux/amd64）"
	fi
done

# ---------------------------------------------------------------------------
# 5. 计算镜像 tag
# ---------------------------------------------------------------------------
server_image="${dockerhub_namespace}/${dockerhub_server_repo}"
agent_image="${dockerhub_namespace}/${dockerhub_agent_repo}"

tags=()
if [ -n "$tags_override" ]; then
	IFS=',' read -r -a raw_tags <<<"$tags_override"
	for raw_tag in "${raw_tags[@]}"; do
		raw_tag=$(echo "$raw_tag" | tr -d ' \t')
		[ -n "$raw_tag" ] || continue
		if ! [[ "$raw_tag" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$ ]]; then
			die "非法的自定义 tag：${raw_tag}"
		fi
		tags+=("$raw_tag")
	done
	[ "${#tags[@]}" -gt 0 ] || die "tags_override 解析后为空"
else
	if [ "$channel" = "release" ]; then
		tags=("$version" "latest")
	else
		tags=("${version}-beta" "latest-beta")
	fi
fi

join_tags() {
	local image=$1
	local out="" tag
	for tag in "${tags[@]}"; do
		if [ -n "$out" ]; then
			out+=$'\n'
		fi
		out+="${image}:${tag}"
	done
	printf '%s' "$out"
}

server_tags=$(join_tags "$server_image")
agent_tags=$(join_tags "$agent_image")

# ---------------------------------------------------------------------------
# 6. 等待时间校验
# ---------------------------------------------------------------------------
if ! [[ "$wait_r2_minutes" =~ ^[0-9]+$ ]] || [ "$wait_r2_minutes" -gt 120 ]; then
	die "wait_r2_minutes 必须是 0~120 的整数：${wait_r2_minutes}"
fi

# ---------------------------------------------------------------------------
# 7. 输出
# ---------------------------------------------------------------------------
images_json="["
first=1
IFS=',' read -r -a image_list <<<"$images"
for image in "${image_list[@]}"; do
	if [ "$first" = "1" ]; then
		first=0
	else
		images_json+=","
	fi
	images_json+="\"${image}\""
done
images_json+="]"

emit channel "$channel"
emit version "$version"
emit images "$images"
emit images_json "$images_json"
emit platforms "$platforms"
emit server_dockerfile "$server_dockerfile"
emit agent_dockerfile "$agent_dockerfile"
emit server_image "$server_image"
emit agent_image "$agent_image"
emit_multiline server_tags "$server_tags"
emit_multiline agent_tags "$agent_tags"
emit dry_run "$dry_run"
emit self_test "$self_test"
emit wait_r2_minutes "$wait_r2_minutes"
emit r2_base "$r2_base"

log "解析完成：channel=${channel} version=${version} images=${images} platforms=${platforms} dry_run=${dry_run} self_test=${self_test}"
log "镜像 tag："
log "  server: $(echo "$server_tags" | tr '\n' ' ')"
log "  agent : $(echo "$agent_tags" | tr '\n' ' ')"
