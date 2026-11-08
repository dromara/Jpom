---
name: jpom-ops
description: >
  Jpom 运维操作 Skill（REST 全能力，只读默认）。通过 Jpom REST API 完成工作空间、节点、项目、构建、
  脚本、资产、监控、分发、备份、集群、用户与日志的查询，以及（显式授权后）触发构建、启停项目、杀进程、
  在线升级等写操作。适用于 DeepSeek Harness / Claude / Cursor 等支持指令式 Skill 的 AI 工具。
  Use when the user asks to 查看/巡检/排障 Jpom，list Jpom nodes/projects/builds/monitors/logs,
  or operate Jpom via AI. 写操作默认禁用，必须显式授权（JPOM_OPS_ALLOW_WRITE=true + --confirm）。
whenToUse: >
  用户提到 Jpom 运维、节点/项目状态、构建失败排查、脚本与操作日志、资产/监控/分发巡检，
  或要求 AI 接管 Jpom 日常查看与受控操作时使用。
  即使未明确说“skill”，只要意图是查询/巡检/操作 Jpom，也应使用。
metadata:
  project: Jpom
  scope: read-only-default + gated-write
  jpom-api-compat: "2.11.x"
---

# Jpom Ops Skill（REST 全能力版）

> **不修改 Jpom 主程序**，仅调用 Jpom 现有 REST API。
> 默认**只读**；写操作需 `JPOM_OPS_ALLOW_WRITE=true` **且** `--confirm`，并先向用户回显目标。
> 交互式/WebSocket 能力（终端、脚本在线执行、文件浏览）**不在本工具范围**，见 §5。

## 1. 前置配置（凭据，禁止入库）

| 变量 | 说明 |
|---|---|
| `JPOM_BASE_URL` | Jpom Server 地址，如 `http://127.0.0.1:2122`（通常无 `/api` 前缀，按部署实际） |
| `JPOM_TOKEN` 或 `JPOM_TOKEN_FILE` | 长期 token，请求头 `JPOM-USER-TOKEN`；改密码后失效 |
| `JPOM_WORKSPACE` | 工作空间 ID，默认 `DEFAULT`（按 token 权限选择） |

**红线**：禁止把 token/URL/密码写入仓库、日志、截图、提交信息。token 文件 `chmod 600`。

## 2. 能力清单（README/文档同源）

先运行 `./scripts/jpom-api.sh capabilities` 可获得机器可读的能力摘要。

### 2.1 只读（默认可用，2.11.13 dev 环境逐条验证）

| 域 | 命令 | 接口 |
|---|---|---|
| 工作空间 | `workspaces` / `workspace-list` | `GET /user/my-workspace`、`GET /system/workspace/list_all` |
| 环境变量 | `workspace-env` | `POST /system/workspace_env/list` |
| 节点 | `nodes` / `node-groups` / `node-machine-info` | `/node/list_data.json`、`/node/list_group_all.json`、`/node/machine-info` |
| 节点监控 | `node-monitor` / `node-process` | `/node/node_monitor_data.json`、`/node/processList` |
| 项目 | `projects` / `project-info` | `/node/project_list`、`/node/manage/get_project_info` |
| 构建 | `builds` / `build-get` / `build-history` / `build-log` | `/build/list`、`/build/get`、`/build/history/history_list.json`、`/build/manage/get-now-log` |
| 构建辅助 | `build-groups` / `repositories` / `repository-groups` / `branches` | `/build/list_group_all`、`/build/repository/list`、`/build/repository/list-group`、`/build/branch-list` |
| 服务端脚本 | `scripts` / `scripts-all` / `script-logs` | `/script/list`、`/script/list-all`、`/script_log/list` |
| 节点脚本 | `node-scripts` / `node-script-logs` | `/node/script/list_all`、`/node/script_log/list` |
| 资产 | `machines` / `ssh-assets` / `ftp-assets` / `docker-assets` | `/system/assets/*/list-data` |
| 监控 | `monitors` | `/monitor/list_data.json` |
| 分发 | `dispatch` / `dispatch-logs` | `/outgiving/dispatch-list`、`/outgiving/log_list_data.json` |
| 系统 | `system-info` / `check-version` / `server-logs` / `backups` / `clusters` | `/system/info`、`/system/check_version.json`、`/system/log_data.json`、`/system/backup/list`、`/cluster/list` |
| 用户与审计 | `users` / `permission-groups` / `login-logs` / `operate-logs` / `recent-logs` | `/user/get_user_list`、`/user-permission-group/get-list`、`/user/login-log/list-data`、`/user/list-operate-log-data`、`/user/recent-log-data` |
| 托管日志 | `log-read` | `/log-read/list` |
| 高级 | `raw <METHOD> <PATH> [--form k=v]...` | 受只读白名单限制 |

### 2.2 写操作（默认禁用；需 `JPOM_OPS_ALLOW_WRITE=true` + `--confirm`）

| 命令 | 接口 | 说明 |
|---|---|---|
| `build-start --id <buildId> [--branch-name x] [--remark x]` | `POST /build/manage/start` | 触发构建（异步，返回 logId/buildId） |
| `build-cancel --id <buildId>` | `POST /build/manage/cancel` | 取消进行中的构建 |
| `build-rerelease --log-id <logId>` | `POST /build/manage/reRelease` | 基于历史记录重新发布 |
| `project-operate --node-id <nid> --id <pid> --opt start|stop|restart` | `POST /node/manage/operate` | 项目启停重启（File 模式项目走分发流程，不支持） |
| `node-kill-pid --node-id <nid> --pid <pid>` | `POST /node/kill.json` | 结束节点上的进程 |
| `system-upgrade [--machine-id <id>]` | `GET /system/remote_upgrade.json` | **真实触发在线升级**，最高风险 |

写操作执行前必须：回显「环境 / 工作空间 / 对象 ID / 动作」→ 用户明确同意 → 再执行；禁止批量/无人值守。

**真实验证状态**（2026-11-08，dev 环境 Jpom 2.11.13，真实执行非模拟）：

| 命令 | 验证结果 |
|---|---|
| `build-start` | ✅ 触发成功，构建 status=2，日志含脚本输出 |
| `build-cancel` | ✅ 长任务取消，status=7（取消构建） |
| `build-rerelease` | ✅ 基于历史 logId 重新发布，返回新构建号 |
| `project-operate start/stop/restart` | ✅ 真实进程 PID 变化、停止后进程退出 |
| `node-kill-pid` | ✅ 指定 PID 被结束（参数为 `nodeId`+`pid`） |
| `system-upgrade` | ⛔ 仅验证闸门，未真实触发升级（高风险，需人工确认后执行） |

## 3. 标准流程

1. **确认环境**：dev / 测试 / 生产？未确认不允许写操作；读操作也要标注环境。
2. **校验鉴权**：先 `workspaces` 或 `nodes --limit 1`。
3. **按意图查询**：先摘要（异常/失败置顶），再展开；输出结论 + 关键字段表 + 依据（接口/时间）。
4. **排障建议**：基于返回码/状态给下一步（构建历史→构建日志→脚本日志→节点状态→监控），不自动执行修复。
5. **写操作**：按 §2.2 流程，单对象、可回滚优先、执行后回读状态确认。

## 4. 输出与错误处理

- **严格模式**：`JPOM_STRICT_CODE=1` 时接口 `code` 非 200 以非 0 退出；默认直通（Jpom 鉴权失败也返回 HTTP 200）。
- **常见业务码**：`code=800` 登录/令牌失效；`code=405` 对象不存在（如 build id 不存在）；`code=500` 参数或路径不匹配。
- **分页**：`--page`、`--limit`；日志类接口用 `--limit` 控制条数。

## 5. 明确不在范围（避免误用）

| 能力 | 原因 |
|---|---|
| 终端（SSH / Docker CLI / 节点控制台） | WebSocket 交互通道，需人工确认，不适合 AI 自动化 |
| 脚本在线执行（服务端/节点脚本运行） | WebSocket 流程，且属高风险写操作；请用 `trigger-url` 在 UI/审批流中执行 |
| 文件浏览/下载/上传/编辑 | 高风险管理面（可读服务器任意文件），不适合默认开放 |
| 用户/权限组/系统配置/数据库备份的写操作 | 管理面变更，请在 Web UI 中由管理员操作 |

## 6. 故障排查

- `401`/`code=800`：token 无效或密码变更后失效，重新生成长期 token。
- `500 No corresponding resource found`：接口路径与版本/部署（context-path）不匹配，按源码/前端 API 校正。
- `database exception`：参数/字段不匹配，检查请求参数。
- 无数据：检查 `JPOM_WORKSPACE` 与 token 权限；分页参数是否过小。
