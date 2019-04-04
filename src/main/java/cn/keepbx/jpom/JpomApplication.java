package cn.keepbx.jpom;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.ApplicationBuilder;
import cn.jiangzeyin.common.EnableCommonBoot;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.common.interceptor.PermissionInterceptor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * jpom 启动类
 * Created by jiangzeyin on 2017/9/14.
 *
 * @author jiangzeyin
 */
@SpringBootApplication
@ServletComponentScan
@EnableCommonBoot
public class JpomApplication {
    private static String[] args;

    private static final JpomManifest JPOM_MANIFEST;

    public static JpomManifest getJpomManifest() {
        return JPOM_MANIFEST;
    }

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

    /**
     * 启动执行
     *
     * @param args 参数
     */
    public static void main(String[] args) throws Exception {
        JpomApplication.args = args;
        ApplicationBuilder.createBuilder(JpomApplication.class)
                .addHttpMessageConverter(new StringHttpMessageConverter(CharsetUtil.CHARSET_UTF_8))
                // 拦截器
                .addInterceptor(LoginInterceptor.class).addInterceptor(PermissionInterceptor.class)
                .run(args);
    }

    /**
     * 获取启动参数
     *
     * @param name 参数名
     * @return 值
     */
    public static String getArgs(String name) {
        if (args == null) {
            return null;
        }
        for (String item : args) {
            item = StrUtil.trim(item);
            if (item.startsWith("--" + name + "=")) {
                return item.substring(name.length() + 3);
            }
        }
        return null;
    }

    /**
     * jpom 的Manifest
     */
    public static class JpomManifest {
        private String version;
        private String timeStamp;

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

        public void setTimeStamp(String timeStamp) {
            this.timeStamp = timeStamp;
        }
    }
}
