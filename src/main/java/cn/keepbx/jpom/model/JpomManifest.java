package cn.keepbx.jpom.model;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.JpomApplication;
import cn.keepbx.jpom.common.JpomApplicationEvent;
import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
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
        ClassLoader classLoader = JpomApplication.class.getClassLoader();
        Enumeration<URL> manifestResources = null;
        try {
            manifestResources = classLoader.getResources("META-INF/MANIFEST.MF");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (manifestResources != null) {
            while (manifestResources.hasMoreElements()) {
                try {
                    try (InputStream inputStream = manifestResources.nextElement().openStream()) {
                        Manifest manifest = new Manifest(inputStream);
                        Attributes attributes = manifest.getMainAttributes();
                        String version = attributes.getValue("Jpom-Project-Version");
                        if (version != null) {
                            JPOM_MANIFEST.setVersion(version);
                            String timeStamp = attributes.getValue("Jpom-Timestamp");
                            JPOM_MANIFEST.setTimeStamp(timeStamp);
                        }
                    }
                } catch (Exception ignored) {
                }
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
    private String version;
    /**
     * 打包时间
     */
    private String timeStamp;
    /**
     * 进程id
     */
    private int pid;

    public int getPid() {
        if (pid == 0) {
            this.pid = JpomApplicationEvent.getPid();
        }
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getVersion() {
        if (StrUtil.isEmpty(version)) {
            return "dev";
        }
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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
}
