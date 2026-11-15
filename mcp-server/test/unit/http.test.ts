/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 		http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { JpomConfig } from "../../src/config.js";
import {
  JpomApiError,
  JpomClient,
  JpomHttpError,
  JpomNetworkError,
} from "../../src/http.js";
import { SERVER_NAME, VERSION } from "../../src/version.js";

function testConfig(overrides: Partial<JpomConfig> = {}): JpomConfig {
  return {
    baseUrl: "http://jpom.test:2122",
    token: "secret-token",
    workspace: "WS1",
    readOnly: true,
    timeoutMs: 5000,
    maxResultChars: 50000,
    verifyTls: true,
    ...overrides,
  };
}

type FetchMock = ReturnType<typeof vi.fn>;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json;charset=UTF-8" },
  });
}

function stubFetch(handler: (url: string, init: RequestInit) => Promise<Response> | Response): FetchMock {
  const mock = vi.fn(async (url: string | URL | Request, init?: RequestInit) =>
    handler(String(url), init ?? {}),
  );
  vi.stubGlobal("fetch", mock);
  return mock as unknown as FetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("JpomClient request headers", () => {
  it("sends JPOM-USER-TOKEN, workspaceId and User-Agent", async () => {
    const fetchMock = stubFetch(() => jsonResponse({ code: 200, data: [] }));
    const client = new JpomClient(testConfig());

    await client.get("/user/my-workspace");

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("http://jpom.test:2122/user/my-workspace");
    const headers = init.headers as Record<string, string>;
    expect(headers["JPOM-USER-TOKEN"]).toBe("secret-token");
    expect(headers.workspaceId).toBe("WS1");
    expect(headers.Accept).toBe("application/json");
    expect(headers["User-Agent"]).toBe(`${SERVER_NAME}/${VERSION}`);
    expect(init.method).toBe("GET");
    expect(init.redirect).toBe("follow");
  });

  it("supports a custom Accept header (log downloads)", async () => {
    const fetchMock = stubFetch(() => new Response("line1\nline2\n", { status: 200, headers: { "content-type": "text/plain" } }));
    const client = new JpomClient(testConfig());

    const result = await client.requestText("GET", "/system/log_download", { accept: "text/plain" });

    expect(result).toEqual({ status: 200, text: "line1\nline2\n", contentType: "text/plain" });
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect((init.headers as Record<string, string>).Accept).toBe("text/plain");
  });
});

describe("JpomClient GET query encoding", () => {
  it("encodes strings, numbers, booleans, arrays, dates and objects; skips null/undefined", async () => {
    const fetchMock = stubFetch(() => jsonResponse({ code: 200, data: null }));
    const client = new JpomClient(testConfig());
    const date = new Date("2026-01-02T03:04:05.000Z");

    await client.get("/node/list_data.json", {
      page: 1,
      enabled: true,
      ids: ["a", "b"],
      since: date,
      filter: { name: "demo" },
      empty: undefined,
      nil: null,
    });

    const [url] = fetchMock.mock.calls[0] as [string];
    const parsed = new URL(url);
    expect(parsed.pathname).toBe("/node/list_data.json");
    expect(parsed.searchParams.get("page")).toBe("1");
    expect(parsed.searchParams.get("enabled")).toBe("true");
    expect(parsed.searchParams.getAll("ids")).toEqual(["a", "b"]);
    expect(parsed.searchParams.get("since")).toBe(date.toISOString());
    expect(parsed.searchParams.get("filter")).toBe('{"name":"demo"}');
    expect(parsed.searchParams.has("empty")).toBe(false);
    expect(parsed.searchParams.has("nil")).toBe(false);
  });

  it("rejects API paths that do not start with a slash", async () => {
    stubFetch(() => jsonResponse({ code: 200, data: null }));
    const client = new JpomClient(testConfig());
    await expect(client.get("user/my-workspace")).rejects.toThrow(/must start with/);
  });
});

describe("JpomClient POST form encoding", () => {
  it("sends application/x-www-form-urlencoded body with repeated arrays", async () => {
    const fetchMock = stubFetch(() => jsonResponse({ code: 200, data: { total: 0, result: [] } }));
    const client = new JpomClient(testConfig());

    await client.postForm("/node/list_data.json", {
      page: 1,
      limit: 20,
      name: "hello world",
      tags: ["x", "y"],
      skip: undefined,
    });

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    const headers = init.headers as Record<string, string>;
    expect(headers["Content-Type"]).toBe("application/x-www-form-urlencoded; charset=UTF-8");
    expect(init.method).toBe("POST");
    expect(init.body).toBe("page=1&limit=20&name=hello+world&tags=x&tags=y");
  });

  it("sends a JSON body and JSON content type for json requests", async () => {
    const fetchMock = stubFetch(() => jsonResponse({ code: 200, data: null }));
    const client = new JpomClient(testConfig());

    await client.request("POST", "/system/upgrade", { json: { version: "2.11.13" } });

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    const headers = init.headers as Record<string, string>;
    expect(headers["Content-Type"]).toBe("application/json; charset=UTF-8");
    expect(init.body).toBe('{"version":"2.11.13"}');
  });

  it("sends an empty form body for POST without options", async () => {
    const fetchMock = stubFetch(() => jsonResponse({ code: 200, data: null }));
    const client = new JpomClient(testConfig());

    await client.request("POST", "/system/info");

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(init.body).toBe("");
    expect((init.headers as Record<string, string>)["Content-Type"]).toBe(
      "application/x-www-form-urlencoded; charset=UTF-8",
    );
  });
});

describe("JpomClient error mapping", () => {
  it("throws JpomApiError for non-200 business codes in a HTTP 200 envelope", async () => {
    stubFetch(() => jsonResponse({ code: 800, msg: "token invalid", data: null }));
    const client = new JpomClient(testConfig());

    const error = await client.get("/user/my-workspace").catch((err: unknown) => err);
    expect(error).toBeInstanceOf(JpomApiError);
    const apiError = error as JpomApiError;
    expect(apiError.code).toBe(800);
    expect(apiError.message).toBe("token invalid");
    expect(apiError.method).toBe("GET");
    expect(apiError.path).toBe("/user/my-workspace");
  });

  it("uses a fallback message when the envelope has no msg", async () => {
    stubFetch(() => jsonResponse({ code: 500, data: null }));
    const client = new JpomClient(testConfig());
    await expect(client.get("/x")).rejects.toThrow(/code 500/);
  });

  it("throws JpomHttpError for a non-2xx HTTP status with a non-JSON body", async () => {
    stubFetch(() => new Response("Bad Gateway", { status: 502 }));
    const client = new JpomClient(testConfig());

    const error = await client.get("/node/list_data.json").catch((err: unknown) => err);
    expect(error).toBeInstanceOf(JpomHttpError);
    expect((error as JpomHttpError).status).toBe(502);
    expect((error as JpomHttpError).bodyPreview).toContain("Bad Gateway");
    expect((error as JpomHttpError).message).toContain("HTTP 502");
  });

  it("throws JpomHttpError for a non-2xx HTTP status with an empty body", async () => {
    stubFetch(() => new Response("", { status: 500 }));
    const client = new JpomClient(testConfig());

    const error = await client.get("/node/list_data.json").catch((err: unknown) => err);
    expect(error).toBeInstanceOf(JpomHttpError);
    expect((error as JpomHttpError).status).toBe(500);
  });

  it("throws JpomHttpError for a non-JSON HTTP 200 response", async () => {
    stubFetch(() => new Response("<html>login required</html>", { status: 200, headers: { "content-type": "text/html" } }));
    const client = new JpomClient(testConfig());

    const error = await client.get("/node/list_data.json").catch((err: unknown) => err);
    expect(error).toBeInstanceOf(JpomHttpError);
    expect((error as JpomHttpError).message).toContain("non-JSON");
    expect((error as JpomHttpError).bodyPreview).toContain("<html>");
  });

  it("returns raw (non-envelope) JSON payloads unchanged", async () => {
    stubFetch(() => jsonResponse(["a", "b"]));
    const client = new JpomClient(testConfig());
    await expect(client.get("/raw")).resolves.toEqual({ code: 200, data: ["a", "b"], msg: "" });
  });

  it("throws JpomNetworkError and keeps the fetch failure cause", async () => {
    stubFetch(() => Promise.reject(new TypeError("fetch failed", { cause: new Error("connect ECONNREFUSED") })));
    const client = new JpomClient(testConfig());

    const error = await client.get("/node/list_data.json").catch((err: unknown) => err);
    expect(error).toBeInstanceOf(JpomNetworkError);
    expect((error as JpomNetworkError).message).toContain("connect ECONNREFUSED");
    expect((error as JpomNetworkError).method).toBe("GET");
  });

  it("throws JpomNetworkError when the request times out", async () => {
    stubFetch((_url, init) =>
      new Promise<Response>((_resolve, reject) => {
        init.signal?.addEventListener("abort", () => {
          reject(new DOMException("The operation was aborted.", "AbortError"));
        });
      }),
    );
    const client = new JpomClient(testConfig({ timeoutMs: 20 }));

    const error = await client.get("/slow").catch((err: unknown) => err);
    expect(error).toBeInstanceOf(JpomNetworkError);
    expect((error as JpomNetworkError).message).toContain("timed out after 20ms");
  });

  it("throws JpomHttpError from requestText for non-2xx responses", async () => {
    stubFetch(() => new Response("forbidden", { status: 403 }));
    const client = new JpomClient(testConfig());

    const error = await client.requestText("GET", "/system/log_download", { query: { path: "server.log" } }).catch(
      (err: unknown) => err,
    );
    expect(error).toBeInstanceOf(JpomHttpError);
    expect((error as JpomHttpError).status).toBe(403);
  });
});

describe("JpomClient envelope handling", () => {
  it("returns the parsed envelope for code=200", async () => {
    stubFetch(() => jsonResponse({ code: 200, msg: "ok", data: { result: [1, 2], total: 2 } }));
    const client = new JpomClient(testConfig());

    const response = await client.postForm("/node/list_data.json", { page: 1 });
    expect(response.code).toBe(200);
    expect(response.data).toEqual({ result: [1, 2], total: 2 });
  });

  it("surfaces an empty successful HTTP body as a synthetic envelope", async () => {
    stubFetch(() => new Response("", { status: 200 }));
    const client = new JpomClient(testConfig());

    await expect(client.request("DELETE", "/node/x")).resolves.toEqual({ code: 200, msg: "", data: undefined });
  });
});
