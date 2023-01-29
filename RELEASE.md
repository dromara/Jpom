# 发布流程

## 手动版

1. 将 dev 分支合并到 master
2. changelog 版本增加发版日期
3. 更新代码版本号：`cd ./jpom-parent/script && sh replaceVersion.sh 2.x.x`
4. 打包前端：`npm i && npm run build`
5. 打包后端：`mvn clean package`
6. 人工测试服务端包和插件端包是否可以正常启动
7. 提交一个 commit 2.x.x
8. 基于 commit 新增 tag: v2.x.x 并且将代码推送到远程
9. 将包上传到 oss : ` sh deploy-release-oss.sh`
10. 更新文档版本号：`cd ./docs/script && sh replaceVersion.sh 2.x.x`
11. 手动 copy 更新文档 changelog
12. 发布文档：`sh deploy-docs-pages.sh`
13. 测试远程更新是否正常
14. 发布服务端 docker 镜像包 `docker buildx build --platform linux/amd64,linux/arm64 -t jpomdocker/jpom:2.x.x -t jpomdocker/jpom:latest -f ./modules/server/DockerfileRelease --push .`
15. 添加 gitee、github 发行版
16. 可选填写发版公告

## 流水线版

1. 将 dev 分支合并到 master
2. 人工打包并测试相关功能
3. changelog 版本增加发版日期
4. 执行-发布代码任务 (注意修改构建命令版本号)
5. 基于 commit 新增 tag: v2.x.x 并且将代码推送到远程
6. 手动 copy 更新文档 changelog
7. 执行-发布文档 (注意修改构建命令版本号)
8. 测试远程更新是否正常
9. 执行-发布镜像
10. 添加 gitee、github 发行版
11. 可选填写发版公告