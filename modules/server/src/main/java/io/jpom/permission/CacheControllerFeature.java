package io.jpom.permission;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 缓存
 *
 * @author bwcx_jzy
 * @date 2019/8/15
 */
public class CacheControllerFeature {

    private static final Map<ClassFeature, Set<MethodFeature>> FEATURE_MAP = new HashMap<>();

    private static final Map<String, UrlFeature> URL_FEATURE_MAP = new TreeMap<>();

    /**
     * 系统管理员使用的权限
     */
    private static final List<String> SYSTEM_URL = new ArrayList<>();

    public static Map<ClassFeature, Set<MethodFeature>> getFeatureMap() {
        return FEATURE_MAP;
    }

    /**
     * 判断是否为系统管理员权限url
     *
     * @param url url
     * @return true 只能是系统管理员访问
     */
    public static boolean isSystemUrl(String url) {
        return SYSTEM_URL.contains(url);
    }

    /**
     * 获取url 功能方法对象
     *
     * @param url url
     * @return url功能
     */
    public static UrlFeature getUrlFeature(String url) {
        return URL_FEATURE_MAP.get(url);
    }


    /**
     * 扫描包
     */
    public static void init() {
        // 扫描系统管理员
        Set<Class<?>> classes = ClassUtil.scanPackage("io.jpom.controller");
        classes.forEach(aClass -> {
            RequestMapping requestMapping = aClass.getAnnotation(RequestMapping.class);
            Method[] publicMethods = ReflectUtil.getPublicMethods(aClass);
            for (Method publicMethod : publicMethods) {
                SystemPermission systemPermission = publicMethod.getAnnotation(SystemPermission.class);
                if (systemPermission == null) {
                    continue;
                }
                RequestMapping methodAnnotation = publicMethod.getAnnotation(RequestMapping.class);
                String format = String.format("/%s/%s", requestMapping.value()[0], methodAnnotation.value()[0]);
                format = FileUtil.normalize(format);
                SYSTEM_URL.add(format);
            }
        });
        // 扫描功能方法
        classes.forEach(aClass -> {
            Feature annotation = aClass.getAnnotation(Feature.class);
            if (annotation == null) {
                return;
            }
            ClassFeature classFeature = annotation.cls();
            if (classFeature == ClassFeature.NULL) {
                return;
            }
            RequestMapping requestMapping = aClass.getAnnotation(RequestMapping.class);
            //
            Set<MethodFeature> methodFeatures1 = FEATURE_MAP.computeIfAbsent(classFeature, classFeature1 -> new HashSet<>());
            Method[] publicMethods = ReflectUtil.getPublicMethods(aClass);
            //
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
                RequestMapping methodAnnotation = publicMethod.getAnnotation(RequestMapping.class);
                methodFeatures.add(methodFeature);
                //
                String format = String.format("/%s/%s", requestMapping.value()[0], methodAnnotation.value()[0]);
                format = FileUtil.normalize(format);
                URL_FEATURE_MAP.put(format, new UrlFeature(format, classFeature, methodFeature));
            }
            methodFeatures1.addAll(methodFeatures);
        });
    }

    public static class UrlFeature {
        private String url;
        private ClassFeature classFeature;
        private MethodFeature methodFeature;

        public String getUrl() {
            return url;
        }

        public ClassFeature getClassFeature() {
            return classFeature;
        }

        public MethodFeature getMethodFeature() {
            return methodFeature;
        }

        UrlFeature(String url, ClassFeature classFeature, MethodFeature methodFeature) {
            this.url = url;
            this.classFeature = classFeature;
            this.methodFeature = methodFeature;
        }
    }
}
