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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
                    log.error(I18nMessageUtil.get("i18n.get_decrypt_implementation_failure.e77a"), e);
                    encryptor1 = null;
                }
                break;
            default:
                log.warn(I18nMessageUtil.get("i18n.unsupported_encoding_with_placeholder.3bd9"), transportEncryption);
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
