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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * 隧道握手鉴权：连接密钥 + 机器节点标识 + 时间戳 + 随机数 的 HMAC-SHA256 签名。
 * <p>
 * 服务端只配置一个全局连接密钥，因此签名必须绑定 {@code machineId} 与时间戳，
 * 避免密钥泄露后任意机器无条件接入，也让重放失效。
 *
 * @author jpom
 * @since 2026/11/18
 */
public final class TunnelAuth {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private TunnelAuth() {
    }

    /**
     * 生成签名
     *
     * @param key       连接密钥
     * @param machineId 机器节点标识
     * @param ts        时间戳（秒）
     * @param nonce     随机数
     * @return 小写十六进制签名
     */
    public static String sign(String key, String machineId, long ts, String nonce) {
        if (StrUtil.isEmpty(key)) {
            throw new TunnelProtocolException("连接密钥不能为空");
        }
        if (StrUtil.isEmpty(machineId)) {
            throw new TunnelProtocolException("机器节点标识不能为空");
        }
        String plain = machineId + "|" + ts + "|" + (nonce == null ? StrUtil.EMPTY : nonce);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] raw = mac.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception e) {
            throw new TunnelProtocolException("握手签名计算失败", e);
        }
    }

    /**
     * 校验签名（含时间戳偏移校验）
     *
     * @param key        连接密钥
     * @param machineId  机器节点标识
     * @param ts         时间戳（秒）
     * @param nonce      随机数
     * @param sign       对端签名
     * @param nowSeconds 当前时间戳（秒）
     * @param skewSeconds 允许的时间偏移（秒）
     * @return true 校验通过
     */
    public static boolean verify(String key, String machineId, Long ts, String nonce, String sign, long nowSeconds, long skewSeconds) {
        if (ts == null || StrUtil.isEmpty(sign)) {
            return false;
        }
        if (!timestampValid(ts, nowSeconds, skewSeconds)) {
            return false;
        }
        String expect = sign(key, machineId, ts, nonce);
        // 常量时间比较，避免时间侧信道
        return MessageDigest.isEqual(expect.getBytes(StandardCharsets.UTF_8), sign.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 时间戳是否在允许偏移内
     *
     * @param ts          时间戳（秒）
     * @param nowSeconds  当前时间戳（秒）
     * @param skewSeconds 允许偏移（秒）
     * @return true 合法
     */
    public static boolean timestampValid(long ts, long nowSeconds, long skewSeconds) {
        return Math.abs(nowSeconds - ts) <= skewSeconds;
    }

    /**
     * 生成随机数
     *
     * @return 随机数
     */
    public static String nonce() {
        return UUID.randomUUID().toString().replace("-", StrUtil.EMPTY);
    }
}
