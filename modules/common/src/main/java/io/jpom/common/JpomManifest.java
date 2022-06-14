/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.common;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.ManifestUtil;
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.GlobalHeaders;
import cn.hutool.http.Header;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.system.ConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.CommandUtil;
import io.jpom.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
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
 * @author jiangzeyin
 * @since 2019/4/7
 */
@Slf4j
public class JpomManifest {

    private volatile static JpomManifest JPOM_MANIFEST;
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

    private static void init() {
        if (JPOM_MANIFEST == null) {
            synchronized (JpomManifest.class) {
                if (JPOM_MANIFEST == null) {
                    JPOM_MANIFEST = new JpomManifest();
                    File jarFile = getRunPath();
                    Tuple jarVersion = getJarVersion(jarFile);
                    if (jarVersion != null) {
                        JPOM_MANIFEST.setVersion(jarVersion.get(0));
                        JPOM_MANIFEST.setTimeStamp(jarVersion.get(1));
                    }
                    JPOM_MANIFEST.setJarFile(FileUtil.getAbsolutePath(jarFile));
                    //
                    JPOM_MANIFEST.randomId = IdUtil.fastSimpleUUID();
                }
                String jpomTag = StrUtil.format("Jpom {}/{}", JPOM_MANIFEST.getType(), JPOM_MANIFEST.getVersion());
                GlobalHeaders.INSTANCE.header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36 " + jpomTag, true);
            }
        }
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
        init();
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
        return SecureUtil.sha1(this.getPid() + this.randomId);
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
            port = ConfigBean.getInstance().getPort();
        }
        return port;
    }

    public String getDataPath() {
        if (StrUtil.isEmpty(dataPath)) {
            dataPath = ConfigBean.getInstance().getDataPath();
        }
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getUpTimeStr() {
        long uptime = SystemUtil.getRuntimeMXBean().getUptime();
        return DateUtil.formatBetween(uptime, BetweenFormatter.Level.SECOND);
    }

    public long getUpTime() {
        return SystemUtil.getRuntimeMXBean().getUptime();
    }

    public String getOsName() {
        return osName;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
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
            return new JsonMessage<>(405, "jar 包文件不合法");
        }
        try (JarFile jarFile1 = new JarFile(jarFile)) {
            //Manifest manifest = jarFile1.getManifest();
            //Attributes attributes = manifest.getMainAttributes();
            String mainClass = jarVersion.get(2);
            if (mainClass == null) {
                return new JsonMessage<>(405, "清单文件中没有找到对应的MainClass属性");
            }
            try (JarClassLoader jarClassLoader = JarClassLoader.load(jarFile)) {
                jarClassLoader.loadClass(mainClass);
            } catch (ClassNotFoundException notFound) {
                return new JsonMessage<>(405, "中没有找到对应的MainClass:" + mainClass);
            }
            String applicationClass = type.getApplicationClass();
            ZipEntry entry = jarFile1.getEntry(StrUtil.format("BOOT-INF/classes/{}.class",
                    StrUtil.replace(applicationClass, ".", StrUtil.SLASH)));
            if (entry == null) {
                return new JsonMessage<>(405, "此包不是Jpom【" + type.name() + "】包");
            }
            String version = jarVersion.get(0);
            String timeStamp = jarVersion.get(1);
            String minVersion = jarVersion.get(4);
            if (StrUtil.hasEmpty(version, timeStamp, minVersion)) {
                return new JsonMessage<>(405, "此包没有版本号、打包时间、最小兼容版本");
            }
            if (checkRepeat) {
                //
                JpomManifest jpomManifest = JpomManifest.getInstance();
                if (StrUtil.equals(version, jpomManifest.getVersion()) &&
                        StrUtil.equals(timeStamp, jpomManifest.getTimeStamp())) {
                    return new JsonMessage<>(405, "新包和正在运行的包一致");
                }
                if (StrUtil.compareVersion(jpomManifest.getVersion(), minVersion) < 0) {
                    return new JsonMessage<>(405, StrUtil.format("当前程序版本 {} 新版程序最低兼容 {} 不能直接升级", jpomManifest.getVersion(), minVersion));
                }
            }
        } catch (Exception e) {
            log.error("解析jar", e);
            return new JsonMessage<>(500, " 解析错误:" + e.getMessage());
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
        String upgrade = FileUtil.file(runPath, ConfigBean.UPGRADE).getAbsolutePath();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) JsonFileUtil.readJson(upgrade);
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
        // 更新管理命令
        List<String> newData = new LinkedList<>();
        //
        String typeName = JpomApplication.getAppType().name().toLowerCase();
        final String[] oldName = new String[]{typeName + ".log"};
        final boolean[] logBack = {true};
        File scriptFile = getScriptFile();
        Charset charset = ExtConfigBean.getInstance().getConsoleLogCharset();
        String finalNewFile = newFile;
        FileUtil.readLines(scriptFile, charset, (LineHandler) line -> {
            if (!line.startsWith(String.valueOf(StrUtil.C_TAB)) &&
                    !line.startsWith(String.valueOf(StrUtil.C_SPACE))) {
                if (StrUtil.containsAny(line, "RUNJAR=")) {
                    // jar 包
                    if ("sh".equals(CommandUtil.SUFFIX)) {
                        newData.add(StrUtil.format("RUNJAR=\"{}\"", finalNewFile));
                    } else if ("bat".equals(CommandUtil.SUFFIX)) {
                        newData.add(StrUtil.format("set RUNJAR={}", finalNewFile));
                    } else {
                        newData.add(line);
                    }
                } else if (SystemUtil.getOsInfo().isWindows()) {
                    // windows 控制台文件相关
                    if (StrUtil.containsAny(line, "set LogName=")) {
                        //
                        oldName[0] = CharSequenceUtil.splitToArray(line, "=")[0];
                        newData.add(StrUtil.format("set LogName={}_{}.log", typeName, System.currentTimeMillis()));
                    } else if (StrUtil.containsAny(line, "set LogBack=")) {
                        // 记忆logBack
                        logBack[0] = Convert.toBool(CharSequenceUtil.splitToArray(line, "=")[1], true);
                        newData.add(line);
                    } else {
                        newData.add(line);
                    }
                } else {
                    newData.add(line);
                }
            } else {
                newData.add(line);
            }
        });
        // 新增升级次数 @author jzy 2021-08-04
        jsonObject.put("upgradeCount", jsonObject.getIntValue("upgradeCount"));
        jsonObject.put("oldLogName", oldName[0]);
        jsonObject.put("logBack", logBack[0]);
        //
        JsonFileUtil.saveJson(upgrade, jsonObject);
        FileUtil.writeLines(newData, scriptFile, charset);
    }

    /**
     * 获取当前的管理名文件
     *
     * @return file
     */
    public static File getScriptFile() {
        File runPath = getRunPath().getParentFile().getParentFile();
        String type = JpomApplication.getAppType().name();
        File scriptFile = FileUtil.file(runPath, StrUtil.format("{}.{}", type, CommandUtil.SUFFIX));
        if (!scriptFile.exists() || scriptFile.isDirectory()) {
            throw new JpomRuntimeException("当前服务中没有命令脚本：" + StrUtil.format("{}.{}", type, CommandUtil.SUFFIX));
        }
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
                Assert.state(first.isPresent(), "上传的压缩包不是 Jpom [" + type + "] 包");
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
        throw new IllegalArgumentException("此文件不是 jpom 安装包");
    }
}
