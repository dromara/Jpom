package io.jpom.common.commander.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.Scheduler;
import cn.hutool.cron.task.Task;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import com.sun.management.OperatingSystemMXBean;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.model.system.ProcessModel;
import io.jpom.util.CommandUtil;
import io.jpom.util.JvmUtil;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * windows 系统查询命令
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class WindowsSystemCommander extends AbstractSystemCommander {

    private static String lastResult;

    private static final String ID = "windows_system_process_list";

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
        double total = operatingSystemMXBean.getTotalPhysicalMemorySize();
        double free = operatingSystemMXBean.getFreePhysicalMemorySize();
        jsonObject.put("memory", String.format("%.2f", (total - free) / total * 100));
        //最近系统cpu使用量
        double systemCpuLoad = operatingSystemMXBean.getSystemCpuLoad();
        if (systemCpuLoad <= 0) {
            systemCpuLoad = 0;
        }
        jsonObject.put("cpu", String.format("%.2f", systemCpuLoad * 100));
        jsonObject.put("disk", getHardDisk());
        return jsonObject;
    }

    @Override
    public List<ProcessModel> getProcessList() {
        Scheduler scheduler = CronUtil.getScheduler();
        Task task = scheduler.getTask(ID);
        if (task == null) {
            CronUtil.schedule(ID, "0 0/1 * * * ?", () -> {
                try {
                    lastResult = CommandUtil.execSystemCommand("tasklist /V | findstr java");
                } catch (Exception e) {
                    DefaultSystemLog.getLog().error("执行控制台进程统计错误", e);
                }
            });
        }
        if (lastResult == null) {
            // 返回上一次结果
            lastResult = CommandUtil.execSystemCommand("tasklist /V | findstr java");
        }
        return formatWindowsProcess(lastResult, false);
    }

    @Override
    public ProcessModel getPidInfo(int pid) {
        String command = "tasklist /V /FI \"pid eq " + pid + "\"";
        String result = CommandUtil.execSystemCommand(command);
        List<ProcessModel> array = formatWindowsProcess(result, true);
        if (array == null || array.isEmpty()) {
            return null;
        }
        return array.get(0);
    }

    @Override
    public String emptyLogFile(File file) {
        return CommandUtil.execSystemCommand("echo  \"\" > " + file.getAbsolutePath());
    }

    /**
     * 将windows的tasklist转为集合
     *
     * @param header 是否包含投信息
     * @param result 进程信息
     * @return jsonArray
     */
    private static List<ProcessModel> formatWindowsProcess(String result, boolean header) {
        List<String> list = StrSplitter.splitTrim(result, StrUtil.LF, true);
        if (list.isEmpty()) {
            return null;
        }
        List<ProcessModel> processModels = new ArrayList<>();
        for (int i = header ? 2 : 0, len = list.size(); i < len; i++) {
            String param = list.get(i);
            List<String> memList = StrSplitter.splitTrim(param, StrUtil.SPACE, true);
            ProcessModel processModel = new ProcessModel();
            int pid = Convert.toInt(memList.get(1), 0);
            processModel.setPid(pid);
            //
            String name = memList.get(0);
            processModel.setCommand(name);
            //使用内存 kb
            String mem = memList.get(4).replace(",", "");
            long aLong = Convert.toLong(mem, 0L);
//            FileUtil.readableFileSize()
            processModel.setRes(aLong / 1024 + " MB");
            String status = memList.get(6);
            processModel.setStatus(formatStatus(status));
            //
            processModel.setUser(memList.get(7));
            processModel.setTime(memList.get(8));

            try {
                JvmUtil.getOperatingSystemMXBean(memList.get(1), operatingSystemMXBean -> {
                    if (operatingSystemMXBean != null) {
                        //最近jvm cpu使用率
                        double processCpuLoad = operatingSystemMXBean.getProcessCpuLoad() * 100;
                        if (processCpuLoad <= 0) {
                            processCpuLoad = 0;
                        }
                        processModel.setCpu(String.format("%.2f", processCpuLoad) + "%");
                        //服务器总内存
                        long totalMemorySize = operatingSystemMXBean.getTotalPhysicalMemorySize();
                        BigDecimal total = new BigDecimal(totalMemorySize / 1024);
                        // 进程
                        double v = new BigDecimal(aLong).divide(total, 4, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
                        processModel.setMem(String.format("%.2f", v) + "%");
                    }
                });

            } catch (Exception ignored) {

            }
            processModels.add(processModel);
        }
        return processModels;
    }

    private static String formatStatus(String status) {
        if ("RUNNING".equalsIgnoreCase(status)) {
            return "运行";
        }
        if ("SUSPENDED".equalsIgnoreCase(status)) {
            return "睡眠";
        }
        if ("NOT RESPONDING".equalsIgnoreCase(status)) {
            return "无响应";
        }
        if ("Unknown".equalsIgnoreCase(status)) {
            return "未知";
        }
        return status;
    }

    @Override
    public boolean getServiceStatus(String serviceName) {
        String result = CommandUtil.execSystemCommand("sc query " + serviceName);
        return StrUtil.containsIgnoreCase(result, "RUNNING");
    }

    @Override
    public String startService(String serviceName) {
        String format = StrUtil.format("net start {}", serviceName);
        return CommandUtil.execSystemCommand(format);
    }

    @Override
    public String stopService(String serviceName) {
        String format = StrUtil.format("net stop {}", serviceName);
        return CommandUtil.execSystemCommand(format);
    }

    @Override
    public String buildKill(int pid) {
        return String.format("taskkill /F /PID %s", pid);
    }
}
