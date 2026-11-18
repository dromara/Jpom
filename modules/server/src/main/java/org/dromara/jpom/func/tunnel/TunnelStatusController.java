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
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannelRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 反向隧道在线状态查询（供节点/资产页展示"反向在线"）。
 *
 * @author jpom
 * @since 2026/11/18
 */
@RestController
@RequestMapping("/tunnel")
public class TunnelStatusController extends BaseServerController {

    private final TunnelChannelRegistry tunnelChannelRegistry;
    private final TunnelPushService tunnelPushService;

    public TunnelStatusController(TunnelChannelRegistry tunnelChannelRegistry, TunnelPushService tunnelPushService) {
        this.tunnelChannelRegistry = tunnelChannelRegistry;
        this.tunnelPushService = tunnelPushService;
    }

    /**
     * 在线机器节点
     *
     * @return 机器节点标识集合
     */
    @GetMapping("/online")
    @Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE, method = MethodFeature.LIST)
    public IJsonMessage<List<String>> online() {
        return JsonMessage.success("", new ArrayList<>(this.tunnelChannelRegistry.onlineMachineIds()));
    }

    /**
     * 指定机器节点是否通过反向隧道在线
     *
     * @param machineId 机器节点标识
     * @return true 在线
     */
    @GetMapping("/status")
    @Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE, method = MethodFeature.LIST)
    public IJsonMessage<Boolean> status(@RequestParam String machineId) {
        return JsonMessage.success("", this.tunnelChannelRegistry.isOnline(machineId));
    }

    /**
     * 插件端主动推送的接收情况（排障用）
     *
     * @param machineId 可选：指定机器节点时返回其最新推送时间与新鲜度
     * @return 统计信息
     */
    @GetMapping("/push")
    @Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE, method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> push(@RequestParam(required = false) String machineId) {
        JSONObject data = new JSONObject();
        data.put("received", this.tunnelPushService.receivedCount());
        data.put("nodes", this.tunnelPushService.snapshotSize());
        data.put("maxAgeSeconds", this.tunnelPushService.maxAgeSeconds());
        if (StrUtil.isNotEmpty(machineId)) {
            TunnelPushService.Snapshot snapshot = this.tunnelPushService.latest(machineId);
            data.put("machineId", machineId);
            data.put("receivedAt", snapshot == null ? null : snapshot.getReceivedAt());
            data.put("seq", snapshot == null ? null : snapshot.getSeq());
            data.put("fresh", this.tunnelPushService.fresh(machineId) != null);
        }
        return JsonMessage.success("", data);
    }
}
