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
# 用法：VERSION=2.11.13 bash script/package.sh
# 产物：dist/agent-go-<version>-release-<os>-<arch>.{zip,tar.gz,sha1}
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="${VERSION:-${1:-}}"
if [[ -z "${VERSION}" ]]; then
  if [[ -f ../script/tag.release.txt ]]; then
    VERSION=$(tr -d '[:space:]' < ../script/tag.release.txt)
  else
    VERSION=dev
  fi
fi
BUILD_TIME="${BUILD_TIME:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"
OUT=dist
STAGE="$OUT/stage"
LDFLAGS="-s -w -X main.version=$VERSION -X main.buildTime=$BUILD_TIME"

rm -rf "$STAGE"
mkdir -p "$OUT" "$STAGE"

# 平台列表：Linux 为主，保留 arm64 与 Windows 以覆盖常见部署环境
PLATFORMS=("${PLATFORMS:-linux/amd64 linux/arm64 windows/amd64}")

for platform in ${PLATFORMS//,/ }; do
  os="${platform%/*}"
  arch="${platform#*/}"
  exe="agent"
  [[ "$os" == "windows" ]] && exe="agent.exe"
  name="agent-go-$VERSION-release-$os-$arch"
  dir="$STAGE/$name"
  mkdir -p "$dir/bin" "$dir/conf" "$dir/data" "$dir/logs" "$dir/tmp" "$dir/project-log"

  echo ">>> build $name"
  CGO_ENABLED=0 GOOS="$os" GOARCH="$arch" \
    bash script/go.sh build -trimpath -ldflags "$LDFLAGS" -o "$dir/bin/$exe" .

  cp conf/application.yml "$dir/conf/application.yml"
  if [[ -f ../LICENSE ]]; then cp ../LICENSE "$dir/LICENSE"; fi
  if [[ -f ../CHANGELOG.md ]]; then cp ../CHANGELOG.md "$dir/CHANGELOG.md"; fi
  # /system/change_log 在 beta 通道会读 CHANGELOG-BETA.md（Java 从 classpath 读），发布包需同时携带
  if [[ -f ../CHANGELOG-BETA.md ]]; then cp ../CHANGELOG-BETA.md "$dir/CHANGELOG-BETA.md"; fi
  cp README.md "$dir/README-GO.md"
  if [[ "$os" == "windows" ]]; then
    cp script/AgentGo.bat "$dir/bin/AgentGo.bat"
  else
    cp script/AgentGo.sh "$dir/bin/AgentGo.sh"
    chmod +x "$dir/bin/AgentGo.sh" "$dir/bin/agent"
  fi

  (cd "$STAGE" && zip -q -r "../$name.zip" "$name")
  if [[ "$os" != "windows" ]]; then
    (cd "$STAGE" && tar czf "../$name.tar.gz" "$name")
  fi
  for artifact in "$OUT/$name.zip" "$OUT/$name.tar.gz"; do
    if [[ -f "$artifact" ]]; then
      sha1sum "$artifact" | awk '{print $1}' > "$artifact.sha1"
    fi
  done
done

rm -rf "$STAGE"
echo "--- artifacts ---"
ls -lh "$OUT"/agent-go-* | sed 's/  */ /g'
