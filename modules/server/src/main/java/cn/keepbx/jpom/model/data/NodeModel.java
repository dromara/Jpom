package cn.keepbx.jpom.model.data;

import cn.keepbx.jpom.model.BaseModel;

/**
 * 节点实体
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class NodeModel extends BaseModel {
    private String name;
    private String url;
    private String loginName;
    private String loginPwd;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getLoginPwd() {
        return loginPwd;
    }

    public void setLoginPwd(String loginPwd) {
        this.loginPwd = loginPwd;
    }
}
