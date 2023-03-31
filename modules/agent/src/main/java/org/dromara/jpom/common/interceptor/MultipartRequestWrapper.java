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

import cn.hutool.core.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
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
            log.error("解密失败", e);
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
            log.error("解密失败", e);
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
