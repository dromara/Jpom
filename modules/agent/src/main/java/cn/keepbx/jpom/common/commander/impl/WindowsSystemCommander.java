package cn.keepbx.jpom.common.commander.impl;

import cn.keepbx.jpom.common.commander.AbstractSystemCommander;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

/**
 * Created by jiangzeyin on 2019/4/16.
 */
public class WindowsSystemCommander extends AbstractSystemCommander {
    /**
     * 获取windows 监控
     * https://docs.oracle.com/javase/7/docs/jre/api/management/extension/com/sun/management/OperatingSystemMXBean.html
     *
     * @return 返回cpu占比和内存占比
     */
    @Override
    public JSONObject getAllMonitor() {
        OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("top", true);
        JSONArray memory = new JSONArray();
        {
            long totalPhysicalMemorySize = operatingSystemMXBean.getTotalPhysicalMemorySize();
            long freePhysicalMemorySize = operatingSystemMXBean.getFreePhysicalMemorySize();
            //单位 kb
            memory.add(putObject("占用内存", (totalPhysicalMemorySize - freePhysicalMemorySize) / 1024f, "memory"));
            memory.add(putObject("空闲内存", freePhysicalMemorySize / 1024f, "memory"));
        }
        JSONArray cpus = new JSONArray();
        {
            //最近系统cpu使用量
            double systemCpuLoad = operatingSystemMXBean.getSystemCpuLoad();
            if (systemCpuLoad <= 0) {
                systemCpuLoad = 0;
            }
            cpus.add(putObject("占用cpu", systemCpuLoad, "cpu"));
            cpus.add(putObject("空闲cpu", 1 - systemCpuLoad, "cpu"));
        }
        jsonObject.put("memory", memory);
        jsonObject.put("cpu", cpus);
        jsonObject.put("disk", getHardDisk());
        return jsonObject;
    }

}
