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

export function registerDispatchTools(server: McpServer, ctx: ToolContext): void {
  server.registerTool(
    "jpom_dispatch",
    {
      title: "Jpom dispatch records",
      description:
        "List Jpom dispatch (outgiving) task records: what was dispatched, target, status and result. Paginated (page 1, limit 20 by default; limit max 200).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/outgiving/dispatch-list", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_dispatch_logs",
    {
      title: "Jpom dispatch logs",
      description:
        "List Jpom dispatch execution logs (per-run output/status of dispatched features). Paginated (page 1, limit 20 by default; limit max 200).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/outgiving/log_list_data.json", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );
}
