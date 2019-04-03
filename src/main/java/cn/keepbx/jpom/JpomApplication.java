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

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
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

    private static String version;

    /**
     * 启动执行
     *
     * @param args 参数
     */
    public static void main(String[] args) throws Exception {
        JpomApplication.getVersion();
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
     * 获取当前jpom 版本号
     *
     * @return version
     */
    public static String getVersion() {
        if (version == null) {
            try {
                ClassLoader classLoader = JpomApplication.class.getClassLoader();
                String ver = getVersion(classLoader.getResources("META-INF/MANIFEST.MF"));
                JpomApplication.version = StrUtil.emptyToDefault(ver, "dev");
            } catch (Exception ignored) {
                JpomApplication.version = "err";
            }
        }
        return JpomApplication.version;
    }

    private static String getVersion(Enumeration<URL> manifestResources) {
        while (manifestResources.hasMoreElements()) {
            try {
                try (InputStream inputStream = manifestResources.nextElement().openStream()) {
                    Manifest manifest = new Manifest(inputStream);
                    String startClass = manifest.getMainAttributes().getValue("Jpom-Project-Version");
                    if (startClass != null) {
                        return startClass;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
