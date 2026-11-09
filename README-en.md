<p align="center">
	<a href="https://jpom.top/" target="_blank">
	    <img src="https://jpom.top/images/logo/jpom_logo.svg" width="400" alt="Jpom logo">
	</a>
</p>
<p align="center">
	<strong>【dromara】🚀 A simple, lightweight, low-intrusion platform for online builds, automated deployment, daily operations, and project monitoring</strong>
</p>
<p align="center">
	<strong>Also a native Ops platform</strong> · <a href="./README.md">中文</a>
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
  👉 Mainland site: <a target="_blank" href="https://jpom.top">https://jpom.top</a> 👈
</p>

> ⚠️ **Maintenance status**: Jpom has paused regular feature development, while security maintenance continues. Users on `2.11.12` and earlier versions should upgrade to `2.11.13`, the security-fixed release, as soon as possible.
>
> For details, see: [Project maintenance suspension notice](https://jpom.top/pages/project-suspended/) · [2.11.13 security advisory](https://jpom.top/pages/security/2.11.13/) · [All security advisories](https://jpom.top/pages/security/) · [Download center](https://jpom.top/pages/all-downloads/)

## 📖 Table of Contents

- [Pain points and solutions](#pain-points)
- [Key features](#features)
- [Special notes](#notes)
- [Changelog](#changelog)
- [Install Jpom](#install)
  - [Option 1: One-click installation (Linux, recommended)](#install-one-click)
  - [Option 2: Containerized installation](#install-docker)
  - [Option 3: Download the installation package](#install-download)
  - [Option 4: Build from source](#install-build)
  - [Option 5: One-click start with Docker Compose](#install-compose)
  - [Option 6: Build and run locally](#install-dev)
- [Manage Jpom services](#commands)
- [Manage Jpom as a Linux system service](#systemd)
- [Jpom configuration](#config)
- [FAQ and practical examples](#faq)
- [Example repositories for builds](#examples)
- [Architecture](#architecture)
- [Community, feedback, and suggestions](#community)
- [Merchandise](#merch)
- [Contributing guide](#contributing)
  - [Contributors](#contributors)
- [Knowledge Planet](#planet)
- [AI Ops integration (Skill / MCP)](#ai-ops)
- [Recommended projects](#recommended)
- [License](#license)
- [Acknowledgements](#thanks)

<a name="pain-points"></a>
## 😭 Do you face these pain points in your daily development?

- <font color="red">**Your team has no dedicated ops engineer, so developers also have to handle operations work**</font>, including manual project builds and deployments.
- Different projects require different build and deployment commands.
- You need to package for multiple environments such as development, testing, and production.
- You need to monitor the running status of multiple projects at the same time.
- <u>You need to download an SSH tool</u> to connect to servers remotely.
- *You need to download an FTP tool* to transfer files to servers.
- With multiple servers, synchronizing account passwords across different computers is inconvenient.
- You want to use automation tools, but they are resource-hungry on the server and cumbersome to set up.
- **You have custom requirements for automation tools and want to modify the project yourself**, but the tools on the market are too complex.

> For distributed projects, these steps become even more cumbersome.
>
> Let Jpom help you solve these pain points! Yet these are only the most basic problems that Jpom solves.

<a name="features"></a>
### 😁 After using [Jpom](https://gitee.com/dromara/Jpom)

- Convenient user management
  1. User activity monitoring: send notifications by email for specified operations performed by specified users
  2. Multi-user management with independent project permissions (upload and delete permissions are controllable), complete operation logs, and workspace-based permission isolation
  3. Accounts support **MFA two-factor authentication** for improved account security
- View project running status and console logs in real time through the UI, and manage project files
  1. Edit project text files online
- Docker container management and Docker Swarm cluster management (**Docker UI**)
- **Online SSH terminal**: manage servers without installing PuTTY, Xshell, FinalShell, or similar tools
  1. No need to know the server password after logging in to Jpom
  2. Specify SSH commands that are forbidden to run to avoid high-risk operations, with automatic command logging
  3. After setting the file directory, view and edit project files and configuration files online
  4. SSH command templates can run scripts online and support scheduled execution
  5. **A simple, lightweight "bastion host" implementation**
- One-click multi-machine deployment of cluster projects with project distribution
- Online builds: no more manual project updates and upgrades
  1. Supports pulling from Git and SVN repositories
  2. **Supports container builds (Docker)**
  3. Supports deployment via SSH and FTP
  4. Supports scheduled builds
  5. Supports WebHook-triggered builds
  6. Supports builds triggered by path changes (build only when changed files match the rules)
- Supports online editing of nginx configuration files with automatic reload
  1. Manage nginx status and SSL certificates
- Project status monitoring with automatic alerts and restart attempts on exceptions
  1. Supports email, DingTalk group, and WeChat group notifications to proactively learn about project health
- Node script templates plus scheduled execution or triggers to extend functionality
- Authorization configuration for important paths to prevent users from accidentally modifying system files

<a name="notes"></a>
### 🔔️ Special notes

> 1. On Windows servers, some features may have compatibility issues due to system characteristics; test them thoroughly before using them in production. Linux currently has good compatibility.
> 2. Install the server and the agent in different directories; never install them in the same directory.
> 3. Before uninstalling the server or the agent, stop the corresponding service first, then delete the program files, the log folder, and the data directory.
> 4. Local builds depend on the system environment: if build commands require Maven or Node.js, install the corresponding environment on the server where the project is built first; if the environment is installed after the server has started, restart the server from the command line for it to take effect.
> 5. On Ubuntu/Debian servers, adding the agent may fail; in that case, create a `.bash_profile` file in the current user's home directory.
> 6. Downgrading after an upgrade is not recommended, as the data may be incompatible.
> 7. The 2.x.x agent and server mainly communicate over HTTP, so make sure the network between them is reachable.
> 8. Before upgrading, read the `⚠️ 升级说明` (upgrade notes) in [CHANGELOG.md](https://gitee.com/dromara/Jpom/blob/master/CHANGELOG.md); after downloading the installation package, verify its integrity with the accompanying `.sha1` file first.
> 9. Jpom has paused regular feature development (security maintenance continues). See the [project maintenance suspension notice](https://jpom.top/pages/project-suspended/) for details.

<a name="changelog"></a>
### 🗒️ [Changelog](https://gitee.com/dromara/Jpom/blob/master/CHANGELOG.md)

Must-read before upgrading: [CHANGELOG.md](https://gitee.com/dromara/Jpom/blob/master/CHANGELOG.md) (including version changes and `⚠️ 升级说明`); security fixes are recorded in the [security advisories](https://jpom.top/pages/security/).

<a name="install"></a>
## 📥 Install Jpom

Jpom consists of a **Server** and an **Agent**: the Server provides the web management UI and APIs, while the Agent executes projects, scripts, files, monitoring, and other tasks on the managed servers. Their default ports are `2122` and `2123` respectively.

Jpom supports multiple installation methods; choose any one of them.

<a name="install-one-click"></a>
### Option 1: 🚀 (Recommended) One-click installation (Linux)

> **Note: The installation directory is the directory in which the command is executed.**
>
> ⚠️ **The Server and the Agent must not be installed in the same directory!**
>
> To change the data and log storage paths: edit the `jpom.path` setting in [`conf/application.yml`](https://gitee.com/dromara/Jpom/blob/master/modules/server/src/main/resources/config_default/application.yml) under the installation directory.
>
> If `curl` is not available on the server, install it first: `yum install -y curl` (CentOS) or `apt-get install -y curl` (Ubuntu/Debian).

#### One-click server installation

```shell
# Default installation (installs JDK automatically)
curl -fsSL https://jpom.top/docs/install.sh | bash -s Server jdk+default
# Default installation, and configure a service that starts on boot
curl -fsSL https://jpom.top/docs/install.sh | bash -s Server jdk+default+service

# Install the server with the JDK environment
curl -fsSL https://jpom.top/docs/install.sh | bash -s Server jdk
# Install the server with the JDK and Maven environments
curl -fsSL https://jpom.top/docs/install.sh | bash -s Server jdk+mvn
```

After a successful start, the default server port is `2122`, and the management page is available at `http://127.0.0.1:2122/` (when accessing from another machine, replace `127.0.0.1` with the server IP).

> If the management page is unreachable, first check whether the firewall allows port `2122`:
>
> ```bash
> # Allow port 2122 for the management system
> firewall-cmd --add-port=2122/tcp --permanent
> # Reload the firewall for the change to take effect
> firewall-cmd --reload
> ```
>
> If the port is already allowed in the system and the page is still unreachable on a cloud server, check the security group rules in the cloud server console.
>
> ⚠️ A Linux system may have multiple firewalls such as Firewall, Iptables, and SELinux; check each of them when troubleshooting.

#### One-click agent installation

> If the machine hosting the Server also needs to be managed, install the Agent on the same machine as well (they can share a machine, but must use different directories).
>
> To change the agent data and log storage paths: edit the `jpom.path` setting in [`conf/application.yml`](https://gitee.com/dromara/Jpom/blob/master/modules/agent/src/main/resources/config_default/application.yml) under the installation directory.

```shell
# Default installation (installs JDK automatically)
curl -fsSL https://jpom.top/docs/install.sh | bash -s Agent jdk+default
# Default installation, and configure a service that starts on boot
curl -fsSL https://jpom.top/docs/install.sh | bash -s Agent jdk+default+service

# Install the agent with the JDK environment
curl -fsSL https://jpom.top/docs/install.sh | bash -s Agent jdk
```

After a successful start, the default agent port is `2123`, which the server connects to.

<a name="install-docker"></a>
### Option 2: 📦 Containerized installation

> ⚠️ Docker must be installed first; see the [Docker installation guide](https://jpom.top/pages/practice/c846d3/).
>
> Containerized installation provides the server only. Because the container is isolated from the host environment, many agent features cannot work properly, so containerizing the agent is not recommended.

#### One-command installation

```shell
docker run -d --name jpom-server --restart=always -p 2122:2122 jpomdocker/jpom
```

#### Mount directories to store data (compatibility issues may occur in some environments)

1. Docker Hub repository

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

2. Alibaba Cloud image registry (for networks in mainland China)

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

#### Use Docker volumes to store data

1. Docker Hub repository

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

2. Alibaba Cloud image registry (for networks in mainland China)

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

> On older Docker versions you may encounter the error `ls: cannot access '/usr/local/jpom-server/lib/': Operation not permitted`; in that case add the `--privileged` flag, for example:
>
> ```shell
> docker run -d --name jpom-server --privileged -p 2122:2122 jpomdocker/jpom
> ```
>
> For installing Docker, configuring registries, auto-start, and locating the installation directory, see the [Docker container installation guide](https://jpom.top/pages/practice/c846d3/).

<a name="install-download"></a>
### Option 3: 💾 Download the installation package

1. Download the corresponding `server-x.x.x-release` and `agent-x.x.x-release` packages (tar.gz; 2.11.x transitional releases also provide zip) and their `.sha1` checksum files from the [download center](https://jpom.top/pages/all-downloads/).
2. Verify the integrity of the packages (recommended), for example `sha1sum server-x.x.x-release.tar.gz`, and compare the result with the contents of the downloaded `.sha1` file.
3. Extract the packages and upload the entire directories to the target servers.
4. Install the agent
   1. The `agent-x.x.x-release` directory contains all the agent installation files
   2. Start the agent: use the bat script on Windows and the sh script on Linux (if characters are garbled or the script cannot run, check whether the encoding and line endings match)
   3. Default agent port: `2123`
5. Install the server
   1. The `server-x.x.x-release` directory contains all the server installation files
   2. Start the server: use the bat script on Windows and the sh script on Linux (if characters are garbled or the script cannot run, check whether the encoding and line endings match)
   3. Default server port: `2122`; management page: `http://127.0.0.1:2122/` (when accessing from another machine, replace `127.0.0.1` with the server IP)

<a name="install-build"></a>
### Option 4: ⌨️ Build from source

**Requirements**: JDK 8, Maven 3.6+, Node.js 18.

> ⚠️ The build order must not be reversed: build the frontend first, then package the backend; otherwise the management page will return 404 or display stale pages.

1. Pull the latest complete code from the [Jpom repository](https://gitee.com/dromara/Jpom) (the `master` branch is recommended).
2. Build the frontend: switch to the `web-vue` directory, run `npm install`, then run `npm run build` (the output goes to `modules/server/src/main/resources/dist`).
3. Build the backend: return to the project root directory and run `mvn clean package`.
4. Install the agent: the package is located at `modules/agent/target/agent-x.x.x-release`; upload the entire directory to the server and start it; the default port is `2123`.
5. Install the server: the package is located at `modules/server/target/server-x.x.x-release`; upload the entire directory to the server and start it; the default port is `2122`.

> You can also use `script/release.bat` or `script/release.sh` for quick packaging.

<a name="install-compose"></a>
### Option 5: 🧩 One-click start with Docker Compose

Prerequisites: Git, Docker, and Docker Compose are installed. This method pulls the code on the local machine and builds and starts the services automatically.

> Before starting, modify the `SERVER_TOKEN` and other settings in the `.env` file; never use the example token in production.

```shell
# Install git (use yum on CentOS; use apt-get install -y git on Ubuntu/Debian)
yum install -y git
git clone https://gitee.com/dromara/Jpom.git
cd Jpom

# Docker Compose v2
docker compose -f docker-compose.yml up -d
# Docker Compose v1 (older versions)
docker-compose -f docker-compose.yml up -d
```

> The repository also provides the `docker-compose-local.yml` (local build) and `docker-compose-cluster.yml` (cluster) examples for use as needed.
>
> To rebuild the images, run `docker compose -f docker-compose.yml up -d --build`, or `docker compose -f docker-compose.yml build --no-cache`.

<a name="install-dev"></a>
### Option 6: 💻 Build and run locally (development and debugging)

1. Pull the latest complete code from the [Jpom repository](https://gitee.com/dromara/Jpom) (the `master` branch is recommended; use the `dev` branch to try new features).
2. Build the frontend: switch to the `web-vue` directory and run `npm install`, then `npm run build`.
3. Run the server: start `org.dromara.jpom.JpomServerApplication`; the default port is `2122`.
4. Run the agent: start `org.dromara.jpom.JpomAgentApplication`; note the default username and password printed in the console; the default port is `2123`.
5. Frontend development mode: switch to the `web-vue` directory and run `npm run dev`, then visit `http://127.0.0.1:3000/` (when accessing from another machine, replace `127.0.0.1` with the server IP).

<a name="commands"></a>
## 🎛️ Manage Jpom services

The `bin/` directory of the installation package provides management scripts; the Server and the Agent each use their own scripts.

1. On Windows, use the bat script files

```bat
:: Server management script (command line)
:: Usage: Server.bat [start|stop|restart|status]
./bin/Server.bat start
./bin/Server.bat stop
./bin/Server.bat restart
./bin/Server.bat status

:: Server management script (control panel); enter operations as prompted
./bin/Server.bat

:: Agent management script (command line)
:: Usage: Agent.bat [start|stop|restart|status]
./bin/Agent.bat start
./bin/Agent.bat stop
./bin/Agent.bat restart
./bin/Agent.bat status

:: Agent management script (control panel); enter operations as prompted
./bin/Agent.bat
```

> After starting on Windows, follow the logs to track the startup status; if characters are garbled, check or change the encoding. `GB2312` is the recommended encoding for bat files.

2. On Linux, use the sh script files

```bash
# Server
./bin/Server.sh start     # Start the Jpom server
./bin/Server.sh stop      # Stop the Jpom server
./bin/Server.sh restart   # Restart the Jpom server
./bin/Server.sh status    # Check the Jpom server status
./bin/Service.sh install  # Create an application service for the Jpom server (jpom-server)

# Agent
./bin/Agent.sh start      # Start the Jpom agent
./bin/Agent.sh stop       # Stop the Jpom agent
./bin/Agent.sh restart    # Restart the Jpom agent
./bin/Agent.sh status     # Check the Jpom agent status
./bin/Service.sh install  # Create an application service for the Jpom agent (jpom-agent)
```

<a name="systemd"></a>
## 🐧 Manage Jpom as a Linux system service

> The service installation method below is for reference only; customize the configuration as needed.
>
> After running `./bin/Service.sh install` in the Server or Agent installation directory (root or sudo is recommended), the corresponding systemd service (`jpom-server` / `jpom-agent`) is registered.
>
> ```bash
> systemctl {status | start | stop | restart | enable} jpom-server
> systemctl {status | start | stop | restart | enable} jpom-agent
> ```

<a name="config"></a>
## ⚙️ Jpom configuration

The configuration files are located in the `conf/` directory under the program's runtime root path:

### Application configuration `./conf/application.yml`

1. Agent example: [`application.yml`](https://gitee.com/dromara/Jpom/blob/master/modules/agent/src/main/resources/config_default/application.yml)
2. Server example: [`application.yml`](https://gitee.com/dromara/Jpom/blob/master/modules/server/src/main/resources/config_default/application.yml)

### Logging configuration `./conf/logback.xml`

1. Agent example: [`logback.xml`](https://gitee.com/dromara/Jpom/blob/master/modules/agent/src/main/resources/config_default/logback.xml)
2. Server example: [`logback.xml`](https://gitee.com/dromara/Jpom/blob/master/modules/server/src/main/resources/config_default/logback.xml)

> The data directory, ports, database, and other settings can all be configured in `application.yml`; before upgrading, back up the `data/` and `conf/` directories.

<a name="faq"></a>
## 📝 FAQ and practical examples

- [Documentation home](https://jpom.top/)
- [FAQ](https://jpom.top/pages/FQA/)
- [Glossary](https://jpom.top/pages/FQA/proper-noun/)
- [Security advisories](https://jpom.top/pages/security/)
- [Download center](https://jpom.top/pages/all-downloads/)

### Practical examples

> Some images may load slowly.

1. [Local build + SSH deployment for Java projects](https://jpom.top/pages/practice/build-java-ssh-release/)
2. [Local build + project deployment for Node projects](https://jpom.top/pages/practice/build-node-release/)
3. [Local build + SSH deployment for Node projects](https://jpom.top/pages/practice/build-node-ssh-release/)
4. [Local build + custom management for Python projects](https://jpom.top/pages/practice/project-dsl-python/)
5. [Custom management for Java projects](https://jpom.top/pages/practice/project-dsl-java/)
6. [Managing a compiled and installed nginx](https://jpom.top/pages/practice/node-nginx/)
7. [Managing Docker](https://jpom.top/pages/practice/docker-cli/)
8. [Container build + project deployment for Java projects](https://jpom.top/pages/practice/build-docker-java-node-release/)
9. [More practical examples >>](https://jpom.top/pages/practice/)

<a name="examples"></a>
## Example repositories for builds

1. [Jboot example code](https://gitee.com/keepbx/Jpom-demo-case/tree/master/jboot-test)
2. [SpringBoot example code (ClassPath)](https://gitee.com/keepbx/Jpom-demo-case/tree/master/springboot-test)
3. [SpringBoot example code (Jar)](https://gitee.com/keepbx/Jpom-demo-case/tree/master/springboot-test-jar)
4. [Node Vue example code (antdv)](https://gitee.com/keepbx/Jpom-demo-case/tree/master/antdv)
5. [Python example code](https://gitee.com/keepbx/Jpom-demo-case/tree/master/python)

> Compile a specific directory with Node.js:

```bash
yarn --cwd xxxx/ install
yarn --cwd xxxx/ build
```

> Compile a specific directory with Maven:

```bash
mvn -f xxxx/pom.xml clean package
```

<a name="architecture"></a>
## 🛠️ Architecture

![Jpom functional architecture](https://jpom.top/images/jpom-func-arch.png)

<a name="community"></a>
## 🐞 Community discussion, bug reports, and suggestions

1. Join the Jpom WeChat group to discuss and share with us! (When adding the assistant, mention Jpom to join the group.)
2. Open-source projects thrive on community support. If this project has helped you and you would like to buy us a coffee, scan the [WeChat and Alipay donation QR code](https://jpom.top/images/qrcode/praise-all.png), or donate through [Gitee sponsorship](https://gitee.com/dromara/Jpom) (click the donate button at the bottom of the project homepage; WeChat and Alipay are supported). [Donation records](https://jpom.top/pages/praise/publicity/).
3. Buy open-source merchandise: [merchandise introduction](https://jpom.top/pages/shop/).
4. For enterprise technical services, please contact us separately to discuss a service plan.
5. For bug reports and suggestions, feel free to open an [issue](https://gitee.com/dromara/Jpom/issues); developers review and reply from time to time.
6. To contribute, see the [contributing guide](#contributing).

Thank you to everyone who has donated or contributed; your support is what keeps us updating and improving!

<p align="center">
<img src="https://jpom.top/images/qrcode/praise-all.png" alt="Donation QR code">
</p>

<a name="merch"></a>
## 💖 Merchandise

To better sustain the open-source project, we have launched merchandise.

Buying merchandise gives you a small item and also supports the project's continued operation (merchandise prices are slightly higher than market prices; please do not order if you mind).

<p align="center">
<img src="https://jpom.top/images/qrcode/weixin-shop-jpom66.png" alt="Merchandise">
</p>

<a name="contributing"></a>
## 🔨 Contributing guide

> Submitting a contribution is deemed as signing the [CLA](https://gitee.com/dromara/Jpom/blob/master/CLA.md) agreement.

### Contribution guidelines

As an open-source project, Jpom relies on community support and welcomes anyone to make changes and suggestions. Contributions, big or small, help thousands of users and developers, and your contribution will be permanently preserved in the project's contributor list — that is the essence of open-source!

To ensure the quality and standards of the project code and to help you understand the project structure quickly, please read the following before contributing:

- [Contributor Code of Conduct](https://gitee.com/dromara/Jpom/blob/master/CODE_OF_CONDUCT.md)
- [Chinese-English typography specifications](https://gitee.com/dromara/Jpom/blob/master/typography-specification.md)

### Contribution steps (code)

1. Fork this repository.

2. After forking, a repository identical to this one appears under your account. Clone it to your local machine, replacing `username` in the URL with your own Gitee username. Use the `dev` branch for code contributions.

   ```bash
   git clone -b dev https://gitee.com/username/Jpom.git
   ```

3. Modify the code and commit your changes.

   ```bash
   # Add the modified files to the staging area
   git add .
   # Commit to the local repository, describing what you changed
   git commit -m 'Describe what you changed'
   # Push to the remote repository
   git push origin dev
   ```

4. On Gitee, open a Pull Request against the `dev` branch of `dromara/Jpom` and wait for the maintainers to review and merge it.

### Documentation contributions

The project documentation has been separated into the [jpom-docs](https://github.com/bwcx-jzy/jpom-docs) repository (`main` branch), where the documentation site content lives in the `docs/` directory. Please submit documentation changes to that repository, not to the `docs` branch of the main repository.

### Branch description

| Branch | Description |
|--------|-------------|
| master | Stable release branch, protected, does not accept PRs. Merged from the `beta` branch after testing. |
| beta   | Beta pre-release branch, protected, does not accept PRs. Merged from the `dev` branch after testing. |
| dev    | Development branch, accepts PRs. Submit features and fixes to the `dev` branch. |

> Currently, PRs are mainly accepted on the `dev` branch; the `docs` branch has been archived, and documentation should be submitted to the [jpom-docs](https://github.com/bwcx-jzy/jpom-docs) repository.

<a name="contributors"></a>
### Contributors

<a href="https://github.com/dromara/Jpom/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=dromara/Jpom" alt="Contributors">
</a>

Made with [contrib.rocks](https://contrib.rocks).

<a name="planet"></a>
## 🌍 Knowledge Planet

<p align="center">
<img src="https://jpom.top/images/zsxq.jpg" alt="Scan to join Knowledge Planet and learn more">
</p>

<a name="ai-ops"></a>
## 🤖 AI Ops integration (Skill / MCP)

Jpom provides a portable AI ops Skill (`jpom-ops`): it uses the existing REST API and a long-term token to cover queries for workspaces, nodes, projects, builds, scripts, assets, monitoring, distribution, system settings, and audit logs, and supports controlled write operations after explicit authorization (trigger/cancel builds, start/stop/restart projects, kill processes, online upgrades). It works with DeepSeek Harness, Claude, Cursor, and similar tools; it is read-only by default, and write operations require `JPOM_OPS_ALLOW_WRITE=true` plus `--confirm`.

- Source: [`skills/jpom-ops/`](skills/jpom-ops/) (maintained in this repository; verified item by item in 2.11.13 and compatible with 2.11.x)
- Usage and download: see the official documentation "AI Ops Integration (Skill / MCP)" (https://jpom.top/pages/ai-ops-skill-mcp/)

> Note: configure `JPOM-USER-TOKEN` securely in your local environment only, and never commit credentials. WebSocket capabilities such as terminals and online script execution are outside the Skill's scope; an MCP Server is planned.

<a name="recommended"></a>
## 🔔 Recommended projects

| Project | Address | Description |
| --- | --- | --- |
| SpringBoot_v2 | [https://gitee.com/bdj/SpringBoot_v2](https://gitee.com/bdj/SpringBoot_v2) | A clean SpringBoot scaffold |
| TLog GVP project | [https://gitee.com/dromara/TLog](https://gitee.com/dromara/TLog) | A lightweight distributed log tagging and tracing tool that can be integrated in 10 minutes and automatically tags logs for microservice tracing |
| Sa-Token | [https://gitee.com/dromara/sa-token](https://gitee.com/dromara/sa-token) | Possibly the most feature-complete Java authentication and authorization framework ever! |
| Erupt | [https://gitee.com/erupt/erupt](https://gitee.com/erupt/erupt) | Zero frontend code, pure annotation-based admin backend development |
| hippo4j | [https://gitee.com/magegoofy/hippo4j](https://gitee.com/magegoofy/hippo4j) | A powerful dynamic thread pool framework with monitoring and alerting |
| HertzBeat | [https://gitee.com/dromara/hertzbeat](https://gitee.com/dromara/hertzbeat) | An easy-to-use cloud monitoring system with no agent required and powerful custom monitoring capabilities |

<a name="license"></a>
## 📄 License

Jpom is licensed under [Mulan PSL v2](https://license.coscl.org.cn/MulanPSL2); see [LICENSE](https://gitee.com/dromara/Jpom/blob/master/LICENSE) for details.

<a name="thanks"></a>
## 🤝 Acknowledgements

- Thanks to JetBrains for providing a free open-source license:

<p>
<img src="https://jpom.top/images/friends/ad/jetbrains.png" alt="JetBrains" style="float:left;">
</p>
