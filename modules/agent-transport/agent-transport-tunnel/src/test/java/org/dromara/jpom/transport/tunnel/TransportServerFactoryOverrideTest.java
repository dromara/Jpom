/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.tunnel;

import org.dromara.jpom.transport.DownloadCallback;
import org.dromara.jpom.transport.INodeInfo;
import org.dromara.jpom.transport.IProxyWebSocket;
import org.dromara.jpom.transport.IUrlItem;
import org.dromara.jpom.transport.TransportServer;
import org.dromara.jpom.transport.TransportServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

/**
 * {@link TransportServerFactory} 覆盖钩子回归测试。
 * <p>
 * 反向建连隧道依赖该钩子注册「按节点路由的传输门面」；SPI 单例无法按节点区分正反向。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TransportServerFactoryOverrideTest {

    @AfterEach
    public void clear() {
        TransportServerFactory.set(null);
    }

    @Test
    public void testDelegateOverride() {
        StubTransportServer stub = new StubTransportServer();
        TransportServerFactory.set(stub);
        Assertions.assertSame(stub, TransportServerFactory.get(), "注册后应返回注册的实现");
        Assertions.assertEquals("ok", TransportServerFactory.get().execute(null, null, null));
    }

    @Test
    public void testFallbackToSpiWithoutDelegate() {
        TransportServerFactory.set(null);
        // 该模块没有 SPI 实现（服务端才引入 transport 实现 jar），应明确报错而不是静默返回 null
        Assertions.assertThrows(RuntimeException.class, TransportServerFactory::get);
    }

    private static class StubTransportServer implements TransportServer {

        @Override
        public String execute(INodeInfo nodeInfo, IUrlItem urlItem, Object data) {
            return "ok";
        }

        @Override
        public void download(INodeInfo nodeInfo, IUrlItem urlItem, Object data, Consumer<DownloadCallback> consumer) {
            // 测试桩不实现下载
        }

        @Override
        public IProxyWebSocket websocket(INodeInfo nodeInfo, IUrlItem urlItem, Object... parameters) {
            throw new UnsupportedOperationException("stub");
        }
    }
}
