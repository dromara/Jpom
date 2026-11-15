/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 		http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import { readFileSync } from "node:fs";

export interface JpomConfig {
  /** Jpom Server base URL, without trailing slash. */
  baseUrl: string;
  /** Long-term user token (JPOM-USER-TOKEN). */
  token: string;
  /** Workspace id used for all requests. */
  workspace: string;
  /** True by default: write tools are disabled. */
  readOnly: boolean;
  /** Request timeout in milliseconds. */
  timeoutMs: number;
  /** Maximum characters returned in a single tool result. */
  maxResultChars: number;
  /** Verify TLS certificates (set JPOM_VERIFY_SSL=false for self-signed). */
  verifyTls: boolean;
}

export class ConfigError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ConfigError";
  }
}

function clampInt(raw: string | undefined, fallback: number, min: number, max: number): number {
  if (raw === undefined || raw.trim() === "") return fallback;
  const value = Number(raw);
  if (!Number.isFinite(value)) return fallback;
  return Math.min(max, Math.max(min, Math.trunc(value)));
}

function isFalse(raw: string | undefined, defaultValue: boolean): boolean {
  if (raw === undefined || raw.trim() === "") return defaultValue;
  return !["false", "0", "no", "off"].includes(raw.trim().toLowerCase());
}

/**
 * Load configuration from environment variables.
 *
 * Required: JPOM_BASE_URL and (JPOM_TOKEN or JPOM_TOKEN_FILE).
 * Optional: JPOM_WORKSPACE (DEFAULT), JPOM_READ_ONLY (true), JPOM_TIMEOUT_MS (60000),
 *           JPOM_MAX_RESULT_CHARS (50000), JPOM_VERIFY_SSL (true).
 */
export function loadConfig(env: NodeJS.ProcessEnv = process.env): JpomConfig {
  const baseUrl = (env.JPOM_BASE_URL ?? "").trim().replace(/\/+$/, "");
  if (!baseUrl) {
    throw new ConfigError("JPOM_BASE_URL is required, for example http://127.0.0.1:2122");
  }
  if (!/^https?:\/\//i.test(baseUrl)) {
    throw new ConfigError("JPOM_BASE_URL must start with http:// or https://");
  }

  let token = (env.JPOM_TOKEN ?? "").trim();
  const tokenFile = (env.JPOM_TOKEN_FILE ?? "").trim();
  if (!token && tokenFile) {
    try {
      token = readFileSync(tokenFile, "utf8").replace(/\s+/g, "");
    } catch (error) {
      throw new ConfigError(`cannot read JPOM_TOKEN_FILE: ${(error as Error).message}`);
    }
  }
  if (!token) {
    throw new ConfigError("JPOM_TOKEN or JPOM_TOKEN_FILE is required (Jpom long-term user token)");
  }

  const workspace = (env.JPOM_WORKSPACE ?? "").trim() || "DEFAULT";
  const readOnly = isFalse(env.JPOM_READ_ONLY, true);
  const verifyTls = isFalse(env.JPOM_VERIFY_SSL, true);
  if (!verifyTls) {
    // Node's fetch (undici) respects this global switch; warn loudly on stderr, never on stdout (stdio transport).
    process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";
    process.stderr.write("[jpom-mcp-server] WARNING: TLS certificate verification is disabled (JPOM_VERIFY_SSL=false)\n");
  }

  return {
    baseUrl,
    token,
    workspace,
    readOnly,
    timeoutMs: clampInt(env.JPOM_TIMEOUT_MS, 60000, 1000, 600000),
    maxResultChars: clampInt(env.JPOM_MAX_RESULT_CHARS, 50000, 2000, 500000),
    verifyTls,
  };
}
