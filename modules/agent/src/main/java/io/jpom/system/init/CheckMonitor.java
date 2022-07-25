package io.jpom.system.init;

import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import io.jpom.common.RemoteVersion;
import io.jpom.cron.CronUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author bwcx_jzy
 * @since 2022/7/25
 */
@PreLoadClass(value = Integer.MAX_VALUE)
@Slf4j
public class CheckMonitor {

    @PreLoadMethod
    public static void init() {
        // 开启检测调度
        CronUtils.upsert("system_monitor", "0 0 0,12 * * ?", () -> {
            try {
                //
                RemoteVersion.loadRemoteInfo();
            } catch (Exception e) {
                log.error("系统调度执行出现错误", e);
            }
        });
    }
}
