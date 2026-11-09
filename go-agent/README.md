# Jpom Go Agent（插件端 Go 实现）

Jpom 插件端（Agent）的 Go 重写版本：单个静态二进制、低内存、无 JDK 依赖，安装即用。

设计目标是 **与 Java 插件端完全兼容**：

- 读取同一份 `conf/application.yml`（Spring Boot 风格 YAML，键名/默认值一致）；
- 使用同一个数据目录（`data/project.json`、`data/script.json`、`data/agent_authorize.json` 等，schema 一致）；
- 对服务端暴露相同的 HTTP + WebSocket 协议（端口默认 2123、`Jpom-Agent-Authorize` 鉴权、`workspaceId` 头、JSON 响应包 `{code,msg,data}`）；
- 同一节点可以在 **Java Agent ↔ Go Agent** 之间平滑切换（停止一个、用同一目录启动另一个），数据与配置无需迁移，可随时回切。

> 开发分支：`dev`。设计与兼容说明见 `DESIGN.md`，使用/切换/升级说明见官方文档站。

## 功能与兼容性（第二轮补齐后）

- **协议/数据完全兼容**：同一 `conf/application.yml`、同一 `data/` 目录、同一 HTTP/WebSocket 协议；`/info` 增加 `engine:"go"`（Java 旧服务端忽略），支持 Server 识别 Go 引擎的一键在线升级。
- **国际化**：HTTP `msg`、WS 文本/控制帧、日志 SYSTEM 行按 `Accept-Language` 输出 zh-CN / zh-HK / zh-TW / en-US（数据源为 Java 插件端 i18n properties，1759 keys，`internal/i18n`）。
- **跨平台**：Linux（/proc+/sys）、Windows、macOS 系统监控（CPU/内存/磁盘分区/网卡/进程/端口）与项目/脚本执行；发布包覆盖 linux-amd64/arm64、darwin-amd64/arm64、windows-amd64，在线升级容器同样提供 5 个平台（`agent-go-<version>-upgrade-<os>-<arch>.jar`，linux-amd64 另保留兼容别名 `agent-go-<version>-upgrade.jar`）。
- **编码兼容**：`jpom.system.log-charset`、`console-charset`、`jpom.project.log.file-charset`（Windows 默认 GBK）；文件编辑后缀 `后缀@编码`（GBK）。
- **文件/脚本能力**：7z 解压（安全条目校验）、Hutool 兼容 cron（`|` 分组、5/6/7 段含年份）、脚本 autoExecCron 定时、升级失败自动回滚。

## 目录结构

```text
go-agent/
├── main.go                 进程入口（监听、优雅退出）
├── conf/application.yml    默认配置（与 Java 插件端一致）
├── internal/
│   ├── app/                运行时：目录、manifest、授权、日志、自升级重启
│   ├── config/             application.yml 解析（键名兼容 Java）
│   ├── httpx/              路由 / JSON 响应包 / 鉴权 / 工作空间 / transport-encryption
│   ├── store/              JSON 文件持久化（原子写）
│   ├── sysinfo/            系统监控、系统配置、缓存、日志、工作空间环境变量、远程版本、在线升级
│   ├── project/            项目数据、启停/状态/日志、DSL、WebHooks
│   ├── proc/               命令执行/sudo 规则、进程/端口探测、日志 tail
│   ├── projectfile/        项目文件管理与文件备份
│   ├── script/             脚本、脚本库、执行、autoExecCron 定时
│   ├── dsl/                DSL 脚本解析与执行
│   ├── ws/                 WebSocket（console/script_run/free-script-run/system_log/node_update）
│   └── api/                端点装配
└── script/                 构建/打包/升级容器（开发/测试工具在维护者内部工作区）
```

## 构建与运行（开发机）

```bash
cd go-agent
make test          # go test ./...
make build         # 产出 build/agent（本地平台）
./build/agent -base .   # 以当前目录为 base 运行（读取 ./conf/application.yml）
```

> HOME 的 Go 构建缓存可能只读（受限环境/CI），统一使用 `make` 或 `bash script/go.sh <go 命令>`，
> 它们会把 GOCACHE/GOTMPDIR 放到仓库内 `.cache/`。

交叉编译（Linux amd64，静态链接）：`CGO_ENABLED=0 GOOS=linux GOARCH=amd64 make build`。

打包发布包（多平台 + sha1）：

```bash
VERSION=2.11.13.2 bash script/package.sh   # PLATFORMS 默认 5 平台，可显式覆盖
# dist/agent-go-<version>-release-<os>-<arch>.{zip,tar.gz,sha1}
```

生成在线升级容器包（旧版 Server 的 `checkJpomJar` 兼容，Go Agent 自升级）：

```bash
VERSION=2.11.13.2 bash script/build-upgrade-container.sh
# dist/agent-go-<version>-upgrade-<os>-<arch>.jar + .sha1（5 平台）
# dist/agent-go-<version>-upgrade.jar（linux/amd64 兼容别名）
```

## 安装布局

与 Java 插件端一致（可在同一目录共存，切换时只换启动入口）：

```text
agent/
├── bin/agent               Go 二进制
├── bin/AgentGo.sh          Go 服务脚本（start/stop/restart/status）
├── bin/Agent.sh            Java 原服务脚本（保留用于回切）
├── conf/application.yml    同一份配置
├── data/                   同一份数据（自动初始化）
├── logs/agent.log
├── project-log/
└── tmp/
```

启动：`bin/AgentGo.sh start`（生产环境请把 `JAVA_HOME/PATH` 配好，项目启动依赖 java 可执行文件）。
插件端启动成功后会写 `bin/agent.pid`，`AgentGo.sh`/`AgentGo.bat` 的 start/stop/status/restart 均以该文件为准；Windows 由服务管理器（nssm/sc）托管时，在线升级按服务配置自动重启，避免手工双实例。

## 开发约定

1. `conf/application.yml`、`data/*.json`、HTTP/WS 协议 **必须与 Java 插件端保持兼容**：
   字段名、类型、默认值、错误码都不能随意改动；新增字段只能追加且服务端可忽略。
2. 数据文件写入必须原子（`store` 包已处理），避免 Java/Go 切换时读到半个文件。
3. 所有新增 Go 文件带 Mulan PSL v2 版权头。
4. 时间格式统一 `yyyy-MM-dd HH:mm:ss`；Jackson（HTTP）long→字符串、WS/数据文件 fastjson2 long→数字。
5. 端点按功能域分文件实现，避免多人并行开发时冲突；协议说明见 `DESIGN.md`。

## 验证

- 单元测试：`make test`（构建 `make build`、静态检查 `make vet`）。
- 与真实 Java 插件端的黄金响应结构对照样本见 `testdata/`；采集/比对等开发工具由维护者内部维护，不随开源仓发布。