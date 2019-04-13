FROM maven:3.6-jdk-8-alpine
LABEL maintainer="keepbx <service@keepbx.cn>"

WORKDIR /opt/Jpom

ADD . /tmp

# 验证码图片渲染需要ttf的支持
# RUN apk add --update ttf-dejavu

RUN ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo ${TZ} > /etc/timezone

RUN cd /tmp && \
    cp -f /tmp/docker/build/settings.xml /usr/share/maven/conf/settings.xml && \
    mvn package && \
    mv target/jpom-2.3.2-release/* /opt/Jpom/ && \
    # cp -f /tmp/docker/build/Jpom.sh /opt/Jpom/Jpom.sh && \
    chmod +x /opt/Jpom/Jpom.sh &&  \
    rm -rf /tmp

EXPOSE 2122

CMD ["/opt/Jpom/Jpom.sh", "start"]

