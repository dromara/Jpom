/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.transport;

import cn.hutool.core.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.encrypt.Encryptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author loyal.f
 * @since 2023/3/13
 */
@Slf4j
public class MultipartRequestWrapper extends StandardMultipartHttpServletRequest {

    private final Map<String, String[]> parameterMap;

    public MultipartRequestWrapper(HttpServletRequest request, Encryptor encryptor) {
        super(request);
        Map<String, String[]> parameterMap = super.getParameterMap();
        Map<String, String[]> decryptMap = new HashMap<>();
        try {
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String key = entry.getKey();
                String[] value = entry.getValue();
                for (int i = 0; i < value.length; i++) {
                    value[i] = encryptor.decrypt(value[i]);
                }
                decryptMap.put(encryptor.decrypt(key), value);
            }
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.decrypt_failure.ad83"), e);
        }
        this.parameterMap = decryptMap;
        // 处理文件名
        MultiValueMap<String, MultipartFile> multipartFiles = super.getMultipartFiles();
        try {
            MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<>(multipartFiles.size());
            for (String key : multipartFiles.keySet()) {
                files.put(encryptor.decrypt(key), multipartFiles.remove(key));
            }
            setMultipartFiles(files);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.decrypt_failure.ad83"), e);
        }
    }


    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameterMap.get(name);
        return ArrayUtil.get(values, 0);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }
}
