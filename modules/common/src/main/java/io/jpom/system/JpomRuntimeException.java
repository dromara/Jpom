package io.jpom.system;

/**
 * Jpom 运行错误
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class JpomRuntimeException extends RuntimeException {

    public JpomRuntimeException(String message) {
        super(message);
    }

    public JpomRuntimeException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
