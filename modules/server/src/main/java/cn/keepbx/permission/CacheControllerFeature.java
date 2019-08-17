package cn.keepbx.permission;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
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

    public static Map<ClassFeature, Set<MethodFeature>> getFeatureMap() {
        return FEATURE_MAP;
    }

    public static UrlFeature getUrlFeature(String url) {
        return URL_FEATURE_MAP.get(url);
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
