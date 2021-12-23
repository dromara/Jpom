## maven 快速创建项目

1. 在pom 中添加插件如下，具体配置信息请自行修改为对应项目

```
 <plugin>
    <groupId>io.jpom.jpom-plugin</groupId>
    <artifactId>jpom-maven-plugin</artifactId>
    <version>LATEST</version>
    <configuration>
        <!--节点地址 -->
        <url>http://127.0.0.1:2122</url><!--jpom服务器的地址 -->
        <nodeIds>
            <nodeId>localhost</nodeId> <!--节点的id -->
        </nodeIds>
        <!--用户token -->
        <token>5610b7db99f7216e4ed3543f2a56eb95</token><!--用于通过jpom登陆验证 -->
        <project><!--节点中的 管理项目配置信息 -->
            <name>测试</name> <!--项目名称 -->
            <id>ss</id>     <!--项目ID -->
            <runMode>File</runMode> <!--运行方式 -->
            <path>dfgdsfg</path><!--项目文件夹 -->
            <whitelistDirectory>/test/</whitelistDirectory><!--项目路径 -->
            <!--非必填-->
            <group></group> <!--分组的名字 -->
            <mainClass></mainClass> <!--MainClass  当运行方式是ClassPath时填写-->
            <jvm></jvm> <!--运行方式 -->
            <args></args><!--args参数 -->
            <webHook></webHook><!--WebHooks -->
        </project>
            <nodeProjects> <!--参数优先级高于project -->
                                <nodeProject>
                                    <nodeId>test</nodeId>
                                    <args>--spring.profiles.active=dev</args> <!--如果这里指定,那么上面的就不生效了 -->
                                </nodeProject>
                            </nodeProjects>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>jpom-project</goal> <!--执行的moto值 -->
            </goals>
        </execution>
    </executions>
</plugin>
```

2. 执行命令

```
mvn clean package
```
