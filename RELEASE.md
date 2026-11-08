# 发布流程

> ⚠️ 主仓库是 Gitee（`gitee.com/dromara/Jpom`），GitHub（`github.com/dromara/Jpom`）由 Gitee 自动镜像（**代码/分支/tag 只读**）。
> 所有提交、推送、tag 只对 Gitee 操作，**请勿直接推送到 GitHub**。
> **例外：GitHub Release 不会被镜像同步**，必须在 Gitee Release 创建后手动创建（推荐 `script/release-post.sh` 自动完成）。
> 更完整、已校正的流程见 `AGENTS.md` 第 6 节；耗时与优化见 `dev-docs/runbooks/release-flow-timeline-and-optimization.md`。
>
> 📦 安装包统一上传到 **Cloudflare R2**（下载中心 `https://d2.jpom.download`）。阿里云 OSS 已全面停用，
> 上传使用根目录 `script/deploy-release-r2.sh`（依赖本机已配置好的 `rclone` remote `r2`）。

## 一键版（推荐）

根目录 `script/release.sh` 串联：合并分支 → CHANGELOG 日期 → 版本替换 → 前端/后端构建
→ 产物完整性校验 → 服务端/插件端启动冒烟 → 提交/推送 Gitee → R2 上传 → docs 更新
→（可选）Docker（默认触发 GitHub Actions `docker-publish.yml` 并等待+回验）→
（可选）`--with-post` 发布后置（Gitee/GitHub Release + 微信群通知）。

```bash
cd <工作区根目录>

# beta：按 tag 自动计算下一个版本（当前 2.11.12.1 → 2.11.12.2）
bash script/release.sh beta

# 只预检并打印计划，不修改任何文件
bash script/release.sh beta --dry-run

# 只做本地准备：合并 + 日期 + 版本号 + 构建 + 校验 + 冒烟 + 本地提交
# （不推送、不上传 R2、不改文档，适合先本地验收）
bash script/release.sh beta --to commit

# 稳定版：按 tag.release 自动计算下一个 patch（2.11.12 → 2.11.13）
bash script/release.sh release --yes

# 稳定版一条命令含 Docker（CI 发布+回验）与发布后置（Gitee/GitHub Release + 微信群通知）
bash script/release.sh release --yes --with-post --with-docker

# 断点续跑（示例：推送成功后从 R2 上传继续）
bash script/release.sh beta --version 2.11.12.2 --from upload
```

安全约定：

- 不带 `--yes` 时，push / R2 上传 / docs 发布 / Docker 推送前逐项交互确认；非交互环境必须显式 `--yes`。
- `--dry-run` 只做预检并打印计划；`--no-push` / `--no-upload` / `--no-docs` 可关闭对应阶段（组合受保护）。
- beta 要求 `dev` 的 `CHANGELOG-BETA.md` 已有对应版本段落（日期可由脚本补）；release 要求 `master` 的 `CHANGELOG.md` 已有 `x.y.z-release` 段落（release 日志需人工整理，脚本不自动生成）。
- 只推送 Gitee；docs 子模块仍按既有流程推 GitHub Pages。beta 不打 tag、不创建 Release。
- Docker 镜像默认由 GitHub Actions `docker-publish.yml` 发布（`--with-docker` 触发并等待+回验；`--docker-local` 本机构建推送兜底，仍从 d2 下载发布包）；CI 下载发布包默认走 R2 r2.dev 公开域名（d2 对 GitHub runner 有 Cloudflare managed challenge），可用仓库变量 `JPOM_DOWNLOAD_BASE` 覆盖；`workflow_dispatch` 首次运行限制/当前可用状态见 `AGENTS.md` 6.2 第 11 步（本仓库 `dev` 自检运行后已可 dispatch）。
- `--with-post` 在推送/R2/docs 完成后执行 `script/release-post.sh`（幂等）：创建 Gitee Release 并上传安装包、创建 GitHub Release（Latest）、发送 7 个微信群通知（已存在/已送达则跳过）。
- 日志在 `.tmp/release/`；失败后按日志提示用 `--from <阶段>` 续跑。
- **发版后仍需人工在 `dev` 补下一个 beta 版本日志段**（`CHANGELOG-BETA.md` 顶部 `## <新稳定版>.1-beta` 或 `<当前稳定版>.<N+1>-beta`，提交 `releases(发布): 开始 ...-beta 开发`）；`release.sh` 暂未自动化这一步，详见 `AGENTS.md` 6.2/6.3。

## 手动版

1. 将 dev 分支合并到 master
2. changelog 版本增加发版日期；**若含用户可见变更（权限、接口参数/错误码、字段、操作流程），补充 `### ⚠️ 升级说明` 并同步文档站更新日志**
3. 更新代码版本号：`cd ./jpom-parent/script && sh replaceVersion.sh 2.x.x release`
4. 打包前端：`cd ./jpom-parent/web-vue && npm i && npm run build`
5. 打包后端：`cd ./jpom-parent && mvn clean package`
6. 人工测试服务端包和插件端包是否可以正常启动
7. 提交一个 commit 2.x.x
8. 基于 commit 新增 tag: v2.x.x 并且将代码推送到远程（只推 Gitee，GitHub 由镜像自动同步）
9. 将包上传到 R2：`bash script/deploy-release-r2.sh release`（在仓库根目录执行；可加 `--dry-run` 先预览）
10. 更新文档版本号：`cd ./docs/script && sh replaceVersion.sh 2.x.x release`
11. 手动 copy 更新文档 changelog
12. 发布文档：push docs 仓库 `main`，由 GitHub Actions 构建并发布到 GitHub Pages（详见 AGENTS.md 第 6.2 节）
13. 测试远程更新是否正常
14. 发布服务端/插件端 Docker 镜像（默认走 GitHub Actions）：推 tag `v2.x.x` 后 `docker-publish.yml` 自动触发（CI 从 R2 公开下载基址下载发布包构建：默认 r2.dev，`JPOM_DOWNLOAD_BASE` 可覆盖——`d2.jpom.download` 对 GitHub runner 有 Cloudflare managed challenge；工作流会等待 R2 就绪，务必先完成第 9 步 R2 上传）。推荐 `bash script/release.sh release --with-docker` 触发并等待回验；也可在 Actions 页面手动 `workflow_dispatch`（`channel=release`、`version=2.x.x`、`images=both`）。验证：run 成功 + `docker buildx imagetools inspect jpomdocker/jpom:2.x.x` 平台为 amd64+arm64、Docker Hub API digest 与 run 一致。本机兜底：`bash script/release.sh release --docker-local`；国内网络排障见 `dev-docs/runbooks/docker-hub-push-cn.md`，CI 操作与失败排查见 `dev-docs/runbooks/docker-publish-ci.md`。
15. 在 **Gitee** 基于 tag 创建发行版，上传 server/agent 的 zip/tar.gz 与 sha1
16. **创建 GitHub Release**（Gitee 镜像不同步 Release；用 `bash script/release-post.sh release --version 2.x.x --github-only --yes`）
17. **微信群发版通知**（`bash script/release-post.sh release --version 2.x.x --notify-only --yes`，或直接用 `jpom-release-notify` skill；默认不 @所有人、不发朋友圈）
18. 归档记录到 `dev-docs/archive/<YYYY-MM>-release-<version>/`（含 `notification-<version>.md`）
19. **发版后立即在 `dev` 补下一个 beta 版本日志段**：`CHANGELOG-BETA.md` 顶部新增 `## <新稳定版>.1-beta`（空“新增功能/解决BUG”小节），给刚发版的 beta 段补日期，提交 `releases(发布): 开始 <新稳定版>.1-beta 开发`。

> beta 版上传：`bash script/deploy-release-r2.sh beta`（版本号取自 `jpom-parent/script/tag.beta.txt`）。

## 流水线版

1. 将 dev 分支合并到 master
2. 人工打包并测试相关功能
3. changelog 版本增加发版日期
4. 执行-发布代码任务 (注意修改构建命令版本号)
5. 基于 commit 新增 tag: v2.x.x 并且将代码推送到远程（只推 Gitee）
6. 下载构建产物后上传到 R2：`bash script/deploy-release-r2.sh release`（在仓库根目录执行）
7. 手动 copy 更新文档 changelog
8. 执行-发布文档 (注意修改构建命令版本号)
9. 测试远程更新是否正常
10. 发布镜像：推 tag 后由 GitHub Actions `docker-publish.yml` 自动发布（或手动 dispatch；详见 `AGENTS.md` 6.2 第 11 步）
11. 在 Gitee 创建发行版并上传安装包
12. 创建 GitHub Release（`bash script/release-post.sh release --version 2.x.x --github-only --yes`）
13. 微信群发版通知（`bash script/release-post.sh release --version 2.x.x --notify-only --yes`）
14. 可选填写发版公告（已发群通知则无需重复）
