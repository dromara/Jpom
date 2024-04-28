/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket;

import lombok.Getter;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.node.script.NodeScriptServer;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.socket.handler.*;

/**
 * @author bwcx_jzy
 * @since 2019/8/9
 */
@Getter
public enum HandlerType {
    /**
     * 脚本模板
     */
    nodeScript(NodeScriptHandler.class, NodeScriptServer.class),
    /**
     * 系统日志
     */
    systemLog(SystemLogHandler.class, null),
    /**
     * 插件端日志
     */
    agentLog(AgentLogHandler.class, null),
    /**
     * 项目控制台和首页监控
     */
    console(ConsoleHandler.class, ProjectInfoCacheService.class),
    /**
     * ssh
     */
    ssh(SshHandler.class, SshService.class, MachineSshServer.class, "machineSshId"),
    /**
     * 节点升级
     */
    nodeUpdate(NodeUpdateHandler.class, null),
    /**
     * 服务端 脚本模版
     */
    script(ServerScriptHandler.class, ScriptServer.class),
    /**
     * 容器 log
     */
    dockerLog(DockerLogHandler.class, DockerInfoService.class, MachineDockerServer.class, "machineDockerId"),
    /**
     * 容器 终端
     */
    docker(DockerCliHandler.class, DockerInfoService.class, MachineDockerServer.class, "machineDockerId"),
    freeScript(FreeScriptHandler.class, null),
    ;
    final Class<?> handlerClass;

    final Class<? extends BaseWorkspaceService<?>> serviceClass;
    final Class<? extends BaseDbService<?>> assetsServiceClass;
    /**
     * 资产关联字段
     */
    final String assetsLinkDataId;

    HandlerType(Class<?> handlerClass,
                Class<? extends BaseWorkspaceService<?>> serviceClass) {
        this.handlerClass = handlerClass;
        this.serviceClass = serviceClass;
        this.assetsServiceClass = null;
        this.assetsLinkDataId = null;
    }

    HandlerType(Class<?> handlerClass,
                Class<? extends BaseWorkspaceService<?>> serviceClass,
                Class<? extends BaseDbService<?>> assetsServiceClass,
                String assetsLinkDataId) {
        this.handlerClass = handlerClass;
        this.serviceClass = serviceClass;
        this.assetsServiceClass = assetsServiceClass;
        this.assetsLinkDataId = assetsLinkDataId;
    }
}
