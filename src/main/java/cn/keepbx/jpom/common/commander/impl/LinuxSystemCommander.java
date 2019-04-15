package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.commander.AbstractSystemCommander;
import cn.keepbx.jpom.model.system.ProcessModel;
import cn.keepbx.jpom.util.CommandUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/15
 */
public class LinuxSystemCommander extends AbstractSystemCommander {

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
            JSONArray cpu = getLinuxCpu(cpus);
            jsonObject.put("cpu", cpu);
        }
        if (length >= 3) {
            String mem = split[3];
            //内存占比
            JSONArray memory = getLinuxMemory(mem);
            jsonObject.put("memory", memory);
        }
        jsonObject.put("top", true);
        jsonObject.put("disk", getHardDisk());
        return jsonObject;
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
        if (val.endsWith("g")) {
            val = String.format("%.2f MB", Convert.toDouble(val.replace("g", "")) * 1024);
        } else {
            val = Convert.toLong(val) / 1024 + " MB";
        }
        return val;
    }


    /**
     * 获取内存信息
     *
     * @param info 内存信息
     * @return 内存信息
     */
    private static JSONArray getLinuxMemory(String info) {
        if (StrUtil.isEmpty(info)) {
            return null;
        }
        int index = info.indexOf(":") + 1;
        String[] split = info.substring(index).split(",");
        JSONArray memory = new JSONArray();
        for (String str : split) {
            str = str.trim();
//            509248k total — 物理内存总量（509M）
//            495964k used — 使用中的内存总量（495M）
//            13284k free — 空闲内存总量（13M）
//            25364k buffers — 缓存的内存量 （25M）
            if (str.endsWith("free")) {
                memory.add(putObject("空闲内存", str.replace("free", "").trim(), "memory"));
            }
            if (str.endsWith("used")) {
                memory.add(putObject("使用中的内存", str.replace("used", "").trim(), "memory"));
            }
            if (str.endsWith("buff/cache")) {
                memory.add(putObject("缓存的内存", str.replace("buff/cache", "").trim(), "memory"));
            }
        }
        return memory;
    }

    /**
     * 获取cpu信息
     *
     * @param info cpu信息
     * @return cpu信息
     */
    private static JSONArray getLinuxCpu(String info) {
        if (StrUtil.isEmpty(info)) {
            return null;
        }
        int i = info.indexOf(":");
        String[] split = info.substring(i + 1).split(",");
        JSONArray cpu = new JSONArray();
        for (String str : split) {
            str = str.trim();
//            1.3% us — 用户空间占用CPU的百分比。
//            1.0% sy — 内核空间占用CPU的百分比。
//            0.0% ni — 改变过优先级的进程占用CPU的百分比
//            97.3% id — 空闲CPU百分比
//            0.0% wa — IO等待占用CPU的百分比
//            0.3% hi — 硬中断（Hardware IRQ）占用CPU的百分比
//            0.0% si — 软中断（Software Interrupts）占用CPU的百分比
            String value = str.substring(0, str.length() - 2).trim();
            String tag = str.substring(str.length() - 2);
            switch (tag) {
                case "us":
                    cpu.add(putObject("用户空间", value, "cpu"));
                    break;
                case "sy":
                    cpu.add(putObject("内核空间", value, "cpu"));
                    break;
                case "ni":
                    cpu.add(putObject("改变过优先级的进程", value, "cpu"));
                    break;
                case "id":
                    cpu.add(putObject("空闲CPU", value, "cpu"));
                    break;
                case "wa":
                    cpu.add(putObject("IO等待", value, "cpu"));
                    break;
                case "hi":
                    cpu.add(putObject("硬中断", value, "cpu"));
                    break;
                case "si":
                    cpu.add(putObject("软中断", value, "cpu"));
                    break;
                default:
                    break;
            }
        }
        return cpu;
    }
}
