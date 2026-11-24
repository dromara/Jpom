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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * 响应编码兜底配置（服务端与插件端共用，组件扫描自动生效）。
 * <p>
 * 背景：拦截器与部分控制器直接手写 JSON 提示（登录失效、权限拦截、构建记录不存在等），
 * contentType 未带 charset 时容器默认按 ISO-8859-1 输出，中文会变成问号。
 * 安装包的启动脚本使用 {@code -Dspring.config.location} 指定安装目录配置文件，
 * classpath 里的 application.yml 不参与加载；老安装实例的 conf/application.yml 没有
 * {@code server.servlet.encoding} 配置块，因此需要在代码里兜底。
 * <p>
 * 兜底规则（用户显式配置优先，保持既有行为不变）：
 * <ul>
 *     <li>未显式配置 {@code server.servlet.encoding.force} 时，默认开启响应 UTF-8；</li>
 *     <li>请求编码默认不强制覆盖：与自动配置一致，只在请求未声明编码时补齐，
 *     客户端显式声明了 charset 的请求保持原样；</li>
 *     <li>显式配置了 charset/force/force-request/force-response 时按配置执行；</li>
 *     <li>该 bean 会让 Spring Boot 的 HttpEncodingAutoConfiguration 自动退让，
 *     全局只注册一个编码过滤器，并使用最高优先级（与自动配置的顺序一致）。</li>
 * </ul>
 *
 * @author jpom
 * @since 2026/11/18
 */
@Configuration
public class EncodingConfiguration {

    static final String CHARSET_KEY = "server.servlet.encoding.charset";
    static final String FORCE_KEY = "server.servlet.encoding.force";
    static final String FORCE_REQUEST_KEY = "server.servlet.encoding.force-request";
    static final String FORCE_RESPONSE_KEY = "server.servlet.encoding.force-response";

    @Bean
    @ConditionalOnProperty(name = "server.servlet.encoding.enabled", havingValue = "true", matchIfMissing = true)
    public CharacterEncodingFilter characterEncodingFilter(Environment environment) {
        return createFilter(environment);
    }

    /**
     * 按环境配置创建编码过滤器（包级可见，便于单测直接覆盖行为）。
     *
     * @param environment 环境配置
     * @return 编码过滤器
     */
    static CharacterEncodingFilter createFilter(Environment environment) {
        String charset = environment.getProperty(CHARSET_KEY, "UTF-8");
        boolean forceSpecified = environment.containsProperty(FORCE_KEY);
        boolean force = environment.getProperty(FORCE_KEY, Boolean.class, false);
        boolean forceRequest = environment.getProperty(FORCE_REQUEST_KEY, Boolean.class, force);
        boolean forceResponse = environment.getProperty(FORCE_RESPONSE_KEY, Boolean.class, forceSpecified ? force : true);
        return new OrderedCharacterEncodingFilter(charset, forceRequest, forceResponse);
    }

    /**
     * 与 Spring Boot 自动配置一致的顺序（最高优先级），避免被其它过滤器提前写出响应。
     */
    static class OrderedCharacterEncodingFilter extends CharacterEncodingFilter implements Ordered {

        OrderedCharacterEncodingFilter(String charset, boolean forceRequest, boolean forceResponse) {
            super(charset);
            this.setForceRequestEncoding(forceRequest);
            this.setForceResponseEncoding(forceResponse);
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
