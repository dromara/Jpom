<p align="center">
	<a href="https://jpom.top/" target="_blank">
	    <img src="https://jpom.top/images/logo/jpom_logo.svg" width="400" alt="Jpom logo">
	</a>
</p>
<p align="center">
	<strong>【dromara】🚀 简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件</strong>
</p>
<p align="center">
	<strong>也是一款原生 Ops 软件</strong> · <a href="./README-en.md">English</a>
</p>

<p align="center">
	<a target="_blank" href="https://gitee.com/dromara/Jpom">
        <img src="https://gitee.com/dromara/Jpom/badge/star.svg?theme=gvp" alt="Gitee stars">
    </a>
	<a target="_blank" href="https://github.com/dromara/Jpom">
		<img src="https://img.shields.io/github/stars/dromara/Jpom.svg?style=social" alt="GitHub stars">
    </a>
	<a target="_blank" href="https://gitcode.com/dromara/Jpom">
		<img src="https://gitcode.com/dromara/Jpom/star/badge.svg" alt="GitCode stars">
    </a>
    <a target="_blank" href="https://license.coscl.org.cn/MulanPSL2">
		<img src="https://img.shields.io/:license-MulanPSL2-blue.svg" alt="License: Mulan PSL v2">
    </a>
</p>

<p align="center">
  <a target="_blank" href="https://jpom.top/pages/changelog/new/">
        <img src="https://img.shields.io/github/v/release/dromara/Jpom.svg" alt="Latest release">
    </a>
    <a target="_blank" href="https://hub.docker.com/repository/docker/jpomdocker/jpom">
		<img src="https://img.shields.io/docker/pulls/jpomdocker/jpom.svg" alt="Docker pulls">
    </a>
    <a target="_blank" href="https://www.codacy.com/gh/dromara/Jpom/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=dromara/Jpom&amp;utm_campaign=Badge_Grade">
      <img src="https://app.codacy.com/project/badge/Grade/843b953f1446449c9a075e44ea778336" alt="Codacy grade">
    </a>
    <a target="_blank" href="https://jpom.top/">
        <img src="https://img.shields.io/badge/JDK-1.8.0_40+-green.svg" alt="JDK 8+">
    </a>
</p>

<p align="center">
  👉 大陆站：<a target="_blank" href="https://jpom.top">https://jpom.top</a> 👈
</p>

> ⚠️ **维护状态**：Jpom 已暂停常规功能迭代，安全维护仍在继续。使用 `2.11.12` 及更早版本的用户请尽快升级到 `2.11.13` 安全修复版。
>
> 详情见：[项目暂停维护公告](https://jpom.top/pages/project-suspended/) · [2.11.13 安全公告](https://jpom.top/pages/security/2.11.13/) · [全部安全公告](https://jpom.top/pages/security/) · [下载中心](https://jpom.top/pages/all-downloads/)

## 📖 目录

- [痛点与解决方案](#pain-points)
- [核心功能](#features)
- [特别提醒](#notes)
- [版本更新日志](#changelog)
- [安装 Jpom](#install)
  - [方式一：一键安装（Linux，推荐）](#install-one-click)
  - [方式二：容器化安装](#install-docker)
  - [方式三：下载安装包](#install-download)
  - [方式四：编译安装](#install-build)
  - [方式五：Docker Compose 一键启动](#install-compose)
  - [方式六：本地编译运行](#install-dev)
- [管理 Jpom 服务](#commands)
- [Linux 系统服务方式管理](#systemd)
- [Jpom 参数配置](#config)
- [常见问题与实践案例](#faq)
- [构建案例仓库](#examples)
- [整体架构](#architecture)
- [交流与反馈](#community)
- [周边商品](#merch)
- [贡献指南](#contributing)
  - [贡献者](#contributors)
- [知识星球](#planet)
- [精品项目推荐](#recommended)
- [开源许可证](#license)
- [鸣谢](#thanks)

<a name="pain-points"></a>
## 😭 日常开发中，您是否有以下痛点？

- <font color="red">**团队中没有专职运维，开发还要兼做运维的活**</font>，需要自己手动构建、部署项目。
- 不同的项目有不同的构建、部署命令。
- 有开发、测试、生产等多环境打包的需求。
- 需要同时监控多个项目的运行状态。
- <u>需要下载 SSH 工具</u>远程连接服务器。
- *需要下载 FTP 工具*传输文件到服务器。
- 多台服务器时，在不同电脑之间同步账号密码很不方便。
- 想使用自动化工具，但工具对服务器性能要求高、搭建麻烦。
- **对自动化工具有个性化需求，想自己修改项目**，但市面上的工具太复杂。

> 如果是分布式项目，以上步骤会更加繁琐。
>
> 让 Jpom 来帮您解决这些痛点吧！然而，这些只是 Jpom 解决的最基础的问题。

<a name="features"></a>
### 😁 使用 [Jpom](https://gitee.com/dromara/Jpom) 后

- 方便的用户管理
  1. 用户操作监控，可将指定用户的指定操作以邮件形式通知
  2. 多用户管理，项目权限独立（上传、删除权限可控制），操作日志完善，支持按工作空间隔离权限
  3. 账号支持开启 **MFA 两步验证**，提高账号安全性
- 以界面形式实时查看项目运行状态、控制台日志，管理项目文件
  1. 支持在线修改项目文本文件
- Docker 容器管理、Docker Swarm 集群管理（**Docker UI**）
- **在线 SSH 终端**，无需安装 PuTTY、Xshell、FinalShell 等软件即可管理服务器
  1. 登录 Jpom 后无需知道服务器密码
  2. 可指定 SSH 禁止执行的命令，避免执行高风险命令，并自动记录命令日志
  3. 设置文件目录后，可在线查看、编辑项目文件及配置文件
  4. SSH 命令模版可在线执行脚本，并支持定时执行
  5. **轻量实现了简单的“堡垒机”功能**
- 使用项目分发一键完成集群项目的多机部署
- 在线构建，无需手动更新、升级项目
  1. 支持拉取 Git、SVN 仓库
  2. **支持容器构建（Docker）**
  3. 支持 SSH、FTP 方式发布
  4. 支持定时构建
  5. 支持 WebHook 触发构建
  6. 支持按路径差异触发构建（仅当变更文件匹配规则时才构建）
- 支持在线编辑 nginx 配置文件并自动 reload
  1. 管理 nginx 状态、SSL 证书
- 项目状态监控，异常时自动报警并尝试重启
  1. 支持邮件、钉钉群、微信群通知，主动感知项目运行状况
- 节点脚本模版 + 定时执行或触发器，可拓展更多功能
- 重要路径授权配置，杜绝用户误操作系统文件

<a name="notes"></a>
### 🔔️ 特别提醒

> 1. 在 Windows 服务器中，部分功能可能因系统特性出现兼容性问题，建议充分测试后再用于生产。Linux 目前兼容性良好。
> 2. 服务端和插件端请安装到不同目录，切勿安装到同一目录。
> 3. 卸载服务端或插件端时，请先停止对应服务，再删除程序文件、日志文件夹和数据目录文件夹。
> 4. 本地构建依赖系统环境：如果构建命令需要 Maven 或 Node.js，请先在构建项目的服务器上安装对应环境；如果安装环境后服务端已启动，需要通过命令行重启服务端才会生效。
> 5. 在 Ubuntu/Debian 服务器上作为插件端可能会添加失败，请在当前用户根目录创建 `.bash_profile` 文件。
> 6. 不建议升级后再降级，可能存在数据不兼容的情况。
> 7. 2.x.x 版本的插件端和服务端主要采用 HTTP 协议通讯，请确保两者网络互通。
> 8. 升级前请阅读 [CHANGELOG.md](https://gitee.com/dromara/Jpom/blob/master/CHANGELOG.md) 中的 `⚠️ 升级说明`；下载安装包后建议先用附带的 `.sha1` 文件校验完整性。
> 9. Jpom 已暂停常规功能迭代（安全维护继续），详见[项目暂停维护公告](https://jpom.top/pages/project-suspended/)。

<a name="changelog"></a>
### 🗒️ [版本更新日志](https://gitee.com/dromara/Jpom/blob/master/CHANGELOG.md)

升级前必看：[CHANGELOG.md](https://gitee.com/dromara/Jpom/blob/master/CHANGELOG.md)（含版本变更与 `⚠️ 升级说明`）；安全修复记录见 [安全公告](https://jpom.top/pages/security/)。

<a name="install"></a>
## 📥 安装 Jpom

Jpom 由 **Server（服务端）** 和 **Agent（插件端）** 组成：Server 提供 Web 管理界面与 API，Agent 在被管理的服务器上执行项目、脚本、文件、监控等任务。两者默认端口分别为 `2122`、`2123`。

Jpom 支持多种安装方式，选择其中一种安装即可。

<a name="install-one-click"></a>
### 方式一：🚀（推荐）一键安装（Linux）

> **注意：安装目录为执行命令时所在的目录。**
>
> ⚠️ **Server 端和 Agent 端不可安装在同一目录下！**
>
> 修改数据、日志存储路径：编辑安装目录下 [`conf/application.yml`](https://gitee.com/dromara/Jpom/blob/master/modules/server/src/main/resources/config_default/application.yml) 中的 `jpom.path` 配置。
>
> 如果服务器没有 `curl`，请先安装：`yum install -y curl`（CentOS）或 `apt-get install -y curl`（Ubuntu/Debian）。

#### 一键安装服务端

```shell
# 默认安装（自动安装 JDK）
curl -fsSL https://jpom.top/docs/install.sh | bash -s Server jdk+default
# 默认安装，并配置开机自启服务
curl -fsSL https://jpom.top/docs/install.sh | bash -s Server jdk+default+service

# 安装服务端和 JDK 环境
curl -fsSL https://jpom.top/docs/install.sh | bash -s Server jdk
# 安装服务端和 JDK、Maven 环境
curl -fsSL https://jpom.top/docs/install.sh | bash -s Server jdk+mvn
```

启动成功后，服务端默认端口为 `2122`，可通过 `http://127.0.0.1:2122/` 访问管理页面（非本机访问时，把 `127.0.0.1` 换成服务器 IP）。

> 如果无法访问管理页面，先检查防火墙是否放行 `2122` 端口：
>
> ```bash
> # 放行管理系统的 2122 端口
> firewall-cmd --add-port=2122/tcp --permanent
> # 重启防火墙后生效
> firewall-cmd --reload
> ```
>
> 如果系统内已放行端口仍无法访问，并且使用的是云服务器，请到云服务器控制台检查安全组规则。
>
> ⚠️ Linux 系统可能有 Firewall、Iptables、SELinux 等多种防火墙，排查时需要逐一检查。

#### 一键安装插件端

> 如果安装服务端的服务器也需要被管理，请在服务端所在服务器上再安装插件端（两者可以安装在同一服务器，但目录必须不同）。
>
> 修改插件端数据、日志存储路径：编辑安装目录下 [`conf/application.yml`](https://gitee.com/dromara/Jpom/blob/master/modules/agent/src/main/resources/config_default/application.yml) 中的 `jpom.path` 配置。

```shell
# 默认安装（自动安装 JDK）
curl -fsSL https://jpom.top/docs/install.sh | bash -s Agent jdk+default
# 默认安装，并配置开机自启服务
curl -fsSL https://jpom.top/docs/install.sh | bash -s Agent jdk+default+service

# 安装插件端和 JDK 环境
curl -fsSL https://jpom.top/docs/install.sh | bash -s Agent jdk
```

启动成功后，插件端默认端口为 `2123`，供服务端连接使用。

<a name="install-docker"></a>
### 方式二：📦 容器化安装

> ⚠️ 需要先安装 Docker，参考 [Docker 安装文档](https://jpom.top/pages/practice/c846d3/)。
>
> 容器化安装仅提供服务端。由于容器与宿主机环境隔离，插件端的很多功能无法正常使用，因此不建议对插件端做容器化。

#### 一条命令安装

```shell
docker run -d --name jpom-server --restart=always -p 2122:2122 jpomdocker/jpom
```

#### 挂载目录存储数据（部分环境可能存在兼容性问题）

1. Docker Hub 仓库

```shell
docker pull jpomdocker/jpom
mkdir -p /home/jpom-server/logs
mkdir -p /home/jpom-server/data
mkdir -p /home/jpom-server/conf
docker run -d --name jpom-server --restart=always -p 2122:2122 \
	-v /home/jpom-server/logs:/usr/local/jpom-server/logs \
	-v /home/jpom-server/data:/usr/local/jpom-server/data \
	-v /home/jpom-server/conf:/usr/local/jpom-server/conf \
	jpomdocker/jpom
```

2. 阿里云镜像仓库（国内网络）

```shell
docker pull registry.cn-chengdu.aliyuncs.com/jpomdocker/jpom
mkdir -p /home/jpom-server/logs
mkdir -p /home/jpom-server/data
mkdir -p /home/jpom-server/conf
docker run -d --name jpom-server --restart=always -p 2122:2122 \
	-v /home/jpom-server/logs:/usr/local/jpom-server/logs \
	-v /home/jpom-server/data:/usr/local/jpom-server/data \
	-v /home/jpom-server/conf:/usr/local/jpom-server/conf \
	registry.cn-chengdu.aliyuncs.com/jpomdocker/jpom
```

#### 使用容器卷存储数据

1. Docker Hub 仓库

```shell
docker pull jpomdocker/jpom
docker volume create jpom-server-data
docker volume create jpom-server-logs
docker volume create jpom-server-conf
docker run -d --name jpom-server --restart=always -p 2122:2122 \
	-v jpom-server-data:/usr/local/jpom-server/data \
	-v jpom-server-logs:/usr/local/jpom-server/logs \
	-v jpom-server-conf:/usr/local/jpom-server/conf \
	jpomdocker/jpom
```

2. 阿里云镜像仓库（国内网络）

```shell
docker pull registry.cn-chengdu.aliyuncs.com/jpomdocker/jpom
docker volume create jpom-server-data
docker volume create jpom-server-logs
docker volume create jpom-server-conf
docker run -d --name jpom-server --restart=always -p 2122:2122 \
	-v jpom-server-data:/usr/local/jpom-server/data \
	-v jpom-server-logs:/usr/local/jpom-server/logs \
	-v jpom-server-conf:/usr/local/jpom-server/conf \
	registry.cn-chengdu.aliyuncs.com/jpomdocker/jpom
```

> 在低版本 Docker 中运行可能出现 `ls: cannot access '/usr/local/jpom-server/lib/': Operation not permitted` 错误，此时需要添加 `--privileged` 参数，例如：
>
> ```shell
> docker run -d --name jpom-server --privileged -p 2122:2122 jpomdocker/jpom
> ```
>
> 安装 Docker、配置镜像、自动启动、查找安装目录等可参考文档 [Docker 容器安装教程](https://jpom.top/pages/practice/c846d3/)。

<a name="install-download"></a>
### 方式三：💾 下载安装包

1. 从 [下载中心](https://jpom.top/pages/all-downloads/) 下载对应的 `server-x.x.x-release`、`agent-x.x.x-release` 安装包（zip 或 tar.gz）及 `.sha1` 校验文件。
2. 校验安装包完整性（推荐），例如 `sha1sum server-x.x.x-release.zip`，与下载的 `.sha1` 文件内容比对。
3. 解压安装包，并将整个目录上传到目标服务器。
4. 安装插件端
   1. `agent-x.x.x-release` 目录为插件端的全部安装文件
   2. 启动插件端：Windows 使用 bat 脚本，Linux 使用 sh 脚本（如出现乱码或无法执行，请检查编码格式、换行符是否匹配）
   3. 插件端默认运行端口：`2123`
5. 安装服务端
   1. `server-x.x.x-release` 目录为服务端的全部安装文件
   2. 启动服务端：Windows 使用 bat 脚本，Linux 使用 sh 脚本（如出现乱码或无法执行，请检查编码格式、换行符是否匹配）
   3. 服务端默认运行端口：`2122`，访问管理页面：`http://127.0.0.1:2122/`（非本机访问时，把 `127.0.0.1` 换成服务器 IP）

<a name="install-build"></a>
### 方式四：⌨️ 编译安装

**环境要求**：JDK 8、Maven 3.6+、Node.js 18。

> ⚠️ 构建顺序不能颠倒：必须先构建前端，再打包后端，否则管理页面会 404 或显示旧页面。

1. 从 [Jpom 仓库](https://gitee.com/dromara/Jpom) 拉取最新完整代码（建议使用 master 分支）
2. 构建前端：切换到 `web-vue` 目录，执行 `npm install`，再执行 `npm run build`（产物输出到 `modules/server/src/main/resources/dist`）
3. 构建后端：回到项目根目录，执行 `mvn clean package`
4. 安装插件端：安装包位于 `modules/agent/target/agent-x.x.x-release`，将整个目录上传到服务器后启动，默认端口 `2123`
5. 安装服务端：安装包位于 `modules/server/target/server-x.x.x-release`，将整个目录上传到服务器后启动，默认端口 `2122`

> 也可以使用 `script/release.bat` 或 `script/release.sh` 快速打包。

<a name="install-compose"></a>
### 方式五：🧩 Docker Compose 一键启动

前提：已安装 Git、Docker 与 Docker Compose。该方式会在本机拉取代码并自动构建、启动服务。

> 启动前请修改 `.env` 文件中的 `SERVER_TOKEN` 等配置，切勿将示例 token 用于生产环境。

```shell
# 安装 git（CentOS 使用 yum，Ubuntu/Debian 使用 apt-get install -y git）
yum install -y git
git clone https://gitee.com/dromara/Jpom.git
cd Jpom

# Docker Compose v2
docker compose -f docker-compose.yml up -d
# Docker Compose v1（旧版本）
docker-compose -f docker-compose.yml up -d
```

> 仓库还提供 `docker-compose-local.yml`（本地构建）和 `docker-compose-cluster.yml`（集群）示例，可按需使用。
>
> 如需重新构建镜像，可执行 `docker compose -f docker-compose.yml up -d --build`，或 `docker compose -f docker-compose.yml build --no-cache`。

<a name="install-dev"></a>
### 方式六：💻 本地编译运行（开发调试）

1. 从 [Jpom 仓库](https://gitee.com/dromara/Jpom) 拉取最新完整代码（建议使用 master 分支；想体验新功能可使用 dev 分支）
2. 构建前端：切换到 `web-vue` 目录，执行 `npm install`、`npm run build`
3. 运行服务端：启动 `org.dromara.jpom.JpomServerApplication`，默认端口 `2122`
4. 运行插件端：启动 `org.dromara.jpom.JpomAgentApplication`，留意控制台打印的默认账号密码信息，默认端口 `2123`
5. 前端开发模式：切换到 `web-vue` 目录执行 `npm run dev`，访问 `http://127.0.0.1:3000/`（非本机访问时把 `127.0.0.1` 换成服务器 IP）

<a name="commands"></a>
## 🎛️ 管理 Jpom 服务

安装包的 `bin/` 目录提供管理脚本，Server 与 Agent 分别使用各自的脚本。

1. Windows 系统使用 bat 脚本文件

```bat
:: 服务端管理脚本（命令行）
:: 用法：Server.bat [start|stop|restart|status]
./bin/Server.bat start
./bin/Server.bat stop
./bin/Server.bat restart
./bin/Server.bat status

:: 服务端管理脚本（控制面板），按面板提示输入操作
./bin/Server.bat

:: 插件端管理脚本（命令行）
:: 用法：Agent.bat [start|stop|restart|status]
./bin/Agent.bat start
./bin/Agent.bat stop
./bin/Agent.bat restart
./bin/Agent.bat status

:: 插件端管理脚本（控制面板），按面板提示输入操作
./bin/Agent.bat
```

> Windows 系统中执行启动后需要根据日志跟进启动状态；如果出现乱码，请检查或修改编码格式，bat 编码格式推荐为 `GB2312`。

2. Linux 系统使用 sh 脚本文件

```bash
# 服务端
./bin/Server.sh start     # 启动 Jpom 服务端
./bin/Server.sh stop      # 停止 Jpom 服务端
./bin/Server.sh restart   # 重启 Jpom 服务端
./bin/Server.sh status    # 查看 Jpom 服务端运行状态
./bin/Service.sh install  # 创建 Jpom 服务端的应用服务（jpom-server）

# 插件端
./bin/Agent.sh start      # 启动 Jpom 插件端
./bin/Agent.sh stop       # 停止 Jpom 插件端
./bin/Agent.sh restart    # 重启 Jpom 插件端
./bin/Agent.sh status     # 查看 Jpom 插件端运行状态
./bin/Service.sh install  # 创建 Jpom 插件端的应用服务（jpom-agent）
```

<a name="systemd"></a>
## 🐧 Linux 系统服务方式管理

> 以下服务安装方式仅供参考，实际中可根据需求自定义配置。
>
> 在 Server 或 Agent 安装目录执行 `./bin/Service.sh install`（建议使用 root 或 sudo）后，会注册对应的 systemd 服务（`jpom-server` / `jpom-agent`）。
>
> ```bash
> systemctl {status | start | stop | restart | enable} jpom-server
> systemctl {status | start | stop | restart | enable} jpom-agent
> ```

<a name="config"></a>
## ⚙️ Jpom 参数配置

配置文件位于程序运行根路径的 `conf/` 目录：

### 程序配置 `./conf/application.yml`

1. 插件端示例：[`application.yml`](https://gitee.com/dromara/Jpom/blob/master/modules/agent/src/main/resources/config_default/application.yml)
2. 服务端示例：[`application.yml`](https://gitee.com/dromara/Jpom/blob/master/modules/server/src/main/resources/config_default/application.yml)

### 日志配置 `./conf/logback.xml`

1. 插件端示例：[`logback.xml`](https://gitee.com/dromara/Jpom/blob/master/modules/agent/src/main/resources/config_default/logback.xml)
2. 服务端示例：[`logback.xml`](https://gitee.com/dromara/Jpom/blob/master/modules/server/src/main/resources/config_default/logback.xml)

> 数据目录、端口、数据库等均可在 `application.yml` 中配置；升级前建议备份 `data/`、`conf/` 目录。

<a name="faq"></a>
## 📝 常见问题、操作说明

- [文档主页](https://jpom.top/)
- [FAQ](https://jpom.top/pages/FQA/)
- [名词解释](https://jpom.top/pages/FQA/proper-noun/)
- [安全公告](https://jpom.top/pages/security/)
- [下载中心](https://jpom.top/pages/all-downloads/)

### 实践案例

> 部分图片加载可能较慢。

1. [本地构建 + SSH 发布 Java 项目](https://jpom.top/pages/practice/build-java-ssh-release/)
2. [本地构建 + 项目发布 Node 项目](https://jpom.top/pages/practice/build-node-release/)
3. [本地构建 + SSH 发布 Node 项目](https://jpom.top/pages/practice/build-node-ssh-release/)
4. [本地构建 + 自定义管理 Python 项目](https://jpom.top/pages/practice/project-dsl-python/)
5. [自定义管理 Java 项目](https://jpom.top/pages/practice/project-dsl-java/)
6. [管理编译安装的 nginx](https://jpom.top/pages/practice/node-nginx/)
7. [管理 Docker](https://jpom.top/pages/practice/docker-cli/)
8. [容器构建 + 项目发布 Java 项目](https://jpom.top/pages/practice/build-docker-java-node-release/)
9. [更多实践案例 >>](https://jpom.top/pages/practice/)

<a name="examples"></a>
## 构建案例仓库代码

1. [Jboot 案例代码](https://gitee.com/keepbx/Jpom-demo-case/tree/master/jboot-test)
2. [SpringBoot 案例代码（ClassPath）](https://gitee.com/keepbx/Jpom-demo-case/tree/master/springboot-test)
3. [SpringBoot 案例代码（Jar）](https://gitee.com/keepbx/Jpom-demo-case/tree/master/springboot-test-jar)
4. [Node Vue 案例代码（antdv）](https://gitee.com/keepbx/Jpom-demo-case/tree/master/antdv)
5. [Python 案例代码](https://gitee.com/keepbx/Jpom-demo-case/tree/master/python)

> Node.js 编译指定目录：

```bash
yarn --cwd xxxx/ install
yarn --cwd xxxx/ build
```

> Maven 编译指定目录：

```bash
mvn -f xxxx/pom.xml clean package
```

<a name="architecture"></a>
## 🛠️ 整体架构

![Jpom 功能架构](https://jpom.top/images/jpom-func-arch.png)

<a name="community"></a>
## 🐞 交流讨论、反馈 BUG、提出建议

1. 欢迎加入 Jpom 微信交流群，和我们一起交流讨论（添加小助手时备注 Jpom 进群）
2. 开源项目离不开社区的支持。如果项目帮助到了您，并且想给我们加个餐，欢迎扫描下方[微信、支付宝收款码赞赏](https://jpom.top/images/qrcode/praise-all.png)，或通过 [Gitee 赞赏](https://gitee.com/dromara/Jpom)（在项目首页下方点击捐赠，支持微信和支付宝）。[赞赏记录](https://jpom.top/pages/praise/publicity/)
3. 购买开源周边商品：[周边介绍](https://jpom.top/pages/shop/)
4. 企业技术服务请单独与我们联系沟通服务方案
5. 反馈 BUG、提出建议，欢迎新建 [issues](https://gitee.com/dromara/Jpom/issues)，开发人员会不定期查看回复
6. 参与贡献，请查看[贡献指南](#contributing)

感谢所有赞赏以及参与贡献的小伙伴，您们的支持是我们不断更新前进的动力！

<p align="center">
<img src="https://jpom.top/images/qrcode/praise-all.png" alt="赞赏二维码">
</p>

<a name="merch"></a>
## 💖 周边商品

为了更好地维持开源项目，我们推出了周边商品。

购买周边既能获得一份小商品，也能支持项目持续运营（周边商品价格会比市场价稍高，介意请勿下单）。

<p align="center">
<img src="https://jpom.top/images/qrcode/weixin-shop-jpom66.png" alt="周边商品">
</p>

<a name="contributing"></a>
## 🔨 贡献指南

> 提交贡献即认为签署了 [CLA](https://gitee.com/dromara/Jpom/blob/master/CLA.md) 协议。

### 贡献须知

Jpom 作为开源项目，离不开社区的支持，欢迎任何人修改和提出建议。贡献无论大小，您的贡献会帮助背后成千上万的使用者以及开发者，您做出的贡献也会永远保留在项目的贡献者名单中，这也是开源项目的意义所在！

为了保证项目代码的质量与规范，以及帮助您更快地了解项目结构，请在贡献之前阅读：

- [贡献者行为准则](https://gitee.com/dromara/Jpom/blob/master/CODE_OF_CONDUCT.md)
- [中英文排版规范](https://gitee.com/dromara/Jpom/blob/master/typography-specification.md)

### 贡献步骤（代码）

1. Fork 本仓库。

2. Fork 后您的账号下会多出一个和本仓库一样的仓库，把您账号下的仓库 clone 到本地。请把链接中的用户名替换为您自己的 Gitee 用户名：代码贡献请使用 `dev` 分支。

   ```bash
   git clone -b dev https://gitee.com/用户名/Jpom.git
   ```

3. 修改代码并提交。

   ```bash
   # 把修改的文件添加到暂存区
   git add .
   # 提交到本地仓库，说明您具体做了什么修改
   git commit -m '填写您做了什么修改'
   # 推送到远程仓库
   git push origin dev
   ```

4. 在 Gitee 上向 `dromara/Jpom` 的 `dev` 分支发起 Pull Request，等待维护者审核合并。

### 文档贡献

项目文档已独立为 [jpom-docs](https://github.com/bwcx-jzy/jpom-docs) 仓库（`main` 分支），文档站内容位于该仓库的 `docs/` 目录。文档相关修改请提交到该仓库，不再提交到主仓的 `docs` 分支。

### 项目分支说明

| 分支     | 说明                                                                    |
|--------|-----------------------------------------------------------------------|
| master | 稳定发布分支，受保护，不接受 PR。beta 分支测试通过后合并到此分支。                                 |
| beta   | beta 预发布分支，受保护，不接受 PR。dev 分支测试通过后合并到此分支。                               |
| dev    | 开发分支，接受 PR，功能与修复请提交到 dev 分支。                                           |

> 目前主要使用 `dev` 分支接受 PR；`docs` 分支已归档，文档请提交到 [jpom-docs](https://github.com/bwcx-jzy/jpom-docs) 仓库。

<a name="contributors"></a>
### 贡献者

<a href="https://github.com/dromara/Jpom/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=dromara/Jpom" alt="贡献者">
</a>

Made with [contrib.rocks](https://contrib.rocks).

<a name="planet"></a>
## 🌍 知识星球

<p align="center">
<img src="https://jpom.top/images/zsxq.jpg" alt="扫码加入知识星球，了解学习更多知识">
</p>

<a name="recommended"></a>
## 🔔 精品项目推荐

| 项目名称 | 项目地址 | 项目介绍 |
| --- | --- | --- |
| SpringBoot_v2 | [https://gitee.com/bdj/SpringBoot_v2](https://gitee.com/bdj/SpringBoot_v2) | 基于 SpringBoot 的一款纯净脚手架 |
| TLog GVP 项目 | [https://gitee.com/dromara/TLog](https://gitee.com/dromara/TLog) | 一个轻量级的分布式日志标记追踪神器，10 分钟即可接入，自动对日志打标签完成微服务的链路追踪 |
| Sa-Token | [https://gitee.com/dromara/sa-token](https://gitee.com/dromara/sa-token) | 这可能是史上功能最全的 Java 权限认证框架！ |
| Erupt | [https://gitee.com/erupt/erupt](https://gitee.com/erupt/erupt) | 零前端代码，纯注解开发 admin 管理后台 |
| hippo4j | [https://gitee.com/magegoofy/hippo4j](https://gitee.com/magegoofy/hippo4j) | 强大的动态线程池框架，附带监控报警功能 |
| HertzBeat | [https://gitee.com/dromara/hertzbeat](https://gitee.com/dromara/hertzbeat) | 易用友好的云监控系统，无需 Agent，具备强大的自定义监控能力 |

<a name="license"></a>
## 📄 开源许可证

Jpom 使用 [Mulan PSL v2](https://license.coscl.org.cn/MulanPSL2) 许可证，详见 [LICENSE](https://gitee.com/dromara/Jpom/blob/master/LICENSE)。

<a name="thanks"></a>
## 🤝 鸣谢

- 感谢 JetBrains 提供的免费开源 License：

<p>
<img src="https://jpom.top/images/friends/ad/jetbrains.png" alt="JetBrains" style="float:left;">
</p>
