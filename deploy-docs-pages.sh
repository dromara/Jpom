#!/bin/bash

DIR_PATH=$(cd `dirname $0`; pwd)"/"

echo ${DIR_PATH}

# 提交代码 docs
cd ${DIR_PATH} && cd docs && git add . &&  git commit -m 'update docs pages' && git push

# 打包
cd ${DIR_PATH} && cd docs && yarn run build

# 同步到 oss 中
ossutilmac64 sync ${DIR_PATH}/docs/dist/ oss://jpom-docs/ --delete -f

# 删除文件
#cd ${DIR_PATH} && cd docs-pages && ls -lt | awk '{ print $9 }' | xargs rm -rf

# 复制文件
#cd ${DIR_PATH} && cp -r ./docs/dist/* ./docs-pages

# 提交代码 pages
#cd ${DIR_PATH} && cd docs-pages && git add . &&  git commit -m 'update docs pages' && git push



