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
