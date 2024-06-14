/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.permission;

import lombok.Getter;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.func.assets.server.ScriptLibraryServer;
import org.dromara.jpom.func.cert.service.CertificateInfoService;
import org.dromara.jpom.func.files.service.FileReleaseTaskService;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.func.files.service.StaticFileStorageService;
import org.dromara.jpom.func.system.service.ClusterInfoService;
import org.dromara.jpom.func.user.server.UserLoginLogServer;
import org.dromara.jpom.service.dblog.*;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.docker.DockerSwarmInfoService;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.dromara.jpom.service.monitor.MonitorService;
import org.dromara.jpom.service.monitor.MonitorUserOptService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.node.script.NodeScriptExecuteLogServer;
import org.dromara.jpom.service.node.script.NodeScriptServer;
import org.dromara.jpom.service.node.ssh.CommandExecLogService;
import org.dromara.jpom.service.node.ssh.SshCommandService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.service.outgiving.DbOutGivingLogService;
import org.dromara.jpom.service.outgiving.LogReadServer;
import org.dromara.jpom.service.outgiving.OutGivingServer;
import org.dromara.jpom.service.script.ScriptExecuteLogServer;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.service.system.WorkspaceEnvVarService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.dromara.jpom.service.user.UserPermissionGroupServer;
import org.dromara.jpom.service.user.UserService;

import java.util.function.Supplier;

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
    NULL(() -> "", null, null),
    NODE(() -> I18nMessageUtil.get("i18n.node_management.b26d"), NodeService.class),
    NODE_STAT(() -> I18nMessageUtil.get("i18n.node_statistics.b4e1"), NodeService.class),
    UPGRADE_NODE_LIST(() -> I18nMessageUtil.get("i18n.node_upgrade.3bf3"), NodeService.class),
    SEARCH_PROJECT(() -> I18nMessageUtil.get("i18n.search_project.7e9b"), ProjectInfoCacheService.class),
    SSH(() -> I18nMessageUtil.get("i18n.ssh_management.9e0f"), SshService.class),
    SSH_FILE(() -> I18nMessageUtil.get("i18n.ssh_file_manager.1482"), SshService.class),
    SSH_TERMINAL(() -> I18nMessageUtil.get("i18n.ssh_terminal.ec50"), SshService.class),
    SSH_TERMINAL_LOG(() -> I18nMessageUtil.get("i18n.ssh_terminal_log.775f"), SshTerminalExecuteLogService.class),
    SSH_COMMAND(() -> I18nMessageUtil.get("i18n.ssh_command_management.c40a"), SshCommandService.class),
    SSH_COMMAND_LOG(() -> I18nMessageUtil.get("i18n.ssh_command_log.7fd1"), CommandExecLogService.class),
    OUTGIVING(() -> I18nMessageUtil.get("i18n.distribute_management.3a2d"), OutGivingServer.class),
    LOG_READ(() -> I18nMessageUtil.get("i18n.log_reading.a4c8"), LogReadServer.class),
    OUTGIVING_LOG(() -> I18nMessageUtil.get("i18n.distribute_log.c612"), DbOutGivingLogService.class),
    OUTGIVING_CONFIG_WHITELIST(() -> I18nMessageUtil.get("i18n.auth_config.3d48")),
    MONITOR(() -> I18nMessageUtil.get("i18n.project_monitor.d2ff"), MonitorService.class),
    MONITOR_LOG(() -> I18nMessageUtil.get("i18n.monitoring_logs.2217"), DbMonitorNotifyLogService.class),
    OPT_MONITOR(() -> I18nMessageUtil.get("i18n.operation_monitoring.0cd5"), MonitorUserOptService.class),
    DOCKER(() -> I18nMessageUtil.get("i18n.docker_management.e7e5"), DockerInfoService.class),
    DOCKER_SWARM(() -> I18nMessageUtil.get("i18n.container_cluster.a5b4"), DockerSwarmInfoService.class),
    /**
     * ssh
     */
    BUILD(() -> I18nMessageUtil.get("i18n.online_build.6f7a"), BuildInfoService.class),
    BUILD_LOG(() -> I18nMessageUtil.get("i18n.build_log.7c0e"), DbBuildHistoryLogService.class),
    BUILD_REPOSITORY(() -> I18nMessageUtil.get("i18n.repository_info.22cd"), RepositoryService.class),
    USER(() -> I18nMessageUtil.get("i18n.user_management.7d94"), UserService.class),
    USER_LOG(() -> I18nMessageUtil.get("i18n.operation_log.cda8"), DbUserOperateLogService.class),
    USER_LOGIN_LOG(() -> I18nMessageUtil.get("i18n.login_log.3fb2"), UserLoginLogServer.class),
    FILE_STORAGE(() -> I18nMessageUtil.get("i18n.file_storage_center.6acf"), FileStorageService.class),
    STATIC_FILE_STORAGE(() -> I18nMessageUtil.get("i18n.static_file_storage.35f6"), StaticFileStorageService.class),
    FILE_STORAGE_RELEASE(() -> I18nMessageUtil.get("i18n.file_published.d1d9"), FileReleaseTaskService.class),
    CERTIFICATE_INFO(() -> I18nMessageUtil.get("i18n.certificate_management.4001"), CertificateInfoService.class),
    USER_PERMISSION_GROUP(() -> I18nMessageUtil.get("i18n.permission_group.ea59"), UserPermissionGroupServer.class),
    SYSTEM_EMAIL(() -> I18nMessageUtil.get("i18n.email_configuration.b3f7")),
    OAUTH_CONFIG(() -> I18nMessageUtil.get("i18n.authentication_config.964c")),
    SYSTEM_CACHE(() -> I18nMessageUtil.get("i18n.system_cache.c4a8")),
    SYSTEM_LOG(() -> I18nMessageUtil.get("i18n.system_logs.84aa")),
    SYSTEM_UPGRADE(() -> I18nMessageUtil.get("i18n.online_upgrade.da8c")),
    SYSTEM_ASSETS_MACHINE(() -> I18nMessageUtil.get("i18n.machine_asset_management.36ea"), MachineNodeServer.class),
    SYSTEM_ASSETS_MACHINE_SSH(() -> I18nMessageUtil.get("i18n.ssh_asset_management.3b6c"), MachineSshServer.class),
    SYSTEM_ASSETS_MACHINE_DOCKER(() -> I18nMessageUtil.get("i18n.docker_asset_management.96d9"), MachineDockerServer.class),
    SYSTEM_ASSETS_GLOBAL_SCRIPT(() -> I18nMessageUtil.get("i18n.script_library.aed1"), ScriptLibraryServer.class),
    SYSTEM_CONFIG(() -> I18nMessageUtil.get("i18n.server_system_config.3181")),
    SYSTEM_EXT_CONFIG(() -> I18nMessageUtil.get("i18n.system_configuration_directory.0f82")),
    SYSTEM_CONFIG_IP(() -> I18nMessageUtil.get("i18n.system_IP_authorization.9c08")),
    //    SYSTEM_CONFIG_MENUS("系统菜单配置"),
    SYSTEM_NODE_WHITELIST(() -> I18nMessageUtil.get("i18n.node_authorized_distribution.c5d7")),
    SYSTEM_BACKUP(() -> I18nMessageUtil.get("i18n.database_backup_label.62d8"), BackupInfoService.class),
    SYSTEM_WORKSPACE(() -> I18nMessageUtil.get("i18n.workspace_label.98d6"), WorkspaceService.class),
    SYSTEM_WORKSPACE_ENV(() -> I18nMessageUtil.get("i18n.environment_variable.3867"), WorkspaceEnvVarService.class),
    CLUSTER_INFO(() -> I18nMessageUtil.get("i18n.cluster_management.74ea"), ClusterInfoService.class),

    SCRIPT(() -> I18nMessageUtil.get("i18n.script_template.54f2"), ScriptServer.class),
    SCRIPT_LOG(() -> I18nMessageUtil.get("i18n.script_template_log.30cb"), ScriptExecuteLogServer.class),

    //******************************************     节点管理功能
    PROJECT(() -> I18nMessageUtil.get("i18n.project_management.4363"), ClassFeature.NODE, ProjectInfoCacheService.class),
    PROJECT_FILE(() -> I18nMessageUtil.get("i18n.project_file_manager.c8cb"), ClassFeature.NODE, ProjectInfoCacheService.class),
    PROJECT_LOG(() -> I18nMessageUtil.get("i18n.project_log.2926"), ClassFeature.NODE, ProjectInfoCacheService.class),
    PROJECT_CONSOLE(() -> I18nMessageUtil.get("i18n.project_console.3a94"), ClassFeature.NODE, ProjectInfoCacheService.class),
    //    JDK_LIST("JDK管理", ClassFeature.NODE),
    NODE_SCRIPT(() -> I18nMessageUtil.get("i18n.node_script_template.be6a"), ClassFeature.NODE, NodeScriptServer.class),
    NODE_SCRIPT_LOG(() -> I18nMessageUtil.get("i18n.node_script_template_log.85e3"), ClassFeature.NODE, NodeScriptExecuteLogServer.class),
    AGENT_LOG(() -> I18nMessageUtil.get("i18n.plugin_system_log.955c"), ClassFeature.NODE),
    FREE_SCRIPT(() -> I18nMessageUtil.get("i18n.free_script.7760"), ClassFeature.NODE, MachineNodeServer.class),
//    TOMCAT_FILE("Tomcat file", ClassFeature.NODE),
//    TOMCAT_LOG("Tomcat log", ClassFeature.NODE),


    NODE_CONFIG_WHITELIST(() -> I18nMessageUtil.get("i18n.node_authorized_config.f934"), ClassFeature.NODE),
    NODE_CONFIG(() -> I18nMessageUtil.get("i18n.node_authorized_config.f934"), ClassFeature.NODE),
    NODE_CACHE(() -> I18nMessageUtil.get("i18n.node_cache.d68c"), ClassFeature.NODE),
    NODE_LOG(() -> I18nMessageUtil.get("i18n.node_system_logs.3ac9"), ClassFeature.NODE),
    NODE_UPGRADE(() -> I18nMessageUtil.get("i18n.node_online_upgrade.f144"), ClassFeature.NODE),


//	PROJECT_RECOVER("项目回收", ClassFeature.NODE),

    ;

    private final Supplier<String> name;
    private final ClassFeature parent;
    private final Class<? extends BaseDbService<?>> dbService;

    ClassFeature(Supplier<String> name) {
        this(name, null, null);
    }

    ClassFeature(Supplier<String> name, ClassFeature parent) {
        this(name, parent, null);
    }


    ClassFeature(Supplier<String> name, Class<? extends BaseDbService<?>> dbService) {
        this(name, null, dbService);
    }

    ClassFeature(Supplier<String> name, ClassFeature parent, Class<? extends BaseDbService<?>> dbService) {
        this.name = name;
        this.parent = parent;
        this.dbService = dbService;
    }
}
