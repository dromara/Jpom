# AGENTS.md

> 本文件面向 AI 编码助手（也适合人类贡献者），说明 Jpom 项目的**仓库/子模块结构**、**分支用法**、**发版流程**与开发约定。
>
> 适用范围：本工作区根目录（Jpom 仓库 `gitmodules` 分支）。文中 `jpom-parent/`、`docs/` 等路径均相对工作区根目录。

## 0. 先读：项目状态与硬性约束

- **上游已暂停维护**：Jpom 官方于 2026-02 发布公告暂停维护（<https://jpom.top/pages/project-suspended/>），公告明确仍欢迎社区提交 PR，但常规发版节奏已停止。
- 本工作区采用 **Git submodule 聚合布局**：根目录是同一 Jpom 仓库的 `gitmodules` 分支，只保留子模块指针与少量工具脚本；**真正的代码在 `jpom-parent/` 子模块中**。
- AI 助手硬性约束：
  1. 开发以 `jpom-parent` 为准，基线分支是 **`dev`**；外部贡献的 PR 只能提到 `dev`，不要向 `master` / `beta` 提 PR。
  2. 子模块内的修改先在子模块仓库提交，再回根仓更新子模块指针；不要在根仓“顺手”提交子模块内的文件、也不要混用两个仓库的提交。
  3. **没有明确授权时，不要执行任何发版动作**：不要上传制品到 R2（下载中心已全面使用 Cloudflare R2，阿里云 OSS 已停用）、不要推送 Docker 镜像、不要打 tag、不要创建 Gitee Release、不要发布文档站。第 6 节流程仅用于理解与授权后执行。
  4. 不要提交构建产物与本地工具文件：`target/`、`dist/`、`*.bak`、`pom.xml.versionsBackup`、`*.zip`、`*.tar.gz`、`*.sha1`、`.tmp/`、`awscliv2.zip`、`ossutil-*/`、`node_modules/`，以及 `*.pem`、`*.key` 等密钥/证书文件。
  5. **每次提交、推送前都必须做敏感信息检查**（开源仓库）：不得提交真实密码、Token、密钥、证书、内网地址和用户隐私数据。提交前运行 `bash script/check-sensitive.sh --all`，推送前加上 `--range <基线>..HEAD`，详见第 8 节。
  6. **GitHub 是 Gitee 的自动镜像，只读**：主仓库在 Gitee（`gitee.com/dromara/Jpom`），Gitee 收到推送后会自动镜像到 GitHub（`github.com/dromara/Jpom`）。**代码提交、推送、PR、tag、Release 一律只对 Gitee 操作；禁止直接向 GitHub 提交、推送、提 PR 或做任何写操作。**
  7. **文档与临时文件分离（`.tmp` 治理）**：`.tmp/` 只放**可随时删除**的产物（构建/依赖缓存、下载的二进制、PoC 复现环境、大体积证据、一次性脚本）；**不要把唯一一份文档或结论只放在 `.tmp/`**。需要留存的文档（审计/分析/验证记录、环境信息、操作手册）写入工作区内的独立文档库 `dev-docs/`（独立 git 仓库，不提交到本仓，规则见 `dev-docs/README.md`）；任务收尾时把 `.tmp/` 中值得保留的 md 归档过去，详见第 9 节。

## 1. 项目概览

Jpom 是一款“简而轻的低侵入式在线构建、自动部署、日常运维、项目监控”软件，由**服务端 Server** 与**插件端 Agent** 组成。

| 项 | 说明 |
| --- | --- |
| 后端 | Java 8（`java.version=1.8`），Spring Boot 2.7.18，Maven 多模块 |
| 前端 | Vue 3 + Vite 5 + TypeScript + Ant Design Vue 4 + Pinia，目录 `jpom-parent/web-vue` |
| 数据库 | 默认内置 H2；支持 MySQL / MariaDB / PostgreSQL / 达梦（Dameng） |
| 默认端口 | Server `2122`，Agent `2123`，前端 dev server `3000`（代理到 `2122`） |
| 许可证 | Mulan PSL v2（新增源文件需带版权头，见第 7 节） |
| 构建产物 | `modules/server/target/server-<version>-release.{zip,tar.gz}`、`modules/agent/target/agent-<version>-release.{zip,tar.gz}`（均带 `.sha1`） |

`jpom-parent/modules` 模块：

| 模块 | 作用 |
| --- | --- |
| `server` | 服务端：Web UI/API、构建、SSH/FTP、Docker、监控、在线升级等 |
| `agent` | 插件端：执行项目、脚本、文件、监控等任务 |
| `common` | 公共代码、常量、工具、i18n 资源 |
| `agent-transport` | 服务端与插件端通信（`agent-transport-common`、`agent-transport-http`） |
| `storage-module` | 存储层，按数据库拆分子模块（h2 / mysql / mariadb / postgresql / dameng） |
| `sub-plugin` | 功能插件：git-clone、svn-clone、docker-cli、email、webhook、ssh-jsch、encrypt 等 |

## 2. 工作区结构与子模块

| 路径 | 对应仓库 / 分支 | 作用 |
| --- | --- | --- |
| `.`（根目录） | Jpom 仓库 `gitmodules` 分支 | 子模块聚合：`.gitmodules`、`RELEASE.md`、根 `pom.xml`、`script/` 发布脚本 |
| `jpom-parent/` | Jpom 仓库（`.gitmodules` 默认 `master`，开发实际用 `dev`） | 全部源码、构建/发版脚本、Gitee Go 与 GitHub Actions 配置 |
| `docs/` | `bwcx-jzy/jpom-docs` 仓库 `main` 分支 | 官网/文档站（VuePress），由 GitHub Pages 发布 |
| `download-link/` | Jpom 仓库 `download_link` 分支 | JDK 下载镜像清单（`jdk.rb`），非业务代码 |
| `script/`（根） | `gitmodules` 分支 | 发布安装包到 Cloudflare R2（`deploy-release-r2.sh`）、同步下载链接、敏感信息检查等维护脚本 |
| `.run/` | `gitmodules` 分支 | IDEA 本地运行配置（`JpomServerApplication` / `JpomAgentApplication`） |

子模块常用命令（在根目录执行）：

```bash
git submodule status                  # 查看子模块当前 commit 与分支状态
git submodule update --init --recursive   # 首次拉取/初始化子模块
git -C jpom-parent checkout dev       # 代码开发切到 dev（推荐）
git -C docs checkout main
```

注意事项：

- `.gitmodules` 中 `jpom-parent` 的 `branch = master`，**不要**在开发时盲目执行 `git submodule update --remote`（会把子模块切到 master）。需要更新指针时，先确认子模块在预期分支，再在根仓 `git add jpom-parent` 提交指针。
- 根仓 `HEAD` 记录的只是一组子模块 commit，**不代表子模块当前工作区分支/改动**；联调前先用 `git submodule status` 与各子模块 `git status` 确认。
- 根仓 `master` 分支是完整代码快照（历史默认分支），本工作区使用的是 `gitmodules` 分支；两者内容不同，不要交叉提交。

### 远程与镜像（重要）

| 远程 | 地址 | 用途 |
| --- | --- | --- |
| Gitee（`origin`） | `gitee.com/dromara/Jpom` | **主仓库，唯一的代码推送目标** |
| GitHub | `github.com/dromara/Jpom` | Gitee 自动镜像的**只读**仓库，不要 push/PR/tag/Release |
| `jpom-docs`（docs 子模块） | `github.com/bwcx-jzy/jpom-docs` | 独立的文档站仓库，按 3.2 的文档流程操作（不属于 Jpom 主仓镜像） |

- `git push` 只推 Gitee（如 `git push origin dev` / `git push gitee dev`，远程名以本地配置为准）；**不要** `git push github ...`。
- 不要在 GitHub 上提 PR / Issue / Release / tag；GitHub 上的内容会在 Gitee 推送后由镜像自动同步。
- 历史脚本 `jpom-parent/script/push.bat` 会向 gitee、github 双远程推送，**已不符合当前流程**，不要使用其中的 GitHub 推送部分。

## 3. 分支体系与用法

### 3.1 代码仓库 `jpom-parent`（核心）

分支模型以 `jpom-parent/README.md` 的“项目分支说明”为准（其中 `docs` 分支为历史说明，文档现在已独立为 `jpom-docs` 仓库）：

| 分支 | 类型 | 接受 PR | 用法与说明 |
| --- | --- | --- | --- |
| `dev` | 开发主线 | ✅ 是 | **所有功能/修复都先进入 `dev`**；PR 目标分支；保持随时可编译 |
| `beta` | beta 预发布 | ❌ 否（受保护） | 由 `dev` 合并而来，用于发布 `x.y.z.N` beta 包；测试通过后合并到 `master` |
| `master` | 稳定发布 | ❌ 否（受保护） | 只接受经过测试的 `beta`（或 `dev`）合并；稳定版构建、打 `v2.x.y` tag 都在此分支 |
| `2.6.x` | 归档线 | ❌ 否 | 旧 2.6.x 版本维护线，已停止（最后提交 2021-09），仅历史参考 |
| `main` | 归档 | ❌ 否 | 历史发布分支（最后提交 `commit release 2.11.1`），归档 |
| `docs-docsify` / `backup-thymeleaf` / `stand-alone` / `i18n` / `pipeline` / `dev-ftp1` / `dev_netty` / `dev_script_library` | 归档/一次性特性分支 | ❌ 否 | 历史特性实验或迁移分支，**不要作为开发基线** |
| `docs`（历史） | 文档 | — | 文档已拆分为独立 `jpom-docs` 仓库（`docs/` 子模块 `main` 分支） |

推荐流程：

```text
功能分支/贡献者 fork ──PR──> dev ──测试后合并──> beta ──测试后合并──> master ──tag v2.x.y──> 发布
```

- `RELEASE.md`「手动版」第 1 步写的是“将 dev 分支合并到 master”，即允许跳过 beta 直接发稳定版；实际以维护者当次选择为准。
- 远程操作只有 Gitee：提交、推送、tag、PR、Release 都不要直接对 GitHub 做（GitHub 由 Gitee 自动镜像），详见第 2 节“远程与镜像”。
- 功能分支从 `dev` 切出，命名可参考历史（`dev-ftp1`、`i18n`、`pipeline` 等），合并回 `dev` 后删除。
- 贡献者典型操作流：

  ```bash
  # fork 后克隆 dev 分支
  git clone -b dev https://gitee.com/<你的用户名>/Jpom.git
  cd Jpom
  git remote add upstream https://gitee.com/dromara/Jpom.git
  git fetch upstream && git checkout -b fix/xxx upstream/dev
  # 修改代码并提交（见第 7 节规范）
  git push origin fix/xxx
  # 在 Gitee 向 dromara/Jpom 的 dev 分支发起 PR
  ```

- 提交 PR 即视为签署 [CLA.md](jpom-parent/CLA.md)；请同时遵守 `CODE_OF_CONDUCT.md`。
- **主仓在 Gitee（`gitee.com/dromara/Jpom`），GitHub（`github.com/dromara/Jpom`）是 Gitee 自动镜像的只读仓库**：fork、分支、PR 全在 Gitee 上操作，PR 目标为 Gitee 的 `dev` 分支；不要向 GitHub 提 PR、推送代码、创建 tag 或 Release。
- 历史脚本 `jpom-parent/script/push.bat` 会向 gitee、github 双远程推送并双向合并 `dev`/`master`，**已过时**；当前只需要推送 Gitee，GitHub 由镜像自动同步。

### 3.2 文档仓库 `docs/`（jpom-docs）

| 分支 | 用途 |
| --- | --- |
| `main` | 文档源文件，接受 PR；文档站内容在 `docs/docs/` 下 |
| `dist` | GitHub Actions 自动构建产物分支，**不要手动提交**；GitHub Pages 从该分支发布 |

- 发布链路：push `main` → `.github/workflows/deploy.yml` 构建（`npm install && npm run lbuild`）→ 推送 `dist` 分支 → GitHub Pages 发布（自定义域名以 `docs/docs/.vuepress/public/CNAME` 为准，当前为 `jpom.top`）。
- 文档站发布统一走 GitHub Pages；历史 OSS 回退工作流（`ali-oss.yml`）及配套 `Refresh.py`、`cdn-urllist.txt` 已随阿里云 OSS 停用一并移除，`baiduPush.yml` 为每日百度收录推送。
- 本地预览：`cd docs && npm install && npm run ldev`（默认端口 `2216`）。
- **例外说明**：`docs` 是独立的 GitHub 仓库（`bwcx-jzy/jpom-docs`），其 `main`/`dist` 与 GitHub Pages 流程不受“主仓只在 Gitee 操作”的限制；但**不要**把 Jpom 主仓代码提交到这个仓库。

### 3.3 其他分支

- `download_link`：提供 JDK 下载清单（`download-link/` 子模块），由 `update-jdk.yml` 手动触发（或推送 `error-understand` 分支）更新并发布。
- `gitmodules`：本工作区的聚合分支（子模块指针 + 发版工具），不包含业务代码。

## 4. 版本号、产物与发布渠道

### 4.1 版本号规则

| 类型 | 格式 | 示例 | 说明 |
| --- | --- | --- | --- |
| 稳定版 | `2.x.y` | `2.11.12` | 每次发版 patch +1；打 tag `v2.x.y` |
| beta 版 | `2.x.y.N` | `2.11.12.1` | 以**当前已发布稳定版号**为前缀 + 递增序号；**beta 不打 tag** |
| 下个稳定版 | `2.x.(y+1)` | `2.11.13` | beta 测稳后的稳定版号（历史：beta `2.11.11.1~4` → release `2.11.12`） |

版本号记录位置（由替换脚本统一维护）：

| 文件 | 作用 |
| --- | --- |
| `jpom-parent/script/tag.release.txt` | 当前稳定版号（替换脚本据此找旧版本） |
| `jpom-parent/script/tag.beta.txt` | 当前 beta 版号 |
| 各 `pom.xml` | Maven 工程版本（`mvn versions:set` 统一替换） |
| `jpom-parent/.env`、`env-release.env`、`env-beta.env` | Docker Compose 使用的版本 |
| `jpom-parent/web-vue/package.json` | 前端版本 |
| `jpom-parent/modules/common/src/main/resources/banner.txt` | 启动 banner 版本 |
| `docs/docs/.vuepress/public/docs/versions.release.tag`、`versions.beta.tag` | 文档站版本 |

### 4.2 构建产物

| 产物 | 路径 |
| --- | --- |
| 服务端整包 | `jpom-parent/modules/server/target/server-<version>-release.{zip,tar.gz}` + `.sha1` |
| 插件端整包 | `jpom-parent/modules/agent/target/agent-<version>-release.{zip,tar.gz}` + `.sha1` |
| 服务端/插件端合并包 | 由根目录 `script/deploy-release-r2.sh` 生成 `jpom-<version>.zip` 并上传 R2 |
| 运行目录（解压态） | `modules/server/target/server-<version>-release/`（含 `bin/`、`conf/`、`lib/`） |

### 4.3 发布渠道

| 渠道 | 说明 |
| --- | --- |
| 下载中心 | `https://d2.jpom.download`（Cloudflare R2 托管）；稳定版路径 `release/<version>/`，beta 路径 `beta/<version>/` |
| 上传工具 | 根目录 `script/deploy-release-r2.sh`（rclone remote `r2`，bucket `jpom-releases-apac`） |
| 历史 OSS | 已停用（阿里云 AK 已禁用）；旧上传脚本 `deploy-release-oss.sh` 已删除，仅 git 历史可查 |
| Docker Hub | `jpomdocker/jpom:<version>` 与 `:latest`（稳定版）；beta 镜像按需构建 |
| Gitee Release | 基于 tag `v2.x.y` 创建发行版，附安装包与 sha1；GitHub 由 Gitee 自动镜像，不手动操作 |

Dockerfile 对应关系：`modules/server/DockerfileRelease`（稳定版）、`DockerfileReleaseJdk17`（JDK17 变体）、`DockerfileBeta`、`DockerfileBetaJdk17`；插件端 `modules/agent/DockerfileRelease`、`DockerfileBeta`。

## 5. 构建与本地运行

### 5.1 环境要求

- JDK **8**（工程 `source/target=1.8`，与 CI 一致；更高 JDK 可能因老插件/编译参数出问题）
- Maven **3.6+**（可用 `-s script/settings.xml` 走阿里云镜像）
- Node.js **18**（CI 使用 18.x / 18.19.0）
- 前端依赖安装：CI 与发版脚本使用 `npm install`；`web-vue/README.md` 推荐本地用 pnpm 8，但**发版打包请统一用 npm**，避免产物差异

### 5.2 常用命令

```bash
# 1) 构建前端（必须先执行；产物直接写入服务端 resources）
cd jpom-parent/web-vue
npm install
npm run build          # 输出到 modules/server/src/main/resources/dist

# 2) 构建后端（会生成 server/agent 的 release zip/tar.gz/sha1）
cd ../
mvn clean package
# CI 等价写法（跳过测试）：
mvn -B -e clean package -Dmaven.test.skip=true -Dmaven.compile.fork=true -s script/settings.xml

# 3) 本地开发
cd web-vue && npm run dev        # http://127.0.0.1:3000 ，代理到 2122
# 服务端：运行 org.dromara.jpom.JpomServerApplication（端口 2122）
# 插件端：运行 org.dromara.jpom.JpomAgentApplication（端口 2123）

# 4) 代码规范/辅助
cd jpom-parent && mvn license:format   # 为源码补充/修正 Mulan PSL v2 版权头
cd jpom-parent/web-vue && npm run lint && npm run i18n   # 前端 lint / 生成多语言
```

要点：

- **构建顺序不能反**：前端 `npm run build` 的产物进入 `modules/server/src/main/resources/dist`，`mvn package` 时打进服务端包。只跑 Maven 会导致页面 404 或旧页面。
- 测试现状：各 `pom.xml` 默认 `skipTests=true`，CI 使用 `-Dmaven.test.skip=true`，没有自动化测试门禁；`src/test` 下多为手工/工具类测试。**发版前必须人工冒烟**：服务端与插件端包可正常启动、登录、核心功能可用、在线升级可用。
- 数据库改动需在 `storage-module` 各数据库子模块同步实现，至少保证 H2 与 MySQL 兼容，不要只改 H2。
- 前端 dev 代理配置在 `web-vue/.env.dev`（`JPOM_PROXY_HOST=127.0.0.1:2122`）。

## 6. 发版流程（核心）

### 6.1 前置说明

- 项目已暂停维护，正式发版前必须确认：下载域名（R2：`d2.jpom.download`）与 rclone remote 凭据、Docker Hub、文档 Pages、Gitee 凭据均可用；**无授权不得执行**。GitHub 由 Gitee 自动镜像，不需要也不应单独准备 GitHub 推送凭据。
- 发版提交固定节奏（与历史提交一致）：
  1. `releases(发布): 准备<version>` —— 更新 CHANGELOG；
  2. 运行版本替换脚本 + 打包 + 冒烟；
  3. `commit release <version>` / `commit beta <version>` —— 版本号提交。
- 开始前：各子模块在预期分支、`git status` 干净、无遗留 `*.bak`。
- **优先使用一键脚本 `script/release.sh`（见 6.6）**：它已包含合并、CHANGELOG 日期、版本替换、构建、产物校验、启动冒烟、推送、R2、docs 与可选 Docker；本文手动步骤保留用于排障与理解流程。

### 6.2 稳定版发版（手动版，按 `RELEASE.md` 整理）

1. **合并代码**：将 `dev` 合并到 `beta` 测试；beta 稳定后合并到 `master`（或按 `RELEASE.md` 直接将 `dev` 合并到 `master`）。`master`/`beta` 不接受 PR。
2. **更新 CHANGELOG**：`CHANGELOG-BETA.md` 对应版本补充发布日期；`CHANGELOG.md` 新增 `## 2.x.y-release (YYYY-MM-DD)` 段落（内容参考对应 beta 版本整理）。提交：`git commit -m "releases(发布): 准备release 2.x.y"`。
3. **替换版本号**：

   ```bash
   cd jpom-parent/script
   sh replaceVersion.sh 2.x.y release
   ```

   脚本会基于 `script/tag.release.txt` 的旧版本号，替换各 `pom.xml`、Dockerfile、`.env`、`env-release.env`、`web-vue/package.json`、`.workflow/MasterPipeline.yml`、`banner.txt`，并写回 `tag.release.txt`。产生的 `*.bak`、`pom.xml.versionsBackup` 不要提交。
4. **打包前端**：`cd jpom-parent/web-vue && npm i && npm run build`。
5. **打包后端**：`cd jpom-parent && mvn clean package`，确认 `modules/{server,agent}/target/*-release.zip|tar.gz|sha1` 已生成。
6. **人工冒烟**：解压 server/agent 的 release 包，分别用 `bin/Server.sh start`、`bin/Agent.sh start`（Windows 用 `.bat`）启动，确认可启动、可登录、核心功能正常。
7. **提交并打 tag**：

   ```bash
   cd jpom-parent
   git add -A && git commit -m "commit release 2.x.y"
   git tag v2.x.y
   git push origin master && git push origin --tags   # 只推 Gitee
   ```

   （`origin` 为 Gitee；**GitHub 由 Gitee 自动镜像同步，禁止 `git push github`**；历史脚本 `script/push.bat` 已过时，不要使用。）
8. **上传安装包到 R2**：

   ```bash
   cd <工作区根目录>
   bash script/deploy-release-r2.sh release    # 可先加 --dry-run 预览
   ```

   脚本使用 `rclone`（remote `r2`）上传到 `r2:jpom-releases-apac/release/<version>/`（agent/server 的 zip、tar.gz、sha1 及合并包 `jpom-<version>.zip`），随后自动通过 `https://d2.jpom.download` 回验 sha1。注意：
   - 版本号取自 `jpom-parent/script/tag.release.txt`，无需再改脚本；
   - 脚本可用 `R2_REMOTE` / `R2_BUCKET` / `R2_PUBLIC_BASE` 环境变量覆盖默认值；
   - 阿里云 OSS 已停用，不要再尝试 `ossutil` 上传；
   - Cloudflare 对 d2 域名的 GET 响应实测缓存约 8 天（`max-age=691200`），同一版本重复覆盖上传会触发脚本的 `--force` 保护，覆盖后需在 Cloudflare 控制台 Purge 对应 URL；
9. **发布文档**：

   ```bash
   cd docs/script
   sh replaceVersion.sh 2.x.y release
   # 手动把 CHANGELOG 的 release 段落同步到 docs/日志&下载/01.更新日志/01.最新.md
   cd docs && npm install && npm run lbuild   # 本地验证构建
   ```

   发布方式：提交并 push `main` 分支，由 GitHub Actions（`.github/workflows/deploy.yml`）构建并推送 `dist` 分支，GitHub Pages 自动更新。历史 OSS 同步脚本（`deploy-docs-pages.sh`、`docs/.github/workflows/ali-oss.yml`）已删除。
10. **测试远程更新**：在旧版本服务端上测试“在线升级”到新版本，确认下载与校验正常。
11. **发布 Docker 镜像**（在 `jpom-parent` 下执行，需先启用 buildx，见 `jpom-parent/script/docker-buildx.md`）：

    ```bash
    docker buildx build --platform linux/amd64,linux/arm64 \
      -t jpomdocker/jpom:2.x.y -t jpomdocker/jpom:latest \
      -f ./modules/server/DockerfileRelease --push .
    ```

12. **创建发行版**：在 **Gitee** 基于 tag `v2.x.y` 创建 Release，上传 server/agent 安装包与 sha1。**不要到 GitHub 手动创建 Release/tag**，GitHub 内容由 Gitee 镜像自动同步。
13. **可选**：发布发版公告。

### 6.3 beta 发版

1. 将 `dev` 合并到 `beta`（或从 `dev` 切出 beta 发版线）。
2. 更新 `CHANGELOG-BETA.md`：新增 `## 2.x.y.N-beta (YYYY-MM-DD)` 段落，提交 `releases(发布): 准备 2.x.y.N`。
3. 替换版本号：`cd jpom-parent/script && sh replaceVersion.sh 2.x.y.N beta`（只改 beta 相关文件：`DockerfileBeta*`、pom、`env-beta.env`、banner、`tag.beta.txt`）。
4. 前端、后端打包（同 6.2 第 4、5 步）。
5. 人工冒烟测试。
6. 提交 `commit beta 2.x.y.N`。
7. 上传到 beta 渠道：`bash script/deploy-release-r2.sh beta`（版本号取自 `jpom-parent/script/tag.beta.txt`，上传到 `r2:jpom-releases-apac/beta/<version>/` 并自动回验）。
8. 可选构建 beta 镜像（`-f ./modules/server/DockerfileBeta`）。
9. **beta 不打 tag、不创建 Gitee Release（GitHub 镜像无需操作）**；测稳后按 6.2 合并到 `master` 发稳定版。

### 6.4 流水线发版（Gitee Go）

| 配置 | 触发 | 作用 |
| --- | --- | --- |
| `jpom-parent/.workflow/MasterPipeline.yml` | push `master`/`dev`，或手动 | Node 18 + `npm install && npm run build` + `mvn -B -e clean package -Dmaven.test.skip=true -Dmaven.compile.fork=true -s script/settings.xml`；产出 `all_zip`/`server_zip`/`agent_zip`，再由“合并打包”发布 `jpom-<version>` 制品 |
| `jpom-parent/.workflow/docker-hub.yml` | push `dev` / tag 前缀 `v`，或手动 | 构建并推送服务端多平台镜像（示例版本号需手动更新） |
| `jpom-parent/.github/workflows/build.yml` | push / PR 到 `master` | GitHub Actions 构建校验（Node 18 + JDK 8 + `mvn -B package`） |
| `jpom-parent/.github/workflows/codeql-analysis.yml` | push / PR 到 `master` + 每周计划 | CodeQL 静态扫描 |
| `jpom-parent/.github/workflows/update-jdk.yml` | 手动，或推送 `error-understand` 分支 | 检出 `download_link` 分支并更新 JDK 下载清单 |

流水线发版注意：`MasterPipeline.yml` 内的版本号（artifact 路径、构建命令）是硬编码的，发版时需同步修改；`RELEASE.md` 特别提示“注意修改构建命令版本号”。`replaceVersion.sh` 的 release 模式会顺带替换该文件，但仍要复核。

### 6.5 发版检查清单

- [ ] 目标分支正确（稳定版 `master` / beta `beta`），工作区干净
- [ ] CHANGELOG（release 与 beta）已更新并带日期
- [ ] 版本替换脚本执行成功，`git diff` 无遗漏、无 `*.bak`
- [ ] 前端已 build（`modules/server/src/main/resources/dist` 已更新）
- [ ] `mvn clean package` 成功，server/agent 的 zip、tar.gz、sha1 齐全
- [ ] 安装包人工启动/登录/核心功能冒烟通过
- [ ] 提交 `commit release|beta <version>`，稳定版打 `v2.x.y` tag 并推送
- [ ] 安装包已上传 R2（release 或 beta 目录），`https://d2.jpom.download/...` 的 sha1 可校验
- [ ] 文档站版本号、CHANGELOG、下载链接已更新并发布
- [ ] 在线升级测试通过（稳定版）
- [ ] Docker 镜像已推送（稳定版：`<version>` 与 `latest`）
- [ ] Gitee Release 已创建（GitHub 由镜像自动同步，不手动操作）
- [ ] 已确认得到维护者授权，且所有正式渠道操作符合预期

### 6.6 一键发版脚本（推荐）

根目录 `script/release.sh` 把 6.2 / 6.3 的机械步骤串成一条命令，适用于 beta 与 stable：

```bash
# beta：自动计算下一个 beta（如 2.11.12.1 -> 2.11.12.2）
bash script/release.sh beta

# stable：自动计算下一个 patch（如 2.11.12 -> 2.11.13）
bash script/release.sh release

# 只预检并打印计划，不修改任何文件
bash script/release.sh beta --dry-run

# 只做本地准备（合并/日期/版本/构建/校验/冒烟/本地提交），不推送、不上传、不改文档
bash script/release.sh beta --to commit

# 失败后从指定阶段续跑（阶段：preflight/merge/changelog/version/build/verify/smoke/commit/push/upload/docs/docker/finish）
bash script/release.sh beta --version 2.11.12.2 --from upload
```

脚本行为与安全约定：

- 版本号默认由 `script/tag.*.txt` 自动计算；beta 要求源分支的 `CHANGELOG-BETA.md` 已有对应段落（日期可自动补），release 要求目标分支的 `CHANGELOG.md` 已有 `x.y.z-release` 段落（内容需人工整理）。
- 默认依次执行：合并源分支 → 补日期 → `replaceVersion.sh` → 前端 `npm install && npm run build` → `mvn clean package` → 产物校验（sha1/zip/class 非空/class 与 `target/classes` 一致/前端 dist）→ 服务端与插件端启动冒烟 → 提交 → 推送 Gitee → R2 上传 → docs 更新 →（可选）Docker。
- push / R2 / docs / Docker 前默认交互确认，非交互环境必须显式 `--yes`；`--dry-run` 只预检；`--no-push` / `--no-upload` / `--no-docs` 可关闭阶段（组合受保护）。
- beta 不打 tag、不创建 Gitee Release；release 打 `vX.Y.Z` tag。只推 Gitee；docs 子模块按 3.2 推 GitHub Pages。
- 本机 Maven 曾出现偶发坏包（0 字节 class、jar 内旧 class），脚本默认 `--build-retries 1` 自动重跑构建并重新校验；日志在 `.tmp/release/`。
- 脚本需要 JDK、Maven、Node/npm、python3、unzip、curl、rclone（上传时）；可用 `--jdk-home`、`--mvn`、`--node`、`--npm`、`--npm-cache` 覆盖检测结果。

## 7. 提交规范与代码风格

### 7.1 提交信息

采用 **Conventional Commits + 中文简述**（与仓库历史一致）：

- 格式：`type(scope): 中文说明`
- 常用 type：`feat`、`fix`、`refactor`、`docs`、`build`、`ci`、`style`、`perf`、`chore`、`releases`（发版专用）
- 常用 scope：`server`、`agent`、`web-vue`、`all`、`i18n`、`storage`、`system`、`h2db`、`docs`、`build`、`ci` 等
- 示例：`feat(server): 新增构建后发布到 FTP`、`fix(web-vue): 修复表格自定义列不生效问题`、`docs: 更新 CHANGELOG-BETA.md 文件`
- 发版固定提交：`releases(发布): 准备release 2.x.y` / `releases(发布): 准备 2.x.y.N`、`commit release 2.x.y` / `commit beta 2.x.y.N`
- 历史提交中也有 gitmoji 风格（见根目录 `README.md` 的 emoji 表），但当前以 Conventional Commits 为主。

### 7.2 代码风格

- 以 `.editorconfig` 为准：默认 UTF-8、LF、Tab 缩进；`*.java`/`*.xml`/`*.sql` 4 空格；`*.vue`/`*.js`/`*.ts`/`*.json`/`*.yml` 2 空格；`*.bat` CRLF。
- 换行符：代码仓库根 `.gitattributes`（`jpom-parent/.gitattributes`）声明 `* text=auto`、`*.sh eol=lf`、`*.bat eol=crlf`；建议 `git config --global core.autocrlf false`。
- 版权头：新建源文件需带 Mulan PSL v2 头（模板见 `HEADER.txt`）；`license-maven-plugin` 在 `generate-sources` 阶段强制执行，可运行 `mvn license:format` 自动补全。
- 前端：ESLint + Prettier（`web-vue/.eslintrc.json`、`.prettierrc.json`），提交前建议 `npm run lint:fix`。
- 国际化：
  - 后端：`modules/common/src/main/resources/i18n/messages_{zh_CN,zh_HK,zh_TW,en_US}.properties` 与 `words.json`；
  - 前端：`web-vue/src/i18n/locales/zh_cn.json`（主文件）与 `en_us.json`（同步文件），新增文案后用 `npm run i18n` 生成（依赖 `.env` 中的百度翻译配置，特殊字符转义见 `web-vue/README.md` 的 `_#_` 说明）。
- 新增/修改 UI 文案必须同时考虑中英（及繁体）语言资源，避免硬编码中文。

## 8. 开源项目：隐私与密钥泄露检查（提交/推送必做）

> Jpom 是公开开源项目，提交与推送的内容会永久进入公共 git 历史。**密钥一旦进入历史就视为已泄露**，删除文件或 revert 都无法真正清除。

### 8.1 红线：禁止提交的内容

- **真实凭据**：密码、私钥（`id_rsa`、`*.pem`、`*.p12`、`*.jks`、`*.keystore` 等）、API Key / Token、云厂商 AK/SK、数据库账号、SSH 口令、CI / 镜像仓库 / rclone 凭据。
- **真实配置**：`.env` 真实值、生产/测试数据库连接串、内网 IP、内部域名、VPN 配置。
- **用户隐私**：真实用户名、手机号、邮箱、身份证/银行卡号、地址、订单、聊天记录、真实数据库导出。
- **未脱敏的日志/截图/抓包**：可能包含 Cookie、JWT、Authorization、会话 ID、用户数据。
- **提交身份**：`git config user.name` / `user.email` 会永久公开在提交历史中；请使用你愿意公开的身份，开源项目建议使用 GitHub/Gitee 的 noreply 邮箱，避免暴露私人邮箱或真名。
- **本地文件**：`.claude/settings.local.json`、`.tmp/`、`aws/`、`ossutil-*`、本机脚本与配置（已由 `.gitignore` 忽略，禁止用 `git add -f` 绕过）。

### 8.2 仓库里的示例占位值（不要替换成真实值）

- `jpom-parent/.env`、`env-release.env`、`env-beta.env` 的 `SERVER_TOKEN` 是 docker-compose 演示值；不要把生产 token 填进去。
- `jpom-parent/script/settings.xml` 的 `proxyuser/proxypass`、`repouser/repopwd` 是 Maven 模板占位。
- `jpom-parent/web-vue/.env` 的 `BAIDU_FY_ID/SECRET` 是 `xxx` 占位。
- 新增配置项时示例值必须写成 `xxx` / `your_xxx` / `change_me`，真实值用环境变量或本地未跟踪文件注入。

### 8.3 提交前检查（每次 `git commit` 前）

```bash
git status --short          # 确认没有把无关文件/密钥文件加进来
git diff --cached           # 逐行审阅即将提交的内容
git config user.name; git config user.email   # 确认提交身份是愿意公开的（见 8.1）
bash script/check-sensitive.sh --all   # 检查根仓 + 全部子模块的暂存区与未跟踪文件
```

- 告警必须逐条人工确认；确认误报也不要直接忽略，改用假数据或在脚本 `ALLOW_PATTERN` 中精确加入该占位值。
- 新增 `*.pem` / `*.key` / `*.jks` / `.env.local` 等密钥类文件默认会被阻断；确需提交公开证书时先与维护者确认并脱敏。
- 可选（能力更强，未安装可跳过）：`gitleaks detect --source . --redact`、`trufflehog git file://. --only-verified`。
- 脚本只做启发式检查（暂存区新增行、未跟踪文件、指定提交区间），不能替代人工审阅；已提交历史请用 gitleaks 等工具扫描。

### 8.4 推送前检查（每次 `git push` / 发版前）

```bash
# 检查 jpom-parent 即将推送的提交（基线/远程名按实际调整）
bash script/check-sensitive.sh --repo jpom-parent --range origin/dev..HEAD

# 直接审阅提交内容
git -C jpom-parent log -p origin/dev..HEAD

# 根仓推送前（子模块指针 + AGENTS.md 等）
bash script/check-sensitive.sh --all --range origin/dev..HEAD
```

- Issue / PR / Release 描述、附件、截图中同样不得包含真实凭据与隐私信息。
- 推送目标只能是 **Gitee**；GitHub 会在 Gitee 推送后由镜像自动同步，**不要**手动 `git push github`、提 GitHub PR 或创建 GitHub tag/Release。
- 不要用 `git push --force` 掩盖已泄露提交（reflog、镜像、缓存仍可能恢复）；正确做法是轮换凭据（见 8.6）。

### 8.5 测试与示例数据

- 单元测试/示例统一用假数据（如 `Poc123456`、`example.com`、`127.0.0.1`），不要拷贝真实用户数据。
- 日志/截图需脱敏：IP、域名、账号、token、Cookie、数据库名等。
- 需要真实样本复现问题时，放到仓库外的临时目录（如 `~/jpom-work/`），不要留在 `.tmp/` 里。

### 8.6 发现泄露后的处理

1. **按“已泄露”处理**：立即轮换/吊销对应密码、Token、API Key、SSH 授权，不要只删文件或 revert。
2. 通知维护者，评估是否影响已发布版本与用户。
3. 由维护者用 `git filter-repo`（或 BFG）清理历史，并协调强推与镜像（Gitee/GitHub）同步；操作前先备份。
4. 按 `jpom-parent/SECURITY.md` 的流程披露与说明修复措施。

## 9. 常见坑

- **子模块分支错位**：`.gitmodules` 里 `jpom-parent` 默认 `master`，开发应在 `dev`；`git submodule update --remote` 会切走，操作前先确认。
- **根仓 `pom.xml` 过期**（版本 2.9.10，仅用于 IDE 聚合导入）：实际构建请在 `jpom-parent/` 内执行 Maven。
- **版本替换脚本**：`replaceVersion.sh` 必须传两个参数（新版本号 + `release`/`beta`），旧版本读自 `script/tag.*.txt`；脚本使用 GNU `sed -i.bak`，macOS 可能需要 `gsed` 或手动适配；执行后检查是否残留 `.bak`。
- **R2 上传脚本**：`script/deploy-release-r2.sh` 依赖本机 `rclone`（remote `r2`、bucket `jpom-releases-apac`），版本号取自 `jpom-parent/script/tag.*.txt`；支持 `--dry-run`（缺产物时非 0 退出）、`--deep-verify`（下载制品逐字节校验）、`--force`（同版本覆盖）与 `--skip-verify`；本机上行带宽慢时建议在服务器执行或用 `nohup` 后台跑。
- **Cloudflare 缓存**：`d2.jpom.download` 的 GET 响应实测带 `cache-control: max-age=691200`（约 8 天，`cf-cache-status` 会变 HIT），同一版本重复覆盖上传后需在 Cloudflare 控制台 Purge，否则用户可能下到旧包；上传脚本的回验只能证明 R2 对象正确，不能证明边缘缓存已刷新。
- **文档站发布**：只走 GitHub Pages（push `docs` 的 `main`）；历史 `deploy-docs-pages.sh` 与 `ali-oss.yml` 已删除。
- **根目录 `README.md` 的子模块说明可能过时**：以 `.gitmodules` 实际配置为准（`docs` 已指向 `bwcx-jzy/jpom-docs` 的 `main`）。
- **不要提交**工作区根部的本地工具/临时文件：`.tmp/`、`awscliv2.zip`、`ossutil-2.3.0-linux-amd64/`、`aws/` 等。
- **`.tmp/` 不是文档仓库**：`.tmp/` 只用于可丢弃的临时产物；需要留存的文档、环境信息和操作手册写 `dev-docs/`（第 0 节第 7 条），任务结束前归档并提交，避免 `.tmp` 被清理后文档丢失。
- **敏感信息检查**：提交前运行 `bash script/check-sensitive.sh --all`，推送前加上 `--range <基线>..HEAD`；`.gitignore` 已忽略密钥类文件与本地临时文件，禁止用 `git add -f` 绕过（详见第 8 节）。
- **GitHub 只读**：主仓在 Gitee，GitHub 是 Gitee 自动镜像；不要直接向 GitHub 提交/推送/开 PR、不要配置 GitHub 推送 remote，`push.bat` 里的 GitHub 推送部分已过时。
- **构建 JDK**：请使用 JDK 8；更高版本 JDK 可能因老版本 maven 插件/编译参数失败。
- **Docker 构建**：`buildx` 需先启用 experimental / binfmt_misc，参见 `jpom-parent/script/docker-buildx.md`。
- **项目已暂停维护**：不要在未授权情况下更新下载中心、镜像、文档站或发布公告；对外发布前与维护者确认。

## 10. 相关权威文件

修改流程或结构后，请同步更新本文件与以下来源：

- `RELEASE.md`（根目录）：发布流程（一键版 / 手动版 / 流水线版）
- `script/release.sh`：一键发版编排脚本（合并、版本、构建、校验、冒烟、推送、R2、docs、Docker）
- `script/check-sensitive.sh`：提交/推送前的隐私与密钥检查脚本（第 8 节）
- `script/deploy-release-r2.sh`：安装包上传 Cloudflare R2（rclone）的发布脚本
- `jpom-parent/SECURITY.md`：安全漏洞报告与披露流程
- `.gitignore`（根目录与 `jpom-parent/`）：临时文件、密钥/证书类文件忽略规则
- `jpom-parent/README.md`：分支说明、贡献指南
- `jpom-parent/script/`：`replaceVersion.sh`、`release.sh`、`release-sha1sum.sh`、`push.bat`、`docker-buildx.md`
- `jpom-parent/.workflow/`、`jpom-parent/.github/workflows/`：CI/流水线
- `docs/README.md`、`docs/.github/workflows/deploy.yml`：文档站与部署
- `.gitmodules`：子模块清单与默认分支
- `dev-docs/README.md`：本地工作区文档库（文档写入规则、环境登记、历史归档；独立 git 仓库，不随本仓提交）
