package io.jpom.system.init;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.node.NodeService;

/**
 * @author bwcx_jzy
 * @date 2019/7/14
 */
@PreLoadClass
public class CheckMonitor {

    @PreLoadMethod
    private static void init() {
        MonitorService monitorService = SpringUtil.getBean(MonitorService.class);
        boolean status = monitorService.checkCronStatus();
        if (status) {
            DefaultSystemLog.LOG().info("已经开启监听调度：监控");
        }
        //
        NodeService nodeService = SpringUtil.getBean(NodeService.class);
        status = nodeService.checkCronStatus();
        if (status) {
            DefaultSystemLog.LOG().info("已经开启监听调度：节点信息采集");
        }
    }
}
