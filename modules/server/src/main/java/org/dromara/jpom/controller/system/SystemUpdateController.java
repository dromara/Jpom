/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.Type;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.*;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.dblog.BackupInfoService;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.dromara.jpom.system.ServerConfig;
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
    public IJsonMessage<JSONObject> info(HttpServletRequest request, String machineId) {
        IJsonMessage<JSONObject> message = this.tryRequestMachine(machineId, request, NodeUrl.Info);
        return Optional.ofNullable(message).orElseGet(() -> {
            JpomManifest instance = JpomManifest.getInstance();
            cn.keepbx.jpom.RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
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
    public IJsonMessage<String> changeBetaRelease(String beta) {
        boolean betaBool = this.changeBetaRelease2(beta);
        RemoteVersion.loadRemoteInfo();
        String isBeta = I18nMessageUtil.get("i18n.joined_beta_program.c4e2");
        String closeBeta = I18nMessageUtil.get("i18n.close_beta_plan_success.5a94");
        return JsonMessage.success(betaBool ? isBeta : closeBeta);
    }

    private boolean changeBetaRelease2(String beta) {
        boolean betaBool = BooleanUtil.toBoolean(beta);
        systemParametersServer.upsert(JOIN_JPOM_BETA_RELEASE, String.valueOf(betaBool), I18nMessageUtil.get("i18n.join_beta_program.5c1f"));
        RemoteVersion.changeBetaRelease(String.valueOf(betaBool));
        return betaBool;
    }

    /**
     * 更新日志
     *
     * @return changelog md
     */
    @PostMapping(value = "change_log", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> changeLog(HttpServletRequest request, String machineId) {
        boolean betaRelease = RemoteVersion.betaRelease();
        JsonMessage<String> message = this.tryRequestMachine(machineId, request, NodeUrl.CHANGE_LOG, "beta", String.valueOf(betaRelease));
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
    public IJsonMessage<String> uploadJarSharding(MultipartFile file,
                                                  String machineId,
                                                  String sliceId,
                                                  Integer totalSlice,
                                                  Integer nowSlice,
                                                  String fileSumMd5) throws IOException {
        MultipartHttpServletRequest multiRequest = getMultiRequest();
        if (StrUtil.isNotEmpty(machineId)) {
            MachineNodeModel model = machineNodeServer.getByKey(machineId);
            Assert.notNull(model, I18nMessageUtil.get("i18n.no_machine_found.c16c"));
            return NodeForward.requestMultipart(model, multiRequest, NodeUrl.SystemUploadJar);
        }
        String absolutePath = serverConfig.getUserTempPath().getAbsolutePath();
        this.uploadSharding(file, absolutePath, sliceId, totalSlice, nowSlice, fileSumMd5, "jar", "zip");
        return JsonMessage.success(I18nMessageUtil.get("i18n.upload_success.a769"));
    }

    @PostMapping(value = "upload-jar-sharding-merge", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> uploadJar(String sliceId,
                                          Integer totalSlice,
                                          String fileSumMd5,
                                          HttpServletRequest request,
                                          String machineId) throws IOException {
        JsonMessage<String> message = this.tryRequestMachine(machineId, request, NodeUrl.SystemUploadJarMerge);
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
        return JsonMessage.success(Const.UPGRADE_MSG.get());
    }

    /**
     * 检查是否存在新版本
     *
     * @return json
     * @see RemoteVersion
     */
    @PostMapping(value = "check_version.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<cn.keepbx.jpom.RemoteVersion> checkVersion(HttpServletRequest request,
                                                                   String machineId) {
        IJsonMessage<cn.keepbx.jpom.RemoteVersion> message = this.tryRequestMachine(machineId, request, NodeUrl.CHECK_VERSION);
        return Optional.ofNullable(message).orElseGet(() -> {
            cn.keepbx.jpom.RemoteVersion remoteVersion = RemoteVersion.loadRemoteInfo();
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
    public IJsonMessage<String> upgrade(HttpServletRequest request,
                                        String machineId) throws IOException {

        IJsonMessage<String> message = this.tryRequestMachine(machineId, request, NodeUrl.REMOTE_UPGRADE);
        return Optional.ofNullable(message).orElseGet(() -> {
            try {
                RemoteVersion.upgrade(JpomApplication.getInstance().getTempPath().getAbsolutePath(), objects -> backupInfoService.autoBackup());
            } catch (IOException e) {
                throw Lombok.sneakyThrow(e);
            }
            return JsonMessage.success(Const.UPGRADE_MSG.get());
        });
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        String config = systemParametersServer.getConfig(JOIN_JPOM_BETA_RELEASE, String.class);
        boolean release2 = this.changeBetaRelease2(config);
        log.debug("beta plan:{}", release2);
    }
}
