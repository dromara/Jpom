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

# 受限沙箱/CI 下 HOME 的 Go 构建缓存可能只读，统一把缓存放到仓库内。
# 用法：bash script/go.sh build ./... | bash script/go.sh test ./... | bash script/go.sh mod tidy
set -euo pipefail
cd "$(dirname "$0")/.."
export GOCACHE="${GOCACHE:-$PWD/.cache/build}"
export GOTMPDIR="${GOTMPDIR:-$PWD/.cache/tmp}"
export GOMODCACHE="${GOMODCACHE:-$PWD/.cache/mod}"
# 受限沙箱/CI 的全局 module cache 可能只读，统一放到仓库内
export GOSUMDB="${GOSUMDB:-off}"
export GOFLAGS="${GOFLAGS:--mod=mod}"
mkdir -p "$GOCACHE" "$GOTMPDIR" "$GOMODCACHE"
exec go "$@"