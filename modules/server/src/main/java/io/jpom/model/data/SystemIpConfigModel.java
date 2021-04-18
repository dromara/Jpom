package io.jpom.model.data;

import io.jpom.model.BaseJsonModel;

/**
 * @author bwcx_jzy
 * @date 2021/4/18
 */
public class SystemIpConfigModel extends BaseJsonModel {

    /**
     * ip 白名单  允许访问
     */
    private String allowed;

    /**
     * 禁止
     */
    private String prohibited;

    public String getAllowed() {
        return allowed;
    }

    public void setAllowed(String allowed) {
        this.allowed = allowed;
    }

    public String getProhibited() {
        return prohibited;
    }

    public void setProhibited(String prohibited) {
        this.prohibited = prohibited;
    }
}
