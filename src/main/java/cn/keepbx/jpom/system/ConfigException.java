package cn.keepbx.jpom.system;

import java.io.IOException;

/**
 * 配置异常
 *
 * @author jiangzeyin
 * @date 2019/1/16
 */
public class ConfigException extends IOException {
    /**
     * 配置异常的路径
     */
    private String path;

    public String getPath() {
        return path;
    }

    public ConfigException(String message, String path) {
        super(message);
        this.path = path;
    }
}
