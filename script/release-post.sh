#!/usr/bin/env bash
#
# 发布后置步骤：Gitee Release + GitHub Release + 微信群通知（可重复执行，自动跳过已完成项）
#
# 用法：
#   bash script/release-post.sh [release] [选项]
#
# 选项：
#   --version VER        版本号（默认读取 jpom-parent/script/tag.release.txt）
#   --dry-run            只打印将要执行的动作，不做任何写操作
#   --yes                非交互确认（自动化/CI 场景必须显式传入）
#   --skip-gitee         跳过 Gitee Release
#   --skip-github        跳过 GitHub Release
#   --skip-notify        跳过微信群通知
#   --gitee-only         只做 Gitee Release
#   --github-only        只做 GitHub Release
#   --notify-only        只做微信群通知
#   --message FILE       微信群通知正文文件（不传则按模板与 CHANGELOG 自动生成）
#   --repo OWNER/REPO    GitHub 仓库（默认 dromara/Jpom）
#   --gitee-repo REPO    Gitee 仓库（默认 dromara/Jpom）
#   --notify-groups FILE 传给 jpom-release-notify 的群清单（可选）
#
# 依赖：
#   - Gitee：curl + Gitee 私人令牌（环境变量 GITEE_TOKEN，或 GNOME Keyring：
#     secret-tool lookup service gitee username <GITEE_KEYRING_USER|bwcx-jzy>）
#   - GitHub：gh（已登录，且对目标仓库有 Release 权限）
#   - 微信群：dev-docs/.dsh/skills/jpom-release-notify/scripts/{send-notice,check-delivery}.sh
#
# 说明：
#   - Gitee→GitHub 代码/tag 由镜像自动同步，但 **Release 不会自动同步**，因此本脚本会手动创建 GitHub Release。
#   - 默认不 @所有人、不发朋友圈；同一版本通知已送达时自动跳过（避免重复发送）。
#   - 安装包与 sha1 从 jpom-parent/modules/{server,agent}/target/ 上传到 Gitee Release。
#
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PARENT_DIR="${ROOT_DIR}/jpom-parent"
CHANGELOG="${PARENT_DIR}/CHANGELOG.md"
TAG_FILE="${PARENT_DIR}/script/tag.release.txt"
NOTIFY_SKILL_DIR="${ROOT_DIR}/.dsh/skills/jpom-release-notify"

VERSION=""
DRY_RUN=0
ASSUME_YES=0
DO_GITEE=1
DO_GITHUB=1
DO_NOTIFY=1
MESSAGE_FILE=""
GH_REPO="${GH_REPO:-dromara/Jpom}"
GITEE_REPO="${GITEE_REPO:-dromara/Jpom}"
GITEE_KEYRING_USER="${GITEE_KEYRING_USER:-bwcx-jzy}"
NOTIFY_GROUPS=""

die() { echo "ERROR: $*" >&2; exit 1; }
log() { echo ">>> $*"; }
warn() { echo "WARN: $*" >&2; }

usage() {
	sed -n '2,40p' "$0" | sed 's/^# \{0,1\}//'
	exit 2
}

while [[ $# -gt 0 ]]; do
	case "$1" in
	release|stable) ;;
	beta) die "release-post.sh 只用于稳定版（beta 无 tag/Release）" ;;
	--version)
		VERSION="${2:?缺少 --version 的值}"
		shift
		;;
	--dry-run) DRY_RUN=1 ;;
	--yes | -y) ASSUME_YES=1 ;;
	--skip-gitee) DO_GITEE=0 ;;
	--skip-github) DO_GITHUB=0 ;;
	--skip-notify) DO_NOTIFY=0 ;;
	--gitee-only) DO_GITHUB=0; DO_NOTIFY=0 ;;
	--github-only) DO_GITEE=0; DO_NOTIFY=0 ;;
	--notify-only) DO_GITEE=0; DO_GITHUB=0 ;;
	--message)
		MESSAGE_FILE="${2:?缺少 --message 的值}"
		shift
		;;
	--repo)
		GH_REPO="${2:?缺少 --repo 的值}"
		shift
		;;
	--gitee-repo)
		GITEE_REPO="${2:?缺少 --gitee-repo 的值}"
		shift
		;;
	--notify-groups)
		NOTIFY_GROUPS="${2:?缺少 --notify-groups 的值}"
		shift
		;;
	-h | --help) usage ;;
	-*) die "未知参数：$1（--help 查看用法）" ;;
	*) die "未知参数：$1" ;;
	esac
	shift
done

if [ -z "${VERSION}" ]; then
	[ -f "${TAG_FILE}" ] || die "找不到 ${TAG_FILE}，请用 --version 指定版本"
	VERSION="$(tr -d ' \r\n' <"${TAG_FILE}")"
fi
[[ "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "版本号格式不正确：${VERSION}（稳定版应为 x.y.z）"
TAG="v${VERSION}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT
BODY_FILE="${TMP_DIR}/release-body.md"
ASSETS=(
	"${PARENT_DIR}/modules/server/target/server-${VERSION}-release.zip"
	"${PARENT_DIR}/modules/server/target/server-${VERSION}-release.zip.sha1"
	"${PARENT_DIR}/modules/server/target/server-${VERSION}-release.tar.gz"
	"${PARENT_DIR}/modules/server/target/server-${VERSION}-release.tar.gz.sha1"
	"${PARENT_DIR}/modules/agent/target/agent-${VERSION}-release.zip"
	"${PARENT_DIR}/modules/agent/target/agent-${VERSION}-release.zip.sha1"
	"${PARENT_DIR}/modules/agent/target/agent-${VERSION}-release.tar.gz"
	"${PARENT_DIR}/modules/agent/target/agent-${VERSION}-release.tar.gz.sha1"
)

echo "=================================================================="
echo " 发布后置步骤：Gitee Release / GitHub Release / 微信群通知"
echo " 版本：${VERSION}    Tag：${TAG}    仓库：${GITEE_REPO} / ${GH_REPO}"
((DRY_RUN)) && echo " 模式：DRY-RUN（不做任何写操作）"
echo "=================================================================="

# ---------------------------------------------------------------------------
# 生成发行说明（与 Gitee/GitHub Release 共用）
# ---------------------------------------------------------------------------
generate_body() {
	[ -f "${CHANGELOG}" ] || die "找不到 CHANGELOG：${CHANGELOG}"
	python3 - "${CHANGELOG}" "${BODY_FILE}" "${VERSION}" <<'PY'
import re, sys
changelog, out, version = sys.argv[1], sys.argv[2], sys.argv[3]
text = open(changelog, encoding="utf-8").read()
m = re.search(r"^## %s-release \([0-9-]+\)\n(.*?)(?=^## |\Z)" % re.escape(version), text, re.M | re.S)
if not m:
    sys.exit("CHANGELOG 中找不到 %s-release 段落" % version)
body = m.group(0).strip().rstrip("\n").replace("\n------", "")
body += """

---

### 📦 下载（下载中心 https://d2.jpom.download）

| 包 | 链接 |
| --- | --- |
| 服务端 + 插件端合并包 | https://d2.jpom.download/release/{v}/jpom-{v}.zip |
| server zip | https://d2.jpom.download/release/{v}/server-{v}-release.zip |
| server tar.gz | https://d2.jpom.download/release/{v}/server-{v}-release.tar.gz |
| agent zip | https://d2.jpom.download/release/{v}/agent-{v}-release.zip |
| agent tar.gz | https://d2.jpom.download/release/{v}/agent-{v}-release.tar.gz |

> 安装包与 sha1 校验文件见发行版附件；Docker 镜像：`jpomdocker/jpom:{v}`、`jpomdocker/jpom-agent:{v}`。
""".replace("{v}", version)
open(out, "w", encoding="utf-8").write(body)
print("发行说明已生成：%s（%d 字）" % (out, len(body)))
PY
}

# ---------------------------------------------------------------------------
# Gitee Release
# ---------------------------------------------------------------------------
gitee_token() {
	if [ -n "${GITEE_TOKEN:-}" ]; then
		printf '%s' "${GITEE_TOKEN}"
		return 0
	fi
	command -v secret-tool >/dev/null 2>&1 || return 1
	secret-tool lookup service gitee username "${GITEE_KEYRING_USER}" 2>/dev/null || return 1
}

gitee_api() {
	local method="$1" path="$2"
	shift 2
	local sep='?'
	[[ "${path}" == *\?* ]] && sep='&'
	curl -sS -m 60 -X "${method}" "https://gitee.com/api/v5${path}${sep}access_token=${GITEE_TOKEN}" "$@"
}

gitee_release_id() {
	gitee_api GET "/repos/${GITEE_REPO}/releases/tags/${TAG}" 2>/dev/null |
		python3 -c 'import json,sys
try:
    d=json.load(sys.stdin); print(d.get("id") or "")
except Exception:
    print("")'
}

gitee_asset_names() {
	local rid="$1"
	gitee_api GET "/repos/${GITEE_REPO}/releases/${rid}" 2>/dev/null |
		python3 -c 'import json,sys
try:
    d=json.load(sys.stdin)
    print("\n".join(a.get("name","") for a in (d.get("assets") or [])))
except Exception:
    pass'
}

gitee_ensure_release() {
	[ -f "${BODY_FILE}" ] || generate_body
	local rid
	rid="$(gitee_release_id)"
	if [ -n "${rid}" ]; then
		log "Gitee Release 已存在（id=${rid}，tag=${TAG}）"
	else
		if ((DRY_RUN)); then
			log "(dry-run) 创建 Gitee Release ${TAG}"
			return 0
		fi
		confirm_or_die "创建 Gitee Release ${TAG}？" || return 1
		local resp
		resp="$(gitee_api POST "/repos/${GITEE_REPO}/releases" \
			--data-urlencode "tag_name=${TAG}" \
			--data-urlencode "name=${VERSION}" \
			--data-urlencode "body@${BODY_FILE}" \
			--data-urlencode "prerelease=false" \
			--data-urlencode "target_commitish=master")"
		rid="$(printf '%s' "${resp}" | python3 -c 'import json,sys
try:
    print(json.load(sys.stdin).get("id") or "")
except Exception:
    print("")')"
		[ -n "${rid}" ] || die "创建 Gitee Release 失败：$(printf '%s' "${resp}" | head -c 300)"
		log "Gitee Release 创建成功（id=${rid}）"
	fi

	local existing
	existing="$(gitee_asset_names "${rid}")"
	local missing=()
	local src
	for src in "${ASSETS[@]}"; do
		[ -f "${src}" ] || die "缺少安装包：${src}"
		local name
		name="$(basename "${src}")"
		grep -qxF -- "${name}" <<<"${existing}" || missing+=("${src}")
	done
	if [ "${#missing[@]}" -eq 0 ]; then
		log "Gitee Release 附件已齐全（$(echo "${existing}" | grep -c . ) 个）"
		return 0
	fi
	if ((DRY_RUN)); then
		log "(dry-run) 上传 ${#missing[@]} 个附件到 Gitee Release"
		printf '      %s\n' "${missing[@]##*/}"
		return 0
	fi
	confirm_or_die "上传 ${#missing[@]} 个附件到 Gitee Release？" || return 1
	for src in "${missing[@]}"; do
		log "上传附件：$(basename "${src}")"
		gitee_api POST "/repos/${GITEE_REPO}/releases/${rid}/attach_files" -F "file=@${src}" >/dev/null ||
			die "上传附件失败：$(basename "${src}")"
	done
	log "Gitee Release 附件上传完成"
}

# ---------------------------------------------------------------------------
# GitHub Release
# ---------------------------------------------------------------------------
gh_available() { command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; }

github_ensure_release() {
	[ -f "${BODY_FILE}" ] || generate_body
	if ! gh_available; then
		warn "gh 未安装或未登录，跳过 GitHub Release"
		return 0
	fi
	if gh release view "${TAG}" -R "${GH_REPO}" >/dev/null 2>&1; then
		log "GitHub Release 已存在：https://github.com/${GH_REPO}/releases/tag/${TAG}"
		return 0
	fi
	if ((DRY_RUN)); then
		log "(dry-run) 创建 GitHub Release ${TAG}（Latest）"
		return 0
	fi
	confirm_or_die "创建 GitHub Release ${TAG}（Latest）？" || return 1
	gh release create "${TAG}" -R "${GH_REPO}" \
		--title "${TAG}" \
		--notes-file "${BODY_FILE}" \
		--latest >/dev/null || die "创建 GitHub Release 失败"
	log "GitHub Release 创建成功：https://github.com/${GH_REPO}/releases/tag/${TAG}"
}

# ---------------------------------------------------------------------------
# 微信群通知
# ---------------------------------------------------------------------------
generate_notify_message() {
	local out="$1"
	[ -f "${CHANGELOG}" ] || die "找不到 CHANGELOG：${CHANGELOG}"
	python3 - "${CHANGELOG}" "${NOTIFY_SKILL_DIR}" "${VERSION}" "${out}" <<'PY'
import re, sys
changelog, skill_dir, version, out = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
text = open(changelog, encoding="utf-8").read()
m = re.search(r"^## %s-release \(([0-9-]+)\)\n(.*?)(?=^## |\Z)" % re.escape(version), text, re.M | re.S)
if not m:
    sys.exit("CHANGELOG 中找不到 %s-release 段落" % version)
date, section = m.group(1), m.group(2)
def bullets(title):
    mm = re.search(r"^### %s\n(.*?)(?=^### |\Z)" % re.escape(title), section, re.M | re.S)
    if not mm:
        return []
    return [l.strip() for l in mm.group(1).splitlines() if re.match(r"^\d+\.\s", l.strip())]
def clean(s):
    s = re.sub(r"（感谢[^）]*）", "", s)
    s = re.sub(r"\[Gitee (?:issues|pr) [^\]]*\]\([^)]*\)", "", s)
    s = re.sub(r"\(\[[^\]]*\]\([^)]*\)\)", "", s)
    s = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", s)
    s = re.sub(r"(#\d+(?:、#\d+)*)", r"\1", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s
security = [l for l in bullets("🐞 解决BUG、优化功能") if re.search(r"安全|CVE|越权|注入|密钥|验证码|JWT|fastjson|Tomcat|BouncyCastle|commons-lang3", l)]
features = bullets("🐣 新增功能")
bugs = bullets("🐞 解决BUG、优化功能")
highlights = []
if security:
    highlights.append("【安全】集中修复多项安全风险（" + "；".join(clean(x) for x in security[:2])[:180] + " 等，详见更新日志）")
for x in bugs:
    if len(highlights) >= 2:
        break
    if x in security:
        continue
    highlights.append("【修复】" + clean(x))
for x in features:
    if len(highlights) >= 5:
        break
    highlights.append("【新增】" + clean(x))
for x in bugs:
    if len(highlights) >= 5:
        break
    if x in security or any(clean(x)[:20] in h for h in highlights):
        continue
    highlights.append("【优化】" + clean(x))
highlights = [h[:160] for h in highlights[:5]]
upgrade = ""
mm = re.search(r"^### ⚠️ 升级说明\n(.*?)(?=^### |\Z)", section, re.M | re.S)
if mm:
    up = [clean(l) for l in mm.group(1).splitlines() if re.match(r"^\d+\.\s", l.strip())]
    if up:
        upgrade = "升级提示：" + " ".join(up)[:260]
msg = """【Jpom 版本更新】%s 稳定版已发布（%s）

本次更新：
%s

下载：
服务端 https://d2.jpom.download/release/%s/server-%s-release.zip
插件端 https://d2.jpom.download/release/%s/agent-%s-release.zip
更新日志 https://jpom.top/pages/changelog/new/
下载页 https://jpom.top/pages/all-downloads/

%s
说明：在线升级自带旧包与数据库备份，无需手工备份；集群部署请确认各节点配置一致。
""" % (version, date, "\n".join("%d. %s" % (i + 1, h) for i, h in enumerate(highlights)),
       version, version, version, version, (upgrade + "\n") if upgrade else "")
msg = re.sub(r"\n{3,}", "\n\n", msg).strip() + "\n"
open(out, "w", encoding="utf-8").write(msg)
print("通知文案已生成：%s（%d 字）" % (out, len(msg)))
PY
}

notify_ensure_sent() {
	if [ ! -f "${NOTIFY_SKILL_DIR}/scripts/send-notice.sh" ]; then
		warn "找不到 jpom-release-notify skill（${NOTIFY_SKILL_DIR}），跳过微信群通知"
		return 0
	fi
	local check_out
	check_out="$(bash "${NOTIFY_SKILL_DIR}/scripts/check-delivery.sh" "${VERSION}" ${NOTIFY_GROUPS:+--groups "${NOTIFY_GROUPS}"} 2>&1 || true)"
	if grep -q '全部群均已命中' <<<"${check_out}"; then
		log "微信群通知已送达（check-delivery 全部命中），跳过"
		return 0
	fi
	local msg_file="${MESSAGE_FILE}"
	if [ -z "${msg_file}" ]; then
		msg_file="${TMP_DIR}/notify-${VERSION}.txt"
		generate_notify_message "${msg_file}"
	fi
	[ -s "${msg_file}" ] || die "通知正文为空：${msg_file}"
	if ((DRY_RUN)); then
		log "(dry-run) 发送微信群通知（去掉 --dry-run 后实际发送）"
		sed -n '1,8p' "${msg_file}" | sed 's/^/      /'
		return 0
	fi
	confirm_or_die "发送微信群通知到 7 个 Jpom 群（不 @所有人）？" || return 1
	bash "${NOTIFY_SKILL_DIR}/scripts/send-notice.sh" "${msg_file}" ${NOTIFY_GROUPS:+--groups "${NOTIFY_GROUPS}"} ||
		die "微信群通知发送失败（不会自动重试，请人工核对后决定是否补发）"
	bash "${NOTIFY_SKILL_DIR}/scripts/check-delivery.sh" "${VERSION}" ${NOTIFY_GROUPS:+--groups "${NOTIFY_GROUPS}"} ||
		warn "送达核对存在未命中群，请人工核对（不要自动重发）"
}

# ---------------------------------------------------------------------------
# 确认
# ---------------------------------------------------------------------------
confirm_or_die() {
	local prompt="$1"
	((ASSUME_YES)) && return 0
	read -r -p "${prompt} [y/N] " ans
	[[ "${ans}" =~ ^[Yy]$ ]] || die "已取消（${prompt}）"
}

# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------
if ((DO_GITEE)); then
	section_label="Gitee Release"
	GITEE_TOKEN="$(gitee_token)" || die "未找到 Gitee 令牌：请设置 GITEE_TOKEN，或用 secret-tool 存 service=gitee username=${GITEE_KEYRING_USER}"
	[ -n "${GITEE_TOKEN}" ] || die "Gitee 令牌为空"
	echo
	log "== Gitee Release =="
	generate_body
	gitee_ensure_release || die "Gitee Release 步骤失败"
fi

if ((DO_GITHUB)); then
	echo
	log "== GitHub Release =="
	generate_body
	github_ensure_release
fi

if ((DO_NOTIFY)); then
	echo
	log "== 微信群通知 =="
	notify_ensure_sent
fi

echo
echo "=================================================================="
if ((DRY_RUN)); then
	echo " DRY-RUN 完成（未做任何写操作）"
else
	echo " 发布后置步骤完成"
fi
echo " 后续：验证 Gitee/GitHub Release 与群消息；归档到 dev-docs/archive/"
echo "=================================================================="
