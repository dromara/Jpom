package io.jpom.plugin;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.util.ClassLoaderUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.common.JpomManifest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件工厂
 *
 * @author bwcx_jzy
 * @date 2019/8/13
 */
public class PluginFactory implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final List<FeatureCallback> FEATURE_CALLBACKS = new ArrayList<>();

    /**
     * 添加回调事件
     *
     * @param featureCallback 回调
     */
    public static void addFeatureCallback(FeatureCallback featureCallback) {
        FEATURE_CALLBACKS.add(featureCallback);
    }

    public static List<FeatureCallback> getFeatureCallbacks() {
        return FEATURE_CALLBACKS;
    }

    /**
     * 正式环境添加依赖
     */
    private static void init() {
        if (JpomManifest.getInstance().isDebug()) {
            return;
        }
        File runPath = JpomManifest.getRunPath().getParentFile();
        File plugin = FileUtil.file(runPath, "plugin");
        if (!plugin.exists() || plugin.isFile()) {
            return;
        }
        File[] files = plugin.listFiles(File::isDirectory);
        if (files == null) {
            return;
        }
        for (File file : files) {
            File lib = FileUtil.file(file, "lib");
            if (!lib.exists() || lib.isFile()) {
                continue;
            }
            File[] listFiles = lib.listFiles((dir, name) -> StrUtil.endWith(name, FileUtil.JAR_FILE_EXT, true));
            if (listFiles == null || listFiles.length <= 0) {
                continue;
            }
            addPlugin(file.getName(), lib);
        }
    }

    private static void addPlugin(String pluginName, File file) {
        DefaultSystemLog.getLog().info("加载：{}插件", pluginName);
        ClassLoader contextClassLoader = ClassLoaderUtil.getClassLoader();
        JarClassLoader.loadJar((URLClassLoader) contextClassLoader, file);
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        init();
    }
}
