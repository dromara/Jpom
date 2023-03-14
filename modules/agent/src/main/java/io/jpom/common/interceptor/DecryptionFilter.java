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
package io.jpom.common.interceptor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import io.jpom.encrypt.EncryptFactory;
import io.jpom.encrypt.Encryptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

/**
 * @author loyal.f
 * @since 2023/3/13
 */
@Configuration
@Slf4j
@Order(1)
public class DecryptionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String transportEncryption = request.getHeader("transport-encryption");
        // 兼容没有没有传入
        Encryptor encryptor;
        try {
            encryptor = EncryptFactory.createEncryptor(Convert.toInt(transportEncryption, 0));
        } catch (NoSuchAlgorithmException e) {
            log.error("获取解密分发失败", e);
            chain.doFilter(servletRequest, response);
            return;
        }
        log.debug("当前请求需要解密：{}", encryptor.name());
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
                log.error("解密失败", e);
                temp = body;
            }
            BodyRewritingRequestWrapper requestWrapper = new BodyRewritingRequestWrapper(request, temp.getBytes(StandardCharsets.UTF_8));
            chain.doFilter(requestWrapper, response);
        } else if (ServletFileUpload.isMultipartContent(request)) {
            // 文件上传
            HttpServletRequestWrapper wrapper = new MultipartRequestWrapper(request, encryptor);
            chain.doFilter(wrapper, response);
        } else {
            log.warn("当前请求类型不支持加密：{}", contentType);
            chain.doFilter(servletRequest, response);
        }
    }
}
