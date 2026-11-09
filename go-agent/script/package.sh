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
# 构建 Go 插件端发布包（多平台静态二进制 + 与 Java 插件端一致的目录布局）。
# 用法：VERSION=2.11.13.2 bash script/package.sh
#       VERSION=2.11.13.2 PLATFORMS="linux/amd64 linux/arm64" bash script/package.sh
# 产物：dist/agent-go-<version>-release-<os>-<arch>.tar.gz + .sha1（5 平台统一 tar.gz，Windows 也是 tar.gz）
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="${VERSION:-}"
if [[ -z "$VERSION" && $# -gt 0 ]]; then
  VERSION="$1"
fi
if [[ -z "$VERSION" ]]; then
  echo "错误：必须显式设置 VERSION（不再回退 script/tag.release.txt），例如 VERSION=2.11.13.2 bash script/package.sh" >&2
  exit 1
fi
BUILD_TIME="${BUILD_TIME:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"
OUT=dist
STAGE="$OUT/stage"
LDFLAGS="-s -w -X main.version=$VERSION -X main.buildTime=$BUILD_TIME"
PLATFORMS="${PLATFORMS:-linux/amd64 linux/arm64 darwin/amd64 darwin/arm64 windows/amd64}"

rm -rf "$STAGE"
mkdir -p "$OUT" "$STAGE"

built=()
for platform in ${PLATFORMS//,/ }; do
  os="${platform%/*}"
  arch="${platform#*/}"
  if [[ -z "$os" || -z "$arch" || "$os" == "$platform" ]]; then
    echo "错误：平台格式应为 <os>/<arch>，实际: $platform" >&2
    exit 1
  fi
  exe="agent"
  if [[ "$os" == "windows" ]]; then
    exe="agent.exe"
  fi
  name="agent-go-$VERSION-release-$os-$arch"
  dir="$STAGE/$name"
  mkdir -p "$dir/bin" "$dir/conf" "$dir/data" "$dir/logs" "$dir/tmp" "$dir/project-log"

  echo ">>> build $name ($platform)"
  CGO_ENABLED=0 GOOS="$os" GOARCH="$arch" \
    bash script/go.sh build -trimpath -ldflags "$LDFLAGS" -o "$dir/bin/$exe" .

  cp conf/application.yml "$dir/conf/application.yml"
  if [[ -f ../LICENSE ]]; then cp ../LICENSE "$dir/LICENSE"; fi
  if [[ -f ../CHANGELOG.md ]]; then cp ../CHANGELOG.md "$dir/CHANGELOG.md"; fi
  # /system/change_log 在 beta 通道会读 CHANGELOG-BETA.md，发布包需同时携带。
  if [[ -f ../CHANGELOG-BETA.md ]]; then cp ../CHANGELOG-BETA.md "$dir/CHANGELOG-BETA.md"; fi
  cp README.md "$dir/README-GO.md"
  if [[ "$os" == "windows" ]]; then
    cp script/AgentGo.bat "$dir/bin/AgentGo.bat"
  else
    cp script/AgentGo.sh "$dir/bin/AgentGo.sh"
    chmod +x "$dir/bin/AgentGo.sh" "$dir/bin/$exe"
  fi

  # 5 平台统一 tar.gz（含 Windows，内含 bin/agent.exe），不再产出 zip
  (cd "$STAGE" && tar czf "../${name}.tar.gz" "${name}")
  built+=("$OUT/${name}.tar.gz")
done

rm -rf "$STAGE"
for artifact in "${built[@]}"; do
  if [[ ! -s "$artifact" ]]; then
    echo "错误：产物缺失或为空: $artifact" >&2
    exit 1
  fi
  sha1sum "$artifact" | cut -d" " -f1 > "$artifact.sha1"
done

echo "--- Go 插件端发布产物 (VERSION=$VERSION) ---"
for artifact in "${built[@]}"; do
  echo "$(cat "$artifact.sha1")  $(basename "$artifact")"
done
