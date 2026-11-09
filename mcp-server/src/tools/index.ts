/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 		http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { ToolContext } from "./types.js";
import { registerWorkspaceTools } from "./workspace.js";
import { registerNodeTools } from "./node.js";
import { registerProjectTools } from "./project.js";
import { registerBuildTools } from "./build.js";
import { registerScriptTools } from "./script.js";
import { registerAssetTools } from "./asset.js";
import { registerMonitorTools } from "./monitor.js";
import { registerDispatchTools } from "./dispatch.js";
import { registerSystemTools } from "./system.js";
import { registerUserTools } from "./user.js";
import { registerLogTools } from "./logs.js";
import { registerWriteTools } from "./write.js";
import { registerRawTools } from "./raw.js";

export function registerAllTools(server: McpServer, ctx: ToolContext): void {
  registerWorkspaceTools(server, ctx);
  registerNodeTools(server, ctx);
  registerProjectTools(server, ctx);
  registerBuildTools(server, ctx);
  registerScriptTools(server, ctx);
  registerAssetTools(server, ctx);
  registerMonitorTools(server, ctx);
  registerDispatchTools(server, ctx);
  registerSystemTools(server, ctx);
  registerUserTools(server, ctx);
  registerLogTools(server, ctx);
  registerWriteTools(server, ctx);
  registerRawTools(server, ctx);
}
