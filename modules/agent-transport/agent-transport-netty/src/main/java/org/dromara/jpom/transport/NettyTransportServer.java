/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dromara.jpom.transport;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.transport.netty.service.ChannelServiceManager;
import org.dromara.jpom.transport.protocol.DiskInfoMessage;
import org.dromara.jpom.transport.protocol.HwDiskInfoMessage;
import org.dromara.jpom.transport.protocol.NetworkInterfacesMessage;
import org.dromara.jpom.transport.protocol.ProcessListMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Socket消息传输
 *
 * @author Hong
 * @since 2023/09/21
 */
@Slf4j
public class NettyTransportServer implements TransportServer {


    @Override
    public String execute(INodeInfo nodeInfo, IUrlItem urlItem, Object data) {
        try {
            if (urlItem.messageClass() == ProcessListMessage.class) {
                CompletableFuture<String> future = new CompletableFuture<>();
                ChannelServiceManager.INSTANCE.writeAndFlush(new ProcessListMessage(new HashMap<>(), JSON.toJSONString(data)), message -> future.complete(((ProcessListMessage) message).text()), ((Map<String, String>) data).getOrDefault("installId", ""));
                return future.get(10, TimeUnit.SECONDS);
            } else if (urlItem.messageClass() == DiskInfoMessage.class) {
                CompletableFuture<String> future = new CompletableFuture<>();
                ChannelServiceManager.INSTANCE.writeAndFlush(new DiskInfoMessage(new HashMap<>(), JSON.toJSONString(data)), message -> future.complete(((DiskInfoMessage) message).text()), ((Map<String, String>) data).getOrDefault("installId", ""));
                return future.get(10, TimeUnit.SECONDS);
            } else if (urlItem.messageClass() == HwDiskInfoMessage.class) {
                CompletableFuture<String> future = new CompletableFuture<>();
                ChannelServiceManager.INSTANCE.writeAndFlush(new HwDiskInfoMessage(new HashMap<>(), JSON.toJSONString(data)), message -> future.complete(((HwDiskInfoMessage) message).text()), ((Map<String, String>) data).getOrDefault("installId", ""));
                return future.get(10, TimeUnit.SECONDS);
            } else if (urlItem.messageClass() == NetworkInterfacesMessage.class) {
                CompletableFuture<String> future = new CompletableFuture<>();
                ChannelServiceManager.INSTANCE.writeAndFlush(new NetworkInterfacesMessage(new HashMap<>(), JSON.toJSONString(data)), message -> future.complete(((NetworkInterfacesMessage) message).text()), ((Map<String, String>) data).getOrDefault("installId", ""));
                return future.get(10, TimeUnit.SECONDS);
            }
            return null;
        } catch (Exception e) {
            log.error("{}", e.toString());
            return null;
        }
    }

    @Override
    public void download(INodeInfo nodeInfo, IUrlItem urlItem, Object data, Consumer<DownloadCallback> consumer) {
        throw new RuntimeException("not support download.");
    }

    @Override
    public IProxyWebSocket websocket(INodeInfo nodeInfo, IUrlItem urlItem, Object... parameters) {
        throw new RuntimeException("not support websocket.");
    }

    @Override
    public boolean support(Integer transportMode) {
        return transportMode.equals(1);
    }
}
