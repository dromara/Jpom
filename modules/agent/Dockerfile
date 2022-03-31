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

FROM maven:3.8.5-jdk-8-slim as builder
WORKDIR /target/dependency
COPY . .

RUN mvn -B -e -T 1C clean package -pl modules/agent -am -Dmaven.test.skip=true -Dmaven.compile.fork=true -s script/settings.xml

FROM openjdk:8
ENV JPOM_HOME	/usr/local/jpom-agent
ARG JPOM_VERSION
ENV JPOM_PKG    agent-${JPOM_VERSION}-release
WORKDIR $JPOM_HOME
ARG DEPENDENCY=/target/dependency
COPY --from=builder ${DEPENDENCY}/modules/agent/target/${JPOM_PKG} ${JPOM_HOME}

# 时区
ENV TZ Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

VOLUME $JPOM_HOME
EXPOSE 2123

ENTRYPOINT ["/bin/bash", "Agent.sh", "start"]

