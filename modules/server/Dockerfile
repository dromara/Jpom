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

# syntax = docker/dockerfile:experimental

FROM maven:3.8.5-jdk-8-slim as builder
WORKDIR /target/dependency
COPY . .

VOLUME ["/root/.m2","/target/dependency/web-vue/node_modules"]
# 多次 builder 不同的版本号
ARG TEMP_VERSION=""
ARG JPOM_VERSION
ENV USE_JPOM_VERSION=${JPOM_VERSION}${TEMP_VERSION}
RUN --mount=type=cache,target=/root/.m2 sh ./script/replaceVersion.sh ${USE_JPOM_VERSION}

ENV NODE_VERSION 16.13.1

RUN set -eux; \
    ARCH="$(dpkg --print-architecture)"; \
    case "${ARCH}" in \
       aarch64|arm64) \
         BINARY_ARCH='arm64'; \
         ;; \
       amd64|x86_64) \
         BINARY_ARCH='x64'; \
         ;; \
       *) \
         echo "Unsupported arch: ${ARCH}"; \
         exit 1; \
         ;; \
    esac; \
    curl -LfsSo /opt/node-v${NODE_VERSION}-linux-${BINARY_ARCH}.tar.gz https://npmmirror.com/mirrors/node/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${BINARY_ARCH}.tar.gz \
    && tar -zxvf /opt/node-v${NODE_VERSION}-linux-${BINARY_ARCH}.tar.gz -C /usr/local --strip-components=1  \
    && ln -s /usr/local/bin/node /usr/local/bin/nodejs \
    && npm config set registry https://registry.npmmirror.com/

RUN --mount=type=cache,target=/target/dependency/web-vue/node_modules cd web-vue && npm install && npm run build

RUN --mount=type=cache,target=/root/.m2 mvn -B -e -T 1C clean package -pl modules/server -am -Dmaven.test.skip=true -Dmaven.compile.fork=true -s script/settings.xml

FROM maven:3.8.5-jdk-8

ARG BUILD_DATE
ARG JPOM_VERSION
ARG TEMP_VERSION=""
ARG DEPENDENCY=/target/dependency

LABEL build_info="dromara/Jpom build-date:- ${BUILD_DATE}"
LABEL maintainer="bwcx-jzy <bwcx_jzy@163.com>"
LABEL documentation="https://jpom.top"

ENV JPOM_HOME   /usr/local/jpom-server
ENV JPOM_PKG_VERSION	${JPOM_VERSION}${TEMP_VERSION}
ENV JPOM_PKG    server-${JPOM_PKG_VERSION}-release

ENV JPOM_DATA_PATH ${JPOM_HOME}/data
ENV JPOM_LOG_PATH ${JPOM_HOME}/log
WORKDIR $JPOM_HOME

COPY --from=builder ${DEPENDENCY}/modules/server/target/${JPOM_PKG} ${JPOM_HOME}

# 时区
ENV TZ Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 数据目录
ENV jpom.path ${JPOM_DATA_PATH}

VOLUME $JPOM_DATA_PATH $JPOM_LOG_PATH

EXPOSE 2122

HEALTHCHECK CMD curl -X POST -f http://localhost:2122/check-system || exit 1

ENTRYPOINT ["/bin/bash", "./bin/Server.sh", "start"]


