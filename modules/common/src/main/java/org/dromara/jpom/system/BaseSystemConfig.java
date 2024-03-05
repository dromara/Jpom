/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.system;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.system.SystemUtil;
import lombok.Data;
import org.dromara.jpom.common.JpomApplicationEvent;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.RemoteVersion;

import java.nio.charset.Charset;

/**
 * @author bwcx_jzy
 * @since 2022/12/17
 */
@Data
public abstract class BaseSystemConfig {

    /**
     * 是否开启秒级匹配
     */
    private boolean timerMatchSecond = false;
    /**
     * 允许降级
     */
    private boolean allowedDowngrade = false;
    /**
     * 旧包文件保留个数
     */
    private int oldJarsCount = 2;
    /**
     * 远程更新地址
     */
    private String remoteVersionUrl;
    /**
     * 系统日志编码格式
     */
    private Charset logCharset;
    /**
     * 控制台编码格式
     */
    private Charset consoleCharset;
    /**
     * 执行系统主要命名是否填充 sudo(sudo xxx)
     * 使用前提需要配置 sudo 免密
     */
    private boolean commandUseSudo = false;

    public void setTimerMatchSecond(boolean timerMatchSecond) {
        this.timerMatchSecond = timerMatchSecond;
        // 开启秒级
        CronUtil.setMatchSecond(timerMatchSecond);
    }

    public void setOldJarsCount(int oldJarsCount) {
        this.oldJarsCount = oldJarsCount;
        JpomApplicationEvent.setOldJarsCount(oldJarsCount);
    }

    public void setRemoteVersionUrl(String remoteVersionUrl) {
        this.remoteVersionUrl = remoteVersionUrl;
        RemoteVersion.setRemoteVersionUrl(remoteVersionUrl);
    }

    /**
     * 默认 utf-8
     *
     * @return 日志文件编码格式
     */
    public Charset getLogCharset() {
        return ObjectUtil.defaultIfNull(logCharset, CharsetUtil.CHARSET_UTF_8);
    }


    public void setConsoleCharset(Charset consoleCharset) {
        this.consoleCharset = consoleCharset;
        ExtConfigBean.setConsoleLogCharset(consoleCharset);
    }

    public void setAllowedDowngrade(boolean allowedDowngrade) {
        this.allowedDowngrade = allowedDowngrade;
        JpomManifest.setAllowedDowngrade(allowedDowngrade);
    }

    public void setCommandUseSudo(boolean commandUseSudo) {
        this.commandUseSudo = commandUseSudo;
        SystemUtil.set("JPOM_COMMAND_USE_SUDO", String.valueOf(commandUseSudo));
    }
}
