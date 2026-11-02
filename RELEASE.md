# 发布流程

> ⚠️ 主仓库是 Gitee（`gitee.com/dromara/Jpom`），GitHub（`github.com/dromara/Jpom`）由 Gitee 自动镜像，**只读**。
> 所有提交、推送、tag、Release 只对 Gitee 操作，**请勿直接推送到 GitHub**。
> 更完整、已校正的流程见 `AGENTS.md` 第 6 节。
>
> 📦 安装包统一上传到 **Cloudflare R2**（下载中心 `https://d2.jpom.download`）。阿里云 OSS 已全面停用，
> 上传使用根目录 `script/deploy-release-r2.sh`（依赖本机已配置好的 `rclone` remote `r2`）。

## 一键版（推荐）

根目录 `script/release.sh` 串联：合并分支 → CHANGELOG 日期 → 版本替换 → 前端/后端构建
→ 产物完整性校验 → 服务端/插件端启动冒烟 → 提交/推送 Gitee → R2 上传 → docs 更新 →（可选）Docker。

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

# 断点续跑（示例：推送成功后从 R2 上传继续）
bash script/release.sh beta --version 2.11.12.2 --from upload
```

安全约定：

- 不带 `--yes` 时，push / R2 上传 / docs 发布 / Docker 推送前逐项交互确认；非交互环境必须显式 `--yes`。
- `--dry-run` 只做预检并打印计划；`--no-push` / `--no-upload` / `--no-docs` 可关闭对应阶段（组合受保护）。
- beta 要求 `dev` 的 `CHANGELOG-BETA.md` 已有对应版本段落（日期可由脚本补）；release 要求 `master` 的 `CHANGELOG.md` 已有 `x.y.z-release` 段落（release 日志需人工整理，脚本不自动生成）。
- 只推送 Gitee；docs 子模块仍按既有流程推 GitHub Pages。beta 不打 tag、不创建 Gitee Release。
- 日志在 `.tmp/release/`；失败后按日志提示用 `--from <阶段>` 续跑。

## 手动版

1. 将 dev 分支合并到 master
2. changelog 版本增加发版日期
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
14. 发布服务端 docker 镜像包 `docker buildx build --platform linux/amd64,linux/arm64 -t jpomdocker/jpom:2.x.x -t jpomdocker/jpom:latest -f ./modules/server/DockerfileRelease --push .`
15. 在 Gitee 添加发行版（GitHub 发行版由镜像同步，不手动操作）
16. 可选填写发版公告

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
10. 执行-发布镜像
11. 在 Gitee 添加发行版（GitHub 发行版由镜像同步，不手动操作）
12. 可选填写发版公告
