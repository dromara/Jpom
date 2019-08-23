package io.jpom.common.commander.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.model.system.ProcessModel;
import io.jpom.util.CommandUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class LinuxSystemCommander extends AbstractSystemCommander {

    @Override
    public JSONObject getAllMonitor() {
        String result = CommandUtil.execCommand("top -b -n 1");
        if (StrUtil.isEmpty(result)) {
            return null;
        }
        String[] split = result.split(StrUtil.LF);
        int length = split.length;
        JSONObject jsonObject = new JSONObject();
        if (length >= 2) {
            String cpus = split[2];
            //cpu占比
            String cpu = getLinuxCpu(cpus);
            jsonObject.put("cpu", cpu);
        }
        if (length >= 3) {
            String mem = split[3];
            //内存占比
            String memory = getLinuxMemory(mem);
            jsonObject.put("memory", memory);
        }
        jsonObject.put("top", true);
        jsonObject.put("disk", getHardDisk());
        return jsonObject;
    }

    @Override
    public List<ProcessModel> getProcessList() {
        String s = CommandUtil.execSystemCommand("top -b -n 1 | grep java");
        return formatLinuxTop(s, false);
    }


    @Override
    public ProcessModel getPidInfo(int pid) {
        String command = "top -b -n 1 -p " + pid;
        String internal = CommandUtil.execCommand(command);
        List<ProcessModel> processModels = formatLinuxTop(internal, true);
        if (processModels == null || processModels.isEmpty()) {
            return null;
        }
        return processModels.get(0);
    }


    /**
     * 将linux的top信息转为集合
     *
     * @param top top
     */
    private static List<ProcessModel> formatLinuxTop(String top, boolean header) {
        List<String> list = StrSpliter.splitTrim(top, StrUtil.LF, true);
        if (list.size() <= 0) {
            return null;
        }
        List<ProcessModel> list1 = new ArrayList<>();
        ProcessModel processModel;
        for (int i = header ? 6 : 0, len = list.size(); i < len; i++) {
            processModel = new ProcessModel();
            String item = list.get(i);
            List<String> values = StrSpliter.splitTrim(item, StrUtil.SPACE, true);
            processModel.setPid(Integer.parseInt(values.get(0)));
            processModel.setUser(values.get(1));
            processModel.setPr(values.get(2));
            processModel.setNi(values.get(3));
            //
            processModel.setVirt(formSize(values.get(4)));
            processModel.setRes(formSize(values.get(5)));
            processModel.setShr(formSize(values.get(6)));
            //
            processModel.setStatus(formStatus(values.get(7)));
            //
            processModel.setCpu(values.get(8) + "%");
            processModel.setMem(values.get(9) + "%");
            //
            processModel.setTime(values.get(10));
            processModel.setCommand(values.get(11));
            list1.add(processModel);
        }
        return list1;
    }


    private static String formStatus(String val) {
        String value = "未知";
        if ("S".equalsIgnoreCase(val)) {
            value = "睡眠";
        } else if ("R".equalsIgnoreCase(val)) {
            value = "运行";
        } else if ("T".equalsIgnoreCase(val)) {
            value = "跟踪/停止";
        } else if ("Z".equalsIgnoreCase(val)) {
            value = "僵尸进程 ";
        } else if ("D".equalsIgnoreCase(val)) {
            value = "不可中断的睡眠状态 ";
        } else if ("i".equalsIgnoreCase(val)) {
            value = "多线程 ";
        }
        return value;
    }

    private static String formSize(String val) {
        if (StrUtil.endWithIgnoreCase(val, "g")) {
            String newVal = val.substring(0, val.length() - 1);
            return String.format("%.2f MB", Convert.toDouble(newVal, 0D) * 1024);
        }
        if (StrUtil.endWithIgnoreCase(val, "m")) {
            String newVal = val.substring(0, val.length() - 1);
            return Convert.toLong(newVal, 0L) / 1024 + " MB";
        }
        return Convert.toLong(val, 0L) / 1024 + " MB";
    }

    /**
     * 获取内存信息
     *
     * @param info 内存信息
     * @return 内存信息
     */
    private static String getLinuxMemory(String info) {
        if (StrUtil.isEmpty(info)) {
            return null;
        }
        int index = info.indexOf(":") + 1;
        String[] split = info.substring(index).split(",");
//            509248k total — 物理内存总量（509M）
//            495964k used — 使用中的内存总量（495M）
//            13284k free — 空闲内存总量（13M）
//            25364k buffers — 缓存的内存量 （25M）
        double total = 0, used = 0;
        for (String str : split) {
            str = str.trim();
            if (str.endsWith("used")) {
                String value = str.replace("used", "").replace("k", "").trim();
                used = Convert.toDouble(value, 0.0);
            }
            if (str.endsWith("total")) {
                String value = str.replace("total", "").replace("k", "").trim();
                total = Convert.toDouble(value, 0.0);
            }
        }
        return String.format("%.2f", used / total * 100);
    }

    /**
     * 获取占用cpu信息
     *
     * @param info cpu信息
     * @return cpu信息
     */
    private static String getLinuxCpu(String info) {
        if (StrUtil.isEmpty(info)) {
            return null;
        }
        int i = info.indexOf(":");
        String[] split = info.substring(i + 1).split(",");
//            1.3% us — 用户空间占用CPU的百分比。
//            1.0% sy — 内核空间占用CPU的百分比。
//            0.0% ni — 改变过优先级的进程占用CPU的百分比
//            97.3% id — 空闲CPU百分比
//            0.0% wa — IO等待占用CPU的百分比
//            0.3% hi — 硬中断（Hardware IRQ）占用CPU的百分比
//            0.0% si — 软中断（Software Interrupts）占用CPU的百分比
        for (String str : split) {
            str = str.trim();
            String value = str.substring(0, str.length() - 2).trim();
            String tag = str.substring(str.length() - 2);
            if ("id".equalsIgnoreCase(tag)) {
                value = value.replace("%", "");
                double val = Convert.toDouble(value, 0.0);
                return String.format("%.2f", 100.00 - val);
            }
        }
        return "0";
    }

    @Override
    public boolean getServiceStatus(String serviceName) {
        String format = StrUtil.format("service {} status", serviceName);
        String result = CommandUtil.execCommand(format);
        return StrUtil.containsIgnoreCase(result, "RUNNING");
    }

    @Override
    public String startService(String serviceName) {
        String format = StrUtil.format("service {} start", serviceName);
        return CommandUtil.execSystemCommand(format);
    }

    @Override
    public String stopService(String serviceName) {
        String format = StrUtil.format("service {} stop", serviceName);
        return CommandUtil.execSystemCommand(format);
    }
}
