package cn.jiangzeyin.system;

import java.io.IOException;

/**
 * @author jiangzeyin
 * @date 2019/1/16
 */
public class ConfigException extends IOException {
    private String path;

    public String getPath() {
        return path;
    }

    public ConfigException(String message, String path) {
        super(message);
        this.path = path;
    }
}
