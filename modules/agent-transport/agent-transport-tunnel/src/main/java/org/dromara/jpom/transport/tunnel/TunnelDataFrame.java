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
 * 解析后的二进制数据帧。
 * <p>
 * 帧结构（大端无关，逐字节）：
 * <pre>
 * +--------+--------+----------+--------+------------------+
 * | 0x01   | sidLen | sid(utf8)| flags  | payload          |
 * | 1 byte | 1 byte | 1..24 B  | 1 byte | 余下全部（≤256KiB）|
 * +--------+--------+----------+--------+------------------+
 * flags: bit0 = 该 sid 方向结束（FIN）
 * </pre>
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelDataFrame {

    /**
     * 结束标志位
     */
    public static final int FLAG_FIN = 0x01;

    private final String sid;
    private final byte[] payload;
    private final int flags;

    public TunnelDataFrame(String sid, byte[] payload, int flags) {
        this.sid = sid;
        this.payload = payload;
        this.flags = flags;
    }

    public String getSid() {
        return sid;
    }

    public byte[] getPayload() {
        return payload;
    }

    public int getFlags() {
        return flags;
    }

    public int getPayloadLength() {
        return payload == null ? 0 : payload.length;
    }

    /**
     * 是否是该 sid 方向的最后一帧
     *
     * @return true 结束
     */
    public boolean isFin() {
        return (flags & FLAG_FIN) != 0;
    }

    @Override
    public String toString() {
        return "TunnelDataFrame{sid=" + sid + ", len=" + getPayloadLength() + ", fin=" + isFin() + "}";
    }
}
