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
# Jpom 发布产物完整性校验（release.sh 与 GitHub Actions release-package 工作流共用）
#
# 校验项：
#   1. server/agent 的 zip、tar.gz 及 .sha1 存在且 sha1 一致、压缩包完整
#   2. 从 zip 中提取 lib/<kind>-<version>.jar，检查 class 魔数/常量池非空/无 Unresolved compilation
#   3. jar 内 BOOT-INF/classes 与 modules/<kind>/target/classes 逐字节一致（防止打入旧/坏 class）
#   4. server jar 包含前端产物 BOOT-INF/classes/dist/index.html
#   5. 解压运行目录存在且含 bin/
#   6. --verify-go：go-agent/dist 下的 Go 插件端产物（5 平台安装包 + 5 平台升级容器 + linux/amd64 兼容别名）：
#      命名、sha1、压缩包完整性、安装包目录结构（bin/agent(.exe)+conf）、升级容器 MANIFEST/
#      agent.json（platform 必须与文件名平台一致）/容器内对应平台可执行文件 sha256、
#      别名与 linux/amd64 容器逐字节一致（供旧 Server 手工上传/默认回退）
#
# 用法：
#   bash script/verify-release-artifacts.sh --parent-dir jpom-parent --version 2.11.14 [--log-dir DIR]
#   bash script/verify-release-artifacts.sh --parent-dir DIR --version V --downloaded   # 校验从 R2 下载的产物（无 target/classes）
#   bash script/verify-release-artifacts.sh --parent-dir DIR --version V --verify-go --go-only   # 只校验 Go 插件端产物

#-----------------------------------------------------------

set -euo pipefail

parent_dir="jpom-parent"
version=""
log_dir=""
skip_target_compare=0
verify_go=0
go_only=0

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
	--verify-go)
		verify_go=1
		shift
		;;
	--go-only)
		verify_go=1
		go_only=1
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

# 校验 Go 插件端产物（release-package.yml 构建，go-agent/dist）
verify_go_artifacts() {
	local go_dist="${parent_dir}/go-agent/dist"
	[ -d "$go_dist" ] || die "缺少 Go 产物目录：${go_dist}（先执行 go-agent/script/package.sh 与 build-upgrade-container.sh）"

	# 平台清单：安装包与升级容器同为 5 平台；linux/amd64 另有兼容别名 upgrade.jar
	local go_platforms=("linux/amd64" "linux/arm64" "darwin/amd64" "darwin/arm64" "windows/amd64")
	local go_packages=() go_containers=() go_platform go_os go_arch
	for go_platform in "${go_platforms[@]}"; do
		go_os="${go_platform%/*}"
		go_arch="${go_platform#*/}"
		go_packages+=("agent-go-${version}-release-${go_os}-${go_arch}.zip")
		if [ "${go_os}" != "windows" ]; then
			go_packages+=("agent-go-${version}-release-${go_os}-${go_arch}.tar.gz")
		fi
		go_containers+=("agent-go-${version}-upgrade-${go_os}-${go_arch}.jar")
	done
	local go_alias="agent-go-${version}-upgrade.jar"
	local go_names=("${go_packages[@]}" "${go_containers[@]}" "${go_alias}")

	# 1) 存在性 / sha1 / 压缩包完整性
	local name file sha file_sha expected
	for name in "${go_names[@]}"; do
		file="${go_dist}/${name}"
		sha="${file}.sha1"
		[ -s "$file" ] || die "缺少 Go 产物：${file}"
		[ -s "$sha" ] || die "缺少 Go 产物校验文件：${sha}"
		file_sha=$(sha1_of "$file")
		expected=$(tr -d ' \r\n' <"$sha")
		[ "$file_sha" = "$expected" ] || die "${file} 的 sha1 与 .sha1 不一致"
		case "$name" in
		*.zip) unzip -t "$file" >/dev/null || die "${file} zip 完整性校验失败" ;;
		*.tar.gz) tar -tzf "$file" >/dev/null || die "${file} tar.gz 完整性校验失败" ;;
		*.jar) unzip -t "$file" >/dev/null || die "${file} jar 完整性校验失败" ;;
		esac
		log "OK  ${name}  $(stat -c '%s' "$file" 2>/dev/null || stat -f '%z' "$file") bytes  sha1=${file_sha:0:12}…"
	done

	# 2) 安装包目录结构：非 Windows 为 bin/agent，Windows 为 bin/agent.exe；都需 conf/application.yml
	local listing
	for name in "${go_packages[@]}"; do
		file="${go_dist}/${name}"
		case "$name" in
		*.zip) listing=$(unzip -Z1 "$file") ;;
		*.tar.gz) listing=$(tar -tzf "$file") ;;
		esac
		case "$name" in
		*-windows-*) grep -qE '(^|/)bin/agent\.exe$' <<<"$listing" || die "${name} 缺少 bin/agent.exe" ;;
		*) grep -qE '(^|/)bin/agent$' <<<"$listing" || die "${name} 缺少 bin/agent" ;;
		esac
		grep -qE '(^|/)conf/application\.yml$' <<<"$listing" || die "${name} 缺少 conf/application.yml"
		log "OK  ${name} 目录结构含 bin/agent(.exe) 与 conf/application.yml"
	done

	# 3) 升级容器：逐平台校验命名/元信息/容器内对应平台可执行文件
	mkdir -p "${log_dir}/go-upgrade"
	for go_platform in "${go_platforms[@]}"; do
		go_os="${go_platform%/*}"
		go_arch="${go_platform#*/}"
		verify_go_container "${go_dist}/agent-go-${version}-upgrade-${go_os}-${go_arch}.jar" "${go_platform}"
	done

	# 4) 兼容别名：必须与 linux/amd64 容器逐字节一致（旧 Server 手工上传/默认回退）
	local linux_jar="${go_dist}/agent-go-${version}-upgrade-linux-amd64.jar"
	cmp -s "${go_dist}/${go_alias}" "$linux_jar" || die "${go_alias} 必须与 ${linux_jar##*/} 逐字节一致（linux/amd64 兼容别名）"
	log "OK  ${go_alias} 为 linux/amd64 容器副本（sha1=$(sha1_of "${go_dist}/${go_alias}")）"
}

# 校验单个 Go 升级容器：命名/平台/可执行文件/MANIFEST/agent.json sha256
verify_go_container() {
	local jar=$1 expected_platform=$2
	local go_os="${expected_platform%/*}" go_arch="${expected_platform#*/}"
	local expected_bin="go-agent/bin/go-agent-${go_os}-${go_arch}"
	[ "$go_os" = "windows" ] && expected_bin="${expected_bin}.exe"
	local jar_dir="${log_dir}/go-upgrade"
	local entries="${jar_dir}/entries-${go_os}-${go_arch}.txt"
	local manifest="${jar_dir}/MANIFEST-${go_os}-${go_arch}.MF"
	local agent_json="${jar_dir}/agent-${go_os}-${go_arch}.json"
	local agent_bin="${jar_dir}/agent-${go_os}-${go_arch}.bin"
	[ -s "$jar" ] || die "缺少 Go 升级容器：${jar}"
	unzip -Z1 "$jar" >"$entries" || die "无法读取 Go 升级容器：${jar}"
	grep -qx 'META-INF/MANIFEST.MF' "$entries" || die "${jar##*/} 缺少 META-INF/MANIFEST.MF"
	grep -qx 'go-agent/agent.json' "$entries" || die "${jar##*/} 缺少 go-agent/agent.json"
	grep -qx 'BOOT-INF/classes/org/dromara/jpom/JpomAgentApplication.class' "$entries" ||
		die "${jar##*/} 缺少 BOOT-INF/classes 空壳 Main-Class class（旧 Server 的 checkJpomJar 需要）"
	grep -qx 'org/dromara/jpom/JpomAgentApplication.class' "$entries" ||
		die "${jar##*/} 缺少 jar 根空壳 Main-Class class（旧 Server 的 loadClass 需要）"
	grep -qx "$expected_bin" "$entries" || die "${jar##*/} 缺少 ${expected_bin}（平台/命名不一致）"
	unzip -p "$jar" META-INF/MANIFEST.MF | tr -d '\r' >"$manifest"
	grep -qx "Implementation-Version: ${version}" "$manifest" || die "${jar##*/} MANIFEST 的 Implementation-Version 与 ${version} 不一致"
	grep -q '^Jpom-Timestamp: ' "$manifest" || die "${jar##*/} MANIFEST 缺少 Jpom-Timestamp"
	grep -q '^Jpom-Min-Version: ' "$manifest" || die "${jar##*/} MANIFEST 缺少 Jpom-Min-Version"
	grep -q '^Main-Class: ' "$manifest" || die "${jar##*/} MANIFEST 缺少 Main-Class"
	unzip -p "$jar" go-agent/agent.json >"$agent_json"
	unzip -p "$jar" "$expected_bin" >"$agent_bin"
	python3 - "$version" "$expected_platform" "$agent_json" "$agent_bin" <<'PY' || die "${jar##*/} 的 agent.json/可执行文件校验失败"
import hashlib
import json
import re
import sys

version, expected_platform, agent_json, agent_bin = sys.argv[1:5]
with open(agent_json, encoding="utf-8") as fh:
    data = json.load(fh)
if data.get("version") != version:
    raise SystemExit("agent.json.version != release version: %r" % data.get("version"))
if not data.get("minVersion"):
    raise SystemExit("agent.json.minVersion 为空")
if not data.get("timeStamp"):
    raise SystemExit("agent.json.timeStamp 为空")
platform = (data.get("platform") or "").strip().lower()
if platform != expected_platform:
    raise SystemExit("agent.json.platform 与容器文件名平台不一致: %r != %r" % (platform, expected_platform))
expected = (data.get("sha256") or "").lower()
if not re.fullmatch(r"[0-9a-f]{64}", expected):
    raise SystemExit("agent.json.sha256 不是合法 sha256: %r" % data.get("sha256"))
with open(agent_bin, "rb") as fh:
    actual = hashlib.sha256(fh.read()).hexdigest()
if actual != expected:
    raise SystemExit("agent.json.sha256 与容器内可执行文件不一致: %r != %r" % (actual, expected))
PY
	log "OK  Go 升级容器 ${jar##*/}（platform=${expected_platform}，MANIFEST/agent.json/可执行文件 sha256）"
}

main() {
	log "开始校验产物：version=${version} parent_dir=${parent_dir}"
	if [ "$go_only" = "0" ]; then
		verify_java_artifacts
	fi
	if [ "$verify_go" = "1" ]; then
		verify_go_artifacts
	fi
	log "校验完成"
}

# 原有 Java 产物校验（含 target/classes 逐字节比对，--downloaded 时跳过）
verify_java_artifacts() {

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

main "$@"
