---
home: true
heroImage: /images/jpom_logo.png
heroText: 
tagline: 简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件
actionText: 开始使用 →
actionLink: /pages/install/
bannerBg: none # auto => 网格纹背景(有bodyBgImg时无背景)，默认 | none => 无 | '大图地址' | background: 自定义背景样式       提示：如发现文本颜色不适应你的背景时可以到palette.styl修改$bannerTextColor变量

features: # 可选的
- title: 节点管理
  details: 集群节点，统一管理多节点的项目，实现快速一键分发项目文件
- title: 项目管理
  details: 创建、启动、停止、实时查看项目控制台日志，管理项目文件
- title: SSH面板
  details: 在浏览器中管理 SSH，方便进行日常运维,记录执行命令记录
- title: 在线构建
  details: 在线拉取 GIT、SVN 仓库快速构建项目包，不用运维人员手动上传项目包
- title: 项目监控
  details: 实时监控项目当前状态、如果异常自动触发邮件、钉钉报警通知
- title: 用户管理
  details: 多用户管理，实现不同用户不同权限，用户操作、管理日志完善记录
- title: 白名单模式
  details: 为了数据安全防止用户随便查看、删除文件配置安全的白名单路径。
- title: DOCKER 管理
  details: 在线管理镜像、容器、SWARM 集群。界面化管理 DOCKER 
- title: NGINX 配置、SSL 证书
  details: 在线快速方便的修改 NGINX 配置文件，SSL 证书统一管理

# 文章列表显示方式: detailed 默认，显示详细版文章列表（包括作者、分类、标签、摘要、分页等）| simple => 显示简约版文章列表（仅标题和日期）| none 不显示文章列表
postList: none
---

<br/>

<p align="center">
  <a class="become-sponsor" href="https://jpom.keepbx.cn?from=site-middle">演示站点</a>
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
</style>

<br/>

## 😭 日常开发中，您是否有以下痛点？

- 团队中没有专门的运维，开发还要做运维的活，需要自己手动构建、部署项目。
- 不同的项目有不同的构建、部署命令。
- 有开发、测试、生产等多环境打包的需求。
- 需要同时监控多个项目的运行状态。
- 需要下载 SSH 工具远程连接服务器。
- 需要下载 FTP 工具传输文件到服务器。
- 多台服务器时，在不同电脑之间账号密码同步不方便。
- 想使用一些自动化工具，但是对服务器性能太高，搭建太麻烦。
- 对自动化工具有个性化的需求，想自己修改项目，但是市面上的工具太复杂了。

> 如果是分布式的项目，以上步骤则更加繁琐。
>
> 让 Jpom 来帮你解决这些痛点吧！然而，这些只是 Jpom 解决的最基础的功能。

### 😁 使用 [Jpom](https://gitee.com/dromara/Jpom) 后

1. 使用浏览器登录方便快捷管理项目
2. 方便的用户管理
  1. 用户操作监控，监控指定用户指定操作以邮件形式通知
  2. 多用户管理，用户项目权限独立（上传、删除权限可控制），完善的操作日志，使用工作空间隔离权限
  3. 账号可以开启 MFA 两步验证提高账号安全性
3. 界面形式实时查看项目运行状态、控制台日志、管理项目文件
  1. 在线修改项目文本文件
4. docker 容器管理、docker swarm 集群管理
5. 在线 SSH 终端，让您在没有 Xshell、FinalShell 等软件也能轻松管理服务器
  1. 运维登录 Jpom 系统后不需要知道服务器密码
  2. 能指定 SSH 禁止执行的命令，避免执行高风险命令，并且能自动执行命令日志
  3. 设置文件目录，在线查看管理对应项目文件及配置文件
  4. SSH 命令模版在线执行脚本还能定时执行
  5. 在线修改文本文件
  6. 轻量的实现了简单的"堡垒机"功能
6. 使用项目分发一键搞定集群项目多机部署
7. 在线构建不用手动更新升级项目
  1. 支持拉取 GIT、SVN 仓库
  2. 支持容器构建（docker）
  3. 支持 SSH 方式发布
  4. 支持定时构建
  5. 支持 WebHook 形式触发构建
8. 支持在线编辑 nginx 配置文件并自动 reload 等操作
  1. 管理 nginx 状态，管理 SSL 证书
9. 项目状态监控异常自动报警、自动尝试重启
  1. 支持邮件 + 钉钉群 + 微信群通知，主动感知项目运行状况
10. 节点脚本模版+定时执行，拓展更多可能
11. 重要路径白名单模式，杜绝用户误操作系统文件


<br/>

## 🏡代码托管

- [Gitee ![star](https://gitee.com/dromara/Jpom/badge/star.svg?theme=gvp)](https://gitee.com/dromara/Jpom/)
- [Github](https://github.com/dromara/Jpom)

<br/>

## 💪🏻参与贡献

欢迎各路好汉一起来参与完善 Jpom，我们期待你的 PR！

如果想贡献，请先查看[贡献准则](/pages/ae4dd5/)。

<br/>

## 🍭架构图

<img :src="$withBase('/images/jpom-func-arch.jpg')" style="zoom: 120%" alt="jpom-func-arch">

<br/>

## 🤝 dromara 组织项目
<span style="width: 150px;flex:1;text-align: left">
    <a href="https://hutool.cn/" target="_blank">
        <img :src="$withBase('/images/friends/hutool-logo.png')" class="no-zoom" style="height:40px;max-width:150px;margin: 10px;" alt="hutool">
    </a>
</span>
<span style="width: 150px;text-align: left">
    <a href="http://sa-token.dev33.cn/" target="_blank">
        <img :src="$withBase('/images/friends/satoken-logo.png')" class="no-zoom" style="height:40px;max-width:150px;margin: 10px;" alt="satoken">
    </a>
</span>
<span style="width: 150px;text-align: left">
    <a href="https://www.maxkey.top/" target="_blank">
        <img :src="$withBase('/images/friends/maxkey.png')" class="no-zoom" style="height:40px;max-width:150px;margin: 10px;" alt="maxkey">
    </a>
</span>
<span style="width: 150px;text-align: left">
    <a href="https://liteflow.yomahub.com/" target="_blank">
        <img :src="$withBase('/images/friends/liteflow-logo.png')" class="no-zoom" style="height:40px;max-width:170px;margin: 10px;" alt="liteflow">
    </a>
</span>
<span style="width: 150px;text-align: left">
    <a href="https://tlog.yomahub.com/" target="_blank">
        <img :src="$withBase('/images/friends/tlog-logo.png')" class="no-zoom" style="height:40px;max-width:150px;margin: 10px;" alt="tlog">
    </a>
</span>
<span style="width: 150px;text-align: left">
    <a href="https://forest.dtflyx.com/" target="_blank">
        <img :src="$withBase('/images/friends/forest-logo.png')" class="no-zoom" style="height:40px;max-width:150px;margin: 10px;" alt="forest">
    </a>
</span>
<span style="width: 150px;text-align: left">
    <a href="https://easy-es.cn/" target="_blank">
        <img :src="$withBase('/images/friends/easy-es.png')" class="no-zoom" style="height:40px;max-width:150px;margin: 10px;" alt="easy-es">
    </a>
</span>
<span style="width: 150px;text-align: left">
    <a href="https://su.usthe.com/" target="_blank">
        <img :src="$withBase('/images/friends/hor-brand128.png')" class="no-zoom" style="height:40px;max-width:150px;margin: 10px;" alt="hor-brand128">
    </a>
</span>
<span style="width: 150px;text-align: left">
    <a href="https://hertzbeat.com/" target="_blank">
        <img :src="$withBase('/images/friends/hertzbeat_brand.jpg')" class="no-zoom" style="height:40px;max-width:150px;margin: 10px;" alt="hertzbeat">
    </a>
</span>

## 🧲友情链接
<span style="width: 150px;text-align: left">
    <a href="https://shop108037867.taobao.com" target="_blank">
        <img :src="$withBase('/images/friends/yuanlaiyishe.png')" class="no-zoom" style="height:40px;max-width:150px;margin: 10px;" alt="yuanlaiyishe">
    </a>
</span>
<span style="width: 150px;text-align: left">
    <a href="https://weihongbin.com/" target="_blank">
        <img :src="$withBase('/images/friends/weihongbin.png')" class="no-zoom" style="height:40px;max-width:150px;margin: 10px;" alt="weihongbin">
    </a>
</span>


<div class="wwads-cn wwads-horizontal" style="max-width:350px;"> <link rel="stylesheet" href="https://cdn.wwads.cn/css/wwads.css">
<a href="https://wwads.cn?aff_id=217" class="wwads-img" target="_blank" rel="nofollow">
<img src="https://cdn.wwads.cn/images/placeholder/wwads-friendly-ads.png" width="130" ></a>
<div class="wwads-content"><a href="https://wwads.cn?aff_id=217" class="wwads-text" target="_blank" rel="nofollow" >B2B Advertising Made Easy —— 我们帮助 to B 企业轻松投放更精准 & 友好的广告</a>
<a href="https://wwads.cn?aff_id=217" class="wwads-poweredby" title="万维广告——让广告交易像网购一样简单" target="_blank" rel="nofollow"><img class="wwads-logo"><span class="wwads-logo-text">广告</span></a> </div></div>