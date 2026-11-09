/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 		http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import { describe, expect, it } from "vitest";
import {
  JpomApiError,
  JpomHttpError,
  JpomNetworkError,
} from "../../src/http.js";
import { errorResult, formatError, jsonResult, safeToolCall, textResult } from "../../src/result.js";

function textOf(result: { content: Array<{ type: string; text?: string }> }): string {
  const first = result.content[0];
  expect(first.type).toBe("text");
  return first.text ?? "";
}

describe("textResult", () => {
  it("returns text unchanged when below the limit", () => {
    const result = textResult("hello", { maxChars: 100 });
    expect(result.isError).toBeUndefined();
    expect(textOf(result)).toBe("hello");
  });

  it("keeps text exactly at the limit", () => {
    const text = "x".repeat(100);
    expect(textOf(textResult(text, { maxChars: 100 }))).toBe(text);
  });

  it("truncates oversized text and reports the original length", () => {
    const text = "a".repeat(1000);
    const result = textResult(text, { maxChars: 100 });
    const body = textOf(result);

    expect(body.startsWith("a".repeat(100))).toBe(true);
    expect(body).toContain("[truncated, total 1000 chars]");
    expect(body.length).toBeLessThan(text.length);
  });

  it("uses the default 50000 char limit", () => {
    const text = "b".repeat(50001);
    const body = textOf(textResult(text));
    expect(body).toContain("[truncated, total 50001 chars]");
  });
});

describe("jsonResult", () => {
  it("serializes data as compact JSON text", () => {
    const result = jsonResult({ total: 2, result: [{ id: "a" }, { id: "b" }] }, { maxChars: 1000 });
    expect(textOf(result)).toBe('{"total":2,"result":[{"id":"a"},{"id":"b"}]}');
  });

  it("serializes null instead of returning an empty string", () => {
    expect(textOf(jsonResult(undefined))).toBe("null");
    expect(textOf(jsonResult(null))).toBe("null");
  });

  it("truncates oversized JSON payloads", () => {
    const result = jsonResult({ values: Array.from({ length: 100 }, (_, i) => i) }, { maxChars: 50 });
    const body = textOf(result);
    expect(body).toContain("[truncated, total");
    expect(body.startsWith('{"values":[')).toBe(true);
  });
});

describe("errorResult", () => {
  it("marks the result as an error with the message as text", () => {
    const result = errorResult("boom");
    expect(result.isError).toBe(true);
    expect(textOf(result)).toBe("boom");
  });
});

describe("formatError", () => {
  it("formats JpomApiError with the API code and request context", () => {
    const message = formatError(new JpomApiError(500, "bad thing", "POST", "/node/list_data.json"));
    expect(message).toBe("Jpom API error code=500: bad thing [POST /node/list_data.json]");
  });

  it("adds hints for well-known Jpom API codes", () => {
    expect(formatError(new JpomApiError(800, "expired", "GET", "/user/my-workspace"))).toContain(
      "token invalid or expired",
    );
    expect(formatError(new JpomApiError(999, "denied", "GET", "/x"))).toContain("permission denied");
  });

  it("formats JpomHttpError with status and body preview", () => {
    const message = formatError(new JpomHttpError("gateway down", 502, "GET", "/x", "upstream"));
    expect(message).toBe("Jpom HTTP error status=502: gateway down [GET /x] body=upstream");
  });

  it("formats JpomNetworkError as a plain message", () => {
    expect(formatError(new JpomNetworkError("connect ECONNREFUSED", "GET", "/x"))).toBe(
      "connect ECONNREFUSED",
    );
  });

  it("formats generic errors and non-error values", () => {
    expect(formatError(new Error("plain failure"))).toBe("plain failure");
    expect(formatError("string failure")).toBe("string failure");
    expect(formatError(42)).toBe("42");
  });
});

describe("safeToolCall", () => {
  it("passes successful results through", async () => {
    const result = await safeToolCall(async () => textResult("ok"));
    expect(textOf(result)).toBe("ok");
    expect(result.isError).toBeUndefined();
  });

  it("converts thrown errors into an isError result", async () => {
    const result = await safeToolCall(async () => {
      throw new JpomApiError(999, "no permission", "GET", "/system/info");
    });
    expect(result.isError).toBe(true);
    expect(textOf(result)).toContain("Jpom API error code=999: no permission");
    expect(textOf(result)).toContain("permission denied");
  });
});
