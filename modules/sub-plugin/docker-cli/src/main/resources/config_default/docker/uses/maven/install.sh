#!/bin/bash
#
# Copyright (c) 2019 Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

if [ $MAVEN_VERSION ]; then
	echo "MAVEN_VERSION ${MAVEN_VERSION}"
else
	echo "not found MAVEN_VERSION"
	exit 1
fi
cd /tmp
download_url="https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
wget ${download_url} -O maven.tar.gz
tar -zxf maven.tar.gz --strip-components 1 -C /opt/maven/
