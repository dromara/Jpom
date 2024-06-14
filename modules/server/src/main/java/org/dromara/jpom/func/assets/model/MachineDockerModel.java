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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseGroupNameModel;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.springframework.util.Assert;

import java.io.File;

/**
 * @author bwcx_jzy
 * @see DockerInfoModel
 * @since 2023/3/3
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MACHINE_DOCKER_INFO",
    nameKey = "i18n.machine_docker_info.9914")
@Data
@NoArgsConstructor
public class MachineDockerModel extends BaseGroupNameModel {
    /**
     * 地址
     */
    private String host;
    /**
     * 开启 tls 验证
     */
    private Boolean tlsVerify;
    /**
     * 证书信息
     */
    private String certInfo;

    private Boolean certExist;
    /**
     * 状态 0 , 异常离线 1 正常
     */
    private Integer status;
    /**
     * 错误消息
     */
    private String failureMsg;
    /**
     * docker 版本
     */
    private String dockerVersion;
    /**
     * 最后心跳时间
     */
    private Long lastHeartbeatTime;
    /**
     * 超时时间，单位 秒
     */
    private Integer heartbeatTimeout;
    /**
     * 仓库账号
     */
    private String registryUsername;

    /**
     * 仓库密码
     */
    private String registryPassword;

    /**
     * 仓库邮箱
     */
    private String registryEmail;

    /**
     * 仓库地址
     */
    private String registryUrl;

    /**
     * 集群ID
     */
    private String swarmId;
    /**
     * 集群节点ID
     */
    private String swarmNodeId;
    /**
     * 集群的创建时间
     */
    private Long swarmCreatedAt;
    /**
     * 集群的更新时间
     */
    private Long swarmUpdatedAt;
    /**
     * 节点 地址
     */
    private String swarmNodeAddr;
    /**
     * 集群管理员
     */
    private Boolean swarmControlAvailable;

    /**
     * 开启 SSH 访问
     */
    private Boolean enableSsh;

    /**
     * SSH Id
     */
    private String machineSshId;


    public void setFailureMsg(String failureMsg) {
        this.failureMsg = StrUtil.maxLength(failureMsg, 240);
    }

    public boolean isControlAvailable() {
        return swarmControlAvailable != null && swarmControlAvailable;
    }

    /**
     * 生成证书路径
     *
     * @return path
     */
    @Deprecated
    public String generateCertPath() {
        String dataPath = JpomApplication.getInstance().getDataPath();
        String host = this.getHost();
        Assert.hasText(host, "host empty");
        host = SecureUtil.sha1(host);
        File docker = FileUtil.file(dataPath, "docker", "tls-cert", host);
        return FileUtil.getAbsolutePath(docker);
    }


    public void restSwarm() {
        this.setSwarmId(StrUtil.EMPTY);
        this.setSwarmNodeId(StrUtil.EMPTY);
        this.setSwarmCreatedAt(0L);
        this.setSwarmUpdatedAt(0L);
        this.setSwarmNodeAddr(StrUtil.EMPTY);
        this.setSwarmControlAvailable(false);
    }

}
