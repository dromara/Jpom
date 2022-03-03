# 常见问题

# 忘记系统管理员密码

##### 2.8.0 版本及其以上

> linux 执行：sh /xxxx/Server.sh restart --rest:super_user_pwd
> 
> windows 执行 Server.bat 操作面板会提示如果执行

#### 2.8.0 版本以下

> 1. 删除服务端的数据目录中【data/user.json】所有用户信息将失效，需要重新初始化系统管理员账号信息（此操作不会其他其他信息以及项目运行状态）

> 2. 重新确定密码，并js sha1后修改【data/user.json】中对应的系统管理员中的密码字段即可

### IP 白名单配置错误需要手动恢复


> linux 执行：sh /xxxx/Server.sh restart --rest:ip_config
>
> windows 执行 Server.bat 操作面板会提示如果执行

# 在线构建异常

> 在使用在线构建中出现 xxx:未找到命令 等相关提示。

> 构建依赖的是系统环境，如果需要 maven 或者 node 需要服务端所在的服务器中有对应插件，如果已经启动服务端再安装的对应环境需要通过命令行重启服务端后才生效。

    
# 在linux 系统中执行管理 sh 提示

![jpom](../images/error/ff-unix.png)

![jpom](../images/error/command-not-found.png)

执行如下命令：(https://blog.csdn.net/perter_liao/article/details/76757605)

```
1.编辑文件
vim filename（文件名）
  
2.进入末行模式（按esc键）

3.设置文件格式
:set fileformat=unix
 
4.保存退出
 :wq
 
5.#sh filename
  OK!
```
  
> 同时需要注意文件编码格式和内容换行符 

# windows 中执行管理bat命令乱码或者执行失败

> 请修改文件编码为当前系统默认的编码（windows中默认为GB2312）、检查文件内容换行符

#  Jpom使用Nginx代理推荐配置

[查看>>](../辅助配置/nginx-config.md)

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
```
WARN [main] o.s.b.StartupInfoLogger [StartupInfoLogger.java:117]- x:() InetAddress.getLocalHost().getHostName() took 10084 milliseconds to respond. Please verify your network configuration.
```

解决方法：
1. 查看主机名

```
hostname
```

假设输出：`myhostname`

2. 在/etc/hosts上加上主机名

```
127.0.0.1   localhost myhostname
::1         localhost myhostname
```

注意：myhostname 请修改为第一步执行结果

# 服务端添加插件端

1. 手动添加
2. 插件端自动注册
3. SSH 安装插件端

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

# 开发计划

[开发计划](https://cdn.jsdelivr.net/gh/dromara/Jpom/PLANS.md)

# 数据库异常

### 字段没有找到

```
Caused by: org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException: NULL not allowed for column "xxxx"; SQL statement:
```

出现原因可能是版本会退，还原低版本数据。造成新版程序需要的字段在数据库中不存在。

解决办法：

1. 在执行启动命令后填参数 `--rest:load_init_db` 

```
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

### 常见问题未知问题

https://github.com/alibaba/arthas/issues/347

https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4770092

### windows 环境项目在运行中不能删除文件

> 由于系统原因，暂时还没有找到解决办法
