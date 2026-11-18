/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.tunnel.ws;

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.transport.tunnel.TunnelFrame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 插件端默认请求处理器：把隧道 {@code REQ} 还原成一次本机 HTTP 调用。
 * <p>
 * 复用插件端已有 controller、参数校验、传输解密与国际化，业务代码零改动。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class LocalHttpTunnelHandler implements TunnelRequestHandler {

    private static final Set<String> SKIP_REQUEST_HEADERS = new java.util.HashSet<>(java.util.Arrays.asList(
        "host", "connection", "content-length", "transfer-encoding", "accept-encoding", "keep-alive", "upgrade"
    ));
    private static final Set<String> SKIP_RESPONSE_HEADERS = new java.util.HashSet<>(java.util.Arrays.asList(
        "content-length", "transfer-encoding", "connection", "keep-alive", "server", "date"
    ));
    private static final int BUFFER_SIZE = 32 * 1024;

    private final String baseUrl;
    private final java.util.function.Supplier<Map<String, String>> fixedHeaders;
    private final int timeoutSeconds;

    /**
     * 固定请求头（在构造时求值，适用于值不变化的场景）
     *
     * @param baseUrl        本机 HTTP 基础地址
     * @param fixedHeaders   固定请求头
     * @param timeoutSeconds 超时（秒）
     */
    public LocalHttpTunnelHandler(String baseUrl, Map<String, String> fixedHeaders, int timeoutSeconds) {
        this(baseUrl, () -> fixedHeaders, timeoutSeconds);
    }

    /**
     * 动态请求头：每次请求时求值（插件端授权信息在启动阶段才初始化完成，不能在构造时固化）
     *
     * @param baseUrl        本机 HTTP 基础地址
     * @param fixedHeaders   请求头提供者
     * @param timeoutSeconds 超时（秒）
     */
    public LocalHttpTunnelHandler(String baseUrl, java.util.function.Supplier<Map<String, String>> fixedHeaders, int timeoutSeconds) {
        this.baseUrl = StrUtil.removeSuffix(baseUrl, "/");
        this.fixedHeaders = fixedHeaders == null ? Collections::emptyMap : fixedHeaders;
        this.timeoutSeconds = Math.max(timeoutSeconds, 1);
    }

    @Override
    public void handle(TunnelFrame request, byte[] body, TunnelResponseSender sender) throws Exception {
        String path = request.getPath();
        if (StrUtil.isEmpty(path)) {
            sender.abort(400, "缺少请求路径");
            return;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(this.baseUrl + path).openConnection();
        try {
            String method = StrUtil.emptyToDefault(request.getMethod(), "POST");
            connection.setRequestMethod(method);
            connection.setConnectTimeout(timeoutSeconds * 1000);
            connection.setReadTimeout(timeoutSeconds * 1000);
            connection.setInstanceFollowRedirects(false);
            // 固定请求头（插件端自身的授权与加密约定）
            Map<String, String> fixedValues = this.fixedHeaders.get();
            if (fixedValues != null) {
                for (Map.Entry<String, String> entry : fixedValues.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            if (request.getHeaders() != null) {
                for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                    if (entry.getKey() == null || SKIP_REQUEST_HEADERS.contains(entry.getKey().toLowerCase())) {
                        continue;
                    }
                    connection.setRequestProperty(entry.getKey(), StrUtil.nullToEmpty(entry.getValue()));
                }
            }
            if (StrUtil.isNotEmpty(request.getContentType())) {
                connection.setRequestProperty("Content-Type", request.getContentType());
            }
            byte[] requestBody = body == null ? new byte[0] : body;
            boolean hasBody = requestBody.length > 0 && !"GET".equalsIgnoreCase(method);
            connection.setDoInput(true);
            if (hasBody) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(requestBody.length);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(requestBody);
                }
            }
            int status = connection.getResponseCode();
            Map<String, String> headers = new LinkedHashMap<>();
            for (Map.Entry<String, java.util.List<String>> entry : connection.getHeaderFields().entrySet()) {
                String name = entry.getKey();
                if (name == null || SKIP_RESPONSE_HEADERS.contains(name.toLowerCase())) {
                    continue;
                }
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    headers.put(name, entry.getValue().get(0));
                }
            }
            sender.head(status, headers);
            InputStream inputStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (inputStream != null) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    byte[] chunk = new byte[read];
                    System.arraycopy(buffer, 0, chunk, 0, read);
                    sender.write(chunk);
                }
                inputStream.close();
            }
            sender.end();
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 读取全部数据（测试辅助）
     *
     * @param inputStream 输入流
     * @return 字节
     * @throws IOException 读取异常
     */
    static byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }
}
