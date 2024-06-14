/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.docker;

import cn.hutool.core.annotation.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.data.WorkspaceModel;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "DOCKER_INFO",
    nameKey = "i18n.docker_info.00d2")
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
