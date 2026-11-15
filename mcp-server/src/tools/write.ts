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
import type { CallToolResult } from "@modelcontextprotocol/sdk/types.js";
import { z } from "zod";
import { errorResult, jsonResult, safeToolCall } from "../result.js";
import { pickDefined } from "./common.js";
import type { ToolContext } from "./types.js";

/**
 * Write tools mutate Jpom server state (builds, running projects, processes, online upgrade)
 * and are therefore destructive and disabled unless JPOM_READ_ONLY=false.
 */
const WRITE_ANNOTATIONS = { readOnlyHint: false, destructiveHint: true };

function writeDisabled(tool: string, action: string): CallToolResult {
  return errorResult(
    `Refused: ${tool} (${action}) is a write tool and JPOM_READ_ONLY is enabled (the default). ` +
      "Set JPOM_READ_ONLY=false in the MCP server environment and restart the server to allow write operations. No request was sent.",
  );
}

export function registerWriteTools(server: McpServer, ctx: ToolContext): void {
  server.registerTool(
    "jpom_build_start",
    {
      title: "Start a Jpom build",
      description:
        "Trigger a Jpom build for a build configuration. WRITE operation: refused unless JPOM_READ_ONLY=false. " +
        "Optionally build a different branch and attach a remark. Returns the created build record (buildId).",
      annotations: WRITE_ANNOTATIONS,
      inputSchema: {
        id: z.string().min(1).describe("Build configuration id (see jpom_builds)."),
        branchName: z
          .string()
          .optional()
          .describe("Git branch to build; defaults to the branch configured for this build."),
        buildRemark: z.string().optional().describe("Remark stored on this build record."),
      },
    },
    async (args) => {
      if (ctx.config.readOnly) return writeDisabled("jpom_build_start", "build.start");
      return safeToolCall(async () => {
        const form = pickDefined(
          { id: args.id, branchName: args.branchName, buildRemark: args.buildRemark },
          ["id", "branchName", "buildRemark"],
        );
        const response = await ctx.client.postForm("/build/manage/start", form);
        return jsonResult(
          { ok: true, action: "build.start", target: form, response: response.data },
          { maxChars: ctx.maxChars },
        );
      });
    },
  );

  server.registerTool(
    "jpom_build_cancel",
    {
      title: "Cancel a Jpom build",
      description:
        "Cancel a running Jpom build by build configuration id. WRITE operation: refused unless JPOM_READ_ONLY=false.",
      annotations: WRITE_ANNOTATIONS,
      inputSchema: {
        id: z.string().min(1).describe("Build configuration id (see jpom_builds)."),
      },
    },
    async (args) => {
      if (ctx.config.readOnly) return writeDisabled("jpom_build_cancel", "build.cancel");
      return safeToolCall(async () => {
        const form = { id: args.id };
        const response = await ctx.client.postForm("/build/manage/cancel", form);
        return jsonResult(
          { ok: true, action: "build.cancel", target: form, response: response.data },
          { maxChars: ctx.maxChars },
        );
      });
    },
  );

  server.registerTool(
    "jpom_build_rerelease",
    {
      title: "Re-release a Jpom build",
      description:
        "Re-run/re-release a previous Jpom build history record (logId) with the same build parameters. " +
        "WRITE operation: refused unless JPOM_READ_ONLY=false.",
      annotations: WRITE_ANNOTATIONS,
      inputSchema: {
        logId: z.string().min(1).describe("Build history record id (logId) from jpom_build_history."),
      },
    },
    async (args) => {
      if (ctx.config.readOnly) return writeDisabled("jpom_build_rerelease", "build.rerelease");
      return safeToolCall(async () => {
        const form = { logId: args.logId };
        const response = await ctx.client.postForm("/build/manage/reRelease", form);
        return jsonResult(
          { ok: true, action: "build.rerelease", target: form, response: response.data },
          { maxChars: ctx.maxChars },
        );
      });
    },
  );

  server.registerTool(
    "jpom_project_operate",
    {
      title: "Operate a Jpom project",
      description:
        "Start, stop or restart a Jpom project on a node (opt = start|stop|restart). WRITE operation: refused unless " +
        "JPOM_READ_ONLY=false. Stopping or restarting interrupts the running project.",
      annotations: WRITE_ANNOTATIONS,
      inputSchema: {
        nodeId: z.string().min(1).describe("Jpom node id that owns the project (see jpom_nodes)."),
        id: z.string().min(1).describe("Jpom project id."),
        opt: z.enum(["start", "stop", "restart"]).describe("Operation to perform: start, stop or restart."),
      },
    },
    async (args) => {
      if (ctx.config.readOnly) return writeDisabled("jpom_project_operate", "project.operate");
      return safeToolCall(async () => {
        const form = { nodeId: args.nodeId, id: args.id, opt: args.opt };
        const response = await ctx.client.postForm("/node/manage/operate", form);
        return jsonResult(
          { ok: true, action: "project.operate", target: form, response: response.data },
          { maxChars: ctx.maxChars },
        );
      });
    },
  );

  server.registerTool(
    "jpom_node_kill_pid",
    {
      title: "Kill a process on a Jpom node",
      description:
        "Kill a process by pid on a Jpom node. DESTRUCTIVE WRITE operation: refused unless JPOM_READ_ONLY=false. " +
        "Killing the wrong pid can take down unrelated services; verify the pid first.",
      annotations: WRITE_ANNOTATIONS,
      inputSchema: {
        nodeId: z.string().min(1).describe("Jpom node id (see jpom_nodes)."),
        pid: z.string().min(1).describe("Process id (pid) on the node, e.g. from jpom_node_processes."),
        machineId: z.string().optional().describe("Optional machine id when the process runs on a machine asset."),
      },
    },
    async (args) => {
      if (ctx.config.readOnly) return writeDisabled("jpom_node_kill_pid", "node.kill-pid");
      return safeToolCall(async () => {
        const form = pickDefined(
          { nodeId: args.nodeId, pid: args.pid, machineId: args.machineId },
          ["nodeId", "pid", "machineId"],
        );
        const response = await ctx.client.postForm("/node/kill.json", form);
        return jsonResult(
          { ok: true, action: "node.kill-pid", target: form, response: response.data },
          { maxChars: ctx.maxChars },
        );
      });
    },
  );

  server.registerTool(
    "jpom_system_upgrade",
    {
      title: "Trigger a Jpom online upgrade",
      description:
        "Trigger the Jpom server (or a node agent when machineId is given) online upgrade to the latest version. " +
        "HIGH RISK WRITE operation: refused unless JPOM_READ_ONLY=false. The server may restart and interrupt running tasks.",
      annotations: WRITE_ANNOTATIONS,
      inputSchema: {
        machineId: z
          .string()
          .optional()
          .describe("Optional machine id; when omitted the Jpom server itself is upgraded."),
      },
    },
    async (args) => {
      if (ctx.config.readOnly) return writeDisabled("jpom_system_upgrade", "system.upgrade");
      return safeToolCall(async () => {
        const query = pickDefined({ machineId: args.machineId }, ["machineId"]);
        const response = await ctx.client.get("/system/remote_upgrade.json", query);
        return jsonResult(
          { ok: true, action: "system.upgrade", target: query, response: response.data },
          { maxChars: ctx.maxChars },
        );
      });
    },
  );
}
