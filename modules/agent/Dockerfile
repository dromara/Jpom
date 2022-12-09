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

VOLUME ["/root/.m2"]
# 多次 builder 不同的版本号
ARG TEMP_VERSION=""
ARG JPOM_VERSION
ENV USE_JPOM_VERSION=${JPOM_VERSION}${TEMP_VERSION}
RUN --mount=type=cache,target=/root/.m2 sh ./script/replaceVersion.sh ${USE_JPOM_VERSION}

RUN --mount=type=cache,target=/root/.m2 mvn -B -e -T 1C clean package -pl modules/agent -am -Dmaven.test.skip=true -Dmaven.compile.fork=true -s script/settings.xml

FROM openjdk:8
ARG BUILD_DATE
ARG JPOM_VERSION
ARG TEMP_VERSION=""
ARG RUN_ARG=""
ARG DEPENDENCY=/target/dependency

LABEL build_info="dromara/Jpom build-date:- ${BUILD_DATE}"
LABEL maintainer="bwcx-jzy <bwcx_jzy@163.com>"
LABEL documentation="https://jpom.top"

ENV JPOM_HOME	/usr/local/jpom-agent
ENV JPOM_PKG_VERSION	${JPOM_VERSION}${TEMP_VERSION}
ENV JPOM_PKG    agent-${JPOM_PKG_VERSION}-release
ENV RUN_ARG ${RUN_ARG}

WORKDIR $JPOM_HOME

COPY --from=builder ${DEPENDENCY}/modules/agent/target/${JPOM_PKG} ${JPOM_HOME}

# 时区
ENV TZ Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

VOLUME $JPOM_HOME
EXPOSE 2123

ENTRYPOINT ["/bin/bash", "./bin/Agent.sh", "start"]

