/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.update;

import cn.keepbx.jpom.RemoteVersion;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 版本清单里 Go 插件端地址的解析回归（实现位于 jpom-core 的 {@link RemoteVersion}）。
 *
 * <p>背景：清单新增 `goAgentUrls`/`goUpgradeUrls`（按平台）与带 `{token}` 的 `auth.` 同名节点，
 * 服务端下载 Go 升级容器时优先用清单地址，读不到才回退「Java 插件端地址同目录推导」。
 * 这里锁定解析口径：auth 优先且替换 token、平台 key 兼容 os/arch 与 os-arch、缺字段返回 null。</p>
 *
 * @author jpom
 * @since 2.11.15.4
 */
public class RemoteVersionGoUrlTest {

    private static final String AUTH_KEY = "JPOM_REMOTE_VERSION_AUTH";

    @AfterEach
    public void clearToken() {
        System.clearProperty(AUTH_KEY);
    }

    private static RemoteVersion manifest() {
        JSONObject root = new JSONObject();
        JSONObject goUpgradeUrls = new JSONObject();
        goUpgradeUrls.put("linux/amd64", "https://d2.example.com/beta/1.0.0/agent-go-1.0.0-upgrade-linux-amd64.jar");
        goUpgradeUrls.put("linux-arm64", "https://d2.example.com/beta/1.0.0/agent-go-1.0.0-upgrade-linux-arm64.jar");
        root.put("goUpgradeUrls", goUpgradeUrls);
        JSONObject goAgentUrls = new JSONObject();
        goAgentUrls.put("linux/amd64", "https://d2.example.com/beta/1.0.0/agent-go-1.0.0-release-linux-amd64.tar.gz");
        root.put("goAgentUrls", goAgentUrls);
        JSONObject auth = new JSONObject();
        JSONObject authGo = new JSONObject();
        authGo.put("linux/amd64", "https://download.example.com/beta/1.0.0/agent-go-1.0.0-upgrade-linux-amd64.jar?token={token}");
        auth.put("goUpgradeUrls", authGo);
        root.put("auth", auth);
        return root.to(RemoteVersion.class);
    }

    @Test
    public void preferAuthUrlAndReplaceToken() {
        System.setProperty(AUTH_KEY, "abc123");
        String url = manifest().getGoUpgradeUrl("linux/amd64");
        Assertions.assertEquals("https://download.example.com/beta/1.0.0/agent-go-1.0.0-upgrade-linux-amd64.jar?token=abc123", url);
    }

    @Test
    public void fallbackToTopLevelWhenAuthMissingPlatform() {
        System.setProperty(AUTH_KEY, "abc123");
        String url = manifest().getGoUpgradeUrl("linux/arm64");
        Assertions.assertEquals("https://d2.example.com/beta/1.0.0/agent-go-1.0.0-upgrade-linux-arm64.jar", url);
    }

    @Test
    public void ignoreAuthWithoutToken() {
        String url = manifest().getGoUpgradeUrl("linux/amd64");
        Assertions.assertEquals("https://d2.example.com/beta/1.0.0/agent-go-1.0.0-upgrade-linux-amd64.jar", url);
    }

    @Test
    public void acceptDashStylePlatformKey() {
        JSONObject root = new JSONObject();
        JSONObject urls = new JSONObject();
        urls.put("linux-amd64", "https://example.com/agent-go-upgrade-linux-amd64.jar");
        root.put("goUpgradeUrls", urls);
        Assertions.assertEquals("https://example.com/agent-go-upgrade-linux-amd64.jar",
            root.to(RemoteVersion.class).getGoUpgradeUrl("linux/amd64"));
        Assertions.assertEquals("linux/amd64", RemoteVersion.normalizePlatformKey(" Linux-AMD64 "));
    }

    @Test
    public void installPackageUrlAndMissingPlatform() {
        RemoteVersion remoteVersion = manifest();
        Assertions.assertEquals("https://d2.example.com/beta/1.0.0/agent-go-1.0.0-release-linux-amd64.tar.gz",
            remoteVersion.getGoAgentUrl("linux/amd64"));
        Assertions.assertNull(remoteVersion.getGoUpgradeUrl("windows/amd64"));
        Assertions.assertNull(remoteVersion.getGoUpgradeUrl(null));
        Assertions.assertNull(new RemoteVersion().getGoUpgradeUrl("linux/amd64"));
    }
}
