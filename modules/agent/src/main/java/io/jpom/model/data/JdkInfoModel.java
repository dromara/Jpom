package io.jpom.model.data;

import io.jpom.model.BaseModel;

/**
 * jdk 信息
 *
 * @author bwcx_jzy
 * @date 2019/10/29
 */
public class JdkInfoModel extends BaseModel {

    /**
     * jdk 路径
     */
    private String path;

    /**
     * jdk 版本
     */
    private String version;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
