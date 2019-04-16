package cn.keepbx.jpom.model.system;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import org.springframework.boot.ApplicationHome;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Jpom 的运行信息
 *
 * @author jiangzeyin
 * @date 2019/4/7
 */
public class JpomManifest {
    private static final JpomManifest JPOM_MANIFEST;

    static {
        JPOM_MANIFEST = new JpomManifest();
        File jarFile = getRunPath();
        if (jarFile.isFile()) {
            JarFile jarFile1;
            try {
                jarFile1 = new JarFile(jarFile);
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
        return JPOM_MANIFEST;
    }

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
    private int pid;

    /**
     * 记录当前程序进程
     */
    private static int PID = 0;

    public int getPid() {
        if (pid == 0) {
            this.pid = getRunPid();
        }
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
        return timeStamp;
    }

    /**
     * 装换打包时间
     *
     * @param timeStamp utc时间
     */
    public void setTimeStamp(String timeStamp) {
        if (StrUtil.isNotEmpty(timeStamp)) {
            DateTime dateTime = DateUtil.parseUTC(timeStamp);
            this.timeStamp = dateTime.toStringDefaultTimeZone();
        } else {
            this.timeStamp = timeStamp;
        }
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
        ApplicationHome home = new ApplicationHome(JpomManifest.class);
        String path = (home.getSource() == null ? "" : home.getSource().getAbsolutePath());
        return FileUtil.file(path);
    }

    /**
     * 获取当前程序进程id
     *
     * @return pid
     */
    public static int getRunPid() {
        if (PID == 0) {
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            // format: "pid@hostname"
            String name = runtime.getName();
            try {
                PID = Integer.parseInt(name.substring(0, name.indexOf('@')));
            } catch (Exception e) {
                PID = -1;
            }
        }
        return PID;
    }
}
