FROM centos:7

ENV LANG		en_US.utf8

ENV JPOM_HOME	/usr/local/jpom-server
ENV JPOM_PKG	server-2.8.0-release.zip

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
