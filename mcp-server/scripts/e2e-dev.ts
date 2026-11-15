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
 * End-to-end smoke test for the built MCP server over the real stdio transport.
 *
 * It spawns `node dist/index.js` with StdioClientTransport, lists the registered tools and
 * calls a set of READ-ONLY tools against a real Jpom server. Run `npm run build` first.
 *
 * Environment (required):
 *   JPOM_E2E_BASE_URL   Jpom server URL, e.g. http://127.0.0.1:2122
 *   JPOM_E2E_TOKEN      Jpom long-term user token (or JPOM_E2E_TOKEN_FILE)
 * Environment (optional):
 *   JPOM_E2E_TOKEN_FILE file containing the token (used when JPOM_E2E_TOKEN is unset)
 *   JPOM_E2E_WORKSPACE  workspace id (default DEFAULT)
 *
 * Output: a single JSON report on stdout: { ok, toolCount, checks }. Exit code 1 on failure.
 * The token is never printed.
 */
import { existsSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { getDefaultEnvironment, StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import type { CallToolResult } from "@modelcontextprotocol/sdk/types.js";

interface CheckResult {
  tool: string;
  ok: boolean;
  detail?: string;
  error?: string;
}

interface E2eReport {
  ok: boolean;
  toolCount: number;
  checks: CheckResult[];
  missingTools?: string[];
  error?: string;
}

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

function fail(message: string): never {
  throw new Error(message);
}

function resolveToken(): string {
  const token = (process.env.JPOM_E2E_TOKEN ?? "").trim();
  if (token !== "") return token;
  const tokenFile = (process.env.JPOM_E2E_TOKEN_FILE ?? "").trim();
  if (tokenFile === "") fail("JPOM_E2E_TOKEN or JPOM_E2E_TOKEN_FILE is required");
  if (!existsSync(tokenFile)) fail(`JPOM_E2E_TOKEN_FILE does not exist: ${tokenFile}`);
  const fromFile = readFileSync(tokenFile, "utf8").replace(/\s+/g, "");
  if (fromFile === "") fail("JPOM_E2E_TOKEN_FILE is empty");
  return fromFile;
}

function textOf(result: CallToolResult): string {
  const first = result.content?.[0] as { type?: string; text?: string } | undefined;
  return first?.text ?? "";
}

function parseJson<T>(result: CallToolResult): T {
  const text = textOf(result);
  try {
    return JSON.parse(text) as T;
  } catch {
    fail(`tool returned non-JSON content: ${text.slice(0, 200)}`);
  }
}

function requiredString(value: unknown, label: string): string {
  if (typeof value !== "string" || value === "") fail(`missing ${label} in tool response`);
  return value;
}

const checks: CheckResult[] = [];
let toolCount = 0;

async function main(): Promise<void> {
  const baseUrl = (process.env.JPOM_E2E_BASE_URL ?? "").trim();
  if (baseUrl === "") fail("JPOM_E2E_BASE_URL is required");
  const token = resolveToken();
  const workspace = (process.env.JPOM_E2E_WORKSPACE ?? "").trim() || "DEFAULT";

  const serverEntry = fileURLToPath(new URL("../dist/index.js", import.meta.url));
  if (!existsSync(serverEntry)) {
    fail(`${serverEntry} not found; run "npm run build" first`);
  }

  const env: Record<string, string> = { ...getDefaultEnvironment() };
  env.JPOM_BASE_URL = baseUrl;
  env.JPOM_TOKEN = token;
  env.JPOM_WORKSPACE = workspace;
  env.JPOM_READ_ONLY = "true";

  const transport = new StdioClientTransport({
    command: process.execPath,
    args: [serverEntry],
    env,
    cwd: fileURLToPath(new URL("..", import.meta.url)),
    stderr: "inherit",
  });
  const client = new Client(
    { name: "jpom-mcp-e2e", version: "0.0.0" },
    { capabilities: {} },
  );

  const run = async (tool: string, args: Record<string, unknown>, detail?: string): Promise<CallToolResult> => {
    try {
      const result = (await client.callTool({ name: tool, arguments: args })) as CallToolResult;
      if (result.isError) fail(textOf(result) || "tool returned isError");
      return result;
    } catch (error) {
      checks.push({ tool, ok: false, detail, error: error instanceof Error ? error.message : String(error) });
      throw error;
    }
  };

  try {
    await client.connect(transport);

    const { tools } = await client.listTools();
    toolCount = tools.length;
    const names = new Set(tools.map((tool) => tool.name));
    const missingTools = EXPECTED_TOOLS.filter((name) => !names.has(name));

    // 1) Workspaces
    const workspaces = parseJson<Array<{ id?: string; name?: string }>>(
      await run("jpom_workspaces", {}),
    );
    if (!Array.isArray(workspaces) || workspaces.length === 0) fail("jpom_workspaces returned no workspaces");
    checks.push({ tool: "jpom_workspaces", ok: true, detail: `${workspaces.length} workspace(s)` });

    // 2) Nodes -> nodeId for dependent tools
    const nodes = parseJson<{ result?: Array<{ id?: string; name?: string }>; total?: number }>(
      await run("jpom_nodes", { page: 1, limit: 20 }),
    );
    if (!Array.isArray(nodes.result) || nodes.result.length === 0) fail("jpom_nodes returned no nodes");
    const nodeId = requiredString(nodes.result[0].id, "node id");
    checks.push({ tool: "jpom_nodes", ok: true, detail: `${nodes.result.length} node(s), first=${nodeId}` });

    // 3) Projects of the first node
    const projects = parseJson<{ result?: unknown[]; total?: number }>(
      await run("jpom_projects", { nodeId, page: 1, limit: 10 }),
    );
    if (!Array.isArray(projects.result)) fail("jpom_projects response has no result array");
    checks.push({
      tool: "jpom_projects",
      ok: true,
      detail: `total=${projects.total ?? projects.result.length}`,
    });

    // 4) System log files
    const logFiles = parseJson<Array<{ path?: string }>>(await run("jpom_system_log_files", {}));
    if (!Array.isArray(logFiles)) fail("jpom_system_log_files response is not an array");
    checks.push({ tool: "jpom_system_log_files", ok: true, detail: `${logFiles.length} entries` });

    // 5) Read the tail of server.log
    const logRead = parseJson<{ lines?: string[]; returnedLines?: number }>(
      await run("jpom_system_log_read", { path: "server.log", tailLines: 20 }),
    );
    if (!Array.isArray(logRead.lines)) fail("jpom_system_log_read response has no lines array");
    checks.push({
      tool: "jpom_system_log_read",
      ok: true,
      detail: `${logRead.lines.length} line(s) from server.log`,
    });

    // 6) Node script execution logs
    const nodeScriptLogs = parseJson<{ result?: unknown[]; total?: number }>(
      await run("jpom_node_script_logs", { nodeId, page: 1, limit: 5 }),
    );
    if (!Array.isArray(nodeScriptLogs.result)) fail("jpom_node_script_logs response has no result array");
    checks.push({
      tool: "jpom_node_script_logs",
      ok: true,
      detail: `total=${nodeScriptLogs.total ?? nodeScriptLogs.result.length}`,
    });

    const report: E2eReport = {
      ok: missingTools.length === 0 && checks.every((check) => check.ok),
      toolCount,
      checks,
    };
    if (missingTools.length > 0) report.missingTools = missingTools;
    process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
    if (!report.ok) process.exitCode = 1;
  } finally {
    await client.close().catch(() => {
      // ignore close errors
    });
  }
}

main().catch((error: unknown) => {
  const message = error instanceof Error ? error.message : String(error);
  process.stderr.write(`[e2e-dev] ${message}\n`);
  const report: E2eReport = {
    ok: false,
    toolCount,
    checks,
    error: message,
  };
  process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
  process.exitCode = 1;
});
