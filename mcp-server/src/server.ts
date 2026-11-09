/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 		http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { JpomConfig } from "./config.js";
import { JpomClient } from "./http.js";
import { registerAllTools } from "./tools/index.js";
import type { ToolContext } from "./tools/types.js";
import { SERVER_NAME, VERSION } from "./version.js";

export interface CreateServerOptions {
  /** Inject a client for tests. */
  client?: JpomClient;
}

const INSTRUCTIONS = `Jpom MCP server. Jpom is a lightweight DevOps platform (server + agents) for builds, deployments, projects, scripts, assets, monitoring and logs.

Configuration comes from the process environment:
- JPOM_BASE_URL: Jpom server URL, e.g. http://127.0.0.1:2122
- JPOM_TOKEN or JPOM_TOKEN_FILE: long-term user token (JPOM-USER-TOKEN)
- JPOM_WORKSPACE: workspace id (default DEFAULT)
- JPOM_READ_ONLY: true by default; set to false to enable write tools

Guidelines:
1. Prefer the typed tools (jpom_*) over the generic jpom_api_call tool.
2. Logs are large: start with jpom_system_log_read or jpom_build_log using a small maxLines,
   then use the returned line/cursor values to read more. jpom_system_log_follow opens a short
   WebSocket live window (bounded by durationMs) for tailing new lines.
3. Write tools are disabled unless JPOM_READ_ONLY=false. Before a write, echo the target
   workspace/object/action, and prefer the smallest reversible action.
4. Jpom returns business errors in a JSON envelope; tools surface them as isError results.
`;

export function createServer(config: JpomConfig, options: CreateServerOptions = {}): McpServer {
  const server = new McpServer(
    { name: SERVER_NAME, version: VERSION },
    { instructions: INSTRUCTIONS },
  );
  const client = options.client ?? new JpomClient(config);
  const ctx: ToolContext = { client, config, maxChars: config.maxResultChars };
  registerAllTools(server, ctx);
  return server;
}
