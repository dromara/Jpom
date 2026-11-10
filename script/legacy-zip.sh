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
# 过渡期兼容：从已解压的运行目录重新生成旧版在线升级需要的 zip 发布包
#
# 背景（过渡窗口（桥接稳定版及之前））：
#   Java 发布包默认只产出 server/agent 的 tar.gz（Maven Assembly 的 dir,tar.gz），
#   但旧版 Server/Agent 的在线升级只认 <kind>-<version>-release.zip。本脚本从 Assembly
#   解压出的运行目录 modules/<kind>/target/<kind>-<version>-release/ 重新打包出与旧版
#   结构一致的 zip（zip 根为 bin/conf/lib/LICENSE，不带 ./ 前缀），并生成同名 .sha1。
#
# 用法：
#   bash script/legacy-zip.sh --parent-dir jpom-parent --version 2.11.14
#   bash script/legacy-zip.sh --parent-dir . --version 2.11.14 --kind server
#
# 参数：
#   --parent-dir  jpom-parent 仓库根（默认 jpom-parent）
#   --version     版本号（必填，如 2.11.14 / 2.11.14.1）
#   --kind        server | agent | all（默认 all）
#
# 依赖：zip、unzip、sha1sum（无 sha1sum 时回退 shasum -a 1）
#-----------------------------------------------------------

set -euo pipefail

parent_dir="jpom-parent"
version=""
kind="all"

while [ $# -gt 0 ]; do
	case "$1" in
	--parent-dir)
		parent_dir="${2:?}"
		shift 2
		;;
	--version)
		version="${2:?}"
		shift 2
		;;
	--kind)
		kind="${2:?}"
		shift 2
		;;
	-h | --help)
		sed -n '/^# 用法：/,/^#----/p' "$0" | sed 's/^# \{0,1\}//'
		exit 0
		;;
	*)
		echo "ERROR: 不支持的参数 $1" >&2
		exit 2
		;;
	esac
done

[ -n "$version" ] || {
	echo "ERROR: 必须指定 --version" >&2
	exit 2
}
case "$kind" in
server | agent | all) ;;
*)
	echo "ERROR: --kind 只能是 server/agent/all：${kind}" >&2
	exit 2
	;;
esac
[ -d "$parent_dir" ] || {
	echo "ERROR: parent-dir 不存在：${parent_dir}" >&2
	exit 2
}
parent_dir=$(cd "$parent_dir" && pwd)

log() { echo "[legacy-zip] $*"; }
die() {
	echo "[legacy-zip][ERROR] $*" >&2
	exit 1
}

command -v zip >/dev/null 2>&1 || die "缺少 zip 命令"
command -v unzip >/dev/null 2>&1 || die "缺少 unzip 命令"
if ! command -v sha1sum >/dev/null 2>&1 && ! command -v shasum >/dev/null 2>&1; then
	die "缺少 sha1sum（或 shasum）命令"
fi

# 生成旧版约定的 .sha1：文件内容只写 40 位十六进制（不带文件名，旧客户端/下载中心按此读取）
write_sha1() {
	local file=$1
	if command -v sha1sum >/dev/null 2>&1; then
		sha1sum "$file" | awk '{print $1}' >"${file}.sha1"
	else
		shasum -a 1 "$file" | awk '{print $1}' >"${file}.sha1"
	fi
}

file_size() { stat -c '%s' "$1" 2>/dev/null || stat -f '%z' "$1" 2>/dev/null; }

pack_kind() {
	local kind=$1
	local target_dir="${parent_dir}/modules/${kind}/target"
	local run_dir="${target_dir}/${kind}-${version}-release"
	local zip_file="${target_dir}/${kind}-${version}-release.zip"
	[ -d "$run_dir" ] || die "缺少解压运行目录：${run_dir}（先执行 mvn package 生成 tar.gz/dir 产物）"

	# 只打包旧版 zip 的顶层内容（bin/conf/lib/LICENSE 等），避免把运行目录里的其他临时文件带入；
	# 顶层不要用 "." 通配，否则 zip 内可能出现 ./ 前缀，旧版在线升级解析会失败。
	local entries=() entry
	for entry in bin conf lib LICENSE CHANGELOG.md CHANGELOG-BETA.md; do
		if [ -e "${run_dir}/${entry}" ]; then
			entries+=("$entry")
		fi
	done
	[ "${#entries[@]}" -gt 0 ] || die "运行目录为空：${run_dir}"
	[ -d "${run_dir}/bin" ] || die "运行目录缺少 bin：${run_dir}"
	[ -d "${run_dir}/lib" ] || die "运行目录缺少 lib：${run_dir}"

	rm -f "$zip_file" "${zip_file}.sha1"
	(
		cd "$run_dir"
		zip -q -r "$zip_file" "${entries[@]}"
	)
	[ -s "$zip_file" ] || die "zip 生成失败：${zip_file}"
	unzip -tqq "$zip_file" >/dev/null || die "${zip_file##*/} zip 完整性校验失败"
	if unzip -Z1 "$zip_file" | grep -qE '^\./'; then
		die "${zip_file##*/} 内含 ./ 前缀条目（旧版在线升级解析会失败）"
	fi
	unzip -Z1 "$zip_file" | grep -qx "lib/${kind}-${version}.jar" ||
		die "${zip_file##*/} 缺少 lib/${kind}-${version}.jar"

	write_sha1 "$zip_file"
	log "OK  ${kind}-${version}-release.zip  $(file_size "$zip_file") bytes  sha1=$(cat "${zip_file}.sha1")"
}

kinds=()
case "$kind" in
all) kinds=(server agent) ;;
*) kinds=("$kind") ;;
esac

for item in "${kinds[@]}"; do
	pack_kind "$item"
done
log "完成：版本 ${version}"
