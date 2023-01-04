#!/bin/bash
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

if [ $JAVA_VERSION ]; then
	echo "JAVA_VERSION ${JAVA_VERSION}"
else
	echo "not found JAVA_VERSION"
	exit 1
fi
cd /tmp
download_url=""
ARCH_O=`uname -m`
# https://mirrors.tuna.tsinghua.edu.cn/Adoptium/
# https://github.com/AdoptOpenJDK/openjdk-docker/blob/master/8/jdk/ubuntu/Dockerfile.hotspot.nightly.full
case "${ARCH_O}" in
	aarch64|arm64)
		ARCH='aarch64';
	;;
	armhf|armv7l)
		ARCH='arm';
	;;
	ppc64el|ppc64le)
		ARCH='ppc64le';
	;;
	s390x)
		ARCH='s390x';
	;;
	amd64|x86_64)
	 	ARCH='x64';
	;;
	*)
	 	echo "Unsupported arch: ${ARCH_O}";
	 	exit 1;
	;;
esac;

download_url=`curl -s https://gitee.com/dromara/Jpom/raw/download_link/jdk/${JAVA_VERSION}/${ARCH}`

wget ${download_url} -O jdk.tar.gz
tar -zxf jdk.tar.gz --strip-components 1 -C /opt/java/
