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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
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

    /**
     * 停止项目等待的时长 单位秒，最小为1秒
     */
    @Value("${project.stopWaitTime:10}")
    private int stopWaitTime;

    /**
     * 项目状态检测间隔时间 单位毫秒，最小为1毫秒
     */
    @Value("${project.statusDetectionInterval:500}")
    private int statusDetectionInterval;

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

    public int getStatusDetectionInterval() {
        return statusDetectionInterval;
    }

    public int getProjectFileBackupCount() {
        return ObjectUtil.defaultIfNull(projectFileBackupCount, 0);
    }

    public String[] getProjectFileBackupSuffix() {
        return projectFileBackupSuffix;
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
        boolean newConfig = Optional.ofNullable(autoBackToFile).orElse(true);
        if (newConfig) {
            // 使用旧配置
            String cron = StrUtil.emptyToDefault(autoBackConsoleCron, "none");
            return !"none".equalsIgnoreCase(cron.trim());
        }
        return false;
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
