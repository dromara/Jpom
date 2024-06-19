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
import cn.hutool.system.SystemUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * 国际化转换工具类
 *
 * @author bwcx_jzy
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class I18nMessageUtil {
    /**
     * 线程中的语言
     */
    private static final ThreadLocal<String> LANGUAGE = new ThreadLocal<>();
    /**
     * 语言获取方式
     */
    private static final List<Supplier<String>> LANGUAGE_OBTAIN = new ArrayList<>();

    static {
        // 线程变量获取
        LANGUAGE_OBTAIN.add(LANGUAGE::get);
        // http 请求获取
        LANGUAGE_OBTAIN.add(I18nMessageUtil::getLanguageByRequest);
        // Jpom 配置获取
        LANGUAGE_OBTAIN.add(() -> SystemUtil.get("JPOM_LANG"));
        // 系统语言
        LANGUAGE_OBTAIN.add(() -> {
            Locale locale = Locale.getDefault();
            String country = locale.getCountry();
            if (StrUtil.equals("zh", country)) {
                // 中国
                return "zh-CN";
            }
            return "en-US";
        });
    }

    /**
     * 尝试获取语言(系统语言)
     *
     * @return 语言
     */
    public static String tryGetSystemLanguage() {
        String language = null;
        for (int i = 2; i < LANGUAGE_OBTAIN.size(); i++) {
            language = LANGUAGE_OBTAIN.get(i).get();
            if (language != null) {
                break;
            }
        }
        return language;
    }

    /**
     * 设置语言
     *
     * @param language 语言
     */
    public static void setLanguage(String language) {
        LANGUAGE.set(language);
    }

    /**
     * 清除语言
     */
    public static void clearLanguage() {
        LANGUAGE.remove();
    }

    /**
     * 获取语言 通过 http 请求
     *
     * @return 语言
     */
    public static String getLanguageByRequest() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes != null) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            return ServletUtil.getHeader(request, Header.ACCEPT_LANGUAGE.getValue(), CharsetUtil.CHARSET_UTF_8);
        }
        return null;
    }

    /**
     * 语言格式化
     *
     * @param language 语言
     * @return 语言
     */
    private static String normalLanguage(String language) {
        language = language != null ? language.toLowerCase() : StrUtil.EMPTY;
        language = StrUtil.replace(language, "_", "-");
        switch (language) {
            case "en-us":
                return "en-US";
            case "zh-tw":
                return "zh-TW";
            case "zh-hk":
                return "zh-HK";
            case "zh-cn":
            default:
                return "zh-CN";
        }
    }

    /**
     * 尝试获取语言
     *
     * @return 语言
     */
    public static String tryGetNormalLanguage() {
        return normalLanguage(tryGetLanguage());
    }

    /**
     * 尝试获取语言
     *
     * @return 语言
     */
    public static String tryGetLanguage() {
        String language = null;
        for (Supplier<String> supplier : LANGUAGE_OBTAIN) {
            language = supplier.get();
            if (language != null) {
                break;
            }
        }
        return language;
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
        String language = tryGetLanguage();
        language = normalLanguage(language);
        Locale locale;
        switch (language) {
            case "zh-CN":
                locale = Locale.CHINA;
                break;
            case "zh-TW":
                locale = Locale.TAIWAN;
                break;
            case "zh-HK":
                locale = getZhHkInstance();
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

    private static Locale getZhHkInstance() {
        return LazyZhHk.LOCALE;
    }

    /**
     * 使用懒加载方式实例化messageSource国际化工具
     */
    private static class Lazy {
        private static final MessageSource MESSAGE_SOURCE = SpringUtil.getBean(MessageSource.class);
    }

    private static class LazyZhHk {
        private static final Locale LOCALE = new Locale("zh", "HK");
    }

}
