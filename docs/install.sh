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
    if [[ ! -x "${JAVA_HOME}/bin/java" ]]; then
      JAVA=`which java`
      if [[ ! -x "$JAVA" ]]; then
        # 判断是否存在文件
        if [[ ! -f "jdk-8u251-linux-x64.tar.gz" ]]; then
         wget -O jdk-8u251-linux-x64.tar.gz https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/jdk-8u251-linux-x64.tar.gz
        fi
        mkdir /usr/java/
        #
        tar -zxvf jdk-8u251-linux-x64.tar.gz  -C /usr/java/
        #
        PATH=$PATH:/usr/java/jdk1.8.0_251/bin
        export PATH
        echo '安装jdk,路径/usr/java/jdk1.8.0_251/'
        # 修改环境变量
        echo ''>>/etc/profile
        echo 'export JAVA_HOME=/usr/java/jdk1.8.0_251'>>/etc/profile
        echo 'export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar'>>/etc/profile
        echo 'export PATH=$PATH:$JAVA_HOME/bin'>>/etc/profile
        export JAVA_HOME=/usr/java/jdk1.8.0_251
        # 删除jdk压缩包
        rm -f jdk-8u251-linux-x64.tar.gz
    fi
  fi
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