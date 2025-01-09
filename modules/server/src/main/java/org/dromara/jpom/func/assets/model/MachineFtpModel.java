/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.model;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.ftp.FtpConfig;
import cn.hutool.extra.ftp.FtpMode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseGroupNameModel;

/**
 * @author bwcx_jzy
 * @see FtpConfig
 * @since 2024/08/31
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MACHINE_FTP_INFO",
    nameKey = "资产FTP信息")
@Data
@NoArgsConstructor
public class MachineFtpModel extends BaseGroupNameModel {

    /**
     * 主机地址
     */
    private String host;
    /**
     * 端口
     */
    private Integer port;
    /**
     * 登录账号
     */
    private String user;
    /**
     * 账号密码
     */
    private String password;
    /**
     * 编码格式
     */
    private String charset;
    /**
     * 超时时间
     */
    private Integer timeout;
    /**
     * 设置服务器语言
     */
    private String serverLanguageCode;

    /**
     * 设置服务器系统关键词
     */
    private String systemKey;
    /**
     * 模式
     */
    private FtpMode mode;
    /**
     * ssh连接状态
     * <p>
     * 状态{0，无法连接，1 正常，2 禁用监控}
     */
    private Integer status;
    /**
     * 状态消息
     */
    private String statusMsg;


    public MachineFtpModel(String id) {
        setId(id);
    }

    public FtpConfig toFtpConfig() {
        FtpConfig ftpConfig = new FtpConfig();
        ftpConfig.setHost(host);
        ftpConfig.setPort(port);
        ftpConfig.setUser(user);
        ftpConfig.setPassword(password);
        ftpConfig.setCharset(CharsetUtil.parse(charset));
        if (timeout != null) {
            ftpConfig.setSoTimeout(timeout * 1000);
            ftpConfig.setConnectionTimeout(timeout * 1000);
        }
        ftpConfig.setSystemKey(systemKey);
        ftpConfig.setServerLanguageCode(serverLanguageCode);
        return ftpConfig;
    }
}
