/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 		http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
/**
 * Read-only integration test against a real Jpom dev server.
 *
 * Skipped unless JPOM_E2E_BASE_URL and JPOM_E2E_TOKEN (or JPOM_E2E_TOKEN_FILE) are set, e.g.:
 *   JPOM_E2E_BASE_URL=http://127.0.0.1:2122 \
 *   JPOM_E2E_TOKEN_FILE=/path/to/jpom-longterm-token \
 *   npx vitest run --config vitest.config.ts --dir test/integration
 *
 * Only read-only tools are exercised. Never print the token.
 */
import { existsSync } from "node:fs";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
import type { CallToolResult } from "@modelcontextprotocol/sdk/types.js";
import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { loadConfig, type JpomConfig } from "../../src/config.js";
import { createServer } from "../../src/server.js";

const BASE_URL = (process.env.JPOM_E2E_BASE_URL ?? "").trim();
const TOKEN = (process.env.JPOM_E2E_TOKEN ?? "").trim();
const TOKEN_FILE = (process.env.JPOM_E2E_TOKEN_FILE ?? "").trim();
const HAS_ENV =
  BASE_URL !== "" && (TOKEN !== "" || (TOKEN_FILE !== "" && existsSync(TOKEN_FILE)));

const READ_TOOLS = [
  "jpom_workspaces",
  "jpom_nodes",
  "jpom_projects",
  "jpom_system_log_files",
  "jpom_system_log_read",
  "jpom_node_script_logs",
];

function devConfig(): JpomConfig {
  const env: NodeJS.ProcessEnv = {
    JPOM_BASE_URL: BASE_URL,
    JPOM_READ_ONLY: "true",
  };
  if (TOKEN !== "") env.JPOM_TOKEN = TOKEN;
  else if (TOKEN_FILE !== "") env.JPOM_TOKEN_FILE = TOKEN_FILE;
  return loadConfig(env);
}

function textOf(result: CallToolResult): string {
  const first = result.content?.[0];
  expect(first?.type).toBe("text");
  return (first as { type: "text"; text: string }).text;
}

function jsonOf<T = unknown>(result: CallToolResult): T {
  return JSON.parse(textOf(result)) as T;
}

interface ToolState {
  workspaceId?: string;
  nodeId?: string;
}

const state: ToolState = {};

let client: Client;
let close: () => Promise<void> = async () => {};

describe.skipIf(!HAS_ENV)("Jpom dev server (read-only integration)", () => {
  beforeAll(async () => {
    const config = devConfig();
    const server = createServer(config);
    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    client = new Client({ name: "jpom-mcp-integration", version: "0.0.0" }, { capabilities: {} });
    await Promise.all([server.connect(serverTransport), client.connect(clientTransport)]);
    close = async () => {
      await client.close();
      await server.close();
    };
  }, 30000);

  afterAll(async () => {
    await close();
  }, 30000);

  it("listTools exposes all read-only tools used by this suite", async () => {
    const { tools } = await client.listTools();
    const names = tools.map((tool) => tool.name);
    for (const expected of READ_TOOLS) {
      expect(names, `missing tool ${expected}`).toContain(expected);
    }
    expect(names.length).toBeGreaterThanOrEqual(READ_TOOLS.length);
  }, 30000);

  it("jpom_workspaces lists the workspaces of the token user", async () => {
    const result = await client.callTool({ name: "jpom_workspaces", arguments: {} });
    expect(result.isError).toBeFalsy();
    const workspaces = jsonOf<Array<{ id: string; name?: string }>>(result);
    expect(Array.isArray(workspaces)).toBe(true);
    expect(workspaces.length).toBeGreaterThan(0);
    const first = workspaces[0];
    expect(typeof first.id).toBe("string");
    state.workspaceId = first.id;
  }, 30000);

  it("jpom_nodes lists nodes and provides a nodeId for dependent tools", async () => {
    const result = await client.callTool({ name: "jpom_nodes", arguments: { page: 1, limit: 20 } });
    expect(result.isError).toBeFalsy();
    const data = jsonOf<{ result?: Array<{ id: string; name?: string }>; total?: number }>(result);
    expect(Array.isArray(data.result)).toBe(true);
    expect((data.result ?? []).length).toBeGreaterThan(0);
    state.nodeId = data.result?.[0]?.id;
    expect(typeof state.nodeId).toBe("string");
  }, 30000);

  it("jpom_projects lists projects of the first node", async () => {
    expect(state.nodeId, "jpom_nodes must run before jpom_projects").toBeTruthy();
    const result = await client.callTool({
      name: "jpom_projects",
      arguments: { nodeId: state.nodeId, page: 1, limit: 10 },
    });
    expect(result.isError).toBeFalsy();
    const data = jsonOf<{ result?: unknown[]; total?: number }>(result);
    expect(data).toBeTypeOf("object");
    expect(Array.isArray(data.result)).toBe(true);
  }, 30000);

  it("jpom_system_log_files lists system log files", async () => {
    const result = await client.callTool({ name: "jpom_system_log_files", arguments: {} });
    expect(result.isError).toBeFalsy();
    const files = jsonOf<Array<{ title?: string; path?: string; children?: unknown[] }>>(result);
    expect(Array.isArray(files)).toBe(true);
    expect(files.length).toBeGreaterThan(0);
    const paths = files.map((entry) => entry.path);
    expect(paths).toContain("server.log");
  }, 30000);

  it("jpom_system_log_read returns the tail of server.log", async () => {
    const result = await client.callTool({
      name: "jpom_system_log_read",
      arguments: { path: "server.log", tailLines: 20 },
    });
    expect(result.isError).toBeFalsy();
    const data = jsonOf<{
      path?: string;
      returnedLines?: number;
      lines?: string[];
      totalLines?: number;
    }>(result);
    expect(data.path).toBe("server.log");
    expect(Array.isArray(data.lines)).toBe(true);
    expect((data.lines ?? []).length).toBeGreaterThan(0);
    expect((data.lines ?? []).length).toBeLessThanOrEqual(20);
    expect(data.returnedLines).toBe((data.lines ?? []).length);
  }, 30000);

  it("jpom_node_script_logs lists node script execution logs", async () => {
    expect(state.nodeId, "jpom_nodes must run before jpom_node_script_logs").toBeTruthy();
    const result = await client.callTool({
      name: "jpom_node_script_logs",
      arguments: { nodeId: state.nodeId, page: 1, limit: 5 },
    });
    expect(result.isError).toBeFalsy();
    const data = jsonOf<{ result?: unknown[]; total?: number }>(result);
    expect(data).toBeTypeOf("object");
    expect(Array.isArray(data.result)).toBe(true);
  }, 30000);
});
