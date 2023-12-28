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
package org.dromara.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.configuration.*;
import org.dromara.jpom.model.AgentFileModel;
import org.dromara.jpom.model.user.UserModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 2022/12/17
 */
@EqualsAndHashCode(callSuper = true)
@Configuration
@ConfigurationProperties("jpom")
@EnableConfigurationProperties({ClusterConfig.class, SystemConfig.class, NodeConfig.class, UserConfig.class, FileStorageConfig.class, WebConfig.class})
@Data
public class ServerConfig extends BaseExtConfig implements InitializingBean {

    private final JpomApplication configBean;

    public ServerConfig(JpomApplication configBean) {
        this.configBean = configBean;
    }

    /**
     * 集群 配置信息
     */
    private ClusterConfig cluster;
    /**
     * 系统配置参数
     */
    private SystemConfig system;
    /**
     * 节点配置
     */
    private NodeConfig node;
    /**
     * 用户相关配置
     */
    private UserConfig user;
    /**
     * 前端配置
     */
    private WebConfig web;
    /**
     * 文件中心配置
     */
    private FileStorageConfig fileStorage = new FileStorageConfig();

    public SystemConfig getSystem() {
        return Optional.ofNullable(this.system).orElseGet(() -> {
            this.system = new SystemConfig();
            return this.system;
        });
    }


    public NodeConfig getNode() {
        return Optional.ofNullable(this.node).orElseGet(() -> {
            this.node = new NodeConfig();
            return this.node;
        });
    }


    public UserConfig getUser() {
        return Optional.ofNullable(this.user).orElseGet(() -> {
            this.user = new UserConfig();
            return this.user;
        });
    }


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
        UserModel userModel = BaseServerController.getUserModel();
        if (userModel == null) {
            throw new JpomRuntimeException("没有登录");
        }
        return getUserTempPath(userModel.getId());
    }

    /**
     * 获取指定用户操作的临时目录
     *
     * @return file
     */
    public File getUserTempPath(String userId) {
        File file = configBean.getTempPath();
        file = FileUtil.file(file, userId);
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

    /**
     * 获取当前集群 Id
     *
     * @return 集群Id
     */
    public ClusterConfig getCluster() {
        return Optional.ofNullable(this.cluster).orElseGet(() -> {
            this.cluster = new ClusterConfig();
            return this.cluster;
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ClusterConfig clusterConfig = this.getCluster();
        String clusterId1 = clusterConfig.getId();
        if (!Validator.isGeneral(clusterId1, 1, 20)) {
            throw new JpomRuntimeException("请配置正确的集群Id,【jpom.clusterId】");
        }
    }


    /**
     * 获取文件中心存储目录
     *
     * @return path
     */
    public File fileStorageSavePath() {
        String savePah = fileStorage.getSavePah();
        if (StrUtil.isEmpty(savePah)) {
            String dataPath = configBean.getDataPath();
            fileStorage.setSavePah(FileUtil.getAbsolutePath(FileUtil.file(dataPath, "file-storage")));
        }
        return FileUtil.file(fileStorage.getSavePah());
    }
}
