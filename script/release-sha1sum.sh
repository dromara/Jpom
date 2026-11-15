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


# 版本
jpom_version=$1
jpom_tag=$2

#Mirror_Host=download.fastgit.org
#Mirror_Host=hub.fastgit.xyz
#Mirror_Host=github.com

# 校验单个产物：tar.gz 为默认产物（必选）；zip 为过渡期兼容产物（可选，该版本未发布时跳过）
# 用法：checkItem <server|agent> <tar.gz|zip> [required|optional]
function checkItem()
{
local kind=$1
local ext=$2
local required=${3:-required}
local name="$kind-${jpom_version}-release.$ext"

rm -f "$name.sha1" "$name"

if ! curl -LfsSo "$name.sha1" "https://d2.jpom.download/${jpom_tag}/${jpom_version}/$name.sha1"; then
	if [ "$required" = "optional" ]; then
		echo "跳过 $name（该版本未发布）"
		rm -f "$name.sha1"
		return 0
	fi
	echo "错误：$name.sha1 下载失败" >&2
	rm -f "$name.sha1"
	return 1
fi

ESUM=`cat "$name.sha1"`

echo "$name => ${ESUM}"

curl -LfsSo "$name" "https://d2.jpom.download/${jpom_tag}/${jpom_version}/$name"

echo "${ESUM} $name" | sha1sum -c -;

rm -f "$name.sha1" "$name"
}

# check agent：tar.gz 必选；zip 为过渡期兼容（过渡窗口结束后不再产出）
checkItem agent tar.gz required
checkItem agent zip optional

# check server：tar.gz 必选；zip 为过渡期兼容
checkItem server tar.gz required
checkItem server zip optional
