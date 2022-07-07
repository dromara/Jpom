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

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.ServerOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * agent 端外部配置
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@Configuration
public class AgentExtConfigBean {

    private static AgentExtConfigBean agentExtConfigBean;
    /**
     * 白名单路径是否判断包含关系
     */
    @Value("${whitelistDirectory.checkStartsWith:true}")
    public boolean whitelistDirectoryCheckStartsWith;

    /**
     * 检测控制台日志周期，防止日志文件过大，目前暂只支持linux 不停服备份
     */
    @Value("${log.autoBackConsoleCron:0 0/10 * * * ?}")
    public String autoBackConsoleCron;
    /**
     * 当文件多大时自动备份
     *
     * @see ch.qos.logback.core.util.FileSize
     */
    @Value("${log.autoBackSize:50MB}")
    public String autoBackSize;
    /**
     * 是否自动将控制台日志文件备份
     */
    @Value("${log.autoBackToFile:true}")
    public Boolean autoBackToFile;

    /**
     * 控制台日志保存时长单位天
     */
    @Value("${log.saveDays:7}")
    private int logSaveDays;

    @Value("${jpom.agent.id:}")
    private String agentId;

    @Value("${jpom.agent.url:}")
    private String agentUrl;

    @Value("${jpom.server.url:}")
    private String serverUrl;

    @Value("${jpom.server.token:}")
    private String serverToken;

    /**
     * 停止项目等待的时长 单位秒，最小为1秒
     */
    @Value("${project.stopWaitTime:10}")
    private int stopWaitTime;

    /**
     * 项目文件备份保留个数,大于 1 才会备份
     */
    @Value("${project.fileBackupCount:0}")
    private Integer projectFileBackupCount;

    /**
     *
     */
    @Value("${project.fileBackupSuffix:}")
    private String[] projectFileBackupSuffix;

    public int getStopWaitTime() {
        return stopWaitTime;
    }

    public int getProjectFileBackupCount() {
        return ObjectUtil.defaultIfNull(projectFileBackupCount, 0);
    }

    public String[] getProjectFileBackupSuffix() {
        return projectFileBackupSuffix;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getServerToken() {
        return serverToken;
    }

    /**
     * 获取当前的url
     *
     * @return 如果没有配置将自动生成：http://+本地IP+端口
     */
    public String getAgentUrl() {
        if (StrUtil.isEmpty(agentUrl)) {
            String localhostStr = NetUtil.getLocalhostStr();
            int port = ConfigBean.getInstance().getPort();
            agentUrl = String.format("http://%s:%s", localhostStr, port);
        }
        if (StrUtil.isEmpty(agentUrl)) {
            throw new JpomRuntimeException("获取Agent url失败");
        }
        return agentUrl;
    }

    /**
     * 创建请求对象
     *
     * @param openApi url
     * @return HttpRequest
     * @see ServerOpenApi
     */
    public HttpRequest createServerRequest(String openApi) {
        if (StrUtil.isEmpty(getServerUrl())) {
            throw new JpomRuntimeException("请先配置server端url");
        }
        if (StrUtil.isEmpty(getServerToken())) {
            throw new JpomRuntimeException("请先配置server端Token");
        }
        // 加密
        String md5 = SecureUtil.md5(getServerToken());
        md5 = SecureUtil.sha1(md5 + ServerOpenApi.HEAD);
        HttpRequest httpRequest = HttpUtil.createPost(String.format("%s%s", serverUrl, openApi));
        httpRequest.header(ServerOpenApi.HEAD, md5);
        return httpRequest;
    }

    /**
     * 配置错误或者没有，默认是7天
     *
     * @return int
     */
    public int getLogSaveDays() {
        if (logSaveDays <= 0) {
            return 7;
        }
        return logSaveDays;
    }

    /**
     * 是否开启日志备份
     *
     * @return 如果表达式配置为none 则不配置，重启也不备份
     */
    public boolean openLogBack() {
        return Optional.ofNullable(autoBackToFile).orElse(true);
//        if(autoBackToFile==null || autoBackToFile){
//            return
//        }
//        String cron = StrUtil.emptyToDefault(autoBackConsoleCron, "none");
//        return !"none".equalsIgnoreCase(cron.trim());
    }

    /**
     * 单例
     *
     * @return this
     */
    public static AgentExtConfigBean getInstance() {
        if (agentExtConfigBean == null) {
            agentExtConfigBean = SpringUtil.getBean(AgentExtConfigBean.class);
        }
        return agentExtConfigBean;
    }
}
