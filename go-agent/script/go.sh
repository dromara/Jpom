#!/usr/bin/env bash
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
#
# 受限沙箱/CI 下 HOME 的 Go 构建缓存可能只读，统一把缓存放到仓库内。
# 用法：bash script/go.sh build ./... | bash script/go.sh test ./... | bash script/go.sh mod tidy
set -euo pipefail
cd "$(dirname "$0")/.."
export GOCACHE="${GOCACHE:-$PWD/.cache/build}"
export GOTMPDIR="${GOTMPDIR:-$PWD/.cache/tmp}"
mkdir -p "$GOCACHE" "$GOTMPDIR"
exec go "$@"
