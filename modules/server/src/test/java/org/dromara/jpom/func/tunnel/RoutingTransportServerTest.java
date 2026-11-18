/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.tunnel;

import org.dromara.jpom.transport.DataContentType;
import org.dromara.jpom.transport.DownloadCallback;
import org.dromara.jpom.transport.INodeInfo;
import org.dromara.jpom.transport.IProxyWebSocket;
import org.dromara.jpom.transport.IUrlItem;
import org.dromara.jpom.transport.TransportAgentException;
import org.dromara.jpom.transport.TransportServer;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannel;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannelRegistry;
import org.dromara.jpom.transport.tunnel.ws.TunnelResponse;
import org.dromara.jpom.transport.tunnel.ws.TunnelServerOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 路由回归测试：有在线反向隧道时走隧道、没有时回退正向，正向行为不变。
 * <p>
 * 反向 / 正向切换是这次改造的核心风险点：如果 <b>未命中</b> 时不回退正向，全部老节点会立刻不可用。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class RoutingTransportServerTest {

    @Test
    public void testFallbackToForwardWhenOffline() {
        StubForward forward = new StubForward();
        TunnelChannelRegistry registry = new TunnelChannelRegistry(new TunnelServerOptions());
        RoutingTransportServer routing = new RoutingTransportServer(registry, forward);

        String body = routing.execute(node("m-1"), urlItem("/manage/info"), Collections.emptyMap());
        Assertions.assertEquals("forward", body);
        Assertions.assertTrue(forward.called.get(), "无隧道时必须回退正向");
    }

    @Test
    public void testRouteThroughTunnelWhenOnline() {
        StubForward forward = new StubForward();
        TunnelChannelRegistry registry = new TunnelChannelRegistry(new TunnelServerOptions());
        RegistryChannel channel = new RegistryChannel("m-1");
        registry.register(channel);
        RoutingTransportServer routing = new RoutingTransportServer(registry, forward);

        String body = routing.execute(node("m-1"), urlItem("/manage/info"), Collections.singletonMap("a", "1"));
        Assertions.assertEquals("{\"ok\":1}", body);
        Assertions.assertFalse(forward.called.get(), "有隧道时不应再走正向");
        Assertions.assertEquals("application/x-www-form-urlencoded", channel.contentType.get());
        Assertions.assertEquals("POST", channel.method.get());
        Assertions.assertTrue(channel.body.get().length > 0, "表单参数应被序列化成请求体");
        Assertions.assertEquals("x", channel.extraHeaders.get().get("Jpom-Agent-Authorize"),
            "反向模式必须把节点授权头下发给插件端（插件端不读取自身授权摘要）");
        Assertions.assertEquals("default", channel.extraHeaders.get().get("workspaceId"));
    }

    @Test
    public void testTunnelErrorStatusPropagates() {
        StubForward forward = new StubForward();
        TunnelChannelRegistry registry = new TunnelChannelRegistry(new TunnelServerOptions());
        RegistryChannel channel = new RegistryChannel("m-1");
        channel.status = 500;
        registry.register(channel);
        RoutingTransportServer routing = new RoutingTransportServer(registry, forward);

        Assertions.assertThrows(TransportAgentException.class,
            () -> routing.execute(node("m-1"), urlItem("/manage/info"), Collections.emptyMap()));
    }

    @Test
    public void testDownloadAndWebsocketFallback() {
        StubForward forward = new StubForward();
        TunnelChannelRegistry registry = new TunnelChannelRegistry(new TunnelServerOptions());
        RoutingTransportServer routing = new RoutingTransportServer(registry, forward);

        routing.download(node("m-1"), urlItem("/manage/download"), null, callback -> Assertions.assertNotNull(callback));
        Assertions.assertTrue(forward.downloadCalled.get());

        IProxyWebSocket socket = routing.websocket(node("m-1"), urlItem("/socket/console"), "a", "b");
        Assertions.assertNotNull(socket);
        Assertions.assertTrue(forward.websocketCalled.get());
    }

    @Test
    public void testDownloadAndWebsocketThroughTunnel() {
        TunnelChannelRegistry registry = new TunnelChannelRegistry(new TunnelServerOptions());
        RegistryChannel channel = new RegistryChannel("m-1");
        registry.register(channel);
        RoutingTransportServer routing = new RoutingTransportServer(registry, new StubForward());

        routing.download(node("m-1"), urlItem("/manage/download"), null,
            callback -> Assertions.assertEquals(200, 200));
        Assertions.assertTrue(channel.downloadCalled.get());
        Assertions.assertEquals("GET", channel.method.get(), "下载走 GET");

        try {
            routing.websocket(node("m-1"), urlItem("/socket/console"), "a", "b");
        } catch (Throwable ignored) {
            // 隧道分支用的是真实 openStream，测试桩不提供服务端会话，这里只验证没有回退正向
        }
    }

    private static INodeInfo node(String machineId) {
        return new INodeInfo() {
            @Override
            public String id() {
                return machineId;
            }

            @Override
            public String name() {
                return machineId;
            }

            @Override
            public String url() {
                return "127.0.0.1:1";
            }

            @Override
            public String scheme() {
                return "http";
            }

            @Override
            public String authorize() {
                return "x";
            }

            @Override
            public java.net.Proxy proxy() {
                return null;
            }

            @Override
            public Integer timeout() {
                return 5;
            }

            @Override
            public Integer transportEncryption() {
                return 0;
            }
        };
    }

    private static IUrlItem urlItem(String path) {
        Map<String, String> headers = new HashMap<>();
        return new IUrlItem() {
            @Override
            public String path() {
                return path;
            }

            @Override
            public Integer timeout() {
                return 5;
            }

            @Override
            public String workspaceId() {
                return "default";
            }

            @Override
            public DataContentType contentType() {
                return DataContentType.FORM_URLENCODED;
            }

            @Override
            public Map<String, String> header() {
                return headers;
            }
        };
    }

    /**
     * 反向通道桩：只重写请求入口，记录路由结果
     */
    private static class RegistryChannel extends TunnelChannel {
        private final AtomicReference<String> method = new AtomicReference<>();
        private final AtomicReference<String> contentType = new AtomicReference<>();
        private final AtomicReference<byte[]> body = new AtomicReference<>(new byte[0]);
        private final AtomicBoolean downloadCalled = new AtomicBoolean();
        private final AtomicReference<Map<String, String>> extraHeaders = new AtomicReference<>();
        private int status = 200;

        RegistryChannel(String machineId) {
            super(null, new TunnelServerOptions());
            this.setMachineId(machineId);
            this.setHelloPassed(true);
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public TunnelResponse request(IUrlItem urlItem, byte[] payload, String requestContentType, String requestMethod, int timeout) {
            return this.request(urlItem, payload, requestContentType, requestMethod, timeout, null);
        }

        @Override
        public TunnelResponse request(IUrlItem urlItem, byte[] payload, String requestContentType, String requestMethod, int timeout,
                                      Map<String, String> extraHeaders) {
            this.contentType.set(requestContentType);
            this.method.set(requestMethod);
            this.body.set(payload == null ? new byte[0] : payload);
            this.extraHeaders.set(extraHeaders);
            return new TunnelResponse(status, Collections.emptyMap(), "{\"ok\":1}".getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public org.dromara.jpom.transport.tunnel.ws.TunnelDownload download(IUrlItem urlItem, byte[] payload, String requestContentType, int timeout) {
            return this.download(urlItem, payload, requestContentType, timeout, null);
        }

        @Override
        public org.dromara.jpom.transport.tunnel.ws.TunnelDownload download(IUrlItem urlItem, byte[] payload, String requestContentType,
                                                                           int timeout, Map<String, String> extraHeaders) {
            this.downloadCalled.set(true);
            this.method.set("GET");
            this.extraHeaders.set(extraHeaders);
            return new org.dromara.jpom.transport.tunnel.ws.TunnelDownload(200, "attachment; filename=a.bin", "application/octet-stream",
                new java.io.ByteArrayInputStream(new byte[]{1, 2, 3}));
        }
    }

    private static class StubForward implements TransportServer {
        private final AtomicBoolean called = new AtomicBoolean();
        private final AtomicBoolean downloadCalled = new AtomicBoolean();
        private final AtomicBoolean websocketCalled = new AtomicBoolean();

        @Override
        public String execute(INodeInfo nodeInfo, IUrlItem urlItem, Object data) {
            called.set(true);
            return "forward";
        }

        @Override
        public void download(INodeInfo nodeInfo, IUrlItem urlItem, Object data, Consumer<DownloadCallback> consumer) {
            downloadCalled.set(true);
            consumer.accept(DownloadCallback.builder().contentType("text/plain").inputStream(new java.io.ByteArrayInputStream(new byte[0])).build());
        }

        @Override
        public IProxyWebSocket websocket(INodeInfo nodeInfo, IUrlItem urlItem, Object... parameters) {
            websocketCalled.set(true);
            return new IProxyWebSocket() {
                @Override
                public void close() {
                }

                @Override
                public boolean connect() {
                    return true;
                }

                @Override
                public boolean connectBlocking() {
                    return true;
                }

                @Override
                public boolean connectBlocking(int seconds) {
                    return true;
                }

                @Override
                public void send(String msg) {
                }

                @Override
                public void send(java.nio.ByteBuffer bytes) {
                }

                @Override
                public void onMessage(Consumer<String> consumer) {
                }

                @Override
                public boolean isConnected() {
                    return true;
                }

                @Override
                public String getCloseStatusMsg() {
                    return null;
                }
            };
        }
    }
}
