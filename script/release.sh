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
# =============================================================================
# Jpom 一键发版脚本（beta / release）
#
# 覆盖流程：
#   预检 -> 合并分支 -> CHANGELOG 日期 -> 版本号替换 -> 前端/后端打包
#   -> 产物完整性校验 -> 服务端/插件端启动冒烟 -> 提交 -> 推送 Gitee
#   -> Cloudflare R2 上传 -> 文档站版本/日志更新并推送 ->（可选）Docker 镜像
#
# 安全约定：
#   - 默认只做本地准备；push / R2 上传 / docs 发布 / docker 推送都要求交互确认或 --yes
#   - 支持 --dry-run 只预检并打印计划，不修改任何文件
#   - 只向 Gitee 推送代码；docs 子模块是独立的 GitHub Pages 仓库，按文档流程推送
#   - beta 不打 tag、不创建 Gitee Release；release 打 vX.Y.Z tag
#
# 用法：
#   bash script/release.sh beta
#   bash script/release.sh beta --dry-run
#   bash script/release.sh beta --version 2.11.12.2 --yes
#   bash script/release.sh release --yes
#   bash script/release.sh beta --to commit              # 只做本地准备（不推送/不上传）
#   bash script/release.sh beta --from upload            # 断点续跑：从上传开始
#   bash script/release.sh beta --skip-build --skip-smoke # 使用已有产物（调试用）
#   bash script/release.sh release --with-r2-ci           # 打包+R2 上传交给 GitHub Actions（CI 完成后本机校验+冒烟）
#   bash script/release.sh beta --with-r2-ci --r2-ci-prefix ci-test   # POC：只上传到 ci-test 前缀
#
# 主要参数：
#   beta|release             发版类型（必填）
#   -v, --version VER        指定版本号；默认按 tag 文件自动计算
#       --source-branch BR   源分支（beta 默认 dev；release 默认 dev）
#       --target-branch BR   目标分支（beta 默认 beta；release 默认 master）
#       --date YYYY-MM-DD    CHANGELOG 发版日期，默认当天
#       --dry-run, --check   只做预检并打印计划
#   -y, --yes                远程操作不再交互确认
#       --from PHASE         从指定阶段开始（隐含 --resume）
#       --to PHASE           到指定阶段结束后停止
#       --resume             断点续跑（跳过“工作区必须干净”等严格检查）
#       --no-fetch           不执行 git fetch
#       --skip-build         跳过前端/后端构建
#       --build-retries N    产物校验失败时自动重跑构建的次数（默认 1，规避本机偶发坏包）
#       --skip-tests         跳过回归单元测试（不推荐；共享基础类/安全修复必须执行）
#       --skip-verify        跳过产物完整性校验（不推荐）
#       --skip-smoke         跳过启动冒烟
#       --skip-sensitive     跳过敏感信息检查（不推荐）
#       --no-push            不推送 Gitee（需同时 --no-upload --no-docs）
#       --no-upload          不上传 R2
#       --no-docs            不更新/发布文档
#       --force-upload       同版本覆盖上传 R2（注意 Cloudflare 缓存）
#       --deep-verify        R2 上传后从下载中心逐字节回验
#       --with-r2-ci         打包+上传 R2 改由 GitHub Actions release-package 工作流执行：
#                            本机跳过构建/产物校验/启动冒烟，CI 成功后自动下载产物回本地做校验+冒烟
#       --r2-ci-prefix PREFIX  CI 上传目标路径前缀（POC/灰度用，如 ci-test；空=正式 release|beta 目录）
#       --r2-ci-ref REF       CI 触发的 ref（默认 release=v<version> tag、beta=目标分支）
#       --no-r2-ci-wait       只触发 CI 不等待完成（不下载/校验/冒烟，需自行跟进）
#       --r2-ci-timeout MIN   CI 等待超时分钟数（默认 30）
#       --with-docker        发布 Docker 镜像（默认走 GitHub Actions，等待 run 完成并回验 Docker Hub）
#       --docker-local       发布 Docker 镜像时改用本机 docker buildx 构建并推送（CI 不可用时兜底）
#       --docker-ref REF     GitHub Actions 触发的 ref（release 默认 vX.Y.Z tag；beta 默认目标分支）
#       --docker-platforms   多平台列表，默认 linux/amd64,linux/arm64
#       --docker-wait        等待 GitHub Actions run 完成（默认）
#       --no-docker-wait     只触发 GitHub Actions，不等待完成（跳过 Docker Hub 回验）
#       --docker-wait-timeout MIN  等待 run 完成的超时分钟数（默认 30）
#       --github-repo OWNER/REPO   GitHub 工作流仓库（默认 dromara/Jpom，也可用 GH_REPO 覆盖）
#       --with-post          发布后自动创建 Gitee/GitHub Release 并发送微信群通知（stable）
#       --no-post            不执行发布后置步骤（默认）
#       --offline            Maven 使用离线模式
#       --keep-temp          保留冒烟临时目录
#       --allow-non-gitee    允许 origin 不是 Gitee（仅本地/测试用）
#       --parent-dir DIR     覆盖 jpom-parent 目录（测试用）
#       --root-dir DIR       覆盖工作区根目录（测试用）
#       --jdk-home DIR       指定 JAVA_HOME
#       --mvn PATH           指定 mvn 可执行文件
#       --node PATH          指定 node 可执行文件
#       --npm PATH           指定 npm 可执行文件
#       --npm-cache DIR      指定 npm 缓存目录（默认 .tmp/npm-cache）
#
# 环境变量：
#   JAVA_HOME / MVN_ARGS / NPM_ARGS / R2_REMOTE / R2_BUCKET / R2_PUBLIC_BASE
#   DOCKER_PLATFORMS / DOCKER_REPOSITORY / DOCKER_AGENT_REPOSITORY
#   GH_REPO / GH_TOKEN    Docker CI 的 GitHub 仓库与 gh 认证（默认 dromara/Jpom）
#   JPOM_GIT_SSH_COMMAND  覆盖 git 使用的 GIT_SSH_COMMAND（SSH 配置异常时可用）
#
# Docker 镜像发布（默认 CI）：
#   走 GitHub Actions（jpom-parent/.github/workflows/docker-publish.yml）：
#   等待 Gitee -> GitHub 镜像同步 -> dispatch 工作流 -> 等待 run 结束 -> 回验 Docker Hub。
#   工作流首次启用前（尚未存在于默认分支 master）或 CI 不可用时，用 --docker-local 本机 buildx 兜底。
#
# 产物校验要点（本机 Maven 曾出现偶发 0 字节 class，必须拦）：
#   - 所有 .class 魔数正确、常量池长度足够（含 BOOT-INF/lib 内嵌 jar）
#   - server jar 必须包含 BOOT-INF/classes/dist/index.html
#   - zip/tar.gz 完整性与 .sha1 一致
#   - 服务端/插件端 release 包能真实启动
# =============================================================================

if [ -z "${BASH_VERSION:-}" ]; then
	exec bash "$0" "$@"
fi

set -Eeuo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# ---------------------------------------------------------------------------
# 参数默认值
# ---------------------------------------------------------------------------
MODE=""
VERSION_OPT=""
TARGET_BRANCH_OPT=""
SOURCE_BRANCH_OPT=""
ROOT_OPT=""
PARENT_OPT=""
DOCS_OPT=""
JDK_HOME_OPT=""
MVN_OPT=""
NODE_OPT=""
NPM_OPT=""
NPM_CACHE_OPT=""
RELEASE_DATE_OPT=""
FROM_PHASE=""
TO_PHASE=""

DRY_RUN=0
YES=0
RESUME=0
FETCH=1
FETCH_EXPLICIT=0
BUILD=1
BUILD_RETRIES=1
TESTS=1
VERIFY=1
SMOKE=1
SENSITIVE=1
PUSH=1
UPLOAD=1
DOCS=1
DOCKER=0
DOCKER_MODE="ci"
DOCKER_REF_OPT=""
DOCKER_WAIT=1
DOCKER_WAIT_TIMEOUT_OPT=""
GH_REPO_OPT=""
POST=0
FORCE_UPLOAD=0
DEEP_VERIFY=0
R2CI=0
R2_CI_PREFIX=""
R2_CI_REF=""
R2_CI_WAIT=1
R2_CI_TIMEOUT=30
R2CI_SMOKE_READY=0
KEEP_TEMP=0
ALLOW_NON_GITEE=0
FORCE_VERSION=0
OFFLINE=0
DOCKER_PLATFORMS_OPT=""

usage() {
	# 打印文件头 ===== 之间的说明（随文件头增长自动适配，无需维护行号）
	awk '/^# =+$/ { n++; if (n == 1) { print; inside = 1; next } if (n == 2) { print; exit } } inside' "$0" |
		sed 's/^# \{0,1\}//'
}

die() {
	echo "ERROR: $*" >&2
	exit 1
}

warn() {
	echo "WARN: $*" >&2
}

need_cmd() {
	command -v "$1" >/dev/null 2>&1 || die "缺少命令：$1"
}

arg_value() {
	[ "$#" -ge 2 ] || die "参数 $1 缺少值"
	printf '%s' "$2"
}

while [ "$#" -gt 0 ]; do
	case "$1" in
	beta | release)
		[ -z "$MODE" ] || die "只能指定一个发版类型"
		MODE="$1"
		shift
		;;
	-v | --version)
		VERSION_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--source-branch)
		SOURCE_BRANCH_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--target-branch)
		TARGET_BRANCH_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--date)
		RELEASE_DATE_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--dry-run | --check)
		DRY_RUN=1
		shift
		;;
	-y | --yes)
		YES=1
		shift
		;;
	--from)
		FROM_PHASE=$(arg_value "$1" "${2:-}")
		RESUME=1
		shift 2
		;;
	--to)
		TO_PHASE=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--resume)
		RESUME=1
		shift
		;;
	--no-fetch)
		FETCH=0
		FETCH_EXPLICIT=1
		shift
		;;
	--fetch)
		FETCH=1
		FETCH_EXPLICIT=1
		shift
		;;
	--skip-build)
		BUILD=0
		shift
		;;
	--build-retries)
		BUILD_RETRIES=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--skip-tests)
		TESTS=0
		shift
		;;
	--skip-verify)
		VERIFY=0
		shift
		;;
	--skip-smoke)
		SMOKE=0
		shift
		;;
	--skip-sensitive)
		SENSITIVE=0
		shift
		;;
	--no-push)
		PUSH=0
		shift
		;;
	--no-upload)
		UPLOAD=0
		shift
		;;
	--no-docs)
		DOCS=0
		shift
		;;
	--force-upload)
		FORCE_UPLOAD=1
		shift
		;;
	--deep-verify)
		DEEP_VERIFY=1
		shift
		;;
	--with-r2-ci)
		R2CI=1
		shift
		;;
	--r2-ci-prefix)
		R2_CI_PREFIX=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--r2-ci-ref)
		R2_CI_REF=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--no-r2-ci-wait)
		R2_CI_WAIT=0
		shift
		;;
	--r2-ci-timeout)
		R2_CI_TIMEOUT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--with-docker)
		DOCKER=1
		DOCKER_MODE="ci"
		shift
		;;
	--docker-local)
		DOCKER=1
		DOCKER_MODE="local"
		shift
		;;
	--docker-ref)
		DOCKER_REF_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--docker-platforms)
		DOCKER_PLATFORMS_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--docker-wait)
		DOCKER_WAIT=1
		shift
		;;
	--no-docker-wait)
		DOCKER_WAIT=0
		shift
		;;
	--docker-wait-timeout)
		DOCKER_WAIT_TIMEOUT_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--github-repo)
		GH_REPO_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--with-post)
		POST=1
		shift
		;;
	--no-post)
		POST=0
		shift
		;;
	--offline)
		OFFLINE=1
		shift
		;;
	--keep-temp)
		KEEP_TEMP=1
		shift
		;;
	--allow-non-gitee)
		ALLOW_NON_GITEE=1
		shift
		;;
	--force-version)
		FORCE_VERSION=1
		shift
		;;
	--parent-dir)
		PARENT_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--root-dir)
		ROOT_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--docs-dir)
		DOCS_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--jdk-home)
		JDK_HOME_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--mvn)
		MVN_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--node)
		NODE_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--npm)
		NPM_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	--npm-cache)
		NPM_CACHE_OPT=$(arg_value "$1" "${2:-}")
		shift 2
		;;
	-h | --help)
		usage
		exit 0
		;;
	*)
		echo "ERROR: 不支持的参数：$1" >&2
		usage
		exit 1
		;;
	esac
done

[ -n "$MODE" ] || {
	usage
	exit 1
}

if [ "$DRY_RUN" = "1" ] && [ "$FETCH_EXPLICIT" = "0" ]; then
	FETCH=0
fi

if [ "$MODE" = "beta" ]; then
	TARGET_BRANCH=${TARGET_BRANCH_OPT:-beta}
	SOURCE_BRANCH=${SOURCE_BRANCH_OPT:-dev}
	VERSION_SUFFIX="beta"
else
	TARGET_BRANCH=${TARGET_BRANCH_OPT:-master}
	SOURCE_BRANCH=${SOURCE_BRANCH_OPT:-dev}
	VERSION_SUFFIX="release"
fi

if [ -n "$ROOT_OPT" ]; then
	ROOT_DIR=$(cd "$ROOT_OPT" && pwd)
else
	ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)
fi
PARENT_DIR="${PARENT_OPT:-${ROOT_DIR}/jpom-parent}"
DOCS_DIR="${DOCS_OPT:-${ROOT_DIR}/docs}"
R2_SCRIPT="${ROOT_DIR}/script/deploy-release-r2.sh"
SENSITIVE_SCRIPT="${ROOT_DIR}/script/check-sensitive.sh"
GIT_REMOTE="${JPOM_GIT_REMOTE:-origin}"
DOCS_REMOTE="${JPOM_DOCS_REMOTE:-origin}"
R2_REMOTE="${R2_REMOTE:-r2}"
export R2_REMOTE
RELEASE_DATE=${RELEASE_DATE_OPT:-$(date +%F)}
NPM_CACHE_DIR=${NPM_CACHE_OPT:-${ROOT_DIR}/.tmp/npm-cache}
export npm_config_cache="$NPM_CACHE_DIR"
DOCKER_PLATFORMS=${DOCKER_PLATFORMS_OPT:-${DOCKER_PLATFORMS:-linux/amd64,linux/arm64}}
GH_REPO="${GH_REPO_OPT:-${GH_REPO:-dromara/Jpom}}"
DOCKER_WORKFLOW="docker-publish.yml"
DOCKER_WAIT_TIMEOUT=${DOCKER_WAIT_TIMEOUT_OPT:-${DOCKER_WAIT_TIMEOUT:-30}}
DOCKER_REF=""
DOCKER_RUN_ID=""
DOCKER_RUN_URL=""
[[ "$GH_REPO" =~ ^[^/[:space:]]+/[^/[:space:]]+$ ]] || die "--github-repo 需为 OWNER/REPO 格式：${GH_REPO}"
[[ "$R2_CI_TIMEOUT" =~ ^[0-9]+$ ]] || die "--r2-ci-timeout 需为非负整数（分钟）：${R2_CI_TIMEOUT}"
if [ -n "$R2_CI_PREFIX" ]; then
	R2_CI_PREFIX="${R2_CI_PREFIX#/}"
	R2_CI_PREFIX="${R2_CI_PREFIX%/}"
	[[ "$R2_CI_PREFIX" =~ ^[A-Za-z0-9._/-]+$ ]] || die "非法的 --r2-ci-prefix：${R2_CI_PREFIX}"
	case "${R2_CI_PREFIX%%/*}" in
	release | beta) die "--r2-ci-prefix 不能以 release/beta 开头（会写入正式目录）" ;;
	esac
fi
if [ "$R2CI" = "1" ] && [ "$PUSH" = "0" ]; then
	die "--with-r2-ci 要求把代码/tag 推送到 Gitee（GitHub 镜像侧才能构建）；请去掉 --no-push"
fi
[[ "$DOCKER_WAIT_TIMEOUT" =~ ^[0-9]+$ ]] || die "--docker-wait-timeout 需为非负整数（分钟）：${DOCKER_WAIT_TIMEOUT}"
[ -n "$DOCKER_PLATFORMS" ] || die "--docker-platforms 不能为空"

git -C "$PARENT_DIR" rev-parse --git-dir >/dev/null 2>&1 || die "不是 git 仓库：${PARENT_DIR}"
if [ "$DOCS" = "1" ] && ! git -C "$DOCS_DIR" rev-parse --git-dir >/dev/null 2>&1; then
	warn "docs 不是 git 仓库：${DOCS_DIR}（--no-docs 可跳过文档步骤）"
fi

if [ "$PUSH" = "0" ] && { [ "$UPLOAD" = "1" ] || [ "$DOCS" = "1" ]; }; then
	die "--no-push 时必须同时 --no-upload --no-docs（只做本地准备建议用 --to commit）"
fi
if [ "$UPLOAD" = "0" ] && [ "$DOCS" = "1" ]; then
	die "--no-upload 时不能发布文档（下载页会指向不存在的包）；请同时 --no-docs"
fi

# ---------------------------------------------------------------------------
# 日志 / 锁 / 清理
# ---------------------------------------------------------------------------
TMP_ROOT="${ROOT_DIR}/.tmp/release"
LOCK_DIR="${TMP_ROOT}/.lock"
mkdir -p "$TMP_ROOT"
if [ -d "$LOCK_DIR" ]; then
	if [ -f "${LOCK_DIR}/pid" ] && kill -0 "$(cat "${LOCK_DIR}/pid")" 2>/dev/null; then
		die "另一个发版进程正在运行（PID $(cat "${LOCK_DIR}/pid")），如确认无进程请删除 ${LOCK_DIR}"
	fi
	rm -rf "$LOCK_DIR"
fi
mkdir "$LOCK_DIR"
echo $$ >"${LOCK_DIR}/pid"

SMOKE_SERVER_PID=""
SMOKE_AGENT_PID=""
LOG_DIR=""

cleanup() {
	local exit_code=$?
	if [ -n "${SMOKE_SERVER_PID:-}" ]; then kill "$SMOKE_SERVER_PID" 2>/dev/null || true; fi
	if [ -n "${SMOKE_AGENT_PID:-}" ]; then kill "$SMOKE_AGENT_PID" 2>/dev/null || true; fi
	if [ -n "${LOCK_DIR:-}" ]; then rm -rf "$LOCK_DIR" 2>/dev/null || true; fi
	if [ "$exit_code" -ne 0 ] && [ -n "${LOG_DIR:-}" ]; then
		echo
		echo "发版失败，日志：${LOG_DIR}/release.log" >&2
		if [ -n "${VERSION:-}" ]; then
			echo "修复后可尝试：bash script/release.sh ${MODE} --version ${VERSION} --resume --from <失败阶段>" >&2
		fi
	fi
	exit "$exit_code"
}
trap cleanup EXIT

LOG_DIR="${TMP_ROOT}/${MODE}-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$LOG_DIR"
rm -f "${TMP_ROOT}/last"
ln -s "$LOG_DIR" "${TMP_ROOT}/last" 2>/dev/null || true
exec > >(tee -a "${LOG_DIR}/release.log") 2>&1

if [ -t 2 ]; then
	C_RED=$'\033[31m'
	C_GREEN=$'\033[32m'
	C_YELLOW=$'\033[33m'
	C_BLUE=$'\033[36m'
	C_RESET=$'\033[0m'
else
	C_RED=''
	C_GREEN=''
	C_YELLOW=''
	C_BLUE=''
	C_RESET=''
fi

log() {
	printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"
}

section() {
	echo
	echo "${C_BLUE}==============================================================${C_RESET}"
	echo "${C_BLUE} $*${C_RESET}"
	echo "${C_BLUE}==============================================================${C_RESET}"
}

run() {
	log "+ $*"
	"$@"
}

run_in() {
	local dir=$1
	shift
	log "+ (cd ${dir} && $*)"
	(cd "$dir" && "$@")
}

gitp() {
	git -C "$PARENT_DIR" "$@"
}

gitd() {
	git -C "$DOCS_DIR" "$@"
}

confirm() {
	if [ "$YES" = "1" ]; then
		return 0
	fi
	if [ ! -t 0 ]; then
		die "非交互环境，执行该操作需要显式加 --yes：$*"
	fi
	local answer
	read -r -p "$* [y/N] " answer
	case "$answer" in
	y | Y | yes | YES) return 0 ;;
	*) die "已取消" ;;
	esac
}

# ---------------------------------------------------------------------------
# 工具链
# ---------------------------------------------------------------------------
JAVA_BIN=""
MVN_BIN=""
NODE_BIN=""
NPM_BIN=""
SHA1_CMD=""

pick_nvm_node() {
	local p
	for p in "$HOME"/.nvm/versions/node/v18*/bin/node "$HOME"/.nvm/versions/node/v20*/bin/node "$HOME"/.nvm/versions/node/v22*/bin/node; do
		[ -x "$p" ] || continue
		printf '%s' "$p"
		return 0
	done
	return 1
}

init_toolchain() {
	if [ -n "$JDK_HOME_OPT" ]; then
		export JAVA_HOME="$JDK_HOME_OPT"
	elif [ -z "${JAVA_HOME:-}" ] && [ -x "$HOME/.sdkman/candidates/java/current/bin/java" ]; then
		export JAVA_HOME="$HOME/.sdkman/candidates/java/current"
	fi

	if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
		JAVA_BIN="${JAVA_HOME}/bin/java"
	else
		JAVA_BIN=$(command -v java || true)
	fi
	[ -n "$JAVA_BIN" ] || die "未找到 java；请安装 JDK 8 或通过 --jdk-home 指定"

	if [ -n "$MVN_OPT" ]; then
		MVN_BIN="$MVN_OPT"
	elif command -v mvn >/dev/null 2>&1; then
		MVN_BIN=$(command -v mvn)
	elif [ -x "$HOME/.sdkman/candidates/maven/current/bin/mvn" ]; then
		MVN_BIN="$HOME/.sdkman/candidates/maven/current/bin/mvn"
	fi
	[ -n "$MVN_BIN" ] || die "未找到 mvn；请安装 Maven 3.6+ 或通过 --mvn 指定"

	if [ -n "$NODE_OPT" ]; then
		NODE_BIN="$NODE_OPT"
	elif command -v node >/dev/null 2>&1; then
		NODE_BIN=$(command -v node)
	else
		NODE_BIN=$(pick_nvm_node || true)
	fi
	[ -n "$NODE_BIN" ] || die "未找到 node；请安装 Node 18（或通过 --node 指定）"

	if [ -n "$NPM_OPT" ]; then
		NPM_BIN="$NPM_OPT"
	elif [ -x "$(dirname "$NODE_BIN")/npm" ]; then
		NPM_BIN="$(dirname "$NODE_BIN")/npm"
	elif command -v npm >/dev/null 2>&1; then
		NPM_BIN=$(command -v npm)
	fi
	[ -n "$NPM_BIN" ] || die "未找到 npm；请安装 Node 18（或通过 --npm 指定）"

	export PATH="$(dirname "$JAVA_BIN"):$(dirname "$MVN_BIN"):$(dirname "$NODE_BIN"):${PATH}"
	export JAVA_HOME="${JAVA_HOME:-$(cd "$(dirname "$JAVA_BIN")/.." && pwd)}"

	if command -v sha1sum >/dev/null 2>&1; then
		SHA1_CMD="sha1sum"
	elif command -v shasum >/dev/null 2>&1; then
		SHA1_CMD="shasum -a 1"
	else
		die "缺少 sha1sum/shasum"
	fi

	local java_version node_version
	java_version=$("$JAVA_BIN" -version 2>&1 | head -1)
	node_version=$("$NODE_BIN" -v 2>/dev/null || true)
	log "JDK : ${java_version}"
	log "Maven: $("$MVN_BIN" -v 2>/dev/null | head -1 || true)"
	log "Node : ${node_version}  (${NODE_BIN})"

	local node_major
	node_major=$(echo "$node_version" | sed -E 's/^v([0-9]+).*/\1/')
	if [ -n "$node_major" ] && [ "$node_major" != "18" ]; then
		warn "当前 Node 版本为 ${node_version}，CI/发版机推荐 Node 18；如构建异常请切换到 18"
	fi

}

# ---------------------------------------------------------------------------
# 版本 / 分支辅助
# ---------------------------------------------------------------------------
VERSION=""
OLD_VERSION=""
OLD_STABLE=""
OLD_BETA=""
TARGET_REF=""
SOURCE_REF=""

resolve_ref() {
	local branch=$1
	if gitp rev-parse --verify --quiet "refs/heads/${branch}" >/dev/null; then
		printf '%s' "$branch"
	elif gitp rev-parse --verify --quiet "refs/remotes/${GIT_REMOTE}/${branch}" >/dev/null; then
		printf '%s' "${GIT_REMOTE}/${branch}"
	else
		die "分支不存在：${branch}"
	fi
}

read_tag() {
	local channel=$1
	local ref=$2
	gitp show "${ref}:script/tag.${channel}.txt" 2>/dev/null | tr -d ' \r\n' || true
}

pom_version_stream() {
	awk 'BEGIN { parent = 0 } /<\/parent>/ { parent = 1; next } parent && /<version>/ { gsub(/^[ \t]*<version>|<\/version>.*$/, ""); print; exit }'
}

pom_version_worktree() {
	pom_version_stream <"${PARENT_DIR}/pom.xml"
}

pom_version_at() {
	gitp show "$1:pom.xml" | pom_version_stream
}

compute_version() {
	TARGET_REF=$(resolve_ref "$TARGET_BRANCH")
	SOURCE_REF=$(resolve_ref "$SOURCE_BRANCH")
	OLD_STABLE=$(read_tag release "$TARGET_REF")
	OLD_BETA=$(read_tag beta "$TARGET_REF")
	[ -n "$OLD_STABLE" ] || die "无法从 ${TARGET_REF}:script/tag.release.txt 读取稳定版号"

	if [ "$MODE" = "beta" ]; then
		[ -n "$OLD_BETA" ] || die "无法从 ${TARGET_REF}:script/tag.beta.txt 读取 beta 版号"
		OLD_VERSION="$OLD_BETA"
		if [ -n "$VERSION_OPT" ]; then
			VERSION="$VERSION_OPT"
			[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "beta 版本号格式应为 x.y.z.N：${VERSION}"
			[[ "$VERSION" == "${OLD_STABLE}."* ]] || die "beta 版本号必须以当前稳定版 ${OLD_STABLE} 为前缀：${VERSION}"
			local old_seq=${OLD_BETA#"${OLD_STABLE}."}
			local new_seq=${VERSION#"${OLD_STABLE}."}
			if [[ "$old_seq" =~ ^[0-9]+$ ]] && [[ "$new_seq" =~ ^[0-9]+$ ]] && [ "$new_seq" -le "$old_seq" ] && [ "$FORCE_VERSION" != "1" ]; then
				die "新 beta 序号必须大于当前 ${OLD_BETA}：${VERSION}"
			fi
		else
			local seq=0
			if [[ "$OLD_BETA" == "${OLD_STABLE}."* ]]; then
				seq=${OLD_BETA#"${OLD_STABLE}."}
				[[ "$seq" =~ ^[0-9]+$ ]] || seq=0
			fi
			VERSION="${OLD_STABLE}.$((seq + 1))"
		fi
	else
		OLD_VERSION="$OLD_STABLE"
		local major minor patch
		IFS=. read -r major minor patch <<<"$OLD_STABLE"
		[[ "$major" =~ ^[0-9]+$ ]] || die "无法解析稳定版号：${OLD_STABLE}"
		[[ "$minor" =~ ^[0-9]+$ ]] || die "无法解析稳定版号：${OLD_STABLE}"
		[[ "$patch" =~ ^[0-9]+$ ]] || die "无法解析稳定版号：${OLD_STABLE}"
		VERSION="$VERSION_OPT"
		if [ -z "$VERSION" ]; then
			VERSION="${major}.${minor}.$((patch + 1))"
		else
			[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "release 版本号格式应为 x.y.z：${VERSION}"
			if [ "$VERSION" != "${major}.${minor}.$((patch + 1))" ] && [ "$FORCE_VERSION" != "1" ]; then
				die "release 版本号应为 ${major}.${minor}.$((patch + 1))（当前 ${OLD_STABLE} 的下一个 patch）；如确需跳版请加 --force-version"
			fi
		fi
	fi
	TAG="v${VERSION}"
}

check_branch_exists() {
	local branch=$1
	gitp rev-parse --verify --quiet "refs/heads/${branch}" >/dev/null ||
		gitp rev-parse --verify --quiet "refs/remotes/${GIT_REMOTE}/${branch}" >/dev/null ||
		die "git 分支不存在：${branch}"
}

check_sync() {
	local branch=$1
	if ! gitp rev-parse --verify --quiet "refs/heads/${branch}" >/dev/null; then
		return 0
	fi
	if ! gitp rev-parse --verify --quiet "refs/remotes/${GIT_REMOTE}/${branch}" >/dev/null; then
		warn "远程不存在分支 ${GIT_REMOTE}/${branch}（新建分支），跳过同步检查"
		return 0
	fi
	local counts
	counts=$(gitp rev-list --left-right --count "${GIT_REMOTE}/${branch}...${branch}")
	local behind ahead
	behind=$(echo "$counts" | awk '{print $1}')
	ahead=$(echo "$counts" | awk '{print $2}')
	if [ "$behind" != "0" ]; then
		die "本地 ${branch} 落后远程 ${behind} 个提交，请先拉取"
	fi
	if [ "$ahead" != "0" ] && [ "$RESUME" != "1" ]; then
		die "本地 ${branch} 有 ${ahead} 个未推送提交；请先推送或用 --resume 继续"
	fi
}

git_clean_or_die() {
	local dir=$1
	local label=$2
	local status
	status=$(git -C "$dir" status --porcelain --untracked-files=normal)
	if [ -n "$status" ]; then
		if [ "$RESUME" = "1" ]; then
			warn "${label} 工作区不干净（--resume 继续）："
			echo "$status" >&2
		else
			echo "$status" >&2
			die "${label} 工作区不干净，请先提交/清理"
		fi
	fi
}

# ---------------------------------------------------------------------------
# 预检
# ---------------------------------------------------------------------------
check_changelog_section() {
	local file ref suffix="$VERSION_SUFFIX"
	if [ "$MODE" = "beta" ]; then
		file="CHANGELOG-BETA.md"
		ref="$SOURCE_REF"
	else
		file="CHANGELOG.md"
		ref="$TARGET_REF"
	fi
	if ! gitp show "${ref}:${file}" 2>/dev/null | grep -E "^## ${VERSION}-${suffix}([[:space:]]|$)" >/dev/null; then
		cat >&2 <<EOF
ERROR: ${ref}:${file} 中缺少 "## ${VERSION}-${suffix}" 段落。
请先补充发版日志并提交到对应分支后再运行：
  beta    : 在 ${SOURCE_BRANCH} 的 CHANGELOG-BETA.md 新增该段落（日期可留空，脚本会自动补当天日期）
  release : 在 ${TARGET_BRANCH} 的 CHANGELOG.md 新增该段落（release 日志需要人工整理，脚本不会自动生成）
EOF
		exit 1
	fi
}

check_version_state() {
	if [ "$RESUME" = "1" ]; then
		log "resume 模式：跳过目标分支版本状态强校验"
		return 0
	fi
	local target_pom target_tag
	target_pom=$(pom_version_at "$TARGET_REF")
	target_tag=$(read_tag "$MODE" "$TARGET_REF")
	if [ "$target_pom" != "$OLD_VERSION" ]; then
		die "${TARGET_REF} 的 pom.xml 版本是 ${target_pom}，期望 ${OLD_VERSION}；请确认分支/版本文件状态"
	fi
	if [ "$target_tag" != "$OLD_VERSION" ]; then
		die "${TARGET_REF} 的 script/tag.${MODE}.txt 是 ${target_tag}，期望 ${OLD_VERSION}"
	fi
}

check_docs_state() {
	[ "$DOCS" = "1" ] || return 0
	local tag_file="${DOCS_DIR}/docs/.vuepress/public/docs/versions.${MODE}.tag"
	[ -f "$tag_file" ] || die "docs 版本文件不存在：${tag_file}"
	local docs_version
	docs_version=$(tr -d ' \r\n' <"$tag_file")
	if [ "$docs_version" = "$VERSION" ]; then
		log "docs 已是 ${VERSION}，跳过版本一致性校验（resume）"
		return 0
	fi
	if [ "$docs_version" != "$OLD_VERSION" ]; then
		die "docs 的 ${MODE} 版本是 ${docs_version}，期望 ${OLD_VERSION}；请先同步 docs 仓库"
	fi
}

check_remote_url() {
	local dir=$1
	local label=$2
	local pattern=$3
	local url
	url=$(git -C "$dir" remote get-url "$GIT_REMOTE" 2>/dev/null || true)
	[ -n "$url" ] || die "${label} 未配置 remote ${GIT_REMOTE}"
	if ! echo "$url" | grep -E "$pattern" >/dev/null; then
		if [ "$ALLOW_NON_GITEE" = "1" ]; then
			warn "${label} remote 不是预期地址（--allow-non-gitee）：${url}"
		else
			die "${label} remote 不是预期地址：${url}（期望匹配 ${pattern}）"
		fi
	fi
}

run_sensitive_check() {
	[ "$SENSITIVE" = "1" ] || {
		warn "已跳过敏感信息检查（--skip-sensitive）"
		return 0
	}
	[ -f "$SENSITIVE_SCRIPT" ] || die "缺少敏感信息检查脚本：${SENSITIVE_SCRIPT}"
	local args=(--all)
	if [ -n "${SENSITIVE_RANGE:-}" ]; then
		args+=(--range "$SENSITIVE_RANGE")
	fi
	log "敏感信息检查：bash script/check-sensitive.sh ${args[*]}"
	if ! bash "$SENSITIVE_SCRIPT" "${args[@]}"; then
		die "敏感信息检查未通过，请先处理告警"
	fi
}

phase_preflight() {
	section "预检"

	local cmd
	for cmd in git awk sed grep find unzip tar python3 chmod stat; do
		need_cmd "$cmd"
	done
	if [ "$UPLOAD" = "1" ] && phase_will_run upload; then
		if [ "$R2CI" = "1" ]; then
			need_cmd gh
			need_cmd curl
			gh auth status >/dev/null 2>&1 ||
				die "gh 未登录 GitHub（--with-r2-ci 需要触发 GitHub Actions）；可 gh auth login 或设置 GH_TOKEN"
			[ -f "${ROOT_DIR}/script/release-r2-ci.sh" ] || die "缺少 CI 触发脚本：script/release-r2-ci.sh"
			[ -f "${ROOT_DIR}/script/verify-release-artifacts.sh" ] || die "缺少产物校验脚本：script/verify-release-artifacts.sh"
		else
			need_cmd rclone
			need_cmd curl
			[ -f "$R2_SCRIPT" ] || die "缺少 R2 上传脚本：${R2_SCRIPT}"
			rclone listremotes 2>/dev/null | grep -x "${R2_REMOTE}:" >/dev/null || die "rclone remote 不存在：${R2_REMOTE}"
		fi
	fi
	if [ "$DOCKER" = "1" ] && phase_will_run docker; then
		need_cmd curl
		if [ "$DOCKER_MODE" = "local" ]; then
			need_cmd docker
			docker buildx version >/dev/null 2>&1 ||
				die "docker buildx 不可用（--docker-local 本机构建需要 buildx；也可去掉 --docker-local 走 GitHub Actions）"
		else
			need_cmd gh
			gh auth status >/dev/null 2>&1 ||
				die "gh 未登录 GitHub：请先执行 gh auth login（本机凭据在 GNOME Keyring），或设置 GH_TOKEN（CI 环境需 repo/workflow 权限）；--docker-local 可改走本机 buildx"
			[ -f "${PARENT_DIR}/.github/workflows/${DOCKER_WORKFLOW}" ] ||
				warn "未找到 ${PARENT_DIR}/.github/workflows/${DOCKER_WORKFLOW}；该工作流需先合并到 GitHub 仓库 ${GH_REPO} 的默认分支 master 才能 dispatch"
		fi
	fi
	if [ "$POST" = "1" ] && phase_will_run post; then
		need_cmd curl
		[ -f "${ROOT_DIR}/script/release-post.sh" ] || die "缺少 script/release-post.sh"
		command -v gh >/dev/null 2>&1 || warn "未安装 gh：GitHub Release 步骤会被跳过（见 script/release-post.sh 说明）"
		command -v secret-tool >/dev/null 2>&1 || warn "未安装 secret-tool：Gitee 令牌需用 GITEE_TOKEN 环境变量提供"
	fi

	git_clean_or_die "$PARENT_DIR" "jpom-parent"
	if [ "$DOCS" = "1" ] && phase_will_run docs; then
		git_clean_or_die "$DOCS_DIR" "docs"
	fi
	gitp config user.name >/dev/null 2>&1 || die "jpom-parent 未配置 git user.name"
	gitp config user.email >/dev/null 2>&1 || die "jpom-parent 未配置 git user.email"

	check_branch_exists "$TARGET_BRANCH"
	check_branch_exists "$SOURCE_BRANCH"
	gitp rev-parse --verify --quiet "refs/heads/${SOURCE_BRANCH}" >/dev/null ||
		die "本地没有源分支 ${SOURCE_BRANCH}，请先 checkout"

	if [ "$PUSH" = "1" ] && phase_will_run push; then
		check_remote_url "$PARENT_DIR" "jpom-parent" "gitee\.com"
		check_sync "$TARGET_BRANCH"
		check_sync "$SOURCE_BRANCH"
	else
		warn "本次不会执行推送，跳过远程地址/同步检查"
	fi
	if [ "$DOCS" = "1" ] && [ "$PUSH" = "1" ] && phase_will_run docs; then
		check_remote_url "$DOCS_DIR" "docs" "jpom-docs|github\.com"
	fi

	check_changelog_section
	check_version_state
	if [ "$DOCS" = "1" ] && phase_will_run docs; then
		check_docs_state
	fi

	run_sensitive_check

	cat <<EOF

----------------------------------------------------------------
发版类型 : ${MODE}
源分支   : ${SOURCE_BRANCH} (${SOURCE_REF})
目标分支 : ${TARGET_BRANCH} (${TARGET_REF})
旧版本   : ${OLD_VERSION}
新版本   : ${VERSION}
分支 tag : $( [ "$MODE" = "release" ] && echo "${TAG}" || echo "无（beta 不打 tag）" )
日期     : ${RELEASE_DATE}
日志目录 : ${LOG_DIR}
推送     : $( [ "$PUSH" = "1" ] && echo "是（Gitee ${GIT_REMOTE}）" || echo "否" )
R2 上传  : $( if [ "$UPLOAD" = "0" ]; then echo "否"; elif [ "$R2CI" = "1" ]; then echo "是（GitHub Actions 构建+上传${R2_CI_PREFIX:+，前缀 ${R2_CI_PREFIX}}）"; else echo "是（${R2_REMOTE}）"; fi )
文档     : $( [ "$DOCS" = "1" ] && echo "是" || echo "否" )
Docker   : $(docker_summary)
----------------------------------------------------------------
EOF
}

# ---------------------------------------------------------------------------
# 合并
# ---------------------------------------------------------------------------
resolve_changelog_conflicts() {
	local unmerged f
	unmerged=$(gitp diff --name-only --diff-filter=U)
	[ -n "$unmerged" ] || return 0
	while IFS= read -r f; do
		[ -n "$f" ] || continue
		case "$f" in
		CHANGELOG-BETA.md | CHANGELOG.md) ;;
		*) die "存在脚本无法自动解决的冲突文件：${f}" ;;
		esac
	done <<<"$unmerged"

	for f in $unmerged; do
		log "自动合并 CHANGELOG 冲突：${f}（保留已有日期）"
		gitp show ":2:${f}" >"${LOG_DIR}/ours.${f//\//_}"
		gitp show ":3:${f}" >"${LOG_DIR}/theirs.${f//\//_}"
		python3 - "${LOG_DIR}/ours.${f//\//_}" "${LOG_DIR}/theirs.${f//\//_}" >"${LOG_DIR}/resolved.${f//\//_}" <<'PY'
import re
import sys

ours = open(sys.argv[1], encoding="utf-8").read()
theirs = open(sys.argv[2], encoding="utf-8").read()

heading = re.compile(
	r"^## ([0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z]+)?)(?: \((\d{4}-\d{2}-\d{2})\))?[ \t]*$",
	re.MULTILINE,
)

ours_keys = set()
dates = {}
for match in heading.finditer(ours):
	key = match.group(1)
	ours_keys.add(key)
	if match.group(2):
		dates[key] = match.group(2)

theirs_keys = {match.group(1) for match in heading.finditer(theirs)}
missing = ours_keys - theirs_keys
if missing:
	print("目标分支独有的 CHANGELOG 段落无法自动合并：" + ", ".join(sorted(missing)), file=sys.stderr)
	sys.exit(3)


def replace(match):
	key = match.group(1)
	date = match.group(2) or dates.get(key)
	return "## " + key + (f" ({date})" if date else "") + "\n"


output = heading.sub(replace, theirs)
for marker in ("<<<<<<<", "=======", ">>>>>>>"):
	if marker in output:
		print("合并结果仍包含冲突标记", file=sys.stderr)
		sys.exit(4)
sys.stdout.write(output)
PY
		mv "${LOG_DIR}/resolved.${f//\//_}" "${PARENT_DIR}/${f}"
		gitp add "$f"
	done
}

checkout_target_branch() {
	local current
	current=$(gitp rev-parse --abbrev-ref HEAD)
	if [ "$current" = "$TARGET_BRANCH" ]; then
		return 0
	fi
	if gitp rev-parse --verify --quiet "refs/heads/${TARGET_BRANCH}" >/dev/null; then
		run_in "$PARENT_DIR" git checkout "$TARGET_BRANCH"
	else
		gitp rev-parse --verify --quiet "refs/remotes/${GIT_REMOTE}/${TARGET_BRANCH}" >/dev/null ||
			die "本地与远程都没有目标分支：${TARGET_BRANCH}"
		run_in "$PARENT_DIR" git checkout -b "$TARGET_BRANCH" --track "${GIT_REMOTE}/${TARGET_BRANCH}"
	fi
}

phase_merge() {
	section "合并 ${SOURCE_BRANCH} -> ${TARGET_BRANCH}"
	checkout_target_branch

	if gitp merge-base --is-ancestor "$SOURCE_BRANCH" "$TARGET_BRANCH"; then
		log "${SOURCE_BRANCH} 已包含在 ${TARGET_BRANCH} 中，跳过合并"
		return 0
	fi

	log "执行合并（--no-ff）"
	local merge_rc=0
	set +e
	GIT_MERGE_AUTOEDIT=no gitp merge --no-ff --no-edit "$SOURCE_BRANCH"
	merge_rc=$?
	set -e
	if [ "$merge_rc" -ne 0 ]; then
		local unmerged
		unmerged=$(gitp diff --name-only --diff-filter=U)
		if [ -n "$unmerged" ]; then
			log "检测到冲突："
			echo "$unmerged" >&2
			resolve_changelog_conflicts
			GIT_MERGE_AUTOEDIT=no gitp commit --no-edit || {
				gitp merge --abort || true
				die "合并冲突自动解决后提交失败"
			}
		else
			gitp merge --abort || true
			die "git merge 失败（无冲突文件），请手工检查"
		fi
	fi
	gitp rev-parse -q --verify MERGE_HEAD >/dev/null && die "合并未完成，仍存在 MERGE_HEAD"
	log "合并完成：$(gitp log -1 --pretty='%h %s')"

	local target_pom target_tag
	target_pom=$(pom_version_worktree)
	target_tag=$(read_tag "$MODE" "HEAD")
	[ "$target_pom" = "$OLD_VERSION" ] || die "合并后 pom.xml 版本为 ${target_pom}，期望 ${OLD_VERSION}（版本文件状态异常）"
	[ "$target_tag" = "$OLD_VERSION" ] || die "合并后 tag.${MODE}.txt 为 ${target_tag}，期望 ${OLD_VERSION}"
}

# ---------------------------------------------------------------------------
# CHANGELOG 日期
# ---------------------------------------------------------------------------
phase_changelog() {
	section "CHANGELOG 日期"

	local file ref expected
	if [ "$MODE" = "beta" ]; then
		file="CHANGELOG-BETA.md"
		ref="HEAD"
	else
		file="CHANGELOG.md"
		ref="HEAD"
	fi

	local result
	result=$(python3 - "${PARENT_DIR}/${file}" "$VERSION" "$VERSION_SUFFIX" "$RELEASE_DATE" <<'PY'
import re
import sys

path, version, suffix, date = sys.argv[1:5]
text = open(path, encoding="utf-8").read()
heading = re.compile(
	r"^## " + re.escape(version) + "-" + re.escape(suffix) + r"(?: \([0-9]{4}-[0-9]{2}-[0-9]{2}\))?[ \t]*$",
	re.MULTILINE,
)
matches = heading.findall(text)
if not matches:
	print("MISSING")
	sys.exit(1)
replacement = f"## {version}-{suffix} ({date})"


def replace(match):
	return replacement


output = heading.sub(replace, text, count=1)
if output == text:
	print("UNCHANGED")
else:
	open(path, "w", encoding="utf-8").write(output)
	print("CHANGED")
PY
) || die "${file} 中缺少 ## ${VERSION}-${VERSION_SUFFIX} 段落，无法补日期"

	case "$result" in
	UNCHANGED)
		log "${file} 已包含日期 (${RELEASE_DATE})"
		;;
	CHANGED)
		log "${file} 已补日期 (${RELEASE_DATE})"
		gitp add "$file"
		local message
		if [ "$MODE" = "beta" ]; then
			message="releases(发布): 补充 ${VERSION} 发布日期"
		else
			message="releases(发布): 准备release ${VERSION}"
		fi
		gitp commit -m "$message"
		;;
	*)
		die "无法解析 ${file} 日期处理结果：${result}"
		;;
	esac
}

# ---------------------------------------------------------------------------
# 版本号替换
# ---------------------------------------------------------------------------
sha1_of() {
	local file=$1
	$SHA1_CMD "$file" | awk '{print $1}'
}

cleanup_version_backups() {
	find "$PARENT_DIR" -type f \( -name '*.bak' -o -name 'pom.xml.versionsBackup' \) -print -delete 2>/dev/null || true
	find "$PARENT_DIR" -type f -name 'temp-docs.log' -delete 2>/dev/null || true
}

grep_file() {
	local pattern=$1
	local file=$2
	grep -q -- "$pattern" "$file" 2>/dev/null
}

verify_version_files() {
	section "校验版本号替换结果"
	local errors=0
	local tag_file="${PARENT_DIR}/script/tag.${MODE}.txt"
	if [ "$(tr -d ' \r\n' <"$tag_file")" != "$VERSION" ]; then
		echo "FAIL tag.${MODE}.txt 不是 ${VERSION}" >&2
		errors=$((errors + 1))
	fi
	if [ "$(pom_version_worktree)" != "$VERSION" ]; then
		echo "FAIL pom.xml 版本不是 ${VERSION}" >&2
		errors=$((errors + 1))
	fi
	local old_poms
	old_poms=$(grep -R --include=pom.xml --exclude-dir=target --exclude-dir=.git -l "<version>${OLD_VERSION}</version>" "$PARENT_DIR" 2>/dev/null || true)
	if [ -n "$old_poms" ]; then
		echo "FAIL 仍残留旧版本 ${OLD_VERSION} 的 pom.xml：" >&2
		echo "$old_poms" >&2
		errors=$((errors + 1))
	fi

	if [ "$MODE" = "beta" ]; then
		local checks=(
			"${PARENT_DIR}/env-beta.env|JPOM_VERSION=${VERSION}"
			"${PARENT_DIR}/modules/server/DockerfileBeta|${VERSION}"
			"${PARENT_DIR}/modules/server/DockerfileBetaJdk17|${VERSION}"
			"${PARENT_DIR}/modules/agent/DockerfileBeta|${VERSION}"
			"${PARENT_DIR}/modules/common/src/main/resources/banner.txt|v${VERSION}"
		)
		local item file pattern
		for item in "${checks[@]}"; do
			file=${item%%|*}
			pattern=${item#*|}
			if ! grep_file "$pattern" "$file"; then
				echo "FAIL ${file} 不包含 ${pattern}" >&2
				errors=$((errors + 1))
			fi
		done
		if [ "$(grep -m1 '"version"' "${PARENT_DIR}/web-vue/package.json" | sed -E 's/.*"version"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/')" != "$OLD_STABLE" ]; then
			warn "web-vue/package.json 在 beta 模式下不随版本替换（历史行为），当前值请确认"
		fi
	else
		local checks=(
			"${PARENT_DIR}/.env|JPOM_VERSION=${VERSION}"
			"${PARENT_DIR}/env-release.env|JPOM_VERSION=${VERSION}"
			"${PARENT_DIR}/modules/server/Dockerfile|${VERSION}"
			"${PARENT_DIR}/modules/server/DockerfileRelease|${VERSION}"
			"${PARENT_DIR}/modules/server/DockerfileReleaseJdk17|${VERSION}"
			"${PARENT_DIR}/modules/agent/Dockerfile|${VERSION}"
			"${PARENT_DIR}/modules/agent/DockerfileRelease|${VERSION}"
			"${PARENT_DIR}/script/docker.sh|${VERSION}"
			"${PARENT_DIR}/web-vue/package.json|${VERSION}"
			"${PARENT_DIR}/.workflow/MasterPipeline.yml|${VERSION}"
			"${PARENT_DIR}/modules/common/src/main/resources/banner.txt|v${VERSION}"
		)
		local item file pattern
		for item in "${checks[@]}"; do
			file=${item%%|*}
			pattern=${item#*|}
			if ! grep_file "$pattern" "$file"; then
				echo "FAIL ${file} 不包含 ${pattern}" >&2
				errors=$((errors + 1))
			fi
		done
	fi

	if [ "$errors" -ne 0 ]; then
		gitp diff --stat >&2 || true
		die "版本号替换校验失败（${errors} 项）"
	fi
	log "版本号替换校验通过"
}

phase_version() {
	section "替换版本号 -> ${VERSION}"

	local current
	current=$(pom_version_worktree)
	if [ "$current" = "$VERSION" ]; then
		log "版本号已是 ${VERSION}，跳过替换"
	else
		log "调用 jpom-parent/script/replaceVersion.sh ${VERSION} ${MODE}"
		(
			cd "${PARENT_DIR}/script"
			export JAVA_HOME
			bash ./replaceVersion.sh "$VERSION" "$MODE"
		)
	fi
	cleanup_version_backups
	verify_version_files
}

# ---------------------------------------------------------------------------
# 构建
# ---------------------------------------------------------------------------
phase_build() {
	section "构建前端与后端"
	if [ "$R2CI" = "1" ]; then
		log "已启用 --with-r2-ci：构建由 GitHub Actions release-package 工作流执行，跳过本地构建"
		return 0
	fi
	if [ "$BUILD" = "0" ]; then
		warn "已跳过构建（--skip-build），将使用 target 下已有产物"
		return 0
	fi

	mkdir -p "$NPM_CACHE_DIR"
	local npm_install_args=(--no-audit --no-fund --cache "$NPM_CACHE_DIR")
	if [ -n "${NPM_ARGS:-}" ]; then
		read -r -a extra <<<"$NPM_ARGS"
		npm_install_args+=("${extra[@]}")
	fi
	if [ "$OFFLINE" = "1" ]; then
		npm_install_args+=(--prefer-offline)
	fi
	run_in "${PARENT_DIR}/web-vue" "$NPM_BIN" install "${npm_install_args[@]}"
	run_in "${PARENT_DIR}/web-vue" "$NPM_BIN" run build
	[ -f "${PARENT_DIR}/modules/server/src/main/resources/dist/index.html" ] ||
		die "前端构建产物缺失：modules/server/src/main/resources/dist/index.html"

	local mvn_args=(-B -Dmaven.test.skip=true)
	if [ "$OFFLINE" = "1" ]; then
		mvn_args+=(-o)
	fi
	if [ -n "${MVN_ARGS:-}" ]; then
		read -r -a extra <<<"$MVN_ARGS"
		mvn_args+=("${extra[@]}")
	fi
	run_in "$PARENT_DIR" "$MVN_BIN" clean package "${mvn_args[@]}"
}

# ---------------------------------------------------------------------------
# 回归单元测试
# ---------------------------------------------------------------------------
phase_tests() {
	section "回归单元测试"
	if [ "$TESTS" = "0" ]; then
		warn "已跳过回归单元测试（--skip-tests，不推荐）"
		return 0
	fi
	# 根 pom 默认 maven.test.skip=true，普通 package 不会跑单测；这里强制打开并运行核心回归集
	local env_args=(MVN="$MVN_BIN")
	if [ "$OFFLINE" = "1" ]; then
		env_args+=(JPOM_MVN_OFFLINE=1)
	fi
	run_in "$PARENT_DIR" env "${env_args[@]}" bash ./script/run-regression-tests.sh
}

# ---------------------------------------------------------------------------
# 产物完整性校验
# ---------------------------------------------------------------------------
verify_artifacts() {
	section "产物完整性校验"
	mkdir -p "${LOG_DIR}/artifacts"
	bash "${ROOT_DIR}/script/verify-release-artifacts.sh" \
		--parent-dir "$PARENT_DIR" --version "$VERSION" --log-dir "${LOG_DIR}/artifacts"
}

phase_verify() {
	if [ "$R2CI" = "1" ]; then
		log "已启用 --with-r2-ci：产物完整性校验将在 CI 上传返回后、对下载的产物执行"
		return 0
	fi
	if [ "$VERIFY" = "0" ]; then
		warn "已跳过产物完整性校验（--skip-verify）"
		return 0
	fi
	local attempt=0
	while true; do
		if (verify_artifacts); then
			return 0
		fi
		if [ "$BUILD" = "1" ] && [ "$attempt" -lt "$BUILD_RETRIES" ]; then
			attempt=$((attempt + 1))
			warn "产物校验失败，自动重跑构建（${attempt}/${BUILD_RETRIES}）"
			phase_build
		else
			die "产物完整性校验失败；请重跑 mvn clean package（本机曾出现偶发坏包）"
		fi
	done
}

# ---------------------------------------------------------------------------
# 启动冒烟
# ---------------------------------------------------------------------------
free_port() {
	python3 - <<'PY'
import socket
s = socket.socket()
s.bind(("127.0.0.1", 0))
print(s.getsockname()[1])
s.close()
PY
}

wait_http_200() {
	local url=$1
	local timeout=${2:-120}
	local elapsed=0
	local code=""
	while [ "$elapsed" -lt "$timeout" ]; do
		code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "$url" 2>/dev/null || true)
		if [ "$code" = "200" ]; then
			return 0
		fi
		sleep 1
		elapsed=$((elapsed + 1))
	done
	return 1
}

smoke_server() {
	local ver=$1
	local root="${LOG_DIR}/smoke"
	local base="${root}/server"
	local logs="${root}/server-logs"
	local data="${root}/server-data"
	mkdir -p "$logs" "$data"
	cp -a "${PARENT_DIR}/modules/server/target/server-${ver}-release" "$base"
	local port
	port=$(free_port)
	log "启动服务端冒烟测试：port=${port}"
	"$JAVA_BIN" -Djava.awt.headless=true -Dfile.encoding=UTF-8 \
		-DJPOM_LOG="$logs" \
		-Dlogging.config="${base}/conf/logback.xml" \
		-Dspring.config.location="${base}/conf/application.yml" \
		-jar "${base}/lib/server-${ver}.jar" \
		--server.port="$port" --jpom.path="$data" >"${logs}/server.out" 2>&1 &
	SMOKE_SERVER_PID=$!
	if ! wait_http_200 "http://127.0.0.1:${port}/" 180; then
		tail -50 "${logs}/server.out" >&2 || true
		die "服务端冒烟启动失败（port=${port}），日志：${logs}"
	fi
	local body asset status
	body=$(curl -s --max-time 10 "http://127.0.0.1:${port}/")
	echo "$body" | grep 'Jpom' >/dev/null || die "服务端首页内容异常"
	echo "$body" | grep '<title>' >/dev/null || die "服务端首页缺少 title"
	asset=$(echo "$body" | grep -oE 'assets/js/index\.[A-Za-z0-9_-]+\.js' | head -1 || true)
	if [ -n "$asset" ]; then
		status=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "http://127.0.0.1:${port}/${asset}" || true)
		[ "$status" = "200" ] || die "服务端前端静态资源访问失败：${asset} (${status})"
	fi
	kill "$SMOKE_SERVER_PID" 2>/dev/null || true
	wait "$SMOKE_SERVER_PID" 2>/dev/null || true
	SMOKE_SERVER_PID=""
	log "服务端冒烟通过"
}

smoke_agent() {
	local ver=$1
	local root="${LOG_DIR}/smoke"
	local base="${root}/agent"
	local logs="${root}/agent-logs"
	local data="${root}/agent-data"
	mkdir -p "$logs" "$data"
	cp -a "${PARENT_DIR}/modules/agent/target/agent-${ver}-release" "$base"
	local port
	port=$(free_port)
	log "启动插件端冒烟测试：port=${port}"
	"$JAVA_BIN" -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true \
		-DJPOM_LOG="$logs" \
		-Dlogging.config="${base}/conf/logback.xml" \
		-Dspring.config.location="${base}/conf/application.yml" \
		-jar "${base}/lib/agent-${ver}.jar" \
		--server.port="$port" --jpom.path="$data" >"${logs}/agent.out" 2>&1 &
	SMOKE_AGENT_PID=$!
	if ! wait_http_200 "http://127.0.0.1:${port}/" 180; then
		tail -50 "${logs}/agent.out" >&2 || true
		die "插件端冒烟启动失败（port=${port}），日志：${logs}"
	fi
	local resp
	resp=$(curl -s --max-time 10 -X POST "http://127.0.0.1:${port}/info")
	echo "$resp" | grep '"code":900' >/dev/null || die "插件端 /info 未返回预期的 900：${resp:0:200}"
	local json="${data}/data/agent_authorize.json"
	[ -f "$json" ] || die "插件端未生成 ${json}"
	local mode
	mode=$(stat -c '%a' "$json")
	[ "$mode" = "600" ] || die "插件端授权文件权限为 ${mode}，期望 600"
	local pwd
	pwd=$(python3 - "$json" <<'PY'
import json
import sys
print(json.load(open(sys.argv[1], encoding="utf-8")).get("agentPwd", ""))
PY
)
	if [ -n "$pwd" ] && grep -qF "$pwd" "${logs}/agent.log" 2>/dev/null; then
		die "插件端日志中出现了明文授权密码"
	fi
	kill "$SMOKE_AGENT_PID" 2>/dev/null || true
	wait "$SMOKE_AGENT_PID" 2>/dev/null || true
	SMOKE_AGENT_PID=""
	log "插件端冒烟通过"
}

phase_smoke() {
	if [ "$R2CI" = "1" ] && [ "$R2CI_SMOKE_READY" != "1" ]; then
		log "已启用 --with-r2-ci：启动冒烟将在 CI 上传返回后、对下载的产物执行"
		return 0
	fi
	if [ "$SMOKE" = "0" ]; then
		warn "已跳过启动冒烟（--skip-smoke）"
		return 0
	fi
	section "启动冒烟"
	rm -rf "${LOG_DIR}/smoke"
	mkdir -p "${LOG_DIR}/smoke"
	smoke_server "$VERSION"
	smoke_agent "$VERSION"
	log "冒烟测试通过（日志：${LOG_DIR}/smoke）"
	if [ "$KEEP_TEMP" = "0" ]; then
		rm -rf "${LOG_DIR}/smoke/server" "${LOG_DIR}/smoke/agent" \
			"${LOG_DIR}/smoke/server-data" "${LOG_DIR}/smoke/agent-data"
	fi
}

# ---------------------------------------------------------------------------
# 提交 / 推送
# ---------------------------------------------------------------------------
allowed_release_file() {
	case "$1" in
	pom.xml | */pom.xml) return 0 ;;
	CHANGELOG-BETA.md | CHANGELOG.md) return 0 ;;
	script/tag.beta.txt | script/tag.release.txt) return 0 ;;
	script/docker.sh | .workflow/MasterPipeline.yml) return 0 ;;
	.env | env-beta.env | env-release.env) return 0 ;;
	web-vue/package.json) return 0 ;;
	modules/common/src/main/resources/banner.txt) return 0 ;;
	modules/*/Dockerfile*) return 0 ;;
	*) return 1 ;;
	esac
}

phase_commit() {
	section "提交发版代码"

	local status
	status=$(gitp status --porcelain --untracked-files=normal)
	if [ -z "$status" ]; then
		log "工作区干净，无需提交"
		if [ "$MODE" = "release" ] && ! gitp rev-parse --verify --quiet "refs/tags/${TAG}" >/dev/null; then
			gitp tag "$TAG"
			log "已创建 tag：${TAG}"
		fi
		return 0
	fi

	local untracked
	untracked=$(gitp status --porcelain --untracked-files=normal | awk '$1 == "??" {print $2}')
	if [ -n "$untracked" ]; then
		echo "$untracked" >&2
		die "存在未跟踪文件，拒绝自动提交；请检查后再运行"
	fi

	gitp add -A
	local staged
	staged=$(gitp diff --cached --name-only)
	[ -n "$staged" ] || die "没有可提交的变更"
	local f
	while IFS= read -r f; do
		[ -n "$f" ] || continue
		if ! allowed_release_file "$f"; then
			gitp reset -q
			die "提交内容包含非发版文件：${f}（已取消暂存，请人工检查）"
		fi
	done <<<"$staged"

	local message
	if [ "$MODE" = "beta" ]; then
		message="commit beta ${VERSION}"
	else
		message="commit release ${VERSION}"
	fi
	gitp commit -m "$message"
	log "提交完成：$(gitp log -1 --pretty='%h %s')"

	if [ "$MODE" = "release" ]; then
		if gitp rev-parse --verify --quiet "refs/tags/${TAG}" >/dev/null; then
			die "tag ${TAG} 已存在"
		fi
		gitp tag "$TAG"
		log "已创建 tag：${TAG}"
	fi
}

phase_push() {
	section "推送 Gitee"
	if [ "$PUSH" = "0" ]; then
		warn "已跳过推送（--no-push）"
		return 0
	fi

	local head
	head=$(gitp rev-parse HEAD)
	log "待推送提交：$(gitp log -1 --pretty='%h %s')"
	gitp status --short --branch || true
	confirm "确认推送 ${TARGET_BRANCH} $( [ "$MODE" = "release" ] && echo "和 tag ${TAG} " )到 Gitee？"

	SENSITIVE_RANGE=""
	if gitp rev-parse --verify --quiet "refs/remotes/${GIT_REMOTE}/${TARGET_BRANCH}" >/dev/null; then
		SENSITIVE_RANGE="${GIT_REMOTE}/${TARGET_BRANCH}..HEAD"
	fi
	run_sensitive_check
	SENSITIVE_RANGE=""

	gitp push "$GIT_REMOTE" "$TARGET_BRANCH"
	if [ "$MODE" = "release" ]; then
		gitp push "$GIT_REMOTE" "refs/tags/${TAG}"
	fi

	local remote_head
	remote_head=$(gitp ls-remote "$GIT_REMOTE" "refs/heads/${TARGET_BRANCH}" | awk '{print $1}')
	[ "$remote_head" = "$head" ] || die "远程 ${TARGET_BRANCH} 未指向本地 HEAD（${remote_head:-空} != ${head}）"
	if [ "$MODE" = "release" ]; then
		local remote_tag
		remote_tag=$(gitp ls-remote "$GIT_REMOTE" "refs/tags/${TAG}" | awk '{print $1}')
		[ -n "$remote_tag" ] || die "远程 tag ${TAG} 不存在"
	fi
	PUSHED=1
	log "推送完成"
}

# ---------------------------------------------------------------------------
# R2
# ---------------------------------------------------------------------------
phase_upload() {
	section "上传 Cloudflare R2"
	if [ "$UPLOAD" = "0" ]; then
		warn "已跳过 R2 上传（--no-upload）"
		return 0
	fi
	if [ "${PUSHED:-0}" != "1" ]; then
		die "未推送代码就禁止上传制品；如确需跳过请使用 --no-upload"
	fi
	if [ "$R2CI" = "1" ]; then
		local ref="${R2_CI_REF:-}"
		if [ -z "$ref" ]; then
			if [ "$MODE" = "release" ]; then
				ref="$TAG"
			else
				ref="$TARGET_BRANCH"
			fi
		fi
		local ci_args=(--channel "$MODE" --version "$VERSION" --ref "$ref" --repo "$GH_REPO"
			--wait-timeout "$R2_CI_TIMEOUT" --download-dir "$PARENT_DIR")
		[ -n "$R2_CI_PREFIX" ] && ci_args+=(--prefix "$R2_CI_PREFIX")
		[ "$R2_CI_WAIT" = "0" ] && ci_args+=(--no-wait)
		[ "$DEEP_VERIFY" = "1" ] && ci_args+=(--deep-verify)
		confirm "确认触发 GitHub Actions 打包并上传 R2（${GH_REPO} ${ref}，前缀${R2_CI_PREFIX:-无}）？"
		log "bash script/release-r2-ci.sh ${ci_args[*]}"
		if ! bash "${ROOT_DIR}/script/release-r2-ci.sh" "${ci_args[@]}"; then
			die "GitHub Actions 打包/上传失败；修复后可运行：bash script/release.sh ${MODE} --version ${VERSION} --from upload --with-r2-ci"
		fi
		if [ "$R2_CI_WAIT" = "0" ]; then
			warn "已触发 GitHub Actions（--no-r2-ci-wait），未下载/校验产物；docs/R2 后续步骤需要人工跟进"
			return 0
		fi
		UPLOADED=1
		if [ "$VERIFY" = "1" ]; then
			section "校验 CI 下载回的产物"
			bash "${ROOT_DIR}/script/verify-release-artifacts.sh" \
				--parent-dir "$PARENT_DIR" --version "$VERSION" --log-dir "$LOG_DIR" --downloaded ||
				die "CI 下载回的产物完整性校验失败"
		fi
		if [ "$SMOKE" = "1" ]; then
			R2CI_SMOKE_READY=1
			phase_smoke
		fi
		return 0
	fi
	local args=("$MODE")
	if [ "$FORCE_UPLOAD" = "1" ]; then args+=(--force); fi
	if [ "$DEEP_VERIFY" = "1" ]; then args+=(--deep-verify); fi
	confirm "确认上传 ${MODE}/${VERSION} 到 R2（${R2_REMOTE}）？"
	log "bash script/deploy-release-r2.sh ${args[*]}"
	if ! bash "$R2_SCRIPT" "${args[@]}"; then
		die "R2 上传失败；修复后可运行：bash script/release.sh ${MODE} --version ${VERSION} --from upload"
	fi
	UPLOADED=1
}

# ---------------------------------------------------------------------------
# docs
# ---------------------------------------------------------------------------
docs_changelog_file() {
	if [ "$MODE" = "beta" ]; then
		printf '%s' "${DOCS_DIR}/docs/日志&下载/01.更新日志/02.最新beta.md"
	else
		printf '%s' "${DOCS_DIR}/docs/日志&下载/01.更新日志/01.最新.md"
	fi
}

insert_docs_changelog() {
	local src="${PARENT_DIR}/CHANGELOG-BETA.md"
	local dst
	[ "$MODE" = "release" ] && src="${PARENT_DIR}/CHANGELOG.md"
	dst=$(docs_changelog_file)
	[ -f "$dst" ] || die "docs 更新日志文件不存在：${dst}"
	python3 - "$src" "$dst" "$VERSION" "$VERSION_SUFFIX" "$RELEASE_DATE" <<'PY'
import re
import sys

src_path, dst_path, version, suffix, date = sys.argv[1:6]
src = open(src_path, encoding="utf-8").read()
heading = re.compile(r"^## " + re.escape(version) + "-" + re.escape(suffix) + r"(?: \([0-9]{4}-[0-9]{2}-[0-9]{2}\))?[ \t]*$", re.MULTILINE)
lines = src.splitlines()
start = None
for idx, line in enumerate(lines):
	if heading.match(line):
		start = idx
		break
if start is None:
	print(f"源 CHANGELOG 中找不到 ## {version}-{suffix}", file=sys.stderr)
	sys.exit(1)
end = len(lines)
for idx in range(start + 1, len(lines)):
	if lines[idx].strip() == "------":
		end = idx
		break
section = "\n".join(lines[start:end]).rstrip() + "\n"

dst = open(dst_path, encoding="utf-8").read()
if heading.search(dst):
	print("docs 已存在该版本文档，跳过")
	sys.exit(0)
front = ""
rest = dst
if dst.startswith("---\n"):
	end_front = dst.find("\n---\n", 4)
	if end_front != -1:
		end_front += len("\n---\n")
		front = dst[:end_front]
		rest = dst[end_front:]
new = front + "\n" + section + "\n------\n\n" + rest.lstrip("\n")
open(dst_path, "w", encoding="utf-8").write(new)
print("docs 更新日志已插入")
PY
}

phase_docs() {
	section "更新文档站"
	if [ "$DOCS" = "0" ]; then
		warn "已跳过文档更新（--no-docs）"
		return 0
	fi
	if [ "${UPLOADED:-0}" != "1" ]; then
		die "未上传 R2 就禁止发布文档（下载链接会失效）；请先完成上传"
	fi
	if ! git -C "$DOCS_DIR" rev-parse --git-dir >/dev/null 2>&1; then
		die "docs 子模块不存在：${DOCS_DIR}"
	fi
	git_clean_or_die "$DOCS_DIR" "docs"
	if [ "$PUSH" = "1" ]; then
		check_remote_url "$DOCS_DIR" "docs" "jpom-docs|github\.com"
	fi

	local tag_file="${DOCS_DIR}/docs/.vuepress/public/docs/versions.${MODE}.tag"
	local current
	current=$(tr -d ' \r\n' <"$tag_file")
	if [ "$current" = "$VERSION" ]; then
		log "docs 版本已是 ${VERSION}，跳过 replaceVersion"
	else
		log "调用 docs/script/replaceVersion.sh ${VERSION} ${MODE}"
		(
			cd "${DOCS_DIR}/script"
			bash ./replaceVersion.sh "$VERSION" "$MODE"
		)
	fi
	rm -f "${DOCS_DIR}/temp-docs.log"
	find "$DOCS_DIR" -type f -name '*.bak' -delete 2>/dev/null || true

	insert_docs_changelog

	# 校验 JSON 文件
	local json_files=()
	if [ "$MODE" = "beta" ]; then
		json_files=("${DOCS_DIR}/docs/.vuepress/public/docs/beta-versions.json")
	else
		json_files=(
			"${DOCS_DIR}/docs/.vuepress/public/docs/versions.json"
			"${DOCS_DIR}/docs/.vuepress/public/docs/release-versions.json"
		)
	fi
	local json
	for json in "${json_files[@]}"; do
		[ -f "$json" ] || die "docs JSON 文件不存在：${json}"
		python3 -m json.tool "$json" >/dev/null || die "docs JSON 格式错误：${json}"
	done

	gitd add -A
	local staged
	staged=$(gitd -c core.quotepath=false diff --cached --name-only)
	if [ -n "$staged" ]; then
		local f
		while IFS= read -r f; do
			[ -n "$f" ] || continue
			case "$f" in
			docs/.vuepress/public/docs/* | docs/.vuepress/public/assets/js/* | docs/日志\&下载/* | package.json) ;;
			*) gitd reset -q; die "docs 提交包含非预期文件：${f}" ;;
			esac
		done <<<"$staged"
		gitd commit -m "commit ${VERSION} update docs pages"
		log "docs 提交完成：$(gitd log -1 --pretty='%h %s')"
	else
		log "docs 无新增变更"
	fi

	confirm "确认推送 docs main 到 ${DOCS_REMOTE}（GitHub Pages 自动发布）？"
	gitd push "$DOCS_REMOTE" main
	log "docs 已推送，等待 GitHub Actions 构建：https://github.com/bwcx-jzy/jpom-docs/actions"
}

# ---------------------------------------------------------------------------
# Docker 发布（默认 GitHub Actions；--docker-local 本机 buildx 兜底）
# ---------------------------------------------------------------------------
docker_image_tags() {
	if [ "$MODE" = "release" ]; then
		printf '%s\n' "$VERSION" "latest"
	else
		printf '%s\n' "${VERSION}-beta" "latest-beta"
	fi
}

docker_image_tags_csv() {
	docker_image_tags | tr '\n' ',' | sed 's/,$//'
}

docker_summary() {
	if [ "$DOCKER" != "1" ]; then
		echo "否"
	elif [ "$DOCKER_MODE" = "local" ]; then
		echo "是（本机 buildx，${DOCKER_PLATFORMS}）"
	else
		echo "是（GitHub Actions ${GH_REPO} ${DOCKER_REF}，${DOCKER_PLATFORMS}）"
	fi
}

resolve_docker_ref() {
	if [ -n "$DOCKER_REF_OPT" ]; then
		DOCKER_REF="$DOCKER_REF_OPT"
	elif [ "$MODE" = "release" ]; then
		DOCKER_REF="$TAG"
	else
		DOCKER_REF="$TARGET_BRANCH"
	fi
}

# 本地 ref -> commit sha（tag 优先，annotated tag 自动解引用）
docker_ci_local_sha() {
	local ref=$1 sha
	if sha=$(gitp rev-parse --verify --quiet "refs/tags/${ref}^{commit}"); then
		printf '%s' "$sha"
		return 0
	fi
	if sha=$(gitp rev-parse --verify --quiet "refs/heads/${ref}^{commit}"); then
		printf '%s' "$sha"
		return 0
	fi
	if sha=$(gitp rev-parse --verify --quiet "refs/remotes/${GIT_REMOTE}/${ref}^{commit}"); then
		printf '%s' "$sha"
		return 0
	fi
	return 1
}

# 本地 ref 是 tag（tags）还是分支（heads）
docker_ci_ref_kind() {
	local ref=$1
	if gitp rev-parse --verify --quiet "refs/tags/${ref}" >/dev/null; then
		printf 'tags'
	elif gitp rev-parse --verify --quiet "refs/heads/${ref}" >/dev/null ||
		gitp rev-parse --verify --quiet "refs/remotes/${GIT_REMOTE}/${ref}" >/dev/null; then
		printf 'heads'
	else
		return 1
	fi
}

# 等待 Gitee -> GitHub 镜像同步：远端 ref 的 commit sha 与本地一致（最多 5 分钟）
docker_ci_wait_ref_sync() {
	local ref=$1 kind local_sha deadline remote_info remote_sha remote_type
	kind=$(docker_ci_ref_kind "$ref") ||
		die "无法判断 Docker 触发 ref ${ref} 是 tag 还是分支（本地不存在）；请检查 --docker-ref"
	local_sha=$(docker_ci_local_sha "$ref") ||
		die "无法解析 ${ref} 的本地 commit"
	log "等待 GitHub ${GH_REPO} 镜像同步：${ref}（${kind}）-> ${local_sha:0:12}…（最多 5 分钟）"
	deadline=$((SECONDS + 300))
	while [ "$SECONDS" -lt "$deadline" ]; do
		remote_info=$(gh api "repos/${GH_REPO}/git/ref/${kind}/${ref}" \
			--jq '.object.sha + " " + .object.type' 2>/dev/null || true)
		if [ -n "$remote_info" ]; then
			remote_sha=${remote_info%% *}
			remote_type=${remote_info##* }
			if [ "$remote_type" = "tag" ]; then
				remote_sha=$(gh api "repos/${GH_REPO}/git/tags/${remote_sha}" \
					--jq '.object.sha' 2>/dev/null || true)
			fi
			if [ -n "$remote_sha" ] && [ "$remote_sha" = "$local_sha" ]; then
				log "GitHub 镜像已同步：${ref} -> ${local_sha:0:12}…"
				return 0
			fi
		fi
		sleep 10
	done
	die "GitHub ${GH_REPO} 上的 ${ref} 在 5 分钟内未同步到本地 commit ${local_sha}；Gitee -> GitHub 镜像可能延迟，请稍后重试（或用 --docker-ref 指定已同步的 ref；也可 --docker-local 本机兜底）"
}

# 查找本次 dispatch 产生的 run：输出 "<run_id> <html_url>"，找不到返回 1
docker_ci_find_run() {
	local dispatched_at=$1 timeout=$2 deadline info local_sha
	deadline=$((SECONDS + timeout))
	local_sha=$(docker_ci_local_sha "$DOCKER_REF" || true)
	while [ "$SECONDS" -lt "$deadline" ]; do
		info=$(gh api "repos/${GH_REPO}/actions/workflows/${DOCKER_WORKFLOW}/runs?event=workflow_dispatch&per_page=20" 2>/dev/null |
			python3 -c '
import datetime
import json
import sys

since = datetime.datetime.strptime(sys.argv[1], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=datetime.timezone.utc)
since -= datetime.timedelta(seconds=120)  # 容忍 GitHub 与本机的时钟偏差
sha, ref = sys.argv[2], sys.argv[3]
try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(0)
candidates = []
for run in data.get("workflow_runs") or []:
    try:
        created_at = datetime.datetime.strptime(
            run.get("created_at") or "", "%Y-%m-%dT%H:%M:%SZ"
        ).replace(tzinfo=datetime.timezone.utc)
    except Exception:
        continue
    if created_at < since:
        continue
    if (sha and run.get("head_sha") == sha) or run.get("head_branch") == ref:
        candidates.append((created_at, run.get("id"), run.get("html_url")))
if not candidates:
    sys.exit(0)
candidates.sort()
_, run_id, url = candidates[0]
print(f"{run_id} {url}")
' "$dispatched_at" "$local_sha" "$DOCKER_REF" || true)
		if [ -n "$info" ]; then
			printf '%s\n' "$info"
			return 0
		fi
		sleep 5
	done
	return 1
}

docker_ci_run_field() {
	local run_id=$1 field=$2
	gh api "repos/${GH_REPO}/actions/runs/${run_id}" --jq ".${field}" 2>/dev/null || true
}

# 轮询 run 直到 completed（失败 die 并给出 run URL 与排查命令）
docker_ci_wait_run() {
	local run_id=$1 deadline status conclusion
	deadline=$((SECONDS + DOCKER_WAIT_TIMEOUT * 60))
	log "等待 GitHub Actions run 完成（超时 ${DOCKER_WAIT_TIMEOUT} 分钟）：${DOCKER_RUN_URL}"
	while [ "$SECONDS" -lt "$deadline" ]; do
		status=$(docker_ci_run_field "$run_id" status)
		[ "$status" = "completed" ] && break
		sleep 15
	done
	status=$(docker_ci_run_field "$run_id" status)
	conclusion=$(docker_ci_run_field "$run_id" conclusion)
	if [ "$status" != "completed" ]; then
		die "GitHub Actions run ${run_id} 在 ${DOCKER_WAIT_TIMEOUT} 分钟内未完成（status=${status:-未知}）：${DOCKER_RUN_URL}
可稍后查看：gh run view ${run_id} --repo ${GH_REPO}；确认完成后手动核对 Docker Hub 镜像"
	fi
	if [ "$conclusion" != "success" ]; then
		die "GitHub Actions 发布 Docker 镜像失败（conclusion=${conclusion}）：${DOCKER_RUN_URL}
排查日志：gh run view ${run_id} --repo ${GH_REPO} --log-failed"
	fi
}

docker_ci_hub_tag_json() {
	local repo=$1 tag=$2 out=$3
	curl -sS --max-time 20 -o "$out" -w '%{http_code}' \
		"https://hub.docker.com/v2/repositories/${repo}/tags/${tag}/" 2>/dev/null || true
}

# 回验 Docker Hub tag 存在且包含 amd64+arm64（网络/API 不可用只 warn）
docker_ci_verify_tag() {
	local repo=$1 tag=$2 json code attempt=0 archs arch_list
	json="${LOG_DIR}/dockerhub-$(printf '%s' "${repo}_${tag}" | tr '/: ' '___').json"
	while :; do
		code=$(docker_ci_hub_tag_json "$repo" "$tag" "$json")
		[ "$code" = "200" ] && break
		if [ "$code" = "404" ] && [ "$attempt" -lt 6 ]; then
			attempt=$((attempt + 1))
			log "Docker Hub 暂未出现 ${repo}:${tag}（等待同步 ${attempt}/6）"
			sleep 10
			continue
		fi
		if [ "$code" = "404" ]; then
			die "Docker Hub 上找不到 ${repo}:${tag}（https://hub.docker.com/r/${repo}/tags）；如 CI 刚完成可稍后手动核对"
		fi
		warn "Docker Hub 回验跳过（${repo}:${tag}，HTTP ${code:-无响应}）；以 GitHub Actions 内 imagetools 回验为准"
		return 0
	done
	archs=$(python3 - "$json" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as fh:
	data = json.load(fh)
archs = []
for image in data.get("images") or []:
	arch = image.get("architecture")
	if arch and arch not in archs:
		archs.append(arch)
for arch in sorted(archs):
	print(arch)
PY
	) || {
		warn "无法解析 Docker Hub 返回（${repo}:${tag}），跳过回验"
		return 0
	}
	local expected_archs expected missing=""
	expected_archs=$(printf '%s' "$DOCKER_PLATFORMS" | tr ',' '\n' |
		sed -E 's#^[[:space:]]*##; s#^linux/##; s#/.*$##' | awk 'NF' | sort -u)
	for expected in $expected_archs; do
		printf '%s\n' "$archs" | grep -qx "$expected" || missing+="${expected} "
	done
	if [ -n "$missing" ]; then
		arch_list=$(printf '%s\n' "$archs" | tr '\n' ',' | sed 's/,$//')
		die "Docker Hub ${repo}:${tag} 架构不完整（实际 ${arch_list:-空}，缺少 ${missing}；期望 ${DOCKER_PLATFORMS}）"
	fi
	arch_list=$(printf '%s\n' "$archs" | tr '\n' ',' | sed 's/,$//')
	log "OK  Docker Hub ${repo}:${tag}（arch=${arch_list}）"
}

docker_ci_hub_digest() {
	local repo=$1 tag=$2 json code
	json="${LOG_DIR}/dockerhub-digest-$(printf '%s' "${repo}_${tag}" | tr '/: ' '___').json"
	code=$(docker_ci_hub_tag_json "$repo" "$tag" "$json")
	[ "$code" = "200" ] || return 0
	python3 - "$json" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as fh:
	print(json.load(fh).get("digest", ""))
PY
}

docker_ci_verify_hub() {
	local server_repo="${DOCKER_REPOSITORY:-jpomdocker/jpom}"
	local agent_repo="${DOCKER_AGENT_REPOSITORY:-jpomdocker/jpom-agent}"
	local tag version_digest latest_digest
	section "回验 Docker Hub 镜像"
	log "回验仓库：${server_repo} / ${agent_repo}（tag：$(docker_image_tags_csv)）"
	while IFS= read -r tag; do
		[ -n "$tag" ] || continue
		docker_ci_verify_tag "$server_repo" "$tag"
		docker_ci_verify_tag "$agent_repo" "$tag"
	done < <(docker_image_tags)
	if [ "$MODE" = "release" ]; then
		version_digest=$(docker_ci_hub_digest "$server_repo" "$VERSION")
		latest_digest=$(docker_ci_hub_digest "$server_repo" "latest")
		if [ -n "$version_digest" ] && [ -n "$latest_digest" ]; then
			[ "$version_digest" = "$latest_digest" ] ||
				die "Docker Hub 上 ${server_repo}:latest 与 ${server_repo}:${VERSION} 的 digest 不一致（${latest_digest:0:19}… != ${version_digest:0:19}…）"
			log "OK  latest 与 ${VERSION} digest 一致：${version_digest:0:19}…"
		else
			warn "未能从 Docker Hub 取得 ${server_repo} 的 digest，跳过 latest 一致性校验"
		fi
	fi
}

# CI 模式：触发 GitHub Actions 并（默认）等待 + 回验
docker_ci_publish() {
	section "触发 GitHub Actions 发布 Docker 镜像"
	if [ "${PUSHED:-0}" != "1" ]; then
		die "CI 模式要求先把代码/tag 推送到 Gitee（--no-push 时 GitHub 镜像不会有该 ref）；请去掉 --no-push，或改用 --docker-local 本机 buildx 兜底"
	fi
	log "GitHub 仓库：${GH_REPO}"
	log "工作流：${DOCKER_WORKFLOW}  ref=${DOCKER_REF}"
	log "渠道：${MODE}  版本：${VERSION}  镜像 tag：$(docker_image_tags_csv)"
	log "平台：${DOCKER_PLATFORMS}  等待：$( [ "$DOCKER_WAIT" = "1" ] && echo "是（超时 ${DOCKER_WAIT_TIMEOUT} 分钟）" || echo "否" )"
	confirm "确认触发 GitHub Actions 发布 Docker 镜像（${GH_REPO} ${DOCKER_REF}）？"

	docker_ci_wait_ref_sync "$DOCKER_REF"

	local dispatched_at dispatch_output found
	dispatched_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
	log "gh workflow run ${DOCKER_WORKFLOW} --repo ${GH_REPO} --ref ${DOCKER_REF} -f channel=${MODE} -f version=${VERSION} -f images=both -f platforms=${DOCKER_PLATFORMS}"
	if ! dispatch_output=$(gh workflow run "$DOCKER_WORKFLOW" --repo "$GH_REPO" --ref "$DOCKER_REF" \
		-f channel="$MODE" -f version="$VERSION" -f images=both -f platforms="$DOCKER_PLATFORMS" 2>&1); then
		die "触发 GitHub Actions 失败：${dispatch_output}
可能原因：工作流 ${DOCKER_WORKFLOW} 还没有在 ${GH_REPO} 运行过（workflow_dispatch 要求工作流先在默认分支存在并运行过至少一次；
本仓库可由 dev/beta 分支改 workflow 时的 push 自检触发一次，或等工作流随发版进入 master 后由 tag push 触发）；也可改用 --docker-local 本机 buildx 兜底"
	fi

	if [ "$DOCKER_WAIT" = "0" ]; then
		if found=$(docker_ci_find_run "$dispatched_at" 90); then
			DOCKER_RUN_ID=${found%% *}
			DOCKER_RUN_URL=${found#* }
			log "已触发 GitHub Actions（--no-docker-wait，不等待完成）：${DOCKER_RUN_URL}"
		else
			warn "已触发 GitHub Actions，但暂未取到 run URL；请到 https://github.com/${GH_REPO}/actions/workflows/${DOCKER_WORKFLOW} 查看"
		fi
		warn "已跳过等待与 Docker Hub 回验（--no-docker-wait）"
		return 0
	fi

	found=$(docker_ci_find_run "$dispatched_at" 180) ||
		die "已触发 GitHub Actions，但 3 分钟内未找到本次 run（event=workflow_dispatch）；请到 https://github.com/${GH_REPO}/actions/workflows/${DOCKER_WORKFLOW} 查看"
	DOCKER_RUN_ID=${found%% *}
	DOCKER_RUN_URL=${found#* }
	docker_ci_wait_run "$DOCKER_RUN_ID"
	log "GitHub Actions 发布成功：${DOCKER_RUN_URL}"
	docker_ci_verify_hub
}

# 本机模式（兜底）：保持原有 buildx 构建、推送与 imagetools 回验逻辑
docker_local_publish() {
	section "本机 buildx 构建并推送 Docker 镜像"
	local server_dockerfile agent_dockerfile
	if [ "$MODE" = "beta" ]; then
		server_dockerfile="modules/server/DockerfileBeta"
		agent_dockerfile="modules/agent/DockerfileBeta"
	else
		server_dockerfile="modules/server/DockerfileRelease"
		agent_dockerfile="modules/agent/DockerfileRelease"
	fi
	confirm "确认本机 buildx 构建并推送 Docker 镜像（${DOCKER_PLATFORMS}）？"

	local server_repo="${DOCKER_REPOSITORY:-jpomdocker/jpom}"
	local agent_repo="${DOCKER_AGENT_REPOSITORY:-jpomdocker/jpom-agent}"
	local server_tags=() agent_tags=() tag
	while IFS= read -r tag; do
		[ -n "$tag" ] || continue
		server_tags+=(-t "${server_repo}:${tag}")
		agent_tags+=(-t "${agent_repo}:${tag}")
	done < <(docker_image_tags)
	[ "${#server_tags[@]}" -gt 0 ] || die "未能计算 Docker 镜像 tag"
	log "镜像 tag 与 CI 规则一致：$(docker_image_tags_csv)"

	run_in "$PARENT_DIR" docker buildx build --platform "$DOCKER_PLATFORMS" \
		"${server_tags[@]}" -f "./${server_dockerfile}" --push .
	run_in "$PARENT_DIR" docker buildx build --platform "$DOCKER_PLATFORMS" \
		"${agent_tags[@]}" -f "./${agent_dockerfile}" --push .
	local first_tag
	first_tag=$(docker_image_tags | head -n 1)
	docker buildx imagetools inspect "${server_repo}:${first_tag}" >/dev/null ||
		die "Docker 镜像回验失败：${server_repo}:${first_tag}"
	docker_ci_verify_hub
	log "Docker 镜像推送完成（本机 buildx）"
}

phase_docker() {
	if [ "$DOCKER" = "0" ]; then
		log "未启用 Docker（--with-docker 走 GitHub Actions 发布镜像；--docker-local 本机 buildx 兜底）"
		return 0
	fi
	if [ "$DOCKER_MODE" = "local" ]; then
		docker_local_publish
	else
		docker_ci_publish
	fi
}

# ---------------------------------------------------------------------------
# 发布后置步骤（stable）：Gitee Release / GitHub Release / 微信群通知
# ---------------------------------------------------------------------------
phase_post() {
	if [ "$POST" = "0" ]; then
		log "未启用发布后置步骤（--with-post 可自动创建 Gitee/GitHub Release 并发送微信群通知）"
		return 0
	fi
	if [ "$MODE" != "release" ]; then
		log "beta 跳过发布后置步骤（beta 无 tag/Release）"
		return 0
	fi
	section "发布后置步骤（Gitee/GitHub Release + 微信群通知）"
	[ -f "${ROOT_DIR}/script/release-post.sh" ] || die "缺少 script/release-post.sh"
	confirm "确认创建 Gitee/GitHub Release 并发送微信群通知（--no-post 可跳过）？"
	if ! bash "${ROOT_DIR}/script/release-post.sh" release --version "${VERSION}" --yes; then
		die "发布后置步骤失败；修复后可单独重跑：bash script/release-post.sh release --version ${VERSION} --yes"
	fi
}

# ---------------------------------------------------------------------------
# 收尾
# ---------------------------------------------------------------------------
phase_finish() {
	section "发版完成"
	local branch_head tag_desc docker_desc docker_followup
	branch_head=$(gitp rev-parse --short HEAD)
	if [ "$MODE" = "release" ]; then
		tag_desc="${TAG}"
	else
		tag_desc="无（beta）"
	fi
	if [ "$DOCKER" = "0" ]; then
		docker_desc="未启用（--with-docker 走 GitHub Actions；--docker-local 本机兜底）"
		docker_followup="未发布；可补发：bash script/release.sh ${MODE} --version ${VERSION} --from docker --with-docker（GitHub Actions）或 --docker-local（本机 buildx 兜底）"
	elif [ "$DOCKER_MODE" = "local" ]; then
		docker_desc="已用本机 buildx 推送（${DOCKER_PLATFORMS}）"
		docker_followup="已用本机 buildx 推送，建议到 Docker Hub 核对 tag：$(docker_image_tags_csv)"
	elif [ "$DOCKER_WAIT" = "0" ]; then
		docker_desc="已触发 GitHub Actions（未等待完成）${DOCKER_RUN_URL:+：${DOCKER_RUN_URL}}"
		docker_followup="GitHub Actions 仍在运行，完成后核对 Docker Hub tag：$(docker_image_tags_csv)${DOCKER_RUN_URL:+（run：${DOCKER_RUN_URL}）}"
	else
		docker_desc="已由 GitHub Actions 发布并回验${DOCKER_RUN_URL:+：${DOCKER_RUN_URL}}"
		docker_followup="已由 GitHub Actions 发布并回验 Docker Hub：$(docker_image_tags_csv)${DOCKER_RUN_URL:+（run：${DOCKER_RUN_URL}）}"
	fi
	cat <<EOF

----------------------------------------------------------------
 发版类型 : ${MODE}
 版本     : ${VERSION}
 分支     : ${TARGET_BRANCH} @ ${branch_head}
 Tag      : ${tag_desc}
 R2       : $( if [ "$UPLOAD" = "0" ]; then echo "未上传"; elif [ "$R2CI" = "1" ]; then echo "已由 GitHub Actions 上传${R2_CI_PREFIX:+（前缀 ${R2_CI_PREFIX}）}"; else echo "https://d2.jpom.download/${MODE}/${VERSION}/"; fi )
 文档     : $( [ "$DOCS" = "1" ] && echo "已提交/推送 docs main" || echo "未更新" )
 Docker   : ${docker_desc}
 后置步骤 : $( [ "$POST" = "1" ] && echo "已执行（Gitee/GitHub Release + 群通知）" || echo "未执行（--with-post 可自动执行）" )
 日志     : ${LOG_DIR}/release.log
----------------------------------------------------------------

后续人工步骤：
EOF
	if [ "$MODE" = "release" ]; then
		cat <<EOF
1. 在旧版本服务端验证“在线升级”到 ${VERSION}（包来自 https://d2.jpom.download/release/${VERSION}/）
2. 如未用 --with-post：创建 Gitee/GitHub Release 并发送微信群通知
     bash script/release-post.sh release --version ${VERSION} --yes
3. Docker 镜像：${docker_followup}
EOF
	else
		cat <<EOF
1. 在测试环境验证 ${VERSION} 的核心功能（构建、SSH、监控、在线升级入口等）
2. beta 没有 tag / Gitee Release；测稳后按 release 模式合并到 master 发稳定版
3. Docker 镜像：${docker_followup}
4. 如后续同版本覆盖上传，注意 Cloudflare 缓存（约 8 天）并 Purge 对应 URL
EOF
	fi
}

# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------
PHASES=(preflight merge changelog version build tests verify smoke commit push upload docs docker post finish)

phase_index() {
	local target=$1
	local i
	for i in "${!PHASES[@]}"; do
		if [ "${PHASES[$i]}" = "$target" ]; then
			echo "$i"
			return 0
		fi
	done
	return 1
}

phase_will_run() {
	local phase=$1
	local idx start_idx end_idx
	idx=$(phase_index "$phase") || return 1
	start_idx=0
	end_idx=$((${#PHASES[@]} - 1))
	if [ -n "$FROM_PHASE" ]; then
		start_idx=$(phase_index "$FROM_PHASE") || return 1
	fi
	if [ -n "$TO_PHASE" ]; then
		end_idx=$(phase_index "$TO_PHASE") || return 1
	fi
	[ "$idx" -ge "$start_idx" ] && [ "$idx" -le "$end_idx" ]
}

print_plan() {
	section "DRY-RUN 计划（未做任何修改）"
	local push_step="提交并推送 Gitee ${TARGET_BRANCH}$( [ "$MODE" = "release" ] && echo " + tag ${TAG}" )"
	local upload_step="上传 R2 ${MODE}/${VERSION}"
	local docs_step="更新 docs 并推送 docs main"
	local docker_step docker_note docker_args=""
	local step_build_web="前端 npm install && npm run build" step_build_backend="后端 mvn clean package"
	local step_verify="产物完整性校验（含 jar class 非空检查）" step_smoke="服务端/插件端启动冒烟"
	if [ "$R2CI" = "1" ]; then
		step_build_web="前端/后端构建由 GitHub Actions 完成（本机跳过）"
		step_build_backend="（不再本机构建，见第 10 步）"
		step_verify="（CI 下载后）产物完整性校验"
		step_smoke="（CI 下载后）服务端/插件端启动冒烟"
	fi
	if [ "$PUSH" = "0" ] || ! phase_will_run push; then push_step="提交（跳过推送）"; fi
	if [ "$UPLOAD" = "0" ] || ! phase_will_run upload; then
		upload_step="跳过 R2 上传"
	elif [ "$R2CI" = "1" ]; then
		upload_step="触发 GitHub Actions release-package 构建并上传 R2（等待后下载产物校验+冒烟）"
	fi
	if [ "$DOCS" = "0" ] || ! phase_will_run docs; then docs_step="跳过 docs"; fi
	if [ "$DOCKER" = "1" ] && phase_will_run docker; then
		if [ "$DOCKER_MODE" = "local" ]; then
			docker_step="本机 docker buildx 构建并推送 Docker 镜像（${DOCKER_PLATFORMS}）"
			docker_note="     - 本机兜底模式：直接 buildx --push，并用 imagetools 回验"
			docker_args=" --docker-local"
		else
			docker_step="触发 GitHub Actions 发布 Docker 镜像（${GH_REPO} ${DOCKER_REF}）$( [ "$DOCKER_WAIT" = "1" ] && echo "，等待完成并回验 Docker Hub" || echo "，不等待完成" )"
			docker_note="     - 仓库/workflow：${GH_REPO} / ${DOCKER_WORKFLOW}；镜像 tag：$(docker_image_tags_csv)"
			docker_note+=$'\n'
			docker_note+="     - 触发前等待 Gitee -> GitHub 镜像同步（最多 5 分钟）；等待超时 ${DOCKER_WAIT_TIMEOUT} 分钟"
			docker_args=" --with-docker"
		fi
		docker_args+=" --docker-platforms ${DOCKER_PLATFORMS}"
		[ -n "$DOCKER_REF_OPT" ] && docker_args+=" --docker-ref ${DOCKER_REF_OPT}"
		[ "$DOCKER_WAIT" = "0" ] && docker_args+=" --no-docker-wait"
		[ "$DOCKER_WAIT_TIMEOUT" != "30" ] && docker_args+=" --docker-wait-timeout ${DOCKER_WAIT_TIMEOUT}"
		[ -n "$GH_REPO_OPT" ] && docker_args+=" --github-repo ${GH_REPO_OPT}"
	else
		docker_step="跳过 Docker（--with-docker 走 GitHub Actions；--docker-local 本机 buildx 兜底）"
		docker_note="     - 如需发布镜像，追加 --with-docker（CI）或 --docker-local（本机兜底）"
	fi
	cat <<EOF
执行顺序：
  1. 合并 ${SOURCE_BRANCH} -> ${TARGET_BRANCH}
  2. 给 CHANGELOG 的 ${VERSION}-${VERSION_SUFFIX} 补日期 ${RELEASE_DATE}
  3. 调用 replaceVersion.sh ${VERSION} ${MODE}
  4. ${step_build_web}
  5. ${step_build_backend}
  6. 回归单元测试（script/run-regression-tests.sh，默认执行）
  7. ${step_verify}
  8. ${step_smoke}
  9. ${push_step}
 10. ${upload_step}$( [ "$R2CI" = "1" ] && echo "
     - 本机跳过构建/产物校验/启动冒烟；CI 成功后下载产物到 modules/*/target 再做校验与冒烟" )
 11. ${docs_step}
 12. ${docker_step}
${docker_note}

下一步（去掉 --dry-run 即执行；远程操作会二次确认或需要 --yes）：
  bash script/release.sh ${MODE} --target-branch ${TARGET_BRANCH} --source-branch ${SOURCE_BRANCH}$( [ -n "$VERSION_OPT" ] && echo " --version ${VERSION_OPT}" )$( [ "$R2CI" = "1" ] && echo " --with-r2-ci${R2_CI_PREFIX:+ --r2-ci-prefix ${R2_CI_PREFIX}}" )${docker_args}
EOF
}

main() {
	init_toolchain
	if [ -n "${JPOM_GIT_SSH_COMMAND:-}" ]; then
		export GIT_SSH_COMMAND="$JPOM_GIT_SSH_COMMAND"
	fi
	if [ "$FETCH" = "1" ]; then
		log "fetch ${GIT_REMOTE} ..."
		if ! gitp fetch "$GIT_REMOTE" --prune --quiet; then
			# 兼容本机 ssh_config include 权限异常（Bad owner or permissions）：
			# 仅在用户未显式指定 GIT_SSH_COMMAND 时回退到忽略用户 ssh 配置。
			if [ -z "${GIT_SSH_COMMAND:-}" ] && [ -z "${JPOM_GIT_SSH_COMMAND:-}" ]; then
				warn "git fetch 失败，尝试用 GIT_SSH_COMMAND=ssh -F /dev/null 重试（规避本机 ssh_config 权限问题）"
				if GIT_SSH_COMMAND='ssh -F /dev/null -o BatchMode=yes -o StrictHostKeyChecking=accept-new' \
					gitp fetch "$GIT_REMOTE" --prune --quiet; then
					export GIT_SSH_COMMAND='ssh -F /dev/null -o BatchMode=yes -o StrictHostKeyChecking=accept-new'
					warn "已切换 GIT_SSH_COMMAND（如使用 Host 别名请改为显式设置 JPOM_GIT_SSH_COMMAND）"
				elif [ "$PUSH" = "1" ]; then
					die "git fetch 失败；检查网络/凭据，或加 --no-fetch、设置 JPOM_GIT_SSH_COMMAND"
				else
					warn "git fetch 失败（--no-push，继续）"
				fi
			elif [ "$PUSH" = "1" ]; then
				die "git fetch 失败；检查网络/凭据，或加 --no-fetch"
			else
				warn "git fetch 失败（--no-push，继续）"
			fi
		fi
		if [ "$DOCS" = "1" ] && phase_will_run docs && gitd remote get-url "$DOCS_REMOTE" >/dev/null 2>&1; then
			if ! gitd fetch "$DOCS_REMOTE" --prune --quiet; then
				warn "docs git fetch 失败（继续，推送前请再次确认 docs 远程可用）"
			fi
		fi
	fi

	compute_version
	resolve_docker_ref
	log "模式=${MODE}  旧版本=${OLD_VERSION}  新版本=${VERSION}  目标分支=${TARGET_BRANCH}"
	if [ "$DOCKER" = "1" ]; then
		log "Docker 发布：$(docker_summary)"
	fi

	if [ "$DRY_RUN" = "1" ]; then
		phase_preflight
		print_plan
		exit 0
	fi

	local start=0
	local end=$((${#PHASES[@]} - 1))
	if [ -n "$FROM_PHASE" ]; then
		start=$(phase_index "$FROM_PHASE") || die "未知阶段：${FROM_PHASE}"
	fi
	if [ -n "$TO_PHASE" ]; then
		end=$(phase_index "$TO_PHASE") || die "未知阶段：${TO_PHASE}"
	fi
	if [ "$start" -gt "$end" ]; then
		die "--from 阶段在 --to 之后"
	fi
	if [ "$start" -gt "$(phase_index push)" ]; then
		PUSHED=1
	fi
	if [ "$start" -gt "$(phase_index upload)" ]; then
		UPLOADED=1
	fi

	local i phase
	for ((i = start; i <= end; i++)); do
		phase=${PHASES[$i]}
		"phase_${phase}"
	done
}

main "$@"
