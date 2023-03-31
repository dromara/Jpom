/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dromara.jpom.common;

import org.dromara.jpom.common.interceptor.IpInterceptor;
import org.dromara.jpom.common.interceptor.LoginInterceptor;
import org.dromara.jpom.common.interceptor.PermissionInterceptor;
import org.dromara.jpom.common.validator.ParameterInterceptor;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @author bwcx_jzy
 * @since 2022/12/8
 */
@Configuration
public class WebConfigurer implements WebMvcConfigurer, WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Resource
    private ParameterInterceptor parameterInterceptor;
    @Resource
    private IpInterceptor ipInterceptor;
    @Resource
    private LoginInterceptor loginInterceptor;
    @Resource
    private PermissionInterceptor permissionInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ipInterceptor).excludePathPatterns(ServerOpenApi.API + "**");
        registry.addInterceptor(loginInterceptor).excludePathPatterns(ServerOpenApi.API + "**");
        registry.addInterceptor(parameterInterceptor).addPathPatterns("/**");
        registry.addInterceptor(permissionInterceptor).excludePathPatterns(ServerOpenApi.API + "**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/dist/css/");
//        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/dist/js/");
//        registry.addResourceHandler("/img/**").addResourceLocations("classpath:/dist/img/");
//        registry.addResourceHandler("/fonts/**").addResourceLocations("classpath:/dist/fonts/");
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
        mappings.add("js", "application/javascript;charset=utf-8");
        factory.setMimeMappings(mappings);
    }
}
