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

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.util.StringUtil;
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
            log.warn("[timeout] {} {} {}", wrapper.getRequestURI(), reqData, StringUtil.formatBetween(l, BetweenFormatter.Level.MILLISECOND));
        }
    }
}
