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
package org.dromara.jpom.common.event;

import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.transport.event.ClientStatusEvent;
import org.dromara.jpom.transport.netty.service.ChannelService;
import org.dromara.jpom.transport.properties.NettyProperties;
import org.dromara.jpom.transport.protocol.RegisterMessage;
import org.dromara.jpom.transport.protocol.extend.RegisterDeviceImpl;
import org.dromara.jpom.util.OshiUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class ClientStatusHandler implements ApplicationListener<ClientStatusEvent> {

    private final ChannelService channelService;
    private final NettyProperties nettyProperties;

    public ClientStatusHandler(ChannelService channelService, NettyProperties nettyProperties) {
        this.channelService = channelService;
        this.nettyProperties = nettyProperties;
    }

    @Override
    public void onApplicationEvent(ClientStatusEvent event) {
        if (event.getStatus() == ClientStatusEvent.Status.CONNECT_SUCCESS) {
            RegisterDeviceImpl registerDevice = new RegisterDeviceImpl();
            JSONObject systemInfo = OshiUtils.getSystemInfo();
            JpomManifest instance = JpomManifest.getInstance();
            registerDevice.setHost(String.join(",", (List) systemInfo.getJSONArray("hostIpv4s")));
            registerDevice.setName(instance.getInstallId());
            registerDevice.setPort(instance.getPort());
            registerDevice.setVersion(instance.getVersion());
            channelService.writeAndFlush(new RegisterMessage(new HashMap<>(0), registerDevice));
        }
    }
}
