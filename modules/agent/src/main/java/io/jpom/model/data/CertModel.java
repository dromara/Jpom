/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.model.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.BaseModel;
import io.jpom.service.system.CertService;
import io.jpom.system.JpomRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * 证书实体
 *
 * @author Arno
 */
@Slf4j
public class CertModel extends BaseModel {

    private static final String KEY = "Jpom 管理系统";
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
    private Type type;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

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
        JSONObject jsonObject = decodeCert(getCert(), getKey());
        if (jsonObject != null) {
            // 获取信息
            this.setDomain(jsonObject.getString("domain"));
            this.setExpirationTime(jsonObject.getLongValue("expirationTime"));
            this.setEffectiveTime(jsonObject.getLongValue("effectiveTime"));

            // 数据持久化到文件中
            CertService certService = SpringUtil.getBean(CertService.class);
            certService.updateItem(this);
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

    /**
     * 解析证书
     *
     * @param key  zip里面文件
     * @param file 证书文件
     * @return 处理后的json
     */
    public static JSONObject decodeCert(String file, String key) {
        if (file == null) {
            return null;
        }
        if (!FileUtil.exist(file)) {
            return null;
        }
        InputStream inputStream = null;
        try {
            inputStream = ResourceUtil.getStream(key);
            PrivateKey privateKey = PemUtil.readPemPrivateKey(inputStream);
            IoUtil.close(inputStream);
            inputStream = ResourceUtil.getStream(file);
            PublicKey publicKey = PemUtil.readPemPublicKey(inputStream);
            IoUtil.close(inputStream);
            RSA rsa = new RSA(privateKey, publicKey);
            String encryptStr = rsa.encryptBase64(KEY, KeyType.PublicKey);
            String decryptStr = rsa.decryptStr(encryptStr, KeyType.PrivateKey);
            if (!KEY.equals(decryptStr)) {
                throw new JpomRuntimeException("证书和私钥证书不匹配");
            }
        } finally {
            IoUtil.close(inputStream);
        }
        try {
            inputStream = ResourceUtil.getStream(file);
            // 创建证书对象
            X509Certificate oCert = (X509Certificate) KeyUtil.readX509Certificate(inputStream);
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
            jsonObject.put("pemPath", file);
            jsonObject.put("keyPath", key);
            return jsonObject;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            IoUtil.close(inputStream);
        }
        return null;
    }

    /**
     * 证书类型
     */
    public enum Type {
        /**
         *
         */
        pem,
        /**
         * Windows
         */
        cer,
        /**
         * Linux
         */
        crt
    }
}
