/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 		http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { ConfigError, loadConfig } from "./config.js";
import { JpomClient } from "./http.js";
import { createServer } from "./server.js";
import { SERVER_NAME, VERSION } from "./version.js";

function printHelp(): void {
  process.stdout.write(`${SERVER_NAME} ${VERSION}

MCP server for Jpom. It speaks the Model Context Protocol over stdio and talks to a Jpom
server via its REST API.

Usage:
  jpom-mcp-server            start the MCP stdio server
  jpom-mcp-server doctor    test the Jpom connection and print the current user/workspace
  jpom-mcp-server --version print the version
  jpom-mcp-server --help    show this help

Environment:
  JPOM_BASE_URL            Jpom server URL (required), e.g. http://127.0.0.1:2122
  JPOM_TOKEN               Jpom long-term user token (or use JPOM_TOKEN_FILE)
  JPOM_TOKEN_FILE          file containing the token (chmod 600 recommended)
  JPOM_WORKSPACE           workspace id (default DEFAULT)
  JPOM_READ_ONLY           true (default) disables write tools; set false to enable
  JPOM_TIMEOUT_MS          request timeout (default 60000)
  JPOM_MAX_RESULT_CHARS    max characters per tool result (default 50000)
  JPOM_VERIFY_SSL          true (default); set false only for self-signed test servers
`);
}

async function doctor(): Promise<number> {
  const config = loadConfig();
  const client = new JpomClient(config);
  const workspaces = await client.get<Array<{ id: string; name?: string }>>("/user/my-workspace");
  const nodes = await client.postForm("/node/list_data.json", { page: 1, limit: 5 });
  const nodeData = nodes.data as { result?: unknown[]; total?: number } | undefined;
  process.stdout.write(
    JSON.stringify(
      {
        ok: true,
        baseUrl: config.baseUrl,
        workspace: config.workspace,
        readOnly: config.readOnly,
        workspaces: workspaces.data,
        nodeTotal: nodeData?.total ?? nodeData?.result?.length ?? 0,
      },
      null,
      2,
    ) + "\n",
  );
  return 0;
}

async function main(): Promise<void> {
  const arg = process.argv[2];
  if (arg === "--version" || arg === "-v") {
    process.stdout.write(`${VERSION}\n`);
    return;
  }
  if (arg === "--help" || arg === "-h" || arg === "help") {
    printHelp();
    return;
  }
  if (arg === "doctor") {
    try {
      process.exitCode = await doctor();
    } catch (error) {
      process.stderr.write(`doctor failed: ${error instanceof Error ? error.message : String(error)}\n`);
      process.exitCode = 1;
    }
    return;
  }
  if (arg !== undefined && arg !== "" && !arg.startsWith("-")) {
    process.stderr.write(`unknown command: ${arg}\n`);
    printHelp();
    process.exitCode = 2;
    return;
  }

  const config = loadConfig();
  const server = createServer(config);
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((error: unknown) => {
  if (error instanceof ConfigError) {
    process.stderr.write(`configuration error: ${error.message}\n`);
  } else {
    process.stderr.write(`fatal error: ${error instanceof Error ? (error.stack ?? error.message) : String(error)}\n`);
  }
  process.exitCode = 1;
});
