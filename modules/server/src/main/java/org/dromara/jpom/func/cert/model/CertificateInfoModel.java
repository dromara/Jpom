/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.cert.model;

import cn.hutool.core.annotation.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;

/**
 * @author bwcx_jzy
 * @since 2023/3/22
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "CERTIFICATE_INFO",
    nameKey = "i18n.certificate_info_table.fff8")
@Data
@NoArgsConstructor
public class CertificateInfoModel extends BaseWorkspaceModel {
    /**
     * 证书类型
     */
    private String keyType;
    private String keyAlias;
    /**
     * 指纹
     */
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

    /**
     * 文件是否存在
     */
    @PropIgnore
    private Boolean fileExists;

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
