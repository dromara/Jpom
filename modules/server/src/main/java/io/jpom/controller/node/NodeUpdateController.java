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
package io.jpom.controller.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.*;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.controller.openapi.NodeInfoController;
import io.jpom.func.assets.model.MachineNodeModel;
import io.jpom.func.assets.server.MachineNodeServer;
import io.jpom.model.AgentFileModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.system.SystemParametersServer;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.ServerConfig;
import lombok.extern.slf4j.Slf4j;
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
    private final MachineNodeServer machineNodeServer;

    public NodeUpdateController(SystemParametersServer systemParametersServer,
                                ServerConfig serverConfig,
                                MachineNodeServer machineNodeServer) {
        this.systemParametersServer = systemParametersServer;
        this.serverConfig = serverConfig;
        this.machineNodeServer = machineNodeServer;
    }

    /**
     * 远程下载
     *
     * @return json
     * @see RemoteVersion
     */
    @GetMapping(value = "download_remote.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.REMOTE_DOWNLOAD)
    public JsonMessage<String> downloadRemote() throws IOException {
        String saveDir = serverConfig.getAgentPath().getAbsolutePath();
        Tuple download = RemoteVersion.download(saveDir, Type.Agent, false);
        // 保存文件
        this.saveAgentFile(download);
        return JsonMessage.success("下载成功");
    }

    /**
     * 检查版本更新
     *
     * @return json
     * @see RemoteVersion
     * @see AgentFileModel
     */
    @GetMapping(value = "check_version.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> checkVersion() {
        RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
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
    public JsonMessage<String> uploadAgentSharding(MultipartFile file,
                                                   String sliceId,
                                                   Integer totalSlice,
                                                   Integer nowSlice,
                                                   String fileSumMd5) throws IOException {
        File userTempPath = serverConfig.getUserTempPath();
        this.uploadSharding(file, userTempPath.getAbsolutePath(), sliceId, totalSlice, nowSlice, fileSumMd5, "jar", "zip");
        return JsonMessage.success("上传成功");
    }

    @RequestMapping(value = "upload-agent-sharding-merge", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    @Feature(method = MethodFeature.UPLOAD)
    public JsonMessage<String> uploadAgent(String sliceId,
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
        return JsonMessage.success("上传成功");
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
    public JsonMessage<JSONObject> fastInstall() {
        InputStream inputStream = ExtConfigBean.getConfigResourceInputStream("/fast-install-info.json");
        String json = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
        JSONObject jsonObject = new JSONObject();
        JpomManifest instance = JpomManifest.getInstance();
        jsonObject.put("token", instance.randomIdSign());
        jsonObject.put("key", ServerOpenApi.PUSH_NODE_KEY);
        //
        JSONArray jsonArray = JSONArray.parseArray(json);
        jsonObject.put("shUrls", jsonArray);
        //
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(getRequest(), ServerConst.PROXY_PATH);
        String url = String.format("/%s/%s", contextPath, ServerOpenApi.RECEIVE_PUSH);
        jsonObject.put("url", FileUtil.normalize(url));
        return JsonMessage.success("", jsonObject);
    }

    @GetMapping(value = "pull_fast_install_result.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<Collection<JSONObject>> pullFastInstallResult(String removeId) {
        Collection<JSONObject> jsonObjects = NodeInfoController.listReceiveCache(removeId);
        jsonObjects = jsonObjects.stream().map(jsonObject -> {
            JSONObject clone = jsonObject.clone();
            clone.remove("canUseNode");
            return clone;
        }).collect(Collectors.toList());
        return JsonMessage.success("", jsonObjects);
    }

    @GetMapping(value = "confirm_fast_install.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<Collection<JSONObject>> confirmFastInstall(HttpServletRequest request,
                                                                  @ValidatorItem String id,
                                                                  @ValidatorItem String ip,
                                                                  int port) {
        JSONObject receiveCache = NodeInfoController.getReceiveCache(id);
        Assert.notNull(receiveCache, "没有对应的缓存信息");
        JSONArray jsonArray = receiveCache.getJSONArray("canUseNode");
        Assert.notEmpty(jsonArray, "没有对应的缓存信息：-1");
        Optional<MachineNodeModel> any = jsonArray.stream().map(o -> {
            if (o instanceof MachineNodeModel) {
                return (MachineNodeModel) o;
            }
            JSONObject jsonObject = (JSONObject) o;
            return jsonObject.toJavaObject(MachineNodeModel.class);
        }).filter(nodeModel -> StrUtil.equals(nodeModel.getJpomUrl(), StrUtil.format("{}:{}", ip, port))).findAny();
        Assert.state(any.isPresent(), "ip 地址信息不正确");
        MachineNodeModel machineNodeModel = any.get();
        try {
            machineNodeServer.testNode(machineNodeModel);
        } catch (Exception e) {
            log.warn("测试结果：{} {}", machineNodeModel.getJpomUrl(), e.getMessage());
            return new JsonMessage<>(500, "节点连接失败：" + e.getMessage());
        }
        String workspaceId = nodeService.getCheckUserWorkspace(request);

        MachineNodeModel existsMachine = machineNodeServer.getByUrl(machineNodeModel.getJpomUrl());
        if (existsMachine == null) {
            // 插入
            machineNodeServer.insertAndNode(machineNodeModel, workspaceId);
        } else {
            boolean exists = nodeService.existsNode2(workspaceId, existsMachine.getId());
            Assert.state(!exists, "对应的节点已经存在拉");
            machineNodeServer.insertNode(machineNodeModel, workspaceId);
        }
        // 更新结果
        receiveCache.put("type", "success");
        return JsonMessage.success("安装成功", NodeInfoController.listReceiveCache(null));
    }


}
