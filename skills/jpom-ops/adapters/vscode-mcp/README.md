# VS Code / MCP 客户端适配
使用支持 MCP 的客户端配置 stdio 启动 `jpom-mcp-server`，环境变量同 Claude/Cursor 示例，务必设置 `JPOM_READ_ONLY=true`（MVP 阶段）。

> ⚠️ **当前不可用**：`mcp*.json` 示例指向尚未发布的 `jpom-mcp-server`，仅作为未来 MCP 接入的配置参考;
> MVP 阶段请使用 Skill 方式（`SKILL.md` + `scripts/jpom-api.sh`）。

> 先用 `./scripts/jpom-api.sh capabilities` 查看完整只读/写命令清单（默认只读）。
