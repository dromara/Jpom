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

import cn.hutool.core.util.RandomUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 隧道协议编解码回归测试。
 * <p>
 * 协议是服务端与 Java/Go 双端插件共用的契约，帧头一旦漂移会导致“连上了但所有请求都报错”，
 * 因此这里锁定帧结构、边界与分帧重组行为。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelFrameCodecTest {

    @Test
    public void testControlFrameRoundTrip() {
        Map<String, String> headers = new HashMap<>();
        headers.put(TunnelConst.HEADER_WORKSPACE_ID, "default");
        headers.put(TunnelConst.HEADER_AUTHORIZE, "abc");

        TunnelFrame frame = TunnelFrame.of(TunnelFrameType.REQ, "s-1");
        frame.setMethod("POST");
        frame.setPath("/manage/project/list");
        frame.setContentType("application/x-www-form-urlencoded");
        frame.setHeaders(headers);
        frame.setLen(0L);
        frame.setTimeout(30);

        String text = TunnelFrameCodec.encodeControl(frame);
        Assertions.assertFalse(text.contains("null"), "控制帧不应输出 null 字段: " + text);

        TunnelFrame decoded = TunnelFrameCodec.decodeControl(text);
        Assertions.assertEquals(TunnelFrameType.REQ, decoded.frameType());
        Assertions.assertEquals("s-1", decoded.getSid());
        Assertions.assertEquals("POST", decoded.getMethod());
        Assertions.assertEquals("/manage/project/list", decoded.getPath());
        Assertions.assertEquals("default", decoded.getHeaders().get(TunnelConst.HEADER_WORKSPACE_ID));
        Assertions.assertEquals(30, decoded.getTimeout().intValue());
    }

    @Test
    public void testPushFrameRoundTrip() {
        // PUSH 是插件端 -> 服务端的单向帧：不占用 sid，靠 topic/seq/body 承载内容
        TunnelFrame frame = TunnelFrame.push(TunnelConst.TOPIC_NODE_STAT, 7L, "{\"simpleStatus\":{\"cpu\":1.5}}");

        String text = TunnelFrameCodec.encodeControl(frame);
        Assertions.assertFalse(text.contains("null"), "推送帧不应输出 null 字段: " + text);

        TunnelFrame decoded = TunnelFrameCodec.decodeControl(text);
        Assertions.assertEquals(TunnelFrameType.PUSH, decoded.frameType());
        Assertions.assertEquals(TunnelConst.TOPIC_NODE_STAT, decoded.getTopic());
        Assertions.assertEquals(7L, decoded.getSeq().longValue());
        Assertions.assertTrue(decoded.getBody().contains("simpleStatus"));
        Assertions.assertNull(decoded.getSid(), "推送帧不需要 sid");
    }

    @Test
    public void testHelloAckPushCapability() {
        // 服务端声明支持推送
        TunnelFrame ack = TunnelFrame.of(TunnelFrameType.HELLO_ACK, null);
        ack.setPush(Boolean.TRUE);
        TunnelFrame decoded = TunnelFrameCodec.decodeControl(TunnelFrameCodec.encodeControl(ack));
        Assertions.assertEquals(Boolean.TRUE, decoded.getPush());
        Assertions.assertEquals(TunnelFrameType.HELLO_ACK, decoded.frameType());

        // 老服务端不带该字段：插件端必须视为“不支持”，不能默认推送
        TunnelFrame legacy = TunnelFrame.of(TunnelFrameType.HELLO_ACK, null);
        TunnelFrame legacyDecoded = TunnelFrameCodec.decodeControl(TunnelFrameCodec.encodeControl(legacy));
        Assertions.assertNull(legacyDecoded.getPush(), "缺少能力位必须解析为 null");
    }

    @Test
    public void testDecodeControlRejectsUnknownType() {
        Assertions.assertThrows(TunnelProtocolException.class, () -> TunnelFrameCodec.decodeControl("{\"type\":\"NOPE\"}"));
        Assertions.assertThrows(TunnelProtocolException.class, () -> TunnelFrameCodec.decodeControl(""));
        Assertions.assertThrows(TunnelProtocolException.class, () -> TunnelFrameCodec.decodeControl("not a json"));
    }

    @Test
    public void testEncodeControlRequiresType() {
        Assertions.assertThrows(TunnelProtocolException.class, () -> TunnelFrameCodec.encodeControl(new TunnelFrame()));
        Assertions.assertThrows(TunnelProtocolException.class, () -> TunnelFrameCodec.encodeControl(null));
    }

    @Test
    public void testDataFrameRoundTrip() {
        byte[] payload = "hello 插件端".getBytes(StandardCharsets.UTF_8);
        byte[] raw = TunnelFrameCodec.encodeData("s-42", payload, true);

        TunnelDataFrame frame = TunnelFrameCodec.decodeData(raw);
        Assertions.assertEquals("s-42", frame.getSid());
        Assertions.assertTrue(frame.isFin());
        Assertions.assertArrayEquals(payload, frame.getPayload());
    }

    @Test
    public void testDataFrameWithoutPayloadAndFin() {
        byte[] raw = TunnelFrameCodec.encodeData("s-1", null, false);
        TunnelDataFrame frame = TunnelFrameCodec.decodeData(raw);
        Assertions.assertEquals(0, frame.getPayloadLength());
        Assertions.assertFalse(frame.isFin());
    }

    @Test
    public void testDecodeDataRejectsBrokenFrames() {
        // 魔数错误
        Assertions.assertThrows(TunnelProtocolException.class, () -> TunnelFrameCodec.decodeData(new byte[]{0x02, 1, 'a', 0}));
        // 长度不足
        Assertions.assertThrows(TunnelProtocolException.class, () -> TunnelFrameCodec.decodeData(new byte[]{0x01}));
        Assertions.assertThrows(TunnelProtocolException.class, () -> TunnelFrameCodec.decodeData(null));
        // sid 为空
        Assertions.assertThrows(TunnelProtocolException.class, () -> TunnelFrameCodec.encodeData("", null, true));
        // sid 超长
        String longSid = RandomUtil.randomString(TunnelConst.MAX_SID_LENGTH + 1);
        Assertions.assertThrows(TunnelProtocolException.class, () -> TunnelFrameCodec.encodeData(longSid, null, true));
    }

    @Test
    public void testPayloadSizeLimit() {
        byte[] tooBig = new byte[TunnelConst.MAX_DATA_FRAME_SIZE + 1];
        Assertions.assertThrows(TunnelProtocolException.class, () -> TunnelFrameCodec.encodeData("s-1", tooBig, true));
    }

    @Test
    public void testSplitAndReassemble() {
        int total = TunnelConst.MAX_DATA_FRAME_SIZE * 2 + 1024;
        byte[] body = new byte[total];
        for (int i = 0; i < total; i++) {
            body[i] = (byte) (i % 251);
        }
        List<byte[]> frames = TunnelFrameCodec.split("s-9", body);
        Assertions.assertEquals(3, frames.size(), "600KiB 数据应切分为 3 帧");

        byte[] rebuilt = new byte[total];
        int offset = 0;
        for (int i = 0; i < frames.size(); i++) {
            TunnelDataFrame frame = TunnelFrameCodec.decodeData(frames.get(i));
            Assertions.assertEquals("s-9", frame.getSid());
            Assertions.assertEquals(i == frames.size() - 1, frame.isFin(), "只有最后一帧带 FIN");
            System.arraycopy(frame.getPayload(), 0, rebuilt, offset, frame.getPayloadLength());
            offset += frame.getPayloadLength();
        }
        Assertions.assertEquals(total, offset);
        Assertions.assertArrayEquals(body, rebuilt);
    }

    @Test
    public void testSplitEmptyBody() {
        List<byte[]> frames = TunnelFrameCodec.split("s-1", null);
        Assertions.assertEquals(1, frames.size());
        TunnelDataFrame frame = TunnelFrameCodec.decodeData(frames.get(0));
        Assertions.assertEquals(0, frame.getPayloadLength());
        Assertions.assertTrue(frame.isFin());
    }
}
