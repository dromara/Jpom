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


DIR_PATH=$(cd `dirname $0`; pwd)"/../"

echo ${DIR_PATH}

# 提交代码 docs
cd ${DIR_PATH} && cd docs && git add . &&  git commit -m 'update docs pages' && git push

# 打包
cd ${DIR_PATH} && cd docs && yarn && yarn run build

OS="$(uname)"
case "${OS}" in
Linux)
	ossUtil='ossutil64'
	;;
Darwin)
	ossUtil='ossutilmac64'
	;;
*)
	echo "Unsupported os: ${OS}"
	exit 1
	;;
esac

# 同步到 oss 中
${ossUtil} sync ${DIR_PATH}/docs/dist/ oss://jpom-docs/ --delete -f --exclude .DS_Store

# 删除文件
#cd ${DIR_PATH} && cd docs-pages && ls -lt | awk '{ print $9 }' | xargs rm -rf

# 复制文件
#cd ${DIR_PATH} && cp -r ./docs/dist/* ./docs-pages

# 提交代码 pages
#cd ${DIR_PATH} && cd docs-pages && git add . &&  git commit -m 'update docs pages' && git push



