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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;

/**
 * 通道注册表回归测试：同一节点重复连接顶替旧连接、下线事件只通知一次、离线后不再返回通道。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelChannelRegistryTest {

    @Test
    public void testRegisterReplacesOldChannel() {
        TunnelChannelRegistry registry = new TunnelChannelRegistry(new TunnelServerOptions());
        List<String> events = new ArrayList<>();
        registry.addListener(new TunnelConnectionListener() {
            @Override
            public void online(TunnelChannel channel) {
                events.add("online:" + channel.sessionId());
            }

            @Override
            public void offline(String machineId, int code, String reason) {
                events.add("offline:" + machineId + ":" + code);
            }
        });

        StubSession session1 = new StubSession();
        TunnelChannel first = new TunnelChannel(session1, new TunnelServerOptions());
        first.setMachineId("m-1");
        first.setHelloPassed(true);
        registry.register(first);
        Assertions.assertTrue(registry.isOnline("m-1"));
        Assertions.assertSame(first, registry.get("m-1"));

        StubSession session2 = new StubSession();
        TunnelChannel second = new TunnelChannel(session2, new TunnelServerOptions());
        second.setMachineId("m-1");
        second.setHelloPassed(true);
        registry.register(second);

        Assertions.assertSame(second, registry.get("m-1"), "新连接应顶替旧连接");
        Assertions.assertTrue(session1.closed, "旧连接应被关闭");
        Assertions.assertEquals(4004, session1.closeCode);
        Assertions.assertTrue(events.contains("online:" + first.sessionId()));
        Assertions.assertEquals(1, events.stream().filter("offline:m-1:4004"::equals).count(), "顶替只通知一次下线");
    }

    @Test
    public void testRemoveOnlyForCurrentChannel() {
        TunnelChannelRegistry registry = new TunnelChannelRegistry(new TunnelServerOptions());
        TunnelChannel first = new TunnelChannel(new StubSession(), new TunnelServerOptions());
        first.setMachineId("m-2");
        registry.register(first);

        TunnelChannel other = new TunnelChannel(new StubSession(), new TunnelServerOptions());
        other.setMachineId("m-2");
        // 未注册的通道移除不应影响已注册通道
        registry.remove(other);
        Assertions.assertTrue(registry.isOnline("m-2"));

        registry.remove(first);
        Assertions.assertFalse(registry.isOnline("m-2"));
    }

    @Test
    public void testGetDropsClosedChannel() {
        TunnelChannelRegistry registry = new TunnelChannelRegistry(new TunnelServerOptions());
        StubSession session = new StubSession();
        TunnelChannel channel = new TunnelChannel(session, new TunnelServerOptions());
        channel.setMachineId("m-3");
        registry.register(channel);

        session.open = false;
        Assertions.assertNull(registry.get("m-3"), "底层连接已关闭时应视为离线");
        Assertions.assertFalse(registry.isOnline("m-3"));
    }

    private static class StubSession implements WebSocketSession {
        private boolean open = true;
        private boolean closed;
        private int closeCode;
        private final java.util.Map<String, Object> attributes = new java.util.HashMap<>();

        @Override
        public String getId() {
            return "stub";
        }

        @Override
        public java.net.URI getUri() {
            return java.net.URI.create("ws://localhost/agent/tunnel");
        }

        @Override
        public org.springframework.http.HttpHeaders getHandshakeHeaders() {
            return new org.springframework.http.HttpHeaders();
        }

        @Override
        public java.util.Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public java.security.Principal getPrincipal() {
            return null;
        }

        @Override
        public java.net.InetSocketAddress getLocalAddress() {
            return new java.net.InetSocketAddress("127.0.0.1", 2122);
        }

        @Override
        public java.net.InetSocketAddress getRemoteAddress() {
            return new java.net.InetSocketAddress("127.0.0.1", 50000);
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getTextMessageSizeLimit() {
            return 0;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 0;
        }

        @Override
        public java.util.List<org.springframework.web.socket.WebSocketExtension> getExtensions() {
            return new java.util.ArrayList<>();
        }

        @Override
        public void sendMessage(org.springframework.web.socket.WebSocketMessage<?> message) {
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            this.closed = true;
            this.open = false;
        }

        @Override
        public void close(org.springframework.web.socket.CloseStatus status) {
            this.closed = true;
            this.closeCode = status.getCode();
            this.open = false;
        }
    }
}
