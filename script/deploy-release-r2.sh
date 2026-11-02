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
# 将 Jpom 发行包上传到 Cloudflare R2（阿里云 OSS 已停用，不再上传 OSS）
#
# 依赖：rclone（remote 名称默认 r2，配置见 ~/.config/rclone/rclone.conf）、zip、curl
#
# 用法：
#   bash script/deploy-release-r2.sh [release|beta] [--dry-run] [--skip-verify] [--deep-verify] [--force]
#
# 示例：
#   bash script/deploy-release-r2.sh                 # 上传当前 stable 版本到 release/<version>/
#   bash script/deploy-release-r2.sh beta            # 上传当前 beta 版本到 beta/<version>/
#   bash script/deploy-release-r2.sh release -n      # 只打印将执行的动作，不做任何上传
#
# 可选参数：
#   release|beta      上传渠道，默认 release
#   -n, --dry-run     仅打印动作，不执行上传（缺产物时以非 0 退出，便于 CI 预检）
#   --skip-verify     上传后不做任何回验
#   --deep-verify     回验时额外从下载中心下载全部制品并比对 sha1（更严格，耗时更长）
#   --force           目标目录已有对象时仍覆盖上传（同一版本重复上传需要）
#   --allow-missing   允许产物缺失（仅配合 --dry-run 做部分预检）
#
# 可用环境变量覆盖：
#   R2_REMOTE        rclone remote 名称，默认 r2
#   R2_BUCKET        R2 bucket 名称，默认 jpom-releases-apac
#   R2_PUBLIC_BASE   下载中心域名，默认 https://d2.jpom.download
#
# 约定：
#   - release 版本号读 jpom-parent/script/tag.release.txt
#   - beta    版本号读 jpom-parent/script/tag.beta.txt
#   - 上传 server/agent 的 zip、tar.gz 及各自 sha1（共 8 个），外加合并包 jpom-<version>.zip
#   - 上传后默认通过下载中心域名回验：制品 sha1、各对象大小；--deep-verify 会下载制品逐字节校验
#   - Cloudflare 对 d2 域名的 GET 响应实测为 cache-control: max-age=691200（约 8 天），
#     同一版本重复覆盖上传后需在 Cloudflare 控制台 Purge 对应 URL，否则用户可能下载到旧内容；
#     回验只能证明 R2 内对象正确，不能证明边缘缓存已刷新
#

if [ -z "${BASH_VERSION:-}" ]; then
	# 本脚本使用 bash 数组等特性，兼容被 sh 直接调用的情况
	exec bash "$0" "$@"
fi

set -euo pipefail

R2_REMOTE="${R2_REMOTE:-r2}"
R2_BUCKET="${R2_BUCKET:-jpom-releases-apac}"
R2_PUBLIC_BASE="${R2_PUBLIC_BASE:-https://d2.jpom.download}"
R2_PUBLIC_BASE="${R2_PUBLIC_BASE%/}"

channel="release"
dry_run=0
verify=1
deep_verify=0
force=0
allow_missing=0

usage() {
	echo "用法：bash $0 [release|beta] [--dry-run] [--skip-verify] [--deep-verify] [--force] [--allow-missing]"
	echo
	echo "  release|beta      上传渠道，默认 release"
	echo "  -n, --dry-run     仅打印动作，不执行上传（缺产物时非 0 退出）"
	echo "  --skip-verify     上传后不做回验"
	echo "  --deep-verify     回验时下载全部制品逐字节比对 sha1"
	echo "  --force           目标目录已有对象时仍覆盖上传"
	echo "  --allow-missing   允许产物缺失（仅配合 --dry-run 做部分预检）"
	echo
	echo "可用环境变量：R2_REMOTE（默认 r2）、R2_BUCKET（默认 jpom-releases-apac）、"
	echo "              R2_PUBLIC_BASE（默认 https://d2.jpom.download）"
}

die() {
	echo "ERROR: $*" >&2
	exit 1
}

# 兼容 macOS（无 sha1sum）：优先 sha1sum，回退 shasum -a 1
sha1_of() {
	if command -v sha1sum >/dev/null 2>&1; then
		sha1sum "$1" | awk '{print $1}'
	else
		shasum -a 1 "$1" | awk '{print $1}'
	fi
}

file_size() {
	stat -c '%s' "$1" 2>/dev/null || stat -f '%z' "$1" 2>/dev/null || die "无法获取文件大小：$1"
}

while [ $# -gt 0 ]; do
	case "$1" in
	release | beta)
		channel="$1"
		shift
		;;
	-n | --dry-run)
		dry_run=1
		shift
		;;
	--skip-verify)
		verify=0
		shift
		;;
	--deep-verify)
		deep_verify=1
		shift
		;;
	--force)
		force=1
		shift
		;;
	--allow-missing)
		allow_missing=1
		shift
		;;
	-h | --help)
		usage
		exit 0
		;;
	*)
		echo "ERROR: 不支持的参数 $1" >&2
		usage
		exit 1
		;;
	esac
done

script_dir=$(cd "$(dirname "$0")" && pwd)
root_dir=$(cd "${script_dir}/.." && pwd)
parent_dir="${root_dir}/jpom-parent"

if [ "${channel}" = "release" ]; then
	version_file="${parent_dir}/script/tag.release.txt"
else
	version_file="${parent_dir}/script/tag.beta.txt"
fi
[ -f "${version_file}" ] || die "版本文件不存在：${version_file}"

version=$(tr -d ' \r\n' <"${version_file}")
[ -n "${version}" ] || die "版本号为空，请检查 ${version_file}"

target_dir="${parent_dir}/modules"
sources=(
	"${target_dir}/agent/target/agent-${version}-release.tar.gz"
	"${target_dir}/agent/target/agent-${version}-release.tar.gz.sha1"
	"${target_dir}/agent/target/agent-${version}-release.zip"
	"${target_dir}/agent/target/agent-${version}-release.zip.sha1"
	"${target_dir}/server/target/server-${version}-release.tar.gz"
	"${target_dir}/server/target/server-${version}-release.tar.gz.sha1"
	"${target_dir}/server/target/server-${version}-release.zip"
	"${target_dir}/server/target/server-${version}-release.zip.sha1"
)

echo "================================================================"
echo " 渠道：${channel}    版本：${version}"
echo " 目标：${R2_REMOTE}:${R2_BUCKET}/${channel}/${version}/"
echo " 下载：${R2_PUBLIC_BASE}/${channel}/${version}/"
[ "${dry_run}" -eq 1 ] && echo " 模式：DRY-RUN（不会执行任何上传）"
echo "================================================================"

if [ "${dry_run}" -eq 0 ]; then
	command -v rclone >/dev/null 2>&1 || die "未找到 rclone，请先安装并配置 remote ${R2_REMOTE}"
	rclone listremotes 2>/dev/null | grep -qx "${R2_REMOTE}:" || die "rclone remote 不存在：${R2_REMOTE}（检查 ~/.config/rclone/rclone.conf）"
	command -v zip >/dev/null 2>&1 || die "未找到 zip 命令，无法生成合并包 jpom-${version}.zip"
	command -v curl >/dev/null 2>&1 || die "未找到 curl 命令，无法回验下载地址"
	command -v sha1sum >/dev/null 2>&1 || command -v shasum >/dev/null 2>&1 || die "未找到 sha1sum/shasum 命令"
else
	command -v rclone >/dev/null 2>&1 || echo "WARN: 当前环境未找到 rclone（dry-run 继续）"
fi

missing=0
for src in "${sources[@]}"; do
	if [ ! -f "${src}" ]; then
		echo "WARN: 缺少文件 ${src}"
		missing=1
	fi
done
if [ "${missing}" -eq 1 ]; then
	if [ "${allow_missing}" -eq 1 ]; then
		echo "WARN: 存在缺失产物，已按 --allow-missing 继续"
	else
		die "存在缺失的构建产物，请先完成前端构建（npm run build）和 mvn clean package"
	fi
fi

# 校验本地 .sha1 与制品内容一致，避免上传过期校验文件
for src in "${sources[@]}"; do
	case "${src}" in
	*.sha1) continue ;;
	esac
	[ -f "${src}" ] || continue
	sha1_file="${src}.sha1"
	if [ -f "${sha1_file}" ]; then
		local_sha1=$(sha1_of "${src}")
		declared_sha1=$(tr -d ' \r\n' <"${sha1_file}")
		[ "${local_sha1}" = "${declared_sha1}" ] || die "本地 ${sha1_file##*/} 与制品不一致：${declared_sha1} != ${local_sha1}"
	else
		echo "WARN: 未找到 ${sha1_file}，上传后该制品将无法通过下载中心回验"
	fi
done

remote_dir="${R2_REMOTE}:${R2_BUCKET}/${channel}/${version}/"
stage_dir="${root_dir}/.tmp/deploy-r2.$$"
merged_zip="${stage_dir}/jpom-${version}.zip"

if [ "${dry_run}" -eq 0 ]; then
	existing=$(rclone lsf "${remote_dir}" 2>/dev/null | head -1 || true)
	if [ -n "${existing}" ] && [ "${force}" -eq 0 ]; then
		die "目标目录已存在对象（${remote_dir}）；同一版本重复上传请加 --force（并注意 Cloudflare 缓存）"
	fi
	mkdir -p "${stage_dir}"
	trap 'rm -rf "${stage_dir}"' EXIT
	echo ">>> 生成合并包 ${merged_zip}"
	zip -j -q "${merged_zip}" \
		"${target_dir}/server/target/server-${version}-release.zip" \
		"${target_dir}/agent/target/agent-${version}-release.zip"
else
	echo ">>> (dry-run) zip -j ${merged_zip} server-${version}-release.zip agent-${version}-release.zip"
fi

upload_file() {
	local src="$1"
	local name
	name=$(basename "${src}")
	local dest="${remote_dir}${name}"
	if [ "${dry_run}" -eq 1 ]; then
		echo ">>> (dry-run) rclone copyto ${src} ${dest}"
		return 0
	fi
	echo ">>> 上传 ${name}"
	rclone copyto "${src}" "${dest}" --retries 3 --low-level-retries 10 --stats 30s --stats-one-line
}

for src in "${sources[@]}"; do
	[ -f "${src}" ] || continue
	upload_file "${src}"
done
if [ "${dry_run}" -eq 1 ]; then
	echo ">>> (dry-run) rclone copyto ${merged_zip} ${remote_dir}jpom-${version}.zip"
	echo
	echo "DRY-RUN 结束：未上传任何文件。"
	exit 0
fi
upload_file "${merged_zip}"

echo
echo "================================================================"
echo " 上传完成，开始回验（下载中心：${R2_PUBLIC_BASE}）"
echo "================================================================"

if [ "${verify}" -eq 0 ]; then
	echo "已指定 --skip-verify，跳过回验。"
else
	# 用于绕过 Cloudflare 边缘缓存，确保拿到 R2 上的最新对象
	cache_buster="verify=$(date +%s)-${RANDOM}"
	fail=0

	# 1) 远端对象大小必须与本地一致（可发现上传截断/缺失）
	uploaded=("${sources[@]}" "${merged_zip}")
	for src in "${uploaded[@]}"; do
		[ -f "${src}" ] || continue
		name=$(basename "${src}")
		local_size=$(file_size "${src}")
		remote_size=$(rclone lsl "${remote_dir}${name}" 2>/dev/null | awk 'NR==1 {print $1}' || true)
		if [ "${local_size}" = "${remote_size}" ]; then
			echo "OK   ${name}  size=${local_size}"
		else
			echo "FAIL ${name}  local_size=${local_size} remote_size=${remote_size:-<无法获取>}"
			fail=1
		fi
	done

	# 2) 制品 sha1 与远端 .sha1 内容一致
	for src in "${sources[@]}"; do
		case "${src}" in
		*.sha1) continue ;;
		esac
		[ -f "${src}" ] || continue
		name=$(basename "${src}")
		local_sha1=$(sha1_of "${src}")
		remote_sha1=$(curl -fsSL --retry 3 --retry-delay 2 \
			"${R2_PUBLIC_BASE}/${channel}/${version}/${name}.sha1?${cache_buster}" 2>/dev/null | tr -d ' \r\n' || true)
		if [ "${local_sha1}" = "${remote_sha1}" ]; then
			echo "OK   ${name}  sha1=${local_sha1}"
		else
			echo "FAIL ${name}  local=${local_sha1} remote=${remote_sha1:-<无法获取>}"
			fail=1
		fi
	done

	# 3) 可选：从下载中心下载全部制品逐字节比对（最严格，耗时更长）
	if [ "${deep_verify}" -eq 1 ]; then
		for src in "${uploaded[@]}"; do
			[ -f "${src}" ] || continue
			name=$(basename "${src}")
			deep_file="${stage_dir}/deep-${name}"
			echo ">>> deep-verify 下载 ${name}"
			if ! curl -fsSL --retry 3 --retry-delay 2 -o "${deep_file}" \
				"${R2_PUBLIC_BASE}/${channel}/${version}/${name}?${cache_buster}"; then
				echo "FAIL ${name}  deep-verify 下载失败"
				fail=1
				continue
			fi
			if [ "$(sha1_of "${src}")" = "$(sha1_of "${deep_file}")" ]; then
				echo "OK   ${name}  deep-verify sha1 一致"
			else
				echo "FAIL ${name}  deep-verify sha1 不一致"
				fail=1
			fi
			rm -f "${deep_file}"
		done
	fi

	if [ "${fail}" -ne 0 ]; then
		echo
		echo "回验失败：请检查上传是否完整；若为同一版本覆盖上传，先在 Cloudflare 控制台" >&2
		echo "Purge 对应 URL 后重新回验（d2 域名 GET 缓存约 8 天）。" >&2
		exit 1
	fi
fi

echo
echo "================================================================"
echo " 发布提醒（尚未完成的发版步骤）"
echo "================================================================"
cat <<EOF
1. 更新文档版本号并 push docs 仓库 main（GitHub Pages 自动构建）：
     cd docs/script && sh replaceVersion.sh ${version} ${channel}
2. 在 Gitee 基于 tag 创建发行版并上传安装包与 sha1（GitHub 由镜像同步，不手动操作）
3. 在旧版本上验证在线升级（服务端读取 https://jpom.top/docs/*-versions.json，包来自 d2）
4. 若本次是同一版本覆盖上传，请到 Cloudflare 控制台 Purge：
     ${R2_PUBLIC_BASE}/${channel}/${version}/*
5. 本机上行带宽较慢时，建议在服务端执行本脚本，或用 nohup 在后台运行
EOF
