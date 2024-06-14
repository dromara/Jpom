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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.Type;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.RemoteVersion;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.AgentConfig;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * 在线升级
 *
 * @author bwcx_jzy
 * @since 2019/7/22
 */
@RestController
@RequestMapping(value = "system")
public class SystemUpdateController extends BaseAgentController {

    private final AgentConfig agentConfig;
    private final JpomApplication configBean;

    public SystemUpdateController(AgentConfig agentConfig,
                                  JpomApplication configBean) {
        this.agentConfig = agentConfig;
        this.configBean = configBean;
    }

    @PostMapping(value = "upload-jar-sharding", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> uploadJarSharding(MultipartFile file, String sliceId,
                                                  Integer totalSlice,
                                                  Integer nowSlice,
                                                  String fileSumMd5) throws IOException {
        //
        String tempPathName = agentConfig.getFixedTempPathName();
        this.uploadSharding(file, tempPathName, sliceId, totalSlice, nowSlice, fileSumMd5, "jar", "zip");
        return JsonMessage.success(I18nMessageUtil.get("i18n.upload_success.a769"));
    }

    @PostMapping(value = "upload-jar-sharding-merge", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> uploadJarShardingMerge(String sliceId,
                                                      Integer totalSlice,
                                                      String fileSumMd5) throws IOException {
        //
        String tempPathName = agentConfig.getFixedTempPathName();
        File successFile = this.shardingTryMerge(tempPathName, sliceId, totalSlice, fileSumMd5);
        Objects.requireNonNull(JpomManifest.getScriptFile());
        String absolutePath = agentConfig.getTempPath().getAbsolutePath();
        String path = FileUtil.getAbsolutePath(successFile);
        // 解析压缩包
        File file = JpomManifest.zipFileFind(path, Type.Agent, absolutePath);
        path = FileUtil.getAbsolutePath(file);
        // 基础检查
        JsonMessage<Tuple> error = JpomManifest.checkJpomJar(path, Type.Agent);
        if (!error.success()) {
            return new JsonMessage<>(error.getCode(), error.getMsg());
        }
        Tuple data = error.getData();
        String version = data.get(0);
        JpomManifest.releaseJar(path, version);
        //
        JpomApplication.restart();
        return JsonMessage.success(Const.UPGRADE_MSG.get());
    }

    @PostMapping(value = "change_log", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> changeLog(String beta) {
        //
        boolean betaBool = Convert.toBool(beta, false);
        boolean betaRelease = RemoteVersion.betaRelease();
        URL resource = ResourceUtil.getResource((betaRelease || betaBool) ? "CHANGELOG-BETA.md" : "CHANGELOG.md");
        String log = StrUtil.EMPTY;
        if (resource != null) {
            InputStream stream = URLUtil.getStream(resource);
            log = IoUtil.readUtf8(stream);
        }
        return JsonMessage.success("", log);
    }

    /**
     * 检查是否存在新版本
     *
     * @return json
     * @see RemoteVersion
     */
    @PostMapping(value = "check_version.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<cn.keepbx.jpom.RemoteVersion> checkVersion() {
        cn.keepbx.jpom.RemoteVersion remoteVersion = RemoteVersion.loadRemoteInfo();
        return JsonMessage.success("", remoteVersion);
    }

    /**
     * 远程下载升级
     *
     * @return json
     * @see RemoteVersion
     */
    @PostMapping(value = "remote_upgrade.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> upgrade() throws IOException {
        RemoteVersion.upgrade(configBean.getTempPath().getAbsolutePath());
        return JsonMessage.success(Const.UPGRADE_MSG.get());
    }
}
