package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.commander.AbstractSystemCommander;
import cn.keepbx.jpom.model.system.ProcessModel;
import cn.keepbx.jpom.util.CommandUtil;
import cn.keepbx.jpom.util.JvmUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.management.OperatingSystemMXBean;
import com.sun.tools.attach.VirtualMachine;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jiangzeyin
 * @date 2019/4/15
 */
public class WindowsSystemCommander extends AbstractSystemCommander {

    /**
     * 锁定查看进程信息
     */
    private static final AtomicBoolean ATOMIC_BOOLEAN = new AtomicBoolean(false);

    @Override
    public List<ProcessModel> getProcessList() {
        if (ATOMIC_BOOLEAN.get()) {
            return null;
        }
        try {
            ATOMIC_BOOLEAN.set(true);
            String s = CommandUtil.execSystemCommand("tasklist /V | findstr java");
            return formatWindowsProcess(s, false);
        } finally {
            ATOMIC_BOOLEAN.set(false);
        }
    }

    @Override
    public ProcessModel getPidInfo(int pid) {
        String command = "tasklist /V /FI \"pid eq " + pid + "\"";
        String result = CommandUtil.execCommand(command);
        List<ProcessModel> array = formatWindowsProcess(result, true);
        if (array == null || array.isEmpty()) {
            return null;
        }
        return array.get(0);
    }


    /**
     * 将windows的tasklist转为集合
     *
     * @param header 是否包含投信息
     * @param result 进程信息
     * @return jsonArray
     */
    private static List<ProcessModel> formatWindowsProcess(String result, boolean header) {
        List<String> list = StrSpliter.splitTrim(result, StrUtil.LF, true);
        if (list.isEmpty()) {
            return null;
        }
        List<ProcessModel> processModels = new ArrayList<>();
        ProcessModel processModel;
        for (int i = header ? 2 : 0, len = list.size(); i < len; i++) {
            String param = list.get(i);
            List<String> memList = StrSpliter.splitTrim(param, StrUtil.SPACE, true);
            processModel = new ProcessModel();
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
                VirtualMachine virtualMachine = VirtualMachine.attach(memList.get(1));
                OperatingSystemMXBean operatingSystemMXBean = JvmUtil.getOperatingSystemMXBean(virtualMachine);
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
        return "未知";
    }

}
