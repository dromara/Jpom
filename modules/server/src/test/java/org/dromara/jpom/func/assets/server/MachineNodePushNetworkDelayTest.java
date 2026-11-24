/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.server;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 推送模式的网络延迟口径回归：推送节点没有请求往返，落库值必须是 0 而不是负数。
 * <p>
 * 插件端上报 {@code systemSleep}（采集固定休眠，{@code NET_STAT_SLEEP + CPU_STAT_SLEEP = 2000ms}），
 * {@code saveStatInfo} 会执行 {@code networkDelay - systemSleep}；如果推送时把 networkDelay 写成 0，
 * 节点详情会显示 -2000ms、监控曲线出现负值（开发环境实测现象）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class MachineNodePushNetworkDelayTest {

    /**
     * 与插件端 {@code OshiUtils.NET_STAT_SLEEP + OshiUtils.CPU_STAT_SLEEP} 一致
     */
    private static final int AGENT_SYSTEM_SLEEP = 2000;

    @Test
    public void pushNetworkDelayIsZeroAfterSystemSleepSubtracted() {
        JSONObject statInfo = new JSONObject();
        statInfo.put("systemSleep", AGENT_SYSTEM_SLEEP);
        statInfo.put("oshiError", "");

        MachineNodeServer.fillPushNetworkDelay(statInfo);

        int networkDelay = statInfo.getIntValue("networkDelay");
        Assertions.assertEquals(AGENT_SYSTEM_SLEEP, networkDelay, "推送时 networkDelay 必须等于采集固定休眠");
        // 与 saveStatInfo 的计算保持一致
        int saved = networkDelay - statInfo.getIntValue("systemSleep");
        Assertions.assertEquals(0, saved, "落库的网络延迟必须是 0，不能是负数");
    }

    @Test
    public void pushNetworkDelayDefaultsToZeroWhenAgentSendsNoSleep() {
        JSONObject statInfo = new JSONObject();
        MachineNodeServer.fillPushNetworkDelay(statInfo);
        Assertions.assertEquals(0, statInfo.getIntValue("networkDelay"));
        Assertions.assertEquals(0, statInfo.getIntValue("networkDelay") - statInfo.getIntValue("systemSleep"));
    }
}
