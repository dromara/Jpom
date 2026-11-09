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
# Go 插件端统一门禁：格式 / 构建 / vet / 单测 / 交叉编译 / i18n 检查。
# 用法：bash script/final-gate.sh [--with-i18n]
set -euo pipefail
cd "$(dirname "$0")/.."

export GOCACHE="${GOCACHE:-$PWD/.cache/build}"
export GOTMPDIR="${GOTMPDIR:-$PWD/.cache/tmp}"
export GOMODCACHE="${GOMODCACHE:-$PWD/.cache/mod}"
export GOSUMDB="${GOSUMDB:-off}"
export GOFLAGS="${GOFLAGS:--mod=mod}"
mkdir -p "$GOCACHE" "$GOTMPDIR" "$GOMODCACHE"

echo '==> gofmt'
unformatted=$(gofmt -l $(find . -path ./.cache -prune -o -name "*.go" -print) || true)
if [[ -n "$unformatted" ]]; then
  echo "以下文件未 gofmt："
  echo "$unformatted"
  exit 1
fi

echo '==> go build ./...'
go build ./...

echo '==> go vet ./...'
go vet ./...

echo '==> go test ./...'
go test ./...

echo '==> cross compile'
CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -trimpath -ldflags '-s -w' -o /tmp/jpom-agent-linux-amd64 .
CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -trimpath -ldflags '-s -w' -o /tmp/jpom-agent-linux-arm64 .
CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build -trimpath -ldflags '-s -w' -o /tmp/jpom-agent-windows-amd64.exe .
CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build -trimpath -ldflags '-s -w' -o /tmp/jpom-agent-darwin-arm64 .

if [[ "${1:-}" == "--with-i18n" ]]; then
  echo '==> i18n check (strict)'
  python3 script/i18n-check.py --strict
fi

echo '==> final gate passed'