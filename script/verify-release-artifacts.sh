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
# Jpom 发布产物完整性校验（release.sh 与 GitHub Actions release-package 工作流共用）
#
# 校验项：
#   1. server/agent 的 zip、tar.gz 及 .sha1 存在且 sha1 一致、压缩包完整
#   2. 从 zip 中提取 lib/<kind>-<version>.jar，检查 class 魔数/常量池非空/无 Unresolved compilation
#   3. jar 内 BOOT-INF/classes 与 modules/<kind>/target/classes 逐字节一致（防止打入旧/坏 class）
#   4. server jar 包含前端产物 BOOT-INF/classes/dist/index.html
#   5. 解压运行目录存在且含 bin/
#
# 用法：
#   bash script/verify-release-artifacts.sh --parent-dir jpom-parent --version 2.11.14 [--log-dir DIR]
#   bash script/verify-release-artifacts.sh --parent-dir DIR --version V --downloaded   # 校验从 R2 下载的产物（无 target/classes）

#-----------------------------------------------------------

set -euo pipefail

parent_dir="jpom-parent"
version=""
log_dir=""
skip_target_compare=0

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
	--log-dir)
		log_dir="${2:?}"
		shift 2
		;;
	--downloaded)
		skip_target_compare=1
		shift
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
[ -d "$parent_dir" ] || {
	echo "ERROR: parent-dir 不存在：$parent_dir" >&2
	exit 2
}
parent_dir=$(cd "$parent_dir" && pwd)
log_dir="${log_dir:-$(cd "${parent_dir}/.." && pwd)/.tmp/verify-release-artifacts}"
mkdir -p "$log_dir"

log() { echo "[verify-artifacts] $*"; }
die() {
	echo "[verify-artifacts][ERROR] $*" >&2
	exit 1
}

sha1_of() {
	if command -v sha1sum >/dev/null 2>&1; then
		sha1sum "$1" | awk '{print $1}'
	else
		shasum -a 1 "$1" | awk '{print $1}'
	fi
}

main() {
	log "开始校验产物：version=${version} parent_dir=${parent_dir}"

	local kinds=(server agent)
	local exts=(zip tar.gz)
	local kind ext file sha file_sha expected

	for kind in "${kinds[@]}"; do
		for ext in "${exts[@]}"; do
			file="${parent_dir}/modules/${kind}/target/${kind}-${version}-release.${ext}"
			sha="${file}.sha1"
			[ -s "$file" ] || die "缺少产物：${file}"
			[ -s "$sha" ] || die "缺少校验文件：${sha}"
			file_sha=$(sha1_of "$file")
			expected=$(tr -d ' \r\n' <"$sha")
			[ "$file_sha" = "$expected" ] || die "${file} 的 sha1 与 .sha1 不一致"
			case "$ext" in
			zip) unzip -t "$file" >/dev/null || die "${file} zip 完整性校验失败" ;;
			tar.gz) tar -tzf "$file" >/dev/null || die "${file} tar.gz 完整性校验失败" ;;
			esac
			log "OK  ${kind}-${version}-release.${ext}  $(stat -c '%s' "$file") bytes  sha1=${file_sha:0:12}…"
		done
	done

	mkdir -p "${log_dir}/artifacts"
	for kind in "${kinds[@]}"; do
		local jar="${log_dir}/artifacts/${kind}.jar"
		unzip -p "${parent_dir}/modules/${kind}/target/${kind}-${version}-release.zip" "lib/${kind}-${version}.jar" >"$jar" ||
			die "无法从 ${kind} zip 中提取 lib/${kind}-${version}.jar"
		[ -s "$jar" ] || die "提取出的 ${kind} jar 为空"
		if ! python3 - "$jar" "$kind" "${parent_dir}/modules/${kind}/target/classes" "$skip_target_compare" <<'PY'
import io
import struct
import sys
import zipfile


def class_min_size(data):
	if len(data) < 10 or data[:4] != b"\xca\xfe\xba\xbe":
		return -1
	pos = 8
	try:
		count = struct.unpack(">H", data[8:10])[0]
		pos = 10
		i = 1
		while i < count:
			tag = data[pos]
			pos += 1
			if tag == 1:
				length = struct.unpack(">H", data[pos : pos + 2])[0]
				pos += 2 + length
			elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
				pos += 4
			elif tag in (5, 6):
				pos += 8
				i += 1
			elif tag in (7, 8, 16, 19, 20):
				pos += 2
			elif tag == 15:
				pos += 3
			else:
				return -1
			i += 1
		return pos
	except Exception:
		return -1


def scan_jar(zf, label):
	total = 0
	bad = []
	for info in zf.infolist():
		name = info.filename
		if name.endswith(".class"):
			total += 1
			data = zf.read(info)
			min_size = class_min_size(data)
			if min_size < 0 or len(data) < min_size:
				bad.append((label + "!" + name, len(data), min_size))
			elif b"Unresolved compilation problem" in data:
				bad.append((label + "!" + name, "unresolved-compilation", 0))
		elif name.startswith("BOOT-INF/lib/") and name.endswith(".jar"):
			try:
				nested = zipfile.ZipFile(io.BytesIO(zf.read(info)))
			except Exception as exc:  # noqa: BLE001
				bad.append((label + "!" + name, str(exc), 0))
				continue
			nested_total, nested_bad = scan_jar(nested, label + "!" + name)
			total += nested_total
			bad.extend(nested_bad)
	return total, bad


path, label, target_dir = sys.argv[1], sys.argv[2], sys.argv[3]
skip_target_compare = len(sys.argv) > 4 and sys.argv[4] == "1"
import os

with zipfile.ZipFile(path) as zf:
	total, bad = scan_jar(zf, label)
	# 关键：字节比对 BOOT-INF/classes 与 target/classes，防止 jar 打包了旧/坏 class
	# （--downloaded 模式下本地没有 target/classes，跳过逐字节比对，只做 class 魔数/截断校验）
	for info in zf.infolist():
		if skip_target_compare:
			break
		if not info.filename.startswith("BOOT-INF/classes/") or not info.filename.endswith(".class"):
			continue
		rel = info.filename[len("BOOT-INF/classes/") :]
		expected = os.path.join(target_dir, rel)
		if not os.path.exists(expected):
			bad.append((label + "!" + info.filename, "missing-in-target", 0))
			continue
		with open(expected, "rb") as fh:
			if fh.read() != zf.read(info):
				bad.append((label + "!" + info.filename, "differs-from-target", 0))
	print(f"{label}: {total} classes, bad={len(bad)}")
	if bad:
		for item in bad[:10]:
			print("BAD_CLASS", item)
		sys.exit(1)
PY
		then
			die "${kind} jar 完整性校验失败（存在空/截断/Unresolved compilation 的 class）"
		fi
		log "OK  ${kind} jar 完整性校验通过（class 非空/未截断）"
	done

	unzip -l "${log_dir}/artifacts/server.jar" | grep 'BOOT-INF/classes/dist/index.html' >/dev/null ||
		die "server jar 未包含前端产物 BOOT-INF/classes/dist/index.html"
	log "OK  server jar 含前端 dist"

	for kind in "${kinds[@]}"; do
		local dir="${parent_dir}/modules/${kind}/target/${kind}-${version}-release"
		[ -d "$dir" ] || die "缺少运行目录：${dir}"
		[ -d "${dir}/bin" ] || die "运行目录缺少 bin：${dir}"
	done
	log "运行目录检查通过"
}

main
