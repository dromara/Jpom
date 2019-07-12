package cn.keepbx.jpom.system.init;

import cn.hutool.cron.CronUtil;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.model.data.MonitorModel;
import cn.keepbx.jpom.service.monitor.MonitorService;
import cn.keepbx.util.CronUtils;

import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/7/12
 **/
@PreLoadClass
public class Monitor {

    @PreLoadMethod
    private static void init() {
        CronUtil.schedule(MonitorModel.Cycle.one.getCronPattern().toString(), new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                MonitorService monitorService = SpringUtil.getBean(MonitorService.class);
                List<MonitorModel> monitorModels = monitorService.listByCycle(MonitorModel.Cycle.one);


                if (MonitorModel.Cycle.five.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
                    monitorModels.addAll(monitorService.listByCycle(MonitorModel.Cycle.five));
                }
                if (MonitorModel.Cycle.ten.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
                    monitorModels.addAll(monitorService.listByCycle(MonitorModel.Cycle.ten));
                }
                if (MonitorModel.Cycle.thirty.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
                    monitorModels.addAll(monitorService.listByCycle(MonitorModel.Cycle.thirty));
                }
                //

            }
        });
        CronUtils.start();
    }
}
