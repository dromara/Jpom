package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.commander.AbstractSystemCommander;
import cn.keepbx.jpom.util.CommandUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * Created by jiangzeyin on 2019/4/16.
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
