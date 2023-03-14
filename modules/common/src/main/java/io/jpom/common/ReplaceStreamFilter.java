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
package io.jpom.common;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * <a href="https://springboot.io/t/topic/3637">https://springboot.io/t/topic/3637</a>
 *
 * @author bwcx_jzy
 * @since 2022/12/8
 */
@Configuration
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReplaceStreamFilter implements Filter {


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        long startTime = System.currentTimeMillis();
        ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper((HttpServletRequest) request);
        chain.doFilter(wrapper, response);
        long endTime = System.currentTimeMillis();
        long l = endTime - startTime;
        if (l > 1000 * 5) {
            byte[] contentAsByteArray = wrapper.getContentAsByteArray();
            String str = StrUtil.str(contentAsByteArray, CharsetUtil.CHARSET_UTF_8);
            String reqData = Opt.ofBlankAble(str)
                .map(s -> wrapper.getParameterMap())
                .map(JSONObject::toJSONString)
                .orElse(StrUtil.EMPTY);
            log.warn("[timeout] {} {} {}", wrapper.getRequestURI(), reqData, DateUtil.formatBetween(l));
        }
    }
}
