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

import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.BaseFileTailWatcher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Optional;

/**
 * 插件端配置信息
 *
 * @author bwcx_jzy
 * @since 2022/12/16
 */

@Configuration
@ConfigurationProperties("jpom")
@Data
@EnableConfigurationProperties({ProjectConfig.class, ProjectLogConfig.class, SystemConfig.class, AgentAuthorize.class})
public class AgentConfig implements ILoadEvent, InitializingBean {

    private final JpomApplication jpomApplication;

    public AgentConfig(JpomApplication jpomApplication) {
        this.jpomApplication = jpomApplication;
    }

    /**
     * 授权配置
     */
    private AgentAuthorize authorize;

    /**
     * 项目配置
     */
    private ProjectConfig project;
    /**
     * 系统配置参数
     */
    private SystemConfig system;
    /**
     * 数据目录
     */
    private String path;

    /**
     * 初始读取日志文件行号
     */
    private int initReadLine = 10;

    public AgentAuthorize getAuthorize() {
        return Optional.ofNullable(this.authorize).orElseGet(() -> {
            this.authorize = new AgentAuthorize();
            return this.authorize;
        });
    }

    public ProjectConfig getProject() {
        return Optional.ofNullable(this.project).orElseGet(() -> {
            this.project = new ProjectConfig();
            return this.project;
        });
    }

    public SystemConfig getSystem() {
        return Optional.ofNullable(this.system).orElseGet(() -> {
            this.system = new SystemConfig();
            return this.system;
        });
    }

    /**
     * 获取临时文件存储路径，并添加一个随机字符串
     *
     * @return 文件夹
     */
    public String getTempPathName() {
        File file = getTempPath();
        // 生成随机的一个文件夹、避免同一个节点分发同一个文件，mv 失败
        return FileUtil.getAbsolutePath(FileUtil.file(file, IdUtil.fastSimpleUUID()));
    }

    /**
     * 获取临时文件存储路径
     *
     * @return 文件夹
     */
    public String getFixedTempPathName() {
        File file = getTempPath();
        return FileUtil.getAbsolutePath(file);
    }


    /**
     * 获取临时文件存储路径
     *
     * @return file
     */
    public File getTempPath() {
        File file = jpomApplication.getTempPath();
        file = FileUtil.file(file, DateTime.now().toDateStr());
        FileUtil.mkdir(file);
        return file;
    }


    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        // 登录名不能为空
        this.getAuthorize().init(jpomApplication);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        int initReadLine = ObjectUtil.defaultIfNull(this.initReadLine, 10);
        BaseFileTailWatcher.setInitReadLine(initReadLine);
        ExtConfigBean.setPath(path);
    }
}
