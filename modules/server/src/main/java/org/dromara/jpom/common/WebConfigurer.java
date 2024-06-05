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
