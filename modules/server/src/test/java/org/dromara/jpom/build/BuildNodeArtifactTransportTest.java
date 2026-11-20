/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import cn.hutool.crypto.digest.DigestUtil;
import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.transport.DataContentType;
import org.dromara.jpom.transport.HttpTransportServer;
import org.dromara.jpom.transport.INodeInfo;
import org.dromara.jpom.transport.IUrlItem;
import org.dromara.jpom.transport.TransportServer;
import org.dromara.jpom.util.LogRecorder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 产物下载端到端回归（真 HTTP + 真 hutool 客户端）。
 * <p>
 * 复现 2026-11-20 现场：节点声明 `Content-Length: 4096` 但只发 1000 字节就断连——hutool 不抛异常，
 * 只给一个短流。下载器必须靠对账发现截断，然后带 `Range` 续传，最终拼出完整产物并通过 sha1 校验。
 *
 * @author jpom
 * @since 2.11.15
 */
class BuildNodeArtifactTransportTest {

    private static final int PAYLOAD_SIZE = 4096;
    private static final int TRUNCATE_AT = 1000;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void initI18n() {
        TestI18nSupport.init();
    }

    @Test
    void truncatedResponseIsDetectedAndResumedOverHttp() throws Exception {
        byte[] payload = payload();
        try (FakeNodeServer server = new FakeNodeServer(payload, TRUNCATE_AT);
             LogRecorder recorder = LogRecorder.builder().file(new File(tempDir.toFile(), "build.log")).build()) {
            INodeInfo nodeInfo = new StubNode(server.port());
            IUrlItem urlItem = new StubUrlItem();
            TransportServer transport = new HttpTransportServer();
            File target = new File(tempDir.toFile(), "artifact.zip");

            ArtifactDownloader downloader = new ArtifactDownloader(target, PAYLOAD_SIZE, DigestUtil.sha1Hex(payload),
                (offset, file) -> {
                    if (offset > 0) {
                        // 与服务端 BuildNodeClient.download 完全一致的续传约定
                        urlItem.header().put("Range", "bytes=" + offset + "-");
                    } else {
                        urlItem.header().remove("Range");
                    }
                    return BuildNodeClient.fetch(transport, nodeInfo, urlItem, null, file, offset);
                }, recorder, 3, milliseconds -> {
            });

            Assertions.assertEquals(PAYLOAD_SIZE, downloader.download());
            Assertions.assertArrayEquals(payload, Files.readAllBytes(target.toPath()));
            // 第二次请求必须是带 Range 的续传，而不是再来一次全量
            List<String> requests = server.requests();
            Assertions.assertEquals(2, requests.size(), "应只重试一次");
            Assertions.assertTrue(requests.get(0).toLowerCase(Locale.ROOT).contains("jpom-agent-authorize"),
                "首个请求应带节点授权头");
            Assertions.assertFalse(requests.get(0).toLowerCase(Locale.ROOT).contains("range:"), "首个请求不应带 Range");
            Assertions.assertTrue(requests.get(1).toLowerCase(Locale.ROOT).contains("range: bytes=" + TRUNCATE_AT + "-"),
                "续传请求应带 Range: bytes=" + TRUNCATE_AT + "-，实际：" + requests.get(1));
        }
    }

    private static byte[] payload() {
        byte[] payload = new byte[PAYLOAD_SIZE];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i * 17 + 3);
        }
        return payload;
    }

    // ---------------------------------------------------------------- 夹具

    /**
     * 假构建节点：首个请求声明全量长度但只发 {@code truncateAt} 字节（模拟链路截断），
     * 带 {@code Range: bytes=N-} 的后续请求返回 206 + 剩余字节。
     */
    private static final class FakeNodeServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final byte[] payload;
        private final int truncateAt;
        private final List<String> requests = Collections.synchronizedList(new ArrayList<>());
        private final Thread thread;
        private volatile boolean closed;

        private FakeNodeServer(byte[] payload, int truncateAt) throws IOException {
            this.payload = payload;
            this.truncateAt = truncateAt;
            this.serverSocket = new ServerSocket(0);
            this.thread = new Thread(this::acceptLoop, "fake-build-node");
            this.thread.setDaemon(true);
            this.thread.start();
        }

        private int port() {
            return this.serverSocket.getLocalPort();
        }

        private List<String> requests() {
            return new ArrayList<>(this.requests);
        }

        private void acceptLoop() {
            while (!this.closed) {
                try (Socket socket = this.serverSocket.accept()) {
                    this.handle(socket);
                } catch (IOException e) {
                    // 关闭时 accept 会抛异常，直接退出
                }
            }
        }

        private void handle(Socket socket) throws IOException {
            BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
            StringBuilder head = new StringBuilder();
            int current;
            while ((current = inputStream.read()) != -1) {
                head.append((char) current);
                int length = head.length();
                if (length >= 4 && head.charAt(length - 4) == '\r' && head.charAt(length - 3) == '\n'
                    && head.charAt(length - 2) == '\r' && head.charAt(length - 1) == '\n') {
                    break;
                }
            }
            String request = head.toString();
            this.requests.add(request);
            int start = 0;
            for (String line : request.split("\r\n")) {
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.startsWith("range:")) {
                    String spec = line.substring("range:".length()).trim();
                    if (spec.startsWith("bytes=")) {
                        spec = spec.substring("bytes=".length());
                    }
                    int dash = spec.indexOf('-');
                    start = Integer.parseInt((dash > 0 ? spec.substring(0, dash) : spec).trim());
                }
            }
            OutputStream outputStream = socket.getOutputStream();
            if (start > 0) {
                this.writeHead(outputStream, "HTTP/1.1 206 Partial Content", this.payload.length - start,
                    "Content-Range: bytes " + start + "-" + (this.payload.length - 1) + "/" + this.payload.length);
                outputStream.write(this.payload, start, this.payload.length - start);
            } else {
                this.writeHead(outputStream, "HTTP/1.1 200 OK", this.payload.length, null);
                // 故意少发：模拟对端提前关闭连接（客户端不会得到异常）
                outputStream.write(this.payload, 0, Math.min(this.truncateAt, this.payload.length));
            }
            outputStream.flush();
        }

        private void writeHead(OutputStream outputStream, String status, int contentLength, String extra) throws IOException {
            StringBuilder head = new StringBuilder(status).append("\r\n")
                .append("Content-Type: application/zip\r\n")
                .append("Content-Length: ").append(contentLength).append("\r\n");
            if (extra != null) {
                head.append(extra).append("\r\n");
            }
            head.append("Connection: close\r\n\r\n");
            outputStream.write(head.toString().getBytes(StandardCharsets.ISO_8859_1));
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            this.serverSocket.close();
        }
    }

    private static final class StubNode implements INodeInfo {

        private final int port;

        private StubNode(int port) {
            this.port = port;
        }

        @Override
        public String name() {
            return "stub-build-node";
        }

        @Override
        public String url() {
            return "127.0.0.1:" + this.port;
        }

        @Override
        public String scheme() {
            return "http";
        }

        @Override
        public String authorize() {
            return "stub-authorize";
        }

        @Override
        public java.net.Proxy proxy() {
            return null;
        }

        @Override
        public Integer timeout() {
            return 30;
        }

        @Override
        public Integer transportEncryption() {
            return 0;
        }
    }

    private static final class StubUrlItem implements IUrlItem {

        private final Map<String, String> header = new HashMap<>();

        @Override
        public String path() {
            return "/build/download";
        }

        @Override
        public Integer timeout() {
            return 30;
        }

        @Override
        public String workspaceId() {
            return "DEFAULT";
        }

        @Override
        public DataContentType contentType() {
            return DataContentType.FORM_URLENCODED;
        }

        @Override
        public Map<String, String> header() {
            return this.header;
        }
    }
}
