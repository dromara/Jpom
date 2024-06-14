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
import cn.hutool.http.GlobalHeaders;
import cn.hutool.http.Header;
import cn.hutool.http.useragent.UserAgentInfo;
import cn.hutool.system.JavaInfo;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import cn.keepbx.jpom.Type;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.system.JpomRuntimeException;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.JsonFileUtil;
import org.springframework.util.Assert;

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

    private static String buildOsInfo() {
        // Windows NT 10.0; Win64; x64
        OsInfo osInfo = SystemUtil.getOsInfo();
        JavaInfo javaInfo = SystemUtil.getJavaInfo();
        boolean inDocker = StrUtil.isNotEmpty(SystemUtil.get("JPOM_PKG"));
        String osName = Opt.ofBlankAble(osInfo.getName()).orElseGet(() -> UserAgentInfo.NameUnknown);
        return StrUtil.format("{} {}; {}; {}",
            inDocker ? "docker" : osName,
            Opt.ofBlankAble(osInfo.getVersion()).orElse("0"),
            Opt.ofBlankAble(osInfo.getArch()).orElse(UserAgentInfo.NameUnknown),
            Opt.ofBlankAble(javaInfo.getVersion()).orElse(UserAgentInfo.NameUnknown)
        );
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
                String jpomTag = StrUtil.format("Jpom {}/{}", JPOM_MANIFEST.getType(), JPOM_MANIFEST.getVersion());
                String osInfo = buildOsInfo();
                // Mozilla/5.0 (${os-name} ${os-version}; ${os-arch}; ${jdk-version}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.0000.000 Safari/537.36 ${jpom-type}/${jpom-version}
                GlobalHeaders.INSTANCE.header(Header.USER_AGENT, StrUtil.format("Mozilla/5.0 ({}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.0000.000 Safari/537.36 {}", osInfo, jpomTag), true);
            }
        }
        return JPOM_MANIFEST;
    }

    public Type getType() {
        return type;
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
     *
     * @param path     文件路径
     * @param type     查找类型
     * @param savePath 保存对文件夹
     * @return 结果文件
     */
    public static File zipFileFind(String path, Type type, String savePath) throws IOException {
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
}
