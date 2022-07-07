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
package io.jpom.system;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.script.ScriptUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 外部配置文件
 *
 * @author jiangzeyin
 * @since 2019/3/04
 */
@Configuration
public class ServerExtConfigBean {

    /**
     * 系统最多能创建多少用户
     */
    @Value("${user.maxCount:10}")
    public int userMaxCount;
    /**
     * 用户连续登录失败次数，超过此数将自动不再被允许登录，零是不限制
     */
    @Value("${user.alwaysLoginError:5}")
    public int userAlwaysLoginError;

    /**
     * 当ip连续登录失败，锁定对应IP时长，单位毫秒
     */
    @Value("${user.ipErrorLockTime:60*60*5*1000}")
    private String ipErrorLockTime;
    private long ipErrorLockTimeValue = -1;
    /**
     * demo 账号的提示
     */
    @Value("${user.demoTip:}")
    private String userDemoTip;

    /**
     * author Hotstrip
     * 是否开启 web 访问数据库
     *
     * @see <a href=http://${ip}:${port}/h2-console>http://${ip}:${port}/h2-console</a>
     */
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    /**
     * 服务端api token,长度要求大于等于6位，字母数字符号组合
     */
    @Value("${jpom.authorize.token:}")
    private String authorizeToken;

    /**
     * 登录token失效时间(单位：小时),默认为24
     */
    @Value("${jpom.authorize.expired:24}")
    private int authorizeExpired;

    /**
     * 登录token失效后自动续签时间（单位：分钟），默认为60，
     */
    @Value("${jpom.authorize.renewal:60}")
    private int authorizeRenewal;

    /**
     * 登录token 加密的key 长度建议控制到 16位
     */
    @Value("${jpom.authorize.key:}")
    private String authorizeKey;

    /**
     * ssh 中执行命令 初始化的环境变量
     */
    @Value("${ssh.initEnv:}")
    private String sshInitEnv;

    /**
     * 上传文件的超时时间 单位秒,最短5秒中
     */
    @Value("${node.uploadFileTimeOut:300}")
    private int uploadFileTimeOut;

    /**
     * 前端接口 超时时间 单位秒
     */
    @Value("${jpom.webApiTimeout:20}")
    private int webApiTimeout;

    /**
     * 系统名称
     */
    @Value("${jpom.name:}")
    private String name;

    /**
     * 系统副名称（标题） 建议4个汉字以内
     */
    @Value("${jpom.subTitle:}")
    private String subTitle;

    /**
     * 登录页标题
     */
    @Value("${jpom.loginTitle:}")
    private String loginTitle;

    /**
     * logo 文件路径
     */
    @Value("${jpom.logoFile:}")
    private String logoFile;

    /**
     * icon 文件路径
     */
    @Value("${jpom.iconFile:}")
    private String iconFile;

    /**
     * 禁用页面引导导航
     */
    @Value("${jpom.disabledGuide:false}")
    private Boolean disabledGuide;
    /**
     * 禁用登录图形验证码
     */
    @Value("${jpom.disabledCaptcha:false}")
    private Boolean disabledCaptcha;

    /**
     * 前端消息弹出位置，可选 topLeft topRight bottomLeft bottomRight
     */
    @Value("${jpom.notificationPlacement:}")
    private String notificationPlacement;

    /**
     * 检查节点心跳间隔时间
     */
    @Value("${system.nodeHeartSecond:30}")
    private Integer nodeHeartSecond;

    /**
     * 获取上传文件超时时间
     *
     * @return 返回毫秒
     */
    public int getUploadFileTimeOut() {
        return Math.max(this.uploadFileTimeOut, 5) * 1000;
    }

    public String getSshInitEnv() {
        return StrUtil.emptyToDefault(this.sshInitEnv, "source /etc/profile && source ~/.bash_profile && source ~/.bashrc");
    }

    public String getAuthorizeToken() {
        return authorizeToken;
    }

    public long getIpErrorLockTime() {
        if (this.ipErrorLockTimeValue == -1) {
            String str = StrUtil.emptyToDefault(this.ipErrorLockTime, "60*60*5*1000");
            this.ipErrorLockTimeValue = Convert.toLong(ScriptUtil.eval(str), TimeUnit.HOURS.toMillis(5));
        }
        return this.ipErrorLockTimeValue;
    }

    public int getAuthorizeExpired() {
        return authorizeExpired;
    }

    public int getAuthorizeRenewal() {
        return authorizeRenewal;
    }

    public boolean isH2ConsoleEnabled() {
        return h2ConsoleEnabled;
    }

    public byte[] getAuthorizeKey() {
        return StrUtil.emptyToDefault(this.authorizeKey, "KZQfFBJTW2v6obS1").getBytes();
    }

    /**
     * 最小值 10秒
     *
     * @return 超时时间（单位秒）
     */
    public int getWebApiTimeout() {
        return Math.max(this.webApiTimeout, 10);
    }

    public String getName() {
        return StrUtil.emptyToDefault(name, "Jpom项目管理系统");
    }

    public String getSubTitle() {
        return StrUtil.emptyToDefault(subTitle, "项目管理");
    }

    public String getLoginTitle() {
        return StrUtil.emptyToDefault(loginTitle, "登录JPOM");
    }

    public String getLogoFile() {
        return logoFile;
    }

    public String getIconFile() {
        return iconFile;
    }

    public String getUserDemoTip() {
        return userDemoTip;
    }

    /**
     * 是否禁用导航
     *
     * @return bool
     */
    public boolean getDisabledGuide() {
        return Convert.toBool(disabledGuide, false);
    }

    /**
     * 是否禁用登录图形验证码
     *
     * @return boolean
     */
    public boolean getDisabledCaptcha() {
        return Convert.toBool(disabledCaptcha, false);
    }

    public int getNodeHeartSecond() {
        int integer = ObjectUtil.defaultIfNull(nodeHeartSecond, 30);
        return Math.max(integer, 5);
    }

    public String getNotificationPlacement() {
        return notificationPlacement;
    }

    /**
     * 单例
     *
     * @return this
     */
    public static ServerExtConfigBean getInstance() {
        return SpringUtil.getBean(ServerExtConfigBean.class);
    }
}
