package org.dromara.jpom.exception;

/**
 * 权限异常
 *
 * @author bwcx_jzy
 * @since 23/12/29 029
 */
public class PermissionException extends RuntimeException {
    public PermissionException(String message) {
        super(message);
    }
}
