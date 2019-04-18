package cn.keepbx.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.BaseModel;
import com.alibaba.fastjson.JSONArray;

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
    /**
     * 节点协议
     */
    private String protocol = "http";

    private String authorize;
    /**
     * 项目信息
     */
    private JSONArray projects;
    private boolean openStatus;

    public boolean isOpenStatus() {
        return openStatus;
    }

    public void setOpenStatus(boolean openStatus) {
        this.openStatus = openStatus;
    }

    public JSONArray getProjects() {
        return projects;
    }

    public void setProjects(JSONArray projects) {
        this.projects = projects;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol.toLowerCase();
    }

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

    public String getAuthorize(boolean get) {
        if (authorize == null) {
            authorize = SecureUtil.sha1(loginName + "@" + loginPwd);
        }
        return authorize;
    }

    public String getRealUrl(NodeUrl nodeUrl) {
        return StrUtil.format("{}://{}{}", getProtocol(), getUrl(), nodeUrl.getUrl());
    }
}
