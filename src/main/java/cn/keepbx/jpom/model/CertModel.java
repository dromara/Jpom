package cn.keepbx.jpom.model;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.service.system.CertService;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * 证书实体
 *
 * @author Arno
 */
public class CertModel extends BaseModel {
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
     * 证书生效日期
     */
    private long effectiveTime;
    /**
     * 绑定域名
     */
    private String domain;
    /**
     * 白名单路径
     */
    private String whitePath;

    public String getWhitePath() {
        return whitePath;
    }

    public void setWhitePath(String whitePath) {
        this.whitePath = whitePath;
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
        this.convertInfo();
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public String getDomain() {
        this.convertInfo();
        return domain;
    }

    /**
     * 兼容手动添加的证书文件
     */
    private void convertInfo() {
        if (!StrUtil.isEmpty(domain)) {
            return;
        }
        JSONObject jsonObject = decodeCert(getCert());
        if (jsonObject != null) {
            // 获取信息
            this.setDomain(jsonObject.getString("domain"));
            this.setExpirationTime(jsonObject.getLongValue("expirationTime"));
            this.setEffectiveTime(jsonObject.getLongValue("effectiveTime"));

            // 数据持久化到文件中
            CertService certService = SpringUtil.getBean(CertService.class);
            certService.updateCert(this);
        }
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public long getEffectiveTime() {
        this.convertInfo();
        return effectiveTime;
    }

    public void setEffectiveTime(long effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public JSONObject toJson() {
        return (JSONObject) JSONObject.toJSON(this);
    }


    /**
     * 解析证书
     *
     * @param file 证书文件
     */
    public static JSONObject decodeCert(String file) {
        if (file == null) {
            return null;
        }
        if (!FileUtil.exist(file)) {
            return null;
        }
        try {
            BufferedInputStream inStream = FileUtil.getInputStream(file);
            // 创建X509工厂类
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // 创建证书对象
            X509Certificate oCert = (X509Certificate) cf.generateCertificate(inStream);
            inStream.close();
            //到期时间
            Date expirationTime = oCert.getNotAfter();
            //生效日期
            Date effectiveTime = oCert.getNotBefore();
            //域名
            String name = oCert.getSubjectDN().getName();
            int i = name.indexOf("=");
            String domain = name.substring(i + 1);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("expirationTime", expirationTime.getTime());
            jsonObject.put("effectiveTime", effectiveTime.getTime());
            jsonObject.put("domain", domain);
            return jsonObject;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }
}
