/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpDownloader;
import cn.hutool.http.HttpUtil;
import cn.keepbx.jpom.Type;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.version.RemoteVersions;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * 远程的版本信息
 *
 * <pre>
 * {
 * "tag_name": "v2.6.4",
 * "agentUrl": "",
 * "serverUrl": "",
 * "changelog": ""
 * }
 * </pre>
 *
 * @author bwcx_jzy
 * @since 2021/9/19
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class RemoteVersion extends cn.keepbx.jpom.RemoteVersion {

    /**
     * 下载地址中的授权 token 脱敏规则（与 AgentPackageService 同口径）
     */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("([?&](?:token|access_token|auth)=)[^&\\s]+", Pattern.CASE_INSENSITIVE);

    @Override
    public String toString() {
        return JSONObject.toJSONString(cn.keepbx.jpom.RemoteVersion.cacheInfo());
    }

    /**
     * 下载
     *
     * @param savePath    下载文件保存路径
     * @param type        类型
     * @param checkRepeat 是否验证重复
     * @return 保存的全路径
     * @throws IOException 异常
     * @see HttpDownloader#requestDownload(String, int)
     */
    public static Tuple download(String savePath, Type type, boolean checkRepeat) throws IOException {
        // 远端版本信息带刷新语义：在线刷新失败自动重试，仍失败回退本机缓存，都没有才抛可读异常
        //（此前只拉一次，下载中心 TLS 抖动一次就让升级/下载直接 405，且文案是「:-1」）
        cn.keepbx.jpom.RemoteVersion remoteVersion = RemoteVersions.infoForUpgrade();
        // 检查是否存在下载地址
        String remoteUrl = type.getRemoteUrl(remoteVersion);
        Assert.hasText(remoteUrl, I18nMessageUtil.get("i18n.new_version_exists_download_unavailable.4ba7"));
        // 下载：2.11.13.2 起发布包同时提供 tar.gz（2.12.0 起仅 tar.gz），优先同目录同名 tar.gz，下载失败回退原 zip 地址
        File downloadFileFromUrl = downloadPackage(remoteUrl, savePath);
        // 解析压缩包
        File file = JpomManifest.zipFileFind(FileUtil.getAbsolutePath(downloadFileFromUrl), type, savePath);
        // 检查
        JsonMessage<Tuple> error = JpomManifest.checkJpomJar(FileUtil.getAbsolutePath(file), type, checkRepeat);
        Assert.state(error.success(), error.getMsg());
        return error.getData();
    }

    /**
     * 下载发布包：优先尝试 tar.gz，失败（404/网络异常等）回退原始下载地址
     *
     * @param remoteUrl 原始下载地址（当前下载中心配置为 .zip）
     * @param savePath  下载文件保存路径
     * @return 下载保存的文件
     */
    private static File downloadPackage(String remoteUrl, String savePath) {
        String tarGzUrl = tarGzUrlOf(remoteUrl);
        if (!StrUtil.equals(tarGzUrl, remoteUrl)) {
            try {
                log.debug("try download tar.gz package: {}", tarGzUrl);
                return downloadFileFromUrl(tarGzUrl, savePath);
            } catch (DownloadAuthorizeException e) {
                // 授权失败（403 等）不降级，直接抛出原有提示
                throw e;
            } catch (Exception e) {
                log.debug("download tar.gz package failed, fallback to original url: {} ({})", tarGzUrl, e.getMessage());
            }
        }
        return downloadFileFromUrl(remoteUrl, savePath);
    }

    /**
     * 下载单个文件，保持原有 403 授权错误提示
     *
     * @param remoteUrl 下载地址
     * @param savePath  下载文件保存路径
     * @return 下载保存的文件
     */
    private static File downloadFileFromUrl(String remoteUrl, String savePath) {
        try {
            return HttpUtil.downloadFileFromUrl(remoteUrl, savePath);
        } catch (RuntimeException e) {
            // HttpException（非 2xx）与 IORuntimeException（连接失败/超时）都要落到可读提示
            String message = sanitizeDownloadMessage(e.getMessage());
            if (StrUtil.containsAnyIgnoreCase(message, "Server response error with status code: [403]")) {
                String msg = I18nMessageUtil.get("i18n.error_message.44ce");
                throw new DownloadAuthorizeException(message + " " + msg);
            }
            // 此前非 403 走 sneakyThrow，响应只有「服务异常：ConnectException: ...」，用户看不出下一步该做什么
            throw new DownloadFailedException(I18nMessageUtil.get("i18n.download_failed.65e2") + ": " + message, e);
        }
    }

    /**
     * 升级包下载失败（网络不可达、超时、非 2xx）：message 已脱敏，可直接展示给用户
     */
    public static class DownloadFailedException extends IllegalStateException {
        public DownloadFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 下载授权失败（403 等）：调用方不得再降级到备用下载地址
     */
    public static class DownloadAuthorizeException extends IllegalStateException {
        public DownloadAuthorizeException(String message) {
            super(message);
        }
    }

    /**
     * 下载地址可能带授权 token，异常信息回显前统一脱敏：日志、界面提示、上报字符串都要过这里
     *
     * @param message 原始异常信息
     * @return 脱敏后的信息
     */
    static String sanitizeDownloadMessage(String message) {
        if (StrUtil.isBlank(message)) {
            return StrUtil.EMPTY;
        }
        return TOKEN_PATTERN.matcher(message).replaceAll("$1***");
    }

    /**
     * 计算 tar.gz 下载地址：把下载地址的 .zip 后缀替换为 .tar.gz（保留 ?token=... 查询串）。
     * <p>
     * 地址已经是 .tar.gz/.tgz 或不是 .zip 时原样返回，便于过渡期与 2.12.0 之后统一处理。
     *
     * @param remoteUrl 原始下载地址
     * @return tar.gz 下载地址
     */
    static String tarGzUrlOf(String remoteUrl) {
        if (StrUtil.isBlank(remoteUrl)) {
            return remoteUrl;
        }
        int queryIndex = remoteUrl.indexOf('?');
        String pathPart = queryIndex >= 0 ? remoteUrl.substring(0, queryIndex) : remoteUrl;
        String queryPart = queryIndex >= 0 ? remoteUrl.substring(queryIndex) : StrUtil.EMPTY;
        if (StrUtil.endWithAnyIgnoreCase(pathPart, ".tar.gz", ".tgz")) {
            return remoteUrl;
        }
        if (StrUtil.endWithIgnoreCase(pathPart, ".zip")) {
            return pathPart.substring(0, pathPart.length() - 4) + ".tar.gz" + queryPart;
        }
        return remoteUrl;
    }

    /**
     * 下载
     *
     * @param savePath 下载文件保存路径
     * @param type     类型
     * @return 保存的全路径
     * @throws IOException 异常
     */
    public static Tuple download(String savePath, Type type) throws IOException {
        return download(savePath, type, true);
    }

    /**
     * 升级
     *
     * @param savePath 下载文件保存路径
     * @throws IOException 异常
     */
    public static void upgrade(String savePath) throws IOException {
        upgrade(savePath, null);
    }

    /**
     * 升级
     *
     * @param savePath 下载文件保存路径
     * @param consumer 执行申请前回调
     * @throws IOException 异常
     */
    public static void upgrade(String savePath, Consumer<Tuple> consumer) throws IOException {
        Type type = JpomManifest.getInstance().getType();
        // 下载
        Tuple data = download(savePath, type);
        File file = data.get(3);
        // 基础检查
        String path = FileUtil.getAbsolutePath(file);
        String version = data.get(0);
        JpomManifest.releaseJar(path, version);
        //
        if (consumer != null) {
            consumer.accept(data);
        }
        JpomApplication.restart();
    }
}
