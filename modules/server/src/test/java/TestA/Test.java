package TestA;

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
 * @author bwcx_jzy
 * @date 2019/8/14
 */
public class Test {
    public static void main(String[] args) {
        Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation("cn.keepbx.jpom.controller", Feature.class);
//        System.out.println(classes);
        Map<ClassFeature, Set<MethodFeature>> classFeatureSetMap = new HashMap<>();
        classes.forEach(aClass -> {
            Feature annotation = aClass.getAnnotation(Feature.class);
            ClassFeature classFeature = annotation.cls();
            if (classFeature == ClassFeature.NULL) {
                return;
            }
            Set<MethodFeature> methodFeatures1 = classFeatureSetMap.computeIfAbsent(classFeature, classFeature1 -> new HashSet<>());
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
        System.out.println(classFeatureSetMap);
    }
}
