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
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import cn.keepbx.jpom.Type;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

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
        cn.keepbx.jpom.RemoteVersion remoteVersion = loadRemoteInfo();
        Assert.notNull(remoteVersion, I18nMessageUtil.get("i18n.no_available_new_version_upgrade.d8f2"));
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
            } catch (IllegalStateException e) {
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
        } catch (HttpException httpException) {
            String message = httpException.getMessage();
            if (StrUtil.containsAnyIgnoreCase(message, "Server response error with status code: [403]")) {
                String msg = I18nMessageUtil.get("i18n.error_message.44ce");
                throw new IllegalStateException(message + " " + msg);
            }
            throw Lombok.sneakyThrow(httpException);
        }
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
