/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.controller;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.db.Entity;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.func.BaseGroupNameController;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.func.cert.model.CertificateInfoModel;
import org.dromara.jpom.func.cert.service.CertificateInfoService;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.model.docker.DockerSwarmInfoMode;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.docker.DockerSwarmInfoService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    private final MachineSshServer machineSshServer;
    private final DockerInfoService dockerInfoService;
    private final DockerSwarmInfoService dockerSwarmInfoService;
    private final WorkspaceService workspaceService;
    private final CertificateInfoService certificateInfoService;

    public MachineDockerController(MachineSshServer machineSshServer,
                                   MachineDockerServer machineDockerServer,
                                   DockerInfoService dockerInfoService,
                                   DockerSwarmInfoService dockerSwarmInfoService,
                                   WorkspaceService workspaceService,
                                   CertificateInfoService certificateInfoService,
                                   ServerConfig serverConfig) {
        super(machineDockerServer);
        this.machineSshServer = machineSshServer;
        this.machineDockerServer = machineDockerServer;
        this.dockerInfoService = dockerInfoService;
        this.dockerSwarmInfoService = dockerSwarmInfoService;
        this.workspaceService = workspaceService;
        this.certificateInfoService = certificateInfoService;
        this.serverConfig = serverConfig;
    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<MachineDockerModel>> listJson(HttpServletRequest request) {
        PageResultDto<MachineDockerModel> pageResultDto = machineDockerServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Object> edit(String id) throws Exception {
        MachineDockerModel dockerInfoModel = this.takeOverModel();
        if (StrUtil.isEmpty(id)) {
            // 创建
            this.check(dockerInfoModel);
            // 默认正常
            dockerInfoModel.setStatus(1);
            machineDockerServer.insert(dockerInfoModel);
        } else {
            this.check(dockerInfoModel);
            machineDockerServer.updateById(dockerInfoModel);
        }
        //
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }


    /**
     * 接收前端参数
     *
     * @return model
     * @throws Exception 异常
     */
    private MachineDockerModel takeOverModel() throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        String name = getParameter("name");
        Assert.hasText(name, I18nMessageUtil.get("i18n.please_fill_in_name.52f3"));
        String host = getParameter("host");
        String id = getParameter("id");
        String tlsVerifyStr = getParameter("tlsVerify");
        String registryUrl = getParameter("registryUrl");
        String registryUsername = getParameter("registryUsername");
        String registryPassword = getParameter("registryPassword");
        String registryEmail = getParameter("registryEmail");
        int heartbeatTimeout = getParameterInt("heartbeatTimeout", -1);
        String groupName = getParameter("groupName");
        String certInfo = getParameter("certInfo");
        String enableSshStr = getParameter("enableSsh");
        boolean enableSsh = Convert.toBool(enableSshStr, false);
        String machineSshId = getParameter("machineSshId");
        if (enableSsh) {
            MachineSshModel model = machineSshServer.getByKey(machineSshId);
            host = "ssh://" + model.getHost() + ":" + (model.getPort() == null ? 22 : model.getPort());
        }
        boolean tlsVerify = Convert.toBool(tlsVerifyStr, false);
        //
        if (tlsVerify) {
            File filePath = certificateInfoService.getFilePath(certInfo);
            Assert.notNull(filePath, I18nMessageUtil.get("i18n.incorrect_certificate_info.aee1"));
            // 验证证书文件是否正确
            boolean ok = (boolean) plugin.execute("certPath", "certPath", filePath.getAbsolutePath());
            Assert.state(ok, I18nMessageUtil.get("i18n.certificate_info_incorrect.a950"));
        } else {
            certInfo = StrUtil.emptyToDefault(certInfo, StrUtil.EMPTY);
        }
        boolean ok = (boolean) plugin.execute("host", "host", host);
        Assert.state(ok, I18nMessageUtil.get("i18n.correct_host_required.8c49"));
        // 验证重复
        Entity entity = Entity.create();
        entity.set("host", host);
        if (StrUtil.isNotEmpty(id)) {
            entity.set("id", StrUtil.format(" <> {}", id));
        }
        boolean exists = machineDockerServer.exists(entity);
        Assert.state(!exists, I18nMessageUtil.get("i18n.docker_already_exists.d9a5"));
        //
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        machineDockerModel.setHeartbeatTimeout(heartbeatTimeout);
        machineDockerModel.setHost(host);
        machineDockerModel.setEnableSsh(enableSsh);
        machineDockerModel.setMachineSshId(machineSshId);
        // 保存是会验证证书一定存在
        machineDockerModel.setCertExist(tlsVerify);
        machineDockerModel.setName(name);
        machineDockerModel.setCertInfo(certInfo);
        machineDockerModel.setTlsVerify(tlsVerify);
        machineDockerModel.setRegistryUrl(registryUrl);
        machineDockerModel.setRegistryUsername(registryUsername);
        machineDockerModel.setRegistryPassword(registryPassword);
        machineDockerModel.setRegistryEmail(registryEmail);
        machineDockerModel.setGroupName(groupName);
        //
        machineDockerModel.setId(id);
        return machineDockerModel;
    }


    private void check(MachineDockerModel dockerInfoModel) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        Map<String, Object> parameter = machineDockerServer.toParameter(dockerInfoModel);
        parameter.put("closeBefore", true);
        String errorReason = (String) plugin.execute("ping", parameter);
        Assert.isNull(errorReason, () -> I18nMessageUtil.get("i18n.unable_to_connect_to_docker.2bb3") + errorReason);
        // 检查授权
        String registryUrl = dockerInfoModel.getRegistryUrl();
        if (StrUtil.isNotEmpty(registryUrl)) {
            MachineDockerModel oldInfoModel = machineDockerServer.getByKey(dockerInfoModel.getId(), false);
            String registryPassword = Optional.ofNullable(dockerInfoModel.getRegistryPassword()).orElseGet(() -> Optional.ofNullable(oldInfoModel).map(MachineDockerModel::getRegistryPassword).orElse(null));
            Assert.hasText(registryPassword, I18nMessageUtil.get("i18n.repository_password_cannot_be_empty.20b3"));
            parameter.put("closeBefore", true);
            parameter.put("registryPassword", registryPassword);
            try {
                JSONObject jsonObject = (JSONObject) plugin.execute("testAuth", parameter);
                log.info("{}", jsonObject);
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.repository_authorization_error.4f50"), e);
                throw new IllegalArgumentException(I18nMessageUtil.get("i18n.incorrect_repository_credentials.f1c8") + e.getMessage());
            }
        }
        // 修改状态为在线
        dockerInfoModel.setStatus(1);
    }

    @GetMapping(value = "try-local-docker", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> tryLocalDocker(HttpServletRequest request) {
        try {
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
            String dockerHost = (String) plugin.execute("testLocal", new HashMap<>(1));
            Entity entity = Entity.create();
            entity.set("host", dockerHost);
            boolean exists = machineDockerServer.exists(entity);
            if (exists) {
                return new JsonMessage<>(405, I18nMessageUtil.get("i18n.local_docker_exists.ec31") + dockerHost);
            }
            MachineDockerModel dockerModel = new MachineDockerModel();
            dockerModel.setHost(dockerHost);
            dockerModel.setName("localhost");
            dockerModel.setStatus(1);
            machineDockerServer.insert(dockerModel);
            return new JsonMessage<>(200, I18nMessageUtil.get("i18n.auto_detect_local_docker_and_add.af72") + dockerHost);
        } catch (Throwable e) {
            log.error(I18nMessageUtil.get("i18n.detect_local_docker_exception.ccfc"), e);
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.detect_local_docker_exception_with_details.7cc9") + e.getMessage());
        }
    }

    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> del(@ValidatorItem String id) throws Exception {
        //
        {
            DockerInfoModel dockerInfoModel = new DockerInfoModel();
            dockerInfoModel.setMachineDockerId(id);
            long count = dockerInfoService.count(dockerInfoModel);
            Assert.state(count <= 0, StrUtil.format(I18nMessageUtil.get("i18n.docker_associated_workspaces_message.de78"), count));
        }
        MachineDockerModel infoModel = machineDockerServer.getByKey(id);
        Optional.ofNullable(infoModel).ifPresent(machineDockerModel -> {
            if (StrUtil.isNotEmpty(machineDockerModel.getSwarmId())) {
                // 判断集群
                DockerSwarmInfoMode dockerInfoModel = new DockerSwarmInfoMode();
                dockerInfoModel.setSwarmId(machineDockerModel.getSwarmId());
                long count = dockerSwarmInfoService.count(dockerInfoModel);
                Assert.state(count <= 0, StrUtil.format(I18nMessageUtil.get("i18n.docker_cluster_associated_workspaces_message.5520"), count));
            }
        });
        machineDockerServer.delByKey(id);
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }


    /**
     * 将 docker 分配到指定工作空间
     *
     * @param ids         docker id
     * @param workspaceId 工作空间id
     * @return json
     */
    @PostMapping(value = "distribute", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> distribute(@ValidatorItem String ids, @ValidatorItem String workspaceId, String type) {
        List<String> list = StrUtil.splitTrim(ids, StrUtil.COMMA);
        for (String id : list) {
            MachineDockerModel machineDockerModel = machineDockerServer.getByKey(id);
            Assert.notNull(machineDockerModel, I18nMessageUtil.get("i18n.no_corresponding_docker.733e"));
            boolean exists = workspaceService.exists(new WorkspaceModel(workspaceId));
            Assert.state(exists, I18nMessageUtil.get("i18n.workspace_not_exist.a6fd"));
            if (StrUtil.equals(type, "docker")) {
                DockerInfoModel dockerInfoModel = new DockerInfoModel();
                dockerInfoModel.setMachineDockerId(id);
                dockerInfoModel.setWorkspaceId(workspaceId);
                //
                exists = dockerInfoService.exists(dockerInfoModel);
                if (!exists) {
                    //
                    dockerInfoModel.setName(machineDockerModel.getName());
                    dockerInfoService.insert(dockerInfoModel);
                }
            } else if (StrUtil.equals(type, "swarm")) {
                Assert.hasText(machineDockerModel.getSwarmId(), () -> StrUtil.format(I18nMessageUtil.get("i18n.current_docker_not_in_cluster.f70c"), machineDockerModel.getName()));
                DockerSwarmInfoMode dockerInfoModel = new DockerSwarmInfoMode();
                dockerInfoModel.setSwarmId(machineDockerModel.getSwarmId());
                dockerInfoModel.setWorkspaceId(workspaceId);
                //
                if (!dockerSwarmInfoService.exists(dockerInfoModel)) {
                    //
                    dockerInfoModel.setName(machineDockerModel.getName());
                    dockerSwarmInfoService.insert(dockerInfoModel);
                }
            } else {
                throw new IllegalArgumentException(I18nMessageUtil.get("i18n.unknown_parameter.96dd"));
            }
        }

        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    @GetMapping(value = "list-workspace-docker", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> listWorkspaceSsh(@ValidatorItem String id) {
        MachineDockerModel machineDockerModel = machineDockerServer.getByKey(id);
        Assert.notNull(machineDockerModel, I18nMessageUtil.get("i18n.no_corresponding_docker.733e"));
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


    /**
     * 导入 docker tls
     *
     * @param file 文件
     * @return model
     * @throws Exception 异常
     */
    @PostMapping(value = "import-tls", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public IJsonMessage<String> importTls(MultipartFile file) throws Exception {
        Assert.notNull(file, I18nMessageUtil.get("i18n.no_uploaded_file.07ef"));
        // 保存路径
        File tempPath = FileUtil.file(serverConfig.getUserTempPath(), "docker", IdUtil.fastSimpleUUID());
        try {
            String originalFilename = file.getOriginalFilename();
            String extName = FileUtil.extName(originalFilename);
            Assert.state(StrUtil.containsIgnoreCase(extName, "zip"), I18nMessageUtil.get("i18n.invalid_file_type.7246"));
            File saveFile = FileUtil.file(tempPath, originalFilename);
            FileUtil.mkParentDirs(saveFile);
            file.transferTo(saveFile);
            // 解压
            ZipUtil.unzip(saveFile, tempPath);
            // 先判断文件
            boolean checkCertPath = machineDockerServer.checkCertPath(tempPath.getAbsolutePath());
            Assert.state(checkCertPath, I18nMessageUtil.get("i18n.certificate_info_incorrect.a950"));
            CertificateInfoModel certificateInfoModel = certificateInfoService.resolveX509(tempPath, true);
            certificateInfoModel.setWorkspaceId(ServerConst.WORKSPACE_GLOBAL);
            certificateInfoService.insert(certificateInfoModel);
            return JsonMessage.success(I18nMessageUtil.get("i18n.import_success.b6d1"));
        } finally {
            FileUtil.del(tempPath);
        }
    }

}
