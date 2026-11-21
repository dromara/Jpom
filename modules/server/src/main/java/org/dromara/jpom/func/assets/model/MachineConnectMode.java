/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.model;

import org.dromara.jpom.model.BaseEnum;

/**
 * 机器与插件端之间的通讯方式。
 * <p>
 * 决定「节点地址 / 账号 / 密码 / 协议 / 代理 / 超时」这些正向连接配置是否参与通信：
 * 反向隧道由插件端主动连服务端，这些配置不参与；正向拉取则需要它们。
 * 默认「自动」＝保持历史行为（隧道在线走隧道，离线回退正向），老数据不受影响。
 *
 * @author jpom
 * @since 2.11.15
 */
public enum MachineConnectMode implements BaseEnum {

    /**
     * 自动：反向隧道在线时走隧道，离线回退正向拉取（默认，历史行为）
     */
    Auto(0, "自动（优先反向隧道，离线回退正向）"),
    /**
     * 仅反向隧道：只走插件端主动建立的长连接，不回退正向
     */
    TunnelOnly(1, "仅反向隧道"),
    /**
     * 仅正向拉取：始终由服务端连接插件端（忽略在线隧道）
     */
    ForwardOnly(2, "仅正向拉取"),
    ;

    private final int code;
    private final String desc;

    MachineConnectMode(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    /**
     * 规范化通讯方式：空或未知值按「自动」处理（兼容老数据）
     *
     * @param code 通讯方式
     * @return 通讯方式编码
     */
    public static int normalize(Integer code) {
        if (code == null) {
            return Auto.code;
        }
        return code == Auto.code || code == TunnelOnly.code || code == ForwardOnly.code ? code : Auto.code;
    }

    /**
     * 是否仅反向隧道
     *
     * @param code 通讯方式
     * @return true 仅反向隧道
     */
    public static boolean isTunnelOnly(Integer code) {
        return normalize(code) == TunnelOnly.code;
    }

    /**
     * 是否仅正向拉取
     *
     * @param code 通讯方式
     * @return true 仅正向拉取
     */
    public static boolean isForwardOnly(Integer code) {
        return normalize(code) == ForwardOnly.code;
    }
}
