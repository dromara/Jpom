/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import lombok.Data;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.*;
import org.dromara.jpom.model.AgentFileModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.util.BaseFileTailWatcher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.io.File;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 2022/12/17
 */
@Configuration
@ConfigurationProperties("jpom")
@EnableConfigurationProperties({
    ClusterConfig.class,
    SystemConfig.class,
    NodeConfig.class,
    UserConfig.class,
    FileStorageConfig.class,
    WebConfig.class})
@Data
public class ServerConfig implements InitializingBean {

    private final JpomApplication configBean;

    public ServerConfig(JpomApplication configBean) {
        this.configBean = configBean;
    }

    /**
     * 数据目录
     */
    private String path;

    /**
     * 初始读取日志文件行号
     */
    private Integer initReadLine = 10;
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
            throw new JpomRuntimeException(I18nMessageUtil.get("i18n.not_logged_in.6605"));
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
            throw new JpomRuntimeException(I18nMessageUtil.get("i18n.configure_correct_cluster_id.5a78"));
        }

        int initReadLine = ObjectUtil.defaultIfNull(this.initReadLine, 10);
        BaseFileTailWatcher.setInitReadLine(initReadLine);
        ExtConfigBean.setPath(path);
        //
        NodeConfig nodeConfig = getNode();
        DataSize messageSizeLimit = nodeConfig.getWebSocketMessageSizeLimit();
        messageSizeLimit = ObjectUtil.defaultIfNull(messageSizeLimit, DataSize.ofMegabytes(5));
        SystemUtil.set("JPOM_NODE_WEB_SOCKET_MESSAGE_SIZE_LIMIT", messageSizeLimit.toBytes() + "");
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
