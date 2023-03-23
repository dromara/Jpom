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
package io.jpom.func.assets.model;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import io.jpom.JpomApplication;
import io.jpom.model.BaseGroupNameModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;
import top.jpom.h2db.TableName;

import java.io.File;

/**
 * @author bwcx_jzy
 * @see io.jpom.model.docker.DockerInfoModel
 * @since 2023/3/3
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MACHINE_DOCKER_INFO", name = "机器DOCKER信息")
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
