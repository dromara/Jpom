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

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 插件端在线升级包解析与兼容性校验工具
 *
 * <p>纯工具类，不依赖 Spring/数据库，便于单元测试。Go 版升级包（CONTRACT-UPGRADE 方案 B）
 * 使用 jar 作为运输容器：容器内 go-agent/agent.json 提供版本、最低兼容版本、平台与
 * 可执行文件 sha256；容器 MANIFEST 仍然保留，以兼容旧版 Server 的 JpomManifest#checkJpomJar。</p>
 *
 * @author jpom
 * @since 2.11.13
 */
public final class AgentPackageResolver {

    /**
     * Go 升级容器内元信息条目
     */
    public static final String GO_CONTAINER_MANIFEST = "go-agent/agent.json";
    /**
     * Go 升级容器文件名前缀
     */
    public static final String GO_UPGRADE_FILE_PREFIX = "agent-go-";
    /**
     * Go 升级容器文件名后缀
     */
    public static final String GO_UPGRADE_FILE_SUFFIX = "-upgrade.jar";
    /**
     * 下载中心 URL 中的版本号片段，例如 /release/2.11.13/ 或 /beta/2.11.13.1/
     */
    private static final Pattern VERSION_IN_URL = Pattern.compile("/(\\d+\\.\\d+\\.\\d+(?:\\.\\d+)?)");

    private AgentPackageResolver() {
    }

    /**
     * 取出 /info 响应中的 manifest 对象（兼容 data 直接就是 manifest 的旧实现）
     *
     * @param infoData /info 响应的 data
     * @return manifest，可能为 null
     */
    public static JSONObject manifest(JSONObject infoData) {
        if (infoData == null) {
            return null;
        }
        JSONObject manifest = infoData.getJSONObject("manifest");
        return manifest == null ? infoData : manifest;
    }

    /**
     * 判断节点运行引擎
     *
     * <p>优先使用 manifest.engine；旧版 Go 插件端没有该字段时使用 manifest.jarFile 兜底：
     * Java 插件端运行文件一定是 .jar，Go 插件端是平台可执行文件。</p>
     *
     * @param infoData /info 响应的 data
     * @return 引擎，无法判断时按 Java 处理（保持旧行为）
     */
    public static AgentEngine resolveEngine(JSONObject infoData) {
        JSONObject manifest = manifest(infoData);
        if (manifest == null) {
            return AgentEngine.JAVA;
        }
        String engine = manifest.getString("engine");
        if (StrUtil.isNotBlank(engine)) {
            return AgentEngine.parse(engine);
        }
        String jarFile = manifest.getString("jarFile");
        if (StrUtil.isNotBlank(jarFile) && !StrUtil.endWithIgnoreCase(jarFile, ".jar")) {
            return AgentEngine.GO;
        }
        return AgentEngine.JAVA;
    }

    /**
     * 节点当前版本号
     *
     * @param infoData /info 响应的 data
     * @return 版本号，可能为 null
     */
    public static String resolveVersion(JSONObject infoData) {
        JSONObject manifest = manifest(infoData);
        return manifest == null ? null : normalizeVersion(manifest.getString("version"));
    }

    /**
     * 节点打包时间
     *
     * @param infoData /info 响应的 data
     * @return 打包时间，可能为 null
     */
    public static String resolveTimeStamp(JSONObject infoData) {
        JSONObject manifest = manifest(infoData);
        return manifest == null ? null : StrUtil.trimToNull(manifest.getString("timeStamp"));
    }

    /**
     * 解析升级包信息（jar 或 zip）
     *
     * @param file 升级包文件
     * @return 包信息
     * @throws IOException 读取失败（不是合法的 zip/jar 等）
     */
    public static AgentPackageInfo inspectPackage(File file) throws IOException {
        AgentPackageInfo info = new AgentPackageInfo();
        info.setFileName(file.getName());
        info.setSize(file.length());
        try (ZipFile zipFile = new ZipFile(file)) {
            ZipEntry goManifest = findEntry(zipFile, GO_CONTAINER_MANIFEST);
            if (goManifest != null) {
                info.setEngine(AgentEngine.GO);
                parseGoManifest(zipFile, goManifest, info);
                return info;
            }
            ZipEntry javaManifest = findEntry(zipFile, "META-INF/MANIFEST.MF");
            if (javaManifest != null) {
                try (InputStream inputStream = zipFile.getInputStream(javaManifest)) {
                    Manifest manifest = new Manifest(inputStream);
                    Attributes attributes = manifest.getMainAttributes();
                    info.setEngine(AgentEngine.JAVA);
                    info.setVersion(normalizeVersion(attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION)));
                    info.setTimeStamp(StrUtil.trimToNull(attributes.getValue("Jpom-Timestamp")));
                    info.setMinVersion(StrUtil.trimToNull(attributes.getValue("Jpom-Min-Version")));
                }
            }
            return info;
        }
    }

    /**
     * 判断文件是否是 Go 升级容器（zip 内包含 go-agent/agent.json）
     *
     * @param file 文件
     * @return true 是 Go 容器
     */
    public static boolean isGoContainer(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        try (ZipFile zipFile = new ZipFile(file)) {
            return findEntry(zipFile, GO_CONTAINER_MANIFEST) != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 校验升级包与节点是否兼容
     *
     * <p>Go 节点完全对齐 Java 的升级规则：版本相同、最低兼容版本、禁止降级；
     * Java 节点的版本重复/最低兼容/降级仍由 Agent 端校验（保持原有行为），
     * 服务端只拦截引擎不匹配（避免把 Go 容器发给 Java 节点、或把 Java 包发给 Go 节点）。</p>
     *
     * @param info           升级包信息
     * @param nodeEngine     节点引擎
     * @param nodeVersion    节点当前版本
     * @param allowedDowngrade 是否允许降级（jpom.system.allowed-downgrade）
     * @return 检查结果
     */
    public static PackageCheckResult verify(AgentPackageInfo info, AgentEngine nodeEngine, String nodeVersion, boolean allowedDowngrade) {
        if (info == null || StrUtil.isBlank(info.getVersion())) {
            return PackageCheckResult.INVALID_PACKAGE;
        }
        if (nodeEngine != null && info.getEngine() != nodeEngine) {
            return PackageCheckResult.ENGINE_MISMATCH;
        }
        if (nodeEngine != AgentEngine.GO) {
            return PackageCheckResult.OK;
        }
        String currentVersion = normalizeVersion(nodeVersion);
        if (StrUtil.isNotBlank(currentVersion) && StrUtil.equals(info.getVersion(), currentVersion)) {
            return PackageCheckResult.SAME_VERSION;
        }
        if (StrUtil.isNotBlank(currentVersion)
            && StrUtil.isNotBlank(info.getMinVersion())
            && StrUtil.compareVersion(currentVersion, normalizeVersion(info.getMinVersion())) < 0) {
            return PackageCheckResult.MIN_VERSION;
        }
        if (!allowedDowngrade
            && StrUtil.isNotBlank(currentVersion)
            && StrUtil.compareVersion(info.getVersion(), currentVersion) < 0) {
            return PackageCheckResult.DOWNGRADE;
        }
        return PackageCheckResult.OK;
    }

    /**
     * Go 升级容器文件名：agent-go-&lt;version&gt;-upgrade.jar
     *
     * @param version 版本号（可带 v 前缀）
     * @return 文件名
     */
    public static String goUpgradeFileName(String version) {
        return StrUtil.format("{}{}{}", GO_UPGRADE_FILE_PREFIX, normalizeVersion(version), GO_UPGRADE_FILE_SUFFIX);
    }

    /**
     * 从 Java 插件端下载地址推导同版本目录下的 Go 升级容器地址。
     *
     * <p>下载中心（d2.jpom.download / R2 / 授权下载域名）会把同一版本的全部产物放在同一目录，
     * 例如 agentUrl=https://d2.jpom.download/release/2.11.13/agent-2.11.13-release.zip
     * 推导为 https://d2.jpom.download/release/2.11.13/agent-go-2.11.13-upgrade.jar。
     * 自动下载场景下 agentUrl 已由 cn.keepbx.jpom.Type#getRemoteUrl 处理授权 token 替换，
     * 这里只替换最后一段文件名并保留 query（token）。</p>
     *
     * @param javaAgentUrl Java 插件端下载地址
     * @param version      版本号（可带 v 前缀）
     * @return Go 升级容器地址，无法推导时返回 null
     */
    public static String buildGoUpgradeUrl(String javaAgentUrl, String version) {
        if (StrUtil.isBlank(javaAgentUrl)) {
            return null;
        }
        String url = StrUtil.trim(javaAgentUrl);
        String targetVersion = normalizeVersion(version);
        if (StrUtil.isBlank(targetVersion)) {
            return null;
        }
        int queryIndex = url.indexOf('?');
        String path = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        String query = queryIndex >= 0 ? url.substring(queryIndex) : StrUtil.EMPTY;
        int slashIndex = path.lastIndexOf('/');
        if (slashIndex < 0) {
            return null;
        }
        return StrUtil.format("{}{}{}", path.substring(0, slashIndex + 1), goUpgradeFileName(targetVersion), query);
    }

    /**
     * 从下载中心地址中解析版本号（tagName 缺失时兜底）
     *
     * @param url 下载地址
     * @return 版本号，未匹配返回 null
     */
    public static String parseVersionInUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        Matcher matcher = VERSION_IN_URL.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 去掉版本号的 v 前缀与空白
     *
     * @param version 版本号
     * @return 规范化版本号，可能为空
     */
    public static String normalizeVersion(String version) {
        return StrUtil.removePrefixIgnoreCase(StrUtil.trim(version), "v");
    }

    /**
     * 校验 Go 升级容器结构（不依赖旧 Server 的 JpomManifest#checkJpomJar）
     *
     * <p>旧 Server 的 checkJpomJar 使用 JarClassLoader 从 jar 根加载 Main-Class；Go 容器的
     * 空壳 Main-Class 位于 BOOT-INF/classes 下时无法加载，因此 Go 容器按容器结构本身校验：
     * MANIFEST 必备字段、BOOT-INF 空壳 class、go-agent/agent.json、至少一个 go-agent/bin 可执行文件。</p>
     *
     * @param file 容器文件
     * @return null 表示通过，否则为失败原因（供日志/错误文案拼接）
     */
    public static String validateGoContainer(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            ZipEntry manifestEntry = findEntry(zipFile, "META-INF/MANIFEST.MF");
            if (manifestEntry == null) {
                return "missing META-INF/MANIFEST.MF";
            }
            Attributes attributes;
            try (InputStream inputStream = zipFile.getInputStream(manifestEntry)) {
                attributes = new Manifest(inputStream).getMainAttributes();
            }
            if (StrUtil.isBlank(attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION))
                || StrUtil.isBlank(attributes.getValue("Jpom-Timestamp"))
                || StrUtil.isBlank(attributes.getValue("Jpom-Min-Version"))) {
                return "missing MANIFEST version/timeStamp/minVersion";
            }
            if (findEntry(zipFile, "BOOT-INF/classes/org/dromara/jpom/JpomAgentApplication.class") == null) {
                return "missing BOOT-INF/classes/org/dromara/jpom/JpomAgentApplication.class";
            }
            ZipEntry goManifest = findEntry(zipFile, GO_CONTAINER_MANIFEST);
            if (goManifest == null) {
                return "missing " + GO_CONTAINER_MANIFEST;
            }
            AgentPackageInfo info = new AgentPackageInfo();
            parseGoManifest(zipFile, goManifest, info);
            if (StrUtil.isBlank(info.getVersion())) {
                return "missing go-agent version";
            }
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = normalizeEntryName(entry.getName()).toLowerCase();
                if (StrUtil.startWith(name, "go-agent/bin/go-agent")) {
                    return null;
                }
            }
            return "missing go-agent/bin executable";
        } catch (IOException e) {
            return "invalid zip: " + e.getMessage();
        }
    }

    private static String normalizeEntryName(String entryName) {
        String name = entryName;
        while (name.startsWith("./")) {
            name = name.substring(2);
        }
        return name;
    }

    private static void parseGoManifest(ZipFile zipFile, ZipEntry goManifest, AgentPackageInfo info) throws IOException {
        String content;
        try (InputStream inputStream = zipFile.getInputStream(goManifest)) {
            content = IoUtil.read(inputStream, StandardCharsets.UTF_8);
        }
        JSONObject meta = StrUtil.isBlank(content) ? null : JSONObject.parseObject(content);
        if (meta == null) {
            return;
        }
        info.setVersion(normalizeVersion(meta.getString("version")));
        info.setTimeStamp(StrUtil.trimToNull(meta.getString("timeStamp")));
        info.setMinVersion(StrUtil.trimToNull(meta.getString("minVersion")));
        info.setPlatform(StrUtil.trimToNull(meta.getString("platform")));
        String sha256 = StrUtil.trimToNull(meta.getString("sha256"));
        info.setSha256(StrUtil.isEmpty(sha256) ? null : sha256.toLowerCase());
    }

    private static ZipEntry findEntry(ZipFile zipFile, String name) {
        ZipEntry entry = zipFile.getEntry(name);
        if (entry != null) {
            return entry;
        }
        // 兼容 ./go-agent/agent.json 与大小写差异（不同打包工具行为）
        java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry current = entries.nextElement();
            if (StrUtil.equalsIgnoreCase(normalizeEntryName(current.getName()), name)) {
                return current;
            }
        }
        return null;
    }
}
