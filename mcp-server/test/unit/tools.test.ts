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
 * MCP protocol tests: the real McpServer from src/server.ts is connected to an SDK Client
 * through InMemoryTransport, so tool registration, input validation and result formatting are
 * exercised over JSON-RPC exactly like a real MCP client would.
 */
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
import type { CallToolResult } from "@modelcontextprotocol/sdk/types.js";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { JpomConfig } from "../../src/config.js";
import { createServer } from "../../src/server.js";

const EXPECTED_TOOLS = [
  "jpom_workspaces",
  "jpom_workspace_list",
  "jpom_workspace_env",
  "jpom_nodes",
  "jpom_node_groups",
  "jpom_node_monitor",
  "jpom_node_processes",
  "jpom_node_machine_info",
  "jpom_projects",
  "jpom_project_info",
  "jpom_builds",
  "jpom_build_get",
  "jpom_build_history",
  "jpom_build_groups",
  "jpom_repositories",
  "jpom_repository_groups",
  "jpom_build_branches",
  "jpom_scripts",
  "jpom_scripts_all",
  "jpom_script_logs",
  "jpom_node_scripts",
  "jpom_node_script_logs",
  "jpom_assets",
  "jpom_machines",
  "jpom_ssh_assets",
  "jpom_ftp_assets",
  "jpom_docker_assets",
  "jpom_monitors",
  "jpom_dispatch",
  "jpom_dispatch_logs",
  "jpom_system_info",
  "jpom_check_version",
  "jpom_system_backups",
  "jpom_clusters",
  "jpom_log_read_list",
  "jpom_users",
  "jpom_permission_groups",
  "jpom_login_logs",
  "jpom_operate_logs",
  "jpom_recent_logs",
  "jpom_system_log_files",
  "jpom_system_log_read",
  "jpom_system_log_follow",
  "jpom_build_log",
  "jpom_script_log",
  "jpom_node_script_log",
  "jpom_build_start",
  "jpom_build_cancel",
  "jpom_build_rerelease",
  "jpom_project_operate",
  "jpom_node_kill_pid",
  "jpom_system_upgrade",
  "jpom_api_call",
];

const WRITE_TOOLS = [
  "jpom_build_start",
  "jpom_build_cancel",
  "jpom_build_rerelease",
  "jpom_project_operate",
  "jpom_node_kill_pid",
  "jpom_system_upgrade",
];

const WRITE_CALLS: Record<string, Record<string, unknown>> = {
  jpom_build_start: { id: "build-1" },
  jpom_build_cancel: { id: "build-1" },
  jpom_build_rerelease: { logId: "log-1" },
  jpom_project_operate: { nodeId: "node-1", id: "project-1", opt: "start" },
  jpom_node_kill_pid: { nodeId: "node-1", pid: "1234" },
  jpom_system_upgrade: {},
};

interface RecordedCall {
  url: URL;
  init: RequestInit;
}

interface Harness {
  client: Client;
  calls: RecordedCall[];
  close: () => Promise<void>;
}

type Route = (url: URL, init: RequestInit) => Response | Promise<Response>;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json;charset=UTF-8" },
  });
}

function defaultRoute(url: URL): Response {
  if (url.pathname === "/user/my-workspace") {
    return jsonResponse({ code: 200, msg: "", data: [{ id: "DEFAULT", name: "default" }] });
  }
  if (url.pathname === "/system/log_download") {
    return new Response("2026-01-01 line one\n2026-01-01 line two\n2026-01-01 line three\n", {
      status: 200,
      headers: { "content-type": "text/plain" },
    });
  }
  return jsonResponse({ code: 200, msg: "", data: { result: [], total: 0 } });
}

function testConfig(overrides: Partial<JpomConfig> = {}): JpomConfig {
  return {
    baseUrl: "http://jpom.test:2122",
    token: "test-token",
    workspace: "WS1",
    readOnly: true,
    timeoutMs: 5000,
    maxResultChars: 50000,
    verifyTls: true,
    ...overrides,
  };
}

async function connect(route: Route = defaultRoute, overrides: Partial<JpomConfig> = {}): Promise<Harness> {
  const calls: RecordedCall[] = [];
  const fetchMock = vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
    const url = new URL(String(input));
    const requestInit = init ?? {};
    calls.push({ url, init: requestInit });
    return route(url, requestInit);
  });
  vi.stubGlobal("fetch", fetchMock);

  const config = testConfig(overrides);
  const server = createServer(config);
  const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
  const client = new Client({ name: "jpom-mcp-test", version: "0.0.0" }, { capabilities: {} });
  await Promise.all([server.connect(serverTransport), client.connect(clientTransport)]);

  return {
    client,
    calls,
    close: async () => {
      await client.close();
      await server.close();
    },
  };
}

function textOf(result: CallToolResult): string {
  const first = result.content?.[0] as { type?: string; text?: string } | undefined;
  expect(first?.type).toBe("text");
  return first?.text ?? "";
}

const cleanups: Array<() => Promise<void>> = [];

afterEach(async () => {
  while (cleanups.length > 0) {
    const cleanup = cleanups.pop();
    if (cleanup) await cleanup();
  }
  vi.unstubAllGlobals();
});

async function setup(route?: Route, overrides?: Partial<JpomConfig>): Promise<Harness> {
  const harness = await connect(route, overrides);
  cleanups.push(harness.close);
  return harness;
}

function findTool(tools: Array<{ name: string; description?: string; annotations?: Record<string, unknown> }>, name: string) {
  const tool = tools.find((candidate) => candidate.name === name);
  expect(tool, `tool ${name} is not registered`).toBeDefined();
  return tool;
}

describe("MCP tool registration", () => {
  it("registers all expected Jpom tools", async () => {
    const harness = await setup();
    const { tools } = await harness.client.listTools();
    const names = tools.map((tool) => tool.name);

    for (const expected of EXPECTED_TOOLS) {
      expect(names, `missing expected tool ${expected}`).toContain(expected);
    }
    expect(new Set(names).size).toBe(names.length);
    expect(names.length).toBeGreaterThanOrEqual(EXPECTED_TOOLS.length);
  }, 30000);

  it("gives every tool a description and an object input schema", async () => {
    const harness = await setup();
    const { tools } = await harness.client.listTools();
    expect(tools.length).toBeGreaterThan(0);

    for (const tool of tools) {
      expect(typeof tool.name).toBe("string");
      expect(tool.name.startsWith("jpom_")).toBe(true);
      expect(typeof tool.description).toBe("string");
      expect((tool.description ?? "").length).toBeGreaterThan(10);
      expect(tool.inputSchema).toBeTypeOf("object");
      expect(tool.inputSchema.type).toBe("object");
    }
  }, 30000);

  it("marks read tools as read-only and write tools as destructive", async () => {
    const harness = await setup();
    const { tools } = await harness.client.listTools();

    const readOnly = findTool(tools, "jpom_workspaces");
    expect(readOnly.annotations?.readOnlyHint).toBe(true);

    for (const name of WRITE_TOOLS) {
      const tool = findTool(tools, name);
      expect(tool.annotations?.readOnlyHint, `${name} must not be readOnly`).toBe(false);
      expect(tool.annotations?.destructiveHint, `${name} must be destructive`).toBe(true);
    }
  }, 30000);
});

describe("MCP read-only tools over in-memory transport", () => {
  it("jpom_workspaces sends token/workspace headers and returns the workspace list", async () => {
    const harness = await setup();
    const result = (await harness.client.callTool({ name: "jpom_workspaces", arguments: {} })) as CallToolResult;

    expect(result.isError).toBeFalsy();
    expect(JSON.parse(textOf(result))).toEqual([{ id: "DEFAULT", name: "default" }]);

    expect(harness.calls).toHaveLength(1);
    const [{ url, init }] = harness.calls;
    expect(url.pathname).toBe("/user/my-workspace");
    expect(init.method).toBe("GET");
    const headers = init.headers as Record<string, string>;
    expect(headers["JPOM-USER-TOKEN"]).toBe("test-token");
    expect(headers.workspaceId).toBe("WS1");
  }, 30000);

  it("jpom_nodes forwards pagination as a form-encoded POST", async () => {
    const harness = await setup();
    const result = (await harness.client.callTool({
      name: "jpom_nodes",
      arguments: { page: 2, limit: 5 },
    })) as CallToolResult;

    expect(result.isError).toBeFalsy();
    expect(JSON.parse(textOf(result))).toEqual({ result: [], total: 0 });

    const [{ url, init }] = harness.calls;
    expect(url.pathname).toBe("/node/list_data.json");
    expect(init.method).toBe("POST");
    expect(init.body).toBe("page=2&limit=5");
    expect((init.headers as Record<string, string>)["Content-Type"]).toContain(
      "application/x-www-form-urlencoded",
    );
  }, 30000);

  it("jpom_projects passes nodeId through", async () => {
    const harness = await setup();
    const result = (await harness.client.callTool({
      name: "jpom_projects",
      arguments: { nodeId: "node-1", page: 1, limit: 10 },
    })) as CallToolResult;

    expect(result.isError).toBeFalsy();
    const [{ url, init }] = harness.calls;
    expect(url.pathname).toBe("/node/project_list");
    expect(init.body).toBe("nodeId=node-1&page=1&limit=10");
  }, 30000);

  it("jpom_system_log_files reads the server log directory", async () => {
    const harness = await setup();
    const result = (await harness.client.callTool({
      name: "jpom_system_log_files",
      arguments: {},
    })) as CallToolResult;

    expect(result.isError).toBeFalsy();
    expect(JSON.parse(textOf(result))).toEqual({ result: [], total: 0 });
    expect(harness.calls[0].url.pathname).toBe("/system/log_data.json");
    expect(harness.calls[0].init.method).toBe("POST");
  }, 30000);

  it("jpom_system_log_read returns the tail of a text log file", async () => {
    const harness = await setup();
    const result = (await harness.client.callTool({
      name: "jpom_system_log_read",
      arguments: { path: "server.log", tailLines: 2 },
    })) as CallToolResult;

    expect(result.isError).toBeFalsy();
    const payload = JSON.parse(textOf(result)) as {
      path: string;
      returnedLines: number;
      lines: string[];
    };
    expect(payload.path).toBe("server.log");
    expect(payload.lines).toEqual(["2026-01-01 line two", "2026-01-01 line three"]);
    expect(payload.returnedLines).toBe(2);

    const [{ url, init }] = harness.calls;
    expect(url.pathname).toBe("/system/log_download");
    expect(url.searchParams.get("path")).toBe("server.log");
    expect(init.method).toBe("GET");
  }, 30000);

  it("jpom_node_script_logs passes nodeId and pagination", async () => {
    const harness = await setup();
    const result = (await harness.client.callTool({
      name: "jpom_node_script_logs",
      arguments: { nodeId: "node-9", page: 1, limit: 5 },
    })) as CallToolResult;

    expect(result.isError).toBeFalsy();
    const [{ url, init }] = harness.calls;
    expect(url.pathname).toBe("/node/script_log/list");
    expect(init.body).toBe("nodeId=node-9&page=1&limit=5");
  }, 30000);

  it("jpom_api_call allows GET requests in read-only mode", async () => {
    const harness = await setup();
    const result = (await harness.client.callTool({
      name: "jpom_api_call",
      arguments: { method: "GET", path: "/system/info" },
    })) as CallToolResult;

    expect(result.isError).toBeFalsy();
    expect(harness.calls).toHaveLength(1);
    expect(harness.calls[0].url.pathname).toBe("/system/info");
    expect(harness.calls[0].init.method).toBe("GET");
  }, 30000);

  it("truncates tool output to config.maxResultChars", async () => {
    const harness = await setup(
      () =>
        jsonResponse({
          code: 200,
          data: { text: "y".repeat(5000) },
        }),
      { maxResultChars: 2000 },
    );
    const result = (await harness.client.callTool({
      name: "jpom_workspaces",
      arguments: {},
    })) as CallToolResult;

    const body = textOf(result);
    expect(body).toContain("[truncated, total");
    expect(body.length).toBeLessThan(2200);
  }, 30000);
});

describe("MCP write tools are gated by JPOM_READ_ONLY", () => {
  it("refuses every write tool by default without sending a request", async () => {
    const harness = await setup();

    for (const name of WRITE_TOOLS) {
      const result = (await harness.client.callTool({
        name,
        arguments: WRITE_CALLS[name],
      })) as CallToolResult;
      expect(result.isError, `${name} must be refused in read-only mode`).toBe(true);
      expect(textOf(result)).toContain("JPOM_READ_ONLY");
      expect(textOf(result)).toContain("No request was sent");
    }
    expect(harness.calls).toHaveLength(0);
  }, 30000);

  it("allows write tools to call the API when JPOM_READ_ONLY=false", async () => {
    const harness = await setup(defaultRoute, { readOnly: false });
    const result = (await harness.client.callTool({
      name: "jpom_build_start",
      arguments: { id: "build-1", branchName: "dev" },
    })) as CallToolResult;

    expect(result.isError).toBeFalsy();
    expect(harness.calls).toHaveLength(1);
    const [{ url, init }] = harness.calls;
    expect(url.pathname).toBe("/build/manage/start");
    expect(init.method).toBe("POST");
    expect(init.body).toBe("id=build-1&branchName=dev");
  }, 30000);

  it("jpom_api_call refuses non-whitelisted POST in read-only mode without a request", async () => {
    const harness = await setup();
    const result = (await harness.client.callTool({
      name: "jpom_api_call",
      arguments: { method: "POST", path: "/build/manage/start", form: { id: "build-1" } },
    })) as CallToolResult;

    expect(result.isError).toBe(true);
    expect(textOf(result)).toContain("read-only whitelist");
    expect(harness.calls).toHaveLength(0);
  }, 30000);

  it("jpom_api_call allows whitelisted read-only POST endpoints", async () => {
    const harness = await setup();
    const result = (await harness.client.callTool({
      name: "jpom_api_call",
      arguments: { method: "POST", path: "/system/info" },
    })) as CallToolResult;

    expect(result.isError).toBeFalsy();
    expect(harness.calls).toHaveLength(1);
    expect(harness.calls[0].url.pathname).toBe("/system/info");
  }, 30000);

  it("jpom_api_call requires confirm for non-whitelisted POST when writes are enabled", async () => {
    const harness = await setup(defaultRoute, { readOnly: false });

    const unconfirmed = (await harness.client.callTool({
      name: "jpom_api_call",
      arguments: { method: "POST", path: "/build/manage/start" },
    })) as CallToolResult;
    expect(unconfirmed.isError).toBe(true);
    expect(textOf(unconfirmed)).toContain("confirm: true");
    expect(harness.calls).toHaveLength(0);

    const confirmed = (await harness.client.callTool({
      name: "jpom_api_call",
      arguments: { method: "POST", path: "/build/manage/start", form: { id: "build-1" }, confirm: true },
    })) as CallToolResult;
    expect(confirmed.isError).toBeFalsy();
    expect(harness.calls).toHaveLength(1);
    expect(harness.calls[0].url.pathname).toBe("/build/manage/start");
  }, 30000);
});

describe("MCP input validation", () => {
  it("rejects out-of-range arguments without calling the API", async () => {
    const harness = await setup();
    const result = (await harness.client.callTool({
      name: "jpom_workspace_env",
      arguments: { page: 0 },
    })) as CallToolResult;

    expect(result.isError).toBe(true);
    expect(textOf(result).toLowerCase()).toContain("validation");
    expect(harness.calls).toHaveLength(0);
  }, 30000);

  it("rejects missing required arguments without calling the API", async () => {
    const harness = await setup();
    const result = (await harness.client.callTool({
      name: "jpom_projects",
      arguments: {},
    })) as CallToolResult;

    expect(result.isError).toBe(true);
    expect(textOf(result).toLowerCase()).toContain("validation");
    expect(harness.calls).toHaveLength(0);
  }, 30000);

  it("reports unknown tools as errors", async () => {
    const harness = await setup();
    const result = (await harness.client.callTool({
      name: "jpom_does_not_exist",
      arguments: {},
    })) as CallToolResult;

    expect(result.isError).toBe(true);
    expect(textOf(result)).toMatch(/not found/i);
    expect(harness.calls).toHaveLength(0);
  }, 30000);

  it("jpom_api_call rejects unsafe paths", async () => {
    const harness = await setup();
    const cases = ["https://evil.example/api", "/api/node/list", "system/info"];

    for (const path of cases) {
      const result = (await harness.client.callTool({
        name: "jpom_api_call",
        arguments: { method: "GET", path },
      })) as CallToolResult;
      expect(result.isError, `path ${path} must be rejected`).toBe(true);
    }
    expect(harness.calls).toHaveLength(0);
  }, 30000);
});
