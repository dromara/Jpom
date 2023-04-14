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
