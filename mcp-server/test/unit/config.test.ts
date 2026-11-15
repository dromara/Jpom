/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 		http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterAll, afterEach, describe, expect, it, vi } from "vitest";
import { ConfigError, loadConfig } from "../../src/config.js";

const baseEnv = {
  JPOM_BASE_URL: "http://127.0.0.1:2122",
  JPOM_TOKEN: "test-token",
};

const tempDirs: string[] = [];

function makeTempDir(): string {
  const dir = mkdtempSync(join(tmpdir(), "jpom-mcp-config-"));
  tempDirs.push(dir);
  return dir;
}

afterAll(() => {
  for (const dir of tempDirs) rmSync(dir, { recursive: true, force: true });
});

afterEach(() => {
  vi.unstubAllEnvs();
  delete process.env.NODE_TLS_REJECT_UNAUTHORIZED;
});

describe("loadConfig defaults", () => {
  it("loads minimal env and applies documented defaults", () => {
    const config = loadConfig({ ...baseEnv });

    expect(config.baseUrl).toBe("http://127.0.0.1:2122");
    expect(config.token).toBe("test-token");
    expect(config.workspace).toBe("DEFAULT");
    expect(config.readOnly).toBe(true);
    expect(config.timeoutMs).toBe(60000);
    expect(config.maxResultChars).toBe(50000);
    expect(config.verifyTls).toBe(true);
  });

  it("strips a trailing slash (or several) from JPOM_BASE_URL", () => {
    const config = loadConfig({ ...baseEnv, JPOM_BASE_URL: "https://jpom.example.com:2122///" });
    expect(config.baseUrl).toBe("https://jpom.example.com:2122");
  });

  it("honours JPOM_WORKSPACE", () => {
    const config = loadConfig({ ...baseEnv, JPOM_WORKSPACE: "WS-42" });
    expect(config.workspace).toBe("WS-42");
  });

  it("reads JPOM_TOKEN_FILE and strips whitespace/newlines", () => {
    const dir = makeTempDir();
    const tokenFile = join(dir, "token");
    writeFileSync(tokenFile, "  file-token-value \n\n", "utf8");

    const config = loadConfig({ JPOM_BASE_URL: baseEnv.JPOM_BASE_URL, JPOM_TOKEN_FILE: tokenFile });
    expect(config.token).toBe("file-token-value");
  });

  it("prefers JPOM_TOKEN over JPOM_TOKEN_FILE", () => {
    const dir = makeTempDir();
    const tokenFile = join(dir, "token");
    writeFileSync(tokenFile, "file-token", "utf8");

    const config = loadConfig({ ...baseEnv, JPOM_TOKEN_FILE: tokenFile });
    expect(config.token).toBe("test-token");
  });
});

describe("loadConfig required values", () => {
  it("throws ConfigError when JPOM_BASE_URL is missing", () => {
    expect(() => loadConfig({ JPOM_TOKEN: "t" })).toThrowError(ConfigError);
    expect(() => loadConfig({ JPOM_TOKEN: "t" })).toThrowError(/JPOM_BASE_URL is required/);
  });

  it("throws ConfigError when JPOM_BASE_URL is blank", () => {
    expect(() => loadConfig({ JPOM_BASE_URL: "   ", JPOM_TOKEN: "t" })).toThrowError(/JPOM_BASE_URL is required/);
  });

  it("throws ConfigError when neither JPOM_TOKEN nor JPOM_TOKEN_FILE is set", () => {
    expect(() => loadConfig({ JPOM_BASE_URL: "http://127.0.0.1:2122" })).toThrowError(ConfigError);
    expect(() => loadConfig({ JPOM_BASE_URL: "http://127.0.0.1:2122" })).toThrowError(
      /JPOM_TOKEN or JPOM_TOKEN_FILE is required/,
    );
  });

  it("throws ConfigError when JPOM_TOKEN_FILE cannot be read", () => {
    const missing = join(makeTempDir(), "does-not-exist");
    expect(() => loadConfig({ JPOM_BASE_URL: baseEnv.JPOM_BASE_URL, JPOM_TOKEN_FILE: missing })).toThrowError(
      /cannot read JPOM_TOKEN_FILE/,
    );
  });

  it("rejects URLs without an http(s) scheme", () => {
    expect(() => loadConfig({ JPOM_BASE_URL: "127.0.0.1:2122", JPOM_TOKEN: "t" })).toThrowError(
      /must start with http:\/\/ or https:\/\//,
    );
    expect(() => loadConfig({ JPOM_BASE_URL: "ftp://127.0.0.1:2122", JPOM_TOKEN: "t" })).toThrowError(
      /must start with http:\/\/ or https:\/\//,
    );
  });
});

describe("loadConfig JPOM_READ_ONLY parsing", () => {
  it.each([
    ["undefined", undefined, true],
    ["true", "true", true],
    ["1", "1", true],
    ["false", "false", false],
    ["0", "0", false],
    ["no", "no", false],
    ["off", "off", false],
    ["FALSE", "FALSE", false],
    [" Off ", " Off ", false],
  ])("JPOM_READ_ONLY=%s -> readOnly=%s", (_label, raw, expected) => {
    const env = { ...baseEnv } as Record<string, string | undefined>;
    if (raw === undefined) delete env.JPOM_READ_ONLY;
    else env.JPOM_READ_ONLY = raw;
    expect(loadConfig(env).readOnly).toBe(expected);
  });

  it("clamps JPOM_TIMEOUT_MS and JPOM_MAX_RESULT_CHARS", () => {
    expect(loadConfig({ ...baseEnv, JPOM_TIMEOUT_MS: "100" }).timeoutMs).toBe(1000);
    expect(loadConfig({ ...baseEnv, JPOM_TIMEOUT_MS: "99999999" }).timeoutMs).toBe(600000);
    expect(loadConfig({ ...baseEnv, JPOM_TIMEOUT_MS: "not-a-number" }).timeoutMs).toBe(60000);
    expect(loadConfig({ ...baseEnv, JPOM_MAX_RESULT_CHARS: "10" }).maxResultChars).toBe(2000);
    expect(loadConfig({ ...baseEnv, JPOM_MAX_RESULT_CHARS: "9999999" }).maxResultChars).toBe(500000);
  });

  it("disables TLS verification and warns on stderr when JPOM_VERIFY_SSL=false", () => {
    const write = vi.spyOn(process.stderr, "write").mockImplementation(() => true);
    const config = loadConfig({ ...baseEnv, JPOM_VERIFY_SSL: "false" });

    expect(config.verifyTls).toBe(false);
    expect(process.env.NODE_TLS_REJECT_UNAUTHORIZED).toBe("0");
    expect(write).toHaveBeenCalledWith(expect.stringContaining("TLS certificate verification is disabled"));
    write.mockRestore();
  });

  it("keeps TLS verification enabled by default", () => {
    const config = loadConfig({ ...baseEnv });
    expect(config.verifyTls).toBe(true);
    expect(process.env.NODE_TLS_REJECT_UNAUTHORIZED).toBeUndefined();
  });
});
