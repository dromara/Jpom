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
import { JpomApiError } from "../http.js";
import { errorResult, jsonResult, safeToolCall } from "../result.js";
import type { ToolContext } from "./types.js";

/**
 * POST endpoints that only read data, copied verbatim from
 * jpom-parent/skills/jpom-ops/scripts/jpom_api.py (READONLY_POST).
 * In read-only mode these POST calls are allowed; everything else needs JPOM_READ_ONLY=false.
 */
export const READONLY_POST: ReadonlySet<string> = new Set<string>([
  "/user/recent-log-data",
  "/node/list_data.json",
  "/node/list_group_all.json",
  "/node/project_list",
  "/node/node_monitor_data.json",
  "/node/processList",
  "/node/manage/get_project_info",
  "/node/script/list_all",
  "/node/script_log/list",
  "/build/list",
  "/build/history/history_list.json",
  "/build/manage/get-now-log",
  "/build/repository/list",
  "/build/branch-list",
  "/script/list",
  "/script_log/list",
  "/outgiving/dispatch-list",
  "/outgiving/log_list_data.json",
  "/system/assets/machine/list-data",
  "/system/assets/ssh/list-data",
  "/system/assets/ftp/list-data",
  "/system/assets/docker/list-data",
  "/system/backup/list",
  "/system/log_data.json",
  "/system/info",
  "/system/check_version.json",
  "/monitor/list_data.json",
  "/cluster/list",
  "/user/get_user_list",
  "/user-permission-group/get-list",
  "/system/workspace_env/list",
  "/user/login-log/list-data",
  "/user/list-operate-log-data",
  "/log-read/list",
]);

const MAX_PATH_LENGTH = 300;

/**
 * Endpoints that mutate state even with GET (or that must only be reached through a typed,
 * explicitly gated tool). The generic raw tool never executes these.
 */
const DANGEROUS_PATHS: ReadonlySet<string> = new Set<string>([
  "/system/remote_upgrade.json",
  "/system/restart",
  "/system/reload",
]);

/** Reject paths that are not plain Jpom API paths. Returns an error message or undefined. */
function pathError(path: string): string | undefined {
  if (!path.startsWith("/")) return 'path must start with "/" (for example /system/info).';
  if (path.includes("://")) return 'path must not contain "://"; pass an API path, not a full URL.';
  if (path.toLowerCase().startsWith("/api/")) {
    return 'path must not start with "/api/": the Jpom open API bypasses user permission checks and is not exposed to AI tools.';
  }
  if (path.length > MAX_PATH_LENGTH) return `path is too long (max ${MAX_PATH_LENGTH} characters).`;
  return undefined;
}

export function registerRawTools(server: McpServer, ctx: ToolContext): void {
  server.registerTool(
    "jpom_api_call",
    {
      title: "Call an arbitrary Jpom REST API",
      description:
        "Generic Jpom REST API call (escape hatch). READ-ONLY BY DEFAULT: GET is always allowed; POST is allowed only " +
        "for whitelisted read-only endpoints (for example /system/info or /build/list). Other POST calls change server " +
        "state: they are refused while JPOM_READ_ONLY=true (the default) and, once writes are enabled, require " +
        "confirm=true. HIGH RISK: prefer the typed jpom_* tools; this tool cannot bypass Jpom permissions. " +
        'path must start with "/", must not contain "://", must not start with "/api/" (the open API is not exposed) ' +
        "and is at most 300 characters. Returns the Jpom envelope {code,msg,data}.",
      annotations: { readOnlyHint: false, openWorldHint: false },
      inputSchema: {
        method: z
          .enum(["GET", "POST"])
          .describe("HTTP method: GET for reads, POST for whitelisted read-only endpoints or confirmed state changes."),
        path: z
          .string()
          .min(1)
          .describe(
            'Jpom API path starting with "/", for example /system/info or /build/list; max 300 characters.',
          ),
        query: z.record(z.unknown()).optional().describe("Query string parameters."),
        form: z
          .record(z.unknown())
          .optional()
          .describe("application/x-www-form-urlencoded body (POST only; cannot be combined with json)."),
        json: z.unknown().optional().describe("JSON body (POST only; cannot be combined with form)."),
        confirm: z
          .boolean()
          .optional()
          .describe("Must be true for a non-whitelisted POST when JPOM_READ_ONLY=false."),
      },
    },
    async (args) => {
      const path = args.path;
      const invalid = pathError(path);
      if (invalid !== undefined) return errorResult(`jpom_api_call: ${invalid}`);
      if (DANGEROUS_PATHS.has(path)) {
        return errorResult(
          `Refused: ${path} is a high-risk endpoint and is not available through jpom_api_call. ` +
            "Use the dedicated typed tool (for example jpom_system_upgrade), which enforces the write gate.",
        );
      }

      if (args.method === "GET") {
        if (args.form !== undefined || args.json !== undefined) {
          return errorResult(
            "jpom_api_call: GET requests accept only the query parameter; use form or json with POST.",
          );
        }
      } else if (args.form !== undefined && args.json !== undefined) {
        return errorResult("jpom_api_call: provide either form or json, not both.");
      }

      if (ctx.config.readOnly) {
        if (args.method === "POST" && !READONLY_POST.has(path)) {
          return errorResult(
            `Refused: POST ${path} is not in the read-only whitelist and JPOM_READ_ONLY is enabled (the default). ` +
              "Set JPOM_READ_ONLY=false to allow this call; non-whitelisted POST calls then also require confirm: true. " +
              "No request was sent.",
          );
        }
      } else if (args.method === "POST" && !READONLY_POST.has(path) && args.confirm !== true) {
        return errorResult(
          `Refused: POST ${path} changes Jpom server state and requires explicit confirmation. ` +
            "Re-run with confirm: true, or use a typed write tool (jpom_build_start, jpom_build_cancel, " +
            "jpom_build_rerelease, jpom_project_operate, jpom_node_kill_pid, jpom_system_upgrade). No request was sent.",
        );
      }

      return safeToolCall(async () => {
        try {
          const response = await ctx.client.request(args.method, path, {
            query: args.query,
            form: args.form,
            json: args.json,
          });
          return jsonResult(
            { code: response.code, msg: response.msg ?? "", data: response.data },
            { maxChars: ctx.maxChars },
          );
        } catch (error) {
          if (error instanceof JpomApiError) {
            // Surface the business code to the model instead of only a formatted message.
            return {
              ...jsonResult({ code: error.code, msg: error.message, data: null }, { maxChars: ctx.maxChars }),
              isError: true,
            };
          }
          throw error;
        }
      });
    },
  );
}
