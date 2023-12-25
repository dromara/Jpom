package org.dromara.jpom.configuration;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
     * 登录token 加密的key 长度建议控制到 16位
     */
    private String tokenJwtKey;

    public byte[] getTokenJwtKeyByte() {
        return StrUtil.emptyToDefault(this.tokenJwtKey, "KZQfFBJTW2v6obS1").getBytes();
    }
}
