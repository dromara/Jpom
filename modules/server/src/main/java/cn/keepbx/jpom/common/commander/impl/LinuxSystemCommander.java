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
            return String.format("%.2f MB", Convert.toDouble(val.replace("g", "")) * 1024);
        } else {
            return Convert.toLong(val) / 1024 + " MB";
        }
    }



}
