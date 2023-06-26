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
package org.dromara.jpom.plugins;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author bwcx_
 */
@Slf4j
public class JschLogger implements com.jcraft.jsch.Logger {

    public static final JschLogger LOGGER = new JschLogger();

    @Override
    public boolean isEnabled(int level) {
        switch (level) {
            case DEBUG:
                return log.isDebugEnabled();
            case INFO:
                return log.isInfoEnabled();
            case WARN:
                return log.isWarnEnabled();
            case ERROR:
            case FATAL:
                return log.isErrorEnabled();
            default:
                log.warn("未知的 jsch 日志级别：{}", level);
                return false;
        }
    }

    @Override
    public void log(int level, String message) {
        switch (level) {
            case DEBUG:
                // info 日志太多 记录维 debug
            case INFO:
                log.debug(message);
                break;
            case WARN:
                if (StrUtil.isWrap(message, "Permanently added", "to the list of known hosts.")) {
                    // 避免过多日志
                    log.debug(message);
                } else {
                    log.warn(message);
                }
                break;
            case ERROR:
            case FATAL:
                log.error(message);
                break;
            default:
                log.warn("未知的 jsch 日志级别：{} {}", level, message);
        }
    }
}
