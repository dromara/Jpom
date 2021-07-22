package io.jpom.system.init;

import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.JpomApplication;
import io.jpom.common.JpomManifest;
import io.jpom.util.JvmUtil;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;

import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/9/5
 */
class CheckDuplicateRun {

    static void check() {
        try {
            Class<?> appClass = JpomApplication.getAppClass();
            String pid = String.valueOf(JpomManifest.getInstance().getPid());
            List<MonitoredVm> monitoredVms = JvmUtil.listMainClass(appClass.getName());
            monitoredVms.forEach(monitoredVm -> {
                VmIdentifier vmIdentifier = monitoredVm.getVmIdentifier();
                if (pid.equals(vmIdentifier.getUserInfo())) {
                    return;
                }
                DefaultSystemLog.getLog().warn("Jpom 程序建议一个机器上只运行一个对应的程序：" + JpomApplication.getAppType() + "  pid:" + vmIdentifier.getUserInfo());
            });
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("检查异常", e);
        }
    }
}

