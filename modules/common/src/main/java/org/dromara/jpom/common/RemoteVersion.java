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
import cn.hutool.http.HttpUtil;
import cn.keepbx.jpom.Type;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
     */
    public static Tuple download(String savePath, Type type, boolean checkRepeat) throws IOException {
        cn.keepbx.jpom.RemoteVersion remoteVersion = loadRemoteInfo();
        Assert.notNull(remoteVersion, I18nMessageUtil.get("i18n.no_available_new_version_upgrade.d8f2"));
        // 检查是否存在下载地址
        String remoteUrl = type.getRemoteUrl(remoteVersion);
        Assert.hasText(remoteUrl, I18nMessageUtil.get("i18n.new_version_exists_download_unavailable.4ba7"));
        // 下载
        File downloadFileFromUrl = HttpUtil.downloadFileFromUrl(remoteUrl, savePath);
        // 解析压缩包
        File file = JpomManifest.zipFileFind(FileUtil.getAbsolutePath(downloadFileFromUrl), type, savePath);
        // 检查
        JsonMessage<Tuple> error = JpomManifest.checkJpomJar(FileUtil.getAbsolutePath(file), type, checkRepeat);
        Assert.state(error.success(), error.getMsg());
        return error.getData();
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
