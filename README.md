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
3. 修改运行参数配置信息 [run.sh](/bin/run.sh)
4. 执行如下命令（记得给命令文件添加权限奥[*chmod  777 /jpom/run.sh*]）
```
/jpom/run.sh start
```
5. 修改默认账号信息 在项目运行路径中找到【data/user.json】文件修改[data/user.json](/src/main/resources/bin/data/user.json)
6. 配置端口访问域名或者指定端口访问（如果是nginx推荐配置：[/doc/jpom-nginx.conf](/doc/jpom-nginx.conf)）

### 编译安装

访问[Jpom](https://gitee.com/jiangzeyin/jpom)的码云主页,拉取最新完整代码：

1. 端口修改确认（默认：2122）
2. 项目运行目录修改确认,如需修改请查看[/src/main/resources/application.yml](/src/main/resources/application.yml)（默认：/jpom/）数据路径存储用户信息、项目配置信息、运行命令，基础格查看项目目录[/src/main/resources/bin](/src/main/resources/bin) 
4. 项目运行日志目录确认,如需修改请查看[/src/main/resources/logback-spring.xml](/src/main/resources/logback-spring.xml)（默认：/jpom/log/）
5. 进入项目目录执行，然后在目录中查看 target/jpom-1.0-lib
```
mvn package
```
6. 打包上传服务器运行 （推荐命令执行文件[/bin/run.sh](/bin/run.sh)） 

### 编译运行

1. 将【编译安装】中的1-4部都确认执行
2. copy项目运行数据文件到对应目录
3. 运行*cn.jiangzeyin.JpomApplication*
4. 浏览器访问

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
