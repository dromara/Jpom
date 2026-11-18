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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.dromara.jpom.transport.DataContentType;
import org.dromara.jpom.transport.IUrlItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 反向隧道端到端集成测试：
 * 真实内嵌 Tomcat 提供 {@code /agent/tunnel} 与 {@code /socket/echo}，插件端用 {@link TunnelClient} 拨号，
 * 请求经隧道转发到本机桩 HTTP 服务（等价于插件端 controller），验证 execute / download / 双向流 / 鉴权拒绝。
 *
 * @author jpom
 * @since 2026/11/18
 */
@SpringBootTest(classes = TunnelTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TunnelEndToEndTest {

    private static final int PAYLOAD_SIZE = 300 * 1024;
    private static final byte[] DOWNLOAD_BYTES = new byte[PAYLOAD_SIZE];

    static {
        for (int i = 0; i < PAYLOAD_SIZE; i++) {
            DOWNLOAD_BYTES[i] = (byte) (i % 251);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TunnelChannelRegistry registry;

    private static HttpServer stubServer;
    private static TunnelClient tunnelClient;

    @BeforeAll
    public static void startStub() throws IOException {
        stubServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stubServer.createContext("/stub/json", exchange -> {
            String body = readBody(exchange.getRequestBody());
            String response = "{\"path\":\"/stub/json\",\"method\":\"" + exchange.getRequestMethod()
                + "\",\"workspaceId\":\"" + exchange.getRequestHeaders().getFirst("workspaceId")
                + "\",\"contentType\":\"" + exchange.getRequestHeaders().getFirst("Content-Type")
                + "\",\"body\":\"" + body.replace("\"", "'") + "\"}";
            respond(exchange, 200, response.getBytes(StandardCharsets.UTF_8), "application/json", null);
        });
        stubServer.createContext("/stub/download", exchange ->
            respond(exchange, 200, DOWNLOAD_BYTES, "application/octet-stream", "attachment; filename=\"a.bin\""));
        stubServer.createContext("/stub/slow", exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "slow".getBytes(StandardCharsets.UTF_8), "text/plain", null);
        });
        stubServer.start();
    }

    @AfterAll
    public static void stopStub() {
        if (tunnelClient != null) {
            tunnelClient.stop();
        }
        if (stubServer != null) {
            stubServer.stop(0);
        }
    }

    @Test
    public void testTunnelEndToEnd() throws Exception {
        tunnelClient = startClient(TunnelTestApplication.KEY);
        Assertions.assertTrue(waitOnline(20_000), "隧道应在 20 秒内上线，最后一次错误：" + tunnelClient.getLastError());
        TunnelChannel channel = registry.get(TunnelTestApplication.MACHINE_ID);
        Assertions.assertNotNull(channel);

        // 1) execute：请求经隧道到桩服务，header 透传、body 透传
        TunnelResponse response = channel.request(urlItem("/stub/json", "/stub/json", "default"), "a=1&b=2".getBytes(StandardCharsets.UTF_8), 30);
        Assertions.assertEquals(200, response.getStatus());
        String body = response.bodyString();
        Assertions.assertTrue(body.contains("\"workspaceId\":\"default\""), body);
        Assertions.assertTrue(body.contains("\"body\":\"a=1&b=2\""), body);
        Assertions.assertTrue(body.contains("application/x-www-form-urlencoded"), body);

        // 2) download：流式返回，不缓存完整响应体
        TunnelDownload download = channel.download(urlItem("/stub/download", "/stub/download", "default"), null, 30);
        Assertions.assertEquals(200, download.getStatus());
        Assertions.assertTrue(download.getContentDisposition().contains("a.bin"), download.getContentDisposition());
        byte[] received = readAll(download.getInputStream());
        Assertions.assertEquals(PAYLOAD_SIZE, received.length);
        Assertions.assertArrayEquals(DOWNLOAD_BYTES, received);

        // 3) 双向流：服务端 <-> 隧道 <-> 本机 /socket/echo
        IUrlItem streamItem = urlItem("/socket/echo", "/socket/echo", "default");
        TunnelProxyWebSocket proxy = TunnelProxyWebSocket.of(channel, streamItem, 10);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> message = new AtomicReference<>();
        proxy.onMessage(text -> {
            message.set(text);
            latch.countDown();
        });
        Assertions.assertTrue(proxy.connectBlocking(), "双向流应打开成功");
        proxy.send("hello-tunnel");
        Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "应收到回显消息");
        Assertions.assertEquals("echo:hello-tunnel", message.get());
        proxy.close();
    }

    @Test
    public void testRequestTimeoutAborts() throws Exception {
        TunnelClient client = startClient(TunnelTestApplication.KEY);
        try {
            Assertions.assertTrue(waitOnline(20_000), "隧道应上线");
            TunnelChannel channel = registry.get(TunnelTestApplication.MACHINE_ID);
            TunnelException exception = Assertions.assertThrows(TunnelException.class,
                () -> channel.request(urlItem("/stub/slow", "/stub/slow", "default"), null, 1));
            Assertions.assertEquals(504, exception.getCode(), "慢接口应按超时返回 504");
        } finally {
            client.stop();
        }
    }

    @Test
    public void testWrongKeyRejected() throws Exception {
        TunnelClient client = startClient("wrong-key");
        try {
            long deadline = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < deadline && client.getLastError() == null) {
                Thread.sleep(200);
            }
            Assertions.assertFalse(client.isConnected(), "连接密钥错误时不应上线");
            Assertions.assertNotNull(client.getLastError());
            Assertions.assertTrue(client.getLastError().contains("密钥"), "应返回明确的鉴权失败原因：" + client.getLastError());
        } finally {
            client.stop();
        }
    }

    private TunnelClient startClient(String key) {
        TunnelClientOptions options = new TunnelClientOptions();
        options.setServerUrl("ws://127.0.0.1:" + port);
        options.setMachineId(TunnelTestApplication.MACHINE_ID);
        options.setKey(key);
        options.setAgentVersion(TunnelTestApplication.AGENT_VERSION);
        options.setOs("linux");
        options.setArch("amd64");
        options.setLocalHttpBase("http://127.0.0.1:" + stubServer.getAddress().getPort());
        options.setLocalWsBase("ws://127.0.0.1:" + port);
        options.setMaxConcurrency(8);
        options.setTimeoutSeconds(20);
        Map<String, String> headers = new HashMap<>();
        headers.put("Jpom-Agent-Authorize", "stub-authorize");
        options.setLocalHeaders(headers);
        options.setLocalQuery(Collections.singletonMap("Jpom-Agent-Authorize", "stub-authorize"));
        options.setRequestHandler(new LocalHttpTunnelHandler(options.getLocalHttpBase(), headers, 20));
        options.setStreamHandler(new LocalWebSocketTunnelHandler(options.getLocalWsBase(), options.getLocalQuery(), 20));
        TunnelClient client = new TunnelClient(options);
        client.start();
        return client;
    }

    private boolean waitOnline(long timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < deadline) {
            if (registry.isOnline(TunnelTestApplication.MACHINE_ID)) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }

    private static IUrlItem urlItem(String path, String description, String workspaceId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept-Language", "zh_CN");
        return new IUrlItem() {
            @Override
            public String path() {
                return path;
            }

            @Override
            public Integer timeout() {
                return 30;
            }

            @Override
            public String workspaceId() {
                return workspaceId;
            }

            @Override
            public DataContentType contentType() {
                return DataContentType.FORM_URLENCODED;
            }

            @Override
            public Map<String, String> header() {
                return headers;
            }

            @Override
            public String toString() {
                return description;
            }
        };
    }

    private static String readBody(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    private static byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private static void respond(HttpExchange exchange, int status, byte[] body, String contentType, String disposition) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        if (disposition != null) {
            exchange.getResponseHeaders().add("Content-Disposition", disposition);
        }
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }
}
