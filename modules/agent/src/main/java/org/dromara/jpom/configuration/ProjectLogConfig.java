/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
