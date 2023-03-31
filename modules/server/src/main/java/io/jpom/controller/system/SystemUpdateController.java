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
package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.*;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.func.assets.model.MachineNodeModel;
import io.jpom.model.data.NodeModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.dblog.BackupInfoService;
import io.jpom.service.system.SystemParametersServer;
import io.jpom.system.ServerConfig;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

/**
 * 在线升级
 *
 * @author bwcx_jzy
 * @since 2019/7/22
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_UPGRADE)
@SystemPermission(superUser = true)
@Slf4j
public class SystemUpdateController extends BaseServerController implements ILoadEvent {

    private static final String JOIN_JPOM_BETA_RELEASE = "JOIN_JPOM_BETA_RELEASE";

    private final BackupInfoService backupInfoService;
    private final ServerConfig serverConfig;
    private final SystemParametersServer systemParametersServer;

    public SystemUpdateController(BackupInfoService backupInfoService,
                                  ServerConfig serverConfig,
                                  SystemParametersServer systemParametersServer) {
        this.backupInfoService = backupInfoService;
        this.serverConfig = serverConfig;
        this.systemParametersServer = systemParametersServer;
    }

    @PostMapping(value = "info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<JSONObject> info(HttpServletRequest request, String machineId) {
        JsonMessage<JSONObject> message = this.tryRequestNode(machineId, request, NodeUrl.Info);
        return Optional.ofNullable(message).orElseGet(() -> {
            JpomManifest instance = JpomManifest.getInstance();
            RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
            //
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("manifest", instance);
            jsonObject.put("remoteVersion", remoteVersion);
            jsonObject.put("joinBetaRelease", RemoteVersion.betaRelease());
            return JsonMessage.success("", jsonObject);
        });
    }

    @GetMapping(value = "change-beta-release", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<String> changeBetaRelease(String beta) {
        boolean betaBool = this.changeBetaRelease2(beta);
        RemoteVersion.loadRemoteInfo();
        return JsonMessage.success(betaBool ? "成功加入 beta 计划" : "关闭 beta 计划成功");
    }

    private boolean changeBetaRelease2(String beta) {
        boolean betaBool = BooleanUtil.toBoolean(beta);
        systemParametersServer.upsert(JOIN_JPOM_BETA_RELEASE, String.valueOf(betaBool), "是否加入 beta 计划");
        RemoteVersion.changeBetaRelease(String.valueOf(betaBool));
        return betaBool;
    }

    /**
     * 更新日志
     *
     * @return changelog md
     */
    @PostMapping(value = "change_log", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<String> changeLog(HttpServletRequest request, String machineId) {
        boolean betaRelease = RemoteVersion.betaRelease();
        JsonMessage<String> message = this.tryRequestNode(machineId, request, NodeUrl.CHANGE_LOG, "beta", String.valueOf(betaRelease));
        if (message != null) {
            return message;
        }
        //

        URL resource = ResourceUtil.getResource(betaRelease ? "CHANGELOG-BETA.md" : "CHANGELOG.md");
        String log = StrUtil.EMPTY;
        if (resource != null) {
            InputStream stream = URLUtil.getStream(resource);
            log = IoUtil.readUtf8(stream);
        }
        return JsonMessage.success("", log);
    }

    @PostMapping(value = "upload-jar-sharding", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE, log = false)
    public JsonMessage<String> uploadJarSharding(MultipartFile file,
                                                 String machineId,
                                                 String sliceId,
                                                 Integer totalSlice,
                                                 Integer nowSlice,
                                                 String fileSumMd5) throws IOException {
        MultipartHttpServletRequest multiRequest = getMultiRequest();
        NodeModel nodeModel = tryGetNode();
        if (nodeModel != null) {
            Assert.state(BaseServerController.SHARDING_IDS.containsKey(sliceId), "不合法的分片id");
            return NodeForward.requestMultipart(nodeModel, multiRequest, NodeUrl.SystemUploadJar);
        }
        if (StrUtil.isNotEmpty(machineId)) {
            MachineNodeModel model = machineNodeServer.getByKey(machineId);
            Assert.notNull(model, "没有找到对应的机器");
            return NodeForward.requestMultipart(model, multiRequest, NodeUrl.SystemUploadJar);
        }
        String absolutePath = serverConfig.getUserTempPath().getAbsolutePath();
        this.uploadSharding(file, absolutePath, sliceId, totalSlice, nowSlice, fileSumMd5, "jar", "zip");
        return JsonMessage.success("上传成功");
    }

    @PostMapping(value = "upload-jar-sharding-merge", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<String> uploadJar(String sliceId,
                                         Integer totalSlice,
                                         String fileSumMd5,
                                         HttpServletRequest request,
                                         String machineId) throws IOException {
        JsonMessage<String> message = this.tryRequestNode(machineId, request, NodeUrl.SystemUploadJarMerge);
        if (message != null) {
            // 判断-删除分片id
            BaseServerController.SHARDING_IDS.remove(sliceId);
            return message;
        }
        //
        String absolutePath = serverConfig.getUserTempPath().getAbsolutePath();
        File successFile = this.shardingTryMerge(absolutePath, sliceId, totalSlice, fileSumMd5);
        Objects.requireNonNull(JpomManifest.getScriptFile());
        String path = FileUtil.getAbsolutePath(successFile);
        // 解析压缩包
        File file = JpomManifest.zipFileFind(path, Type.Server, absolutePath);
        path = FileUtil.getAbsolutePath(file);
        // 基础检查
        JsonMessage<Tuple> error = JpomManifest.checkJpomJar(path, Type.Server);
        if (!error.success()) {
            return new JsonMessage<>(error.getCode(), error.getMsg());
        }
        Tuple data = error.getData();
        String version = data.get(0);
        JpomManifest.releaseJar(path, version);
        //
        backupInfoService.autoBackup();
        //
        JpomApplication.restart();
        return JsonMessage.success(Const.UPGRADE_MSG);
    }

    /**
     * 检查是否存在新版本
     *
     * @return json
     * @see RemoteVersion
     */
    @PostMapping(value = "check_version.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<RemoteVersion> checkVersion(HttpServletRequest request,
                                                   String machineId) {
        JsonMessage<RemoteVersion> message = this.tryRequestNode(machineId, request, NodeUrl.CHECK_VERSION);
        return Optional.ofNullable(message).orElseGet(() -> {
            RemoteVersion remoteVersion = RemoteVersion.loadRemoteInfo();
            return JsonMessage.success("", remoteVersion);
        });
    }

    /**
     * 远程下载升级
     *
     * @return json
     * @see RemoteVersion
     */
    @GetMapping(value = "remote_upgrade.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DOWNLOAD)
    public JsonMessage<String> upgrade(HttpServletRequest request,
                                       String machineId) throws IOException {

        JsonMessage<String> message = this.tryRequestNode(machineId, request, NodeUrl.REMOTE_UPGRADE);
        return Optional.ofNullable(message).orElseGet(() -> {
            try {
                RemoteVersion.upgrade(JpomApplication.getInstance().getTempPath().getAbsolutePath(), objects -> backupInfoService.autoBackup());
            } catch (IOException e) {
                throw Lombok.sneakyThrow(e);
            }
            return JsonMessage.success(Const.UPGRADE_MSG);
        });
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        String config = systemParametersServer.getConfig(JOIN_JPOM_BETA_RELEASE, String.class);
        boolean release2 = this.changeBetaRelease2(config);
        log.debug("beta plan:{}", release2);
    }
}
