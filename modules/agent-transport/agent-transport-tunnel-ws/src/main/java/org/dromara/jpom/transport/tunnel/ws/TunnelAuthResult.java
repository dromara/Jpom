/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.tunnel.ws;

/**
 * 握手鉴权结果。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelAuthResult {

    private final boolean allowed;
    private final String machineId;
    private final int code;
    private final String msg;
    private final boolean autoRegistered;

    private TunnelAuthResult(boolean allowed, String machineId, int code, String msg, boolean autoRegistered) {
        this.allowed = allowed;
        this.machineId = machineId;
        this.code = code;
        this.msg = msg;
        this.autoRegistered = autoRegistered;
    }

    public static TunnelAuthResult ok(String machineId) {
        return new TunnelAuthResult(true, machineId, 0, null, false);
    }

    public static TunnelAuthResult okAutoRegistered(String machineId) {
        return new TunnelAuthResult(true, machineId, 0, null, true);
    }

    public static TunnelAuthResult deny(int code, String msg) {
        return new TunnelAuthResult(false, null, code, msg, false);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getMachineId() {
        return machineId;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public boolean isAutoRegistered() {
        return autoRegistered;
    }
}
