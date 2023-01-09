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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.model.AgentFileModel;
import io.jpom.model.user.UserModel;
import io.jpom.socket.ServiceFileTailWatcher;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 2022/12/17
 */
@EqualsAndHashCode(callSuper = true)
@Configuration
@ConfigurationProperties("jpom")
@Data
public class ServerConfig extends BaseExtConfig {

    private final JpomApplication configBean;

    public ServerConfig(JpomApplication configBean) {
        this.configBean = configBean;
    }

    /**
     * 系统配置参数
     */
    private SystemConfig system;

    public SystemConfig getSystem() {
        return Optional.ofNullable(this.system).orElseGet(() -> {
            this.system = new SystemConfig();
            return this.system;
        });
    }

    /**
     * 节点配置
     */
    private NodeConfig node;

    public NodeConfig getNode() {
        return Optional.ofNullable(this.node).orElseGet(() -> {
            this.node = new NodeConfig();
            return this.node;
        });
    }

    /**
     * 用户相关配置
     */
    private UserConfig user;

    public UserConfig getUser() {
        return Optional.ofNullable(this.user).orElseGet(() -> {
            this.user = new UserConfig();
            return this.user;
        });
    }

    /**
     * 前端配置
     */
    private WebConfig web;

    public WebConfig getWeb() {
        return Optional.ofNullable(this.web).orElseGet(() -> {
            this.web = new WebConfig();
            return this.web;
        });
    }

    /**
     * 获取当前登录用户的临时文件存储路径，如果没有登录则抛出异常
     *
     * @return file
     */
    public File getUserTempPath() {
        File file = configBean.getTempPath();
        UserModel userModel = BaseServerController.getUserModel();
        if (userModel == null) {
            throw new JpomRuntimeException("没有登录");
        }
        file = FileUtil.file(file, userModel.getId());
        FileUtil.mkdir(file);
        return file;
    }


    /**
     * 获取保存 agent jar 包目录文件夹
     *
     * @return 数据目录下的 agent 目录
     */
    public File getAgentPath() {
        File file = new File(configBean.getDataPath());
        file = new File(file.getPath() + "/agent/");
        FileUtil.mkdir(file);
        return file;
    }

    /**
     * 获取保存 agent zip 包目录和文件名
     *
     * @return 数据目录下的 agent 目录
     */
    public File getAgentZipPath() {
        File file = FileUtil.file(getAgentPath(), AgentFileModel.ZIP_NAME);
        FileUtil.mkParentDirs(file);
        return file;
    }

    @Data
    public static class WebConfig {

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
            return StrUtil.emptyToDefault(name, "Jpom项目管理系统");
        }

        public String getSubTitle() {
            return StrUtil.emptyToDefault(subTitle, "项目管理");
        }

        public String getLoginTitle() {
            return StrUtil.emptyToDefault(loginTitle, "登录JPOM");
        }

    }

    @Data
    public static class UserConfig {

        /**
         * 用户连续登录失败次数，超过此数将自动不再被允许登录，零是不限制
         */
        private int alwaysLoginError = 5;

        /**
         * IP连续登录失败次数，超过此数将自动不再被允许登录，零是不限制
         */
        private int alwaysIpLoginError = 10;

        /**
         * 是否强制提醒用户开启  mfa
         */
        private boolean forceMfa = false;
        /**
         * 当ip连续登录失败，锁定对应IP时长，单位毫秒
         */
        private Duration ipErrorLockTime;

        public Duration getIpErrorLockTime() {
            return Optional.ofNullable(this.ipErrorLockTime).orElseGet(() -> {
                ipErrorLockTime = Duration.ofHours(5);
                return ipErrorLockTime;
            });
        }

        /**
         * demo 账号的提示
         */
        private String demoTip;


        /**
         * 登录token失效时间(单位：小时),默认为24
         */
        private int tokenExpired = 24;

        public int getTokenExpired() {
            return Math.max(this.tokenExpired, 1);
        }

        /**
         * 登录token失效后自动续签时间（单位：分钟），默认为60，
         */
        private int tokenRenewal = 60;

        public int getTokenRenewal() {
            return Math.max(this.tokenRenewal, 1);
        }

        /**
         * 登录token 加密的key 长度建议控制到 16位
         */
        private String tokenJwtKey;

        public byte[] getTokenJwtKeyByte() {
            return StrUtil.emptyToDefault(this.tokenJwtKey, "KZQfFBJTW2v6obS1").getBytes();
        }
    }


    @Data
    public static class NodeConfig {
        /**
         * 检查节点心跳间隔时间,最小值 5 秒
         */
        private int heartSecond = 30;

        public int getHeartSecond() {
            return Math.max(this.heartSecond, 5);
        }

        /**
         * 上传文件的超时时间 单位秒,最短5秒中
         */
        private int uploadFileTimeout = 300;

        /**
         * 节点文件分片上传大小，单位 M
         */
        private int uploadFileSliceSize = 1;

        /**
         * 节点文件分片上传并发数,最小1 最大 服务端 CPU 核心数
         */
        private int uploadFileConcurrent = 2;

        public int getUploadFileTimeout() {
            return Math.max(this.uploadFileTimeout, 5);
        }

        public int getUploadFileSliceSize() {
            return Math.max(this.uploadFileSliceSize, 1);
        }

        public void setUploadFileConcurrent(int uploadFileConcurrent) {
            this.uploadFileConcurrent = Math.min(Math.max(uploadFileConcurrent, 1), RuntimeUtil.getProcessorCount());
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class SystemConfig extends BaseSystemConfig {

        @Override
        public void setLogCharset(Charset logCharset) {
            super.setLogCharset(logCharset);
            ServiceFileTailWatcher.setCharset(getLogCharset());
        }
    }
}
