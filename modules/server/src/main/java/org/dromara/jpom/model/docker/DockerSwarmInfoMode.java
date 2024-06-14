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
 * @since 2022/2/13
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "DOCKER_SWARM_INFO",
    nameKey = "i18n.docker_cluster_info.a2eb")
public class DockerSwarmInfoMode extends BaseWorkspaceModel {
    /**
     * 集群名称
     */
    private String name;
    /**
     * 集群ID
     */
    private String swarmId;

    /**
     * 集群容器标签
     */
    private String tag;

    @PropIgnore
    private MachineDockerModel machineDocker;

    @PropIgnore
    private WorkspaceModel workspace;
}
