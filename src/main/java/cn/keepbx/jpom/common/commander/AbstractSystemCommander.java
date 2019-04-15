package cn.keepbx.jpom.common.commander;

import cn.keepbx.jpom.common.commander.impl.LinuxSystemCommander;
import cn.keepbx.jpom.common.commander.impl.WindowsSystemCommander;
import cn.keepbx.jpom.model.system.ProcessModel;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.util.List;

/**
 * 系统监控基类
 *
 * @author jiangzeyin
 * @date 2019/4/15
 */
public abstract class AbstractSystemCommander {
    private static AbstractSystemCommander abstractSystemCommander = null;

    public static AbstractSystemCommander getInstance() {
        if (abstractSystemCommander != null) {
            return abstractSystemCommander;
        }
        if (AbstractProjectCommander.OS_INFO.isLinux()) {
            // Linux系统
            abstractSystemCommander = new LinuxSystemCommander();
        } else if (AbstractProjectCommander.OS_INFO.isWindows()) {
            // Windows系统
            abstractSystemCommander = new WindowsSystemCommander();
        } else {
            throw new RuntimeException("不支持的：" + AbstractProjectCommander.OS_INFO.getName());
        }
        return abstractSystemCommander;
    }

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
     * 获取整个服务器监控信息
     *
     * @return data
     */
    public abstract JSONObject getAllMonitor();

    /**
     * 磁盘占用
     *
     * @return 磁盘占用
     */
    protected static JSONArray getHardDisk() {
        File[] files = File.listRoots();
        long freeSpace = 0;
        long useAbleSpace = 0;
        for (File file : files) {
            long free = file.getFreeSpace();
            freeSpace += free;
            useAbleSpace += file.getTotalSpace() - free;
        }
        JSONArray array = new JSONArray();
        //单位 kb
        array.add(putObject("已使用磁盘", useAbleSpace / 1024f, "disk"));
        array.add(putObject("空闲磁盘", freeSpace / 1024f, "disk"));
        return array;
    }

    protected static JSONObject putObject(String name, Object value, String type) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("value", value);
        jsonObject.put("type", type);
        return jsonObject;
    }
}
