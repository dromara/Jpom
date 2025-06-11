package org.dromara.jpom.socket.handler;

import java.util.Arrays;

/**
 * 功能键枚举
 *
 * @author bwcx_jzy
 * @since 2025/6/11
 */
public enum KeyControl {
    KEY_TAB((byte) 9), // TAB
    KEY_ETX((byte) 3), // Control + C
    KEY_ENTER((byte) 13), // Enter
    KEY_SEARCH((byte) 18), // Control + R
    KEY_BACK((byte) 127), // 退格键
    KEY_DELETE(new byte[]{27, 91, 51, 126}), // DELETE键
    KEY_LEFT(new byte[]{27, 91, 68}), // 左
    KEY_RIGHT(new byte[]{27, 91, 67}), // 右
    KEY_UP(new byte[]{27, 91, 65}), // 上
    KEY_DOWN(new byte[]{27, 91, 66}), // 下
    KEY_HOME(new byte[]{27, 91, 72}),
    KEY_END(new byte[]{27, 91, 70}),
    KEY_FUNCTION(new byte[]{27, 91}), //其他功能键
    KEY_INPUT(new byte[]{-1}); // 正常输入

    private final byte[] control;

    KeyControl(byte... control) {
        this.control = control;
    }

    public static KeyControl getKeyControl(byte[] bytes) {
        for (KeyControl value : KeyControl.values()) {
            if (Arrays.equals(value.control, bytes)) {
                return value;
            }
        }
        // 其他功能键
        if (Arrays.equals(KEY_FUNCTION.control, Arrays.copyOf(bytes, 2))) {
            return KEY_FUNCTION;
        }
        // 正常输入
        return KEY_INPUT;
    }
}
