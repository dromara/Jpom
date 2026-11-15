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

export function registerUserTools(server: McpServer, ctx: ToolContext): void {
  server.registerTool(
    "jpom_users",
    {
      title: "Jpom users",
      description:
        "List Jpom users (account, permission groups, disabled state and last login info). Requires system-level permission. Paginated (page 1, limit 20 by default; limit max 200).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/user/get_user_list", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_permission_groups",
    {
      title: "Jpom permission groups",
      description:
        "List Jpom permission groups (role/permission configuration and bound users). Requires system-level permission. Paginated (page 1, limit 20 by default; limit max 200).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/user-permission-group/get-list", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_login_logs",
    {
      title: "Jpom login logs",
      description:
        "List Jpom login logs (user, login time, IP and result) for auditing sign-in activity. Paginated (page 1, limit 20 by default; limit max 200).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/user/login-log/list-data", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_operate_logs",
    {
      title: "Jpom operate logs",
      description:
        "List Jpom operation/audit logs (who did what, when, target and result) for the workspace. Paginated (page 1, limit 20 by default; limit max 200).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/user/list-operate-log-data", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_recent_logs",
    {
      title: "Jpom recent logs",
      description:
        "List the most recent Jpom operation logs without pagination paging; limit controls how many entries are returned (1-200, default 20).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { limit: pageSchema.limit },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/user/recent-log-data", {
          limit: paging(undefined, args.limit).limit,
        });
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );
}
