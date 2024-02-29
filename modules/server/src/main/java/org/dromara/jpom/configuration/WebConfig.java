/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.configuration;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author bwcx_jzy
 * @since 23/12/25 025
 */
@Data
@ConfigurationProperties("jpom.web")
public class WebConfig {
    /**
     * 前端接口 超时时间 单位秒
     */
    private int apiTimeout = 20;

    public int getApiTimeout() {
        return Math.max(this.apiTimeout, 5);
    }

    /**
     * 系统名称
     */
    private String name;

    /**
     * 系统副名称（标题） 建议4个汉字以内
     */
    private String subTitle;

    /**
     * 登录页标题
     */
    private String loginTitle;

    /**
     * logo 文件路径
     */
    private String logoFile;

    /**
     * icon 文件路径
     */
    private String iconFile;

    /**
     * 禁用页面引导导航
     */
    private boolean disabledGuide = false;
    /**
     * 禁用登录图形验证码
     */
    private boolean disabledCaptcha = false;

    /**
     * 前端消息弹出位置，可选 topLeft topRight bottomLeft bottomRight
     */
    private String notificationPlacement;
    /**
     * 消息传输加密或者编码
     * NONE
     * <p>
     * BASE64
     */
    private String transportEncryption = "NONE";

    public String getName() {
        return StrUtil.emptyToDefault(name, "Jpom项目运维系统");
    }

    public String getSubTitle() {
        return StrUtil.emptyToDefault(subTitle, "项目运维");
    }

    public String getLoginTitle() {
        return StrUtil.emptyToDefault(loginTitle, "登录JPOM");
    }
}
