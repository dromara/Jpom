/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.i18n;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.Header;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * 国际化转换工具类
 *
 * @author bwcx_jzy
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class I18nMessageUtil {

    /**
     * 获取语言
     *
     * @param request 请求
     * @return 合法的语言
     */
    public static String parseLanguage(HttpServletRequest request) {
        String language = ServletUtil.getHeader(request, Header.ACCEPT_LANGUAGE.getValue(), CharsetUtil.CHARSET_UTF_8);
        language = StrUtil.emptyToDefault(language, "zh-cn");
        language = language.toLowerCase();
        switch (language) {
            case "en-us":
            case "en_us":
                return "en-US";
            case "zh_cn":
            case "zh-cn":
            default:
                return "zh-CN";
        }
    }

    /**
     * 根据key信息获取对应语言的内容
     *
     * @param key 消息key值
     * @return msg
     */
    public static String get(String key) {
        if (StrUtil.isEmpty(key)) {
            return StrUtil.EMPTY;
        }
        String language = "zh-CN";
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes != null) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            language = parseLanguage(request);
        }
        Locale locale;
        switch (language) {
            case "zh-CN":
                locale = Locale.CHINA;
                break;
            case "en-US":
                locale = Locale.US;
                break;
            default:
                locale = Locale.CHINA;
                log.warn("Unknown language:{}", language);
                break;
        }
        return get(key, locale);
    }

    private static String get(String key, Locale language) {
        return get(key, new String[0], language);
    }

    private static String get(String key, Object[] params, Locale language) {
        return getInstance().getMessage(key, params, language);
    }

    private static MessageSource getInstance() {
        return Lazy.MESSAGE_SOURCE;
    }

    /**
     * 使用懒加载方式实例化messageSource国际化工具
     */
    private static class Lazy {
        private static final MessageSource MESSAGE_SOURCE = SpringUtil.getBean(MessageSource.class);
    }

}
