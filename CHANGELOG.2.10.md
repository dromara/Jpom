## 2.10.0

### 🐣 新增功能

1. 【all】外置 `logback` 配置文件

### 🐞 解决BUG、优化功能

1. 【all】启动相关信息由控制台输出改为 `logback`
2. 【all】节点管理中 `其他功能` 菜单更名为 `脚本管理`
3. 【all】优化版本升级修改管理脚本里变量,采用文件记录方式
4. 【server】优化容器启动脚本，支持监听进程已经终端重启操作
5. 【server】修复 自动刷新页面已经关闭的标签页，后台仍然在发送请求
   （感谢[@苏生不语](https://gitee.com/sushengbuyu) [Gitee issues I664OP](https://gitee.com/dromara/Jpom/issues/I664OP) ）

### ❌ 不兼容功能

1. 【server】取消支持 2.8.0 以下 json 文件转存数据库
2. 【all】下架 JDK 管理模块（请使用 DSL 项目模式代替）
3. 【all】下架 TOMCAT 管理模块（请使用 DSL 项目模式代替）
4. 【all】删除 项目内存监控页面
5. 【all】配置文件名称由 `extConfig.yml` 变更为 `application.yml`
6. 【all】调整项目打包目录结构
7. 【all】取消兼容低版本数据目录文件迁移（调试运行）
8. 【all】取消自动识别文件编码格式模块 `auto-charset-jchardet`
9. 【all】更新管理脚本，进程标识更新（已经存在的需要手动停止）
10. 【all】取消插件端配置化向服务端注册功能（采用快速导入方式替代）
11. 【server】取消服务端授权 token 配置
12. 【all】下架 节点脚本导入功能
13. 【server】取消限制创建用户最大数配置：`user.maxCount`
14. 【server】删除 node_info 表 cycle 字段

### ❌ 不兼容的属性配置变更

> 属性配置支持驼峰和下划线

1. 【agent】`whitelistDirectory.checkStartsWith` -> `jpom.whitelist-directory.check-starts-with`
2. 【agent】`project.stopWaitTime` -> `jpom.project.statusWaitTime`
3. 【agent】`project.*` -> `jpom.project.*`
4. 【agent】修正拼写错误 `log.*back` -> `jpom.project.log.*backup`
5. 【agent】`log.*` -> `jpom.project.log.*`
6. 【agent】`log.intiReadLine` -> `jpom.init-read-line`
7. 【agent】 `log.autoBackConsoleCron` 不支持配置 none (none 使用 `jpom.project.log.autoBackupToFile` 代替)
8. 【all】删除 `consoleLog.reqXss` 、`consoleLog.reqResponse`
9. 【all】`consoleLog.charset` -> `jpom.system.console-charset`
10. 【server】`node.uploadFileTimeOut` -> `jpom.node.uploadFileTimeout`
11. 【server】`system.nodeHeartSecond` -> `jpom.node.heartSecond`
12. 【server】`user.*` -> `jpom.user.*`
13. 【server】`jpom.authorize.expired` -> `jpom.user.tokenExpired`
14. 【server】`jpom.authorize.renewal` -> `jpom.user.tokenRenewal`
15. 【server】`jpom.authorize.key` -> `jpom.user.tokenJwtKey`
16. 【server】`jpom.webApiTimeout` -> `jpom.web.api-timeout`
17. 【server】删除 `ssh.initEnv`
18. 【server】批量修正前端相关配置属性均修改到 `jpom.web.*`
19. 【server】`db.*` -> `jpom.db.*`
20. 【server】`build.*` -> `jpom.build.*`

### TODO

1. ~~配置文件优化~~
2. 项目触发器
3. 节点转发模块优化
4. 分片上传文件
5. 构建事件触发新增更多（前后）
6. 复制项目
7. 测命令行参数
8. 资产管理
9. ~~标签页缓存问题（定时器未清空）~~