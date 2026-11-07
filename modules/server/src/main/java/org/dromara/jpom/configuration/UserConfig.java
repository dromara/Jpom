/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.configuration;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 23/12/25 025
 */
@Data
@ConfigurationProperties("jpom.user")
public class UserConfig {
    /**
     * 用户连续登录失败次数，超过此数将自动不再被允许登录，零是不限制
     */
    private int alwaysLoginError = 5;

    /**
     * IP连续登录失败次数，超过此数将自动不再被允许登录，零是不限制
     */
    private int alwaysIpLoginError = 10;

    /**
     * 是否强制提醒用户开启  mfa
     */
    private boolean forceMfa = false;
    /**
     * 当ip连续登录失败，锁定对应IP时长，单位毫秒
     */
    private Duration ipErrorLockTime;

    public Duration getIpErrorLockTime() {
        return Optional.ofNullable(this.ipErrorLockTime).orElseGet(() -> {
            ipErrorLockTime = Duration.ofHours(5);
            return ipErrorLockTime;
        });
    }

    /**
     * demo 账号的提示
     */
    private String demoTip;


    /**
     * 登录token失效时间(单位：小时),默认为24
     */
    private int tokenExpired = 24;

    public int getTokenExpired() {
        return Math.max(this.tokenExpired, 1);
    }

    /**
     * 登录token失效后自动续签时间（单位：分钟），默认为60，
     */
    private int tokenRenewal = 60;

    public int getTokenRenewal() {
        return Math.max(this.tokenRenewal, 1);
    }

    /**
     * 登录token 签名(加密)的key，建议使用至少 32 位的随机字符串。
     * 未配置时由 {@link org.dromara.jpom.util.JwtKeyUtil} 在首次启动时生成并持久化到数据目录。
     */
    private String tokenJwtKey;

    public byte[] getTokenJwtKeyByte() {
        if (StrUtil.isBlank(this.tokenJwtKey)) {
            // 正常情况下 ServerConfig 初始化时会调用 JwtKeyUtil 写入密钥；
            // 走到这里说明启动流程异常，直接失败而不是回退到公开固定密钥。
            throw new IllegalStateException("JWT token key is not configured or not initialized");
        }
        return this.tokenJwtKey.getBytes(StandardCharsets.UTF_8);
    }
}
