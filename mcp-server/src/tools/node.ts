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

const nodeIdSchema = {
  nodeId: z.string().min(1).describe("Jpom node id (from jpom_nodes or jpom_node_groups)."),
};

export function registerNodeTools(server: McpServer, ctx: ToolContext): void {
  server.registerTool(
    "jpom_nodes",
    {
      title: "Jpom nodes",
      description:
        "List the Jpom nodes (agent machines) of the current workspace, paginated. The returned node id is required by the project, node-script, monitor and process tools.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/node/list_data.json", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_node_groups",
    {
      title: "Jpom node groups",
      description: "List all node group names of the current workspace (used to filter jpom_nodes by group).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {},
    },
    async () =>
      safeToolCall(async () => {
        const response = await ctx.client.get("/node/list_group_all.json");
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_node_monitor",
    {
      title: "Jpom node monitor data",
      description:
        "Get the latest monitor samples (CPU, memory, disk, network) of a Jpom node/machine. Returns up to the most recent 500 samples.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...nodeIdSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const nodeId = requireText("nodeId", args.nodeId);
        const response = await ctx.client.postForm("/node/node_monitor_data.json", { nodeId });
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_node_processes",
    {
      title: "Jpom node processes",
      description:
        "List the OS processes visible on a Jpom node (pid, command, cpu/memory usage). Works for both agent nodes and asset machines resolved by node id.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...nodeIdSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const nodeId = requireText("nodeId", args.nodeId);
        const response = await ctx.client.postForm("/node/processList", { nodeId });
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_node_machine_info",
    {
      title: "Jpom node machine info",
      description: "Get the machine (host) information registered for a Jpom node: OS, architecture, CPU, memory and agent version.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...nodeIdSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const nodeId = requireText("nodeId", args.nodeId);
        const response = await ctx.client.get("/node/machine-info", { nodeId });
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );
}
