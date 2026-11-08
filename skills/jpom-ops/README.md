# jpom-ops Skill（Jpom AI 运维，REST 全能力 / 只读默认）

通过 Jpom 现有 REST API + 长期 token（`JPOM-USER-TOKEN`）完成日常巡检与受控操作，**不修改 Jpom 主程序**。

- 默认**只读**；写操作（触发构建、启停项目、杀进程、在线升级）需 `JPOM_OPS_ALLOW_WRITE=true` **且** `--confirm`。
- 适配：DeepSeek Harness / Claude / Cursor 等（见 `adapters/`；MCP Server 规划中）。
- 官方文档：《AI 运维接入（Skill / MCP）》<https://jpom.top/pages/ai-ops-skill-mcp/>

## 快速开始

**Linux / macOS**（需 Python 3.8+；`jpom-api.sh` 是薄包装）：

```bash
export JPOM_BASE_URL=http://127.0.0.1:2122
export JPOM_TOKEN_FILE=/path/to/jpom-longterm-token   # chmod 600；或 JPOM_TOKEN=...
export JPOM_WORKSPACE=DEFAULT

./scripts/jpom-api.sh capabilities
./scripts/jpom-api.sh workspaces
./scripts/jpom-api.sh nodes
./scripts/jpom-api.sh project-info --node-id <节点ID>
./scripts/jpom-api.sh build-history
./scripts/jpom-api.sh system-info
```

**Windows（cmd / PowerShell，推荐直接用 Python）**：

```bat
set JPOM_BASE_URL=http://127.0.0.1:2122
set JPOM_TOKEN_FILE=C:\path\jpom-token
set JPOM_WORKSPACE=DEFAULT
python scripts\jpom_api.py capabilities
python scripts\jpom_api.py nodes --limit 5
```

> 同一份 `scripts/jpom_api.py` 在 Windows / Linux / macOS 行为一致；无需 bash/curl。

## 能力覆盖

| 域 | 命令 |
|---|---|
| 工作空间 | `workspaces` `workspace-list` `workspace-env` |
| 节点 | `nodes` `node-groups` `node-machine-info` `node-monitor` `node-process` |
| 项目 | `projects` `project-info` |
| 构建 | `builds` `build-get` `build-history` `build-log` `build-groups` `repositories` `repository-groups` `branches` |
| 脚本 | `scripts` `scripts-all` `script-logs` `node-scripts` `node-script-logs` |
| 资产 | `machines` `ssh-assets` `ftp-assets` `docker-assets` |
| 监控/分发/系统 | `monitors` `dispatch` `dispatch-logs` `system-info` `check-version` `server-logs` `backups` `clusters` |
| 用户与审计 | `users` `permission-groups` `login-logs` `operate-logs` `recent-logs` `log-read` |

写命令（默认禁用）：`build-start` `build-cancel` `build-rerelease` `project-operate` `node-kill-pid` `system-upgrade`。

**不在范围**（WebSocket/交互式/高风险管理面）：终端、脚本在线执行、文件浏览上传、用户/权限/系统配置写操作。

## 安全约定

- token 只从环境变量或 `chmod 600` 本地文件读取，脚本用 0600 临时 curl 配置传递，不出现在命令行。
- 写操作必须回显「环境 / 工作空间 / 对象 / 动作」并获得明确同意。
- `JPOM_STRICT_CODE=1` 可让鉴权失败等业务错误以非 0 退出。
