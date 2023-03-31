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
package org.dromara.jpom.model.docker;

import cn.hutool.core.annotation.PropIgnore;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "DOCKER_INFO", name = "docker 信息")
public class DockerInfoModel extends BaseWorkspaceModel {
    /**
     * 名称
     */
    private String name;
    /**
     * 地址
     */
    @Deprecated
    private String host;
    /**
     * 开启 tls 验证
     */
    @Deprecated
    private Boolean tlsVerify;
    /**
     * 证书路径
     */
    @PropIgnore
    private Boolean certExist;
    /**
     * 集群节点ID
     */
    @Deprecated
    private String swarmNodeId;
    /**
     * 最后心跳时间
     */
    @Deprecated
    private Long lastHeartbeatTime;
    /**
     * 超时时间，单位 秒
     */
    @Deprecated
    private Integer heartbeatTimeout;
    /**
     * 标签
     */
    private String tags;
    /**
     * 集群ID
     */
    @Deprecated
    private String swarmId;

    /**
     * 仓库账号
     */
    @Deprecated
    private String registryUsername;

    /**
     * 仓库密码
     */
    @Deprecated
    private String registryPassword;

    /**
     * 仓库邮箱
     */
    @Deprecated
    private String registryEmail;

    /**
     * 仓库地址
     */
    @Deprecated
    private String registryUrl;

    /**
     * 机器 docker id
     */
    private String machineDockerId;

    @PropIgnore
    private MachineDockerModel machineDocker;

    @PropIgnore
    private WorkspaceModel workspace;

}
