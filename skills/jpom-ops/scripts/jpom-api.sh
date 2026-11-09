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

# jpom-ops 兼容入口（Linux / macOS）。
#
# 跨平台实现见同目录 jpom_api.py（仅标准库，Windows 直接用
#   python scripts\jpom_api.py <command> ...）。
# 本脚本只是薄包装，避免 sh 与 py 两套实现漂移。
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"

if command -v python3 >/dev/null 2>&1; then
  exec python3 "$here/jpom_api.py" "$@"
fi

echo "jpom-ops 需要 Python 3.8+（Linux 请安装 python3；Windows 可直接运行:" >&2
echo "  python scripts\\jpom_api.py <command> ...）" >&2
exit 2
