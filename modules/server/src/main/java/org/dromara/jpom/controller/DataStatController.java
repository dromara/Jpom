/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.dialect.DialectUtil;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.func.files.service.StaticFileStorageService;
import org.dromara.jpom.func.system.service.ClusterInfoService;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.docker.DockerSwarmInfoService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.node.script.NodeScriptServer;
import org.dromara.jpom.service.node.ssh.SshCommandService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.service.outgiving.OutGivingServer;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.service.system.WorkspaceService;
import org.dromara.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 24/1/4 004
 */
@RestController
@RequestMapping(value = "stat")
@Slf4j
public class DataStatController {

    private final NodeService nodeService;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final NodeScriptServer nodeScriptServer;
    private final OutGivingServer outGivingServer;
    private final SshService sshService;
    private final SshCommandService sshCommandService;
    private final ScriptServer scriptServer;
    private final DockerInfoService dockerInfoService;
    private final FileStorageService fileStorageService;
    private final StaticFileStorageService staticFileStorageService;
    private final DockerSwarmInfoService dockerSwarmInfoService;
    private final UserService userService;
    private final WorkspaceService workspaceService;
    private final ClusterInfoService clusterInfoService;
    private final MachineNodeServer machineNodeServer;
    private final MachineSshServer machineSshServer;
    private final MachineDockerServer machineDockerServer;

    public DataStatController(NodeService nodeService,
                              ProjectInfoCacheService projectInfoCacheService,
                              NodeScriptServer nodeScriptServer,
                              OutGivingServer outGivingServer,
                              SshService sshService,
                              SshCommandService sshCommandService,
                              ScriptServer scriptServer,
                              DockerInfoService dockerInfoService,
                              FileStorageService fileStorageService,
                              StaticFileStorageService staticFileStorageService,
                              DockerSwarmInfoService dockerSwarmInfoService,
                              UserService userService,
                              WorkspaceService workspaceService,
                              ClusterInfoService clusterInfoService,
                              MachineNodeServer machineNodeServer,
                              MachineSshServer machineSshServer,
                              MachineDockerServer machineDockerServer) {
        this.nodeService = nodeService;
        this.projectInfoCacheService = projectInfoCacheService;
        this.nodeScriptServer = nodeScriptServer;
        this.outGivingServer = outGivingServer;
        this.sshService = sshService;
        this.sshCommandService = sshCommandService;
        this.scriptServer = scriptServer;
        this.dockerInfoService = dockerInfoService;
        this.fileStorageService = fileStorageService;
        this.staticFileStorageService = staticFileStorageService;
        this.dockerSwarmInfoService = dockerSwarmInfoService;
        this.userService = userService;
        this.workspaceService = workspaceService;
        this.clusterInfoService = clusterInfoService;
        this.machineNodeServer = machineNodeServer;
        this.machineSshServer = machineSshServer;
        this.machineDockerServer = machineDockerServer;
    }

    @RequestMapping(value = "workspace", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Map<String, Number>> workspace(HttpServletRequest request) {
        String workspaceId = nodeService.getCheckUserWorkspace(request);
        Entity entity = Entity.create();
        entity.set("workspaceId", workspaceId);
        Map<String, Number> map = new HashMap<>(10);
        map.put("nodeCount", nodeService.count(entity));
        map.put("projectCount", projectInfoCacheService.count(entity));
        map.put("nodeScriptCount", nodeScriptServer.count(entity));
        map.put("outGivingCount", outGivingServer.count(entity));
        map.put("sshCount", sshService.count(entity));
        map.put("sshCommandCount", sshCommandService.count(entity));
        map.put("scriptCount", scriptServer.count(entity));
        map.put("dockerCount", dockerInfoService.count(entity));
        map.put("dockerSwarmCount", dockerSwarmInfoService.count(entity));
        map.put("fileCount", fileStorageService.count(entity));
        //map.put("staticFileCount", staticFileStorageService.count(entity));
        return JsonMessage.success("", map);
    }

    @RequestMapping(value = "system", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    public IJsonMessage<Map<String, Object>> system(HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>(10);
        {
            long count = userService.count();
            map.put("userCount", count);
            {
                UserModel userModel = new UserModel();
                userModel.setSystemUser(1);
                long systemUserCount = userService.count(userModel);
                map.put("systemUserCount", systemUserCount);
            }
            {
                UserModel userModel = new UserModel();
                userModel.setStatus(0);
                long disableUserCount = userService.count(userModel);
                map.put("disableUserCount", disableUserCount);
            }
            String twoFactorAuthKey = DialectUtil.wrapField("twoFactorAuthKey");
            String sql = StrUtil.format("select count(1) from {} where {} is null or {}=''",
                userService.getTableName(),twoFactorAuthKey,twoFactorAuthKey);
            Number closeTwoFactorAuth = ObjectUtil.defaultIfNull(userService.queryNumber(sql), 0);
            map.put("openTwoFactorAuth", count - closeTwoFactorAuth.intValue());
        }
        {
            long workspaceCount = workspaceService.count();
            long clusterCount = clusterInfoService.count();
            map.put("workspaceCount", workspaceCount);
            map.put("clusterCount", clusterCount);
        }
        {
            String sql = "select status,count(1) as count from " + machineDockerServer.getTableName() + " group by status";
            List<Entity> query = machineDockerServer.query(sql);
            map.put("dockerStat", query);
        }
        {
            String sql = "select status,count(1) as count from " + machineSshServer.getTableName() + " group by status";
            List<Entity> query = machineSshServer.query(sql);
            map.put("sshStat", query);
        }
        {
            String sql = "select status,count(1) as count from " + machineNodeServer.getTableName() + " group by status";
            List<Entity> query = machineNodeServer.query(sql);
            map.put("nodeStat", query);
        }
        return JsonMessage.success("", map);
    }
}
