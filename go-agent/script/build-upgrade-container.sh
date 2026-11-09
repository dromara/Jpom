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
# 构建 Go 插件端在线升级容器包（CONTRACT-UPGRADE 方案 B：jar 仅作为运输容器，
# 让旧版 Server 的 checkJpomJar 校验通过，Agent 端识别 go-agent/agent.json 后自升级）。
#
# 用法：
#   bash script/build-upgrade-container.sh
#   VERSION=2.11.13.1-go1 BINARY=build/agent-linux-amd64 bash script/build-upgrade-container.sh
#
# 产物：dist/agent-go-<version>-upgrade.jar
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="${VERSION:-}"
if [[ -z "${VERSION}" ]]; then
  if [[ -f ../script/tag.release.txt ]]; then
    VERSION=$(tr -d '[:space:]' < ../script/tag.release.txt)
  else
    VERSION=dev
  fi
fi
MIN_VERSION="${MIN_VERSION:-1.0.0}"
PLATFORM="${PLATFORM:-linux/amd64}"
BINARY="${BINARY:-build/agent-linux-amd64}"
OUT="${OUT:-dist/agent-go-$VERSION-upgrade.jar}"
TIME_STAMP="${TIME_STAMP:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"

[[ -f "$BINARY" ]] || { echo "找不到二进制: $BINARY（先执行 make build 或交叉编译）" >&2; exit 2; }
os="${PLATFORM%/*}"
arch="${PLATFORM#*/}"

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/root/go-agent/bin" "$work/root/BOOT-INF/classes/org/dromara/jpom" "$work/classes/org/dromara/jpom" "$(dirname "$OUT")"

# 1) 空壳 Main-Class（Server 的 checkJpomJar 需要能 loadClass）
cat > "$work/classes/org/dromara/jpom/JpomAgentApplication.java" <<'EOF'
package org.dromara.jpom;

/** 占位类：仅供旧版 Server 的 checkJpomJar 校验 Main-Class 可加载。 */
public class JpomAgentApplication {
}
EOF
javac --release 8 -d "$work/classes" "$work/classes/org/dromara/jpom/JpomAgentApplication.java"
cp "$work/classes/org/dromara/jpom/JpomAgentApplication.class" "$work/root/BOOT-INF/classes/org/dromara/jpom/"
# 旧版 Server 的 checkJpomJar 用 JarClassLoader.loadClass(Main-Class) 从 jar 根加载，
# 因此占位类还需放在 jar 根同一路径（BOOT-INF/classes 下保留供“是否 Jpom 包”校验）。
mkdir -p "$work/root/org/dromara/jpom"
cp "$work/classes/org/dromara/jpom/JpomAgentApplication.class" "$work/root/org/dromara/jpom/"

# 2) 升级元信息 + 可执行文件
sha256=$(sha256sum "$BINARY" | cut -d' ' -f1)
cp "$BINARY" "$work/root/go-agent/bin/go-agent-$os-$arch"
chmod +x "$work/root/go-agent/bin/go-agent-$os-$arch"
cat > "$work/root/go-agent/agent.json" <<EOF
{"version":"$VERSION","timeStamp":"$TIME_STAMP","minVersion":"$MIN_VERSION","platform":"$PLATFORM","sha256":"$sha256"}
EOF

# 3) MANIFEST（Server checkJpomJar 必需字段）
cat > "$work/MANIFEST.MF" <<EOF
Manifest-Version: 1.0
Implementation-Version: $VERSION
Jpom-Timestamp: $TIME_STAMP
Main-Class: org.dromara.jpom.JpomAgentApplication
Jpom-Min-Version: $MIN_VERSION
EOF

# 4) 打成 jar（MANIFEST 作为第一条 entry）
# OUT 可能传入相对或绝对路径，jar 需要在 root 目录内执行，这里先转成绝对路径。
OUT_ABS="$OUT"
if [[ "$OUT_ABS" != /* ]]; then
  OUT_ABS="$PWD/$OUT_ABS"
fi
mkdir -p "$(dirname "$OUT_ABS")"
(cd "$work/root" && jar cfm "$OUT_ABS" "$work/MANIFEST.MF" .)

echo "upgrade container: $OUT_ABS"
unzip -l "$OUT_ABS" | head -14