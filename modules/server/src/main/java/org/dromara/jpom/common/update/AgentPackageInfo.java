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

import lombok.Data;

/**
 * 插件端在线升级包信息
 *
 * <p>Java 版插件端升级包信息来自 jar 的 MANIFEST；Go 版插件端升级容器（jar/zip 运输容器）
 * 信息来自容器内的 go-agent/agent.json，同时 MANIFEST 仍然保留用于兼容旧版 Server 的
 * JpomManifest#checkJpomJar 校验。</p>
 *
 * @author jpom
 * @since 2.11.13
 */
@Data
public class AgentPackageInfo {

    /**
     * 升级包对应的引擎
     */
    private AgentEngine engine = AgentEngine.JAVA;
    /**
     * 版本号
     */
    private String version;
    /**
     * 打包时间
     */
    private String timeStamp;
    /**
     * 最低兼容版本
     */
    private String minVersion;
    /**
     * Go 容器包平台，例如 linux/amd64（Java 包为空）
     */
    private String platform;
    /**
     * Go 可执行文件 sha256（Java 包为空）
     */
    private String sha256;
    /**
     * 包文件名
     */
    private String fileName;
    /**
     * 包大小
     */
    private long size;

    /**
     * 是否 Go 升级容器（zip 内包含 go-agent/agent.json）
     *
     * @return true 是 Go 容器
     */
    public boolean isGoContainer() {
        return engine == AgentEngine.GO;
    }
}
