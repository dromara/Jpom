package cn.keepbx.jpom.common.commander;

import cn.hutool.http.HttpRequest;
import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.common.commander.impl.LinuxTomcatCommander;
import cn.keepbx.jpom.common.commander.impl.WindowsTomcatCommander;
import cn.keepbx.jpom.model.data.TomcatInfoModel;
import cn.keepbx.jpom.system.JpomRuntimeException;

/**
 * tomcat命令执行工具类
 * @author LF
 */
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

    /**
     * 执行tomcat命令
     * @param tomcatInfoModel tomcat信息
     * @param cmd 执行的命令，包括start stop
     * @return 返回tomcat启动结果
     */
    public abstract String execCmd(TomcatInfoModel tomcatInfoModel, String cmd);

    /**
     * 检查tomcat状态
     * @param tomcatInfoModel tomcat信息
     * @param cmd 操作命令
     * @return 状态结果
     */
    protected String getStatus(TomcatInfoModel tomcatInfoModel, String cmd) {
        String strReturn = "start".equals(cmd) ? "stopped" : "started";
        int i = 0;
        while (i < 10) {
            int result = 0;
            String url = String.format("http://127.0.0.1:%d/", tomcatInfoModel.getPort());
            HttpRequest httpRequest = new HttpRequest(url);
            httpRequest.setConnectionTimeout(3000); // 设置超时时间为3秒
            try {
                httpRequest.execute();
                result = 1;
            } catch (Exception ignored) {}

            i++;
            if ("start".equals(cmd) && result == 1) {
                strReturn = "started";
                break;
            }
            if ("stop".equals(cmd) && result == 0) {
                strReturn = "stopped";
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }
        return strReturn;
    }
}
