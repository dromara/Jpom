package org.dromara.jpom.common.event;

import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.transport.event.ClientStatusEvent;
import org.dromara.jpom.transport.netty.service.ChannelService;
import org.dromara.jpom.transport.properties.NettyProperties;
import org.dromara.jpom.transport.protocol.RegisterMessage;
import org.dromara.jpom.transport.protocol.extend.RegisterDevice;
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
            registerDevice.setHost(String.join(",", (List) systemInfo.getJSONArray("hostIpv4s")));
            registerDevice.setName(systemInfo.getString("hostName"));
            registerDevice.setPort(nettyProperties.getPort());
            registerDevice.setVersion("");
            channelService.writeAndFlush(new RegisterMessage(new HashMap<>(0), registerDevice));
        }
    }
}
