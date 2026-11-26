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

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.ManifestUtil;
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import cn.keepbx.jpom.Type;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.system.JpomRuntimeException;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.JsonFileUtil;
import org.dromara.jpom.util.PlatformUtil;
import org.springframework.util.Assert;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Jpom 的运行信息
 *
 * @author bwcx_jzy
 * @since 2019/4/7
 */
@Slf4j
public class JpomManifest {

    private volatile static JpomManifest JPOM_MANIFEST;
    /**
     * 允许降级
     */
    private volatile static boolean allowedDowngrade;
    /**
     * 当前版本
     */
    private String version = "dev";
    /**
     * 打包时间
     */
    private String timeStamp;
    /**
     * 进程id
     */
    private long pid = SystemUtil.getCurrentPID();
    /**
     * 当前运行类型
     */
    private final Type type = JpomApplication.getAppType();
    /**
     * 端口号
     */
    private int port;
    /**
     * 随机ID
     */
    private String randomId;
    /**
     * Jpom 的数据目录
     */
    private String dataPath;
    /**
     * jar 运行路径
     */
    private String jarFile;
    /**
     * 系统名称
     */
    private final String osName = SystemUtil.getOsInfo().getName();
    /**
     * 系统架构（规范化后的值，如 amd64、arm64），与 Go 插件端 {@code arch} 口径一致
     */
    private final String arch = PlatformUtil.normalizeArch(SystemUtil.getOsInfo().getArch());
    /**
     * 运行平台（os/arch，如 linux/amd64），与 Go 插件端 {@code platform} 口径一致。
     * <p>Java 升级包与平台无关，该字段只用于界面展示（在线升级页「平台」列）。</p>
     */
    private final String platform = buildPlatform();
    /**
     * 安装id
     */
    private String installId;

    private static JpomManifest buildJpomManifest() {
        JpomManifest jpomManifest = new JpomManifest();
        File jarFile = getRunPath();
        Tuple jarVersion = getJarVersion(jarFile);
        if (jarVersion != null) {
            jpomManifest.setVersion(jarVersion.get(0));
            jpomManifest.setTimeStamp(jarVersion.get(1));
        }
        jpomManifest.setJarFile(FileUtil.getAbsolutePath(jarFile));
        //
        jpomManifest.randomId = IdUtil.fastSimpleUUID();
        return jpomManifest;
    }

    /**
     * 计算规范化运行平台（os/arch）。
     * <p>osName 里带版本号（"Windows 10"、"Mac OS X"、"Ubuntu 22.04"），不能直接规范化，
     * 因此优先用 hutool 的系统判定布尔值，只有都判不出来时才退回 osName 文本。</p>
     *
     * @return 平台（如 linux/amd64），无法识别返回 null
     */
    private static String buildPlatform() {
        OsInfo osInfo = SystemUtil.getOsInfo();
        String arch = PlatformUtil.normalizeArch(osInfo.getArch());
        if (StrUtil.isBlank(arch)) {
            return null;
        }
        String os;
        if (osInfo.isWindows()) {
            os = "windows";
        } else if (osInfo.isLinux()) {
            os = "linux";
        } else if (osInfo.isMac() || osInfo.isMacOsX()) {
            os = "darwin";
        } else {
            os = PlatformUtil.normalizeOs(osInfo.getName());
        }
        return StrUtil.isBlank(os) ? null : os + "/" + arch;
    }

    /**
     * 根据 jar 文件解析 jpom 版本信息
     *
     * @param jarFile 文件
     * @return 版本, 打包时间, mainClass
     */
    private static Tuple getJarVersion(File jarFile) {
        Manifest manifest = ManifestUtil.getManifest(jarFile);
        if (manifest != null) {
            Attributes attributes = manifest.getMainAttributes();
            String version = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            if (version != null) {
                // @see VersionUtils#getVersion()
                String timeStamp = attributes.getValue("Jpom-Timestamp");
                timeStamp = parseJpomTime(timeStamp);
                String mainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
                String jpomMinVersion = attributes.getValue("Jpom-Min-Version");
                return new Tuple(version, timeStamp, mainClass, jarFile, jpomMinVersion);
            }
        }
        return null;
    }


    private JpomManifest() {
    }

    /**
     * 单利模式获取Jpom 信息
     *
     * @return this
     */
    public static JpomManifest getInstance() {
        if (JPOM_MANIFEST == null) {
            synchronized (JpomManifest.class) {
                if (JPOM_MANIFEST == null) {
                    JPOM_MANIFEST = buildJpomManifest();
                }
                // 出站 User-Agent 唯一产生点：JpomUserAgent（hutool 全局头 + 非 hutool 通道显式取值）
                JpomUserAgent.applyGlobal(JPOM_MANIFEST.getType(), JPOM_MANIFEST.getVersion());
            }
        }
        return JPOM_MANIFEST;
    }

    public Type getType() {
        return type;
    }

    /**
     * 系统架构（如 amd64、arm64）
     *
     * @return 规范化架构，无法识别时返回 null
     */
    public String getArch() {
        return arch;
    }

    /**
     * 运行平台（os/arch，如 linux/amd64）
     *
     * @return 规范化平台，无法识别时返回 null
     */
    public String getPlatform() {
        return platform;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String randomIdSign() {
        String tempToken = SystemUtil.get("JPOM_SERVER_TEMP_TOKEN");
        return Opt.ofBlankAble(tempToken).orElseGet(() -> SecureUtil.sha1(JpomManifest.this.getPid() + JpomManifest.this.randomId));

    }

    /**
     * 获取当前运行的版本号
     *
     * @return 返回当前版本号
     */
    public String getVersion() {
        return version;
    }

    /**
     * 判断当前是否为调试模式
     *
     * @return jar 为非调试模式
     */
    public boolean isDebug() {
        return "dev".equals(getVersion());
    }

    public void setVersion(String version) {
        if (StrUtil.isNotEmpty(version)) {
            this.version = version;
        }
    }

    public String getJarFile() {
        return jarFile;
    }

    public void setJarFile(String jarFile) {
        this.jarFile = jarFile;
    }

    public String getTimeStamp() {
        if (timeStamp == null) {
            long uptime = SystemUtil.getRuntimeMXBean().getUptime();
            long statTime = System.currentTimeMillis() - uptime;
            return new DateTime(statTime).toString();
        }
        return timeStamp;
    }

    /**
     * 装换打包时间
     *
     * @param timeStamp utc时间
     */
    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 程序运行的端口
     *
     * @return 端口
     */
    public int getPort() {
        if (port == 0) {
            port = JpomApplication.getInstance().getPort();
        }
        return port;
    }

    public String getDataPath() {
        if (StrUtil.isEmpty(dataPath)) {
            dataPath = JpomApplication.getInstance().getDataPath();
        }
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public long getUpTime() {
        return SystemUtil.getRuntimeMXBean().getUptime();
    }

    public String getOsName() {
        return osName;
    }

    public String getInstallId() {
        return installId;
    }

    public void setInstallId(String installId) {
        this.installId = installId;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

    /**
     * 获取当前运行的路径
     *
     * @return jar 或者classPath
     */
    public static File getRunPath() {
        URL location = ClassUtil.getLocation(JpomApplication.getAppClass());
        String file = location.getFile();
        String before = StrUtil.subBefore(file, "!", false);
        return FileUtil.file(before);
    }

    /**
     * 升级之后的旧包
     *
     * @return oldJars
     */
    public static File getOldJarsPath() {
        File runFile = getRunPath().getParentFile();
        return FileUtil.file(runFile, "oldJars");
    }

    /**
     * 转化时间
     *
     * @param timeStamp time
     * @return 默认使用utc
     */
    private static String parseJpomTime(String timeStamp) {
        if (StrUtil.isNotEmpty(timeStamp)) {
            try {
                DateTime dateTime = DateUtil.parseUTC(timeStamp);
                return dateTime.toStringDefaultTimeZone();
            } catch (Exception e) {
                return timeStamp;
            }
        } else {
            return "dev";
        }
    }

    /**
     * 检查是否为jpom包
     *
     * @param path 路径
     * @param type 类型
     * @return 结果消息
     * @see Type#getApplicationClass()
     */
    public static JsonMessage<Tuple> checkJpomJar(String path, Type type) {
        return checkJpomJar(path, type, true);
    }

    public static void setAllowedDowngrade(boolean allowedDowngrade) {
        JpomManifest.allowedDowngrade = allowedDowngrade;
    }

    /**
     * 检查是否为jpom包
     *
     * @param path        路径
     * @param type        类型
     * @param checkRepeat 是否检查版本重复
     * @return 结果消息
     * @see Type#getApplicationClass()
     */
    public static JsonMessage<Tuple> checkJpomJar(String path, Type type, boolean checkRepeat) {
        File jarFile = new File(path);
        Tuple jarVersion = getJarVersion(jarFile);
        if (jarVersion == null) {
            return new JsonMessage<>(405, I18nMessageUtil.get("i18n.invalid_jar_file.e80a"));
        }
        try (JarFile jarFile1 = new JarFile(jarFile)) {
            //Manifest manifest = jarFile1.getManifest();
            //Attributes attributes = manifest.getMainAttributes();
            String mainClass = jarVersion.get(2);
            if (mainClass == null) {
                return new JsonMessage<>(405, I18nMessageUtil.get("i18n.main_class_attribute_not_found.24c9"));
            }
            try (JarClassLoader jarClassLoader = JarClassLoader.load(jarFile)) {
                jarClassLoader.loadClass(mainClass);
            } catch (ClassNotFoundException notFound) {
                return new JsonMessage<>(405, I18nMessageUtil.get("i18n.main_class_not_found.b4b7") + mainClass);
            }
            String applicationClass = type.getApplicationClass();
            ZipEntry entry = jarFile1.getEntry(StrUtil.format("BOOT-INF/classes/{}.class",
                StrUtil.replace(applicationClass, ".", StrUtil.SLASH)));
            if (entry == null) {
                return new JsonMessage<>(405, StrUtil.format(I18nMessageUtil.get("i18n.not_jpom_package.ea3e"), type.name()));
            }
            String version = jarVersion.get(0);
            String timeStamp = jarVersion.get(1);
            String minVersion = jarVersion.get(4);
            if (StrUtil.hasEmpty(version, timeStamp, minVersion)) {
                return new JsonMessage<>(405, I18nMessageUtil.get("i18n.package_missing_info.e277"));
            }
            if (checkRepeat) {
                //
                JpomManifest jpomManifest = JpomManifest.getInstance();
                if (StrUtil.equals(version, jpomManifest.getVersion()) &&
                    StrUtil.equals(timeStamp, jpomManifest.getTimeStamp())) {
                    return new JsonMessage<>(405, I18nMessageUtil.get("i18n.new_package_same_as_running_package.e25a"));
                }
                if (StrUtil.compareVersion(jpomManifest.getVersion(), minVersion) < 0) {
                    return new JsonMessage<>(405, StrUtil.format(I18nMessageUtil.get("i18n.incompatible_program_versions.5291"), jpomManifest.getVersion(), minVersion));
                }
                // 判断降级
                if (!allowedDowngrade && StrUtil.compareVersion(version, jpomManifest.getVersion()) < 0) {
                    return new JsonMessage<>(405, I18nMessageUtil.get("i18n.online_upgrade_cannot_downgrade.d419"));
                }
            }
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.parse_jar.a26e"), e);
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.parse_error.da6d") + e.getMessage());
        }
        return new JsonMessage<>(200, "", jarVersion);
    }

    /**
     * 发布包到对应运行路径
     *
     * @param path    文件路径
     * @param version 新版本号
     */
    public static void releaseJar(String path, String version) {
        File runFile = getRunPath();
        File runPath = runFile.getParentFile();
        if (!runPath.isDirectory()) {
            throw new JpomRuntimeException(runPath.getAbsolutePath() + " error");
        }
        String upgrade = FileUtil.file(runPath, Const.UPGRADE).getAbsolutePath();
        JSONObject jsonObject = null;
        try {
            jsonObject = JsonFileUtil.readJson(upgrade);
        } catch (FileNotFoundException ignored) {
        }
        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }
        jsonObject.put("beforeJar", runFile.getName());
        // 如果升级的版本号一致
        if (StrUtil.equals(version, JpomManifest.getInstance().getVersion())) {
            version = StrUtil.format("{}_{}", version, System.currentTimeMillis());
        }
        String newFile;
        File to;
        while (true) {
            newFile = JpomApplication.getAppType().name() + "-" + version + FileUtil.JAR_FILE_EXT;
            to = FileUtil.file(runPath, newFile);
            if (FileUtil.equals(to, runFile)) {
                version = StrUtil.format("{}_{}", version, RandomUtil.randomInt(1, 100));
                continue;
            }
            break;
        }
        //
        FileUtil.move(new File(path), to, true);
        jsonObject.put("newJar", newFile);
        jsonObject.put("updateTime", new DateTime().toString());
        // 新增升级次数 @author jzy 2021-08-04
        jsonObject.put("upgradeCount", jsonObject.getIntValue("upgradeCount"));
        //
        JsonFileUtil.saveJson(upgrade, jsonObject);
        FileUtil.writeString(newFile, FileUtil.file(runPath, Const.RUN_JAR), CharsetUtil.CHARSET_UTF_8);
    }

    /**
     * 获取当前的管理名文件
     *
     * @return file
     */
    public static File getScriptFile() {
        File runPath = getRunPath().getParentFile().getParentFile();
        String type = JpomApplication.getAppType().name();
        File scriptFile = FileUtil.file(runPath, "bin", StrUtil.format("{}.{}", type, CommandUtil.SUFFIX));
        Assert.state(FileUtil.isFile(scriptFile), StrUtil.format(I18nMessageUtil.get("i18n.command_script_not_found_in_service.25ac"), type, CommandUtil.SUFFIX));
        return scriptFile;
    }

    /**
     * 解析 jpom 安装包
     * <p>
     * 支持 .zip（历史发布包）、.tar.gz/.tgz（2.11.13.2 起发布包格式）以及直接传入的 .jar
     *
     * @param path     文件路径
     * @param type     查找类型
     * @param savePath 保存对文件夹
     * @return 结果文件
     */
    public static File zipFileFind(String path, Type type, String savePath) throws IOException {
        String fileName = FileUtil.getName(path);
        if (StrUtil.endWithAnyIgnoreCase(fileName, ".tar.gz", ".tgz")) {
            return tarGzFileFind(path, type, savePath);
        }
        String extName = FileUtil.extName(path);
        if (StrUtil.endWithIgnoreCase(extName, "zip")) {
            try (ZipFile zipFile = ZipUtil.toZipFile(FileUtil.file(path), CharsetUtil.CHARSET_UTF_8)) {
                Optional<? extends ZipEntry> first = zipFile.stream().filter((Predicate<ZipEntry>) zipEntry -> {
                    String name = zipEntry.getName().toLowerCase();
                    String typeName = type.name().toLowerCase();
                    return StrUtil.startWith(name, "lib/" + typeName) && StrUtil.endWith(name, ".jar");
                }).findFirst();
                Assert.state(first.isPresent(), StrUtil.format(I18nMessageUtil.get("i18n.invalid_zip_file.3092"), type));
                //
                ZipEntry zipEntry = first.get();
                try (InputStream stream = ZipUtil.getStream(zipFile, zipEntry)) {
                    String name = FileUtil.getName(zipEntry.getName());
                    return FileUtil.writeFromStream(stream, FileUtil.file(savePath, name));
                }
            }
        } else if (StrUtil.endWithIgnoreCase(extName, "jar")) {
            return FileUtil.file(path);
        }
        throw new IllegalArgumentException(I18nMessageUtil.get("i18n.not_jpom_install_package.2cca"));
    }

    /**
     * 从 tar.gz/.tgz 发布包中提取 lib 目录下对应类型的 jar 文件
     *
     * @param path     压缩包路径
     * @param type     查找类型
     * @param savePath 保存目录
     * @return 提取到的 jar 文件
     * @throws IOException 解析异常
     */
    private static File tarGzFileFind(String path, Type type, String savePath) throws IOException {
        String typeName = type.name().toLowerCase();
        File foundFile = null;
        try (InputStream fileStream = FileUtil.getInputStream(path);
             BufferedInputStream bufferedStream = new BufferedInputStream(fileStream);
             GzipCompressorInputStream gzipStream = new GzipCompressorInputStream(bufferedStream);
             TarArchiveInputStream tarStream = new TarArchiveInputStream(gzipStream)) {
            TarArchiveEntry tarEntry;
            while ((tarEntry = tarStream.getNextTarEntry()) != null) {
                if (!tarEntry.isFile()) {
                    continue;
                }
                String entryName = normalizePackageEntryName(tarEntry.getName());
                String lowerEntryName = entryName.toLowerCase();
                if (StrUtil.startWith(lowerEntryName, "lib/" + typeName) && StrUtil.endWith(lowerEntryName, ".jar")) {
                    String name = FileUtil.getName(entryName);
                    foundFile = FileUtil.writeFromStream(tarStream, FileUtil.file(savePath, name));
                    break;
                }
            }
        }
        Assert.state(foundFile != null, StrUtil.format(I18nMessageUtil.get("i18n.invalid_zip_file.3092"), type));
        return foundFile;
    }

    /**
     * 归一化压缩包内文件路径：兼容 tar 打包产生的 ./ 前缀与 Windows 反斜杠
     *
     * @param entryName 压缩包内路径
     * @return 归一化后的路径
     */
    private static String normalizePackageEntryName(String entryName) {
        String name = StrUtil.replace(entryName, "\\", "/");
        while (StrUtil.startWith(name, "./")) {
            name = StrUtil.subSuf(name, 2);
        }
        while (StrUtil.startWith(name, "/")) {
            name = StrUtil.subSuf(name, 1);
        }
        return name;
    }
}
