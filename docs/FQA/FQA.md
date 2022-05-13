# 常见问题

# 忘记系统管理员密码

##### 2.8.0 版本及其以上

> linux 执行：sh /xxxx/Server.sh restart --rest:super_user_pwd
> 
> windows 执行 Server.bat 操作面板会提示如果执行

注意⚠️：低版本的管理脚本可能未接收命令行传参，或者没有这个选项。还需要自行同步一下管理脚本里面的参数。（linux 版本中接收命令行参数传入程序变量为 `$@`，windows 版本中参考官方最新版本管理脚本内容 ）


#### 2.8.0 版本以下

> 1. 删除服务端的数据目录中【data/user.json】所有用户信息将失效，需要重新初始化系统管理员账号信息（此操作不会其他其他信息以及项目运行状态）

> 2. 重新确定密码，并js sha1后修改【data/user.json】中对应的系统管理员中的密码字段即可

# IP 白名单配置错误需要手动恢复

> linux 执行：sh /xxxx/Server.sh restart --rest:ip_config
>
> windows 执行 Server.bat 操作面板会提示如果执行

注意⚠️：低版本的管理脚本可能未接收命令行传参，或者没有这个选项。还需要自行同步一下管理脚本里面的参数。（linux 版本中接收命令行参数传入程序变量为 `$@`，windows 版本中参考官方最新版本管理脚本内容 ）


# 账号异常锁定

> 为了保证账号安全，系统默认开启了账号多次登录失败后将锁定一定时间的功能。

账号被有意或者无意锁定后解决方案：

1. 联系管理员，手动解锁账号。
2. 超级管理则走忘记系统管理员密码流程(会自动解锁，限定 2.8.0 版本及其以上)

# 在线构建异常

> 在使用在线构建中出现 xxx:未找到命令 等相关提示。

> 构建依赖的是系统环境，如果需要 maven 或者 node 需要服务端所在的服务器中有对应插件，如果已经启动服务端再安装的对应环境需要通过命令行重启服务端后才生效。

# 验证码无法正常加载（登录页）

> 图形验证码使用到字体相关的资源，如果没有安装字体，需要手动安装字体，并且需要重启服务端。
> 
> 如果服务器无法安装字体，则可以禁用图形验证码，并且需要重启服务端。

配置方式：在服务端中的配置文件（extConfig.yml）修改 `jpom.disabledCaptcha` 为 `true`

# 在linux 系统中执行管理 sh 提示

![jpom](../images/error/ff-unix.png)

![jpom](../images/error/command-not-found.png)

执行如下命令：(https://blog.csdn.net/perter_liao/article/details/76757605)

```shell
# 1.编辑文件
vim filename（文件名）
  
# 2.进入末行模式（按esc键）

# 3.设置文件格式
:set fileformat=unix
 
# 4.保存退出
 :wq
 
# 5.#sh filename OK!
```
  
> 同时需要注意文件编码格式和内容换行符 

# windows 中执行管理bat命令乱码或者执行失败

> 请修改文件编码为当前系统默认的编码（windows中默认为GB2312）、检查文件内容换行符
> 
> 使用 GB2312 编码后请检查脚本里面是否包含 CHCHP 相关命令,如果存在需要将其一并删除

# Jpom添加项目、启动、查看项目报错

1.运行的java进程有32位和64位的

![jpom](../images/error/32bit.jpg)

2.抛出异常Unable to open socket file: target process not responding or HotSpot VM not load。

![jpom](../images/error/can't-open-socket-file.jpg)

针对以上两个问题，Jpom目前采用略过这些进程的解决办法，请更新到2.3.1以上。 


# 查看控制台日志中文乱码

> 由于目前采用自动识别文件编码格式，可能不准确，如果明确日志文件编码格式。可以在外部文件【extConfig.yml】中指定

# 启动很慢

在 linux 中出现如下日志：`Please verify your network configuration.`
```log
WARN [main] o.s.b.StartupInfoLogger [StartupInfoLogger.java:117]- x:() InetAddress.getLocalHost().getHostName() took 10084 milliseconds to respond. Please verify your network configuration.
```

解决方法：
1. 查看主机名

```log
hostname
```

假设输出：`myhostname`

2. 在/etc/hosts上加上主机名

```log
127.0.0.1   localhost myhostname
::1         localhost myhostname
```

注意：myhostname 请修改为第一步执行结果

# 服务端添加插件端

目前支持的方式有如下：

1. 手动添加，节点列表添加按钮，填写节点信息
2. 插件端自动注册，配置服务端 token，在插件端中配置注册信息
3. SSH 安装插件端，先添加 ssh 到服务端中，然后根据页面按钮提示安装
4. 快速安装并绑定，节点列表中有快速安装操作引导

# 升级 Jpom 版本

> 注意：升级前请仔细阅读版本更新日志，如果有特殊说明或者注意事项请仔细确认。升级前建议提前做好相关备份,避免出现意外造成数据丢失

目前支持的升级方式有：

1. 手动替换 jar 
2. 在线上传 jar 包
3. 远程检查版本并更新
4. 批量升级插件端

升级可能出现启动失败的情况，失败请检查控制台日志

# Ubuntu/Debian 执行脚本错误

> Syntax error: "(" unexpected

代码对于标准bash而言没有错，因为`Ubuntu/Debian`为了加快开机速度，用`dash`代替了传统的`bash`，是`dash`在捣鬼。

解决方法:
1. 就是取消`dash`
   1. `sudo dpkg-reconfigure dash` 在选择项中选No，搞定了！
2. 通过 `bash ./Agent.sh`、`bash ./Server.sh`执行


# 数据库异常

### 字段没有找到

```log
Caused by: org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException: NULL not allowed for column "xxxx"; SQL statement:
```

出现原因可能是版本会退，还原低版本数据。造成新版程序需要的字段在数据库中不存在。

解决办法：

1. 在执行启动命令后填参数 `--rest:load_init_db` 

```shell
sh /xxxx/Server.sh restart --rest:load_init_db
```

2. 手动删除数据目录里面的数据库初始化记录文件
路径为：`${Jpom数据目录}/db/execute.init.sql.log` 文件后重新启动

### 无法正常启动，数据无法链接

出现原因大部分为非正确停止程序造成 h2 数据库文件损坏

特别提醒：建议在重启服务器前先关闭服务端，尽量不使用 `kill -9 xxx` 来关闭服务端

解决办法：

1. 【推荐】从备份恢复（系统默认每天备份一次）
   1. 找到数据库备份文件 路径为：`${Jpom数据目录}/db/backup/xxx.sql` 文件名为时间建议使用最后一次备份文件
   2. 将 `${Jpom数据目录}/db/` 整个目录备份后删除
   3. 重启服务端
   4. 到系统管理->数据库备份 中去上传 sql 文件恢复
2. 尝试自动恢复（能恢复到最后状态的数据）
   1. 在执行启动命令后填参数 `--recover:h2db`
   2. `sh /xxxx/Server.sh restart --recover:h2db`
   3. 此方法不一定成功，或者可能出现恢复后的数据不完整（恢复后需要检查数据是否完整）



# WebSocket （web socket 错误,请检查是否开启 ws 代理）

Jpom 中服务端和插件端通信方式有 http、WebSocket,http 主要传输一下基础操作、上传文件相关，WebSocket 主要用于一些需要实时传输的信息如：控制台日志、脚本执行日志等。

服务端中对接口也是采用 http+WebSocket 来实现，如果用户在使用中出现 WebSocket 连接错误或者控制台看不到任何信息时候：需要检查一下是否使用代理软件代理 Jpom 服务端或者插件端（如：nginx 代理）

如果使用了代理需要确认是否配置 WebSocket 相关配置

```log
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

#  Jpom使用Nginx代理推荐配置

### Http 相关配置

```log
server {
    #charset koi8-r;
    access_log  /var/log/nginx/jpom.log main;
    listen       80;
    server_name  jpom.xxxxxx.cn;
    
    location / {
        proxy_pass   http://127.0.0.1:2122/;
        proxy_set_header Host      $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        # iframe 重定向
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;
        # 上传文件大小
        client_max_body_size  50000m;
        client_body_buffer_size 128k;
        #  websocket 配置
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        #  代理的二级路径配置 值填写nginx 中location的值  如 location /test-path/ {}
        #  proxy_set_header Jpom-ProxyPath      /test-path/;
    }
}
```

### Https 推荐配置

```log
server {
    listen 443;
    server_name jpom.xxxxxx.cn;
    access_log  /var/log/nginx/jpom.log main;
    ssl on;
    ssl_certificate   /etc/nginx/ssl/jpom-xxxxxx.pem;
    ssl_certificate_key  /etc/nginx/ssl/jpom-xxxxxx.key;
    ssl_session_timeout 5m;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE:ECDH:AES:HIGH:!NULL:!aNULL:!MD5:!ADH:!RC4;
    ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
    ssl_prefer_server_ciphers on;
    
    location / {
        proxy_pass   http://127.0.0.1:2122/;
        # 代理的二级路径配置 值填写nginx 中location的值  如 location /test-path/ {}
        # proxy_set_header Jpom-ProxyPath      /test-path/;
        proxy_set_header Host      $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        # iframe 重定向
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;
        client_max_body_size  50000m;
        client_body_buffer_size 128k;
        #	websocket 配置
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}

server {
    #charset koi8-r;
    listen       80;
    server_name  jpom.xxxxxx.cn;
    rewrite ^(.*)$  https://$host$1 permanent;
}
```


# 配置 maven 私服

在 Java 中很多时候都需要配置 maven 私服来构建项目

### 本地安装服务端 + 本地构建

> 将 Jpom 服务端直接安装到服务器中并且使用`本地构建`场景说明

1. 推荐使用全局 maven 配置来实现，安装 maven 插件后默认都会使用 `{user.home}/.m2/settings.xml` 一般使用 `root` 用户文件示例：`/root/.m2/settings.xml`
2. 使用构建命令来指定 `settings.xml` 文件位置，如：mvn -s xxx/settings.xml clean package

### 容器安装服务端 + 本地构建

> 将 Jpom 服务端直接安装到 docker 中并且使用`本地构建`场景说明

1. 使用构建命令来指定 `settings.xml` 文件位置，如：mvn -s xxx/settings.xml clean package

### 本地安装服务端 + 容器构建

> 将 Jpom 服务端直接安装到服务器中并且使用`容器构建`场景说明

1. 推荐使用构建命令来指定 `settings.xml` 文件位置，如：mvn -s xxx/settings.xml clean package
2. 使用 `binds` 来配置指定 maven `.m2 目录`或者`settings.xml`【注意容器构建必须使用宿主机对应的 docker 容器构建否则 binds 将不生效】

```yaml
# 指定 .m2 目录
binds:
  - /Users/user/.m2/:/root/.m2/
```

```yaml
# 指定 settings.xml 文件
binds:
  - /Users/user/.m2/settings.xml:/root/.m2/settings.xml
```

### 容器安装服务端 + 容器构建

> 将 Jpom 服务端直接安装到 docker 中并且使用`容器构建`场景说明

1. 使用构建命令来指定 `settings.xml` 文件位置，如：mvn -s xxx/settings.xml clean package

# 获取项目状态超时

如果在节点（插件端）控制台或者项目列表中出现一直没有显示项目状态，或者出现提示超时信息。
这种一般是获取项目状态超时，因为 jpom 针对 java 程序获取状态默认使用 jps 命令来处理，据了解可能在不同的 jdk 中 jps 命令存在一定差异，可能出现 jps 命令响应很慢（openJ9 ）

解决办法有如下：

1. 配置节点超时时间 + 配置前端请求超时时间（前端请求超时时间在服务端 extConfig 中配置）
2. 跟换 jdk（建议使用 openjdk） 或者排查 是否出现异常进程
3. 升级 jpom 版本，2.8.18+ 优化过批量获取项目状态相关逻辑，会缩短整个接口耗时

# 在线升级（远程更新）

> jpom 中提供在线升级功能，方便用户及时知道有新版本并快速更新。

目前更新策略是每天定时更新和手动触发更新

使用该功能可能出现对问题有如下：

1. 无法获取到最新版本
2. 控制台有错误信息（`获取远程版本信息失败: xxxx`）

出现上述大部分为网络原因，因为默认使用 `github pages`（海外）来获取最新信息，如果服务器无法访问外网或者不能使用公网那么基本是不能正常使用。

目前系统中提供到解决方案有如下：

1. 不能访问外网则可以自定义配置远程地址：(配置方法在 extConfig.yml 文件中修改 `system.remoteVersionUrl` 属性)
   1. [https://jpom.io/docs/release-versions.json](https://jpom.io/docs/release-versions.json) （GitHub Pages）
   2. [https://keepbx.gitee.io/jpom-site/docs/release-versions.json](https://keepbx.gitee.io/jpom-site/docs/release-versions.json) (Gitee Pages)
   3. [https://cdn.jsdelivr.net/gh/dromara/Jpom@docs/docs/release-versions.json](https://cdn.jsdelivr.net/gh/dromara/Jpom@docs/docs/release-versions.json) (jsdelivr)
2. 自己搭建服务（Jpom 中更新规则使用二级从定向）
   1. 生成第一级远程更新信息地址
   2. 第一级地址内容如示例：`{url:"https://dromara.gitee.io/jpom/docs/versions.json"}`
   3. 生成第二级远程安装包地址
   4. 第二级地址内容示例：`{tag_name:"v2.8.18",agentUrl:"https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/agent-2.8.18-release.zip",serverUrl:"https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/server-2.8.18-release.zip",changelogUrl:"https://gitee.com/dromara/Jpom/raw/master/CHANGELOG.md"}`
   5. 配置远程更新地址 (配置方法在 extConfig.yml 文件中修改 `system.remoteVersionUrl` 属性)


# 如何修改Jpom自身控制台日志级别

修改管理程序命令文件中`--spring.profiles.active=`属性值`pro/dev`

# 如何修改程序日志路径

修改管理程序命令文件中 --jpom.log=/jpom/log/

# 如何指定Jpom运行 jvm 内存

修改管理程序命令文件中 JVM 变量添加 -Xms512m -Xmx1024m

# 配置上传文件大小限制

Jpom 上传文件大小限制默认为 1GB，如果有上传更大到文件需要修改限制大小

Jpom 使用 SpringBoot 实现，大部分配置遵循 SpringBoot 配置属性。

上传文件大小限制配置属性为：

```yaml
spring:
  servlet:
    multipart:
      # 上传文件大小限制
      max-request-size: 2GB
      max-file-size: 1GB
```

如果上传大文件出现 OOM 那么则需要为程序配置更大的内存，因为上传文件默认都会先缓存到内存中

配置方式为：修改配置文件(`extConfig.yml`)中对应属性值

# 关于上传文件进度条说明

目前 Jpom 中到上传文件进度条仅是一个心里安慰剂（有相关经验到开发同学都懂）

目前暂时没有考虑优化为真实进度条的计划（主要是因为开发起来有一定工作量，带来的体验并不能提升多少）

关于在浏览器中上传项目文件缓慢原因说明， Jpom 目前都使用 http 协议和插件端通讯，那么在浏览器中上传，首先会将文件上传到服务端，再由服务端上传到插件端（节点）,用户感知到的上传耗时就会多用一部分时间。服务端上传到插件端中理论上如果使用内网通讯那么此耗时基本可以忽略。

# 上传或者构建发布出现：`Error writing to server` 异常信息

```log
io.jpom.system.AgentException: xxx节点异常：Error writing to server
	at io.jpom.common.forward.NodeForward.responseException(NodeForward.java:235)
	at io.jpom.common.forward.NodeForward.request(NodeForward.java:208)
	at io.jpom.common.forward.NodeForward.request(NodeForward.java:90)
	at io.jpom.outgiving.OutGivingRun.fileUpload(OutGivingRun.java:145)
	at io.jpom.build.ReleaseManage.doProject(ReleaseManage.java:505)
	at io.jpom.build.ReleaseManage.start(ReleaseManage.java:165)
	at io.jpom.build.ReleaseManage.run(ReleaseManage.java:546)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
```

出现上述信息可能是因为上传文件超过插件端上传文件大小限制，需要配置更大的上传文件限制

# 如何在 Jpom 中备份项目文件

在 Jpom 中备份项目相关文件有如下方式：

1. 在线构建产生到构建历史
2. 为节点项目开启备份文件功能

## 在线构建备份文件说明

在线构建会为每次构建成功到产物目录生成一个构建历史并备份相关文件

可以针对相关产物文件进行：

- 下载：下载到本地
- 回滚：重新执行一遍发布逻辑

## 开启备份文件功能相关说明如下
 
- 默认未开启文件备份功能
- 如果配置值小于等于 0 则不开启备份功能
- 备份文件保留规则为，只保留有差异的文件

### 全局开启

插件端配置（ `extConfig.yml` ）文件中配置`project.fileBackupCount`属性

### 单个项目开启

目前仅支持对 DSL 的单个项目开启文件备份，配置到 DSL 内容中

DSL 项目可以在配置内容新增 `file.backupCount` 来开启（DSL 配置优先级最高）

## 如果限制仅备份指定后缀文件

### 全局限制

插件端配置（ `extConfig.yml` ）文件中配置`project.fileBackupSuffix`属性

配置示例：
```yaml
project:
  # 项目文件备份保留个数,大于 0 才会备份
  fileBackupCount: 5
  # 限制备份指定文件后缀（支持正则）
  fileBackupSuffix: [ '.jar','.html','^.+\\.(?i)(txt)$' ]
```

### 单个项目限制

 目前仅支持对 DSL 的单个项目配置限制，配置到 DSL 内容中

配置示例：
```yaml
project:
  # 项目文件备份保留个数,大于 0 才会备份
  backupCount: 5
  # 限制备份指定文件后缀（支持正则）
  backupSuffix: [ '.jar','.html','^.+\\.(?i)(txt)$' ]
```


# 如何修改Jpom数据存储目录

> 修改外部配置文件`extConfig.yml`中的 jpom.path 属性

# 如何修改Jpom插件端账户/密码

>修改插件端外部配置文件`extConfig.yml`中的 jpom.authorize.agentName 和 jpom.authorize.agentPwd 属性


# 开发计划

[开发计划](https://cdn.jsdelivr.net/gh/dromara/Jpom/PLANS.md)

### 常见问题未知问题

https://github.com/alibaba/arthas/issues/347

https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4770092

### windows 环境项目在运行中不能删除文件

> 由于系统原因，暂时还没有找到解决办法
