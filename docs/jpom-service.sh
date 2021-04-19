#!/bin/bash

ROOT_PATH="$1"
FILE_NAME="$2"

echo '#!/bin/bash
# chkconfig: 356 10 90
# description: Jpom-Server service
# processname: jpom-server
# The MIT License (MIT)
#
# Copyright (c) 2019 码之科技工作室
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
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.' > $FILE_NAME

echo '# make sure sh could load enviroment variables, in particular, in the init.d directory
source /etc/profile' >> $FILE_NAME

echo "# 启动程序
function start() {
    ${ROOT_PATH}Server.sh start
}" >> $FILE_NAME

echo "# 停止程序
function stop() {
    ${ROOT_PATH}Server.sh stop
    status
}" >> $FILE_NAME

echo "# 获取程序状态
function status() {
    ${ROOT_PATH}Server.sh status
}" >> $FILE_NAME

echo '# 提示使用语法
function usage() {
    echo "Usage: $0 {start|stop|restart|status}"
    RETVAL="2"
}' >> $FILE_NAME

echo '# See how we were called.
RETVAL="0"
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        start
        ;;
    status)
        status
        ;;
    *)
      usage
      ;;
esac

exit $RETVAL' >> $FILE_NAME

rm -f jpom-service.sh
