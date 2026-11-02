# 发布流程

> ⚠️ 主仓库是 Gitee（`gitee.com/dromara/Jpom`），GitHub（`github.com/dromara/Jpom`）由 Gitee 自动镜像，**只读**。
> 所有提交、推送、tag、Release 只对 Gitee 操作，**请勿直接推送到 GitHub**。
> 更完整、已校正的流程见 `AGENTS.md` 第 6 节。
>
> 📦 安装包统一上传到 **Cloudflare R2**（下载中心 `https://d2.jpom.download`）。阿里云 OSS 已全面停用，
> 上传使用根目录 `script/deploy-release-r2.sh`（依赖本机已配置好的 `rclone` remote `r2`）。

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
