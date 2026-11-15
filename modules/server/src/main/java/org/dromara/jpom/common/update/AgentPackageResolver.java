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
import java.util.Locale;
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
     * Go 升级容器文件名后缀（linux/amd64 兼容别名；多平台使用 {@code -upgrade-<os>-<arch>.jar}）
     */
    public static final String GO_UPGRADE_FILE_SUFFIX = "-upgrade.jar";
    /**
     * 未上报/未指定平台时按 linux/amd64 处理（旧版 Server/旧版 Go 插件端兼容口径）
     */
    public static final String DEFAULT_PLATFORM = "linux/amd64";
    /**
     * Go 升级容器内可执行文件目录
     */
    public static final String GO_BIN_DIR = "go-agent/bin/";
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
     * 解析节点运行平台（os/arch）
     *
     * <p>优先取 manifest.platform（Go 2.11.13.2-beta 起上报，如 linux/arm64）；旧版 Go 插件端
     * 只有 osName 时按 {@code arch + osName} 映射（Linux→linux、Windows→windows、Mac OS X→darwin，
     * 与 Java 插件端 osName 兼容值一致）。两者都没有时返回 null，由调用方按 legacy 处理
     * （仅允许 linux/amd64）。</p>
     *
     * @param infoData /info 响应的 data
     * @return 规范化平台（小写 os/arch），无法识别返回 null
     */
    public static String resolvePlatform(JSONObject infoData) {
        JSONObject manifest = manifest(infoData);
        if (manifest == null) {
            return null;
        }
        String platform = normalizePlatform(manifest.getString("platform"));
        if (StrUtil.isNotBlank(platform)) {
            return platform;
        }
        String arch = normalizeArch(manifest.getString("arch"));
        if (StrUtil.isBlank(arch)) {
            return null;
        }
        String os = normalizeOs(manifest.getString("osName"));
        if (StrUtil.isBlank(os)) {
            return null;
        }
        return os + "/" + arch;
    }

    /**
     * 节点 /info 是否上报了平台相关信息（platform 或 arch）
     *
     * <p>用于区分“旧版节点未上报平台（legacy，仅允许 linux/amd64）”与“上报了但无法识别
     * （例如未知 osName/非法 platform，应拒绝并提示手动上传）”。</p>
     *
     * @param infoData /info 响应的 data
     * @return true 表示存在平台相关字段
     */
    public static boolean hasPlatformInfo(JSONObject infoData) {
        JSONObject manifest = manifest(infoData);
        if (manifest == null) {
            return false;
        }
        return StrUtil.isNotBlank(manifest.getString("platform")) || StrUtil.isNotBlank(manifest.getString("arch"));
    }

    /**
     * 规范化平台字符串（os/arch，忽略大小写与首尾空白）
     *
     * @param platform 平台，例如 linux/amd64、Linux/ARM64
     * @return 规范化平台（小写、arch 别名归一），非法格式返回 null
     */
    public static String normalizePlatform(String platform) {
        if (StrUtil.isBlank(platform)) {
            return null;
        }
        String value = StrUtil.trim(platform).toLowerCase(Locale.ROOT);
        int index = value.indexOf('/');
        if (index <= 0 || index >= value.length() - 1) {
            return null;
        }
        String os = normalizeOs(value.substring(0, index));
        String arch = normalizeArch(value.substring(index + 1));
        if (StrUtil.isBlank(os) || StrUtil.isBlank(arch)) {
            return null;
        }
        return os + "/" + arch;
    }

    /**
     * 规范化系统名称（兼容 Java 插件端 osName 与 Go runtime.GOOS）
     *
     * @param osName 系统名称
     * @return 规范化系统名（linux/windows/darwin 等），无法识别返回 null
     */
    public static String normalizeOs(String osName) {
        if (StrUtil.isBlank(osName)) {
            return null;
        }
        String os = StrUtil.trim(osName).toLowerCase(Locale.ROOT);
        if ("linux".equals(os)) {
            return "linux";
        }
        if ("windows".equals(os) || "win".equals(os) || "win32".equals(os)) {
            return "windows";
        }
        if ("darwin".equals(os) || "mac".equals(os) || "macos".equals(os) || "mac os x".equals(os) || "osx".equals(os)) {
            return "darwin";
        }
        // 其他平台保留小写原值（仍做精确比较，避免把未知系统误判为不支持）
        return os.matches("[a-z0-9]+") ? os : null;
    }

    /**
     * 规范化 CPU 架构（兼容 Go runtime.GOARCH 与常见别名）
     *
     * @param arch 架构
     * @return 规范化架构（amd64/arm64 等），无法识别返回 null
     */
    public static String normalizeArch(String arch) {
        if (StrUtil.isBlank(arch)) {
            return null;
        }
        String value = StrUtil.trim(arch).toLowerCase(Locale.ROOT);
        if ("x86_64".equals(value) || "x64".equals(value) || "amd64".equals(value)) {
            return "amd64";
        }
        if ("aarch64".equals(value) || "arm64v8".equals(value) || "arm64".equals(value)) {
            return "arm64";
        }
        if ("x86".equals(value) || "i386".equals(value) || "i686".equals(value) || "386".equals(value)) {
            return "386";
        }
        return value.matches("[a-z0-9_]+") ? value : null;
    }

    /**
     * Go 升级容器内对应平台的可执行文件路径
     *
     * @param platform 平台（os/arch）
     * @return 容器内路径，例如 go-agent/bin/go-agent-linux-arm64（windows 带 .exe）
     */
    public static String platformExecutableName(String platform) {
        String normalized = normalizePlatform(platform);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        int index = normalized.indexOf('/');
        String os = normalized.substring(0, index);
        String arch = normalized.substring(index + 1);
        return GO_BIN_DIR + "go-agent-" + os + "-" + arch + ("windows".equals(os) ? ".exe" : "");
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
     * 校验升级包与节点是否兼容（无节点平台信息，按 legacy 规则处理）
     *
     * @param info             升级包信息
     * @param nodeEngine       节点引擎
     * @param nodeVersion      节点当前版本
     * @param allowedDowngrade 是否允许降级（jpom.system.allowed-downgrade）
     * @return 检查结果
     * @see #verify(AgentPackageInfo, AgentEngine, String, String, boolean)
     */
    public static PackageCheckResult verify(AgentPackageInfo info, AgentEngine nodeEngine, String nodeVersion, boolean allowedDowngrade) {
        return verify(info, nodeEngine, null, nodeVersion, allowedDowngrade);
    }

    /**
     * 校验升级包与节点是否兼容
     *
     * <p>Go 节点：先校验平台（容器 platform 与节点 platform 完全一致；容器/节点未上报平台时
     * 按 legacy 处理，仅允许 linux/amd64），再对齐 Java 的版本规则：版本相同、最低兼容版本、
     * 禁止降级。Java 节点完全保持原有行为（平台参数被忽略，版本重复/最低兼容/降级仍由
     * Agent 端校验），服务端只拦截引擎不匹配（避免把 Go 容器发给 Java 节点、或把 Java 包发给
     * Go 节点）。</p>
     *
     * @param info             升级包信息
     * @param nodeEngine       节点引擎
     * @param nodePlatform     节点平台（os/arch，null/空表示旧版节点未上报，按 linux/amd64 legacy 处理）
     * @param nodeVersion      节点当前版本
     * @param allowedDowngrade 是否允许降级（jpom.system.allowed-downgrade）
     * @return 检查结果
     */
    public static PackageCheckResult verify(AgentPackageInfo info, AgentEngine nodeEngine, String nodePlatform, String nodeVersion, boolean allowedDowngrade) {
        if (info == null || StrUtil.isBlank(info.getVersion())) {
            return PackageCheckResult.INVALID_PACKAGE;
        }
        if (nodeEngine != null && info.getEngine() != nodeEngine) {
            return PackageCheckResult.ENGINE_MISMATCH;
        }
        if (nodeEngine != AgentEngine.GO) {
            return PackageCheckResult.OK;
        }
        // 平台校验：节点平台缺失（旧版节点）按 linux/amd64 legacy 处理；容器平台缺失同样视为 legacy 容器
        String platform = normalizePlatform(nodePlatform);
        boolean legacyNode = StrUtil.isBlank(platform);
        if (legacyNode) {
            platform = DEFAULT_PLATFORM;
        }
        String packagePlatform = normalizePlatform(info.getPlatform());
        if (StrUtil.isBlank(packagePlatform)) {
            // legacy 容器（未声明 platform）：仅允许 linux/amd64 节点
            if (!DEFAULT_PLATFORM.equals(platform)) {
                return PackageCheckResult.PLATFORM_MISMATCH;
            }
        } else if (!StrUtil.equals(packagePlatform, platform)) {
            return PackageCheckResult.PLATFORM_MISMATCH;
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
     * Go 升级容器兼容别名文件名：agent-go-&lt;version&gt;-upgrade.jar（等价 linux/amd64 容器）
     *
     * @param version 版本号（可带 v 前缀）
     * @return 文件名
     */
    public static String goUpgradeFileName(String version) {
        return StrUtil.format("{}{}{}", GO_UPGRADE_FILE_PREFIX, normalizeVersion(version), GO_UPGRADE_FILE_SUFFIX);
    }

    /**
     * Go 升级容器文件名：agent-go-&lt;version&gt;-upgrade-&lt;os&gt;-&lt;arch&gt;.jar
     *
     * <p>platform 为空时返回 linux/amd64 兼容别名（agent-go-&lt;version&gt;-upgrade.jar）。</p>
     *
     * @param version  版本号（可带 v 前缀）
     * @param platform 平台（os/arch）
     * @return 文件名
     */
    public static String goUpgradeFileName(String version, String platform) {
        String normalized = normalizePlatform(platform);
        if (StrUtil.isBlank(normalized)) {
            return goUpgradeFileName(version);
        }
        int index = normalized.indexOf('/');
        return StrUtil.format("{}{}-upgrade-{}-{}.jar",
            GO_UPGRADE_FILE_PREFIX, normalizeVersion(version), normalized.substring(0, index), normalized.substring(index + 1));
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
     * @return Go 升级容器地址（linux/amd64 兼容别名），无法推导时返回 null
     */
    public static String buildGoUpgradeUrl(String javaAgentUrl, String version) {
        return buildGoUpgradeAliasUrl(javaAgentUrl, version);
    }

    /**
     * 从 Java 插件端下载地址推导同版本目录下指定平台的 Go 升级容器地址。
     *
     * <p>platform 为空时回退到 linux/amd64 兼容别名 {@code agent-go-<version>-upgrade.jar}。</p>
     *
     * @param javaAgentUrl Java 插件端下载地址
     * @param version      版本号（可带 v 前缀）
     * @param platform     节点平台（os/arch）
     * @return Go 升级容器地址，无法推导时返回 null
     */
    public static String buildGoUpgradeUrl(String javaAgentUrl, String version, String platform) {
        return buildGoUpgradeUrlWithFileName(javaAgentUrl, version, goUpgradeFileName(version, platform));
    }

    /**
     * 从 Java 插件端下载地址推导 linux/amd64 兼容别名容器地址
     * （{@code agent-go-<version>-upgrade.jar}，多平台发布时仍会上传的别名副本）。
     *
     * @param javaAgentUrl Java 插件端下载地址
     * @param version      版本号（可带 v 前缀）
     * @return 别名容器地址，无法推导时返回 null
     */
    public static String buildGoUpgradeAliasUrl(String javaAgentUrl, String version) {
        return buildGoUpgradeUrlWithFileName(javaAgentUrl, version, goUpgradeFileName(version));
    }

    private static String buildGoUpgradeUrlWithFileName(String javaAgentUrl, String version, String fileName) {
        if (StrUtil.isBlank(javaAgentUrl) || StrUtil.isBlank(fileName)) {
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
        return StrUtil.format("{}{}{}", path.substring(0, slashIndex + 1), fileName, query);
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
        return validateGoContainer(file, null);
    }

    /**
     * 校验 Go 升级容器结构（含期望平台）
     *
     * <p>expectedPlatform 非空时额外要求：容器 agent.json.platform 与期望平台一致，且容器内
     * 存在该平台的可执行文件 {@code go-agent/bin/go-agent-<os>-<arch>[.exe]}；期望平台为空时
     * 保持旧行为（仅要求存在任意 go-agent/bin 可执行文件，供旧数据/旧 Server 兼容）。</p>
     *
     * @param file             容器文件
     * @param expectedPlatform 期望平台（os/arch），可为空
     * @return null 表示通过，否则为失败原因（供日志/错误文案拼接）
     */
    public static String validateGoContainer(File file, String expectedPlatform) {
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
            String containerPlatform = normalizePlatform(info.getPlatform());
            String expected = normalizePlatform(expectedPlatform);
            if (StrUtil.isNotBlank(expected)) {
                if (StrUtil.isBlank(containerPlatform)) {
                    return "missing platform in " + GO_CONTAINER_MANIFEST;
                }
                if (!StrUtil.equals(expected, containerPlatform)) {
                    return StrUtil.format("platform mismatch: container {}, expected {}", containerPlatform, expected);
                }
            }
            String expectedBinary = platformExecutableName(containerPlatform);
            if (StrUtil.isNotBlank(expectedBinary)) {
                return findEntry(zipFile, expectedBinary) == null ? "missing " + expectedBinary : null;
            }
            // legacy 容器（未声明 platform）：保持旧行为，要求存在任意 go-agent/bin 可执行文件
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = normalizeEntryName(entry.getName()).toLowerCase(Locale.ROOT);
                if (StrUtil.startWith(name, GO_BIN_DIR + "go-agent")) {
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
