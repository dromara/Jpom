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

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.system.SystemUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 23/12/29 029
 */
@Data
@ConfigurationProperties("jpom.project.log")
public class ProjectLogConfig {
    /**
     * 检测控制台日志周期，防止日志文件过大，目前暂只支持linux 不停服备份
     */
    private String autoBackupConsoleCron = "0 0/10 * * * ?";
    /**
     * 当文件多大时自动备份
     *
     * @see ch.qos.logback.core.util.FileSize
     */
    private DataSize autoBackupSize = DataSize.ofMegabytes(50);
    /**
     * 是否自动将控制台日志文件备份
     */
    private boolean autoBackupToFile = true;

    /**
     * 控制台日志保存时长单位天
     */
    private int saveDays = 7;

    public int getSaveDays() {
        return Math.max(saveDays, 0);
    }

    /**
     * 日志文件的编码格式
     */
    private Charset fileCharset;

    public Charset getFileCharset() {
        return Optional.ofNullable(this.fileCharset).orElseGet(() ->
            SystemUtil.getOsInfo().isWindows() ?
                CharsetUtil.CHARSET_GBK : CharsetUtil.CHARSET_UTF_8);
    }
}
