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

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 隧道帧编解码：文本控制帧（JSON）与二进制数据帧（定长帧头 + payload）。
 * <p>
 * 服务端与插件端必须使用本类完成编解码，避免两端各写一份协议实现后漂移。
 *
 * @author jpom
 * @since 2026/11/18
 */
public final class TunnelFrameCodec {

    private TunnelFrameCodec() {
    }

    /**
     * 编码控制帧
     *
     * @param frame 控制帧
     * @return JSON 文本
     */
    public static String encodeControl(TunnelFrame frame) {
        if (frame == null || frame.frameType() == null) {
            throw new TunnelProtocolException("控制帧类型不能为空");
        }
        return JSON.toJSONString(frame);
    }

    /**
     * 解析控制帧
     *
     * @param text JSON 文本
     * @return 控制帧
     */
    public static TunnelFrame decodeControl(String text) {
        if (StrUtil.isEmpty(text)) {
            throw new TunnelProtocolException("控制帧内容为空");
        }
        TunnelFrame frame;
        try {
            frame = JSON.parseObject(text, TunnelFrame.class);
        } catch (Exception e) {
            throw new TunnelProtocolException("控制帧解析失败: " + StrUtil.maxLength(text, 200), e);
        }
        if (frame == null || frame.frameType() == null) {
            throw new TunnelProtocolException("未知的控制帧类型: " + StrUtil.maxLength(text, 200));
        }
        return frame;
    }

    /**
     * 编码二进制数据帧
     *
     * @param sid     逻辑请求 / 流标识
     * @param payload 数据（可为空）
     * @param fin     是否该 sid 方向结束
     * @return 帧字节
     */
    public static byte[] encodeData(String sid, byte[] payload, boolean fin) {
        byte[] sidBytes = sidBytes(sid);
        int payloadLength = payload == null ? 0 : payload.length;
        if (payloadLength > TunnelConst.MAX_DATA_FRAME_SIZE) {
            throw new TunnelProtocolException("数据帧超过上限 " + TunnelConst.MAX_DATA_FRAME_SIZE + " 字节: " + payloadLength);
        }
        byte[] frame = new byte[3 + sidBytes.length + payloadLength];
        frame[0] = (byte) TunnelConst.DATA_FRAME_MAGIC;
        frame[1] = (byte) sidBytes.length;
        System.arraycopy(sidBytes, 0, frame, 2, sidBytes.length);
        frame[2 + sidBytes.length] = (byte) (fin ? TunnelDataFrame.FLAG_FIN : 0);
        if (payloadLength > 0) {
            System.arraycopy(payload, 0, frame, 3 + sidBytes.length, payloadLength);
        }
        return frame;
    }

    /**
     * 解析二进制数据帧
     *
     * @param raw 帧字节
     * @return 数据帧
     */
    public static TunnelDataFrame decodeData(byte[] raw) {
        if (raw == null || raw.length < 3) {
            throw new TunnelProtocolException("数据帧长度不足: " + (raw == null ? 0 : raw.length));
        }
        if ((raw[0] & 0xFF) != TunnelConst.DATA_FRAME_MAGIC) {
            throw new TunnelProtocolException("数据帧魔数不匹配: " + (raw[0] & 0xFF));
        }
        int sidLength = raw[1] & 0xFF;
        if (sidLength <= 0 || sidLength > TunnelConst.MAX_SID_LENGTH) {
            throw new TunnelProtocolException("数据帧 sid 长度非法: " + sidLength);
        }
        if (raw.length < 3 + sidLength) {
            throw new TunnelProtocolException("数据帧长度不足: " + raw.length);
        }
        String sid = new String(raw, 2, sidLength, StandardCharsets.UTF_8);
        int flags = raw[2 + sidLength] & 0xFF;
        byte[] payload = Arrays.copyOfRange(raw, 3 + sidLength, raw.length);
        if (payload.length > TunnelConst.MAX_DATA_FRAME_SIZE) {
            throw new TunnelProtocolException("数据帧超过上限 " + TunnelConst.MAX_DATA_FRAME_SIZE + " 字节: " + payload.length);
        }
        return new TunnelDataFrame(sid, payload, flags);
    }

    /**
     * 把一段完整数据切成多个数据帧（最后一帧带 FIN）
     *
     * @param sid  逻辑请求 / 流标识
     * @param body 完整数据（可为空，为空时返回一个仅带 FIN 的帧）
     * @return 帧列表
     */
    public static List<byte[]> split(String sid, byte[] body) {
        List<byte[]> frames = new ArrayList<>();
        int length = body == null ? 0 : body.length;
        if (length == 0) {
            frames.add(encodeData(sid, null, true));
            return frames;
        }
        int offset = 0;
        while (offset < length) {
            int end = Math.min(offset + TunnelConst.MAX_DATA_FRAME_SIZE, length);
            byte[] slice = Arrays.copyOfRange(body, offset, end);
            frames.add(encodeData(sid, slice, end >= length));
            offset = end;
        }
        return frames;
    }

    private static byte[] sidBytes(String sid) {
        if (StrUtil.isEmpty(sid)) {
            throw new TunnelProtocolException("数据帧 sid 不能为空");
        }
        byte[] sidBytes = sid.getBytes(StandardCharsets.UTF_8);
        if (sidBytes.length > TunnelConst.MAX_SID_LENGTH) {
            throw new TunnelProtocolException("数据帧 sid 超长: " + sidBytes.length);
        }
        return sidBytes;
    }
}
