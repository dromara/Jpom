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

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.func.assets.model.MachineConnectMode;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.transport.DownloadCallback;
import org.dromara.jpom.transport.INodeInfo;
import org.dromara.jpom.transport.IProxyWebSocket;
import org.dromara.jpom.transport.IUrlItem;
import org.dromara.jpom.transport.TransportAgentException;
import org.dromara.jpom.transport.TransportServer;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannel;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannelRegistry;
import org.dromara.jpom.transport.tunnel.ws.TunnelDownload;
import org.dromara.jpom.transport.tunnel.ws.TunnelProxyWebSocket;
import org.dromara.jpom.transport.tunnel.ws.TunnelResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 传输路由器：节点有在线反向隧道就走隧道，否则回退到正向（HTTP 拉取）实现。
 * <p>
 * 正向行为零变化：只在 {@link INodeInfo#id()} 命中在线通道时才走隧道。
 *
 * @author jpom
 * @since 2026/11/18
 */
@Slf4j
public class RoutingTransportServer implements TransportServer {

    private final TunnelChannelRegistry registry;
    private final TransportServer forward;

    public RoutingTransportServer(TunnelChannelRegistry registry, TransportServer forward) {
        this.registry = registry;
        this.forward = forward;
    }

    @Override
    public String execute(INodeInfo nodeInfo, IUrlItem urlItem, Object data) {
        TunnelChannel channel = this.channel(nodeInfo);
        if (channel == null) {
            return forward.execute(nodeInfo, urlItem, data);
        }
        TunnelRequestBody requestBody = TunnelRequestBody.build(nodeInfo, urlItem, data);
        int timeout = timeout(urlItem);
        if (log.isDebugEnabled()) {
            log.debug("反向隧道请求 {}[{}] {} {}", nodeInfo.name(), urlItem.workspaceId(), requestBody.getContentType(), urlItem.path());
        }
        TunnelResponse response = channel.request(urlItem, requestBody.getBody(), requestBody.getContentType(), "POST", timeout,
            this.extraHeaders(nodeInfo, urlItem));
        int status = response.getStatus();
        if (status != 200) {
            log.warn("反向隧道响应错误 {} status={} body={}", nodeInfo.name(), status, response.bodyString());
            throw new TransportAgentException(nodeInfo.name() + " 节点通过反向隧道响应错误 status=" + status);
        }
        return response.bodyString();
    }

    @Override
    public void download(INodeInfo nodeInfo, IUrlItem urlItem, Object data, Consumer<DownloadCallback> consumer) {
        TunnelChannel channel = this.channel(nodeInfo);
        if (channel == null) {
            forward.download(nodeInfo, urlItem, data, consumer);
            return;
        }
        // 下载类接口在插件端是 @GetMapping，GET 不解析请求体：
        // 必须把参数放到 query 上，否则插件端收到空参数（「请选择文件」/「备份id缺失」，BUGS B-007 / B-018）
        Object bodyData = data;
        IUrlItem targetItem = urlItem;
        String query = TunnelRequestBody.buildQuery(nodeInfo, data);
        if (StrUtil.isNotEmpty(query)) {
            targetItem = new TunnelQueryUrlItem(urlItem, query);
            bodyData = null;
        }
        TunnelRequestBody requestBody = TunnelRequestBody.build(nodeInfo, targetItem, bodyData);
        TunnelDownload download = channel.download(targetItem, requestBody.getBody(), requestBody.getContentType(), timeout(targetItem),
            this.extraHeaders(nodeInfo, targetItem));
        DownloadCallback callback = DownloadCallback.builder()
            .contentDisposition(download.getContentDisposition())
            .contentType(download.getContentType())
            .status(download.getStatus())
            .contentLength(download.getContentLength())
            .inputStream(download.getInputStream())
            .build();
        consumer.accept(callback);
    }

    @Override
    public IProxyWebSocket websocket(INodeInfo nodeInfo, IUrlItem urlItem, Object... parameters) {
        TunnelChannel channel = this.channel(nodeInfo);
        if (channel == null) {
            return forward.websocket(nodeInfo, urlItem, parameters);
        }
        // 反向模式下由服务端把节点授权信息随流打开参数下发，插件端无需读取自身授权摘要
        return TunnelProxyWebSocket.of(channel, urlItem, timeout(urlItem), this.extraHeaders(nodeInfo, urlItem), parameters);
    }

    /**
     * 附加请求头：节点授权信息与工作空间（与正向传输保持一致，插件端本机回环时直接沿用）
     *
     * @param nodeInfo 节点信息
     * @param urlItem  请求信息
     * @return 请求头
     */
    private Map<String, String> extraHeaders(INodeInfo nodeInfo, IUrlItem urlItem) {
        Map<String, String> headers = new HashMap<>();
        if (nodeInfo != null && nodeInfo.authorize() != null) {
            headers.put(org.dromara.jpom.transport.tunnel.TunnelConst.HEADER_AUTHORIZE, nodeInfo.authorize());
        }
        if (urlItem != null && urlItem.workspaceId() != null) {
            headers.put(org.dromara.jpom.transport.tunnel.TunnelConst.HEADER_WORKSPACE_ID, urlItem.workspaceId());
        }
        return headers;
    }

    /**
     * 解析本次请求的通道：
     * <ul>
     *     <li>仅正向拉取：始终返回 null（忽略在线隧道，走正向实现）</li>
     *     <li>仅反向隧道：必须命中在线通道，否则直接报错，不静默回退正向</li>
     *     <li>自动（默认）：命中走隧道，未命中回退正向</li>
     * </ul>
     *
     * @param nodeInfo 节点信息
     * @return 隧道通道，null 表示走正向
     */
    private TunnelChannel channel(INodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return null;
        }
        int connectMode = this.connectMode(nodeInfo);
        if (MachineConnectMode.isForwardOnly(connectMode)) {
            return null;
        }
        TunnelChannel channel = registry.get(nodeInfo.id());
        if (channel == null && MachineConnectMode.isTunnelOnly(connectMode)) {
            throw new TransportAgentException(nodeInfo.name() + " 未通过反向隧道连接（当前通讯方式为「仅反向隧道」，不会回退正向拉取）");
        }
        return channel;
    }

    /**
     * 读取机器资产的通讯方式；节点级（NodeModel）或老数据按「自动」处理
     *
     * @param nodeInfo 节点信息
     * @return 通讯方式编码
     */
    private int connectMode(INodeInfo nodeInfo) {
        if (nodeInfo instanceof MachineNodeModel) {
            return MachineConnectMode.normalize(((MachineNodeModel) nodeInfo).getConnectMode());
        }
        return MachineConnectMode.normalize(null);
    }

    private static int timeout(IUrlItem urlItem) {
        Integer timeout = urlItem == null ? null : urlItem.timeout();
        return timeout == null || timeout <= 0 ? 30 : timeout;
    }
}
