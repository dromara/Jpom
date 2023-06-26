package org.dromara.jpom.plugins;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JschLogger implements com.jcraft.jsch.Logger {

    @Override
    public boolean isEnabled(int level) {
        switch (level) {
            case DEBUG:
                return log.isDebugEnabled() || log.isTraceEnabled();
            case INFO:
                return log.isInfoEnabled();
            case WARN:
                return log.isWarnEnabled();
            case ERROR:
            case FATAL:
                return log.isErrorEnabled();
            default:
                throw new IllegalArgumentException("Unknown log level: " + level);
        }
    }

    @Override
    public void log(int level, String message) {
        switch (level) {
            case DEBUG:
                log.debug(message);
                break;
            case INFO:
                log.info(message);
                break;
            case WARN:
                log.warn(message);
                break;
            case ERROR:
                log.error(message);
                break;
            case FATAL:
                log.error("FATAL: {}", message);
                break;
            default:
                throw new IllegalArgumentException("Unknown log level: " + level);
        }
    }


}
