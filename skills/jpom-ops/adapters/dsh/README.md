# DeepSeek Harness 适配
1. 将 `skills/jpom-ops/` 安装到 DSH 可发现的 skills 目录（项目级或用户级，按 DSH 部署约定）。
2. 仅本地配置：`JPOM_BASE_URL`、`JPOM_TOKEN` 或 `JPOM_TOKEN_FILE`（chmod 600）、`JPOM_WORKSPACE`。
3. 触发：`查看 Jpom 节点状态`、`最近构建失败`、`查操作日志`。默认只读。

> ⚠️ **当前不可用**：`mcp*.json` 示例指向尚未发布的 `jpom-mcp-server`，仅作为未来 MCP 接入的配置参考;
> MVP 阶段请使用 Skill 方式（`SKILL.md` + `scripts/jpom-api.sh`）。

> 先用 `./scripts/jpom-api.sh capabilities` 查看完整只读/写命令清单（默认只读）。
