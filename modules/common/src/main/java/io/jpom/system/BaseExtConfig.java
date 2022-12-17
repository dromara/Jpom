package io.jpom.system;

import lombok.Data;

/**
 * @author bwcx_jzy
 * @since 2022/12/17
 */
@Data
public abstract class BaseExtConfig {
    /**
     * 数据目录
     */
    private String path;

    public void setPath(String path) {
        this.path = path;
        ExtConfigBean.setPath(path);
    }
}
