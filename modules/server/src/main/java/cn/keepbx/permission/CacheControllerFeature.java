package cn.keepbx.permission;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 缓存
 *
 * @author bwcx_jzy
 * @date 2019/8/15
 */
public class CacheControllerFeature {

    private static final Map<ClassFeature, Set<MethodFeature>> FEATURE_MAP = new HashMap<>();

    public static Map<ClassFeature, Set<MethodFeature>> getFeatureMap() {
        return FEATURE_MAP;
    }

    /**
     * 扫描包
     */
    public static void init() {
        Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation("cn.keepbx.jpom.controller", Feature.class);
        classes.forEach(aClass -> {
            Feature annotation = aClass.getAnnotation(Feature.class);
            ClassFeature classFeature = annotation.cls();
            if (classFeature == ClassFeature.NULL) {
                return;
            }
            Set<MethodFeature> methodFeatures1 = FEATURE_MAP.computeIfAbsent(classFeature, classFeature1 -> new HashSet<>());
            Method[] publicMethods = ReflectUtil.getPublicMethods(aClass);
            Set<MethodFeature> methodFeatures = new HashSet<>();
            for (Method publicMethod : publicMethods) {
                Feature publicMethodAnnotation = publicMethod.getAnnotation(Feature.class);
                if (publicMethodAnnotation == null) {
                    continue;
                }
                MethodFeature methodFeature = publicMethodAnnotation.method();
                if (methodFeature == MethodFeature.NULL) {
                    continue;
                }
                methodFeatures.add(methodFeature);
            }
            methodFeatures1.addAll(methodFeatures);
        });
    }
}
