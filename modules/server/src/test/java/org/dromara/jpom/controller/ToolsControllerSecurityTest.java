/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller;

import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.permission.SystemPermission;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * /tools 接口权限与参数校验回归测试
 * <p>
 * 网络工具（net-ping/net-telnet/ip-list）需要系统管理员权限，cron 工具保持所有登录用户可用；
 * port 必须在 1~65535 之间，host 长度为 1~255，timeout 在代码中夹紧到 1~60 秒。
 *
 * @author jpom
 * @since 2026/11/2
 */
public class ToolsControllerSecurityTest {

    @Test
    public void testNetworkToolsRequireSystemPermission() throws NoSuchMethodException {
        Assertions.assertNotNull(ToolsController.class.getMethod("ping", String.class, int.class)
            .getAnnotation(SystemPermission.class), "net-ping 必须要求系统管理员权限");
        Assertions.assertNotNull(ToolsController.class.getMethod("telnet", String.class, int.class, int.class)
            .getAnnotation(SystemPermission.class), "net-telnet 必须要求系统管理员权限");
        Assertions.assertNotNull(ToolsController.class.getMethod("ipList")
            .getAnnotation(SystemPermission.class), "ip-list 必须要求系统管理员权限");
    }

    @Test
    public void testCronToolKeepsPublicForLoginUsers() throws NoSuchMethodException {
        Method cron = ToolsController.class.getMethod("cron", String.class, int.class, String.class, boolean.class);
        Assertions.assertNull(cron.getAnnotation(SystemPermission.class), "cron 工具不应要求系统管理员权限");
    }

    @Test
    public void testControllerClassHasNoSystemPermission() {
        Assertions.assertNull(ToolsController.class.getAnnotation(SystemPermission.class),
            "不要在 ToolsController 类上加 @SystemPermission，否则会连带限制 cron 工具");
    }

    @Test
    public void testTelnetPortRange() throws NoSuchMethodException {
        Method telnet = ToolsController.class.getMethod("telnet", String.class, int.class, int.class);
        ValidatorItem port = findValidatorItem(telnet, 1);
        Assertions.assertNotNull(port, "net-telnet 的 port 参数必须有 @ValidatorItem 校验");
        Assertions.assertEquals(ValidatorRule.NUMBERS, port.value());
        Assertions.assertEquals("1:65535", port.range(), "port 必须限制在 1~65535");
    }

    @Test
    public void testHostLengthRule() throws NoSuchMethodException {
        Method ping = ToolsController.class.getMethod("ping", String.class, int.class);
        Method telnet = ToolsController.class.getMethod("telnet", String.class, int.class, int.class);
        ValidatorItem pingHost = findValidatorItem(ping, 0);
        ValidatorItem telnetHost = findValidatorItem(telnet, 0);
        Assertions.assertNotNull(pingHost, "net-ping 的 host 参数必须有 @ValidatorItem 校验");
        Assertions.assertNotNull(telnetHost, "net-telnet 的 host 参数必须有 @ValidatorItem 校验");
        Assertions.assertEquals("1:255", pingHost.range(), "net-ping 的 host 长度必须限制在 1~255");
        Assertions.assertEquals("1:255", telnetHost.range(), "net-telnet 的 host 长度必须限制在 1~255");
    }

    @Test
    public void testTimeoutParameterValidated() throws NoSuchMethodException {
        Method ping = ToolsController.class.getMethod("ping", String.class, int.class);
        Method telnet = ToolsController.class.getMethod("telnet", String.class, int.class, int.class);
        Assertions.assertNotNull(findValidatorItem(ping, 1), "net-ping 的 timeout 参数必须有 @ValidatorItem 校验");
        Assertions.assertNotNull(findValidatorItem(telnet, 2), "net-telnet 的 timeout 参数必须有 @ValidatorItem 校验");
    }

    @Test
    public void testTimeoutClamp() {
        Assertions.assertEquals(1, ToolsController.normalizeTimeout(0));
        Assertions.assertEquals(1, ToolsController.normalizeTimeout(-1));
        Assertions.assertEquals(1, ToolsController.normalizeTimeout(1));
        Assertions.assertEquals(30, ToolsController.normalizeTimeout(30));
        Assertions.assertEquals(60, ToolsController.normalizeTimeout(60));
        Assertions.assertEquals(60, ToolsController.normalizeTimeout(61));
        Assertions.assertEquals(60, ToolsController.normalizeTimeout(3600));
    }

    private static ValidatorItem findValidatorItem(Method method, int parameterIndex) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Assertions.assertTrue(parameterIndex < parameterAnnotations.length, "参数下标越界：" + method);
        for (Annotation annotation : parameterAnnotations[parameterIndex]) {
            if (annotation instanceof ValidatorItem) {
                return (ValidatorItem) annotation;
            }
        }
        return null;
    }
}
