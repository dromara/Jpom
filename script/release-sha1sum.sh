#!/bin/bash
#
# Copyright (c) 2019 Code Technology Studio
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

function checkItem()
{
rm -f $1-${jpom_version}-release.$2.sha1 $1-${jpom_version}-release.$2

curl -LfsSo $1-${jpom_version}-release.$2.sha1 https://download.jpom.top/${jpom_tag}/${jpom_version}/$1-${jpom_version}-release.$2.sha1

ESUM=`cat $1-${jpom_version}-release.$2.sha1`

echo "$1-${jpom_version}-release.$2 => ${ESUM}"

curl -LfsSo $1-${jpom_version}-release.$2 https://download.jpom.top/${jpom_tag}/${jpom_version}/$1-${jpom_version}-release.$2

echo "${ESUM} $1-${jpom_version}-release.$2" | sha1sum -c -;

rm -f $1-${jpom_version}-release.$2.sha1 $1-${jpom_version}-release.$2
}

# check agent
checkItem agent tar.gz
checkItem agent zip

# check server
checkItem server tar.gz
checkItem server zip

