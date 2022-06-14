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
package io.jpom.permission;

import io.jpom.service.dblog.*;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.service.docker.DockerSwarmInfoService;
import io.jpom.service.h2db.BaseDbService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.monitor.MonitorUserOptService;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.ProjectInfoCacheService;
import io.jpom.service.node.command.CommandExecLogService;
import io.jpom.service.node.command.CommandService;
import io.jpom.service.node.script.NodeScriptExecuteLogServer;
import io.jpom.service.node.script.NodeScriptServer;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.outgiving.DbOutGivingLogService;
import io.jpom.service.outgiving.LogReadServer;
import io.jpom.service.outgiving.OutGivingServer;
import io.jpom.service.script.ScriptExecuteLogServer;
import io.jpom.service.script.ScriptServer;
import io.jpom.service.system.WorkspaceService;
import io.jpom.service.user.UserService;
import lombok.Getter;

/**
 * 功能模块
 *
 * @author bwcx_jzy
 * @since 2019/8/13
 */
@Getter
public enum ClassFeature {
    /**
     * 没有
     */
    NULL("", null, null),
    NODE("节点管理", NodeService.class),
    NODE_STAT("节点统计", NodeService.class),
    UPGRADE_NODE_LIST("节点升级", NodeService.class),
    SEARCH_PROJECT("搜索项目", ProjectInfoCacheService.class),
    SSH("SSH管理", SshService.class),
    SSH_FILE("SSH文件管理", SshService.class),
    SSH_TERMINAL("SSH终端", SshService.class),
    SSH_TERMINAL_LOG("SSH终端日志", SshTerminalExecuteLogService.class),
    SSH_COMMAND("SSH命令管理", CommandService.class),
    SSH_COMMAND_LOG("SSH命令日志", CommandExecLogService.class),
    OUTGIVING("分发管理", OutGivingServer.class),
    LOG_READ("日志阅读", LogReadServer.class),
    OUTGIVING_LOG("分发日志", DbOutGivingLogService.class),
    OUTGIVING_CONFIG_WHITELIST("分发白名单配置"),
    MONITOR("项目监控", MonitorService.class),
    MONITOR_LOG("监控日志", DbMonitorNotifyLogService.class),
    OPT_MONITOR("操作监控", MonitorUserOptService.class),
    DOCKER("Docker管理", DockerInfoService.class),
    DOCKER_SWARM("容器集群", DockerSwarmInfoService.class),
    /**
     * ssh
     */
    BUILD("在线构建", BuildInfoService.class),
    BUILD_LOG("构建日志", DbBuildHistoryLogService.class),
    BUILD_REPOSITORY("仓库信息", RepositoryService.class),
    USER("用户管理", UserService.class),
    USER_LOG("操作日志", DbUserOperateLogService.class),
    SYSTEM_EMAIL("邮箱配置"),
    SYSTEM_CACHE("系统缓存"),
    SYSTEM_LOG("系统日志"),
    SYSTEM_UPGRADE("在线升级"),
    SYSTEM_CONFIG("服务端系统配置"),
    SYSTEM_CONFIG_IP("系统配置IP白名单"),
    SYSTEM_CONFIG_MENUS("系统菜单配置"),
    SYSTEM_NODE_WHITELIST("节点白名单分发"),
    SYSTEM_BACKUP("数据库备份", BackupInfoService.class),
    SYSTEM_WORKSPACE("工作空间", WorkspaceService.class),

    SCRIPT("脚本模板", ScriptServer.class),
    SCRIPT_LOG("脚本模板日志", ScriptExecuteLogServer.class),

    //******************************************     节点管理功能
    PROJECT("项目管理", ClassFeature.NODE, ProjectInfoCacheService.class),
    PROJECT_FILE("项目文件管理", ClassFeature.NODE, ProjectInfoCacheService.class),
    PROJECT_LOG("项目日志", ClassFeature.NODE, ProjectInfoCacheService.class),
    PROJECT_CONSOLE("项目控制台", ClassFeature.NODE, ProjectInfoCacheService.class),
    JDK_LIST("JDK管理", ClassFeature.NODE),
    NODE_SCRIPT("节点脚本模板", ClassFeature.NODE, NodeScriptServer.class),
    NODE_SCRIPT_LOG("节点脚本模板日志", ClassFeature.NODE, NodeScriptExecuteLogServer.class),
    TOMCAT("Tomcat", ClassFeature.NODE),
    TOMCAT_FILE("Tomcat file", ClassFeature.NODE),
    TOMCAT_LOG("Tomcat log", ClassFeature.NODE),

    NGINX("Nginx", ClassFeature.NODE),
    SSL("ssl证书", ClassFeature.NODE),
    NODE_CONFIG_WHITELIST("节点白名单配置", ClassFeature.NODE),
    NODE_CONFIG("节点白名单配置", ClassFeature.NODE),
    NODE_CACHE("节点缓存", ClassFeature.NODE),
    NODE_LOG("节点系统日志", ClassFeature.NODE),
    NODE_UPGRADE("节点在线升级", ClassFeature.NODE),


//	PROJECT_RECOVER("项目回收", ClassFeature.NODE),

    ;

    private final String name;
    private final ClassFeature parent;
    private final Class<? extends BaseDbService<?>> dbService;

    ClassFeature(String name) {
        this(name, null, null);
    }

    ClassFeature(String name, ClassFeature parent) {
        this(name, parent, null);
    }


    ClassFeature(String name, Class<? extends BaseDbService<?>> dbService) {
        this(name, null, dbService);
    }

    ClassFeature(String name, ClassFeature parent, Class<? extends BaseDbService<?>> dbService) {
        this.name = name;
        this.parent = parent;
        this.dbService = dbService;
    }
}
