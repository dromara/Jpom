#!/usr/bin/env bash
#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#
# 构建 Go 插件端在线升级容器包（jar 仅作为运输容器，让旧版 Server 的
# checkJpomJar 校验通过；Go 插件端识别 go-agent/agent.json 后自升级）。
#
# 用法（单平台，兼容旧调用）：
#   VERSION=2.11.13.2 PLATFORM=linux/amd64 BINARY=build/agent-linux-amd64 bash script/build-upgrade-container.sh
# 用法（多平台，默认 5 平台，二进制取 build/agent-<os>-<arch>[.exe]）：
#   VERSION=2.11.13.2 bash script/build-upgrade-container.sh
#
# 产物：dist/agent-go-<version>-upgrade-<os>-<arch>.jar + .sha1
#       PLATFORM=linux/amd64 时额外生成兼容别名 dist/agent-go-<version>-upgrade.jar
# 目标平台可执行文件必须能通过 Agent 端 machine 校验（ELF/PE/Mach-O）。
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="${VERSION:-}"
if [[ -z "$VERSION" && $# -gt 0 ]]; then
  VERSION="$1"
fi
if [[ -z "$VERSION" ]]; then
  echo "错误：必须显式设置 VERSION（不再回退 script/tag.release.txt），例如 VERSION=2.11.13.2 bash script/build-upgrade-container.sh" >&2
  exit 1
fi
MIN_VERSION="${MIN_VERSION:-1.0.0}"
TIME_STAMP="${TIME_STAMP:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"
OUT_DIR="${OUT_DIR:-dist}"
PLATFORMS="${PLATFORMS:-linux/amd64 linux/arm64 darwin/amd64 darwin/arm64 windows/amd64}"
mkdir -p "$OUT_DIR"

# JDK8 的 javac 不支持 --release 8，自动探测并回退到 -source/-target。
JAVAC="${JAVAC:-}"
JAR="${JAR:-}"
if [[ -z "$JAVAC" ]]; then
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
    JAVAC="$JAVA_HOME/bin/javac"
  else
    JAVAC="javac"
  fi
fi
if [[ -z "$JAR" ]]; then
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/jar" ]]; then
    JAR="$JAVA_HOME/bin/jar"
  else
    JAR="jar"
  fi
fi
command -v "$JAVAC" >/dev/null 2>&1 || { echo "错误：找不到 javac（请设置 JAVA_HOME 或 JAVAC）" >&2; exit 1; }
command -v "$JAR" >/dev/null 2>&1 || { echo "错误：找不到 jar（请设置 JAVA_HOME 或 JAR）" >&2; exit 1; }
JAVAC_PLATFORM_ARGS=()
if "$JAVAC" --help 2>&1 | grep -q -- "--release"; then
  JAVAC_PLATFORM_ARGS=(--release 8)
else
  JAVAC_PLATFORM_ARGS=(-source 1.8 -target 1.8)
fi

containers=()

# build_container PLATFORM BINARY OUT
build_container() {
  local platform="$1"
  local binary="$2"
  local out="$3"
  local os="${platform%/*}"
  local arch="${platform#*/}"
  if [[ -z "$os" || -z "$arch" || "$os" == "$platform" ]]; then
    echo "错误：平台格式应为 <os>/<arch>，实际: $platform" >&2
    return 2
  fi
  if [[ ! -f "$binary" ]]; then
    echo "找不到二进制: $binary（先执行 package.sh 或交叉编译 build/agent-$os-$arch）" >&2
    return 2
  fi
  local bin_name="go-agent-$os-$arch"
  if [[ "$os" == "windows" ]]; then
    bin_name="$bin_name.exe"
  fi
  local work
  work=$(mktemp -d)
  trap "rm -rf $work" RETURN
  mkdir -p "$work/root/go-agent/bin" "$work/root/BOOT-INF/classes/org/dromara/jpom" "$work/classes/org/dromara/jpom"

  # 1) 空壳 Main-Class（旧 Server checkJpomJar 需要能 loadClass）
  cat > "$work/classes/org/dromara/jpom/JpomAgentApplication.java" <<JAVA
package org.dromara.jpom;

/** 占位类：仅供旧版 Server 的 checkJpomJar 校验 Main-Class 可加载。 */
public class JpomAgentApplication {
}
JAVA
  "$JAVAC" "${JAVAC_PLATFORM_ARGS[@]}" -d "$work/classes" "$work/classes/org/dromara/jpom/JpomAgentApplication.java"
  cp "$work/classes/org/dromara/jpom/JpomAgentApplication.class" "$work/root/BOOT-INF/classes/org/dromara/jpom/"
  # 旧 Server 用 JarClassLoader 从 jar 根加载 Main-Class，占位类还需放在根路径。
  mkdir -p "$work/root/org/dromara/jpom"
  cp "$work/classes/org/dromara/jpom/JpomAgentApplication.class" "$work/root/org/dromara/jpom/"

  # 2) 升级元信息 + 目标平台可执行文件
  local sha256
  sha256=$(sha256sum "$binary" | cut -d" " -f1)
  cp "$binary" "$work/root/go-agent/bin/$bin_name"
  chmod +x "$work/root/go-agent/bin/$bin_name"
  cat > "$work/root/go-agent/agent.json" <<EOF
{"version":"$VERSION","timeStamp":"$TIME_STAMP","minVersion":"$MIN_VERSION","platform":"$platform","sha256":"$sha256"}
EOF

  # 3) MANIFEST（旧 Server checkJpomJar 必需字段）
  cat > "$work/MANIFEST.MF" <<EOF
Manifest-Version: 1.0
Implementation-Version: $VERSION
Jpom-Timestamp: $TIME_STAMP
Main-Class: org.dromara.jpom.JpomAgentApplication
Jpom-Min-Version: $MIN_VERSION
EOF

  # 4) 打成 jar（MANIFEST 作为第一条 entry）
  local out_abs="$out"
  if [[ "$out_abs" != /* ]]; then
    out_abs="$PWD/$out_abs"
  fi
  mkdir -p "$(dirname "$out_abs")"
  (cd "$work/root" && "$JAR" cfm "$out_abs" "$work/MANIFEST.MF" .)
  rm -rf "$work"
  echo ">>> upgrade container: $out_abs"
}

# 兼容别名：linux/amd64 旧命名 agent-go-<ver>-upgrade.jar
add_alias_if_needed() {
  local platform="$1"
  local jar_file="$2"
  if [[ "$platform" == "linux/amd64" ]]; then
    local alias="$OUT_DIR/agent-go-$VERSION-upgrade.jar"
    if [[ "$jar_file" != "$alias" ]]; then
      cp "$jar_file" "$alias"
    fi
    echo ">>> compat alias: $alias"
  fi
}

build_platform() {
  local platform="$1"
  local os="${platform%/*}"
  local arch="${platform#*/}"
  local binary="${BINARY:-build/agent-$os-$arch}"
  if [[ "$os" == "windows" && "$binary" != *.exe ]]; then
    if [[ ! -f "$binary" && -f "$binary.exe" ]]; then
      binary="$binary.exe"
    fi
  fi
  local out="$OUT_DIR/agent-go-$VERSION-upgrade-$os-$arch.jar"
  if [[ -n "${OUT:-}" && "$SINGLE_PLATFORM" == "1" ]]; then
    out="$OUT"
  fi
  build_container "$platform" "$binary" "$out"
  containers+=("$out")
  add_alias_if_needed "$platform" "$out"
}

SINGLE_PLATFORM=0
if [[ -n "${BINARY:-}" || -n "${PLATFORM:-}" ]]; then
  SINGLE_PLATFORM=1
fi

if [[ "$SINGLE_PLATFORM" == "1" ]]; then
  build_platform "${PLATFORM:-linux/amd64}"
else
  for platform in ${PLATFORMS//,/ }; do
    build_platform "$platform"
  done
fi

for jar_file in "${containers[@]}"; do
  sha1sum "$jar_file" | cut -d" " -f1 > "$jar_file.sha1"
done
# 兼容别名也要有 sha1（如果存在）
if [[ -f "$OUT_DIR/agent-go-$VERSION-upgrade.jar" ]]; then
  sha1sum "$OUT_DIR/agent-go-$VERSION-upgrade.jar" | cut -d" " -f1 > "$OUT_DIR/agent-go-$VERSION-upgrade.jar.sha1"
fi

echo "--- Go 升级容器产物 (VERSION=$VERSION) ---"
for jar_file in "${containers[@]}"; do
  echo "$(cat "$jar_file.sha1")  $(basename "$jar_file")"
done
if [[ -f "$OUT_DIR/agent-go-$VERSION-upgrade.jar" ]]; then
  echo "$(cat "$OUT_DIR/agent-go-$VERSION-upgrade.jar.sha1")  agent-go-$VERSION-upgrade.jar (compat alias)"
fi
