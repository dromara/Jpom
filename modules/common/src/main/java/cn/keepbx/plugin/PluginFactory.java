package cn.keepbx.plugin;

import cn.hutool.core.util.ClassUtil;

import java.util.Set;

/**
 * 插件工厂
 *
 * @author bwcx_jzy
 * @date 2019/8/13
 */
public class PluginFactory {

    private static volatile FeatureCallback featureCallback;

    public static void setFeatureCallback(FeatureCallback featureCallback) {
        PluginFactory.featureCallback = featureCallback;
    }

    public static FeatureCallback getFeatureCallback() {
        return featureCallback;
    }

    public static void init() {
        Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation("cn.keepbx.jpom.controller", Feature.class);
        for (Class<?> aClass : classes) {
            System.out.println(aClass);
        }
    }
}
