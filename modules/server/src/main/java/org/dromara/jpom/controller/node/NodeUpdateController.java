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
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.openapi.controller.NodeInfoController;
import org.dromara.jpom.model.AgentFileModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
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

    public NodeUpdateController(SystemParametersServer systemParametersServer,
                                ServerConfig serverConfig) {
        this.systemParametersServer = systemParametersServer;
        this.serverConfig = serverConfig;
    }

    /**
     * 远程下载
     *
     * @return json
     * @see RemoteVersion
     */
    @GetMapping(value = "download_remote.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.REMOTE_DOWNLOAD)
    public IJsonMessage<String> downloadRemote() throws IOException {
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
    @GetMapping(value = "check_version.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> checkVersion() {
        cn.keepbx.jpom.RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
        AgentFileModel agentFileModel = systemParametersServer.getConfig(AgentFileModel.ID, AgentFileModel.class, agentFileModel1 -> {
            if (agentFileModel1 == null || !FileUtil.exist(agentFileModel1.getSavePath())) {
                return null;
            }
            return agentFileModel1;
        });
        JSONObject jsonObject = new JSONObject();
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
        this.uploadSharding(file, userTempPath.getAbsolutePath(), sliceId, totalSlice, nowSlice, fileSumMd5, "jar", "zip");
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
        // 基础检查
        JsonMessage<Tuple> error = JpomManifest.checkJpomJar(path, Type.Agent, false);
        if (!error.success()) {
            FileUtil.del(path);
            return new JsonMessage<>(error.getCode(), error.getMsg());
        }
        // 保存文件
        this.saveAgentFile(error.getData());
        return JsonMessage.success(I18nMessageUtil.get("i18n.upload_success.a769"));
    }

    private void saveAgentFile(Tuple data) {
        File file = data.get(3);
        AgentFileModel agentFileModel = new AgentFileModel();
        agentFileModel.setName(file.getName());
        agentFileModel.setSize(file.length());
        agentFileModel.setSavePath(FileUtil.getAbsolutePath(file));
        //
        agentFileModel.setVersion(data.get(0));
        agentFileModel.setTimeStamp(data.get(1));
        systemParametersServer.upsert(AgentFileModel.ID, agentFileModel, AgentFileModel.ID);
        // 删除历史包  @author jzy 2021-08-03
        String saveDir = serverConfig.getAgentPath().getAbsolutePath();
        List<File> files = FileUtil.loopFiles(saveDir, pathname -> !FileUtil.equals(pathname, file));
        for (File file1 : files) {
            FileUtil.del(file1);
        }
    }

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
        return JsonMessage.success("", jsonObject);
    }

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
