/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.docker;

import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.model.docker.DockerSwarmInfoMode;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @since 2022/2/13
 */
@Service
@Slf4j
public class DockerSwarmInfoService extends BaseWorkspaceService<DockerSwarmInfoMode> {

    public static final String DOCKER_PLUGIN_NAME = "docker-cli:swarm";
}
