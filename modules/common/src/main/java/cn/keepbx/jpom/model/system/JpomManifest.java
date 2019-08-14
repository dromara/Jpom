package cn.keepbx.jpom.model.system;

import cn.hutool.core.date.BetweenFormater;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.JpomApplication;
import cn.keepbx.jpom.common.Type;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.JpomRuntimeException;
import cn.keepbx.util.CommandUtil;
import cn.keepbx.util.JsonFileUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.system.ApplicationHome;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Jpom 的运行信息
 *
 * @author jiangzeyin
 * @date 2019/4/7
 */
public class JpomManifest {
    private static JpomManifest JPOM_MANIFEST;
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
    private Type type = JpomApplication.getAppType();
    /**
     * 端口号
     */
    private int port;
    /**
     * Jpom 的数据目录
     */
    private String dataPath;

    private static synchronized void init() {
        if (JPOM_MANIFEST != null) {
            return;
        }
        JPOM_MANIFEST = new JpomManifest();
        File jarFile = getRunPath();
        if (jarFile.isFile()) {
            try (JarFile jarFile1 = new JarFile(jarFile)) {
                Manifest manifest = jarFile1.getManifest();
                Attributes attributes = manifest.getMainAttributes();
                String version = attributes.getValue("Jpom-Project-Version");
                if (version != null) {
                    JPOM_MANIFEST.setVersion(version);
                    String timeStamp = attributes.getValue("Jpom-Timestamp");
                    JPOM_MANIFEST.setTimeStamp(timeStamp);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        this.timeStamp = parseJpomTime(timeStamp);
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

    public String getUpTime() {
        long uptime = SystemUtil.getRuntimeMXBean().getUptime();
        return DateUtil.formatBetween(uptime, BetweenFormater.Level.SECOND);
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
        ApplicationHome home = new ApplicationHome(JpomApplication.getAppClass());
        String path = (home.getSource() == null ? "" : home.getSource().getAbsolutePath());
        return FileUtil.file(path);
    }

    /**
     * 转化时间
     *
     * @param timeStamp time
     * @return 默认使用utc
     */
    public static String parseJpomTime(String timeStamp) {
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
     * @param path    路径
     * @param clsName 类名
     * @return 结果消息
     */
    public static JsonMessage checkJpomJar(String path, Class clsName) {
        String version;
        File jarFile = new File(path);
        try (JarFile jarFile1 = new JarFile(jarFile)) {
            Manifest manifest = jarFile1.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            String mainClass = attributes.getValue("Main-Class");
            if (mainClass == null) {
                return new JsonMessage(405, "清单文件中没有找到对应的MainClass属性");
            }
            JarClassLoader jarClassLoader = JarClassLoader.load(jarFile);
            try {
                jarClassLoader.loadClass(mainClass);
            } catch (ClassNotFoundException notFound) {
                return new JsonMessage(405, "中没有找到对应的MainClass:" + mainClass);
            }
            ZipEntry entry = jarFile1.getEntry(StrUtil.format("BOOT-INF/classes/{}.class",
                    StrUtil.replace(clsName.getName(), ".", "/")));
            if (entry == null) {
                return new JsonMessage(405, "此包不是Jpom【" + JpomApplication.getAppType().name() + "】包");
            }
            version = attributes.getValue("Jpom-Project-Version");
            if (StrUtil.isEmpty(version)) {
                return new JsonMessage(405, "此包没有版本号");
            }
            String timeStamp = attributes.getValue("Jpom-Timestamp");
            if (StrUtil.isEmpty(timeStamp)) {
                return new JsonMessage(405, "此包没有版本号");
            }
            timeStamp = parseJpomTime(timeStamp);
            if (StrUtil.equals(version, JpomManifest.getInstance().getVersion()) &&
                    StrUtil.equals(timeStamp, JpomManifest.getInstance().getTimeStamp())) {
                return new JsonMessage(405, "新包和正在运行的包一致");
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("解析jar", e);
            return new JsonMessage(500, " 解析错误:" + e.getMessage());
        }
        return new JsonMessage(200, version);
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
        String newFile = JpomApplication.getAppType().name() + "-" + version + FileUtil.JAR_FILE_EXT;
        File to = FileUtil.file(runPath, newFile);
        if (to.exists()) {
            throw new JpomRuntimeException(newFile + " 已经存在啦");
        }
        FileUtil.move(new File(path), to, true);
        jsonObject.put("newJar", newFile);
        jsonObject.put("updateTime", new DateTime().toString());
        JsonFileUtil.saveJson(upgrade, jsonObject);
        // 更新管理命令
        List<String> newData = new LinkedList<>();
        FileUtil.readLines(getScriptFile(), JpomApplication.getCharset(), (LineHandler) line -> {
            if (!line.startsWith(String.valueOf(StrUtil.C_TAB)) &&
                    !line.startsWith(String.valueOf(StrUtil.C_SPACE)) &&
                    StrUtil.containsAny(line, "RUNJAR=")) {
                if ("sh".equals(CommandUtil.SUFFIX)) {
                    newData.add(StrUtil.format("RUNJAR=\"{}\"", newFile));
                } else if ("bat".equals(CommandUtil.SUFFIX)) {
                    newData.add(StrUtil.format("set RUNJAR={}", newFile));
                } else {
                    newData.add(line);
                }
            } else {
                newData.add(line);
            }
        });
        FileUtil.writeLines(newData, getScriptFile(), JpomApplication.getCharset());
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
}
