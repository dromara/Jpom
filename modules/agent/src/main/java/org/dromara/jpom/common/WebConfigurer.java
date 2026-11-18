/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common;

import org.dromara.jpom.common.interceptor.AuthorizeInterceptor;
import org.dromara.jpom.common.validator.ParameterInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author bwcx_jzy
 * @since 2022/12/8
 */
@Configuration
public class WebConfigurer implements WebMvcConfigurer {

    private final ParameterInterceptor parameterInterceptor;
    private final AuthorizeInterceptor authorizeInterceptor;

    public WebConfigurer(ParameterInterceptor parameterInterceptor,
                         AuthorizeInterceptor authorizeInterceptor) {
        this.parameterInterceptor = parameterInterceptor;
        this.authorizeInterceptor = authorizeInterceptor;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(parameterInterceptor).addPathPatterns("/**");
        registry.addInterceptor(authorizeInterceptor).addPathPatterns("/**");
    }

    /**
     * 强制请求/响应使用 UTF-8：插件端手写 JSON 提示（授权错误、下载失败等）在 contentType
     * 未带 charset 时按容器默认 ISO-8859-1 输出，中文会变问号；与服务端保持一致，代码级兜底。
     */
    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> forcedResponseEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter("UTF-8", true);
        FilterRegistrationBean<CharacterEncodingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("forcedResponseEncodingFilter");
        return registration;
    }
}
