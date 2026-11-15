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
import { jsonResult, safeToolCall } from "../result.js";
import { paging, pageSchema } from "./common.js";
import type { ToolContext } from "./types.js";

export function registerWorkspaceTools(server: McpServer, ctx: ToolContext): void {
  server.registerTool(
    "jpom_workspaces",
    {
      title: "Jpom workspaces of current user",
      description:
        "List the Jpom workspaces the configured token user can access. Use the returned workspace id with other tools or JPOM_WORKSPACE.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {},
    },
    async () =>
      safeToolCall(async () => {
        const response = await ctx.client.get("/user/my-workspace");
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_workspace_list",
    {
      title: "Jpom all workspaces",
      description: "List all Jpom workspaces (requires system-level permission).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {},
    },
    async () =>
      safeToolCall(async () => {
        const response = await ctx.client.get("/system/workspace/list_all");
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_workspace_env",
    {
      title: "Jpom workspace environment variables",
      description: "List environment variables configured for a Jpom workspace.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/system/workspace_env/list", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );
}
