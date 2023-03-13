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

import cn.hutool.core.util.ObjectUtil;
import io.jpom.encrypt.EncryptFactory;
import io.jpom.encrypt.Encryptor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author loyal.f
 * @date 2023/3/13
 */
public class DecryptionFilter implements Filter {


    @SneakyThrows
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String transportEncryption =request.getHeader("transport-encryption");
        Encryptor encryptor= EncryptFactory.createEncryptor(Integer.valueOf(transportEncryption));

        if(request.getContentType().contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)){
            Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());
            if(parameterMap.size()>0){
                Map<String, String[]> decryptMap = new HashMap<>();
                for(Map.Entry<String, String[]> entry : parameterMap.entrySet()){
                    if (ObjectUtil.isNotEmpty(entry.getKey())|| ObjectUtil.isNotEmpty(entry.getValue())) {
                        decryptMap.put(encryptor.decrypt(entry.getKey()),new String[]{encryptor.decrypt(entry.getValue()[0])});
                    }
                }
                ParameterRequestWrapper requestWrapper = new ParameterRequestWrapper(request, decryptMap);
                chain.doFilter(requestWrapper, response);
            }
        }else if(request.getContentType().contains(MediaType.APPLICATION_JSON_VALUE)){
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String temp=encryptor.decrypt(stringBuilder.toString());
            BodyRewritingRequestWrapper requestWrapper = new BodyRewritingRequestWrapper(request, temp.getBytes("UTF-8"));
            chain.doFilter(requestWrapper, response);
        }else{
            chain.doFilter(servletRequest, response);
        }

    }


}
