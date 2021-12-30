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

FROM centos:7

ENV LANG		en_US.utf8

ENV JPOM_HOME		/usr/local/jpom-server/
ENV JAVA_PATH		/usr/local/java/
ENV MAVEN_PATH		/usr/local/maven/
ENV JPOM_PKG		server-2.8.3-release.zip
ENV JDK_PKG			jdk-8u251-linux-x64.tar.gz
ENV JDK_BIN_PATH	jdk1.8.0_251
ENV MAVEN_PKG		apache-maven-3.6.3-bin.tar.gz
ENV MAVEN_BIN_PATH 	apache-maven-3.8.4

# 初始化目录
RUN mkdir -p $JPOM_HOME
RUN mkdir -p $JAVA_PATH
RUN mkdir -p $MAVEN_PATH

# 安装 wget 、 unzip  插件
RUN yum install -y wget
RUN yum install -y unzip

# 下载依赖包
RUN wget -O $JPOM_HOME/$JPOM_PKG https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/$JPOM_PKG
RUN wget -O $JAVA_PATH/$JDK_PKG https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/$JDK_PKG
RUN wget -O $MAVEN_PATH/$MAVEN_PKG https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/$MAVEN_PKG

# 解压 安装 jdk
RUN tar -zxf $JAVA_PATH/$JDK_PKG  -C $JAVA_PATH
ENV JAVA_HOME	$JAVA_PATH/$JDK_BIN_PATH
ENV	CLASSPATH	.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
ENV	PATH		$JAVA_HOME/bin:$PATH

# 解压 安装 maven
RUN tar -zxf $MAVEN_PATH/$MAVEN_PKG  -C $MAVEN_PATH
ENV MAVEN_HOME	$MAVEN_PATH/$MAVEN_BIN_PATH
ENV PATH		$MAVEN_HOME/bin:$PATH

# 解压 jpom
RUN unzip -o $JPOM_HOME/$JPOM_PKG -d $JPOM_HOME
RUN chmod +x $JPOM_HOME/Server.sh

# 清理资源
RUN rm -rf $JAVA_PATH/$JDK_PKG
RUN rm -rf $MAVEN_PATH/$MAVEN_PKG
RUN rm -rf $JPOM_HOME/$JPOM_PKG

WORKDIR $JPOM_HOME

EXPOSE 2122

ENTRYPOINT ["/bin/sh", "Server.sh", "start"]
