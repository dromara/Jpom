/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.openapi.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 节点管理
 *
 * @author bwcx_jzy
 * @since 2019/8/5
 */
@RestController
@Slf4j
public class NodeInfoController extends BaseServerController {

    private static final Map<String, JSONObject> CACHE_RECEIVE_PUSH = new HashMap<>();

    private final NodeService nodeService;
    private final WorkspaceService workspaceService;

    public NodeInfoController(NodeService nodeService,
                              WorkspaceService workspaceService) {
        this.nodeService = nodeService;
        this.workspaceService = workspaceService;
    }

    /**
     * 接收节点推送的信息
     * <p>
     * yum install -y wget && wget -O install.sh https://jpom.top/docs/install.sh && bash install.sh Agent jdk
     * --auto-push-to-server http://127.0.0.1:3000/api/node/receive_push?token=462a47b8fba8da1f824370bb9fcdc01aa1a0fe20&workspaceId=DEFAULT
     *
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.RECEIVE_PUSH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public IJsonMessage<JSONObject> receivePush(@ValidatorItem(msg = "i18n.credential_cannot_be_empty.d055") String token,
                                                @ValidatorItem(msg = "i18n.communication_ip_cannot_be_empty.ae35") String ips,
                                                @ValidatorItem(msg = "i18n.login_name_cannot_be_empty.9a99") String loginName,
                                                @ValidatorItem(msg = "i18n.password_cannot_be_empty.89b5") String loginPwd,
                                                @ValidatorItem(msg = "i18n.workspace_id_required.c967") String workspaceId,
                                                @ValidatorItem(value = ValidatorRule.NUMBERS, msg = "i18n.port_error.312e") int port,
                                                String ping) {
        Assert.state(StrUtil.equals(token, JpomManifest.getInstance().randomIdSign()), "token error");
        boolean exists = workspaceService.exists(new WorkspaceModel(workspaceId));
        Assert.state(exists, "workspaceId error");
        String sha1Id = SecureUtil.sha1(ips);
        //
        List<String> ipsList = StrUtil.split(ips, StrUtil.COMMA);
        String clientIp = getClientIP();
        if (!ipsList.contains(clientIp)) {
            ipsList.add(clientIp);
        }
        List<String> canUseIps = ipsList.stream()
            .filter(s -> this.testIpPort(s, ping, port))
            .collect(Collectors.toList());
        List<MachineNodeModel> canUseNode = canUseIps.stream().map(s -> {
            MachineNodeModel model = this.createMachineNodeModel(s, loginName, loginPwd, port);
            try {
                machineNodeServer.testNode(model);
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.test_result.8441"), model.getJpomUrl(), e.getMessage());
                return null;
            }
            return model;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        // 只返回能通的 IP
        canUseIps = canUseNode.stream().map(MachineNodeModel::getName).collect(Collectors.toList());
        // 标记为系统操作
        BaseServerController.resetInfo(UserModel.EMPTY);
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("allIp", ipsList);
        jsonObject.put("canUseIp", canUseIps);
        jsonObject.put("port", port);
        jsonObject.put("id", sha1Id);
        jsonObject.put("canUseNode", canUseNode);
        //
        for (MachineNodeModel nodeModel : canUseNode) {
            MachineNodeModel existsMachine = machineNodeServer.getByUrl(nodeModel.getJpomUrl());
            if (existsMachine != null) {
                if (nodeService.existsNode2(workspaceId, existsMachine.getId())) {
                    // 存在
                    jsonObject.put("type", "exists");
                } else {
                    // 自动同步
                    jsonObject.put("type", "success");
                    machineNodeServer.insertNode(existsMachine, workspaceId);
                }
                break;
            }
        }
        if (!jsonObject.containsKey("type")) {
            int size1 = CollUtil.size(canUseNode);
            if (size1 == 1) {
                // 只有一个 ip 可以使用,添加插件端
                BaseServerController.resetInfo(UserModel.EMPTY);
                MachineNodeModel first = CollUtil.getFirst(canUseNode);
                machineNodeServer.insertAndNode(first, workspaceId);
                jsonObject.put("type", "success");
            } else {
                jsonObject.put("type", size1 == 0 ? "canUseIpEmpty" : "multiIp");
            }
        }
        CACHE_RECEIVE_PUSH.put(sha1Id, jsonObject);
        return JsonMessage.success("done", jsonObject);
    }

    /**
     * 查询所有缓存
     *
     * @return list
     */
    public static Collection<JSONObject> listReceiveCache(String removeId) {
        if (StrUtil.isNotEmpty(removeId)) {
            CACHE_RECEIVE_PUSH.remove(removeId);
        }
        return CACHE_RECEIVE_PUSH.values();
    }

    public static JSONObject getReceiveCache(String id) {
        return CACHE_RECEIVE_PUSH.get(id);
    }

    /**
     * 尝试 ping
     *
     * @param ip   ip 地址
     * @param ping ping 时间
     * @return true
     */
    private boolean testIpPort(String ip, String ping, int port) {
        int pingTime = Convert.toInt(ping, 5);
        if (pingTime <= 0) {
            return true;
        }
        boolean pinged = NetUtil.ping(ip, pingTime * 1000);
        //
        return pinged || this.testIpCanPort(ip, pingTime, port);
    }

    private boolean testIpCanPort(String ip, int timeout, int port) {
        InetSocketAddress address = NetUtil.createAddress(ip, port);
        return NetUtil.isOpen(address, (int) TimeUnit.SECONDS.toMillis(timeout));
    }

    private MachineNodeModel createMachineNodeModel(String ip, String loginName, String loginPwd, int port) {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setName(ip);
        machineNodeModel.setStatus(1);
        machineNodeModel.setJpomUsername(loginName);
        machineNodeModel.setJpomPassword(loginPwd);
        machineNodeModel.setJpomUrl(ip + StrUtil.COLON + port);
        machineNodeModel.setJpomProtocol("http");
        return machineNodeModel;
    }
}
