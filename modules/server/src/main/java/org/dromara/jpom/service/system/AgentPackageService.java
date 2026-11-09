/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import cn.keepbx.jpom.Type;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.RemoteVersion;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.update.AgentEngine;
import org.dromara.jpom.common.update.AgentPackageInfo;
import org.dromara.jpom.common.update.AgentPackageResolver;
import org.dromara.jpom.model.AgentFileModel;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 插件端在线升级包管理
 *
 * <p>Java 版插件端继续使用系统参数 AgentFileModel.ID（保持原有行为）；Go 版插件端使用独立的
 * 系统参数 key（{@link #GO_ID}，值为同样结构的 AgentFileModel），不改数据库表结构，
 * 从而让 Java/Go 两种升级包可以在同一个 Server 实例上共存（混合集群）。</p>
 *
 * <p>Go 升级包来源：用户通过现有 /node/upload-agent-sharding* 上传（按包内容自动归入
 * {@link #GO_ID}），或在升级时从下载中心同版本目录自动下载官方升级容器
 * agent-go-&lt;version&gt;-upgrade.jar。</p>
 *
 * @author jpom
 * @since 2.11.13
 */
@Slf4j
@Service
public class AgentPackageService {

    /**
     * Go 版插件端升级包的系统参数 key（值为 AgentFileModel JSON，不新增数据库表/字段）
     */
    public static final String GO_ID = "AGENT_GO_FILE";
    /**
     * 下载错误信息中的授权 token 脱敏
     */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("([?&](?:token|access_token|auth)=)[^&\\s]+", Pattern.CASE_INSENSITIVE);

    private final SystemParametersServer systemParametersServer;
    private final ServerConfig serverConfig;

    public AgentPackageService(SystemParametersServer systemParametersServer, ServerConfig serverConfig) {
        this.systemParametersServer = systemParametersServer;
        this.serverConfig = serverConfig;
    }

    /**
     * 读取已上传的升级包（同时校验文件存在）
     *
     * @param engine 引擎
     * @return 升级包参数，不存在返回 null
     */
    public AgentFileModel get(AgentEngine engine) {
        return this.get(engine, true);
    }

    /**
     * 读取已上传的升级包
     *
     * @param engine        引擎
     * @param checkFileExist 是否校验文件存在
     * @return 升级包参数，不存在返回 null
     */
    public AgentFileModel get(AgentEngine engine, boolean checkFileExist) {
        String id = engine == AgentEngine.GO ? GO_ID : AgentFileModel.ID;
        return systemParametersServer.getConfig(id, AgentFileModel.class, model -> {
            if (model == null || StrUtil.isBlank(model.getSavePath())) {
                return null;
            }
            if (checkFileExist && !FileUtil.exist(model.getSavePath())) {
                return null;
            }
            return model;
        });
    }

    /**
     * 保存升级包信息，并清理同引擎的历史包（保留另一引擎正在使用的包）
     *
     * @param engine    引擎
     * @param file      升级包文件（已经位于 server 的 agent 目录）
     * @param version   版本号
     * @param timeStamp 打包时间
     * @return 升级包参数
     */
    public AgentFileModel save(AgentEngine engine, File file, String version, String timeStamp) {
        AgentFileModel agentFileModel = new AgentFileModel();
        agentFileModel.setName(file.getName());
        agentFileModel.setSize(file.length());
        agentFileModel.setSavePath(FileUtil.getAbsolutePath(file));
        agentFileModel.setVersion(AgentPackageResolver.normalizeVersion(version));
        agentFileModel.setTimeStamp(timeStamp);
        String id = engine == AgentEngine.GO ? GO_ID : AgentFileModel.ID;
        systemParametersServer.upsert(id, agentFileModel, id);
        this.cleanOldFiles(engine, file);
        return agentFileModel;
    }

    /**
     * 从下载中心自动下载官方 Go 升级容器
     *
     * <p>下载地址由 Java 插件端下载地址（RemoteVersion.agentUrl，已经过授权 token 替换）
     * 同目录推导：agent-go-&lt;version&gt;-upgrade.jar；存在 .sha1 时校验完整性。
     * 同名（同样版本）的本地包已存在时直接复用，避免重复下载。</p>
     *
     * @return Go 升级包参数
     */
    public synchronized AgentFileModel downloadGoPackage() {
        cn.keepbx.jpom.RemoteVersion remoteVersion = RemoteVersion.loadRemoteInfo();
        if (remoteVersion == null) {
            throw new IllegalStateException(I18nMessageUtil.get("i18n.no_available_new_version_upgrade.d8f2"));
        }
        String version = AgentPackageResolver.normalizeVersion(remoteVersion.getTagName());
        String agentUrl = this.resolveJavaAgentUrl(remoteVersion);
        if (StrUtil.isBlank(version)) {
            version = AgentPackageResolver.parseVersionInUrl(agentUrl);
        }
        String goUrl = AgentPackageResolver.buildGoUpgradeUrl(agentUrl, version);
        if (StrUtil.isBlank(goUrl)) {
            throw new IllegalStateException(I18nMessageUtil.get("i18n.new_version_exists_download_unavailable.4ba7"));
        }
        // 同版本已经下载过则直接复用
        AgentFileModel exists = this.get(AgentEngine.GO);
        if (exists != null && StrUtil.equals(version, AgentPackageResolver.normalizeVersion(exists.getVersion()))) {
            return exists;
        }
        File agentPath = serverConfig.getAgentPath();
        File downloadFile;
        try {
            downloadFile = HttpUtil.downloadFileFromUrl(goUrl, agentPath);
        } catch (HttpException e) {
            String message = this.sanitize(e.getMessage());
            log.error("{}{}", I18nMessageUtil.get("i18n.download_failed.65e2"), message);
            throw new IllegalStateException(I18nMessageUtil.get("i18n.download_failed.65e2") + message);
        }
        try {
            this.verifyDownloaded(version, goUrl, downloadFile);
            File target = FileUtil.file(agentPath, AgentPackageResolver.goUpgradeFileName(version));
            if (!FileUtil.equals(target, downloadFile)) {
                FileUtil.move(downloadFile, target, true);
            }
            AgentPackageInfo info = AgentPackageResolver.inspectPackage(target);
            return this.save(AgentEngine.GO, target, info.getVersion(), info.getTimeStamp());
        } catch (RuntimeException | IOException e) {
            FileUtil.del(downloadFile);
            throw e instanceof IllegalStateException ? (IllegalStateException) e
                : new IllegalStateException(I18nMessageUtil.get("i18n.agent_jar_damaged.74a8") + e.getMessage());
        }
    }

    /**
     * 校验下载的 Go 升级容器
     *
     * @param version      期望版本
     * @param goUrl        下载地址（用于推导 .sha1）
     * @param downloadFile 下载的文件
     * @throws IOException IO
     */
    private void verifyDownloaded(String version, String goUrl, File downloadFile) throws IOException {
        this.verifyRemoteSha1(goUrl, downloadFile);
        // Go 升级容器按容器结构校验（不依赖旧 Server 的 checkJpomJar：容器空壳 Main-Class 位于 BOOT-INF/classes）
        String containerError = AgentPackageResolver.validateGoContainer(downloadFile);
        if (containerError != null) {
            throw new IllegalStateException(StrUtil.format(I18nMessageUtil.get("i18n.invalid_go_agent_upgrade_container.0755"), containerError));
        }
        AgentPackageInfo info = AgentPackageResolver.inspectPackage(downloadFile);
        if (StrUtil.isBlank(info.getVersion())) {
            throw new IllegalStateException(StrUtil.format(I18nMessageUtil.get("i18n.invalid_go_agent_upgrade_container.0755"), downloadFile.getName()));
        }
        if (StrUtil.isNotBlank(version) && !StrUtil.equals(version, info.getVersion())) {
            throw new IllegalStateException(StrUtil.format(I18nMessageUtil.get("i18n.invalid_go_agent_upgrade_container.0755"),
                downloadFile.getName() + " " + info.getVersion() + " != " + version));
        }
    }

    /**
     * 下载中心存在 .sha1 时校验下载文件完整性（不存在则跳过，Go Agent 端仍会校验可执行文件 sha256）
     *
     * @param goUrl        下载地址
     * @param downloadFile 下载的文件
     */
    private void verifyRemoteSha1(String goUrl, File downloadFile) {
        int queryIndex = goUrl.indexOf('?');
        String path = queryIndex >= 0 ? goUrl.substring(0, queryIndex) : goUrl;
        String query = queryIndex >= 0 ? goUrl.substring(queryIndex) : StrUtil.EMPTY;
        String sha1Url = path + ".sha1" + query;
        String expected;
        try {
            expected = HttpUtil.get(sha1Url, 10 * 1000);
        } catch (Exception e) {
            // .sha1 缺失或下载失败不阻断升级（Go Agent 端仍会校验容器内可执行文件 sha256）
            log.warn("{}{}", I18nMessageUtil.get("i18n.download_failed.65e2"), this.sanitize(e.getMessage()));
            return;
        }
        expected = StrUtil.trimToNull(expected);
        if (expected == null) {
            return;
        }
        expected = StrUtil.trim(StrUtil.subBefore(expected, " ", false));
        String actual = SecureUtil.sha1(downloadFile);
        if (!StrUtil.equalsIgnoreCase(expected, actual)) {
            throw new IllegalStateException(I18nMessageUtil.get("i18n.agent_jar_damaged.74a8") + "sha1 " + actual + " != " + expected);
        }
    }

    private String resolveJavaAgentUrl(cn.keepbx.jpom.RemoteVersion remoteVersion) {
        try {
            String url = Type.Agent.getRemoteUrl(remoteVersion);
            if (StrUtil.isNotBlank(url)) {
                return url;
            }
        } catch (Exception e) {
            log.warn("{}{}", I18nMessageUtil.get("i18n.download_failed.65e2"), this.sanitize(e.getMessage()));
        }
        return remoteVersion.getAgentUrl();
    }

    private void cleanOldFiles(AgentEngine engine, File keep) {
        File agentPath = serverConfig.getAgentPath();
        AgentFileModel other = this.get(engine == AgentEngine.GO ? AgentEngine.JAVA : AgentEngine.GO, false);
        File otherFile = other == null || StrUtil.isBlank(other.getSavePath()) ? null : FileUtil.file(other.getSavePath());
        List<File> files = FileUtil.loopFiles(agentPath, pathname -> FileUtil.isFile(pathname)
            && !FileUtil.equals(pathname, keep)
            && (otherFile == null || !FileUtil.equals(pathname, otherFile)));
        for (File file : files) {
            FileUtil.del(file);
        }
    }

    private String sanitize(String message) {
        if (StrUtil.isBlank(message)) {
            return StrUtil.EMPTY;
        }
        return TOKEN_PATTERN.matcher(message).replaceAll("$1***");
    }
}
