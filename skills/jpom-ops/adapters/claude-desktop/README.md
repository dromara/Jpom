# Claude Desktop 适配
- **MVP（Skill 模式）**：将 `SKILL.md` 作为项目指令/上下文；本机配置 `JPOM_*` 环境变量并允许执行 `scripts/jpom_api.py`（Linux/macOS 可用 `jpom-api.sh` 包装）。
- **后续（MCP）**：使用 `claude_desktop_config.example.json` 指向 `jpom-mcp-server`（`JPOM_READ_ONLY=true`）。

> ⚠️ **当前不可用**：`mcp*.json` 示例指向尚未发布的 `jpom-mcp-server`，仅作为未来 MCP 接入的配置参考;
> MVP 阶段请使用 Skill 方式（`SKILL.md` + `scripts/jpom_api.py`（Linux/macOS 可用 `jpom-api.sh` 包装））。

> 先用 `python scripts/jpom_api.py capabilities`（Linux/macOS 可用 `./scripts/jpom-api.sh capabilities`） 查看完整只读/写命令清单（默认只读）。
