/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.tunnel;

/**
 * 隧道控制帧类型。
 *
 * @author jpom
 * @since 2026/11/18
 */
public enum TunnelFrameType {

    /**
     * 插件端 -> 服务端：首帧握手
     */
    HELLO,
    /**
     * 服务端 -> 插件端：握手通过
     */
    HELLO_ACK,
    /**
     * 服务端 -> 插件端：握手拒绝
     */
    REJECT,
    /**
     * 心跳
     */
    PING,
    /**
     * 心跳响应
     */
    PONG,
    /**
     * 服务端 -> 插件端：开始一个逻辑请求（对应 TransportServer.execute）
     */
    REQ,
    /**
     * 服务端 -> 插件端：请求体发送完毕
     */
    REQ_BODY_END,
    /**
     * 插件端 -> 服务端：响应头（body 随后以数据帧发送）
     */
    RES,
    /**
     * 双向：该 sid 方向结束
     */
    END,
    /**
     * 双向：中止该 sid（超时/异常/对端关闭）
     */
    ABORT,
    /**
     * 服务端 -> 插件端：开一条双向流（对应 TransportServer.websocket）
     */
    OPEN,
    /**
     * 插件端 -> 服务端：双向流打开结果
     */
    OPEN_ACK,
    /**
     * 双向：关闭流
     */
    CLOSE,
    /**
     * 双向：流控确认（已消费字节数）
     */
    ACK;

    /**
     * 按名称解析，未知返回 null
     *
     * @param type 帧类型名
     * @return 帧类型
     */
    public static TunnelFrameType of(String type) {
        if (type == null) {
            return null;
        }
        for (TunnelFrameType value : values()) {
            if (value.name().equals(type)) {
                return value;
            }
        }
        return null;
    }
}
