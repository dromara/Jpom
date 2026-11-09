# Changelog

本文件记录 `jpom-mcp-server` 的版本变更。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

## [0.1.0] - 2026-11-09

首个公开版本。Jpom MCP Server 是一个独立 sidecar：通过 stdio 与 MCP 客户端通信，通过 Jpom REST API
与 Jpom Server 交互，不修改 Jpom 主程序。

### 新增

- MCP stdio server，可被 Claude Desktop、Cursor、VS Code、DeepSeek Harness 等支持 stdio 的 MCP 客户端加载。
- 按域分组的只读工具：
  - 工作空间：`jpom_workspaces`、`jpom_workspace_list`、`jpom_workspace_env`
  - 节点：`jpom_nodes`、`jpom_node_groups`、`jpom_node_monitor`、`jpom_node_processes`、`jpom_node_machine_info`
  - 项目：`jpom_projects`、`jpom_project_info`
  - 构建：`jpom_builds`、`jpom_build_get`、`jpom_build_history`、`jpom_build_groups`、`jpom_repositories`、`jpom_repository_groups`、`jpom_build_branches`
  - 脚本：`jpom_scripts`、`jpom_scripts_all`、`jpom_script_logs`、`jpom_node_scripts`、`jpom_node_script_logs`
  - 资产：`jpom_assets`、`jpom_machines`、`jpom_ssh_assets`、`jpom_ftp_assets`、`jpom_docker_assets`
  - 监控与分发：`jpom_monitors`、`jpom_dispatch`、`jpom_dispatch_logs`
  - 系统：`jpom_system_info`、`jpom_check_version`、`jpom_system_backups`、`jpom_clusters`、`jpom_log_read_list`
  - 用户与审计：`jpom_users`、`jpom_permission_groups`、`jpom_login_logs`、`jpom_operate_logs`、`jpom_recent_logs`
  - 日志：`jpom_system_log_files`、`jpom_system_log_read`、`jpom_system_log_follow`、`jpom_project_log_follow`、`jpom_build_log`、`jpom_script_log`、`jpom_node_script_log`
- 受控写工具（默认禁用，仅 `JPOM_READ_ONLY=false` 时可用）：触发/取消/重新发布构建、项目启停重启、
  结束节点进程、在线升级；写操作回显目标并建议最小可逆动作。
- `jpom_api_call` 通用 REST 调用工具，默认只允许只读方法，写方法同样受 `JPOM_READ_ONLY` 闸门约束。
- 环境变量配置：`JPOM_BASE_URL`、`JPOM_TOKEN` / `JPOM_TOKEN_FILE`、`JPOM_WORKSPACE`、
  `JPOM_READ_ONLY`、`JPOM_TIMEOUT_MS`、`JPOM_MAX_RESULT_CHARS`、`JPOM_VERIFY_SSL`。
- 命令行入口：`jpom-mcp-server`（启动 stdio 服务）、`--version`、`--help`、`doctor`（连通性与工作空间自检）。
- 日志读取采用"快照 + 行游标 + 有界 live 窗口"：先用 tail/grep 读快照，再用 `line`/`nextLine` 续读，
  按需短时 WebSocket follow（有 `durationMs` / `maxLines` 上限），避免把无限日志流塞进模型上下文。
- 工具结果统一 JSON 化并按 `JPOM_MAX_RESULT_CHARS` 截断；Jpom 业务错误（如 `code=800`）以 `isError` 结果返回。

### 安全

- 默认只读（`JPOM_READ_ONLY=true`）：写工具与写方法直接拒绝（不发出任何请求），需显式关闭只读。
- Token 只从环境变量或只读文件读取，不写入日志、URL 或工具结果。
- `JPOM_VERIFY_SSL` 默认开启；关闭时在 stderr 打印明确告警。
- 推荐为 AI 使用单独创建最小权限、指定工作空间的长期 token。

### 说明

- 首版仅支持 **stdio** 传输，暂不提供远程 HTTP/SSE transport。
- 兼容 Jpom 2.11.x 的 REST API；连接失败或业务错误时按错误码排查（见 README「故障排查」）。
- `mcp-server` 与 Jpom 主程序独立发版，Jpom 主体仍为 Java 8 / Spring Boot 2.7。

[Unreleased]: https://gitee.com/dromara/Jpom/compare/mcp-v0.1.0...dev
[0.1.0]: https://gitee.com/dromara/Jpom/releases/tag/mcp-v0.1.0
