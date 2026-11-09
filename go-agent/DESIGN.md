# Go Agent 设计说明（开发中）

> 目标：用 Go 重写 Jpom 插件端（Agent），单二进制、低内存、易安装，且与 Java 插件端**功能、配置、数据、协议完全兼容**，支持同一节点在两者间平滑切换。

## 1. 兼容性契约（不可破坏）

| 维度 | 要求 |
| --- | --- |
| 配置 | 读取同一份 `conf/application.yml`（Spring Boot 风格 YAML），键名与默认值与 Java 一致；`jpom.path`、`jpom.authorize.*`、`server.port` 等语义一致 |
| 数据 | 同一 `data/` 目录：`project.json`、`script.json`、`whitelistDirectory.json`、`workspace_env_var.json`、`global-script/*.json`、`script/<id>/log/*.log`、`INSTALL.json`、`agent_authorize.json`、`remote_version.json`；schema/字段名/类型一致，写入必须原子（`store`） |
| 鉴权 | 请求头 `Jpom-Agent-Authorize: sha1(agentName+"@"+agentPwd)`；WebSocket 使用 query 参数 `Jpom-Agent-Authorize`；未授权返回 HTTP 200 + body `{code:900,msg:"授权信息错误"}` |
| 协议 | HTTP 端点与 `modules/server/.../common/forward/NodeUrl.java` 一一对应；JSON 包恒为 `{"code":200,"msg":"","data":...}`，业务错误 code=500 且 HTTP 200；工作空间头 `workspaceId`；下载接口返回文件流 |
| WebSocket | `/console`、`/script_run`、`/free-script-run`、`/system_log`、`/node_update` 的握手参数、帧格式、命令字、关闭原因与 Java 一致 |
| manifest | `/info` 的 `manifest` 字段（version/timeStamp/pid/type/port/randomId/dataPath/jarFile/osName/installId/debug/upTime）字段名与类型一致，Go 端 `type` 仍为 `Agent`、`jarFile` 填二进制路径 |
| 布局 | `agent/{bin,conf,data,logs,project-log,tmp}`；二进制可放 `bin/agent`，base = 二进制所在目录的父级 |

## 2. 包结构与负责人（写作用域，避免冲突）

| 路径 | 内容 | 负责人 |
| --- | --- | --- |
| `main.go`、`internal/{app,config,httpx,store}/`、`Makefile`、`conf/` | 进程、配置、路由/信封/鉴权、JSON 持久化 | Lead → 移交 **core-platform** |
| `internal/sysinfo/`、`internal/api/system.go` | `/info`、监控（oshi 等价）、进程/磁盘/网卡、系统配置、缓存、白名单、工作空间环境变量、系统日志、版本/升级端点 | **core-platform** |
| `internal/project/`、`internal/proc/`、`internal/api/project.go` | 项目 CRUD、启动/停止/重启/状态/端口、项目日志（tail/backup/export）、`/manage/log/*`、`releaseOutGiving`、`change-workspace-id` | **project-ops** |
| `internal/projectfile/`、`internal/api/projectfile.go` | `/manage/file/*`、`/manage/file2/*`：列表、分片上传/合并、删除/批量删除、读取、复制、压缩、下载、远程下载、新建/重命名、diff、文件备份 | **file-manage** |
| `internal/script/`、`internal/dsl/`、`internal/ws/`、`internal/api/script.go`、`internal/api/ws.go` | 脚本/脚本库 CRUD、执行（异步/同步）、执行日志、DSL 脚本、全部 WebSocket | **script-ws** |
| `script/`、`testdata/`、文档 | 打包/部署/黄金响应比对脚本、契约 testdata | **contract** / Lead |

共享包（`app`/`config`/`httpx`/`store`）由 core-platform 维护；其他成员需要新增公共能力时，先在**自己的包**里实现，确有必要再发消息给 Lead 协调，禁止各自平行修改共享包。

## 3. 实现约定

1. **JSON 字段优先用 `map[string]any` 透传**：项目/脚本文件里存在大量历史字段（DSL、link、token、outGivingProject 等），不要用 struct 裁剪；只在计算时按需取值，避免丢失字段导致 Java 端读回时数据损坏。
2. 时间统一 `2006-01-02 15:04:05`（Java `yyyy-MM-dd HH:mm:ss`）；布尔/数字类型与 Java 序列化一致（如 `pid` 是字符串）。
3. 业务错误用 `ctx.Fail(msg)`（body code=500），参数校验失败也给 500+中文消息；不要返回 HTTP 4xx/5xx（服务端传输层会当成节点异常）。
4. 文件写入先写临时文件再 rename（`store` 已封装）；目录创建 0755，`agent_authorize.json` 0600。
5. 所有新文件加 Mulan PSL v2 版权头；`gofmt` 必须干净。
6. 命令执行/杀进程/端口探测统一走 `internal/proc`（project-ops 维护）；需要 sudo 时遵循 `jpom.system.command-use-sudo`。
7. 日志用 `a.Log()`（slog），不要 fmt.Println；敏感信息不落日志。

## 4. 构建 / 测试

```bash
cd go-agent
make build                 # build/agent
make test                  # go test ./...
bash script/go.sh build ./...   # 任意 go 命令（自动把 GOCACHE 等缓存放到仓库内）
```

- 受限环境 HOME 的 Go 缓存只读，**必须**通过 `make` 或 `bash script/go.sh ...` 执行 go 命令。
- 单元测试与真实 Java agent 的黄金响应比对：由 `script/capture-golden.sh` 采集/校验（结果样本仅本地保存，不提交仓库）。
- dev 虚拟机联调由 Lead 统一部署（Go agent 先以 2124 端口 + 数据目录副本运行，不动生产 2123），成员本地自测为主。

## 5. 验收标准（每个功能域）

1. 端点/WS 行为与契约文档一致（路径、参数、响应字段、错误码）。
2. `go test ./...`、`go vet ./...`、`gofmt -l .` 干净。
3. 数据文件回写与 Java 端 schema 一致（可用真实 Java 插件端 `data/` 目录副本做自测，副本不入库）。
4. 不修改其他成员文件；共享包改动先沟通。
5. 完成后向 Lead 汇报：已实现端点清单、未实现/风险项、测试证据。