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

ENV JPOM_HOME	/usr/local/jpom-server
ENV JPOM_PKG	server-2.8.2-release.zip

ADD jdk-8u202-linux-x64.tar.gz /usr/local/java/
ENV JAVA_HOME	/usr/local/java/jdk1.8.0_202
ENV	CLASSPATH	.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
ENV	PATH		$JAVA_HOME/bin:$PATH

ADD apache-maven-3.8.4-bin.tar.gz /usr/local/maven/
ENV MAVEN_HOME	/usr/local/maven/apache-maven-3.8.4
ENV PATH		$MAVEN_HOME/bin:$PATH

RUN yum install -y unzip

RUN mkdir -p $JPOM_HOME
COPY $JPOM_PKG $JPOM_HOME
RUN unzip -o $JPOM_HOME/$JPOM_PKG -d $JPOM_HOME
RUN chmod +x $JPOM_HOME/Server.sh
RUN rm -rf $JPOM_HOME/$JPOM_PKG

WORKDIR $JPOM_HOME

EXPOSE 2122

ENTRYPOINT ["/bin/sh", "Server.sh", "start"]
