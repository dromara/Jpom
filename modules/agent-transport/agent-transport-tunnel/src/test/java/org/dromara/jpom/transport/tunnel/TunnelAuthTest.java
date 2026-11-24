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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 握手鉴权回归测试：全局连接密钥 + machineId + 时间戳 + nonce 的签名校验。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelAuthTest {

    private static final String KEY = "tunnel-secret";
    private static final String MACHINE_ID = "machine-1";

    @Test
    public void testSignIsDeterministic() {
        long ts = 1_800_000_000L;
        String sign1 = TunnelAuth.sign(KEY, MACHINE_ID, ts, "nonce");
        String sign2 = TunnelAuth.sign(KEY, MACHINE_ID, ts, "nonce");
        Assertions.assertEquals(sign1, sign2);
        Assertions.assertEquals(64, sign1.length(), "HMAC-SHA256 十六进制应为 64 字符");
    }

    /**
     * 与 Go 插件端共用同一份签名向量（go-agent/internal/tunnel/tunnel_test.go），
     * 两端各改一处后签名对不上会在这里先炸，而不是等到线上握手失败。
     */
    @Test
    public void testSignMatchesGoVector() {
        String sign = TunnelAuth.sign("tunnel-secret", "machine-1", 1_800_000_000L, "nonce");
        Assertions.assertEquals("3b0aec6b355848101988e9933cad81359446f849cff0dc5d672b0a8d181d9a87", sign);
    }

    @Test
    public void testSignChangesWithInput() {
        long ts = 1_800_000_000L;
        String base = TunnelAuth.sign(KEY, MACHINE_ID, ts, "nonce");
        Assertions.assertNotEquals(base, TunnelAuth.sign("other-key", MACHINE_ID, ts, "nonce"));
        Assertions.assertNotEquals(base, TunnelAuth.sign(KEY, "machine-2", ts, "nonce"));
        Assertions.assertNotEquals(base, TunnelAuth.sign(KEY, MACHINE_ID, ts + 1, "nonce"));
        Assertions.assertNotEquals(base, TunnelAuth.sign(KEY, MACHINE_ID, ts, "nonce-2"));
    }

    @Test
    public void testVerify() {
        long now = 1_800_000_000L;
        long ts = now - 10;
        String sign = TunnelAuth.sign(KEY, MACHINE_ID, ts, "nonce");

        Assertions.assertTrue(TunnelAuth.verify(KEY, MACHINE_ID, ts, "nonce", sign, now, TunnelConst.DEFAULT_AUTH_SKEW_SECONDS));
        // 密钥不一致
        Assertions.assertFalse(TunnelAuth.verify("other-key", MACHINE_ID, ts, "nonce", sign, now, TunnelConst.DEFAULT_AUTH_SKEW_SECONDS));
        // 机器标识不一致（防止拿别人的签名冒用）
        Assertions.assertFalse(TunnelAuth.verify(KEY, "machine-2", ts, "nonce", sign, now, TunnelConst.DEFAULT_AUTH_SKEW_SECONDS));
        // 签名被改写
        Assertions.assertFalse(TunnelAuth.verify(KEY, MACHINE_ID, ts, "nonce", sign.substring(1), now, TunnelConst.DEFAULT_AUTH_SKEW_SECONDS));
        // 时间戳超出偏移（重放）
        Assertions.assertFalse(TunnelAuth.verify(KEY, MACHINE_ID, ts, "nonce", sign,
            now + TunnelConst.DEFAULT_AUTH_SKEW_SECONDS + 1, TunnelConst.DEFAULT_AUTH_SKEW_SECONDS));
    }

    @Test
    public void testVerifyRejectsMissingFields() {
        long now = 1_800_000_000L;
        Assertions.assertFalse(TunnelAuth.verify(KEY, MACHINE_ID, null, "nonce", "sign", now, 300));
        Assertions.assertFalse(TunnelAuth.verify(KEY, MACHINE_ID, now, "nonce", null, now, 300));
    }

    @Test
    public void testTimestampValid() {
        long now = 1_800_000_000L;
        Assertions.assertTrue(TunnelAuth.timestampValid(now, now, 300));
        Assertions.assertTrue(TunnelAuth.timestampValid(now - 300, now, 300));
        Assertions.assertFalse(TunnelAuth.timestampValid(now - 301, now, 300));
        Assertions.assertFalse(TunnelAuth.timestampValid(now + 301, now, 300));
    }

    @Test
    public void testNonce() {
        String nonce1 = TunnelAuth.nonce();
        String nonce2 = TunnelAuth.nonce();
        Assertions.assertEquals(32, nonce1.length());
        Assertions.assertNotEquals(nonce1, nonce2);
    }
}
