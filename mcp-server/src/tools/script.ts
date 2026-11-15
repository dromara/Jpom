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
import { z } from "zod";
import { jsonResult, safeToolCall } from "../result.js";
import { pageSchema, paging, requireText } from "./common.js";
import type { ToolContext } from "./types.js";

const nodeScriptSchema = {
  nodeId: z.string().min(1).describe("Jpom node id (from jpom_nodes)."),
  ...pageSchema,
};

export function registerScriptTools(server: McpServer, ctx: ToolContext): void {
  server.registerTool(
    "jpom_scripts",
    {
      title: "Jpom server scripts",
      description:
        "List server-side scripts of the current workspace, paginated. Server scripts run on the Jpom server itself; related execution logs come from jpom_script_logs.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/script/list", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_scripts_all",
    {
      title: "Jpom all server scripts",
      description: "List all server-side scripts of the current workspace without pagination (useful for small workspaces and name lookup).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {},
    },
    async () =>
      safeToolCall(async () => {
        const response = await ctx.client.get("/script/list-all");
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_script_logs",
    {
      title: "Jpom server script logs",
      description: "List server-side script execution logs (script id, trigger type, status, duration) of the current workspace, paginated.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/script_log/list", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_node_scripts",
    {
      title: "Jpom node scripts",
      description:
        "List the script templates available on a Jpom node, paginated. These scripts run on the agent; execution logs come from jpom_node_script_logs.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...nodeScriptSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const nodeId = requireText("nodeId", args.nodeId);
        const response = await ctx.client.postForm("/node/script/list_all", {
          nodeId,
          ...paging(args.page, args.limit),
        });
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_node_script_logs",
    {
      title: "Jpom node script logs",
      description: "List node (agent-side) script execution logs for a Jpom node, paginated.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...nodeScriptSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const nodeId = requireText("nodeId", args.nodeId);
        const response = await ctx.client.postForm("/node/script_log/list", {
          nodeId,
          ...paging(args.page, args.limit),
        });
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );
}
