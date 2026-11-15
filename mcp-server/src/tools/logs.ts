/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 		http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import { z } from "zod";
import WebSocket from "ws";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { JpomConfig } from "../config.js";
import type { JpomClient } from "../http.js";
import { jsonResult, safeToolCall } from "../result.js";
import { clampLine, clampMaxLines, requireText } from "./common.js";
import type { ToolContext } from "./types.js";

const DEFAULT_MAX_BYTES = 5 * 1024 * 1024;

interface LineReadResult {
  lines: string[];
  nextLine: number;
  truncated: boolean;
  lastData: unknown;
}

/**
 * Jpom serves log content through a line cursor: pass `line` (1-based) and the API
 * returns `dataLines` plus the next `line` to request. This helper reads at most a few
 * batches so a single MCP call stays bounded.
 */
async function readByLine(
  client: JpomClient,
  path: string,
  baseParams: Record<string, unknown>,
  line: number,
  maxLines: number,
  maxRequests = 4,
): Promise<LineReadResult> {
  const lines: string[] = [];
  let nextLine = line;
  let lastData: unknown;
  for (let request = 0; request < maxRequests && lines.length < maxLines; request += 1) {
    const response = await client.postForm(path, { ...baseParams, line: nextLine });
    const data = response.data as { dataLines?: unknown; line?: unknown } | undefined;
    lastData = data;
    const batch = Array.isArray(data?.dataLines) ? (data.dataLines as string[]) : [];
    if (batch.length === 0) break;
    lines.push(...batch);
    const reportedNext = Number(data?.line);
    if (!Number.isFinite(reportedNext) || reportedNext <= nextLine) break;
    nextLine = reportedNext;
  }
  return {
    lines: lines.slice(0, maxLines),
    nextLine,
    truncated: lines.length > maxLines,
    lastData,
  };
}

function tailLinesWithMeta(text: string, count: number, maxBytes: number): {
  lines: string[];
  totalLines?: number;
  startLine?: number;
  bytesTruncated: boolean;
} {
  let content = text;
  let bytesTruncated = false;
  if (content.length > maxBytes) {
    content = content.slice(content.length - maxBytes);
    const firstNewline = content.indexOf("\n");
    if (firstNewline >= 0) content = content.slice(firstNewline + 1);
    bytesTruncated = true;
  }
  const allLines = content.split(/\r?\n/);
  if (allLines.length > 0 && allLines[allLines.length - 1] === "") allLines.pop();
  const totalLines = bytesTruncated ? undefined : allLines.length;
  const startLine = totalLines === undefined ? undefined : Math.max(1, totalLines - count + 1);
  return { lines: allLines.slice(-count), totalLines, startLine, bytesTruncated };
}

function buildWebSocketUrl(config: JpomConfig, path: string, query: Record<string, string | undefined>): string {
  const url = new URL(config.baseUrl);
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  url.pathname = `${url.pathname.replace(/\/$/, "")}${path}`;
  url.search = "";
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== "") url.searchParams.set(key, value);
  }
  return url.toString();
}

/**
 * Open a short-lived Jpom WebSocket log subscription, collect text frames and close.
 * Jpom only replays a small number of recent lines on subscribe (jpom.init-read-line),
 * so this is intended for a bounded live window, not for backfilling history.
 */
function followWebSocketLog(
  config: JpomConfig,
  path: string,
  query: Record<string, string | undefined>,
  command: Record<string, unknown>,
  durationMs: number,
  maxLines: number,
): Promise<{ lines: string[]; reason: string }> {
  return new Promise((resolve, reject) => {
    const lines: string[] = [];
    let settled = false;
    const ws = new WebSocket(buildWebSocketUrl(config, path, query), {
      headers: { "JPOM-USER-TOKEN": config.token },
      handshakeTimeout: 10000,
    });

    const finish = (reason: string): void => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      try {
        ws.close();
      } catch {
        // ignore close errors
      }
      resolve({ lines, reason });
    };

    const timer = setTimeout(() => finish("duration"), durationMs);

    ws.on("open", () => {
      try {
        ws.send(JSON.stringify(command));
      } catch (error) {
        settled = true;
        clearTimeout(timer);
        reject(error instanceof Error ? error : new Error(String(error)));
      }
    });
    ws.on("message", (data) => {
      const text = data.toString();
      lines.push(text);
      if (lines.length >= maxLines) finish("max-lines");
    });
    ws.on("error", (error) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      reject(new Error(`Jpom WebSocket error: ${error.message}`));
    });
    ws.on("close", () => finish("closed"));
  });
}

export function registerLogTools(server: McpServer, ctx: ToolContext): void {
  const { client, config } = ctx;

  server.registerTool(
    "jpom_system_log_files",
    {
      title: "Jpom system log files",
      description:
        "List Jpom system log files (server log directory tree, including rotated files). Pass machineId to list an agent/machine node log directory instead of the server's own logs.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {
        machineId: z.string().min(1).optional().describe("Machine/agent id; omit to read the Jpom server's own log directory."),
      },
    },
    async (args) =>
      safeToolCall(async () => {
        const form: Record<string, unknown> = {};
        if (args.machineId) form.machineId = args.machineId;
        const response = await client.postForm("/system/log_data.json", form);
        return jsonResult(response.data, { maxChars: ctx.maxChars });
      }),
  );

  server.registerTool(
    "jpom_system_log_read",
    {
      title: "Read Jpom system log content",
      description:
        "Read a Jpom system/agent log file (path from jpom_system_log_files). Returns the last tailLines lines by default, or matching lines when grep is provided. Large files are bounded by maxBytes; content is returned as JSON with line numbers.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {
        path: z.string().min(1).describe("Log file path relative to the log directory, e.g. server.log."),
        machineId: z.string().min(1).optional().describe("Machine/agent id; omit for the Jpom server's own log."),
        tailLines: z.number().int().min(1).max(5000).optional().describe("Return the last N lines (default 200)."),
        maxBytes: z.number().int().min(1024).max(50 * 1024 * 1024).optional().describe("Process at most this many tail bytes (default 5 MiB)."),
        grep: z.string().min(1).optional().describe("Only return lines matching this text or regular expression."),
        ignoreCase: z.boolean().optional().describe("Case-insensitive when grep is a regular expression (default false)."),
        maxMatches: z.number().int().min(1).max(2000).optional().describe("Maximum matching lines to return (default 200)."),
      },
    },
    async (args) =>
      safeToolCall(async () => {
        const path = requireText("path", args.path);
        const query: Record<string, unknown> = { path };
        if (args.machineId) query.machineId = args.machineId;
        const response = await client.requestText("GET", "/system/log_download", { query });
        const text = response.text;
        const maxBytes = args.maxBytes ?? DEFAULT_MAX_BYTES;

        if (args.grep) {
          let pattern: RegExp;
          try {
            pattern = new RegExp(args.grep, args.ignoreCase ? "i" : "");
          } catch {
            pattern = new RegExp(args.grep.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), args.ignoreCase ? "i" : "");
          }
          let content = text;
          let bytesTruncated = false;
          if (content.length > maxBytes) {
            content = content.slice(content.length - maxBytes);
            const firstNewline = content.indexOf("\n");
            if (firstNewline >= 0) content = content.slice(firstNewline + 1);
            bytesTruncated = true;
          }
          const allLines = content.split(/\r?\n/);
          if (allLines.length > 0 && allLines[allLines.length - 1] === "") allLines.pop();
          const matches: Array<{ line: number; text: string }> = [];
          const maxMatches = args.maxMatches ?? 200;
          for (let index = 0; index < allLines.length; index += 1) {
            if (pattern.test(allLines[index])) {
              matches.push({ line: index + 1, text: allLines[index] });
              if (matches.length >= maxMatches) break;
            }
          }
          return jsonResult(
            {
              path,
              machineId: args.machineId,
              bytesTruncated,
              matchCount: matches.length,
              truncated: matches.length >= maxMatches,
              matches,
            },
            { maxChars: ctx.maxChars },
          );
        }

        const tail = tailLinesWithMeta(text, clampMaxLines(args.tailLines, 200, 5000), maxBytes);
        return jsonResult(
          {
            path,
            machineId: args.machineId,
            totalLines: tail.totalLines,
            startLine: tail.startLine,
            returnedLines: tail.lines.length,
            bytesTruncated: tail.bytesTruncated,
            bytes: text.length,
            lines: tail.lines,
          },
          { maxChars: ctx.maxChars },
        );
      }),
  );

  server.registerTool(
    "jpom_system_log_follow",
    {
      title: "Follow Jpom system log (live window)",
      description:
        "Open a short-lived WebSocket subscription to a Jpom system/agent log file and collect new lines. Jpom replays only a few recent lines on subscribe, so use jpom_system_log_read for history. Returns after durationMs or maxLines, whichever comes first.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {
        path: z.string().min(1).describe("Log file path relative to the log directory, e.g. server.log."),
        machineId: z.string().min(1).optional().describe("Machine/agent id; omit for the Jpom server's own log."),
        durationMs: z.number().int().min(200).max(15000).optional().describe("How long to collect lines (default 3000 ms, max 15000)."),
        maxLines: z.number().int().min(1).max(2000).optional().describe("Stop after this many lines (default 200)."),
      },
    },
    async (args) =>
      safeToolCall(async () => {
        const path = requireText("path", args.path);
        const machineId = args.machineId;
        const wsPath = machineId ? "/socket/agent_log" : "/socket/system_log";
        const query: Record<string, string | undefined> = {
          nodeId: "system",
          type: machineId ? "agentLog" : "systemLog",
          workspaceId: config.workspace,
          lang: "zh_CN",
          machineId,
        };
        const { lines, reason } = await followWebSocketLog(
          config,
          wsPath,
          query,
          { op: "showlog", fileName: path },
          args.durationMs ?? 3000,
          clampMaxLines(args.maxLines, 200, 2000),
        );
        return jsonResult(
          {
            path,
            machineId,
            reason,
            count: lines.length,
            lines,
          },
          { maxChars: ctx.maxChars },
        );
      }),
  );


  server.registerTool(
    "jpom_project_log_follow",
    {
      title: "Follow Jpom project log (live window)",
      description:
        "Open a short-lived WebSocket subscription to a project (or project log file) and collect lines. Omit fileName to follow the project's configured log file. Provide keyword to run Jpom's log search mode (head/tail/keyword) instead of a plain tail. History beyond the few replayed lines should be read from a build/script log tool when available.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {
        nodeId: z.string().min(1).describe("Node id that owns the project."),
        id: z
          .string()
          .min(1)
          .describe("Project cache id: the \"id\" field from jpom_projects (used for the WebSocket authorization query)."),
        projectId: z
          .string()
          .min(1)
          .describe("Project short id: the \"projectId\" field from jpom_projects (required by the agent console)."),
        fileName: z
          .string()
          .min(1)
          .optional()
          .describe("Log file path relative to the project lib directory. Omit to use the project's configured log file."),
        keyword: z
          .string()
          .min(1)
          .optional()
          .describe("Search keyword; enables Jpom log search mode (requires fileName)."),
        durationMs: z.number().int().min(200).max(15000).optional().describe("How long to collect lines (default 3000 ms, max 15000)."),
        maxLines: z.number().int().min(1).max(2000).optional().describe("Stop after this many lines (default 200)."),
      },
    },
    async (args) =>
      safeToolCall(async () => {
        const nodeId = requireText("nodeId", args.nodeId);
        const cacheId = requireText("id", args.id);
        const projectId = requireText("projectId", args.projectId);
        const query: Record<string, string | undefined> = {
          id: cacheId,
          nodeId,
          type: "console",
          workspaceId: config.workspace,
          lang: "zh_CN",
        };
        const command: Record<string, unknown> = args.keyword
          ? {
              op: "showlog",
              search: true,
              projectId,
              useProjectId: projectId,
              useNodeId: nodeId,
              logFile: args.fileName,
              keyword: args.keyword,
              beforeCount: 0,
              afterCount: 10,
              head: 0,
              tail: 100,
              first: "false",
            }
          : { op: "showlog", projectId, fileName: args.fileName };
        const { lines, reason } = await followWebSocketLog(
          config,
          "/socket/console",
          query,
          command,
          args.durationMs ?? 3000,
          clampMaxLines(args.maxLines, 200, 2000),
        );
        return jsonResult(
          {
            nodeId,
            id: cacheId,
            projectId,
            fileName: args.fileName,
            keyword: args.keyword,
            reason,
            count: lines.length,
            lines,
          },
          { maxChars: ctx.maxChars },
        );
      }),
  );

  server.registerTool(
    "jpom_build_log",
    {
      title: "Read Jpom build log",
      description:
        "Read a Jpom build log by line cursor. Returns log lines from the requested line plus nextLine for a following call. Works for running and finished builds.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {
        id: z.string().min(1).describe("Build configuration id."),
        buildId: z.number().int().min(1).describe("Build number (buildId)."),
        line: z.number().int().min(1).optional().describe("Start line (1-based, default 1)."),
        maxLines: z.number().int().min(1).max(2000).optional().describe("Maximum lines to return (default 500)."),
      },
    },
    async (args) =>
      safeToolCall(async () => {
        const result = await readByLine(
          client,
          "/build/manage/get-now-log",
          { id: requireText("id", args.id), buildId: args.buildId },
          clampLine(args.line),
          clampMaxLines(args.maxLines, 500, 2000),
        );
        const meta = result.lastData as { run?: unknown; status?: unknown; logId?: unknown } | undefined;
        return jsonResult(
          {
            id: args.id,
            buildId: args.buildId,
            nextLine: result.nextLine,
            truncated: result.truncated,
            run: meta?.run,
            status: meta?.status,
            logId: meta?.logId,
            lines: result.lines,
          },
          { maxChars: ctx.maxChars },
        );
      }),
  );

  server.registerTool(
    "jpom_script_log",
    {
      title: "Read Jpom server script execution log",
      description:
        "Read a server script execution log by line cursor (script template id + execution id). Returns lines plus nextLine.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {
        id: z.string().min(1).describe("Server script template id."),
        executeId: z.string().min(1).describe("Script execution record id."),
        line: z.number().int().min(1).optional().describe("Start line (1-based, default 1)."),
        maxLines: z.number().int().min(1).max(2000).optional().describe("Maximum lines to return (default 500)."),
      },
    },
    async (args) =>
      safeToolCall(async () => {
        const result = await readByLine(
          client,
          "/script_log/log",
          { id: requireText("id", args.id), executeId: requireText("executeId", args.executeId) },
          clampLine(args.line),
          clampMaxLines(args.maxLines, 500, 2000),
        );
        const meta = result.lastData as { run?: unknown } | undefined;
        return jsonResult(
          {
            id: args.id,
            executeId: args.executeId,
            nextLine: result.nextLine,
            truncated: result.truncated,
            run: meta?.run,
            lines: result.lines,
          },
          { maxChars: ctx.maxChars },
        );
      }),
  );

  server.registerTool(
    "jpom_node_script_log",
    {
      title: "Read Jpom node script execution log",
      description:
        "Read a node (agent) script execution log by line cursor (nodeId + script template id + execution id). Returns lines plus nextLine.",
      annotations: { readOnlyHint: true, openWorldHint: false },
      inputSchema: {
        nodeId: z.string().min(1).describe("Node id."),
        id: z.string().min(1).describe("Node script template id."),
        executeId: z.string().min(1).describe("Script execution record id."),
        line: z.number().int().min(1).optional().describe("Start line (1-based, default 1)."),
        maxLines: z.number().int().min(1).max(2000).optional().describe("Maximum lines to return (default 500)."),
      },
    },
    async (args) =>
      safeToolCall(async () => {
        const result = await readByLine(
          client,
          "/node/script_log/log",
          {
            nodeId: requireText("nodeId", args.nodeId),
            id: requireText("id", args.id),
            executeId: requireText("executeId", args.executeId),
          },
          clampLine(args.line),
          clampMaxLines(args.maxLines, 500, 2000),
        );
        const meta = result.lastData as { run?: unknown } | undefined;
        return jsonResult(
          {
            nodeId: args.nodeId,
            id: args.id,
            executeId: args.executeId,
            nextLine: result.nextLine,
            truncated: result.truncated,
            run: meta?.run,
            lines: result.lines,
          },
          { maxChars: ctx.maxChars },
        );
      }),
  );
}
