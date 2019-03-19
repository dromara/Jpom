package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.system.RuntimeInfo;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.commander.AbstractCommander;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.CommandService;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.system.ConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;

/**
 * 内存查看
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/manage/")
public class InternalController extends BaseController {
    @Resource
    private ProjectInfoService projectInfoService;
    @Resource
    private CommandService commandService;

    /**
     * 获取内存信息
     */
    @RequestMapping(value = "internal", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getInternal(String tag) throws Exception {
        JSONObject object = new JSONObject();
        //获取内存监控信息
        getJvmMem(tag, object);
        object.put("tag", tag);
        setAttribute("internal", object);
        return "manage/internal";
    }

    /**
     * 获取内存监控信息
     *
     * @param tag 项目id
     */
    private void getJvmMem(String tag, JSONObject object) throws Exception {
        String pid = AbstractCommander.getInstance().getPid(tag);
        if (AbstractCommander.OS_INFO.isLinux()) {
            String command = "top -b -n 1 -p " + pid;
            String internal = AbstractCommander.getInstance().execCommand(command);
            String[] split = internal.split("\n");
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                String s = split[i].replaceAll(" ", "&nbsp;&nbsp;");
                result.append(s).append("<br/>");
            }
            JSONObject jsonObject = formatTop(internal);
            setAttribute("item", jsonObject);
            object.put("ram", result.toString());
        } else {
            String command = "tasklist /V /FI \"pid eq " + pid + "\"";
            String result = AbstractCommander.getInstance().execCommand(command);
            List<String> list = StrSpliter.splitTrim(result, "\n", true);
            if (list.size() >= 3) {
                List<String> memList = StrSpliter.splitTrim(list.get(2), " ", true);
                JSONObject item = new JSONObject();
                item.put("pid", pid);
                item.put("COMMAND", memList.get(0));
                String mem = memList.get(4).replace(",", "");
                long aLong = Convert.toLong(mem, 0L);
                item.put("RES", aLong / 1024 + "mb");
                String status = memList.get(6);
                if ("RUNNING".equalsIgnoreCase(status)) {
                    item.put("S", "运行");
                } else if ("SUSPENDED".equalsIgnoreCase(status)) {
                    item.put("S", "睡眠");
                } else if ("NOT RESPONDING".equalsIgnoreCase(status)) {
                    item.put("S", "无响应");
                } else {
                    item.put("S", "未知");
                }
                item.put("USER", memList.get(7));
                item.put("TIME", memList.get(8));
                item.put("PR", 0);
                item.put("NI", 0);
                item.put("VIRT", 0);
                item.put("SHR", 0);
                item.put("CPU", 0);
                long totalMemory = new RuntimeInfo().getTotalMemory();
                double d = totalMemory / 1024D;
                double v = new BigDecimal(aLong).divide(new BigDecimal(d), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
                item.put("MEM", v * 100 + "%");
                if (v <= 0) {
                    item.put("MEM", 0);
                }
                setAttribute("item", item);
            }
        }
    }

    private JSONObject formatTop(String top) {
        List<String> list = StrSpliter.splitTrim(top, "\n", true);
        if (list.size() < 5) {
            return null;
        }
        String topName = list.get(list.size() - 2);
        List<String> nameList = StrSpliter.splitTrim(topName, " ", true);
        String ram = list.get(list.size() - 1);
        List<String> ramList = StrSpliter.splitTrim(ram, " ", true);
        JSONObject item = new JSONObject();
        for (int i = 0; i < nameList.size(); i++) {
            String name = nameList.get(i);
            String value = ramList.get(i);
            if (i == 0) {
                item.put("pid", value);
                continue;
            }
            name = name.replaceAll("%", "").replace("+", "");
            if ("VIRT".equalsIgnoreCase(name) || "RES".equalsIgnoreCase(name) || "SHR".equalsIgnoreCase(name)) {
                value = Convert.toLong(value) / 1024 + "mb";
            }
            if ("�".equals(name)) {
                name = "S";
            }
            if ("S".equalsIgnoreCase(name)) {
                if ("S".equalsIgnoreCase(value)) {
                    value = "睡眠";
                } else if ("R".equalsIgnoreCase(value)) {
                    value = "运行";
                } else if ("T".equalsIgnoreCase(value)) {
                    value = "跟踪/停止";
                } else if ("Z".equalsIgnoreCase(value)) {
                    value = "僵尸进程 ";
                } else if ("D".equalsIgnoreCase(value)) {
                    value = "不可中断的睡眠状态 ";
                }
            }
            if ("CPU".equalsIgnoreCase(name) || "MEM".equalsIgnoreCase(name)) {
                value += "%";
            }
            item.put(name, value);
        }
        return item;
    }

    /**
     * 导出堆栈信息
     */
    @RequestMapping(value = "stack", method = RequestMethod.GET)
    @ResponseBody
    public String stack(String tag) throws Exception {
        String fileName = ConfigBean.getInstance().getTempPathName() + "/" + tag + "_java_cpu.txt";
        fileName = FileUtil.normalize(fileName);
        try {
            String pid = AbstractCommander.getInstance().getPid(tag);
            String command = String.format("jstack %s >> %s ", pid, fileName);
            AbstractCommander.getInstance().execSystemCommand(command);
            downLoad(getResponse(), fileName);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            getResponse().sendRedirect("internal?tag=" + tag);
        }
        return JsonMessage.getString(200, "");
    }

    /**
     * 导出内存信息
     */
    @RequestMapping(value = "ram", method = RequestMethod.GET)
    @ResponseBody
    public String ram(String tag) throws Exception {
        String fileName = ConfigBean.getInstance().getTempPathName() + "/" + tag + "_java_ram.txt";
        fileName = FileUtil.normalize(fileName);
        try {
            String pid = AbstractCommander.getInstance().getPid(tag);
            String command = String.format("jmap -histo:live %s >> %s", pid, fileName);
            AbstractCommander.getInstance().execSystemCommand(command);
            downLoad(getResponse(), fileName);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            getResponse().sendRedirect("internal?tag=" + tag);
        }
        return JsonMessage.getString(200, "");
    }

    /**
     * 下载文件
     *
     * @param response response
     * @param fileName 文件名字
     */
    private void downLoad(HttpServletResponse response, String fileName) {
        //获取项目根路径
        File file = new File(fileName);
        ServletUtil.write(response, file);
        FileUtil.del(file);
    }

    @RequestMapping(value = "port", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getPort(String tag) {
        // 查询数据
        try {
            ProjectInfoModel projectInfoModel = projectInfoService.getItem(tag);
            String pId = commandService.execCommand(CommandService.CommandOp.pid, projectInfoModel).trim();
            if (StrUtil.isNotEmpty(pId)) {
                String cmd;
                boolean isLinux = true;
                if (AbstractCommander.OS_INFO.isLinux()) {
                    cmd = "netstat -antup | grep " + pId + " |grep -v \"CLOSE_WAIT\" | head -10";
                } else {
                    isLinux = false;
                    cmd = "netstat -nao | findstr " + pId;
                }
                String result = AbstractCommander.getInstance().execSystemCommand(cmd);
                JSONArray array = formatRam(isLinux, result);
                setAttribute("port", array);
                return "manage/port";
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return "manage/port";
    }

    private JSONArray formatRam(boolean isLinux, String result) {
        List<String> netList = StrSpliter.splitTrim(result, "\n", true);
        if (netList == null || netList.size() <= 0) {
            return null;
        }
        JSONArray array = new JSONArray();
        for (String str : netList) {
            List<String> list = StrSpliter.splitTrim(str, " ", true);
            JSONObject item = new JSONObject();
            if (isLinux) {
                item.put("protocol", list.get(0));
                item.put("receive", list.get(1));
                item.put("send", list.get(2));
                item.put("local", list.get(3));
                item.put("foreign", list.get(4));
                item.put("status", list.get(5));
                item.put("name", list.get(6));
            } else {
                item.put("protocol", list.get(0));
                item.put("receive", 0);
                item.put("send", 0);
                item.put("local", list.get(1));
                item.put("foreign", list.get(2));
                item.put("status", list.get(3));
                item.put("name", list.get(4));
            }
            array.add(item);
        }
        return array;
    }
}
