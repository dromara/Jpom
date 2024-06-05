<p align="center">
	<a href="https://jpom.top/"  target="_blank">
	    <img src="https://jpom.top/images/logo/jpom_logo.svg" width="400" alt="logo">
	</a>
</p>
<p align="center">
	<strong>🚀Simple and lightweight low-invasive online build, automated deployment, daily operations, and project monitoring software.</strong>
</p>
<p align="center">
   【<strong>It is also a native ops software</strong> / <a href="./README.md">中文</a>】
</p>

<p align="center">
	<a target="_blank" href="https://gitee.com/dromara/Jpom">
        <img src='https://gitee.com/dromara/Jpom/badge/star.svg?theme=gvp' alt='gitee star'/>
    </a>
 	<a target="_blank" href="https://github.com/dromara/Jpom">
		<img src="https://img.shields.io/github/stars/dromara/Jpom.svg?style=social" alt="github star"/>
    </a>
    <a target="_blank" href="https://license.coscl.org.cn/MulanPSL2">
		<img src="https://img.shields.io/:license-MulanPSL2-blue.svg" alt="license"/>
	  </a>
    <a target="_blank" href="https://gitee.com/dromara/Jpom">
        <img src='https://img.shields.io/badge/JDK-1.8.0_40+-green.svg' alt='jdk'/>
    </a>
</p>

<p align="center">
    <a target="_blank" href="https://www.codacy.com/gh/dromara/Jpom/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=dromara/Jpom&amp;utm_campaign=Badge_Grade">
      <img src="https://app.codacy.com/project/badge/Grade/843b953f1446449c9a075e44ea778336" alt="codacy"/>
    </a>
  <a target="_blank" href="https://jpom.top/pages/changelog/new/">
		<img src="https://img.shields.io/github/v/release/dromara/Jpom.svg" alt="docker pull"/>
    </a>
    <a target="_blank" href="https://hub.docker.com/repository/docker/jpomdocker/jpom">
		<img src="https://img.shields.io/docker/pulls/jpomdocker/jpom.svg" alt="docker pull"/>
    </a>
</p>

<p align="center">
	👉 <a target="_blank" href="https://jpom.top">https://jpom.top/</a>  👈
</p>

## 😭 Do you experience these pain points in your daily development?

- <font color="red">**No dedicated operations team, so developers have to handle operations tasks**</font>, including manual project building and deployment.
- Different projects require different build and deployment commands.
- Need to package for various environments like development, testing, and production.
- Need to monitor the status of multiple projects simultaneously.
- <u>Need to download SSH tools</u> to remotely connect to servers.
- *Need to download FTP tools* to transfer files to servers.
- Syncing account passwords across multiple servers and computers is inconvenient.
- Want to use automation tools, but they are high-demanding on server performance and too complicated to set up.
- **Have specific needs for automation tools and want to modify the project**, but existing tools are too complex.

> For distributed projects, these steps are even more cumbersome.
>
> Let Jpom help you solve these pain points! And these are just the basic features that Jpom offers.

### 😁 After using [Jpom](https://gitee.com/dromara/Jpom)

- Convenient User Management
  1. User activity monitoring, with email notifications for specific user actions
  2. Multi-user management with independent project permissions (control over upload and delete rights), comprehensive operation logs, and workspace-based permission isolation
  3. Accounts can enable **MFA (Multi-Factor Authentication)** for security
- Real-time interface to view project status, console logs, and manage project files
	1. Edit project text files online
- Docker container management and Docker Swarm cluster management（**Docker UI**）
- **Online SSH Terminal**, allowing easy server management without using PuTTY, Xshell, FinalShell, etc.
	1. No need to know server passwords after logging into the Jpom system
	2. Specify forbidden SSH commands to prevent high-risk operations and automatically log command execution
	3. Set file directories to manage project files and configuration files online
	4. Execute SSH command templates and schedule scripts online
	5. Edit text files online
	6. **Lightweight implementation of basic"bastion host"functionality**
- One-click cluster project deployment using project distribution
- Online build process eliminates the need for manual project updates and upgrades
  1. Supports pulling from GIT and SVN repositories
  2. **Supports container builds (docker)**
	3. Supports SSH-based deployment
  4. Supports scheduled builds
	5. Supports WebHook-triggered builds
- Supports online editing of nginx configuration files and automatic reload operations
	1. Manage nginx status and SSL certificates
- Automatic alerts and restart attempts for abnormal project status
	1. Supports notifications via email, DingTalk groups, and WeChat groups, actively monitoring project status
- Node script templates with scheduling or triggers for expanded functionality
- Configurable authorization for important paths to prevent user errors with system files

### 🔔️ Special Reminders

> 1. On Windows servers, some features may have compatibility issues due to system characteristics. It is recommended to thoroughly test in practical use. Linux currently has good compatibility.
> 2. Install the server and plugin components in different directories; do not install them in the same directory.
> 3. To uninstall Jpom plugin or server components, first stop the corresponding service, then delete the related program files, log folders, and data directories.
> 4. Local builds depend on the system environment. If build commands require Maven or Node.js,
     ensure the corresponding environment is installed on the build server. If the environment is installed after the server is started, restart the server via the command line for the environment to take effect.
> 5. On Ubuntu/Debian servers, the plugin component may fail to add. Please create a .bash_profile file in the root directory of the current user.
> 6. After upgrading to version 2.7.x, downgrading is not recommended due to potential data incompatibility issues.
> 7. Currently, version 2.x.x uses HTTP for communication between the plugin and server components, so ensure network connectivity between them during use.
> 8. Jpom version 3.0 is already being planned. Stay tuned for the new release!

### 🗒️ [Changelog](https://gitee.com/dromara/Jpom/blob/master/CHANGELOG.md)

Must-read before upgrading: [CHANGELOG.md](https://gitee.com/dromara/Jpom/blob/master/CHANGELOG.md)

## 📥 Installing Jpom

Jpom supports various installation methods to meet different user needs. Just choose one method to install.

### Method 1: 🚀(Recommended) One-click Installation (Linux)

#### One-click Server Installation

> **Note: The installation directory is the current directory where the command is executed!**
>
> ⚠️ Special Reminder: When using the one-click installation, ensure the command is executed in different directories. The Server and Agent cannot be installed in the same directory!
>
> To change the data and log storage paths of the server,
> modify the `jpom.path` configuration property in the file
> [`application.yml`](https://gitee.com/dromara/Jpom/blob/master/modules/server/src/main/resources/config_default/application.yml)

```shell
# Default one-click installation
curl -fsSL https://jpom.top/docs/install.sh | bash -s Server jdk+default
# Default one-click installation and automatic startup service configuration
curl -fsSL https://jpom.top/docs/install.sh | bash -s Server jdk+default+service

# Install server and jdk environment
yum install -y wget && \
wget -O install.sh https://jpom.top/docs/install.sh && \
bash install.sh Server jdk

# Install server and jdk, maven environment
yum install -y wget && \
wget -O install.sh https://jpom.top/docs/install.sh && \
bash install.sh Server jdk+mvn

# ubuntu
apt-get install -y wget && \
wget -O install.sh https://jpom.top/docs/install.sh && \
bash install.sh Server jdk
```

After a successful startup, the server port is `2122`. You can access the management page via `http://127.0.0.1:2122/`
(if not accessing from the local machine, replace 127.0.0.1 with the IP address of the installed server).

> If you cannot access the management system, run the command `systemctl status firewalld` to check if the firewall is enabled.
> If you see `Active: active (running)` in green in the status bar, you need to allow port `2122`.
>
>```bash
># Allow port 2122 for the management system
>firewall-cmd --add-port=2122/tcp --permanent
># Reload the firewall to take effect
>firewall-cmd --reload
>```
>
>If you have allowed the port in the operating system but still cannot access it, and you are using a cloud server, check the security group rules in the cloud server's control panel to see if port 2122 is allowed.
>
>⚠️ Note: There are multiple firewalls in Linux systems: Firewall, Iptables, SELinux, etc. When checking firewall configurations, make sure to check all of them.

#### One-Click Agent Installation

> If the server where the server side is installed also needs to be managed, you need to install the agent on the server side as well (both the server and agent can be installed on the same server but in different directories).
>
> ⚠️ Special reminder: Do not execute the one-click installation command in the same directory for both the Server and Agent!
>
> If you need to modify the agent data and log storage paths, update the `jpom.path` configuration property in the file
> [`application.yml`](https://gitee.com/dromara/Jpom/blob/master/modules/agent/src/main/resources/config_default/application.yml)

```shell
# Default one-click installation
curl -fsSL https://jpom.top/docs/install.sh | bash -s Agent jdk+default
# Default one-click installation and auto-configure startup service
curl -fsSL https://jpom.top/docs/install.sh | bash -s Agent jdk+default+service

# Install agent and JDK environment
yum install -y wget && \
wget -O install.sh https://jpom.top/docs/install.sh && \
bash install.sh Agent jdk

# ubuntu
apt-get install -y wget && \
wget -O install.sh https://jpom.top/docs/install.sh && \
bash install.sh Agent jdk
```

After a successful startup, the agent port is `2123`, which is used by the server.

### Method 2: 📦 Container Installation

> ⚠️ Note: Container installation requires Docker to be installed first. [Click here for Docker installation documentation](https://jpom.top/pages/b63dc5/)

#### One-Command Installation

```shell
docker run -p 2122:2122 --name jpom-server jpomdocker/jpom
```

#### Using Mount to Store Data (may have compatibility issues in some environments)

```shell
docker pull jpomdocker/jpom
mkdir -p /home/jpom-server/logs
mkdir -p /home/jpom-server/data
mkdir -p /home/jpom-server/conf
docker run -d -p 2122:2122 \
	--name jpom-server \
	-v /home/jpom-server/logs:/usr/local/jpom-server/logs \
	-v /home/jpom-server/data:/usr/local/jpom-server/data \
	-v /home/jpom-server/conf:/usr/local/jpom-server/conf \
	jpomdocker/jpom
```

#### Using Docker Volumes to Store Data

```shell
docker pull jpomdocker/jpom
docker volume create jpom-server-data
docker volume create jpom-server-logs
docker volume create jpom-server-conf
docker run -d -p 2122:2122 \
	--name jpom-server \
	-v jpom-server-data:/usr/local/jpom-server/data \
	-v jpom-server-logs:/usr/local/jpom-server/logs \
	-v jpom-server-conf:/usr/local/jpom-server/conf \
	jpomdocker/jpom
```

> Container installation only provides the server version. Due to isolation between the container and the host environment, many functionalities of the agent cannot be used properly. Therefore, containerization of the agent is not very meaningful.
>
> For more information on installing Docker, configuring images, auto-start, and locating the installation directory, refer to the documentation
> [https://jpom.top/pages/b63dc5/](https://jpom.top/pages/b63dc5/)
>
> In lower versions of Docker, you may encounter the error `ls: cannot access'/usr/local/jpom-server/lib/': Operation not permitted`
> In this case, add the `--privileged` parameter
> Example: `docker run -p 2122:2122 --name jpom-server jpomdocker/jpom --privileged`

### Method 3: 💾 Download and Install

1. Download the installation package from [https://jpom.top/pages/all-downloads/](https://jpom.top/pages/all-downloads/)
2. Extract the files
3. Install the agent:
	1. The `agent-x.x.x-release` directory contains all the installation files for the agent
	2. Upload the entire directory to the corresponding server
	3. Start the agent. Use the bat script on Windows and the sh script on Linux (if there are garbled characters or execution issues, check the encoding format and line endings)
	4. The default running port for the agent is `2123`
4. Install the server:
	1. The `server-x.x.x-release` directory contains all the installation files for the server
	2. Upload the entire directory to the corresponding server
	3. Start the server. Use the bat script on Windows and the sh script on Linux (if there are garbled characters or execution issues, check the encoding format and line endings)
	4. The default running port for the server is `2122`. Access the management page at `http://127.0.0.1:2122/` (if not accessed locally, replace `127.0.0.1` with your server's IP address)

### Method 4: ⌨️ Compile and Install

1. Visit the [Jpom](https://gitee.com/dromara/Jpom) Gitee page and pull the latest complete code (recommended to use the master branch)
2. Switch to the `web-vue` directory and run `npm install` (you need to have the Vue environment set up in advance; refer to the README.md in the web-vue directory for details)
3. Run `npm run build` to package the Vue project
4. Switch to the project root directory and run: `mvn clean package`
5. Install the agent:
	1. Check the agent installation package in `modules/agent/target/agent-x.x.x-release`
	2. Upload the entire directory to the server
	3. Start the agent. Use the bat script on Windows and the sh script on Linux (if there are garbled characters or execution issues, check the encoding format and line endings)
	4. The default running port for the agent is `2123`
6. Install the server:
	1. Check the server installation package in `modules/server/target/server-x.x.x-release`
	2. Upload the entire directory to the server
	3. Start the server. Use the bat script on Windows and the sh script on Linux (if there are garbled characters or execution issues, check the encoding format and line endings)
	4. The default running port for the server is `2122`. Access the management page at `http://127.0.0.1:2122/` (if not accessed locally, replace `127.0.0.1` with your server's IP address)

> You can also use `script/release.bat` or `script/release.sh` for quick packaging.

### Method 5: 📦 One-Click Start with Docker-Compose

- No environment installation required; automatically compiles and builds

> Note: Remember to modify the token value in the `.env` file

```shell
yum install -y git
git clone https://gitee.com/dromara/Jpom.git
cd Jpom
docker-compose -f docker-compose.yml up
# docker-compose -f docker-compose.yml up --build
# docker-compose -f docker-compose.yml build --no-cache
# docker-compose -f docker-compose-local.yml up
# docker-compose -f docker-compose-local.yml build --build-arg TEMP_VERSION=.0
# docker-compose -f docker-compose-cluster.yml up --build
```

### Method 6: 💻 Compile and Run

1. Visit the [Jpom](https://gitee.com/dromara/Jpom) Gitee page and pull the latest complete code (it's recommended to use the master branch, but if you want to experience new features, you can use the
   dev branch)
2. Run the agent:
	1. Run `org.dromara.jpom.JpomAgentApplication`
	2. Note the default username and password information printed in the console.
	3. The agent's default running port: `2123`
3. Run the server:
	1. Run `org.dromara.jpom.JpomServerApplication`
	2. The server's default running port: `2122`
4. Build the Vue page, switch to the `web-vue` directory (make sure you have node and npm environments set up locally).
5. Install the Vue project dependencies by executing `npm install` in the console.
6. Start development mode by executing `npm run dev` in the console.
7. Access the frontend page using the address output in the console: `http://127.0.0.1:3000/` (if not accessing from the local machine, replace `127.0.0.1` with your server's IP address).

## Managing Jpom Commands

1. Using BAT Script Files on Windows

```bash
# Server management scripts (command line)
./bin/Server.bat start   # Start the Jpom server
./bin/Server.bat stop    # Stop the Jpom server
./bin/Server.bat restart # Restart the Jpom server
./bin/Server.bat status  # Check the Jpom server status
# Server management script (control panel), follow the panel prompts for operations
./bin/Server.bat

# Agent management scripts
./bin/Agent.bat start   # Start the Jpom agent
./bin/Agent.bat stop    # Stop the Jpom agent
./bin/Agent.bat restart # Restart the Jpom agent
./bin/Agent.bat status  # Check the Jpom agent status
# Agent management script (control panel), follow the panel prompts for operations
./bin/Agent.bat

```

> After executing the startup script on Windows, follow the logs to check the startup status. If you encounter garbled text, check or modify the encoding format. It is recommended to use
> `GB2312` for BAT script encoding on Windows.

2. Using SH Script Files on Linux

```bash
# Server management scripts
./bin/Server.sh start     # Start the Jpom server
./bin/Server.sh stop      # Stop the Jpom server
./bin/Server.sh restart   # Restart the Jpom server
./bin/Server.sh status    # Check the Jpom server status
./bin/Service.sh install  # Create a service for the Jpom server (jpom-server)

# Agent management scripts
./bin/Agent.sh start     # Start the Jpom agent
./bin/Agent.sh stop      # Stop the Jpom agent
./bin/Agent.sh restart   # Restart the Jpom agent
./bin/Agent.sh status    # Check the Jpom agent status
./bin/Service.sh install # Create a service for the Jpom agent (jpom-agent)
```

## Linux Service Management

> The following service installation instructions are for reference only; customize configurations as needed.
>
> After successfully using `./bin/Service.sh install`:
>
> systemctl {status | start | stop | restart} jpom-server
>
> systemctl {status | start | stop | restart} jpom-agent

## ⚙️ Jpom Configuration Parameters

Located in the project's root path:

### Application Configuration `./conf/application.yml`

1. Agent example:
   [`application.yml`](https://gitee.com/dromara/Jpom/blob/master/modules/agent/src/main/resources/config_default/application.yml)
2. Server example:
   [`application.yml`](https://gitee.com/dromara/Jpom/blob/master/modules/server/src/main/resources/config_default/application.yml)

### Project Logs `./conf/logback.xml`

1. Agent example:
   [`logback.xml`](https://gitee.com/dromara/Jpom/blob/master/modules/agent/src/main/resources/config_default/logback.xml)
2. Server example:
   [`logback.xml`](https://gitee.com/dromara/Jpom/blob/master/modules/server/src/main/resources/config_default/logback.xml)

## 📝 Frequently Asked Questions and User Guide

- [Home Page](https://jpom.top/)
- [FQA](https://jpom.top/pages/FQA/)
- [Glossary](https://jpom.top/pages/FQA/proper-noun/)

### Practical Examples

> Some images may load slowly.

1. [Local Build + SSH Deployment for Java Projects](https://jpom.top/pages/practice/build-java-ssh-release/)
2. [Local Build + Project Deployment for Node Projects](https://jpom.top/pages/practice/build-node-release/)
3. [Local Build + SSH Deployment for Node Projects](https://jpom.top/pages/practice/build-node-ssh-release/)
4. [Local Build + Custom Management for Python Projects](https://jpom.top/pages/practice/project-dsl-python/)
5. [Custom Management for Java Projects](https://jpom.top/pages/practice/project-dsl-java/)
6. [Managing Compiled and Installed Nginx](https://jpom.top/pages/practice/node-nginx/)
7. [Managing Docker](https://jpom.top/pages/practice/docker-cli/)
8. [Container Build + Project Deployment for Java Projects](https://jpom.top/pages/practice/build-docker-java-node-release/)
9. [More Practical Examples>>](https://jpom.top/pages/practice/)

## Example Code Repositories

1. [Jboot Example Code](https://gitee.com/keepbx/Jpom-demo-case/tree/master/jboot-test)
2. [SpringBoot Example Code (ClassPath)](https://gitee.com/keepbx/Jpom-demo-case/tree/master/springboot-test)
3. [SpringBoot Example Code (Jar)](https://gitee.com/keepbx/Jpom-demo-case/tree/master/springboot-test-jar)
4. [Node Vue Example Code (antdv)](https://gitee.com/keepbx/Jpom-demo-case/tree/master/antdv)
5. [Python Example Code](https://gitee.com/keepbx/Jpom-demo-case/tree/master/python)

> Node.js compile specific directory:

```bash
yarn --cwd xxxx/ install
yarn --cwd xxxx/ build
```

> Maven compile specific directory:

```bash
mvn -f xxxx/pom.xml clean package
```

## 🛠️ Overall Architecture

![jpom-func-arch](https://jpom.top/images/jpom-func-arch.png)


## 🐞 Community Discussion, Bug Reports, Suggestions, etc.

1. Scan the QR code below on the left to join our WeChat group! (Add the assistant and mention Jpom to join the group)
2. Open source projects rely on community support. If Jpom has helped you, consider buying us a coffee.
   You can scan the [WeChat and Alipay QR codes](https://jpom.top/images/qrcode/praise-all.png)
   or support us through [Gitee sponsorship](https://gitee.com/dromara/Jpom)
   (click the donate button at the bottom of the project homepage, supports WeChat and Alipay). [Sponsorship records](https://jpom.top/pages/praise/publicity/)
3. Purchase open source merchandise: [Merchandise Introduction](https://jpom.top/pages/shop/)
4. For enterprise technical services, please contact us directly to discuss service plans.
5. For bug reports and suggestions, feel free to create [issues](https://gitee.com/dromara/Jpom/issues); developers will respond periodically.
6. To contribute, please see the [Contribution Guide](#Contribution Guide)。

Thank you to all sponsors and contributors; your support is our motivation for continuous updates and improvements!
<p align="center">
<img src="https://jpom.top/images/qrcode/praise-all.png" style="" alt="praise img">
</p>

## 💖 Merchandise

To better support the open-source project, we have decided to launch merchandise.

By purchasing, you get a small item, and we receive the profit from your purchase (the prices of the merchandise may be slightly higher than market prices; please be aware before ordering).

<p align="center">
<img src="https://jpom.top/images/qrcode/weixin-shop-jpom66.png" style="" alt="shop home">
</p>

## 🔨Contribution Guide

> By contributing, you agree to the terms of the [CLA](https://gitee.com/dromara/Jpom/blob/master/CLA.md) agreement

### Contribution Guidelines

As an open-source project, Jpom relies on community support and welcomes contributions from everyone. Whether big or small, your contributions will help thousands of users and developers. Your contributions will also be permanently recorded in the list of contributors, which is the essence of open-source projects!

To ensure code quality and standards, and to help you quickly understand the project structure, please read the following before contributing:

- [Jpom Contribution Guide](https://gitee.com/dromara/Jpom/blob/master/CODE_OF_CONDUCT.md)
- [Typography Specifications (Chinese and English)](https://gitee.com/dromara/Jpom/blob/dev/typography-specification.md)

### Contribution Steps

1. Fork this repository.

2. Clone your forked repository to your local machine.

   Replace `branch-name` and `username` with the appropriate values.

   Use `dev` for code contributions and `docs` for documentation contributions.

   ```bash
   git clone -b branch-name https://gitee.com/username/Jpom.git
   ```

3. Modify the code/documentation and commit your changes.

   ```bash
   # Add your changes to the staging area
   git add .
   # Commit your changes with a descriptive message
   git commit -m 'Describe your changes'
   # Push to your remote repository, replacing branch-name with dev or docs
   git push origin branch-name
   ```

4. Create a Pull Request (PR).

   Go to your repository on Gitee, create a PR request, and wait for the administrators to merge your changes.

### Branch Explanation

| Branch     | Description                                                   |
|--------|------------------------------------------------------|
| master | Main branch, protected. Does not accept PRs. Merges from the beta branch after testing.       |
| beta   | 	Beta version branch, protected. Does not accept PRs. Merges from the dev branch after testing. |
| dev    | Development branch, accepts PRs. Please submit PRs to the dev branch.                           |
| docs   | Documentation branch, accepts PRs. Used for project documentation, feature introductions, and FAQ summaries.                         |

> Primarily use the dev and docs branches for PR submissions. Other branches are for archiving and can be ignored by contributors.

## 🌍 Knowledge Planet

<p align="center">
<img src="https://jpom.top/images/zsxq.jpg" alt="Scan to join Knowledge Planet and learn more">
</p>

## 🔔 Recommended Projects

| Project Name          | Project Link                                                                       | Description                                          |
|---------------|----------------------------------------------------------------------------|-----------------------------------------------|
| SpringBoot_v2 | [https://gitee.com/bdj/SpringBoot_v2](https://gitee.com/bdj/SpringBoot_v2) | 	A pure SpringBoot scaffold                          |
| TLog GVP Project   | [https://gitee.com/dromara/TLog](https://gitee.com/dromara/TLog)           | A lightweight distributed log tagging and tracking tool for microservices |
| Sa-Token      | [https://gitee.com/dromara/sa-token](https://gitee.com/dromara/sa-token)   | Possibly the most feature-rich Java authentication framework                      |
| Erupt         | [https://gitee.com/erupt/erupt](https://gitee.com/erupt/erupt)             | Zero frontend code, pure annotation-based admin backend development                        |
| hippo4j       | [https://gitee.com/magegoofy/hippo4j](https://gitee.com/magegoofy/hippo4j) | Powerful dynamic thread pool framework with monitoring and alert features                          |
| HertzBeat     | [https://gitee.com/dromara/hertzbeat](https://gitee.com/dromara/hertzbeat) | Easy-to-use cloud monitoring system with strong custom monitoring capability              |

## 🤝 Acknowledgements

- Special thanks to JetBrains for providing a free open-source license:

<p>
<img src="https://jpom.top/images/friends/ad/jetbrains.png" alt="JetBrains" style="float:left;">
</p>
