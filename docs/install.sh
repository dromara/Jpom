#!/bin/bash
PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin:~/bin
export PATH
LANG=en_US.UTF-8

# 解压命令
if [[ ! -f "/usr/bin/unzip" ]];then
	#rm -f /etc/yum.repos.d/epel.repo
	yum install unzip -y
fi
TYPE="$1"
# 判断
if [[ -z "${TYPE}" ]] ; then
    TYPE="Server";
fi
# 下载
wget -O ${TYPE}.zip https://jpom.keepbx.cn/api/releases/${TYPE}
# 解压
unzip -o ${TYPE}.zip
# 删除安装包
rm -f ${TYPE}.zip
# 启动
sh ${TYPE}.sh start