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
package org.dromara.jpom.common.interceptor;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.dromara.jpom.common.transport.BodyRewritingRequestWrapper;
import org.dromara.jpom.common.transport.MultipartRequestWrapper;
import org.dromara.jpom.common.transport.ParameterRequestWrapper;
import org.dromara.jpom.configuration.WebConfig;
import org.dromara.jpom.encrypt.EncryptFactory;
import org.dromara.jpom.encrypt.Encryptor;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author bwcx_jzy
 * @since 2024/2/22
 */
@Configuration
@Slf4j
@Order(1)
public class ServerDecryptionFilter implements Filter {

    private final Encryptor encryptor;

    public ServerDecryptionFilter(ServerConfig serverConfig) {
        Encryptor encryptor1;
        WebConfig config = serverConfig.getWeb();
        String transportEncryption = config.getTransportEncryption();
        transportEncryption = ObjectUtil.defaultIfNull(transportEncryption, StrUtil.EMPTY).toUpperCase();
        switch (transportEncryption) {
            case "NONE":
                encryptor1 = null;
                break;
            case "BASE64":
                try {
                    encryptor1 = EncryptFactory.createEncryptor(1);
                } catch (Exception e) {
                    log.error("获取解密实现失败", e);
                    encryptor1 = null;
                }
                break;
            default:
                log.warn("不支持的编码方式：{}", transportEncryption);
                encryptor1 = null;
                break;
        }
        encryptor = encryptor1;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (encryptor == null) {
            chain.doFilter(servletRequest, response);
            return;
        }
        log.debug("当前请求需要解码：{}", encryptor.name());
        String contentType = request.getContentType();
        if (ContentType.isDefault(contentType)) {
            // 普通表单
            HttpServletRequestWrapper wrapper = new ParameterRequestWrapper(request, encryptor);
            chain.doFilter(wrapper, response);
        } else if (StrUtil.startWithIgnoreCase(contentType, MediaType.APPLICATION_JSON_VALUE)) {
            String body = ServletUtil.getBody(request);
            String temp;
            try {
                temp = encryptor.decrypt(body);
            } catch (Exception e) {
                log.error("解码失败", e);
                temp = body;
            }
            BodyRewritingRequestWrapper requestWrapper = new BodyRewritingRequestWrapper(request, temp.getBytes(StandardCharsets.UTF_8));
            chain.doFilter(requestWrapper, response);
        } else if (ServletFileUpload.isMultipartContent(request)) {
            // 文件上传
            HttpServletRequestWrapper wrapper = new MultipartRequestWrapper(request, encryptor);
            chain.doFilter(wrapper, response);
        } else {
            log.warn("当前请求类型不支持解码：{}", contentType);
            chain.doFilter(servletRequest, response);
        }
    }
}
