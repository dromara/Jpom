package org.dromara.jpom.common.i18n;

import cn.hutool.core.thread.ThreadUtil;

/**
 * @author bwcx_jzy1
 * @since 2024/6/15
 */
public class I18nThreadUtil {

    /**
     * 线程执行（获取父级线程语言）
     *
     * @param runnable runnable
     */
    public static void execute(Runnable runnable) {
        String language = I18nMessageUtil.tryGetLanguage();
        ThreadUtil.execute(() -> {
            try {
                I18nMessageUtil.setLanguage(language);
                runnable.run();
            } finally {
                I18nMessageUtil.clearLanguage();
            }
        });
    }
}
