# 发布流程

1. 将 dev 分支合并到 master
2. changelog 版本增加发版日期
3. 更新代码版本号：`cd ./jpom-parent/script && sh replaceVersion.sh 2.x.x`
4. 打包前端：`npm run i && npm run build`
5. 打包后端：`mvn clean package`
6. 人工测试服务端包和插件端包是否可以正常启动
7. 将包上传到 oss : ` sh deploy-release-oss.sh`
8. 更新文档版本号：`cd ./docs/script && sh replaceVersion.sh 2.x.x`
9. 发布文档：`sh deploy-docs-pages.sh`
10. 测试远程更新是否正常
11. 发布服务端 docker 镜像包 `docker buildx build --platform linux/amd64,linux/arm64 -t jpomdocker/jpom:2.x.x -t jpomdocker/jpom:latest -f ./modules/server/DockerfileRelease --push .`