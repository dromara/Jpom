/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.permission;

import org.dromara.jpom.common.interceptor.NotLogin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权限拦截 fail-closed 的静态回归：
 * 属于权限管理模块（类级 {@code @Feature} 且 {@code cls != NULL}）的控制器，
 * 其每个 MVC 处理器方法都必须显式声明方法级 {@link Feature}，否则 {@code PermissionInterceptor} 会按无权限拒绝。
 * <p>
 * 新增接口如果忘记加 {@code @Feature}，本测试会失败，避免再次出现“方法级注解缺失即放行”的绕过
 * （例如证书导出接口 {@code CertificateInfoController#export} 曾因此可被任意登录用户访问）。
 * 未声明类级 {@code @Feature} 的控制器属于“登录即可”的公共/自助接口，不在本测试约束内。
 *
 * @author jpom
 */
public class HandlerFeatureAnnotationTest {

    private static boolean isHandlerMethod(Method method) {
        return method.isAnnotationPresent(RequestMapping.class)
            || method.isAnnotationPresent(GetMapping.class)
            || method.isAnnotationPresent(PostMapping.class)
            || method.isAnnotationPresent(PutMapping.class)
            || method.isAnnotationPresent(DeleteMapping.class)
            || method.isAnnotationPresent(PatchMapping.class);
    }

    @Test
    public void testAllNonPublicHandlersDeclareFeature() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        Set<BeanDefinition> candidates = scanner.findCandidateComponents("org.dromara.jpom");
        List<String> missing = new ArrayList<>();
        for (BeanDefinition definition : candidates) {
            Class<?> clazz = Class.forName(definition.getBeanClassName());
            Feature classFeature = clazz.getAnnotation(Feature.class);
            boolean permissionManaged = classFeature != null && classFeature.cls() != ClassFeature.NULL;
            boolean classNotLogin = clazz.isAnnotationPresent(NotLogin.class);
            for (Method method : clazz.getDeclaredMethods()) {
                if (!isHandlerMethod(method)) {
                    continue;
                }
                boolean hasFeature = AnnotatedElementUtils.hasAnnotation(method, Feature.class);
                boolean publicHandler = classNotLogin || method.isAnnotationPresent(NotLogin.class);
                if (!hasFeature && !publicHandler && permissionManaged) {
                    missing.add(clazz.getName() + "#" + method.getName());
                }
            }
        }
        assertTrue(missing.isEmpty(),
            "以下权限管理模块的 MVC 处理器缺少方法级 @Feature（请补充 @Feature(method=...) 或 @Feature(method=MethodFeature.NULL, log=false)）：\n"
                + String.join("\n", missing));
    }
}
