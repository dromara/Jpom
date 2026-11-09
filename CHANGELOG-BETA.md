# 🚀 版本日志

## 2.11.13.2-beta (2026-11-09)

### 🐣 新增功能

1. 【agent-go】新增 Go 语言插件端：单二进制、低内存、免 JDK，兼容 Java 插件端的配置、数据和协议，可在 Java/Go 之间平滑切换。
2. 【server】在线升级支持 Go 插件端：自动识别节点类型与运行平台并下发匹配的升级包，Java 节点升级方式不变。
3. 【agent-go】提供多平台安装包与在线升级包（Linux/macOS 的 amd64、arm64 与 Windows amd64），升级容器按节点平台精确匹配。
4. 【agent-go】支持国际化、Windows/macOS 系统监控、7z 解压、GBK 编码、cron 年份/分组，以及升级失败自动回滚。
5. 【mcp】新增 npm 包 jpom-mcp-server（MCP stdio Server）：配置 Jpom 地址与长期 token 后，Claude Desktop/Cursor/VS Code/DeepSeek Harness 等 MCP 客户端可查询节点、项目、构建、脚本、资产、监控、分发、系统与审计日志，并支持日志读取/有界实时跟随；默认只读，写操作需显式开启。

### 🐞 解决BUG、优化功能

1. 【all】修复 项目/节点脚本控制台、系统日志等日志监听在频繁打开关闭时，WebSocket 会话与文件监听 Tailer 线程可能未被释放、持续占用内存的问题：会话集合改为并发安全并按会话 ID 去重，watcher 的 add/close/restart 原子化且关闭幂等，关闭后不再接收新会话；发送失败立即移除并关闭会话；切换日志文件时先离线上一个监听；服务端 WebSocket 装饰器缓存与传输层代理缓冲在关闭/发送失败时即时释放
2. 【agent】优化 控制台 WebSocket 会话增加空闲超时（依赖前端 5s 心跳），异常断开（未收到 close 帧）的会话由容器回收；脚本执行会话与执行中缓存增加异常路径兜底清理，避免长期运行后堆内存持续增长导致进程假死/OOM
3. 【server】修复 重装/迁移服务端后“同步节点项目（脚本）”只提示拉取到 N 个、实际更新 0 个的问题：插件端项目/脚本仍带已不存在的旧工作空间时，现在会回退到节点当前工作空间再同步；工作空间仍然存在但不同（共享机器多空间）与 GLOBAL 数据保持原逻辑（[Gitee issues ICH1VK](https://gitee.com/dromara/Jpom/issues/ICH1VK)）
4. 【server】优化 严格构建下 sh 脚本在环境模板之后、用户命令之前注入 `set -e`（bash 下同时 `set -o pipefail`），中间命令或管道失败立即中断构建，不再继续执行后续命令；非严格构建与 Windows 批处理语义保持不变（[Gitee issues ICW9KZ](https://gitee.com/dromara/Jpom/issues/ICW9KZ)）
5. 【server】修复 Gitea/Gogs 的 GET webhook 把 push JSON 作为 `payload` 查询串携带、超过 Tomcat 默认请求行/请求头上限（8KB）导致接口直接返回 400 的问题：放宽 `server.max-http-header-size` 到 64KB（[Gitee issues IDG4PT](https://gitee.com/dromara/Jpom/issues/IDG4PT)）
6. 【web-vue】优化 逻辑节点、项目列表、分发列表的自动刷新：默认关闭（开关仍按用户+表格持久化在浏览器本地），项目列表/分发列表刷新间隔由 5s 调整为 30s，浏览器标签页不可见时暂停自动刷新；项目列表按节点查询运行状态增加防重入与 15s 超时，避免节点离线时请求长期挂起并随刷新叠加，占满同源连接导致同页/同标签其它列表请求 20s 超时（[Gitee issues ICXOGI](https://gitee.com/dromara/Jpom/issues/ICXOGI)）
7. 【server】修复 重置超级管理员密码（`--rest:super_user_pwd`）后新密码仅写入 `logs/server.log`、Windows 无控制台导致用户误以为重置失败的问题：命令执行结果同时输出到标准输出（`logs/stdout.log`/控制台）；超级管理员查询统一为 `systemUser=1`（保留 `parent=sys` 回退兼容旧数据），避免历史数据下静默失败（[Gitee issues IJNJZJ](https://gitee.com/dromara/Jpom/issues/IJNZJZ)）
8. 【agent】优化 远程下载文件保存为 URL 原始文件名（此前保存为随机文件名），Java/Go 插件端行为一致。

### ⚠️ 升级说明

1. **严格构建语义变化**：开启【严格构建】后，sh 构建脚本会在环境模板之后、用户命令之前注入 set -e（bash 同时 set -o pipefail），中间命令或管道中任一命令失败会立即中断，不再继续执行后续命令（此前仅在脚本整体退出码非 0 时才判定失败）。依赖“某条命令失败后继续执行”的严格构建脚本，请在脚本开头显式 set +e 自行管理；非严格构建（推荐用于允许中间命令失败的场景）与 Windows 批处理不受影响。
2. **Gitea/Gogs GET webhook 请求头上限**：新安装包默认 server.max-http-header-size: 65536（64KB）。已安装实例的外部配置文件 conf/application.yml 不会随升级自动覆盖，如仍遇到 Gitea/Gogs GET webhook 400，请在该文件 server: 下增加 max-http-header-size: 65536（或在【系统管理 → 外部配置文件】中重置为默认配置后按需调整），再重启服务端。
3. **列表自动刷新默认值变化**：逻辑节点、项目列表、分发列表的自动刷新默认关闭（此前默认开启），仍可在页面右上角手动开启，并按“用户+表格”持久化在浏览器本地；项目列表/分发列表刷新间隔调整为 30 秒，浏览器标签页不可见时暂停自动刷新。此前已手动开启自动刷新的用户不受影响（本地配置优先）。
4. **Go 插件端（可选新组件）**：安装包与在线升级包按平台区分（linux/darwin 的 amd64、arm64，windows-amd64），升级容器必须与节点平台一致；旧版 Server 手工上传时 linux-amd64 可用 agent-go-<version>-upgrade.jar 兼容别名。Windows 请使用包内 AgentGo.bat，服务托管只保留一种方式（nssm/sc 或手工启动）。
5. **Go 插件端配置提示**：JPOM_SERVICE 与 Java 的 -D 启动参数对 Go 插件端无效；AES 密钥请用环境变量 JPOM_ENCRYPT_AES_KEY（与服务端一致），升级后拉起由 systemd/nssm 配置负责。
6. **Java 插件端远程下载文件名变化**：远程下载的文件现在按 URL 原始文件名保存（此前为随机文件名）；如同名文件已存在会使用随机名回退。
------


## 2.11.13.1-beta (2026-11-09)

### 🐣 新增功能


### 🐞 解决BUG、优化功能

1. 【server】修复 文件中心下载接口未认证 SQL 注入风险：Hutool 会把查询参数值解析为 SQL 条件表达式（如 `token=is not null` 绕过别名+token 校验），`sort` 参数可注入原生 ORDER BY，且数据库异常会把完整 SQL 回显给客户端；现在查询 Bean 值统一使用占位符绑定，`sort`/`filter_*` 增加字段白名单，token 增加格式校验，并修复别名下载可跨文件读取的越权问题
2. 【server】修复 通用分页查询的 `:in` 过滤字段（如 `workspaceId:in`）在字段白名单校验前未剥离后缀而被静默丢弃的问题，避免工作空间隔离条件失效导致项目列表、节点脚本列表、文件中心（含触发器 token）、证书、仓库、环境变量（明文值）等数据跨工作空间串显/泄露
3. 【server】修复 权限拦截器在处理器方法缺少 `@Feature` 时静默放行的问题：属于权限管理模块的控制器改为 fail-closed，并补齐证书导出等接口的方法级权限声明（证书导出需要 `DOWNLOAD` 权限），避免未授权用户直达敏感业务接口
4. 【server】修复 WebSocket 鉴权降级：握手改用登录会话（HttpOnly Cookie）认证，不再仅凭 URL 中的密码摘要 `userId` 通过校验；补充禁用账号校验、IP 白名单与 Origin 同源校验，避免绕过 HTTP 侧拦截器；同时修复 `system_log` 及项目日志 WebSocket 日志文件名路径穿越（含符号链接逃逸），服务端与插件端同步修复
5. 【server】加固 工作空间强制过滤与导出范围：客户端伪造 `workspaceId`/`workspaceId:in`/`%workspaceId%` 等同字段参数不再能覆盖服务端强制的工作空间条件；项目导出、仓库导出改为仅导出“当前工作空间 + GLOBAL”，仓库分组下拉与 SSH 终端执行日志列表按工作空间过滤；非时间字段的 `xxx[0]`/`xxx[1]` 过滤参数不再导致 500
6. 【web-vue】修复 触发器/构建说明文案中的 JSON 大括号被前端国际化组件误判为插值占位符、打开相关弹窗时控制台报 `SyntaxError: Invalid token in placeholder` 的问题（文案改为转义输出大括号，显示内容不变）
7. 【all】修复 数据库异常响应回显完整 SQL/列名/表名的信息暴露问题：数据库类异常统一返回通用错误文案，完整异常堆栈仅记录在服务端日志（正常业务/参数/权限异常提示不受影响）


### ⚠️ 升级说明

1. **权限收紧（需要管理员确认）**：以下接口现在要求对应工作空间的方法权限（此前仅有登录/节点数据权限即可访问），升级后如提示“没有对应管理权限”，请到【用户管理 → 权限组】为相关用户的工作空间勾选对应权限；超级管理员不受影响：
   - 证书管理 → 证书导出：需要 `DOWNLOAD`（下载）权限；
   - 节点项目文件备份：下载需要 `DOWNLOAD`，删除需要 `DEL`，创建/恢复备份需要 `EDIT`。
2. **列表数据范围恢复**：项目列表、节点脚本列表、文件中心、静态文件存储、证书、仓库、脚本列表/执行记录、环境变量等“全局/工作空间共享”列表恢复为仅展示**当前工作空间 + 全局（GLOBAL）**数据；此前因工作空间过滤条件失效会显示其他工作空间数据（本次为安全修复，非故障）。
3. **触发器 token 格式校验**：文件下载、构建/脚本/SSH/项目/环境变量等触发器 token 仅接受**字母数字 6~128 位**；标准 32 位十六进制（当前版本）和历史 10 位字母数字 token 兼容。若仍在使用更早期非标准格式 token 且升级后提示无效，请重置该触发器 token。
4. **文件中心下载接口参数语义**：
   - `sort` 仅接受文件记录的真实字段（如 `createTimeMillis:desc`），非法字段会被忽略；
   - `filter_*` 仅接受真实字段，非法字段/非法值会被忽略（不再返回 500）；
   - 按别名下载时按“别名 + token”精确匹配；同一别名对应多个文件时，以 token 实际绑定的记录为准。
5. **错误提示变化**：非法或无权限 token 会提前返回“token 错误或已失效”；按别名下载且过滤条件无匹配时，当前也会提示“token 错误或已失效”（已记录为后续体验优化项，不影响功能）。
6. **无数据库表结构/字段变更**，无需手工迁移；升级前仍建议按惯例备份 `data`/`conf`。
7. **WebSocket 鉴权与日志安全收紧**：
   - WebSocket 改用登录会话（HttpOnly Cookie）认证，不再支持在 URL 中传 `userId=密码摘要`；前端已同步适配，升级后建议刷新页面；**自定义 WebSocket 客户端**需先登录或调用任意接口获取会话 Cookie，并按与页面同源的 `Origin` 连接；非浏览器客户端也可改用 `Authorization`（JWT）或 `JPOM-USER-TOKEN`（长期 token）请求头认证。
   - WebSocket 现在同样受 IP 访问限制；**禁用账号无法建立新的 WebSocket 连接**；已经建立的连接不会因账号随后被禁用而立即断开（重连时会校验）。
   - 系统日志、插件端日志、项目日志的 WebSocket 日志文件名限制在日志/项目目录内，防止路径穿越与符号链接逃逸；若自定义日志文件不在默认日志/项目目录，可能无法通过 WebSocket 实时查看（安全收紧）。
   - 反向代理需保留 `Host`（或设置 `X-Forwarded-Host`）并允许 WebSocket Upgrade；否则同源校验可能拒绝连接（浏览器直连/常规 Nginx 反代无需额外配置）。
8. **导出与空间过滤范围变化（需要管理员确认）**：
   - 【节点&项目 → 项目列表】导出、【在线构建 → 仓库信息】导出：仅导出**当前工作空间 + GLOBAL**数据（此前不受当前空间限制）；需要其它空间数据时请切换工作空间后分别导出；
   - 仓库分组下拉、SSH 终端执行日志列表现在按当前工作空间过滤（系统管理员同样受当前空间约束）；
   - 节点数据权限校验在服务端缓存未同步/数据不存在时由“放行”改为“拒绝”（fail-closed）：刚创建或同步数据后立即操作如提示无对应权限，请稍后重试或刷新页面；
   - 非时间字段带 `[0]`/`[1]` 的过滤参数将被忽略（不再返回 500）。
9. **数据库异常提示变化**：数据库类异常统一返回通用错误提示，不再在接口响应中展示 SQL/列名/表名等细节；排查时请查看服务端日志（完整堆栈仍会记录）。正常业务/参数/权限异常提示不变。


------

## 2.11.12.3-beta (2026-11-07)

### 🐣 新增功能

1. 【server】新增 容器构建 java 插件支持 JDK 21、25（#114）
2. 【server】新增 仓库导入支持 GitLab 私人令牌方式，可直接使用 token 拉取项目（[Gitee issues I9AZSS](https://gitee.com/dromara/Jpom/issues/I9AZSS)）

### 🐞 解决BUG、优化功能

1. 【all】升级 fastjson2 及 fastjson1 兼容包到 2.0.66，修复 fastjson2 AutoType 哈希碰撞绕过导致的远程代码执行风险（影响 fastjson2 ≤2.0.62，QVD-2026-45876；openapi 批量接口在鉴权前会解析请求体）
2. 【all】修复 后端参数校验失败时直接返回 i18n key 未翻译的问题（例如节点注册接口 `workspaceId` 为空时会提示 `i18n.workspace_id_required.c967`），现在按请求语言返回提示文案
3. 【all】优化 补全后端 i18n 遗漏：构建差异检查、JWT 密钥、可信代理、数据库初始化/备份/导入、达梦列 SQL 长度、节点注册错误等 31 处提示与日志支持多语言（新增/补登记 19 个 key，同步 4 种语言），并修正登录验证码提示 key 未登记 words.json、后缀不符合规范的问题
4. 【all】修复 管理脚本通过 `sh xxx.sh` 调用时报语法错误的问题，脚本自动切换到 bash 执行（[Gitee issues IC20RL](https://gitee.com/dromara/Jpom/issues/IC20RL)）
5. 【server】修复 终端在空命令行按退格、方向键等按键时数组越界的问题（#109）
6. 【server】优化 容器构建 runsOn 镜像构建失败时输出明确错误并中断构建，避免后续流程只报 `404 No such image`（#103）
7. 【server】修复 构建产物目录配置为绝对路径时构建列表接口 500 的问题（[Gitee issues IA6VM8](https://gitee.com/dromara/Jpom/issues/IA6VM8)）
8. 【server】优化 构建记录仓库提交说明字段长度加宽至 1000，避免较长提交信息写入失败（[Gitee issues ID35HQ](https://gitee.com/dromara/Jpom/issues/ID35HQ)）
9. 【server】修复 构建配置别名缺少规则校验提示的问题（[Gitee issues IBI5AF](https://gitee.com/dromara/Jpom/issues/IBI5AF)）
10. 【server】修复 权限组工作空间穿梭窗勾选计数错误的问题（[Gitee issues IBHK0I](https://gitee.com/dromara/Jpom/issues/IBHK0I)）

------

## 2.11.12.2-beta (2026-11-02)

### 🐣 新增功能

1. 【server】新增 差异构建支持配置监听路径、排除路径（Ant 风格通配），仅当变更文件匹配规则时才触发构建（感谢@凉拌西瓜 [Gitee pr 295](https://gitee.com/dromara/Jpom/pulls/295) ）
2. 【server】新增 项目文件列表支持分页展示，可切换每页条数（感谢@Justubborn [Gitee pr 294](https://gitee.com/dromara/Jpom/pulls/294) ）

### 🐞 解决BUG、优化功能

1. 【server】修复 跨工作空间越权访问问题：Docker、SSH 命令、构建仓库、SSH/FTP 文件、监控、节点项目与节点分发等接口未校验资产所属工作空间（#115 #119 #120，CVE-2026-91993、CVE-2026-111605）
2. 【server】修复 用户身份校验（checkUser）将入参解析为查询条件，可能被利用匹配任意用户的越权风险（#121，CVE-2026-120201）
3. 【server】修复 系统初始化接口缺少用户名、密码参数校验，可能被绕过初始化的问题
4. 【server】修复 数据库查询条件注入风险：主键条件以及分页过滤、排序字段统一使用占位符绑定和字段名白名单校验
5. 【all】升级 Tomcat 9.0.123、JGit、BouncyCastle、commons-lang3 等依赖，修复已知安全漏洞（#118；#116 已评估为不受影响）
6. 【all】优化 下载中心域名由 `d.jpom.download` 切换为 `d2.jpom.download`
7. 【server】优化 节点监控异常通知内容不再包含完整异常堆栈，仅保留错误信息，减少正式环境无效日志（感谢@Robot [Gitee pr 293](https://gitee.com/dromara/Jpom/pulls/293) ）
8. 【server】修复 JWT 签名密钥未配置时回退到源码公开固定值的问题：现在首次启动自动生成随机密钥并持久化到 `<data>/.jwt-key`（权限 600），空白配置也视为未配置；建议显式配置至少 32 位随机 `jpom.user.token-jwt-key`
9. 【server】修复 客户端可通过伪造 `X-Forwarded-For` / `X-Real-IP` 绕过 IP 白名单和登录 IP 失败锁定的问题：新增可信代理配置 `jpom.web.trusted-proxies`（默认仅信任本机回环），统一 IP 解析并在存在 IPv4 白名单时拒绝非 IPv4 客户端
10. 【agent】修复 Agent 未配置授权密码时把自动生成的明文密码写入日志的问题：日志不再输出密码，`data/agent_authorize.json` 权限收紧为 600
11. 【server】修复 `/tools/net-ping`、`/tools/net-telnet`、`/tools/ip-list` 缺少功能权限、任意登录用户可做服务端网络探测和网卡信息读取的问题：现在要求系统管理员权限，并限制端口/超时、增加操作日志；普通用户仍可使用 Cron 工具
12. 【server】修复 登录验证码可被空串绕过、账号不存在与密码错误返回码不同导致账号枚举的问题：验证码校验前移并改为一次性、5 分钟有效，账号不存在与密码错误统一返回 501

### ⚠️ 升级说明

1. 未配置 `jpom.user.token-jwt-key` 的部署升级后会自动生成 `<data>/.jwt-key`，旧登录 token 将失效，用户需要重新登录；集群部署请为所有节点显式配置相同的 key。
2. 反向代理不在本机时，请将代理地址配置到 `jpom.web.trusted-proxies`，否则 IP 白名单和登录 IP 锁定将看到代理 IP（这是安全收紧，不是故障）。
3. `/tools/net-ping`、`/tools/net-telnet`、`/tools/ip-list` 现在仅系统管理员可访问，普通用户菜单会隐藏；验证码失败或过期后需要重新获取。

------

## 2.11.12.1-beta

### 🐣 新增功能

1. 【server】新增 数据库支持使用达梦数据库（感谢[@wxyShine](https://gitee.com/wxyShine) ）
2. 【server】新增 FTP 管理，文件查看和构建发布方式新增 FTP 支持

### 🐞 解决BUG、优化功能

1. 【server】优化 数据库表支持配置前缀 `jpom.db.table-prefix` （感谢@ccx2480）
2. 【server】修复 终端输入命令，按Backspace 会退出终端（感谢[@dgs](https://gitee.com/dgs0924) [Gitee issues ICA57K](https://gitee.com/dromara/Jpom/issues/ICA57K) ）
3. 【server】优化 脚本日志和 SSH 命令日志支持批量删除（感谢[@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues IBIM6W](https://gitee.com/dromara/Jpom/issues/IBIM6W) ）
4. 【server】修复 表格自定义列在部分字段不生效情况
5. 【server】优化 构建发布方式中 Docker 镜像支持配置网络模式（感谢@酱总）

------

## 2.11.11.4-beta (2025-05-06)

### 🐣 新增功能

1. 【server】新增 远程安装时的授权码支持（感谢@奇奇）

### 🐞 解决BUG、优化功能

1. 【server】修复 SSH控制台输入Control + R后无法记录日志的问题
2. 【server】优化 容器构建镜像新增 [毫秒镜像](https://1ms.run/) 临时仓库
3. 【all】优化 修改 tomcat版本到9.0.99安全版本 （感谢@[龙猫](https://gitee.com/woyeshimao) [Gitee pr 292](https://gitee.com/dromara/Jpom/pulls/292)）

------

## 2.11.11.3-beta (2025-01-10)

### 🐞 解决BUG、优化功能

1. 【server】优化 构建页面保存按钮使用 loading （感谢[@hhh](https://gitee.com/who9264) [Gitee issues IBD8WF](https://gitee.com/dromara/Jpom/issues/IBD8WF) ）
2. 【server】优化 SSH 文件列表支持跟随目录排序
3. 【server】优化 用户操作日志记录表中用户 id 长度不够照成登录失败问题（感谢[@Halo](https://gitee.com/halo-lmf) [Gitee issues IBFNKE](https://gitee.com/dromara/Jpom/issues/IBFNKE) ）
4. 【agent】优化 DSL 项目备份支持配置开启或者关闭差异备份（默认使用差异备份：仅备份变动过的文件）（感谢[@ercats](https://gitee.com/ercats) [Gitee issues IBF73J](https://gitee.com/dromara/Jpom/issues/IBF73J) ）
5. 【server】优化 构建 webhook 新增 git 的提交id和提交信息参数（感谢[@皮儿卡啦没有丘](https://gitee.com/Aimeia) [Gitee issues IBDEU0](https://gitee.com/dromara/Jpom/issues/IBDEU0) ）
6. 【server】优化 文件中心发布文件支持保存为模板方便后续使用（感谢[@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues IB2FRF](https://gitee.com/dromara/Jpom/issues/IB2FRF) ）

------

## 2.11.11.2-beta (2024-12-26)

### 🐣 新增功能

1. 【all】升级 hutool 版本
2. 【server】新增 服务端在线升级页面支持配置下载授权码，方便国内用户使用全速 CDN

### 🐞 解决BUG、优化功能

1. 【server】修复 ssh文件管理显示错位（感谢[@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues IB98AU](https://gitee.com/dromara/Jpom/issues/IB98AU) ）

------

## 2.11.11.1-beta (2024-12-03)

### 🐣 新增功能

1. 【server】新增 支持使用 TOPIAM 企业数字身份管控平台登录（oauth2）

------

## 2.11.10.2-beta (2024-12-02)

### 🐞 解决BUG、优化功能

1. 【server】修复 容器构建自定义插件，环境变量不匹配问题（感谢[@杨晓龙](https://gitee.com/pubresyang) [Gitee pr 285](https://gitee.com/dromara/Jpom/pulls/285) ）
2. 【agent】优化 项目日志文件编码格式支持独立配置（感谢[@不渡](https://gitee.com/zhangjxing) [Gitee issues IB3DCP](https://gitee.com/dromara/Jpom/issues/IB3DCP) ）
3. 【all】优化 使用默认语言时增加判断所在时区来识别中文环境
4. 【all】升级 fastjson 依赖版本

------

## 2.11.10.1-beta (2024-11-21)

### 🐞 解决BUG、优化功能

1. 【server】升级 docker-java 版本到 3.4.0
2. 【server】优化 数据库备份支持外部调用触发器来实现自定义备份周期（感谢@刘志远）
3. 【server】优化 SSH、项目 文件管理树加载 loading 避免网络卡顿影响树渲染错误（感谢@Bo）
4. 【server】优化 SSH、项目 文件管理树支持横向滚动条
5. 【server】优化 构建记录存储最后 commitId commitMsg 信息（感谢@program）
6. 【server】优化 引用脚本库时出现 ${xxxx} 出现引用失败：No group with name {xxxx} （感谢[@foot_print](https://gitee.com/zhangyf35) [Gitee issues IAW413](https://gitee.com/dromara/Jpom/issues/IAW413) ）
7. 【server】优化 构建环境变量自动识别系统变量密文相关关键词：pwd、pass、password、token 为隐私变量（感谢[@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues IB34L9](https://gitee.com/dromara/Jpom/issues/IB34L9) ）
8. 【agent】优化 项目状态触发器 result 参数返回 json 类型，DSL 项目重启成功返回 pid（感谢@新）
9. 【server】优化 日志查看器中行号增加无法选中样式（感谢[@ning](https://gitee.com/mrning001) [Gitee pr 284](https://gitee.com/dromara/Jpom/pulls/284) ）

------

## 2.11.9.3-beta (2024-09-02)

### 🐞 解决BUG、优化功能

1. 【server】优化 构建 webhook 携带工作空间和集群id参数（workspaceId、clusterInfoId、workspaceName）（感谢@🦁子）
2. 【server】修复 构建命令引用脚本时相关脚本引用脚本库未生效问题（感谢@木迷榖）
3. 【server】优化 构建命令、本地发布命令支持直接引起脚本库（`G@("xx")` xx 为脚本标记）
4. 【server】修复 构建时日志记录器异常关闭未提示并且构建状态未修改（感谢@陈晨🍊）
5. 【server】优化 构建日志弹窗支持查看可用环境变量
6. 【server】优化 构建支持查看可用环境变量（感谢[@慌i](https://gitee.com/L_FaN) [Gitee issues IALAUW](https://gitee.com/dromara/Jpom/issues/IALAUW) ）
7. 【server】升级 mysql 驱动版本到 8.2.0
8. 【server】优化 ssh docker 独立配置是否需要 sudo 执行
9. 【server】修复 ssh docker 选型固定只有最新 10 条调整为搜索下拉（@山上雪）

------

## 2.11.9.2-beta (2024-08-22)

### 🐞 解决BUG、优化功能

1. 【server】修复 git仓库账号/密码包含特殊符号导致系统Git工具无法成功认证的问题（感谢@🇩）
2. 【server】修复 SSH 分片上传极大概率上传失败问题（感谢@勤思·、@张飞鸿）
3. 【all】升级 hutool、logback、tomcat 版本
4. 【server】优化 时长格式化工具支持 i18n 国际化
5. 【server】修复 终端日志处理字节'7'导致的日志补全记录不全 （感谢@[adao](https://github.com/YazaiHu) [Github issues 91](https://github.com/dromara/Jpom/issues/91) ）
6. 【server】升级 spring 包到 5.3.39 修复漏洞 CVE-2024-38808，CVE-2024-38809（感谢@[龙猫](https://gitee.com/woyeshimao)）[Gitee Pr 281](https://gitee.com/dromara/Jpom/pulls/281)

------

## 2.11.9.1-beta (2024-08-08)

### 🐣 新增功能

1. 【server】新增 项目监控新增沉默时间配置，避免报警后不再触发报警

### 🐞 解决BUG、优化功能

1. 【all】修复 多会话使用文件跟踪器可能出现多个线程未关闭问题（感谢[@沐剑屏](https://gitee.com/MuJianPing) [Gitee issues IAI0I1](https://gitee.com/dromara/Jpom/issues/IAI0I1) ）
2. 【all】修复 windows 管理脚本在 JDK 1.8 无法正常启动（感谢@流浪的墨染青衫、@LL）
3. 【server】修复 多个工作空间分组后子工作空间层级异常造成不能正常使用
4. 【server】修复 监控日志标题字段长度太短在英语模式下字段异常（感谢@Deer）
5. 【server】优化 监控新增使用语言来实现通知消息国际化

------

## 2.11.8.5-beta (2024-08-02)

### 🐞 解决BUG、优化功能

1. 【server】优化 更多组件的页面层级问题，避免层级错乱无法正常使用页面相关功能
2. 【server】修复 容器构建 DSL 配置示例错乱

------

## 2.11.8.4-beta (2024-07-25)

### 🐞 解决BUG、优化功能

1. 【server】优化 登录验证码去除易混淆字母（oO0、lL1、q9Q、pP）
2. 【server】修复 批量构建引起环境变量丢失问题（感谢@[xieliangza](https://github.com/xieliangza) [Github issues 88](https://github.com/dromara/Jpom/issues/88) ）
3. 【all】优化 解析 HTTP `Accept-Language` 请求头支持多语言最高优先级
4. 【server】修复 页面未刷新情况下打开弹窗次数过多不能提示窗口层级太低（感谢[@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues IAEBUZ](https://gitee.com/dromara/Jpom/issues/IAEBUZ) ）
5. 【server】优化 分发日志显示关联数据信息（感谢[@pumpkinor](https://gitee.com/pumpkinor) [Gitee issues IAF7IV](https://gitee.com/dromara/Jpom/issues/IAF7IV) ）
6. 【server】优化 分发文件使用文件中心或者静态文件上传至节点使用实际文件名（感谢[@pumpkinor](https://gitee.com/pumpkinor) [Gitee issues IAF7GD](https://gitee.com/dromara/Jpom/issues/IAF7GD) ）
7. 【server】优化 SSH 文件管理支持浏览器分片上传（感谢[@超人那个超i](https://gitee.com/chao_a) [Gitee issues IAD9W4](https://gitee.com/dromara/Jpom/issues/IAD9W4) ）
8. 【all】优化 管理脚本判断 JDK9+ 自动添加解除限制反射参数：--add-opens=xxxx

------

## 2.11.8.3-beta (2024-07-05)

### 🐞 解决BUG、优化功能

1. 【server】修复 淘汰 javax.security.cert.X509Certificate（感谢[@Jie](https://github.com/index-jie) [Github issues 84](https://github.com/dromara/Jpom/issues/86) ）
2. 【all】优化 启动控制台日志完善 i18n
3. 【server】修复 证书删除后弹窗未正常关闭

------

## 2.11.8.2-beta (2024-07-04)

### 🐞 解决BUG、优化功能

1. 【server】修复 非管理员用户无法使用脚本库数据（感谢[@丁炳坤](https://gitee.com/dbk12138) [Gitee issues IA8ZKP](https://gitee.com/dromara/Jpom/issues/IA8ZKP) ）
2. 【server】修复 构建编辑页面未输入构建命令提示错误（感谢[@yt](https://gitee.com/a1127583020) [Gitee issues IAAI8D](https://gitee.com/dromara/Jpom/issues/IAAI8D) ）
3. 【server】优化 构建容器镜像支持推送到远程仓库后自动删除本地镜像（感谢 [@爱琳琳真是太好了](https://gitee.com/qiqi513_admin) [Gitee issues IA6UBO](https://gitee.com/dromara/Jpom/issues/IA6UBO) ）

------

## 2.11.8.1-beta (2024-07-03)

### 🐞 解决BUG、优化功能

1. 【server】修复 静态文件扫描到空时更新数据库异常（感谢[@blackcat](https://gitee.com/kylin_lawliet) [Gitee issues IA6UO0](https://gitee.com/dromara/Jpom/issues/IA6UO0) ）
2. 【server】修复 SSH命令行操作日志支持更全展示（感谢[@beens](https://gitee.com/beens) [Gitee issues IA6P3J](https://gitee.com/dromara/Jpom/issues/IA6P3J) ）
3. 【all】优化 升级 hutool、oshi、mwiede-jsch、fastjson2 版本

------

## 2.11.7.1-beta (2024-06-20)

### 🐞 解决BUG、优化功能

1. 【server】修复 操作确认交互弹窗层级过低无法正常显示使用（感谢@ccx2480）

------

## 2.11.6.6-beta (2024-06-19)

### 🐣 新增功能

1. 【all】新增 国际化语言支持：繁體中文（中國香港）、繁體中文（中國臺灣）

### ⚠️ 注意

- 前端国际化翻译程度：98%
- 后端已翻译语言可以度：95%（部分异步执行日志等目前未支持）

------

## 2.11.6.5-beta (2024-06-17)

### 🐣 新增功能

1. 【server】新增 服务端脚本、SSH 脚本支持引用全局脚本库（`G@("xx")` xx 为脚本标记）
2. 【agent】新增 节点脚本支持引用全局脚本库（`G@("xx")` xx 为脚本标记）

### 🐞 解决BUG、优化功能

1. 【agent】修复 不同工作空间下同一个机器节点相同的项目ID的项目数据被覆盖（感谢@小朱）
2. 【agent】优化 DSL 项目支持引用脚本库中的脚本（G@xxxx）xxxx 为脚本标记
3. 【all】优化 新增系统语言配置 `jpom.system.lang`
4. 【server】优化 前端紧凑模式在浅色模式下也生效

### ⚠️ 注意

- 前端国际化翻译程度：90%
- 后端已翻译语言可以度：80%（部分异步执行日志等目前未支持）

后端日志国际化需要新增或者修改 `jpom.system.lang` 配置项

------

## 2.11.6.4-beta (2024-06-14)

### 🐣 新增功能

1. 【all】新增 支持 i18n 语言国际化（zh-CN、en-US）

### ⚠️ 注意

#### 翻译进度

- 前端国际化翻译程度：80%
- 后端国际化翻译程度：90%

#### 使用程度

- 前端已翻译语言可以度：100%
- 后端已翻译语言可以度：60%（后端日志、异步执行日志等目前未支持）

### 🤝致谢

感谢 [@a20070322](https://gitee.com/a20070322) / Controllers 大佬贡献 Jpom 前端 i18n 工具。

------

## 2.11.6.3-beta (2024-06-14)

### 🐣 新增功能

1. 【agent】新增 全局脚本库（DSL 项目可引用）

### 🐞 解决BUG、优化功能

1. 【server】修复 gogs 仓库令牌导入异常（感谢@张飞鸿）
2. 【server】修复 自定义仓库令牌导入后页面异常（感谢@张飞鸿）

------

## 2.11.6.2-beta (2024-06-06)

### 🐞 解决BUG、优化功能

1. 【server】修复 没有端口的容器重建页面异常（感谢@冰淇淋还是冰激凌）
2. 【server】修复 工作空间菜单配置错误（感谢@Again... .）
3. 【server】优化 ssh 管理独立 tab 页面使用默认的字符串排序
4. 【server】修复 服务端脚本无法执行、参数值描述不对应（感谢@冰淇淋还是冰激凌）

------

## 2.11.6.1-beta (2024-06-04)

### 🐣 新增功能

1. 【agent】新增 项目文件支持快捷复制到当前文件夹
2. 【agent】新增 项目文件夹支持快捷压缩（感谢[@yiziyu](https://gitee.com/yiziyu) [Gitee issues I9737L](https://gitee.com/dromara/Jpom/issues/I9737L) ）

### 🐞 解决BUG、优化功能

1. 【server】优化 部分参数、环境变量配置交互优化取消文本输入框采用标签模式（感谢@湘江夜色）
2. 【server】修复 部分页面中文描述未正常显示
3. 【server】优化 文件发布支持选择脚本模板（感谢[@linCodeTest](https://gitee.com/linWorld) [Gitee issues I9P0EU](https://gitee.com/dromara/Jpom/issues/I9P0EU) ）
4. 【server】优化 升级 postgresql 版本（感谢@ʟᴊx💎💎）
5. 【server】修复 机器相关授权配置文件后缀输入框未正常显示（@感谢@ccx2480）

------

## 2.11.5.2-beta (2024-05-30)

### 🐞 解决BUG、优化功能

1. 【server】优化 自动续签采用无感模式（感谢@湘江夜色）
2. 【server】优化 容器构建执行配置自定义 host 参数（感谢@冰淇淋还是冰激凌）
3. 【all】升级 tomcat、yaml 版本（感谢@佳驰）
4. 【all】升级 bcprov-jdk18on 版本

------

## 2.11.5.1-beta (2024-04-30)

### 🐣 新增功能

1. 【agent】新增 项目支持配置禁止扫描目录避免大目录页面超时（感谢@我）

------

## 2.11.5.0-beta (2024-04-29)

### 🐣 新增功能

1. 【all】新增 自由脚本方便调试机器节点

### 🐞 解决BUG、优化功能

1. 【server】修复 资产管理 SSH 配置禁用命令无法回显（感谢@zhangw）
2. 【server】修复 资产管理 SSH 未配置授权目录时 NPE （感谢[@Anley](https://gitee.com/MrAnley) [Gitee issues I9J17G](https://gitee.com/dromara/Jpom/issues/I9J17G) ）
3. 【agent】优化 监控机器网络流程支持配置排除网卡或者仅统计对应的网卡
4. 【server】修复 退出登录时页面会提示需要登录相关信息
5. 【server】优化 页面检测新版本判断是否加入 beta
6. 【agent】优化 添加数据记录修改人（感谢[@陈旭](https://gitee.com/chenxu8989) [Gitee issues I9JSY7](https://gitee.com/dromara/Jpom/issues/I9JSY7) ）
7. 【server】优化 插件端注册到服务端，网络测试支持 ping + telnet （感谢@泊凉青川）

------

## 2.11.4.2-beta (2024-04-22)

### 🐣 新增功能

1. 【server】新增 发布系统公告

### 🐞 解决BUG、优化功能

1. 【server】升级 前端组件版本
2. 【all】优化 管理脚本删除 `-XX:-UseBiasedLocking` 使其能在高版本 jdk 运行
3. 【server】修复 构建列表卡片模式按钮文字错乱
4. 【server】修复 项目列表和逻辑节点卡片视图冲突
5. 【server】修复 docker管理新增docker选择证书界面权重异常 （感谢[@伤感的风铃草](https://gitee.com/bwy-flc) [Gitee issues I9GYVA](https://gitee.com/dromara/Jpom/issues/I9GYVA) ）
6. 【server】修复 系统管理中用户管理中登录日志无法筛选
7. 【server】优化 用户登录记录操作日志（保证操作监控能记录）
8. 【server】修复 系统管理中用户登录日志无法分页
9. 【server】优化 Oauth2 支持配置创建账号配置权限组
10. 【server】修复 文件发布权限为执行权限、文件发布记录删除无记录日志 （感谢@蓝枫）

------

## 2.11.4.1-beta (2024-04-12)

### 🐞 解决BUG、优化功能

1. 【server】优化 构建相关页面无法正常打开
2. 【server】优化 Git 仓库地址不正确相关提示更准确（感谢@易自玉）

------

## 2.11.4.0-beta (2024-04-12)

### 🐣 新增功能

1. 【server】新增 Oauth2 新增【飞书账号】、【自建 Gitlab】登录（感谢[@鸡皮蒜毛与鸡毛蒜皮](https://gitee.com/cuia) [Gitee issues I9ELGS](https://gitee.com/dromara/Jpom/issues/I9ELGS) ）
2. 【server】新增 Oauth2 新增企业微信登录

### 🐞 解决BUG、优化功能

1. 【server】优化 oauth2 第三方平台登录解析用户名将依次尝试：平台用户名、邮箱、uuid
2. 【server】修复 无法查询到分组信息（页面下拉框）（感谢[@Robot](https://gitee.com/robot1937) [Gitee issues I9FN9U](https://gitee.com/dromara/Jpom/issues/I9FN9U) ）
3. 【all】升级 hutool 版本
4. 【server】修复 修复孤独数据描述错别字（感谢[@cuiyongsheng](https://github.com/Cuiys1458) [Github issues 77](https://github.com/dromara/Jpom/issues/77) ）
5. 【server】修复 前端地址栏输入二级路径 404 页面卡死问题

------

## 2.11.3.2-beta (2024-04-07)

### 🐣 新增功能

1. 【server】新增 Oauth2 新增钉钉扫码登录

### 🐞 解决BUG、优化功能

1. 【agent】优化 DSL 项目支持配置在特定目录执行脚本（run.execPath）
2. 【agent】优化 管理脚本 -Xss 默认值修改为 512k（感谢@Again... ）
3. 【server】优化 管理脚本 -Xss 默认值修改为 1024k（感谢@Again... ）
4. 【server】优化 声明使用开源软件列表、增加本软件开源协议声明

------

## 2.11.3.1-beta (2024-04-02)

### 🐣 新增功能

1. 【server】新增 数据库支持 *mariadb*

### 🐞 解决BUG、优化功能

1. 【server】修复 无法查询到分组信息（页面下拉框）（感谢@猫猫向钱跑）
2. 【server】修复 【项目文件管理远程下载】、【镜像创建容器】确认按钮无法使用（感谢@猫猫向钱跑）
3. 【server】修改 资产管理机器管理删除按钮无法正常使用（感谢@🇩）
4. 【server】修复 SSH 面板文件管理无法正常切换（感谢@勤思·）
5. 【server】优化 部分页面在火狐浏览器无法正常打开（感谢[@sparkarvin](https://gitee.com/arvinlovegood_admin) [Gitee issues I96IOA](https://gitee.com/dromara/Jpom/issues/I96IOA) ）
   （感谢[@a20070322](https://gitee.com/a20070322) [Gitee Pr 221](https://gitee.com/dromara/Jpom/pulls/221)  ）

------

## 2.11.3.0-beta (2024-03-21)

### 🐣 新增功能

1. 【server】新增 数据库支持 *postgresql* （感谢[@王先生](https://gitee.com/whz_gmg1)）[Gitee Pr 223](https://gitee.com/dromara/Jpom/pulls/223)

### 🐞 解决BUG、优化功能

1. 【all】优化 新增 `jpom.system.command-use-sudo` 配置属性控制是否使用 sudo 执行部分系统命令
2. 【server】优化 前端页面 keep-alive 可能导致的内存泄漏问题（感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I9510M](https://gitee.com/dromara/Jpom/issues/I9510M) ）
3. 【server】修复 部分弹窗不生效问题（感谢[@a20070322](https://gitee.com/a20070322)  [Gitee Pr 215](https://gitee.com/dromara/Jpom/pulls/215) ）
4. 【server】优化 前端 ES lint 配置规范前端代码（感谢[@a20070322](https://gitee.com/a20070322) [Gitee Pr 214](https://gitee.com/dromara/Jpom/pulls/214) / [Gitee Pr 215](https://gitee.com/dromara/Jpom/pulls/215) / [Gitee Pr 217](https://gitee.com/dromara/Jpom/pulls/217) ）
5. 【server】修复 docker 控制台网络选项卡页面空白（感谢@破冰）
6. 【server】修复 节点历史监控统计图表时间查询不生效（感谢@九問）
7. 【server】优化 SSH 脚本触发器支持传入参数当环境变量（感谢@小朱）
8. 【server】修复 h2迁移其它数据库时部分数据丢失（感谢[@王先生](https://gitee.com/whz_gmg1)）[Gitee issues I9977K](https://gitee.com/dromara/Jpom/issues/I9977K)
9. 【server】优化 逐步引入新版表格（构建、项目、节点、资产机器）（感谢[@a20070322](https://gitee.com/a20070322) [Gitee Pr 218](https://gitee.com/dromara/Jpom/pulls/218) / [Gitee Pr 220](https://gitee.com/dromara/Jpom/pulls/220) / [Gitee Pr 222](https://gitee.com/dromara/Jpom/pulls/222) ）
10. 【server】优化 工作空间概括构建日志支持快速查看详情（感谢@Roger.cao）

------

## 2.11.2.3 (2024-02-29)

### 🐞 解决BUG、优化功能

1. 【server】优化 系统文件占用空间统计周期调整为每天2次（感谢[@singlethread](https://gitee.com/zengwei_joni) [Gitee issues I9302U](https://gitee.com/dromara/Jpom/issues/I9302U) ）
2. 【server】优化 支持配置前端所有参数编码来规避部分安全规则检查（感谢[@zhaozxc2010](https://gitee.com/zhaozxc2010) [Gitee issues I8Z1VJ](https://gitee.com/dromara/Jpom/issues/I8Z1VJ) ）
3. 【server】优化 上传文件空文件提示文件路径（感谢[@SchuckBate](https://gitee.com/skBate) [Gitee issues I93FI6](https://gitee.com/dromara/Jpom/issues/I93FI6) ）
4. 【server】优化 监听日志文件消息发送失败后自动移除会话（感谢[@singlethread](https://gitee.com/zengwei_joni) [Gitee issues I93ZFX](https://gitee.com/dromara/Jpom/issues/I93ZFX) ）
5. 【server】优化 容器构建产物为文件时保存路径层级错误（感谢[@vfhky](https://github.com/vfhky) [Github Pr 71](https://github.com/dromara/Jpom/pull/71) ）

------

## 2.11.2.2 (2024-02-22)

### 🐞 解决BUG、优化功能

1. 【server】优化 升级 docker-java、jgit 版本
2. 【all】优化 升级 commons-compress 版本
3. 【agent】优化 升级 oshi 版本
4. 【server】优化 新增配置节点 websocket 通讯消息大小限制（jpom.node.web-socket-message-size-limit）（感谢@长弘）

------

## 2.11.2.1 (2024-02-19)

### 🐞 解决BUG、优化功能

1. 【server】优化 构建代码未变动流程打断触发器未传入原因（statusMsg）（感谢@烛孩）
2. 【server】修复 项目控制台日志删除弹窗未能正常关闭（感谢@%）
3. 【server】修复 脚本日志时间筛选不生效（感谢[@zhaozxc2010](https://gitee.com/zhaozxc2010) [Gitee issues I8ZNKL](https://gitee.com/dromara/Jpom/issues/I8ZNKL) ）
4. 【server】优化 页面左侧菜单固定悬浮不跟随屏幕滚动条滚动（感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I8ZOOB](https://gitee.com/dromara/Jpom/issues/I8ZOOB) / [Gitee Pr 201](https://gitee.com/dromara/Jpom/pulls/201) ）
5. 【server】优化 新增机器节点提示未选择协议（感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I8ZDZT](https://gitee.com/dromara/Jpom/issues/I8ZDZT) / [Gitee Pr 202](https://gitee.com/dromara/Jpom/pulls/202) ）
6. 【server】修复 SSH 资产硬盘信息显示错误（感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I8ZY7K](https://gitee.com/dromara/Jpom/issues/I8ZY7K) ）
7. 【server】优化 表格搜索区域小屏幕适配 （感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I8ZY0B](https://gitee.com/dromara/Jpom/issues/I8ZY0B) ）
8. 【server】优化 SSH 文件管理树操作优化 （感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I9054L](https://gitee.com/dromara/Jpom/issues/I9054L) / [Gitee issues I5DMKG](https://gitee.com/dromara/Jpom/issues/I5DMKG) ）
9. 【server】优化 整体页面顶部菜单吸顶效果（感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I907Y8](https://gitee.com/dromara/Jpom/issues/I907Y8) ）
10. 【server】优化 资产监控线程池独立管理（感谢[@singlethread](https://gitee.com/zengwei_joni) [Gitee issues I918AB](https://gitee.com/dromara/Jpom/issues/I918AB) ）
11. 【server】优化 构建回滚使用构建独立线程池
12. 【all】优化 升级 hutool 版本（主要解决版本号排序异常）（感谢 [@Tom Xin](https://gitee.com/meiMingle) [Gitee issues I8Z3TI](https://gitee.com/dromara/Jpom/issues/I8Z3TI) / [Hutool issues I8Z3VE](https://gitee.com/dromara/hutool/issues/I8Z3VE)）
13. 【all】优化 升级 fastjson 版本
14. 【server】优化 页面整体滚动条兼容高版本浏览器（感谢@Controllers）

### 🤝致谢

感谢 [@a20070322](https://gitee.com/a20070322) / Controllers 大佬帮助优化 Jpom 前端部分老大难问题。

------

## 2.11.2.0 (2024-01-22)

### 🐞 解决BUG、优化功能

1. 【agent】修复 修改项目日志路径如果文件夹不存在报错（感谢@长弘）
2. 【server】修复 节点机器日志无法下载（感谢@Again...）
3. 【all】升级 hutool 版本
4. 【agent】升级 oshi 版本
5. 【server】升级 mwiede、apache-sshd 版本（感谢@*斌）
6. 【server】优化 项目列表 file 类型正常排序（不再排序到最后）（感谢[@pal865](https://gitee.com/pal865) [Gitee issues I8XU32](https://gitee.com/dromara/Jpom/issues/I8XU32) ）
7. 【all】修复 windows 环境保存配置并重启失败（感谢[@Robot](https://gitee.com/robot1937) [Gitee issues I8Y01T](https://gitee.com/dromara/Jpom/issues/I8Y01T) ）
8. 【server】修复 新版本页面部分分页切换失效（构建详情、资产机器、逻辑节点）（感谢@zac）

------

## 2.11.1.5 (2024-01-18)

### 🐞 解决BUG、优化功能

1. 【agent】修复 插件端非默认工作空间项目重启后变为孤独数据（感谢@ccx2480）
2. 【server】修复 新增节点分发项目数据为孤独数据

------

## 2.11.1.3/4-beta (2024-01-17)

### 🐞 解决BUG、优化功能

1. 【server】优化 修复页面打包页面底部白屏

------

## 2.11.1.2-beta (2024-01-17)

### 🐣 新增功能

1. 【server】新增 触发器调用次数统计、触发器统一管理
2. 【server】新增 本地构建命令执行支持配置多线程方式（多线程接收输出流，避免极端情况卡死）

### 🐞 解决BUG、优化功能

1. 【all】优化 机器状态新增：资源监控异常（资源监控异常不影响功能使用）
2. 【server】优化 取消登录页动态背景图
3. 【server】修复 节点分发文件中心、静态文件后文件自动被删除（感谢@九問）
4. 【server】优化 容器构建支持配置容器资源（HostConfig）（感谢@珂儿）

------

## 2.11.1.1-beta (2024-01-16)

### 🐞 解决BUG、优化功能

1. 【server】修复 静态文件名太短（100个字符）（感谢@*斌）
2. 【server】修复 还原数据库弹窗内容提示为空（感谢@伤感的风铃草🌿）
3. 【server】优化 echarts 支持跟随深色模式
4. 【server】修复 编辑节点分发服务端脚本弹窗被挡住（感谢@🇩）
5. 【server】优化 前端打包（缩减首屏加载时间）（感谢@曾梦想仗剑走天涯）

------

## 2.11.1.0-beta (2024-01-15)

### 🐞 解决BUG、优化功能

1. 【server】修复 docker TLS 证书无法查看全部、证书无法编辑（新版遗漏）
2. 【server】优化 docker 资产监控支持自定义配置 cron `jpom.assets.docker.monitor-cron`
3. 【server】修复 容器终端、容器日志无法正常使用
4. 【server】修复 新版本页面多处无法正常使用相关问题（优化部分提示说明）

------

## 2.11.0.13-beta (2024-01-12)

### 🐞 解决BUG、优化功能

1. 【server】修复 部分 mysql 存储创建索引异常
2. 【agent】优化 插件端自由脚本（分发文件脚本）大小限制调整为 5M（感谢@九問）
3. 【server】修复 新版本 UI 部分错别字、日志阅读查看无法正常使用等
4. 【server】优化 编辑器样式、DSL 配置样式

------

## 2.11.0.12-beta (2024-01-12)

### 🐣 新增功能

1. 【server】新增 资产总览统计

### 🐞 解决BUG、优化功能

1. 【server】修复 mysql 存储使用游标查询报错（不使用游标）（感谢@🇩）
2. 【server】修复 新版 UI 下拉框搜索不生效
3. 【server】优化 **添加**关键词替换为**新增**
4. 【server】优化 部分页面无数据提示
5. 【server】修复 节点分发构建产物选择构建历史为匹配对应构建
6. 【server】修复 部分操作构建环境变量丢失（保存并构建、后台构建、直接构建）

------

## 2.11.0.11-beta (2024-01-11)

### 🐞 解决BUG、优化功能

1. 【server】修复 编辑器无法加载样式
2. 【server】优化 本地 git 联动严格执行开关
3. 【server】修复 打包后终端弹窗样式错乱（感谢@🇩）
4. 【server】修复 切换账户、退出登录菜单未刷新问题（感谢@ccx2480）
5. 【server】修复 登录账户未跳转配置的第一个工作空间（未遵循自定义配置）

------

## 2.11.0.10-beta (2024-01-10)

### 🐞 解决BUG、优化功能

1. 【server】优化 系统Git拉取代码时，强制云端最新代码覆盖本地代码
2. 【server】修复 升级页面更新日志样式错乱
3. 【server】修复 切换账户后用户信息未自动刷新
4. 【agent】修复 **部分操作引起项目节点分发属性丢失问题**
5. 【server】修复 无法新增项目（权限判断异常）（感谢@群友）
6. 【agent】修复 插件端环境变量值获取异常
7. 【agent】优化 插件端 java 项目启动支持读取环境变量
8. 【server】修复 项目列表选择错乱、批量操作无法正常使用（感谢@🇩）

------

## 2.11.0.9-beta (2024-01-10)

### 🐣 新增功能

1. 【all】新增 孤独数据管理（查看孤独数据、修正孤独数据）（感谢[@陈旭](https://gitee.com/chenxu8989) [Gitee issues I8UNXZ](https://gitee.com/dromara/Jpom/issues/I8UNXZ)）

### 🐞 解决BUG、优化功能

1. 【server】优化 上传文件前解析文件信息采用全局 loading
2. 【server】优化 构建流程交互（采用步骤条）
3. 【server】修复 部分 icon 未更新、部分弹窗列表数据不能正常显示
4. 【server】修复 docker-compose 容器状态无非正确显示
5. 【agent】修复 低版本项目数据未存储节点ID
6. 【server】优化 节点项目、节点脚本操作鉴权（需要服务端缓存中有对应的数据）

------

## 2.11.0.8-beta (2024-01-09)

### 🐣 新增功能

1. 【server】新增 前端 UI 支持配置浅色、深色主题、左边菜单主题

### 🐞 解决BUG、优化功能

1. 【server】修复 容器构建 DSL 未回显任何内容
2. 【server】修复 登录页面禁用验证码失效（感谢@ccx2480）

------

## 2.11.0.7-beta (2024-01-08)

### 🐞 解决BUG、优化功能

1. 【server】升级 页面 UI 组件、VUE 版本升级到最新
2. 【server】修复 部分低频功能无法正常使用（项目备份文件管理等）
3. 【server】修复 部分执行异常未输出到操作日志文件中（感谢@闫淼淼）

### ⚠️ 注意

1. 取消全局 loading（局部loading）
2. 编辑器延迟 1 秒加载（避免样式错乱）
3. 所有快捷复制区域变小为一个点击复制图标
4. 弹窗、抽屉样式变动
5. 取消操作引导（临时）
6. 表格将跟随列内容长度自动拉伸出现横向滚动（不会折叠）
7. 个性化配置取消：【页面自动撑开、滚动条显示、页面导航】
8. 新版本前端 node 版本推荐：18.19.0
9. json viewer 还未实现

------

## 2.11.0.6-beta (2024-01-05)

### 🐞 解决BUG、优化功能

1. 【all】优化 日志记录器提升日志记录性能
2. 【server】优化 取消/停止构建采用异常来打断子进程
3. 【server】修复 本地构建无法取消
4. 【server】修复 服务端脚本触发器、节点脚本触发器提示找不到用户（感谢@LYY）

------

## 2.11.0.5-beta (2024-01-04)

### 🐣 新增功能

1. 【server】新增 工作空间管理中新增概括总览页面

### 🐞 解决BUG、优化功能

1. 【server】优化 支持批量删除构建信息（感谢@奇奇）
2. 【server】修复 删除项目、删除分发检查关联构建失败问题
3. 【all】优化 关闭 Process 方式
4. 【server】优化 节点分发相关页面问题（感谢[@陈旭](https://gitee.com/chenxu8989) [Gitee issues I8TMDW](https://gitee.com/dromara/Jpom/issues/I8TMDW)）

------

## 2.11.0.4-beta (2024-01-03)

### 🐞 解决BUG、优化功能

1. 【server】修复 工作空间菜单配置无法使用（感谢@新）
2. 【server】优化 重新同步节点项目、节点脚本缓存交互
3. 【server】优化 SSH 脚本执行模板独立（`/exec/template.sh` -> `/ssh/template.sh`）
4. 【server】优化 服务端脚本支持加载脚本模板来实现自动加载部分环境变量

### ⚠️ 注意

如果您自定义过 SSH 脚本默认那么您需要重新同步一下脚本模板`/exec/template.sh` -> `/ssh/template.sh`

新版本 `/exec/template.sh` 中仅在服务端中生效（本地构建脚本、服务端脚本、本地发布脚本）

------

## 2.11.0.3-beta (2024-01-02)

### 🐞 解决BUG、优化功能

1. 【server】修复 没有对应的工作空间权限

------

## 2.11.0.2-beta (2024-01-02)

### 🐞 解决BUG、优化功能

1. 【all】修复 环境变量为 null 是未忽略

------

## 2.11.0.1-beta (2024-01-02)

### 🐣 新增功能

1. 【all】新增 项目支持软链其他项目（代替项目副本）

### 🐞 解决BUG、优化功能

1. 【server】修复 新版页面漏掉项目复制按钮
2. 【server】优化 逻辑节点中项目数和脚本数仅显示当前工作空间数量
3. 【server】优化 项目编辑和节点分发页面支持快捷配置授权目录
4. 【server】优化 项目编辑支持切换节点（快速同步其他节点项目）
5. 【server】修复 没有工作空间权限时页面循环跳转（感谢[@王先生](https://gitee.com/whz_gmg1) [Gitee issues I8RR01](https://gitee.com/dromara/Jpom/issues/I8RR01)）
6. 【all】优化 授权目录判断逻辑
7. 【agent】取消 插件端授权目录关闭包含判断(`jpom.whitelist.check-starts-with`)
8. 【server】优化 触发器清理优化、删除用户主动删除关联触发器
9. 【server】优化 DSL 项目控制台支持快捷编辑节点脚本（查看流程信息）
10. 【server】修复 项目触发器无法调用

### ⚠️ 注意

1. 如果您配置了授权目录但是保存项目报错您可以尝试重新报错一下授权目录来自动修复授权目录配置数据
2. 项目控制台日志默认路径调整为插件端数据目录下`project-log/${projectId}/${projectId}.log`
3. 项目控制台日志备份默认路径调整为插件端数据目录下`project-log/${projectId}/back/${projectId}-xxxxxxx.log`

------

## 2.11.0.0-beta (2023-12-29)

### 🐣 新增功能

1. 【server】新增 节点分发可以指定构建历史产物发布
2. 【server】新增 节点分发可以指定文件中心发布
3. 【server】新增 DSL 项目新增 reload 事件（可以开启文件变动触发）
4. 【server】新增 静态文件授权服务端指定目录到工作空间来管理（分发）(感谢@*斌)
5. 【server】新增 节点分发可以指定静态文件发布

### 🐞 解决BUG、优化功能

1. 【server】修复 项目列表批量操作弹窗定时刷新引起异常（感谢@曾梦想仗剑走天涯）
2. 【all】下架 全面下架项目副本功能（请使用 DSL 模式代替）
3. 【all】下架 全面节点证书管理功能（请使用工作空间证书代替）
4. 【all】下架 全面架节点 NGINX 管理功能（请使用 DSL 模式代替）
5. 【server】优化 **节点管理仅保留项目管理、脚本管理、脚本日志（其他功能迁移到机器资产管理）**
6. 【server】修复 项目复制按钮点击无响应
7. 【all】优化 查看插件端和服务端的系统日志 websocket 地址
8. 【server】优化 监控机器系统负载保留2位小数
9. 【server】下架 取消节点管理员权限
10. 【server】修复 文件变动触发器不生效的问题
11. 【all】优化 项目操作接口合并（4 合 1）
12. 【server】优化 配置授权目录需要使用到绝对路径

### ⚠️ 注意

1. 全面下架项目副本功能（请使用 DSL 模式代替）如果您当前使用到此功能请先手动备份相关数据
2. 升级后项目副本数据会被人工或者系统更新项目数据自动删除（请一定提前做好备份操作）
3. 全面下架节点证书管理功能（请使用工作空间证书代替）如果您当前使用到此功能请先手动备份相关数据
4. 全面下架全下架节点 NGINX 管理功能（请使用 DSL 模式代替）如果您当前使用到此功能请先手动备份相关数据

> ❓ 为什么要下架上述功能：由于版本迭代已经有更好的新功能可以代替之前旧功能，并且新功能从另一种角度更方便。下架也是为了我们后续版本维护迭代更高效

- 【白名单】关键词统一调整为【授权】
- 【黑名单】关键词统一调整为【禁止】

------