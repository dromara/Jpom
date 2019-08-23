package io.jpom.util;

import cn.hutool.cron.CronUtil;
import cn.hutool.cron.Scheduler;

/**
 * @author bwcx_jzy
 * @date 2019/7/12
 **/
public class CronUtils {

    public static void start() {
        // 开启秒级表达式
        CronUtil.setMatchSecond(true);
        //
        Scheduler scheduler = CronUtil.getScheduler();
        if (!scheduler.isStarted()) {
            CronUtil.start();
        }
    }
}
