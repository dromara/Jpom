#!/bin/bash

DIR_PATH=$(cd `dirname $0`; pwd)"/"

echo ${DIR_PATH}

# 提交代码 docs
cd ${DIR_PATH} && cd docs && git add . &&  git commit -m 'update docs pages' && git push

# 打包
cd ${DIR_PATH} && cd docs && yarn run build

# 删除文件
cd ${DIR_PATH} && cd docs-pages && ls -lt | awk '{ print $9 }' | xargs rm -rf

# 复制文件
cd ${DIR_PATH} && cp -r ./docs/dist/* ./docs-pages

# 提交代码 pages
cd ${DIR_PATH} && cd docs-pages && git add . &&  git commit -m 'update docs pages' && git push

#
#cd ~ && ./ossutilmac64 sync ${DIR_PATH}/docs-pages  oss://jpom-download/  --delete -f


