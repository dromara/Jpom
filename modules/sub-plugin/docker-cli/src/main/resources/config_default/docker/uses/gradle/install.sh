#
# Copyright (c) 2019 Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

if [ $GRADLE_VERSION ]; then
	echo "GRADLE_VERSION ${GRADLE_VERSION}"
else
	echo "not found GRADLE_VERSION"
	exit 1
fi
cd /tmp
download_url="https://downloads.gradle-dn.com/distributions/gradle-${GRADLE_VERSION}-bin.zip"
apt install unzip
wget ${download_url} -O gradle-bin.zip
unzip -q -d /opt/gradle/ -o gradle-bin.zip
mv -f /opt/gradle/gradle-${GRADLE_VERSION}/* /opt/gradle
rm -rf /opt/gradle/gradle-${GRADLE_VERSION}
