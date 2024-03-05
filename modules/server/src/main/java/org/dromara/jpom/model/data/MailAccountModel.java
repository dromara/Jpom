/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import cn.hutool.core.util.ObjectUtil;
import cn.keepbx.jpom.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统邮箱配置
 *
 * @author bwcx_jzy
 * @since 2019/7/16
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class MailAccountModel extends BaseJsonModel {

    public static final String ID = "MAIL_CONFIG";

    /**
     * SMTP服务器域名
     */
    private String host;
    /**
     * SMTP服务端口
     */
    private Integer port;
    /**
     * 用户名
     */
    private String user;
    /**
     * 密码
     */
    private String pass;
    /**
     * 发送方，遵循RFC-822标准
     */
    private String from;
    /**
     * 使用 SSL安全连接
     */
    private Boolean sslEnable;
    /**
     * 指定的端口连接到在使用指定的套接字工厂。如果没有设置,将使用默认端口
     */
    @Deprecated
    private Integer socketFactoryPort;

    /**
     * 超时时间
     */
    private Integer timeout;

    /**
     * 兼容端口
     *
     * @return port
     */
    public Integer getPort() {
        if (sslEnable != null && sslEnable) {
            if (socketFactoryPort != null) {
                return socketFactoryPort;
            }
        }
        return ObjectUtil.defaultIfNull(port, socketFactoryPort);
    }

}
