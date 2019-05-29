package cn.keepbx.jpom.common.commander;

import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.common.commander.impl.LinuxTomcatCommander;
import cn.keepbx.jpom.common.commander.impl.WindowsTomcatCommander;
import cn.keepbx.jpom.model.data.TomcatInfoModel;
import cn.keepbx.jpom.system.JpomRuntimeException;

public abstract class AbstractTomcatCommander {

    private static AbstractTomcatCommander abstractTomcatCommander;

    public static AbstractTomcatCommander getInstance() {
        if (abstractTomcatCommander != null) {
            return abstractTomcatCommander;
        }
        if (BaseJpomApplication.OS_INFO.isLinux()) {
            // Linux系统
            abstractTomcatCommander = new LinuxTomcatCommander();
        } else if (BaseJpomApplication.OS_INFO.isWindows()) {
            // Windows系统
            abstractTomcatCommander = new WindowsTomcatCommander();
        } else if (BaseJpomApplication.OS_INFO.isMac()) {
            abstractTomcatCommander = new LinuxTomcatCommander();
        } else {
            throw new JpomRuntimeException("不支持的：" + BaseJpomApplication.OS_INFO.getName());
        }
        return abstractTomcatCommander;
    }

    public abstract String execCmd(TomcatInfoModel tomcatInfoModel, String cmd);

    public abstract String status();
}
