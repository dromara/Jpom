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
package org.dromara.jpom.controller;

import cn.hutool.db.Entity;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.func.files.service.StaticFileStorageService;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.docker.DockerSwarmInfoService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.node.script.NodeScriptServer;
import org.dromara.jpom.service.node.ssh.SshCommandService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.service.outgiving.OutGivingServer;
import org.dromara.jpom.service.script.ScriptServer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
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
                              DockerSwarmInfoService dockerSwarmInfoService) {
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
}
