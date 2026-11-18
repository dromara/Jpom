/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets;

import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 节点状态时效折算、节点检测范围的行为级回归
 * <p>
 * 背景（2026-11-18 群反馈）：集群记录存在多条历史残留时会被判定为多服务，
 * 若当前服务的资产分组为空，节点监控曾整轮跳过，机器状态会永久停在"正常"——
 * 插件端停机、服务器关机都看不出来。
 *
 * @author bwcx_jzy
 */
public class MachineNodeStatusTimeoutTest {

    private static final long HEART_SECOND_MILLIS = 30 * 1000L;

    private static final long TIMEOUT_MILLIS = HEART_SECOND_MILLIS * 3;

    /**
     * 刚检测过的机器不能折算成检测超时
     */
    @Test
    public void freshStatusKept() {
        long now = System.currentTimeMillis();
        boolean timeout = MachineNodeServer.checkStatusTimeout(1, now - 1000, TIMEOUT_MILLIS, now);
        Assertions.assertFalse(timeout, "刚刷新过的正常状态不应被折算");
    }

    /**
     * 超过 3 个心跳周期没有刷新，必须折算成检测超时（正常 -> 检测超时）
     */
    @Test
    public void staleStatusBecomesTimeout() {
        long now = System.currentTimeMillis();
        boolean timeout = MachineNodeServer.checkStatusTimeout(1, now - TIMEOUT_MILLIS - 1, TIMEOUT_MILLIS, now);
        Assertions.assertTrue(timeout, "长时间未刷新的正常状态必须折算为检测超时");
    }

    /**
     * 边界：刚好等于超时时间不算超时（必须严格大于）
     */
    @Test
    public void timeoutBoundary() {
        long now = System.currentTimeMillis();
        Assertions.assertFalse(MachineNodeServer.checkStatusTimeout(1, now - TIMEOUT_MILLIS, TIMEOUT_MILLIS, now));
        Assertions.assertTrue(MachineNodeServer.checkStatusTimeout(1, now - TIMEOUT_MILLIS - 1, TIMEOUT_MILLIS, now));
    }

    /**
     * 其余状态（无法连接/授权错误/状态码错误/资源监控异常/检测超时）已经是异常态，不做二次折算
     */
    @Test
    public void otherStatusNotChanged() {
        long now = System.currentTimeMillis();
        long stale = now - TIMEOUT_MILLIS - 1;
        for (int status : new int[]{0, 2, 3, 4, MachineNodeServer.STATUS_CHECK_TIMEOUT}) {
            Assertions.assertFalse(MachineNodeServer.checkStatusTimeout(status, stale, TIMEOUT_MILLIS, now),
                "状态 " + status + " 不应被折算");
        }
        Assertions.assertFalse(MachineNodeServer.checkStatusTimeout(null, stale, TIMEOUT_MILLIS, now), "空状态不折算");
    }

    /**
     * 没有最后检测时间的机器不折算（避免新建数据被误判）
     */
    @Test
    public void missingModifyTime() {
        long now = System.currentTimeMillis();
        Assertions.assertFalse(MachineNodeServer.checkStatusTimeout(1, null, TIMEOUT_MILLIS, now));
        Assertions.assertFalse(MachineNodeServer.checkStatusTimeout(1, 0L, TIMEOUT_MILLIS, now));
    }

    /**
     * transportMode 为空（历史数据、手工迁库）也必须参与检测，否则该机器永远不会被检测
     */
    @Test
    public void pollTransportMode() {
        Assertions.assertTrue(MachineNodeServer.isPollTransportMode(null), "transportMode 为空的机器必须参与检测");
        Assertions.assertTrue(MachineNodeServer.isPollTransportMode(0), "transportMode=0 的机器参与检测");
        Assertions.assertFalse(MachineNodeServer.isPollTransportMode(1), "推送模式（1）不走服务端拉取");
    }

    /**
     * 检测超时状态值不能与既有状态冲突（0 无法连接、1 正常、2 授权错误、3 状态码错误、4 资源监控异常）
     */
    @Test
    public void statusValue() {
        Assertions.assertEquals(5, MachineNodeServer.STATUS_CHECK_TIMEOUT);
    }
}
