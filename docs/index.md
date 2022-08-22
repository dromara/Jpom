---
home: true
heroImage: /images/jpom_logo.png
heroText: 
tagline: 🚀简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件
actionText: 开始使用 →
actionLink: /pages/install/
bannerBg: none # auto => 网格纹背景(有bodyBgImg时无背景)，默认 | none => 无 | '大图地址' | background: 自定义背景样式       提示：如发现文本颜色不适应你的背景时可以到palette.styl修改$bannerTextColor变量

features: # 可选的
- title: 节点管理
  details: 集群节点，统一管理多节点的项目，实现快速一键分发项目文件
- title: 项目管理
  details: 创建、启动、停止、实时查看项目控制台日志，管理项目文件
- title: SSH 终端
  details: 在浏览器中执行 SSH 终端，方便进行日常运维,记录执行命令记录
  link: /pages/practice/ssh-terminal/
- title: 在线构建
  details: 在线拉取 GIT、SVN 仓库快速构建项目包，不用运维人员手动上传项目包
  link: /pages/practice/build-java-ssh-release/
- title: 在线脚本
  details: 在线管理脚本、定时执行脚本、webhook 钩子执行、执行日志等
  link: /pages/practice/node-script-backup-docker-mysql/
- title: DOCKER 管理
  details: 在线管理镜像、容器、SWARM 集群。界面化管理 DOCKER
  link: /pages/practice/docker-cli/
- title: 用户管理
  details: 多用户管理,实现不同用户不同权限，用户操作、管理日志完善记录
- title: 项目监控
  details: 实时监控项目当前状态、如果异常自动触发邮件、钉钉报警通知
  link: /pages/practice/monitor-notice/
- title: NGINX 配置、SSL 证书
  details: 在线快速方便的修改 NGINX 配置文件，SSL 证书统一管理
  link: /pages/practice/node-nginx/

# 文章列表显示方式: detailed 默认，显示详细版文章列表（包括作者、分类、标签、摘要、分页等）| simple => 显示简约版文章列表（仅标题和日期）| none 不显示文章列表
postList: none
---

<br/>

<p align="center">
  <a class="become-sponsor" href="https://demo.jpom.top?from=site-middle">演示站点</a>
  <a class="become-sponsor" href="/pages/praise/">支持这个项目</a>
</p>

<style>
.become-sponsor{
  padding: 8px 20px;
  display: inline-block;
  color: #4274F4;
  border-radius: 30px;
  box-sizing: border-box;
  border: 2px solid #4274F4;
}

.main-right{
  display: none;
}

.friends-item {
  width: 150px;
  height:40px;
  flex:1;
  text-align: center;
  display: inline-block;
  margin: 5px;
}

.friends-item-img {
  object-fit: contain;
  max-width:150px !important;
  height: 100%;
}
</style>

<br/>

## 😭 日常开发中，您是否有以下痛点？

- <font color="red">**团队中没有专业的运维，开发还要做运维的活**</font>，需要自己手动构建、部署项目。
- 不同的项目有不同的构建、部署命令。
- 有开发、测试、生产等多环境打包的需求。
- 需要同时监控多个项目的运行状态。
- <u>需要下载 SSH 工具</u>远程连接服务器。
- *需要下载 FTP 工具* 传输文件到服务器。
- 多台服务器时，在不同电脑之间账号密码同步不方便。
- 想使用一些自动化工具，但是对服务器性能太高，搭建太麻烦。
- 对自动化工具有个性化的需求，想自己修改项目，但是市面上的工具太复杂了。

> 如果是分布式的项目，以上步骤则更加繁琐。
>
> 让 [Jpom](https://gitee.com/dromara/Jpom) 来帮你解决这些痛点吧！然而，这些只是 [Jpom](https://gitee.com/dromara/Jpom) 解决的最基础的功能。

### 😁 使用 [Jpom](https://gitee.com/dromara/Jpom) 后

- **方便的用户管理**
  1. 用户操作监控，监控指定用户指定操作以邮件形式通知
  2. 多用户管理，用户项目权限独立（上传、删除权限可控制），完善的操作日志，使用工作空间隔离权限
  3. 账号可以开启 <mark>MFA 两步验证</mark>提高账号安全性
- **界面形式实时查看项目运行状态、控制台日志、管理项目文件**
  1. 在线修改项目文本文件
- **Docker 容器管理、Docker swarm 集群管理** <Badge text="Docker UI"/>
- **在线 SSH 终端，让您在没有 Xshell、FinalShell 等软件也能轻松管理服务器** <Badge text="SSH终端"/>
  1. 登录 Jpom 系统后不需要知道服务器密码
  2. 能指定 SSH 禁止执行的命令，避免执行高风险命令，并且能自动执行命令日志
  3. 设置文件目录，在线查看管理对应项目文件及配置文件
  4. SSH 命令模版在线执行脚本还能定时执行
  5. 在线修改文本文件
  6. <mark>轻量的实现了简单的"堡垒机"功能</mark>
- **使用项目分发一键搞定集群项目多机部署**
- **在线构建不用手动更新升级项目** <Badge text="在线构建"/>
  1. 支持拉取 GIT、SVN 仓库
  2. <mark>支持容器构建（docker）</mark>
  3. 支持 SSH 方式发布
  4. 支持定时构建
  5. 支持 WebHook 形式触发构建
- **支持在线编辑 nginx 配置文件并自动 reload 等操作** <Badge text="Nginx"/>
  1. 管理 nginx 状态，管理 SSL 证书
- **项目状态监控异常自动报警、自动尝试重启**
  1. 支持邮件 + 钉钉群 + 微信群通知，主动感知项目运行状况
- **节点脚本模版+定时执行或者触发器，拓展更多功能**
- **重要路径白名单模式，杜绝用户误操作系统文件**


<br/>

## 🏡代码托管

- [Gitee ![star](https://gitee.com/dromara/Jpom/badge/star.svg?theme=gvp)](https://gitee.com/dromara/Jpom/)
- [Github](https://github.com/dromara/Jpom)

<br/>

## 💪🏻参与贡献

欢迎各路好汉一起来参与完善 <Badge text="Jpom"/>，我们期待你的 PR！

如果想贡献，请先查看[贡献准则](/pages/ae4dd5/)。

<br/>

## 🍭架构图

<img :src="$withBase('/images/jpom-func-arch.jpg')" style="zoom: 120%" alt="jpom-func-arch">

<br/>

## 快速体验

### 一键安装插件端 [详细的安装说明文档](/pages/install/)

<code-group>
  <code-block title="centos" active>
```bash
# 如果服务器中没有 jdk 会自动安装 jdk-8u251
yum install -y wget && \
wget -O install.sh https://jpom.top/docs/install.sh && \
bash install.sh Agent jdk
```
  </code-block>

  <code-block title="ubuntu">
```bash
# 如果服务器中没有 jdk 会自动安装 jdk-8u251
apt install -y wget && \
wget -O install.sh https://jpom.top/docs/install.sh && \
bash install.sh Agent jdk
```
  </code-block>
</code-group>


### 一键安装服务端 [详细的安装说明文档](/pages/install/)

<code-group>
  <code-block title="centos" active>
```bash
# 如果服务器中没有 jdk 会自动安装 jdk-8u251
yum install -y wget && \
wget -O install.sh https://jpom.top/docs/install.sh && \
bash install.sh Server jdk
```
  </code-block>

  <code-block title="ubuntu">
```bash
# 如果服务器中没有 jdk 会自动安装 jdk-8u251
apt install -y wget && \
wget -O install.sh https://jpom.top/docs/install.sh && \
bash install.sh Server jdk
```
  </code-block>
</code-group>

## 🌍 知识星球
<p align="center">
<img class="no-zoom" :src="$withBase('/images/zsxq.jpg')" alt="扫码加入知识星球，了解学习更多知识">
</p>

## 🤝 dromara 组织项目

<p align="center">
<b><a href="https://dromara.org/zh/projects/" target="_blank">为往圣继绝学，一个人或许能走的更快，但一群人会走的更远。</a></b>
</p>

<p >
<a class="friends-item" href="https://hutool.cn/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/hutool2.png')" alt="🍬小而全的Java工具类库，使Java拥有函数式语言般的优雅，让Java语言也可以“甜甜的”。">
</a>
<a class="friends-item" href="https://sa-token.dev33.cn/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/sa-token.png')" alt="一个轻量级 java 权限认证框架，让鉴权变得简单、优雅！">
</a>
<a class="friends-item" href="https://liteflow.yomahub.com/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/liteflow2.png')" alt="轻量，快速，稳定，可编排的组件式流程引擎">
</a>
<a class="friends-item" href="https://jpom.top/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/jpom.png')" alt="一款简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件">
</a>
<a class="friends-item" href="https://gitee.com/dromara/TLog" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/tlog2.png')" alt="一个轻量级的分布式日志标记追踪神器，10分钟即可接入，自动对日志打标签完成微服务的链路追踪">
</a>
<a class="friends-item" href="https://easy-es.cn/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/easy-es2.png')" alt="🚀傻瓜级ElasticSearch搜索引擎ORM框架">
</a>
<a class="friends-item" href="https://gitee.com/dromara/hmily" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/hmily.png')" alt="高性能一站式分布式事务解决方案。">
</a>
<a class="friends-item" href="https://gitee.com/dromara/Raincat" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/raincat.png')" alt="强一致性分布式事务解决方案。">
</a>
<a class="friends-item" href="https://gitee.com/dromara/myth" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/myth.png')" alt="可靠消息分布式事务解决方案。">
</a>
<a class="friends-item" href="https://cubic.jiagoujishu.com/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/cubic.png')" alt="一站式问题定位平台，以agent的方式无侵入接入应用，完整集成arthas功能模块，致力于应用级监控，帮助开发人员快速定位问题">
</a>
<a class="friends-item" href="http://forest.dtflyx.com/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/forest-logo.png')" alt="Forest能够帮助您使用更简单的方式编写Java的HTTP客户端" nf>
</a>
<a class="friends-item" href="https://su.usthe.com/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/sureness.png')" alt="面向 REST API 的高性能认证鉴权框架">
</a>
<a class="friends-item" href="https://gitee.com/dromara/northstar" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/northstar_logo.png')" alt="Northstar盈富量化交易平台">
</a>
<a class="friends-item" href="https://www.jeesuite.com/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/mendmix.png')" alt="开源分布式云原生架构一站式解决方案">
</a>
<a class="friends-item" href="https://www.x-easypdf.cn" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/koalas-rpc2.png')" alt="企业生产级百亿日PV高可用可拓展的RPC框架。">
</a>
<a class="friends-item" href="https://dynamictp.cn/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/dynamic-tp.png')" alt="🔥🔥🔥 基于配置中心的轻量级动态可监控线程池">
</a>
<a class="friends-item" href="https://hertzbeat.com/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/hertzbeat_brand.jpg')" alt="易用友好的云监控系统">
</a>
<a class="friends-item" href="https://maxkey.top/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/maxkey.png')" alt="业界领先的身份管理和认证产品">
</a>
<a class="friends-item" href="https://plugins.sheng90.wang/fast-request/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/fast-request.png')" alt="Idea 版 Postman，为简化调试API而生">
</a>
<a class="friends-item" href="https://async.sizegang.cn/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/gobrs-async.png')" alt="🔥 配置极简功能强大的异步任务动态编排框架">
</a>
<a class="friends-item" href="https://www.x-easypdf.cn" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/x-easypdf.png')" alt="一个用搭积木的方式构建pdf的框架（基于pdfbox）">
</a>
<a class="friends-item" href="http://dromara.gitee.io/image-combiner" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/image-combiner.png')" alt="一个专门用于图片合成的工具，没有很复杂的功能，简单实用，却不失强大">
</a>
<a class="friends-item" href="https://www.herodotus.cn/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/dante-cloud2.png')" alt="Dante-Cloud 是一款企业级微服务架构和服务能力开发平台。">
</a>
<a class="friends-item" href="https://dromara.org/zh/projects/" target="_blank">
	<img class="no-zoom friends-item-img" :src="$withBase('/images/friends/link/dromara.png')" alt="让每一位开源爱好者，体会到开源的快乐。">
</a>
</p>

## 🧲友情链接
<div class="friends-item">
    <a href="https://shop108037867.taobao.com" target="_blank">
        <img :src="$withBase('/images/friends/yuanlaiyishe.png')" class="no-zoom friends-item-img" alt="yuanlaiyishe">
    </a>
</div>
<div class="friends-item">
    <a href="http://www.layui-vue.com" target="_blank">
        <img :src="$withBase('/images/friends/layui-vue.png')" class="no-zoom friends-item-img" alt="layui-vue">
    </a>
</div>
<div class="friends-item" style="max-width:70px !important;">
    <a href="https://weihongbin.com/" target="_blank">
        <img :src="$withBase('/images/friends/weihongbin.png')" class="no-zoom friends-item-img"  alt="weihongbin">
    </a>
</div>
<div class="friends-item" style="max-width:70px !important;">
    <a href="https://www.wxy97.com/" target="_blank">
        <img :src="$withBase('/images/friends/wxy97.png')" class="no-zoom friends-item-img"  alt="王旭阳个人博客">
    </a>
</div>
<div class="friends-item" style="max-width:70px !important;">
    <a href="https://www.luckyhe.com/" target="_blank">
        <img :src="$withBase('/images/friends/luckyhe.png')" class="no-zoom friends-item-img"  alt="牧码人博客">
    </a>
</div>


<div class="wwads-cn wwads-horizontal" style="max-width:350px;"> <link rel="stylesheet" href="https://cdn.wwads.cn/css/wwads.css">
<a href="https://wwads.cn?aff_id=217" class="wwads-img" target="_blank" rel="nofollow">
<img src="https://cdn.wwads.cn/images/placeholder/wwads-friendly-ads.png" width="130" ></a>
<div class="wwads-content"><a href="https://wwads.cn?aff_id=217" class="wwads-text" target="_blank" rel="nofollow" >B2B Advertising Made Easy —— 我们帮助 to B 企业轻松投放更精准 & 友好的广告</a>
<a href="https://wwads.cn?aff_id=217" class="wwads-poweredby" title="万维广告——让广告交易像网购一样简单" target="_blank" rel="nofollow"><img class="wwads-logo"><span class="wwads-logo-text">广告</span></a> </div></div>

<br/>