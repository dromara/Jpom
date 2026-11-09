/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 		http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import type { JpomConfig } from "./config.js";
import { SERVER_NAME, VERSION } from "./version.js";

export interface JpomEnvelope<T = unknown> {
  code: number;
  msg?: string;
  data: T;
}

export interface RequestOptions {
  query?: Record<string, unknown>;
  form?: Record<string, unknown>;
  json?: unknown;
  timeoutMs?: number;
  accept?: string;
}

export interface TextResponse {
  status: number;
  text: string;
  contentType: string;
}

export class JpomApiError extends Error {
  constructor(
    public readonly code: number,
    message: string,
    public readonly method: string,
    public readonly path: string,
  ) {
    super(message);
    this.name = "JpomApiError";
  }
}

export class JpomHttpError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly method: string,
    public readonly path: string,
    public readonly bodyPreview: string,
  ) {
    super(message);
    this.name = "JpomHttpError";
  }
}

export class JpomNetworkError extends Error {
  constructor(message: string, public readonly method: string, public readonly path: string) {
    super(message);
    this.name = "JpomNetworkError";
  }
}

function appendParam(params: URLSearchParams, key: string, value: unknown): void {
  if (value === undefined || value === null) return;
  if (Array.isArray(value)) {
    for (const item of value) appendParam(params, key, item);
    return;
  }
  if (value instanceof Date) {
    params.append(key, value.toISOString());
    return;
  }
  if (typeof value === "object") {
    params.append(key, JSON.stringify(value));
    return;
  }
  params.append(key, String(value));
}

/**
 * Small HTTP client for the Jpom server Open REST API.
 *
 * Jpom returns HTTP 200 with a JSON envelope `{code, msg, data}`; business errors use a
 * non-200 `code` (for example 800 means the token is invalid / login expired).
 */
export class JpomClient {
  constructor(private readonly config: JpomConfig) {}

  private buildUrl(path: string, query?: Record<string, unknown>): string {
    if (!path.startsWith("/")) {
      throw new Error(`invalid Jpom API path (must start with "/"): ${path}`);
    }
    const url = new URL(`${this.config.baseUrl}${path}`);
    if (query) {
      for (const [key, value] of Object.entries(query)) appendParam(url.searchParams, key, value);
    }
    return url.toString();
  }

  private async fetchRaw(method: string, path: string, options: RequestOptions = {}): Promise<Response> {
    const url = this.buildUrl(path, options.query);
    const headers: Record<string, string> = {
      "JPOM-USER-TOKEN": this.config.token,
      workspaceId: this.config.workspace,
      Accept: options.accept ?? "application/json",
      "User-Agent": `${SERVER_NAME}/${VERSION}`,
    };

    let body: string | undefined;
    if (options.json !== undefined) {
      headers["Content-Type"] = "application/json; charset=UTF-8";
      body = JSON.stringify(options.json);
    } else if (options.form !== undefined) {
      const params = new URLSearchParams();
      for (const [key, value] of Object.entries(options.form)) appendParam(params, key, value);
      headers["Content-Type"] = "application/x-www-form-urlencoded; charset=UTF-8";
      body = params.toString();
    } else if (!["GET", "HEAD"].includes(method.toUpperCase())) {
      headers["Content-Type"] = "application/x-www-form-urlencoded; charset=UTF-8";
      body = "";
    }

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), options.timeoutMs ?? this.config.timeoutMs);
    let response: Response;
    try {
      response = await fetch(url, { method, headers, body, signal: controller.signal, redirect: "follow" });
    } catch (error) {
      const cause = (error as Error & { cause?: { message?: string } }).cause;
      const reason = controller.signal.aborted
        ? `request timed out after ${options.timeoutMs ?? this.config.timeoutMs}ms`
        : (cause?.message ?? (error as Error).message);
      throw new JpomNetworkError(`${method} ${path} failed: ${reason}`, method, path);
    } finally {
      clearTimeout(timer);
    }
    return response;
  }

  async request<T = unknown>(method: string, path: string, options: RequestOptions = {}): Promise<JpomEnvelope<T>> {
    const response = await this.fetchRaw(method, path, options);
    const text = await response.text();
    if (text.trim() === "") {
      if (response.ok) {
        return { code: response.status, msg: "", data: undefined as T };
      }
      throw new JpomHttpError(`${method} ${path} returned HTTP ${response.status}`, response.status, method, path, "");
    }

    let parsed: unknown;
    try {
      parsed = JSON.parse(text);
    } catch {
      throw new JpomHttpError(
        `${method} ${path} returned a non-JSON response (HTTP ${response.status})`,
        response.status,
        method,
        path,
        text.slice(0, 500),
      );
    }

    if (parsed !== null && typeof parsed === "object" && "code" in parsed) {
      const envelope = parsed as JpomEnvelope<T>;
      if (typeof envelope.code !== "number") {
        throw new JpomHttpError(`${method} ${path} returned an invalid Jpom response`, response.status, method, path, text.slice(0, 500));
      }
      if (envelope.code !== 200) {
        const message = envelope.msg && envelope.msg.length > 0 ? envelope.msg : `Jpom API returned code ${envelope.code}`;
        throw new JpomApiError(envelope.code, message, method, path);
      }
      return envelope;
    }

    // Some legacy endpoints return raw data without an envelope.
    return { code: response.status, msg: "", data: parsed as T };
  }

  async requestText(method: string, path: string, options: RequestOptions = {}): Promise<TextResponse> {
    const response = await this.fetchRaw(method, path, options);
    const text = await response.text();
    if (!response.ok) {
      throw new JpomHttpError(
        `${method} ${path} returned HTTP ${response.status}`,
        response.status,
        method,
        path,
        text.slice(0, 500),
      );
    }
    return {
      status: response.status,
      text,
      contentType: response.headers.get("content-type") ?? "",
    };
  }

  get<T = unknown>(path: string, query?: Record<string, unknown>): Promise<JpomEnvelope<T>> {
    return this.request<T>("GET", path, { query });
  }

  postForm<T = unknown>(path: string, form: Record<string, unknown> = {}): Promise<JpomEnvelope<T>> {
    return this.request<T>("POST", path, { form });
  }
}
