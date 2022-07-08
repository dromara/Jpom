#!/bin/bash
#
# The MIT License (MIT)
#
# Copyright (c) 2019 Code Technology Studio
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of
# this software and associated documentation files (the "Software"), to deal in
# the Software without restriction, including without limitation the rights to
# use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
# the Software, and to permit persons to whom the Software is furnished to do so,
# subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
# FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
# COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
# IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#


# 版本
jpom_version=2.9.3

#Mirror_Host=download.fastgit.org
#Mirror_Host=hub.fastgit.xyz
Mirror_Host=github.com

function checkItem()
{
rm -f $1-${jpom_version}-release.$2.sha1 $1-${jpom_version}-release.$2

curl -LfsSo $1-${jpom_version}-release.$2.sha1 https://${Mirror_Host}/dromara/Jpom/releases/download/v${jpom_version}/$1-${jpom_version}-release.$2.sha1

ESUM=`cat $1-${jpom_version}-release.$2.sha1`

echo "$1-${jpom_version}-release.$2 => ${ESUM}"

curl -LfsSo $1-${jpom_version}-release.$2 https://${Mirror_Host}/dromara/Jpom/releases/download/v${jpom_version}/$1-${jpom_version}-release.$2

echo "${ESUM} $1-${jpom_version}-release.$2" | sha1sum -c -;

rm -f $1-${jpom_version}-release.$2.sha1 $1-${jpom_version}-release.$2
}

# check agent
checkItem agent tar.gz
checkItem agent zip

# check server
checkItem server tar.gz
checkItem server zip

