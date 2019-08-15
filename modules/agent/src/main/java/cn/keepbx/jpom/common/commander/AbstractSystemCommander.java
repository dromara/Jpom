package cn.keepbx.jpom.common.commander;

import cn.hutool.system.SystemUtil;
import cn.keepbx.jpom.common.commander.impl.LinuxSystemCommander;
import cn.keepbx.jpom.common.commander.impl.WindowsSystemCommander;
import cn.keepbx.jpom.model.system.ProcessModel;
import cn.keepbx.jpom.system.JpomRuntimeException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.util.List;

/**
 * 系统监控命令
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public abstract class AbstractSystemCommander {

    private static AbstractSystemCommander abstractSystemCommander = null;

    public static AbstractSystemCommander getInstance() {
        if (abstractSystemCommander != null) {
            return abstractSystemCommander;
        }
        if (SystemUtil.getOsInfo().isLinux()) {
            // Linux系统
            abstractSystemCommander = new LinuxSystemCommander();
        } else if (SystemUtil.getOsInfo().isWindows()) {
            // Windows系统
            abstractSystemCommander = new WindowsSystemCommander();
        } else if (SystemUtil.getOsInfo().isMac()) {
            abstractSystemCommander = new LinuxSystemCommander();
        } else {
            throw new JpomRuntimeException("不支持的：" + SystemUtil.getOsInfo().getName());
        }
        return abstractSystemCommander;
    }

    /**
     * 获取整个服务器监控信息
     *
     * @return data
     */
    public abstract JSONObject getAllMonitor();

    /**
     * 获取当前服务器的所有进程列表
     *
     * @return array
     */
    public abstract List<ProcessModel> getProcessList();

    /**
     * 获取指定进程的 内存信息
     *
     * @param pid 进程id
     * @return json
     */
    public abstract ProcessModel getPidInfo(int pid);

    /**
     * 磁盘占用
     *
     * @return 磁盘占用
     */
    protected static String getHardDisk() {
        File[] files = File.listRoots();
        double totalSpace = 0;
        double useAbleSpace = 0;
        for (File file : files) {
            double total = file.getTotalSpace();
            totalSpace += total;
            useAbleSpace += total - file.getUsableSpace();
        }
        return String.format("%.2f", useAbleSpace / totalSpace * 100);
    }

    /**
     * 查询服务状态
     *
     * @param serviceName 服务名称
     * @return true 运行中
     */
    public abstract boolean getServiceStatus(String serviceName);

    /**
     * 启动服务
     *
     * @param serviceName 服务名称
     * @return 结果
     */
    public abstract String startService(String serviceName);

    /**
     * 关闭服务
     *
     * @param serviceName 服务名称
     * @return 结果
     */
    public abstract String stopService(String serviceName);
}
