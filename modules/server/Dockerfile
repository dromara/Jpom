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
    curl -LfsSo /opt/node-v16.13.1-linux-${BINARY_ARCH}.tar.gz https://npmmirror.com/mirrors/node/v16.13.1/node-v16.13.1-linux-${BINARY_ARCH}.tar.gz \
    && tar -zxvf /opt/node-v16.13.1-linux-${BINARY_ARCH}.tar.gz -C /opt/ && export PATH=/opt/node-v16.13.1-linux-${BINARY_ARCH}/bin:$PATH \
    && npm config set registry https://registry.npmmirror.com/ \
    && cd web-vue && npm install && npm run build

RUN mvn -B -e -T 1C clean package -pl modules/server -am -Dmaven.test.skip=true -Dmaven.compile.fork=true -s script/settings.xml

FROM maven:3.8.5-jdk-8
ENV JPOM_HOME   /usr/local/jpom-server
ARG JPOM_VERSION
ENV JPOM_PKG    server-${JPOM_VERSION}-release
ENV JPOM_DATA_PATH ${JPOM_HOME}/data
ENV JPOM_LOG_PATH ${JPOM_HOME}/log
WORKDIR $JPOM_HOME
ARG DEPENDENCY=/target/dependency
COPY --from=builder ${DEPENDENCY}/modules/server/target/${JPOM_PKG} ${JPOM_HOME}

# 时区
ENV TZ Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 数据目录
ENV jpom.path ${JPOM_DATA_PATH}

VOLUME $JPOM_DATA_PATH $JPOM_LOG_PATH

HEALTHCHECK CMD curl -X POST -f http://localhost:2122/check-system || exit 1
EXPOSE 2122


ENTRYPOINT ["/bin/bash", "Server.sh", "start"]


