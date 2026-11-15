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
import type { JpomConfig } from "../config.js";
import type { JpomClient } from "../http.js";

export interface ToolContext {
  client: JpomClient;
  config: JpomConfig;
  maxChars: number;
}

export type ToolRegistrar = (server: McpServer, ctx: ToolContext) => void;
