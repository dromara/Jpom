package cn.keepbx.jpom.common.commander;

import cn.keepbx.jpom.common.commander.impl.LinuxCommander;
import cn.keepbx.jpom.common.commander.impl.WindowsCommander;
import cn.keepbx.jpom.model.ProjectInfoModel;

import java.io.IOException;

public abstract class Commander {

    /**
     * 实例化Commander
     * @return
     */
    public static Commander getInstance() {
        Commander commander = null;

        String os = System.getProperty("os.name");

        if ("linux".startsWith(os)) {
            // Linux系统
            commander = new LinuxCommander();
        }

        if ("windows".startsWith(os)) {
            // Winddows系统
            commander = new WindowsCommander();
        }
        return commander;
    }

    /**
     * 启动
     * @return
     */
    public abstract String start(ProjectInfoModel projectInfoModel) throws Exception;

    /**
     * 停止
     * @return
     */
    public abstract String stop();

    /**
     * 重启
     * @return
     */
    public abstract String restart();

    /**
     * 查看状态
     * @return
     */
    public abstract String status();

}
