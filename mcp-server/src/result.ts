/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 		http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import type { CallToolResult } from "@modelcontextprotocol/sdk/types.js";
import { JpomApiError, JpomHttpError, JpomNetworkError } from "./http.js";

const DEFAULT_MAX_CHARS = 50000;

export interface ResultOptions {
  maxChars?: number;
}

export function textResult(text: string, options: ResultOptions = {}): CallToolResult {
  const max = options.maxChars ?? DEFAULT_MAX_CHARS;
  const value =
    text.length > max ? `${text.slice(0, max)}\n...[truncated, total ${text.length} chars]` : text;
  return { content: [{ type: "text", text: value }] };
}

export function jsonResult(data: unknown, options: ResultOptions = {}): CallToolResult {
  const text = JSON.stringify(data, null, 0) ?? "null";
  return textResult(text, options);
}

export function errorResult(message: string): CallToolResult {
  return { content: [{ type: "text", text: message }], isError: true };
}

export function formatError(error: unknown): string {
  if (error instanceof JpomApiError) {
    const hint =
      error.code === 800
        ? " (token invalid or expired; check JPOM_TOKEN)"
        : error.code === 802
          ? " (account disabled)"
          : error.code === 999
            ? " (permission denied; check workspace and token permissions)"
            : "";
    return `Jpom API error code=${error.code}: ${error.message}${hint} [${error.method} ${error.path}]`;
  }
  if (error instanceof JpomHttpError) {
    return `Jpom HTTP error status=${error.status}: ${error.message} [${error.method} ${error.path}]${error.bodyPreview ? ` body=${error.bodyPreview}` : ""}`;
  }
  if (error instanceof JpomNetworkError) {
    return error.message;
  }
  if (error instanceof Error) return error.message;
  return String(error);
}

export async function safeToolCall(fn: () => Promise<CallToolResult>): Promise<CallToolResult> {
  try {
    return await fn();
  } catch (error) {
    return errorResult(formatError(error));
  }
}
