---
title: 带您了解能替代 Jenkins 的 Jpom 中的资产管理和 2.10.x 新特性
date: 2023-03-20 13:31:38
permalink: /pages/7f1127/
categories:
  - docs
  - 推广文章
tags:
  - 
---

## Jpom 介绍

Jpom 是一款简而轻的低侵入式在线构建、自动部署、日常运维、项目运维监控软件

Jpom 从 `18 年`初正式开源至今已经迭代了 `120+ 个`小版本，`2 个`大版本，`9 个`次要版本。

已经在中小团队中得到了可行性的验证，目前有长期许多并稳定使用 Jpom 来运维项目的公司以及团队了，充分证明 Jpom 是能给用户带来便利和可靠性。

业界内已经有使用 **Jpom** 来代替 ~~Jenkins~~ 的传言了

![img.png](/images/tutorial/2.10.x-article/img.png)

![img_1.png](/images/tutorial/2.10.x-article/img_1.png)

![img_2.png](/images/tutorial/2.10.x-article/img_2.png)

![img_3.png](/images/tutorial/2.10.x-article/img_3.png)

## 前言

Jpom 2.10.x 次要版本主要是解决一下遗留问题、优化部分不易用的功能。

2.10.x 从 2022年12月19号发布第一个版本（2.10.1） 到现在已经发布 2.10.35 累积发布了 35 个小版本

我们一起回顾一下在这 35 个小版本中主要更新了啥？

1. 优化了项目日志文件配置（支持自定义配置日志基本）
2. 项目运行的目录结构更标准化、配置属性标准化
3. `2.10.9` 数据库开始支持了 **mysql** （并且能将 h2 数据库内容迁移到 mysql）
4. `2.10.10` 上传文件支持**分片上传**，再也不用担心大文件上传耗内存啦
5. 构建产物模糊匹配规则优化支持更多方式的规则
6. 新增资产管理：机器管理、SSH 管理、docker 管理
7. 支持更详细的机器、SSH 基础信息监控
8. 令牌导入仓库支持了：gitea、gogs
9. 新增文件管理中心来统一存储、发布文件
10. DSL 项目支持解析多 PID、多端口
11. 整合了菜单名（合并优化菜单、节点管理更名为逻辑节点）
12. 系统默认模板、docker 模板支持自定义配置
13. 用户自定义工作空间排序、备注别名
14. 更多优化、小变动更新期待您来发现

## 预览图

> 我们先来几张新版本的整体菜单截图

工作空间中的功能预览图

![img_4.png](/images/tutorial/2.10.x-article/img_4.png)


系统管理功能预览图

![img_5.png](/images/tutorial/2.10.x-article/img_5.png)

节点管理功能预览图

![img_10.png](/images/tutorial/2.10.x-article/img_10.png)

### 总结

通过截图可以看出：有取消的、新增、更名等变动

**取消**

1. 节点管理：和`节点&项目`管理合并
2. 分发管理：和`节点&项目`管理合并

**新增**

1. 文件管理
2. 其他管理
3. 资产管理

**更名**

1. 系统管理（服务端）变更为：服务端配置
2. 系统管理（插件端）变更为：插件端配置
3. 节点管理变更为：逻辑节点

## 资产管理

新版本中的资产管理主要是解决了同一个资源（物理节点、SSH、docker）跨工作空间不方便管理。

**旧版本：** 需要在工作空间下创建资源（物理节点、SSH、docker），但是创建的资源只能在当前工作空间中使用，如果想在其他工作空间中使用则需要在另一个工作空间中重复创建资源的步骤
如此不方便资源较多，工作空间划分较细的情况下来灵活管理资产资源


**新版本：** 同一个资源只需要在资产管理中创建一次即可完成快捷分配到多个工作空间，以及查看资产以及被分配到哪里工作空间中。新版本也对资产新增了基础状态监控

### 机器资产详情

新版本中我们对机器资产做了更准确的服务状态监控

服务器的 CPU、内存、硬盘、网络流量、文件系统、硬盘、网卡都支持实时查看

![img_6.png](/images/tutorial/2.10.x-article/img_6.png)

![img_7.png](/images/tutorial/2.10.x-article/img_7.png)


### SSH 资产列表

SSH 这里在新版本中我们支持对 SSH 的连接状态和 SSH 服务端的基础信息监控

![img_8.png](/images/tutorial/2.10.x-article/img_8.png)

![img_11.png](/images/tutorial/2.10.x-article/img_11.png)

### Docker 资产列表

![img_9.png](/images/tutorial/2.10.x-article/img_9.png)


## 数据库使用 mysql

在之前版本中 Jpom 一致是使用 h2 数据库来存储数据，部分用户提出期望使用 mysql 数据库来存储这样可以更好的保证数据稳定。

在新版本中我们支持了使用 mysql 数据库存储

配置方式如下：

修改服务端配置文件`conf/application.yml`：

```yaml
jpom:
  db:
    # 修改 jpom.db.mode 为 MYSQL，如果配置则新增配置即可
    mode: MYSQL
    # 修改 mysql 的 jdbc 地址( jdbc:mysql://127.0.0.1:3306/jpom?useUnicode=true&characterEncoding=UTF-8&useSSL=false)
    url: jdbc:mysql://127.0.0.1:3306/jpom?useUnicode=true&characterEncoding=UTF-8&useSSL=false
    # 修改 jpom.db.user-name 为对应 mysql 账户
    user-name: jpom
    # 修改 jpom.db.user-pwd 为对应 mysql 密码
    user-pwd: dbpwd
```

如果您需要迁移之前 h2 数据库中的数据到 mysql（需要先将 mysql 的连接信息配置好后才能迁移）

```shell
bash ./bin/Server.sh restart -15 --h2-migrate-mysql --h2-user=jpom --h2-pass=jpom
```

[完整的操作说明 https://jpom.top/pages/4cfb46/](https://jpom.top/pages/4cfb46/)

## 赞赏支持 ❤️

随着用户增加，需要协助解决的问题也多种多样、用户环境、业务场景也是多变的，为了能给用户提供 **优质**、**准确** 的 **疑难问题** 解答我们推出赞赏咨询服务

您可以到 [https://jpom.top/pages/praise/](https://jpom.top/pages/praise/) 选择合适你的赞赏服务方案

## 未来的路 🚀

我们会长期坚持去完善这个项目，希望可以让 Jpom 帮助到更多的中小团队的开发者、运维、公司

`TODO` 我们即将支持的功能列表：

- 插件端通讯支持推送方式
- 用户体系支持接入第三方系统
- 构建支持流水线模式
- 其他功能细节优化
- 更多需求您可以来提

上述功能敬请期待奥

如果您还未加入我们社群您可以点击下面连接查看加入社群方式：[https://jpom.top/pages/praise/join/](https://jpom.top/pages/praise/join/)

## Jpom 链接

官网：[https://jpom.top/](https://jpom.top/)

Gitee: [https://gitee.com/dromara/Jpom](https://gitee.com/dromara/Jpom)

Github: [https://github.com/dromara/Jpom](https://github.com/dromara/Jpom)

常见问题：[https://jpom.top/pages/FQA/](https://jpom.top/pages/FQA/)