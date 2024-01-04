package org.dromara.jpom.exception;

/**
 * @author bwcx_jzy
 * @since 24/1/4 004
 */
public class LogRecorderCloseException extends IllegalStateException {
    public LogRecorderCloseException() {
        super("日志记录器被关闭/或者未启用");
    }
}
