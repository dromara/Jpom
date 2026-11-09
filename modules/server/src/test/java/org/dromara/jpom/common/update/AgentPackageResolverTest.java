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

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 节点在线升级：引擎判定、升级包解析、下载地址推导与版本/降级/最低兼容校验的回归用例。
 *
 * @author jpom
 */
public class AgentPackageResolverTest {

    @TempDir
    Path tempDir;

    // ------------------------------------------------------------------
    // 1. 引擎判定（manifest.engine 优先，旧版 Go 端 jarFile 兜底）
    // ------------------------------------------------------------------

    @Test
    public void resolveEngineByManifestEngine() {
        JSONObject goInfo = info(new JSONObject(), "go", "/opt/jpom/lib/agent");
        assertEquals(AgentEngine.GO, AgentPackageResolver.resolveEngine(goInfo));

        JSONObject javaInfo = info(new JSONObject(), "java", "/opt/jpom/lib/Agent-2.11.13.jar");
        assertEquals(AgentEngine.JAVA, AgentPackageResolver.resolveEngine(javaInfo));

        // 大小写不敏感，未知值按 Java 处理（保持旧行为）
        assertEquals(AgentEngine.GO, AgentPackageResolver.resolveEngine(info(new JSONObject(), "GO", "/opt/agent")));
        assertEquals(AgentEngine.JAVA, AgentPackageResolver.resolveEngine(info(new JSONObject(), "python", "/opt/agent")));
    }

    @Test
    public void resolveEngineFallbackByJarFile() {
        // 旧版 Go 插件端没有 engine 字段：运行文件不是 jar
        JSONObject legacyGo = new JSONObject();
        JSONObject manifest = new JSONObject();
        manifest.put("version", "2.11.13");
        manifest.put("jarFile", "/home/jpom/agent/lib/agent");
        legacyGo.put("manifest", manifest);
        assertEquals(AgentEngine.GO, AgentPackageResolver.resolveEngine(legacyGo));

        // Java 插件端运行文件一定是 .jar
        manifest.put("jarFile", "/home/jpom/agent/lib/Agent-2.11.13.jar");
        assertEquals(AgentEngine.JAVA, AgentPackageResolver.resolveEngine(legacyGo));

        // 没有 manifest 时 data 直接按 manifest 处理；引擎保持旧行为（Java）
        assertNull(AgentPackageResolver.manifest(null));
        assertEquals(AgentEngine.JAVA, AgentPackageResolver.resolveEngine(new JSONObject()));
        assertEquals(AgentEngine.JAVA, AgentPackageResolver.resolveEngine(null));
    }

    @Test
    public void resolveEngineSupportsManifestAtRoot() {
        // 兼容 data 直接就是 manifest 的实现
        JSONObject data = new JSONObject();
        data.put("engine", "go");
        data.put("jarFile", "/opt/agent");
        assertEquals(AgentEngine.GO, AgentPackageResolver.resolveEngine(data));
    }

    @Test
    public void resolveVersionAndTimeStamp() {
        JSONObject info = info(new JSONObject(), "go", "/opt/agent");
        info.getJSONObject("manifest").put("version", "v2.11.13");
        info.getJSONObject("manifest").put("timeStamp", "2026-11-09 00:00:00");
        assertEquals("2.11.13", AgentPackageResolver.resolveVersion(info));
        assertEquals("2026-11-09 00:00:00", AgentPackageResolver.resolveTimeStamp(info));
    }

    // ------------------------------------------------------------------
    // 2. 升级包解析（Go 容器 / Java jar / 非法文件）
    // ------------------------------------------------------------------

    @Test
    public void inspectGoUpgradeContainer() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("META-INF/MANIFEST.MF", manifest("2.11.13", "2.11.0"));
        entries.put("BOOT-INF/classes/org/dromara/jpom/JpomAgentApplication.class", new byte[]{1, 2, 3});
        entries.put("go-agent/agent.json", goAgentJson("2.11.13", "2.11.0", "linux/amd64", "abc123").getBytes(StandardCharsets.UTF_8));
        entries.put("go-agent/bin/go-agent-linux-amd64", new byte[]{4, 5, 6});
        File file = writeZip("agent-go-2.11.13-upgrade.jar", entries);

        AgentPackageInfo info = AgentPackageResolver.inspectPackage(file);
        assertEquals(AgentEngine.GO, info.getEngine());
        assertTrue(info.isGoContainer());
        assertEquals("2.11.13", info.getVersion());
        assertEquals("2.11.0", info.getMinVersion());
        assertEquals("linux/amd64", info.getPlatform());
        assertEquals("abc123", info.getSha256());
        assertEquals("agent-go-2.11.13-upgrade.jar", info.getFileName());
    }

    @Test
    public void inspectGoUpgradeContainerWithDotSlashEntry() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("./go-agent/agent.json", goAgentJson("2.11.13", "1.0.0", "windows/amd64", null).getBytes(StandardCharsets.UTF_8));
        File file = writeZip("go-container.jar", entries);
        AgentPackageInfo info = AgentPackageResolver.inspectPackage(file);
        assertEquals(AgentEngine.GO, info.getEngine());
        assertEquals("2.11.13", info.getVersion());
        assertNull(info.getSha256());
    }

    @Test
    public void inspectJavaAgentJar() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("META-INF/MANIFEST.MF", manifest("2.11.13", "2.11.0"));
        entries.put("BOOT-INF/classes/org/dromara/jpom/JpomAgentApplication.class", new byte[]{1, 2, 3});
        File file = writeZip("agent-2.11.13-release.jar", entries);

        AgentPackageInfo info = AgentPackageResolver.inspectPackage(file);
        assertEquals(AgentEngine.JAVA, info.getEngine());
        assertFalse(info.isGoContainer());
        assertEquals("2.11.13", info.getVersion());
        assertEquals("2.11.0", info.getMinVersion());
    }

    @Test
    public void inspectInvalidPackage() throws IOException {
        File file = tempDir.resolve("not-a-zip.jar").toFile();
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write("not a zip file".getBytes(StandardCharsets.UTF_8));
        }
        assertThrows(IOException.class, () -> AgentPackageResolver.inspectPackage(file));
        assertFalse(AgentPackageResolver.isGoContainer(file));
        assertFalse(AgentPackageResolver.isGoContainer(null));
    }

    @Test
    public void isGoContainerDistinguishesSlots() throws IOException {
        // 上传入口按包内容分类：Go 容器归入 Go 升级包槽，Java jar 保持原槽位
        Map<String, byte[]> goEntries = new LinkedHashMap<>();
        goEntries.put("go-agent/agent.json", goAgentJson("2.11.13", "1.0.0", "linux/amd64", null).getBytes(StandardCharsets.UTF_8));
        assertTrue(AgentPackageResolver.isGoContainer(writeZip("go.jar", goEntries)));

        Map<String, byte[]> javaEntries = new LinkedHashMap<>();
        javaEntries.put("META-INF/MANIFEST.MF", manifest("2.11.13", "2.11.0"));
        assertFalse(AgentPackageResolver.isGoContainer(writeZip("java.jar", javaEntries)));
    }

    // ------------------------------------------------------------------
    // 3. 下载中心 URL 推导（agent-go-<version>-upgrade.jar，同目录，保留 token query）
    // ------------------------------------------------------------------

    @Test
    public void buildGoUpgradeUrlFromJavaAgentUrl() {
        assertEquals("https://d2.jpom.download/release/2.11.13/agent-go-2.11.13-upgrade.jar",
            AgentPackageResolver.buildGoUpgradeUrl("https://d2.jpom.download/release/2.11.13/agent-2.11.13-release.zip", "2.11.13"));
        assertEquals("https://d2.jpom.download/beta/2.11.13.1/agent-go-2.11.13.1-upgrade.jar",
            AgentPackageResolver.buildGoUpgradeUrl("https://d2.jpom.download/beta/2.11.13.1/agent-2.11.13.1-release.zip", "v2.11.13.1"));
        // 授权下载域名：保留 query 中的 token
        assertEquals("https://download.jpom.top/release/2.11.13/agent-go-2.11.13-upgrade.jar?token=abc",
            AgentPackageResolver.buildGoUpgradeUrl("https://download.jpom.top/release/2.11.13/agent-2.11.13-release.zip?token=abc", "2.11.13"));
        // 无法推导的情况
        assertNull(AgentPackageResolver.buildGoUpgradeUrl(null, "2.11.13"));
        assertNull(AgentPackageResolver.buildGoUpgradeUrl("agent-2.11.13-release.zip", "2.11.13"));
        assertNull(AgentPackageResolver.buildGoUpgradeUrl("https://d2.jpom.download/release/2.11.13/agent-2.11.13-release.zip", ""));
    }

    @Test
    public void parseVersionInUrl() {
        assertEquals("2.11.13", AgentPackageResolver.parseVersionInUrl("https://d2.jpom.download/release/2.11.13/agent-2.11.13-release.zip"));
        assertEquals("2.11.13.1", AgentPackageResolver.parseVersionInUrl("https://d2.jpom.download/beta/2.11.13.1/agent-2.11.13.1-release.zip"));
        assertNull(AgentPackageResolver.parseVersionInUrl("https://d2.jpom.download/release/agent-release.zip"));
    }

    @Test
    public void goUpgradeFileNameAndNormalizeVersion() {
        assertEquals("agent-go-2.11.13-upgrade.jar", AgentPackageResolver.goUpgradeFileName("v2.11.13"));
        assertEquals("agent-go-2.11.13.1-upgrade.jar", AgentPackageResolver.goUpgradeFileName("2.11.13.1"));
        assertEquals("2.11.13", AgentPackageResolver.normalizeVersion(" v2.11.13 "));
    }

    @Test
    public void validateGoContainerStructure() throws IOException {
        // 合法容器（旧脚本只放 BOOT-INF/classes 也能通过结构校验；新脚本额外放 jar 根供旧 Server loadClass）
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("META-INF/MANIFEST.MF", manifest("2.11.13.2", "2.11.0"));
        entries.put("BOOT-INF/classes/org/dromara/jpom/JpomAgentApplication.class", new byte[]{1});
        entries.put("go-agent/agent.json", goAgentJson("2.11.13.2", "2.11.0", "linux/amd64", "abc").getBytes(StandardCharsets.UTF_8));
        entries.put("go-agent/bin/go-agent-linux-amd64", new byte[]{2});
        File valid = writeZip("valid-container.jar", entries);
        assertNull(AgentPackageResolver.validateGoContainer(valid));

        // 新脚本：jar 根也放空壳 class（旧 Server checkJpomJar 的 loadClass 依赖它）
        Map<String, byte[]> withRootClass = new LinkedHashMap<>(entries);
        withRootClass.put("org/dromara/jpom/JpomAgentApplication.class", new byte[]{1});
        assertNull(AgentPackageResolver.validateGoContainer(writeZip("root-class-container.jar", withRootClass)));

        // 缺少 go-agent/agent.json
        Map<String, byte[]> noAgentJson = new LinkedHashMap<>(entries);
        noAgentJson.remove("go-agent/agent.json");
        assertNotNull(AgentPackageResolver.validateGoContainer(writeZip("no-agent-json.jar", noAgentJson)));

        // 缺少可执行文件
        Map<String, byte[]> noBinary = new LinkedHashMap<>(entries);
        noBinary.remove("go-agent/bin/go-agent-linux-amd64");
        assertNotNull(AgentPackageResolver.validateGoContainer(writeZip("no-binary.jar", noBinary)));

        // 缺少 MANIFEST 必备字段
        Map<String, byte[]> badManifest = new LinkedHashMap<>(entries);
        badManifest.put("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\nImplementation-Version: 2.11.13.2\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        assertNotNull(AgentPackageResolver.validateGoContainer(writeZip("bad-manifest.jar", badManifest)));

        // 不是 zip
        File notZip = tempDir.resolve("not-zip.jar").toFile();
        try (FileOutputStream outputStream = new FileOutputStream(notZip)) {
            outputStream.write("not zip".getBytes(StandardCharsets.UTF_8));
        }
        assertNotNull(AgentPackageResolver.validateGoContainer(notZip));
    }

    // ------------------------------------------------------------------
    // 4. 版本/降级/最低兼容校验（Go 节点与 Java 一致；Java 节点保持原有行为）
    // ------------------------------------------------------------------

    @Test
    public void verifyGoUpgradeRules() {
        AgentPackageInfo packageInfo = goPackage("2.11.13", "2.11.0");
        // 正常升级
        assertEquals(PackageCheckResult.OK, AgentPackageResolver.verify(packageInfo, AgentEngine.GO, "2.11.12", false));
        // 同版本
        assertEquals(PackageCheckResult.SAME_VERSION, AgentPackageResolver.verify(packageInfo, AgentEngine.GO, "2.11.13", false));
        // 降级：默认不允许，开启 allowed-downgrade 后允许
        AgentPackageInfo lower = goPackage("2.11.12", "2.11.0");
        assertEquals(PackageCheckResult.DOWNGRADE, AgentPackageResolver.verify(lower, AgentEngine.GO, "2.11.13", false));
        assertEquals(PackageCheckResult.OK, AgentPackageResolver.verify(lower, AgentEngine.GO, "2.11.13", true));
        // 当前版本低于包要求的最低兼容版本
        AgentPackageInfo minVersion = goPackage("2.11.13", "2.11.12");
        assertEquals(PackageCheckResult.MIN_VERSION, AgentPackageResolver.verify(minVersion, AgentEngine.GO, "2.11.11", false));
        // 包版本号缺失
        AgentPackageInfo invalid = goPackage(null, null);
        assertEquals(PackageCheckResult.INVALID_PACKAGE, AgentPackageResolver.verify(invalid, AgentEngine.GO, "2.11.12", false));
        assertEquals(PackageCheckResult.INVALID_PACKAGE, AgentPackageResolver.verify(null, AgentEngine.GO, "2.11.12", false));
    }

    @Test
    public void verifyEngineMismatch() {
        // Go 容器不能发给 Java 节点，Java 包也不能发给 Go 节点
        AgentPackageInfo goPackage = goPackage("2.11.13", "2.11.0");
        assertEquals(PackageCheckResult.ENGINE_MISMATCH, AgentPackageResolver.verify(goPackage, AgentEngine.JAVA, "2.11.12", false));

        AgentPackageInfo javaPackage = new AgentPackageInfo();
        javaPackage.setEngine(AgentEngine.JAVA);
        javaPackage.setVersion("2.11.13");
        assertEquals(PackageCheckResult.ENGINE_MISMATCH, AgentPackageResolver.verify(javaPackage, AgentEngine.GO, "2.11.12", false));
    }

    @Test
    public void verifyJavaBehaviorUnchanged() {
        // Java 节点：版本重复/降级/最低兼容仍由 Agent 端校验，服务端按原行为直接放行
        AgentPackageInfo javaPackage = new AgentPackageInfo();
        javaPackage.setEngine(AgentEngine.JAVA);
        javaPackage.setVersion("2.11.12");
        javaPackage.setMinVersion("2.11.0");
        assertEquals(PackageCheckResult.OK, AgentPackageResolver.verify(javaPackage, AgentEngine.JAVA, "2.11.13", false));
        assertEquals(PackageCheckResult.OK, AgentPackageResolver.verify(javaPackage, AgentEngine.JAVA, "2.11.12", false));
        assertEquals(PackageCheckResult.OK, AgentPackageResolver.verify(javaPackage, AgentEngine.JAVA, null, false));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private JSONObject info(JSONObject manifest, String engine, String jarFile) {
        manifest.put("version", "2.11.12");
        manifest.put("timeStamp", "2026-11-01 00:00:00");
        manifest.put("engine", engine);
        manifest.put("jarFile", jarFile);
        JSONObject data = new JSONObject();
        data.put("manifest", manifest);
        return data;
    }

    private AgentPackageInfo goPackage(String version, String minVersion) {
        AgentPackageInfo info = new AgentPackageInfo();
        info.setEngine(AgentEngine.GO);
        info.setVersion(version);
        info.setMinVersion(minVersion);
        info.setFileName("agent-go-" + version + "-upgrade.jar");
        return info;
    }

    private String goAgentJson(String version, String minVersion, String platform, String sha256) {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("timeStamp", "2026-11-09T00:00:00Z");
        json.put("minVersion", minVersion);
        json.put("platform", platform);
        if (sha256 != null) {
            json.put("sha256", sha256);
        }
        return json.toJSONString();
    }

    private byte[] manifest(String version, String minVersion) {
        String manifest = "Manifest-Version: 1.0\r\n"
            + "Implementation-Version: " + version + "\r\n"
            + "Jpom-Timestamp: 2026-11-09T00:00:00Z\r\n"
            + "Main-Class: org.dromara.jpom.JpomAgentApplication\r\n"
            + "Jpom-Min-Version: " + minVersion + "\r\n"
            + "\r\n";
        return manifest.getBytes(StandardCharsets.UTF_8);
    }

    private File writeZip(String name, Map<String, byte[]> entries) throws IOException {
        File file = tempDir.resolve(name).toFile();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(entry.getValue());
                zipOutputStream.closeEntry();
            }
        }
        return file;
    }
}
