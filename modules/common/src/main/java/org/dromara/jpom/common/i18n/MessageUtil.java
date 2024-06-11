package org.dromara.jpom.common.i18n;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
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
public class MessageUtil {

    /**
     * 根据key信息获取对应语言的内容
     *
     * @param key 消息key值
     * @return msg
     */
    public static String get(String key) {
        String language = "zh_CN";
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes != null) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            language = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
            language = StrUtil.emptyToDefault(language, "zh_CN");
        }
        Locale locale;
        switch (language) {
            case "zh_CN":
                locale = Locale.CHINA;
                break;
            case "en_US":
                locale = Locale.US;
                break;
            default:
                locale = Locale.CHINA;
                log.warn("未知的语言方式：{}", language);
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
