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
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.transport.tunnel.TunnelAuth;
import org.dromara.jpom.model.user.UserModel;
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
            // 老数据/自动注册时缺授权信息：用插件端上报的摘要补齐，避免节点一直「授权信息错误」
            this.fillAuthorize(machineNodeModel, hello);
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
        // 插件端在 HELLO 里上报的授权账号/摘要：落库后该节点无需人工补密码即可被服务端调用
        model.setJpomUsername(hello.getAuthUser());
        model.setAuthorizeToken(hello.getAuthToken());
        machineNodeServer.insert(model);
        log.info("反向隧道自动注册节点 {}（ip={}, agent={}, os={}/{}, 授权信息={}）", hello.getMachineId(), remoteAddress,
            hello.getAgentVersion(), hello.getOs(), hello.getArch(), StrUtil.isNotEmpty(hello.getAuthToken()) ? "已自动绑定" : "缺失，需手工补充");
        this.assignWorkspace(model);
    }

    /**
     * 已有机器资产但缺授权信息时，用握手上报的摘要补齐（不覆盖人工配置）
     *
     * @param machineNodeModel 机器资产
     * @param hello            握手帧
     */
    private void fillAuthorize(MachineNodeModel machineNodeModel, TunnelFrame hello) {
        if (StrUtil.isEmpty(hello.getAuthToken())) {
            return;
        }
        boolean needToken = StrUtil.isEmpty(machineNodeModel.getAuthorizeToken());
        boolean needUser = StrUtil.isEmpty(machineNodeModel.getJpomUsername()) && StrUtil.isNotEmpty(hello.getAuthUser());
        if (!needToken && !needUser) {
            return;
        }
        MachineNodeModel update = new MachineNodeModel();
        update.setId(machineNodeModel.getId());
        if (needUser) {
            update.setJpomUsername(hello.getAuthUser());
        }
        if (needToken) {
            update.setAuthorizeToken(hello.getAuthToken());
        }
        machineNodeServer.updateById(update);
        log.info("反向隧道节点 {} 已用插件端上报的授权信息自动补齐", machineNodeModel.getId());
    }

    /**
     * 按配置把自动注册的机器分配到工作空间（未配置则跳过，保持「只建档」行为）
     *
     * @param machineNodeModel 机器资产
     */
    private void assignWorkspace(MachineNodeModel machineNodeModel) {
        String workspaceId = tunnelConfig.getAutoRegisterWorkspace();
        if (StrUtil.isEmpty(workspaceId)) {
            return;
        }
        try {
            // 握手线程没有登录用户上下文，插入节点需要系统用户（否则报「没有对应的用户」）
            BaseServerController.resetInfo(UserModel.EMPTY);
            machineNodeServer.insertNode(machineNodeModel, workspaceId);
            log.info("反向隧道自动注册节点 {} 已分配到工作空间 {}", machineNodeModel.getId(), workspaceId);
        } catch (Exception e) {
            // 分配失败不影响握手（机器资产已建，人工可再分配）
            log.warn("反向隧道自动注册节点 {} 分配到工作空间 {} 失败：{}", machineNodeModel.getId(), workspaceId, e.getMessage());
        } finally {
            BaseServerController.removeEmpty();
        }
    }
}