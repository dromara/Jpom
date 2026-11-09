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

export function registerSystemTools(server: McpServer, ctx: ToolContext): void {
  server.registerTool(
    "jpom_system_info",
    {
      title: "Jpom system information",
      description:
        "Get Jpom server system information: version, JVM/OS details, uptime, license/serial and other runtime facts.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {},
    },
    async () =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/system/info", {});
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_check_version",
    {
      title: "Jpom version check",
      description:
        "Check whether a newer Jpom version is available (current server version and the latest published release information).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {},
    },
    async () =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/system/check_version.json", {});
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_system_backups",
    {
      title: "Jpom system backups",
      description:
        "List Jpom system backups (database/config backup files with size and creation time). Paginated (page 1, limit 20 by default; limit max 200).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/system/backup/list", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_clusters",
    {
      title: "Jpom clusters",
      description:
        "List Jpom server clusters (cluster name, enabled state and member servers) for the configured workspace. Paginated (page 1, limit 20 by default; limit max 200).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/cluster/list", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_log_read_list",
    {
      title: "Jpom managed log files",
      description:
        "List Jpom managed log file entries (log-read): log name, path, available files and update time. Paginated (page 1, limit 20 by default; limit max 200).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/log-read/list", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );
}
