#
# Copyright (c) 2019 Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

FROM ubuntu:latest
ENV DEBIAN_FRONTEND=noninteractive
ENV LANG C.UTF-8
RUN sed -i.bak 's/archive.ubuntu.com/mirror.nju.edu.cn/' /etc/apt/sources.list \
    && sed -i 's/security.ubuntu.com/mirror.nju.edu.cn/' /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y curl wget

RUN apt-get install -y git

ENTRYPOINT ["/bin/bash", "/tmp/build.sh"]
