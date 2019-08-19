#!/bin/bash
PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin:~/bin
export PATH
LANG=en_US.UTF-8

# 解压命令
if [[ ! -f "/usr/bin/unzip" ]];then
	#rm -f /etc/yum.repos.d/epel.repo
	yum install unzip -y
fi
# 下载
wget -O server.zip https://jpom.keepbx.cn/api/releases/server
# 解压
unzip server.zip