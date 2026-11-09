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

export function registerBuildTools(server: McpServer, ctx: ToolContext): void {
  server.registerTool(
    "jpom_builds",
    {
      title: "Jpom online builds",
      description:
        "List the online build configurations of the current workspace, paginated. Build ids from this list are used by jpom_build_get.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/build/list", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_build_get",
    {
      title: "Jpom build detail",
      description: "Get one online build configuration by id (repository, branch, build script, release method and last build state).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {
        id: z.string().min(1).describe("Build id (from jpom_builds)."),
      },
    },
    async (args) =>
      safeToolCall(async () => {
        const id = requireText("id", args.id);
        const response = await ctx.client.get("/build/get", { id });
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_build_history",
    {
      title: "Jpom build history",
      description:
        "List online build history records (status, duration, trigger type, branch, result) of the current workspace, paginated, newest first.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/build/history/history_list.json", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_build_groups",
    {
      title: "Jpom build groups",
      description: "List all online build group names of the current workspace.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {},
    },
    async () =>
      safeToolCall(async () => {
        const response = await ctx.client.get("/build/list_group_all");
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_repositories",
    {
      title: "Jpom build repositories",
      description:
        "List the git/svn repositories configured for online builds, paginated. Repository ids are used by jpom_build_branches.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm("/build/repository/list", paging(args.page, args.limit));
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_repository_groups",
    {
      title: "Jpom repository groups",
      description: "List all build repository group names of the current workspace.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {},
    },
    async () =>
      safeToolCall(async () => {
        const response = await ctx.client.get("/build/repository/list-group");
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_build_branches",
    {
      title: "Jpom repository branches",
      description: "List the branches available in a build repository (requires a reachable repository; returned branches can be used for builds).",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {
        repositoryId: z.string().min(1).describe("Build repository id (from jpom_repositories)."),
      },
    },
    async (args) =>
      safeToolCall(async () => {
        const repositoryId = requireText("repositoryId", args.repositoryId);
        const response = await ctx.client.postForm("/build/branch-list", { repositoryId });
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );
}
