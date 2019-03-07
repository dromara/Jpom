package cn.keepbx.jpom.model;

import com.alibaba.fastjson.JSONObject;

/**
 * @author Arno
 */
public class CertModel {

    private String id;
    /**
     * 证书文件
     */
    private String cert;
    /**
     * 私钥
     */
    private String key;
    /**
     * 证书到期时间
     */
    private long expirationTime;
    /**
     * 绑定域名
     */
    private String domain;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public JSONObject toJson() {
        return (JSONObject) JSONObject.toJSON(this);
    }
}
