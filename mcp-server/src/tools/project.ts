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

const projectSchema = {
  nodeId: z.string().min(1).describe("Jpom node id that owns the projects (from jpom_nodes)."),
  ...pageSchema,
};

export function registerProjectTools(server: McpServer, ctx: ToolContext): void {
  server.registerTool(
    "jpom_projects",
    {
      title: "Jpom node projects",
      description:
        "List the projects managed by a Jpom node, paginated. Use the returned project id with jpom_project_info for details.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...projectSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const nodeId = requireText("nodeId", args.nodeId);
        const response = await ctx.client.postForm("/node/project_list", {
          nodeId,
          ...paging(args.page, args.limit),
        });
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_project_info",
    {
      title: "Jpom project info",
      description:
        "Get project details of a Jpom node, paginated. Without id it lists all projects of the node; with id it narrows the result to that project (run mode, paths, JVM args, status, etc.).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {
        ...projectSchema,
        id: z.string().min(1).optional().describe("Project id to filter by; omit to list all projects of the node."),
      },
    },
    async (args) =>
      safeToolCall(async () => {
        const nodeId = requireText("nodeId", args.nodeId);
        const response = await ctx.client.postForm("/node/manage/get_project_info", {
          nodeId,
          id: args.id,
          ...paging(args.page, args.limit),
        });
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );
}
