package cn.keepbx.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件工厂
 *
 * @author bwcx_jzy
 * @date 2019/8/13
 */
public class PluginFactory {

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
    public static void init() {

    }
}
