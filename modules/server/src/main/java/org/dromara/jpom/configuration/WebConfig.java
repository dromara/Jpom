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
