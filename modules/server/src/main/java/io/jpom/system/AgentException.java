package io.jpom.system;

/**
 * agent 插件端异常
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
public class AgentException extends RuntimeException {

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
