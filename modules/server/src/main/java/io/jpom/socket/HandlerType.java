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
package io.jpom.socket;

import io.jpom.func.assets.server.MachineDockerServer;
import io.jpom.func.assets.server.MachineSshServer;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.service.h2db.BaseDbService;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.service.node.ProjectInfoCacheService;
import io.jpom.service.node.script.NodeScriptServer;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.script.ScriptServer;
import io.jpom.socket.handler.*;
import lombok.Getter;

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
     * tomcat
     */
    tomcat(TomcatHandler.class, null),
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
