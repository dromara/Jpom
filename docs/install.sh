#!/bin/bash
PATH=$PATH:/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin:~/bin:$(cd `dirname $0`; pwd)
export PATH
LANG=en_US.UTF-8

# 解压命令
if [[ ! -f "/usr/bin/unzip" ]];then
	#rm -f /etc/yum.repos.d/epel.repo
	yum install unzip -y
fi
TYPE="$1"

module="$2"
# 判断是否包含jdk
if [[ "$module" = "jdk" ]]; then
  # 判断是否存在文件
  if [[ ! -f "jdk-8u251-linux-x64.tar.gz" ]]; then
   wget -O jdk-8u251-linux-x64.tar.gz https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/jdk-8u251-linux-x64.tar.gz
  fi
  #
  tar -zxvf jdk-8u251-linux-x64.tar.gz
  #
  PATH=$PATH:$(cd `dirname $0`; pwd)/jdk1.8.0_251/bin
  export PATH
fi

# 判断
if [[ -z "${TYPE}" ]] ; then
    TYPE="Server";
fi
# 判断是否在文件
if [[ ! -f "${TYPE}.zip" ]]; then
  # 下载
  wget -O ${TYPE}.zip https://1232788122276831.cn-beijing.fc.aliyuncs.com/2016-08-15/proxy/jpom/jpom-releases/?type=${TYPE}
fi
# 解压
unzip -o ${TYPE}.zip
# 删除安装包
rm -f ${TYPE}.zip
# 添加权限
chmod 755 ${TYPE}.sh
# 启动
sh ${TYPE}.sh start