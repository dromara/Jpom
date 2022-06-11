#!/bin/bash

DIR_PATH=$(cd `dirname $0`; pwd)"/"

echo ${DIR_PATH}

# 打包
cd ${DIR_PATH} && cd docs && yarn run build

# 删除文件
cd ${DIR_PATH} && cd docs-pages && ls -lt | awk '{ print $9 }' | xargs rm -rf

cd ${DIR_PATH} && cp -r ./docs/dist/* ./docs-pages

cd ${DIR_PATH} && cd docs-pages && git add . &&  git commit -m 'update docs pages'


