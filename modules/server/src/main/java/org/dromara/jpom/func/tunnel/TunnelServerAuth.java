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
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.transport.tunnel.TunnelAuth;
import org.dromara.jpom.transport.tunnel.TunnelConst;
import org.dromara.jpom.transport.tunnel.TunnelFrame;
import org.dromara.jpom.transport.tunnel.ws.TunnelAuthResult;
import org.dromara.jpom.transport.tunnel.ws.TunnelAuthenticator;
import org.springframework.stereotype.Component;

/**
 * 隧道握手鉴权：校验全局连接密钥与 HMAC 签名，未注册节点默认拒绝，可配置自动注册。
 *
 * @author jpom
 * @since 2026/11/18
 */
@Slf4j
@Component
public class TunnelServerAuth implements TunnelAuthenticator {

    private final TunnelConfig tunnelConfig;
    private final MachineNodeServer machineNodeServer;

    public TunnelServerAuth(TunnelConfig tunnelConfig, MachineNodeServer machineNodeServer) {
        this.tunnelConfig = tunnelConfig;
        this.machineNodeServer = machineNodeServer;
    }

    @Override
    public TunnelAuthResult authenticate(TunnelFrame hello, String remoteAddress) {
        if (!tunnelConfig.enabled()) {
            return TunnelAuthResult.deny(4001, "服务端未配置反向隧道连接密钥");
        }
        String machineId = hello.getMachineId();
        if (StrUtil.isEmpty(machineId)) {
            return TunnelAuthResult.deny(4001, "缺少机器节点标识");
        }
        if (!StrUtil.equals(hello.getKey(), tunnelConfig.getKey())) {
            log.warn("反向隧道握手失败：连接密钥不正确 machineId={} ip={}", machineId, remoteAddress);
            return TunnelAuthResult.deny(4001, "连接密钥不正确");
        }
        long nowSeconds = System.currentTimeMillis() / 1000;
        boolean verified = TunnelAuth.verify(tunnelConfig.getKey(), machineId, hello.getTs(), hello.getNonce(), hello.getSign(),
            nowSeconds, TunnelConst.DEFAULT_AUTH_SKEW_SECONDS);
        if (!verified) {
            log.warn("反向隧道握手失败：签名校验未通过（可能是时间偏差或重放）machineId={} ip={}", machineId, remoteAddress);
            return TunnelAuthResult.deny(4001, "握手签名校验未通过，请检查服务端与插件端时间是否一致");
        }
        MachineNodeModel machineNodeModel = machineNodeServer.getByKey(machineId);
        if (machineNodeModel != null) {
            return TunnelAuthResult.ok(machineId);
        }
        if (!tunnelConfig.isAllowAutoRegister()) {
            log.warn("反向隧道握手被拒：节点未注册 machineId={} ip={}", machineId, remoteAddress);
            return TunnelAuthResult.deny(4002, "节点未注册，请先在机器节点资产中创建该节点或开启自动注册");
        }
        this.autoRegister(hello, remoteAddress);
        return TunnelAuthResult.okAutoRegistered(machineId);
    }

    private void autoRegister(TunnelFrame hello, String remoteAddress) {
        MachineNodeModel model = new MachineNodeModel();
        model.setId(hello.getMachineId());
        model.setName(StrUtil.emptyToDefault(hello.getMachineId(), "反向节点"));
        model.setStatus(1);
        model.setJpomProtocol("http");
        model.setJpomUrl(remoteAddress);
        model.setJpomVersion(hello.getAgentVersion());
        model.setHostName(hello.getMachineId());
        model.setHostIpv4s(remoteAddress);
        model.setOsName(hello.getOs());
        machineNodeServer.insert(model);
        log.info("反向隧道自动注册节点 {}（ip={}, agent={}, os={}/{})", hello.getMachineId(), remoteAddress,
            hello.getAgentVersion(), hello.getOs(), hello.getArch());
    }
}
