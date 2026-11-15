/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.Type;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.*;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.update.AgentEngine;
import org.dromara.jpom.common.update.AgentPackageInfo;
import org.dromara.jpom.common.update.AgentPackageResolver;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.controller.system.SystemUpdateController;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.openapi.controller.NodeInfoController;
import org.dromara.jpom.model.AgentFileModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.system.AgentPackageService;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * @author bwcx_jzy
 * @since 2021/11/29
 */
@RestController
@RequestMapping(value = "/node")
@SystemPermission(superUser = true)
@Feature(cls = ClassFeature.UPGRADE_NODE_LIST)
@Slf4j
public class NodeUpdateController extends BaseServerController {

    private final SystemParametersServer systemParametersServer;
    private final ServerConfig serverConfig;
    private final AgentPackageService agentPackageService;

    public NodeUpdateController(SystemParametersServer systemParametersServer,
                                ServerConfig serverConfig,
                                AgentPackageService agentPackageService) {
        this.systemParametersServer = systemParametersServer;
        this.serverConfig = serverConfig;
        this.agentPackageService = agentPackageService;
    }

    /**
     * 远程下载
     *
     * <p>默认下载 Java 版插件端发布包（保持原有行为）；engine=go 时从下载中心同版本目录
     * 按节点平台下载 Go 版在线升级容器 agent-go-&lt;version&gt;-upgrade-&lt;os&gt;-&lt;arch&gt;.jar
     * （linux/amd64 下载不到时回退兼容别名 agent-go-&lt;version&gt;-upgrade.jar）。</p>
     *
     * @param engine   引擎：java（默认）/ go
     * @param platform 节点平台（os/arch，engine=go 时使用；为空按 linux/amd64 legacy 处理）
     * @return json
     * @see RemoteVersion
     */
    @GetMapping(value = "download_remote.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.REMOTE_DOWNLOAD)
    public IJsonMessage<String> downloadRemote(String engine, String platform) throws IOException {
        if (AgentEngine.parse(engine) == AgentEngine.GO) {
            String targetPlatform = null;
            if (StrUtil.isNotBlank(platform)) {
                targetPlatform = AgentPackageResolver.normalizePlatform(platform);
                if (StrUtil.isBlank(targetPlatform)) {
                    return new JsonMessage<>(405, StrUtil.format(I18nMessageUtil.get("i18n.agent_package_platform_invalid.b1fb"), platform));
                }
            }
            // Go 升级容器（下载中心同版本目录，按平台命名，linux/amd64 回退兼容别名）
            agentPackageService.downloadGoPackage(targetPlatform);
            return JsonMessage.success(I18nMessageUtil.get("i18n.download_success.5094"));
        }
        String saveDir = serverConfig.getAgentPath().getAbsolutePath();
        Tuple download = RemoteVersion.download(saveDir, Type.Agent, false);
        // 保存文件
        this.saveAgentFile(download);
        return JsonMessage.success(I18nMessageUtil.get("i18n.download_success.5094"));
    }

    /**
     * 检查版本更新
     *
     * @return json
     * @see RemoteVersion
     * @see AgentFileModel
     */
    @Feature(method = MethodFeature.LIST)
    @GetMapping(value = "check_version.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> checkVersion() {
        cn.keepbx.jpom.RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
        AgentFileModel agentFileModel = agentPackageService.get(AgentEngine.JAVA);
        AgentEngine engine = agentFileModel == null ? null : AgentEngine.JAVA;
        if (agentFileModel == null) {
            // 没有 Java 升级包时使用 Go 升级包版本（纯 Go 集群的版本展示，不影响 Java 集群）
            agentFileModel = agentPackageService.get(AgentEngine.GO);
            engine = agentFileModel == null ? null : AgentEngine.GO;
        }
        JSONObject jsonObject = new JSONObject();
        // 升级页展示引擎/平台/包类型（旧前端忽略新增字段；没有本地升级包时 engine/platform 为空）
        jsonObject.put("engine", engine == null ? null : engine.getName());
        jsonObject.put("platform", agentFileModel == null ? null : agentFileModel.getPlatform());
        if (remoteVersion == null) {
            jsonObject.put("upgrade", false);
        } else {
            String tagName = StrUtil.removePrefixIgnoreCase(remoteVersion.getTagName(), "v");
            jsonObject.put("tagName", tagName);
            if (agentFileModel == null) {
                jsonObject.put("upgrade", true);
            } else {
                String version = StrUtil.removePrefixIgnoreCase(agentFileModel.getVersion(), "v");
                jsonObject.put("upgrade", StrUtil.compareVersion(version, tagName) < 0);
                jsonObject.put("path", agentFileModel.getSavePath());
            }
            jsonObject.put("downloadSource", remoteVersion.getDownloadSource());
        }
        return JsonMessage.success("", jsonObject);
    }

    @RequestMapping(value = "upload-agent-sharding", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    @Feature(method = MethodFeature.UPLOAD, log = false)
    public IJsonMessage<String> uploadAgentSharding(MultipartFile file,
                                                    String sliceId,
                                                    Integer totalSlice,
                                                    Integer nowSlice,
                                                    String fileSumMd5) throws IOException {
        File userTempPath = serverConfig.getUserTempPath();
        this.uploadSharding(file, userTempPath.getAbsolutePath(), sliceId, totalSlice, nowSlice, fileSumMd5, "jar", "zip", "tar.gz");
        return JsonMessage.success(I18nMessageUtil.get("i18n.upload_success.a769"));
    }

    @RequestMapping(value = "upload-agent-sharding-merge", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    @Feature(method = MethodFeature.UPLOAD)
    public IJsonMessage<String> uploadAgent(String sliceId,
                                            Integer totalSlice,
                                            String fileSumMd5) throws IOException {
        File agentPath = serverConfig.getAgentPath();

        File userTempPath = serverConfig.getUserTempPath();
        File successFile = this.shardingTryMerge(userTempPath.getAbsolutePath(), sliceId, totalSlice, fileSumMd5);
        FileUtil.move(successFile, agentPath, true);
        //
        String path = FileUtil.file(agentPath, successFile.getName()).getAbsolutePath();
        // 解析压缩包
        File file = JpomManifest.zipFileFind(path, Type.Agent, agentPath.getAbsolutePath());
        path = FileUtil.getAbsolutePath(file);
        // Go 升级容器：按容器结构校验（旧 Server 的 checkJpomJar 要求 Main-Class 可从 jar 根加载，
        // 容器空壳 class 位于 BOOT-INF/classes 时无法加载，这里不依赖该检查）
        if (AgentPackageResolver.isGoContainer(file)) {
            AgentPackageInfo packageInfo;
            try {
                packageInfo = AgentPackageResolver.inspectPackage(file);
            } catch (IOException e) {
                FileUtil.del(path);
                return new JsonMessage<>(405, StrUtil.format(I18nMessageUtil.get("i18n.invalid_go_agent_upgrade_container.0755"), e.getMessage()));
            }
            // 升级容器必须声明平台且存在对应平台的可执行文件；旧容器（未声明 platform）保持旧结构校验
            String expectedPlatform = AgentPackageResolver.normalizePlatform(packageInfo.getPlatform());
            String containerError = AgentPackageResolver.validateGoContainer(file, expectedPlatform);
            if (containerError != null) {
                FileUtil.del(path);
                return new JsonMessage<>(405, StrUtil.format(I18nMessageUtil.get("i18n.invalid_go_agent_upgrade_container.0755"), containerError));
            }
            agentPackageService.save(AgentEngine.GO, file, packageInfo.getVersion(), packageInfo.getTimeStamp(), expectedPlatform);
            return JsonMessage.success(I18nMessageUtil.get("i18n.upload_success.a769"));
        }
        // Java 插件端升级包：保持原有 checkJpomJar 校验与保存行为
        JsonMessage<Tuple> error = JpomManifest.checkJpomJar(path, Type.Agent, false);
        if (!error.success()) {
            FileUtil.del(path);
            return new JsonMessage<>(error.getCode(), error.getMsg());
        }
        // 保存文件（按包内容自动区分 Java / Go 升级包）
        this.saveAgentFile(error.getData());
        return JsonMessage.success(I18nMessageUtil.get("i18n.upload_success.a769"));
    }

    private void saveAgentFile(Tuple data) {
        File file = data.get(3);
        AgentPackageInfo packageInfo = null;
        try {
            packageInfo = AgentPackageResolver.inspectPackage(file);
        } catch (IOException e) {
            log.warn(I18nMessageUtil.get("i18n.parse_jar.a26e"), e);
        }
        if (packageInfo != null && packageInfo.isGoContainer()) {
            // Go 升级容器（兼容旧数据：历史上传可能未做分类）：与 Java 升级包分开保存，记录平台
            agentPackageService.save(AgentEngine.GO, file, packageInfo.getVersion(), packageInfo.getTimeStamp(),
                AgentPackageResolver.normalizePlatform(packageInfo.getPlatform()));
            return;
        }
        agentPackageService.save(AgentEngine.JAVA, file, data.get(0), data.get(1));
    }

    @Feature(method = MethodFeature.EXECUTE)
    @GetMapping(value = "fast_install.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> fastInstall(HttpServletRequest request) {
        boolean beta = RemoteVersion.betaRelease();
        String language = I18nMessageUtil.tryGetNormalLanguage();
        InputStream inputStream = ResourceUtil.getStream("classpath:/fast-install/" + language + (beta ? "/beta.json" : "/release.json"));
        String json = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
        JSONObject jsonObject = new JSONObject();
        JpomManifest instance = JpomManifest.getInstance();
        jsonObject.put("token", instance.randomIdSign());
        jsonObject.put("key", ServerOpenApi.PUSH_NODE_KEY);
        //
        JSONArray jsonArray = JSONArray.parseArray(json);
        jsonObject.put("shUrls", jsonArray);
        //
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        String url = String.format("/%s/%s", contextPath, ServerOpenApi.RECEIVE_PUSH);
        jsonObject.put("url", FileUtil.normalize(url));
        // 下载授权码
        String auth = systemParametersServer.getConfig(SystemUpdateController.JPOM_REMOTE_VERSION_AUTH, String.class);
        jsonObject.put("auth", auth);
        return JsonMessage.success("", jsonObject);
    }

    @Feature(method = MethodFeature.LIST)
    @GetMapping(value = "pull_fast_install_result.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Collection<JSONObject>> pullFastInstallResult(String removeId) {
        Collection<JSONObject> jsonObjects = NodeInfoController.listReceiveCache(removeId);
        jsonObjects = jsonObjects.stream()
            .map(jsonObject -> {
                JSONObject clone = jsonObject.clone();
                clone.remove("canUseNode");
                return clone;
            })
            .collect(Collectors.toList());
        return JsonMessage.success("", jsonObjects);
    }

    @Feature(method = MethodFeature.EXECUTE)
    @GetMapping(value = "confirm_fast_install.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Collection<JSONObject>> confirmFastInstall(HttpServletRequest request,
                                                                   @ValidatorItem String id,
                                                                   @ValidatorItem String ip,
                                                                   int port) {
        JSONObject receiveCache = NodeInfoController.getReceiveCache(id);
        Assert.notNull(receiveCache, I18nMessageUtil.get("i18n.no_cache_info.fba1"));
        JSONArray jsonArray = receiveCache.getJSONArray("canUseNode");
        Assert.notEmpty(jsonArray, I18nMessageUtil.get("i18n.no_cache_info_with_minus_one.52f2"));
        Optional<MachineNodeModel> any = jsonArray.stream().map(o -> {
            if (o instanceof MachineNodeModel) {
                return (MachineNodeModel) o;
            }
            JSONObject jsonObject = (JSONObject) o;
            return jsonObject.toJavaObject(MachineNodeModel.class);
        }).filter(nodeModel -> StrUtil.equals(nodeModel.getJpomUrl(), StrUtil.format("{}:{}", ip, port))).findAny();
        Assert.state(any.isPresent(), I18nMessageUtil.get("i18n.incorrect_ip_address.b872"));
        MachineNodeModel machineNodeModel = any.get();
        try {
            machineNodeServer.testNode(machineNodeModel);
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.test_result.8441"), machineNodeModel.getJpomUrl(), e.getMessage());
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.node_connection_failure_message.aacc") + e.getMessage());
        }
        String workspaceId = nodeService.getCheckUserWorkspace(request);

        MachineNodeModel existsMachine = machineNodeServer.getByUrl(machineNodeModel.getJpomUrl());
        if (existsMachine == null) {
            // 插入
            machineNodeServer.insertAndNode(machineNodeModel, workspaceId);
        } else {
            boolean exists = nodeService.existsNode2(workspaceId, existsMachine.getId());
            Assert.state(!exists, I18nMessageUtil.get("i18n.i18n_node_already_exists.632d"));
            machineNodeServer.insertNode(machineNodeModel, workspaceId);
        }
        // 更新结果
        receiveCache.put("type", "success");
        return JsonMessage.success(I18nMessageUtil.get("i18n.installation_success.811f"), NodeInfoController.listReceiveCache(null));
    }


}
