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

export const pageSchema = {
  page: z.number().int().min(1).optional().describe("Page number, starting from 1 (default 1)."),
  limit: z.number().int().min(1).max(200).optional().describe("Page size, 1-200 (default 20)."),
};

export const workspaceSchema = {
  workspaceId: z
    .string()
    .min(1)
    .optional()
    .describe("Jpom workspace id to operate on. Defaults to JPOM_WORKSPACE (DEFAULT when unset)."),
};

export function paging(page?: number, limit?: number): { page: number; limit: number } {
  return {
    page: Math.max(1, Math.trunc(page ?? 1)),
    limit: Math.min(200, Math.max(1, Math.trunc(limit ?? 20))),
  };
}

export function clampLine(line?: number): number {
  return Math.max(1, Math.trunc(line ?? 1));
}

export function clampMaxLines(maxLines?: number, fallback = 200, max = 2000): number {
  if (maxLines === undefined || !Number.isFinite(maxLines)) return fallback;
  return Math.min(max, Math.max(1, Math.trunc(maxLines)));
}

export function requireText(name: string, value: unknown): string {
  if (typeof value !== "string" || value.trim() === "") {
    throw new Error(`missing required parameter: ${name}`);
  }
  return value.trim();
}

export function pickDefined(source: Record<string, unknown>, keys: string[]): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const key of keys) {
    if (source[key] !== undefined && source[key] !== null && source[key] !== "") result[key] = source[key];
  }
  return result;
}
