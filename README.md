# Jpom(Java Project Online Management)Java项目在线管理

> 在linux 中管理jar包运行，如SpringBoot、Jboot、jfinal、t-io项目如果是打包为Jar那么我们一般是使用shell 命令来管理, 
> 如果有多个项目管理起来显得比较麻烦。
> 如果新增、更新、删除项目都不是很方便。
> 再或者想不使用电脑控制重启、关闭、开启项目。直接使用shell 登录服务器管理显然不是最简单的方法
> 如果休息日在朋友家玩，公司领导联系说需要控制一下某个项目，此时你身边有电脑，但是没有专业的管理服务器的软件。


###  在这里[Jpom](https://gitee.com/jiangzeyin/jpom) 项目将如上问题统一解决

![GitHub](https://img.shields.io/github/license/jiangzeyin/jpom.svg?style=flat)
![jdk](https://img.shields.io/badge/JDK-1.8+-green.svg)
[![travis](https://travis-ci.org/jiangzeyin/jpom.svg?branch=master)](https://travis-ci.org/jiangzeyin/jpom)
[![codecov](https://codecov.io/gh/jiangzeyin/jpom/branch/master/graph/badge.svg)](https://codecov.io/gh/jiangzeyin/jpom)


> Jpom采用SpringBoot开发web项目(前台采用layui)+websocket来统一管理，只要电脑能使用浏览器就ok

#### 项目主要功能

1. 创建、修改、删除项目
2. 实时查看控制台
3. jar包文件夹管理
4. cpu、ram 监控
5. 导出堆栈信息
6. 阿里云 CodePipeline + Oss在线管理
7. 多用户管理
8. 此项目完全不依赖数据库，随装随用
9. 整个服务器top监听
10. 查看项目运行进程占用的端口


#### 项目截图

1. 创建项目
![创建项目](/doc/images/create.png)

2. 修改、删除项目
![修改、删除项目](/doc/images/edit_del.png)

3. 文件管理
![文件管理](/doc/images/file.png)

4. 项目列表
![项目列表](/doc/images/list.png)

5. 监控cpu 内存
![ 监控cpu 内存](/doc/images/cup_ram.png)

6. 控制台日志实时查看
![控制台日志实时查看](/doc/images/console.png)

7. 用户管理
![用户管理](/doc/images/user_list.png)

8. top监听
![top监听](/doc/images/top.png)

### 下载安装

1. 下载安装包 [https://gitee.com/jiangzeyin/jpom/attach_files](https://gitee.com/jiangzeyin/jpom/attach_files)
2. 上传到服务解压到根目录
3. 执行目录下[run.sh](/bin/run.sh)文件（记得给命令文件添加权限奥[*chmod  777 /jpom/run.sh*]）
如
```
/jpom/run.sh start
```
4. 如有需要修改项目端口请查看 run.sh
### 编译安装

访问[Jpom](https://gitee.com/jiangzeyin/jpom)的码云主页,拉取最新完整代码：
 
1. 项目运行日志目录确认,如需修改请查看[/src/main/resources/logback-spring.xml](/src/main/resources/logback-spring.xml)（默认：/jpom/log/）
2. 进入项目目录执行，然后在目录中查看 target/jpom-x.x-release
```
mvn package
```
3. 打包上传服务器运行
4. 命令运行

### 编译运行

访问[Jpom](https://gitee.com/jiangzeyin/jpom)的码云主页,拉取最新完整代码：

1. 运行*cn.keepbx.jpom.JpomApplication*
2. 浏览器访问

### 阿里云CodePipeline 配置

[查看文档](/doc/CodePipeline-Oss.md)

### 贡献人员
    
1.  [F7575](https://gitee.com/F7575)
2.  [bwcx_jzy](https://gitee.com/jiangzeyin)
3.  [arno](https://gitee.com/arnohand)

### 感谢
 Jpom使用以下开源项目
  - [Spring Boot](https://github.com/spring-projects/spring-boot)：核心框架
  - [Fast-Boot](https://gitee.com/jiangzeyin/common-parent)：针对SpringBoot 封装的一系列的快捷包 提供公共的Controller、自动化拦截器、启动加载资源接口、线程池管理
  - [Fastjson](https://github.com/alibaba/fastjson)：用于Java的快速JSON解析器/生成器
  - [Hutool](https://gitee.com/loolly/hutool)：一个Java工具包，也只是一个工具包，它帮助我们简化每一行代码，减少每一个方法，让Java语言也可以“甜甜的”
  - [Layui](https://gitee.com/sentsin/layui)：前端UI框架
  

### 提供bug反馈或建议

- [码云](https://gitee.com/jiangzeyin/jpom/issues)

### 贡献代码
  欢迎任何人为Jpom添砖加瓦，贡献代码
  
### 交流讨论

  QQ群：[136715345](https://shang.qq.com/wpa/qunwpa?idkey=93ff8d8a37a436b752fe38d32075bb1b32a8e0b3d3ff19d0b541ca840433f561)
  
  
### 常见问题

   1. 如何修改程序运行端口：修改管理程序命令文件中 --server.port=2122
   2. 如何修改程序日志路径：建议自行编译安装修改 [/src/main/resources/logback-spring.xml](/src/main/resources/logback-spring.xml) 文件
   3. 如何修改回话超时时长：在管理程序命令文件中 ARGS 变量添加 --tomcat.sessionTimeOut=1800
