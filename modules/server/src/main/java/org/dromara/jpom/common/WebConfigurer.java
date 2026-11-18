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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CharacterEncodingFilter;
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

    /**
     * 强制请求/响应使用 UTF-8。拦截器与部分控制器直接手写 JSON 提示（登录失效、权限拦截、
     * 构建记录不存在等），contentType 未带 charset 时容器默认按 ISO-8859-1 输出，中文会变问号。
     * 默认配置的 server.servlet.encoding.force-response 依赖安装目录配置文件存在该配置块，
     * 老安装实例与裸 jar 运行可能缺失，这里代码级兜底（与自动配置的过滤器幂等）。
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
