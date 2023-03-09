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
package io.jpom.func.assets.controller;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Entity;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.JsonMessage;
import io.jpom.common.multipart.MultipartFileBuilder;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.func.BaseGroupNameController;
import io.jpom.func.assets.model.MachineDockerModel;
import io.jpom.func.assets.server.MachineDockerServer;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.model.docker.DockerSwarmInfoMode;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.service.docker.DockerSwarmInfoService;
import io.jpom.service.system.WorkspaceService;
import io.jpom.system.ServerConfig;
import io.jpom.util.CompressionFileUtil;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;

/**
 * @author bwcx_jzy
 * @since 2023/3/3
 */
@RestController
@RequestMapping(value = "/system/assets/docker")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE_DOCKER)
@SystemPermission
@Slf4j
public class MachineDockerController extends BaseGroupNameController {
    private final MachineDockerServer machineDockerServer;
    private final ServerConfig serverConfig;
    private final DockerInfoService dockerInfoService;
    private final DockerSwarmInfoService dockerSwarmInfoService;
    private final WorkspaceService workspaceService;

    public MachineDockerController(MachineDockerServer machineDockerServer,
                                   ServerConfig serverConfig,
                                   DockerInfoService dockerInfoService,
                                   DockerSwarmInfoService dockerSwarmInfoService,
                                   WorkspaceService workspaceService) {
        super(machineDockerServer);
        this.machineDockerServer = machineDockerServer;
        this.serverConfig = serverConfig;
        this.dockerInfoService = dockerInfoService;
        this.dockerSwarmInfoService = dockerSwarmInfoService;
        this.workspaceService = workspaceService;
    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<MachineDockerModel>> listJson(HttpServletRequest request) {
        PageResultDto<MachineDockerModel> pageResultDto = machineDockerServer.listPage(request);
        pageResultDto.each(this::checkCertPath);
        return JsonMessage.success("", pageResultDto);
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<Object> edit(String id, String host) throws Exception {
        // 保存路径
        File tempPath = serverConfig.getUserTempPath();
        File savePath = FileUtil.file(tempPath, "docker", SecureUtil.sha1(host));
        MachineDockerModel dockerInfoModel = this.takeOverModel(savePath);
        boolean certExist = dockerInfoModel.getCertExist();
        if (StrUtil.isEmpty(id)) {
            // 创建
            if (dockerInfoModel.getTlsVerify()) {
                Assert.state(certExist, "请上传证书文件");
            }
            this.check(dockerInfoModel, certExist, savePath);
            // 默认正常
            dockerInfoModel.setStatus(1);
            machineDockerServer.insert(dockerInfoModel);
        } else {
            this.check(dockerInfoModel, certExist, savePath);
            machineDockerServer.updateById(dockerInfoModel);
        }
        //
        return JsonMessage.success("操作成功");
    }

    /**
     * 验证 证书文件是否存在
     *
     * @param dockerInfoModel docker
     * @return true 证书文件存在
     */
    private boolean checkCertPath(MachineDockerModel dockerInfoModel) {
        if (dockerInfoModel == null) {
            return false;
        }
        if (dockerInfoModel.getCertExist() != null && dockerInfoModel.getCertExist()) {
            return true;
        }
        String certPath = dockerInfoModel.generateCertPath();
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        try {
            boolean execute = (boolean) plugin.execute("certPath", "certPath", certPath);
            dockerInfoModel.setCertExist(execute);
            return execute;
        } catch (Exception e) {
            log.error("检查 docker 证书异常", e);
            return false;
        }
    }

    /**
     * 接收前端参数
     *
     * @param certPathFile 证书保存临时文件夹
     * @return model
     * @throws Exception 异常
     */
    private MachineDockerModel takeOverModel(File certPathFile) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        String name = getParameter("name");
        Assert.hasText(name, "请填写 名称");
        String host = getParameter("host");
        String id = getParameter("id");
        String tlsVerifyStr = getParameter("tlsVerify");
        String registryUrl = getParameter("registryUrl");
        String registryUsername = getParameter("registryUsername");
        String registryPassword = getParameter("registryPassword");
        String registryEmail = getParameter("registryEmail");
        int heartbeatTimeout = getParameterInt("heartbeatTimeout", -1);
        String groupName = getParameter("groupName");
        boolean tlsVerify = Convert.toBool(tlsVerifyStr, false);
        //
        boolean certExist = false;
        if (tlsVerify) {
            // 如果是创建就必须上传证书
            MultipartFileBuilder multipart = null;
            try {
                multipart = createMultipart();
            } catch (Exception e) {
                MachineDockerModel dockerInfoModel = machineDockerServer.getByKey(id);
                certExist = this.checkCertPath(dockerInfoModel);
                Assert.state(certExist, "请上传证书文件");
            }
            if (multipart != null) {
                String absolutePath = FileUtil.getAbsolutePath(certPathFile);
                multipart.setSavePath(absolutePath).addFieldName("file").setUseOriginalFilename(true);
                String localPath = multipart.setFileExt(StringUtil.PACKAGE_EXT).save();
                // 解压
                File file = new File(localPath);
                CompressionFileUtil.unCompress(file, certPathFile);
                boolean ok = (boolean) plugin.execute("certPath", "certPath", absolutePath);
                Assert.state(ok, "证书信息不正确,证书压缩包里面必须包含：ca.pem、key.pem、cert.pem");
                certExist = true;
            }
        }
        boolean ok = (boolean) plugin.execute("host", "host", host);
        Assert.state(ok, "请填写正确的 host");
        // 验证重复
        Entity entity = Entity.create();
        entity.set("host", host);
        if (StrUtil.isNotEmpty(id)) {
            entity.set("id", StrUtil.format(" <> {}", id));
        }
        boolean exists = machineDockerServer.exists(entity);
        Assert.state(!exists, "对应的 docker 已经存在啦");
        //
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        machineDockerModel.setHeartbeatTimeout(heartbeatTimeout);
        machineDockerModel.setHost(host);
        machineDockerModel.setName(name);
        machineDockerModel.setTlsVerify(tlsVerify);
        machineDockerModel.setCertExist(certExist);
        machineDockerModel.setRegistryUrl(registryUrl);
        machineDockerModel.setRegistryUsername(registryUsername);
        machineDockerModel.setRegistryPassword(registryPassword);
        machineDockerModel.setRegistryEmail(registryEmail);
        machineDockerModel.setGroupName(groupName);
        //
        machineDockerModel.setId(id);
        return machineDockerModel;
    }


    private void check(MachineDockerModel dockerInfoModel, boolean certExist, File savePath) throws Exception {
        // 移动证书
        if (certExist) {
            if (FileUtil.isDirectory(savePath) && FileUtil.isNotEmpty(savePath)) {
                String generateCertPath = dockerInfoModel.generateCertPath();
                FileUtil.moveContent(savePath, FileUtil.file(generateCertPath), true);
            }
        }
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        Map<String, Object> parameter = dockerInfoModel.toParameter();
        parameter.put("closeBefore", true);
        String errorReason = (String) plugin.execute("ping", parameter);
        Assert.isNull(errorReason, () -> "无法连接 docker 请检查 host 或者 TLS 证书 以及仓库信息配置是否正确。" + errorReason);
        // 检查授权
        String registryUrl = dockerInfoModel.getRegistryUrl();
        if (StrUtil.isNotEmpty(registryUrl)) {
            MachineDockerModel oldInfoModel = machineDockerServer.getByKey(dockerInfoModel.getId(), false);
            String registryPassword = Optional.ofNullable(dockerInfoModel.getRegistryPassword()).orElseGet(() -> Optional.ofNullable(oldInfoModel).map(MachineDockerModel::getRegistryPassword).orElse(null));
            Assert.hasText(registryPassword, "仓库密码不能为空");
            parameter.put("closeBefore", true);
            parameter.put("registryPassword", registryPassword);
            try {
                JSONObject jsonObject = (JSONObject) plugin.execute("testAuth", parameter);
                log.info("{}", jsonObject);
            } catch (Exception e) {
                log.warn("仓库授权信息错误", e);
                throw new IllegalArgumentException("仓库账号或者密码错误：" + e.getMessage());
            }
        }
    }

    @GetMapping(value = "try-local-docker", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> tryLocalDocker(HttpServletRequest request) {
        try {
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
            String dockerHost = (String) plugin.execute("testLocal", new HashMap<>(1));
            Entity entity = Entity.create();
            entity.set("host", dockerHost);
            boolean exists = machineDockerServer.exists(entity);
            if (exists) {
                return new JsonMessage<>(405, "已经存在本地 docker 信息啦，不要重复添加");
            }
            MachineDockerModel dockerModel = new MachineDockerModel();
            dockerModel.setHost(dockerHost);
            dockerModel.setName("localhost");
            dockerModel.setStatus(1);
            machineDockerServer.insert(dockerModel);
            return new JsonMessage<>(200, "自动探测到本地 docker 并且自动添加：" + dockerHost);
        } catch (Throwable e) {
            log.error("探测本地 docker 异常", e);
            return new JsonMessage<>(500, "探测本地 docker 异常：" + e.getMessage());
        }
    }

    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<Object> del(@ValidatorItem String id) throws Exception {
        //
        {
            DockerInfoModel dockerInfoModel = new DockerInfoModel();
            dockerInfoModel.setMachineDockerId(id);
            long count = dockerInfoService.count(dockerInfoModel);
            Assert.state(count <= 0, "当前 docker 还关联" + count + "个 工作空间 docker 不能删除");
        }
        MachineDockerModel infoModel = machineDockerServer.getByKey(id);
        Optional.ofNullable(infoModel).ifPresent(machineDockerModel -> {
            if (StrUtil.isNotEmpty(machineDockerModel.getSwarmId())) {
                // 判断集群
                DockerSwarmInfoMode dockerInfoModel = new DockerSwarmInfoMode();
                dockerInfoModel.setSwarmId(machineDockerModel.getSwarmId());
                long count = dockerSwarmInfoService.count(dockerInfoModel);
                Assert.state(count <= 0, "当前 docker 还关联" + count + "个 工作空间 docker 集群不能删除");
            }
            // 删除文件
            FileUtil.del(machineDockerModel.generateCertPath());
        });
        machineDockerServer.delByKey(id);
        return JsonMessage.success("删除成功");
    }


    /**
     * 将 docker 分配到指定工作空间
     *
     * @param id          docker id
     * @param workspaceId 工作空间id
     * @return json
     */
    @PostMapping(value = "distribute", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> distribute(@ValidatorItem String id, @ValidatorItem String workspaceId, String type) {
        MachineDockerModel machineDockerModel = machineDockerServer.getByKey(id);
        Assert.notNull(machineDockerModel, "没有对应的 docker");
        boolean exists = workspaceService.exists(new WorkspaceModel(workspaceId));
        Assert.state(exists, "不存在对应的工作空间");
        if (StrUtil.equals(type, "docker")) {
            DockerInfoModel dockerInfoModel = new DockerInfoModel();
            dockerInfoModel.setMachineDockerId(id);
            dockerInfoModel.setWorkspaceId(workspaceId);
            //
            exists = dockerInfoService.exists(dockerInfoModel);
            Assert.state(!exists, "对应工作空间已经存在此 docker 啦");
            //
            dockerInfoModel.setName(machineDockerModel.getName());
            dockerInfoService.insert(dockerInfoModel);
        } else if (StrUtil.equals(type, "swarm")) {
            Assert.hasText(machineDockerModel.getSwarmId(), "当前 docker 不在集群中");
            DockerSwarmInfoMode dockerInfoModel = new DockerSwarmInfoMode();
            dockerInfoModel.setSwarmId(machineDockerModel.getSwarmId());
            dockerInfoModel.setWorkspaceId(workspaceId);
            //
            exists = dockerSwarmInfoService.exists(dockerInfoModel);
            Assert.state(!exists, "对应工作空间已经存在此 docker 集群啦");
            //
            dockerInfoModel.setName(machineDockerModel.getName());
            dockerSwarmInfoService.insert(dockerInfoModel);
        } else {
            throw new IllegalArgumentException("未知参数");
        }
        return JsonMessage.success("操作成功");
    }

    @GetMapping(value = "list-workspace-docker", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<JSONObject> listWorkspaceSsh(@ValidatorItem String id) {
        MachineDockerModel machineDockerModel = machineDockerServer.getByKey(id);
        Assert.notNull(machineDockerModel, "没有对应的 docker");
        JSONObject jsonObject = new JSONObject();
        {
            DockerInfoModel dockerInfoModel = new DockerInfoModel();
            dockerInfoModel.setMachineDockerId(id);
            List<DockerInfoModel> modelList = dockerInfoService.listByBean(dockerInfoModel);
            modelList = Optional.ofNullable(modelList).orElseGet(ArrayList::new);
            for (DockerInfoModel model : modelList) {
                model.setWorkspace(workspaceService.getByKey(model.getWorkspaceId()));
            }
            jsonObject.put("dockerList", modelList);
        }
        {
            String swarmId = machineDockerModel.getSwarmId();
            if (StrUtil.isNotEmpty(swarmId)) {
                DockerSwarmInfoMode dockerInfoModel = new DockerSwarmInfoMode();
                dockerInfoModel.setSwarmId(swarmId);
                List<DockerSwarmInfoMode> modelList = dockerSwarmInfoService.listByBean(dockerInfoModel);
                modelList = Optional.ofNullable(modelList).orElseGet(ArrayList::new);
                for (DockerSwarmInfoMode model : modelList) {
                    model.setWorkspace(workspaceService.getByKey(model.getWorkspaceId()));
                }
                jsonObject.put("swarmList", modelList);
            }
        }
        return JsonMessage.success("", jsonObject);
    }

}
