/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.context.MessageSource;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 单元测试用的国际化初始化工具。
 * <p>
 * {@code I18nMessageUtil} 通过 {@link SpringUtil#getBean(Class)} 懒加载 {@link MessageSource}，
 * 纯 Mockito 单测没有 Spring 上下文，这里注入一个返回 key 的 mock MessageSource，
 * 让 {@code Assert} 的 i18n 消息构造可以正常执行。
 *
 * @author jpom
 */
public final class TestI18nSupport {

    private static volatile boolean initialized;

    private TestI18nSupport() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("messageSource", messageSource);
        applicationContext.refresh();
        new SpringUtil().setApplicationContext(applicationContext);
        initialized = true;
    }
}
