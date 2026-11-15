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
import { paging, pageSchema } from "./common.js";
import type { ToolContext } from "./types.js";

type AssetType = "machine" | "ssh" | "ftp" | "docker";

const ASSET_TYPES = ["machine", "ssh", "ftp", "docker"] as const;

interface AssetListTool {
  name: string;
  type: AssetType;
  title: string;
  description: string;
}

const ASSET_LIST_TOOLS: readonly AssetListTool[] = [
  {
    name: "jpom_machines",
    type: "machine",
    title: "Jpom machine assets",
    description: "List Jpom machine assets (machines managed by the server, with connection status and basic info).",
  },
  {
    name: "jpom_ssh_assets",
    type: "ssh",
    title: "Jpom SSH assets",
    description: "List Jpom SSH assets (saved SSH connection configurations).",
  },
  {
    name: "jpom_ftp_assets",
    type: "ftp",
    title: "Jpom FTP assets",
    description: "List Jpom FTP assets (saved FTP/SFTP connection configurations).",
  },
  {
    name: "jpom_docker_assets",
    type: "docker",
    title: "Jpom Docker assets",
    description: "List Jpom Docker assets (saved Docker daemon connection configurations).",
  },
];

function registerAssetListTool(
  server: McpServer,
  ctx: ToolContext,
  tool: AssetListTool,
): void {
  server.registerTool(
    tool.name,
    {
      title: tool.title,
      description: tool.description,
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: { ...pageSchema },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm(
          `/system/assets/${tool.type}/list-data`,
          paging(args.page, args.limit),
        );
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );
}

export function registerAssetTools(server: McpServer, ctx: ToolContext): void {
  server.registerTool(
    "jpom_assets",
    {
      title: "Jpom assets of one type",
      description:
        "List Jpom assets of a given type: machine, ssh, ftp or docker. Paginated (page 1, limit 20 by default; limit max 200). Use jpom_machines/jpom_ssh_assets/jpom_ftp_assets/jpom_docker_assets for a fixed type.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {
        type: z.enum(ASSET_TYPES).describe("Asset type to list: machine, ssh, ftp or docker."),
        ...pageSchema,
      },
    },
    async (args) =>
      safeToolCall(async () => {
        const response = await ctx.client.postForm(
          `/system/assets/${args.type}/list-data`,
          paging(args.page, args.limit),
        );
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  for (const tool of ASSET_LIST_TOOLS) {
    registerAssetListTool(server, ctx, tool);
  }
}
