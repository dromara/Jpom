#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

FROM maven:3.9.6-eclipse-temurin-8 as builder
WORKDIR /target/dependency
COPY . .

VOLUME ["/root/.m2"]
# 多次 builder 不同的版本号
ARG TEMP_VERSION=""
ARG JPOM_VERSION
ENV USE_JPOM_VERSION=${JPOM_VERSION}${TEMP_VERSION}
RUN --mount=type=cache,target=/root/.m2 sh ./script/replaceVersion.sh ${USE_JPOM_VERSION} "release"

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

