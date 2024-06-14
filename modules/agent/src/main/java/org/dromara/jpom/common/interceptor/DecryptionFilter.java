/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.interceptor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.transport.BodyRewritingRequestWrapper;
import org.dromara.jpom.common.transport.MultipartRequestWrapper;
import org.dromara.jpom.common.transport.ParameterRequestWrapper;
import org.dromara.jpom.encrypt.EncryptFactory;
import org.dromara.jpom.encrypt.Encryptor;
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
            log.error(I18nMessageUtil.get("i18n.get_decrypt_distribution_failure.4feb"), e);
            chain.doFilter(servletRequest, response);
            return;
        }
        log.debug(I18nMessageUtil.get("i18n.request_needs_decoding.d4d7"), encryptor.name());
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
                log.error(I18nMessageUtil.get("i18n.decode_failure.822e"), e);
                temp = body;
            }
            BodyRewritingRequestWrapper requestWrapper = new BodyRewritingRequestWrapper(request, temp.getBytes(StandardCharsets.UTF_8));
            chain.doFilter(requestWrapper, response);
        } else if (ServletFileUpload.isMultipartContent(request)) {
            // 文件上传
            HttpServletRequestWrapper wrapper = new MultipartRequestWrapper(request, encryptor);
            chain.doFilter(wrapper, response);
        } else {
            log.warn(I18nMessageUtil.get("i18n.request_type_not_supported_for_decoding.ea2e"), contentType);
            chain.doFilter(servletRequest, response);
        }
    }
}
