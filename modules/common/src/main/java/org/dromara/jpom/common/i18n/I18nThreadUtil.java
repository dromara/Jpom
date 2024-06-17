package org.dromara.jpom.common.i18n;

import cn.hutool.core.thread.ThreadUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

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

    /**
     * 执行有返回值的异步方法<br>
     * Future代表一个异步执行的操作，通过get()方法可以获得操作的结果，如果异步操作还没有完成，则，get()会使当前线程阻塞
     *
     * @param <T>  回调对象类型
     * @param task {@link Callable}
     * @return Future
     */
    public static <T> Future<T> execAsync(Callable<T> task) {
        String language = I18nMessageUtil.tryGetLanguage();
        return ThreadUtil.execAsync(() -> {
            try {
                I18nMessageUtil.setLanguage(language);
                return task.call();
            } finally {
                I18nMessageUtil.clearLanguage();
            }
        });
    }

    /**
     * 执行有返回值的异步方法<br>
     * Future代表一个异步执行的操作，通过get()方法可以获得操作的结果，如果异步操作还没有完成，则，get()会使当前线程阻塞
     *
     * @param runnable 可运行对象
     * @return {@link Future}
     * @since 3.0.5
     */
    public static Future<?> execAsync(Runnable runnable) {
        String language = I18nMessageUtil.tryGetLanguage();
        return ThreadUtil.execAsync(() -> {
            try {
                I18nMessageUtil.setLanguage(language);
                runnable.run();
            } finally {
                I18nMessageUtil.clearLanguage();
            }
        });
    }

}
