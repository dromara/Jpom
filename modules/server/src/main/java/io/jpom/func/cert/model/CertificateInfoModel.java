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
package io.jpom.func.cert.model;

import io.jpom.model.BaseWorkspaceModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import top.jpom.h2db.TableName;

/**
 * @author bwcx_jzy
 * @since 2023/3/22
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "CERTIFICATE_INFO", name = "证书信息表")
@Data
@NoArgsConstructor
public class CertificateInfoModel extends BaseWorkspaceModel {
    /**
     * 证书类型
     */
    private String keyType;
    private String keyAlias;
    private String fingerprint;
    /**
     * 证书密码
     */
    private String certPassword;
    /**
     * 证书序列号
     */
    private String serialNumberStr;
    /**
     * 颁发者 DN 名称
     */
    private String issuerDnName;
    /**
     * 主题 DN 名称
     */
    private String subjectDnName;
    /**
     * 版本号
     */
    private Integer certVersion;
    /**
     *
     */
    private String sigAlgOid;
    /**
     * 算法名
     */
    private String sigAlgName;

    /**
     * 证书到期时间
     */
    private Long expirationTime;
    /**
     * 证书生效日期
     */
    private Long effectiveTime;

    private String description;

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
