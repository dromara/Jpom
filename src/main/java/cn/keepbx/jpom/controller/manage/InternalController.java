package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
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
import com.sun.management.OperatingSystemMXBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
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
        getMem(tag);
        setAttribute("tag", tag);
        //获取端口信息
        JSONArray port = getPort(tag);
        setAttribute("port", port);
        return "manage/internal";
    }

    /**
     * 获取内存监控信息
     *
     * @param tag 项目id
     */
    private void getMem(String tag) throws Exception {
        String pid = AbstractCommander.getInstance().getPid(tag);
        if (AbstractCommander.OS_INFO.isLinux()) {
            String command = "top -b -n 1 -p " + pid;
            String internal = AbstractCommander.getInstance().execCommand(command);
            setAttribute("item", formatTop(internal));
        } else {
            JSONObject windowsMem = getWindowsMem(pid);
            setAttribute("item", windowsMem);
            JSONObject beanMem = getBeanMem(tag);
            setAttribute("beanMem", beanMem);
        }
    }

    /**
     * 获取window下内存
     *
     * @param pid 进程id
     */
    private JSONObject getWindowsMem(String pid) throws Exception {
        String command = "tasklist /V /FI \"pid eq " + pid + "\"";
        String result = AbstractCommander.getInstance().execCommand(command);
        List<String> list = StrSpliter.splitTrim(result, "\n", true);
        if (list.size() >= 3) {
            List<String> memList = StrSpliter.splitTrim(list.get(2), " ", true);
            JSONObject item = new JSONObject();
            item.put("pid", pid);
            item.put("COMMAND", memList.get(0));
            //使用内存 kb
            String mem = memList.get(4).replace(",", "");
            long aLong = Convert.toLong(mem, 0L);
            item.put("RES", aLong / 1024 + " MB");
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
            item.put("PR", -1);
            item.put("NI", -1);
            item.put("VIRT", -1);
            item.put("SHR", -1);
            item.put("CPU", -1);
            OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            //服务器总内存
            long totalMemorySize = operatingSystemMXBean.getTotalPhysicalMemorySize();
            double v = new BigDecimal(aLong).divide(new BigDecimal(totalMemorySize / 1024), 4, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
            item.put("MEM", String.format("%.2f", v) + "%");
            if (v <= 0) {
                item.put("MEM", 0);
            }
            return item;
        }
        return null;
    }

    /**
     * 获取jvm内存
     *
     * @param tag tag
     * @return JSONObject
     */
    private JSONObject getBeanMem(String tag) {
        try {
            MemoryMXBean memoryMXBean = AbstractCommander.getInstance().getMemoryMXBean(tag);
            if (memoryMXBean == null) {
                return null;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("mount", memoryMXBean.getObjectPendingFinalizationCount());
            //堆内存
            MemoryUsage memory = memoryMXBean.getHeapMemoryUsage();
            //非堆内存
            MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
            long used = memory.getUsed();
            long max = memory.getMax();
            //未定义
            if (-1 == max) {
                max = memory.getCommitted();
            }
            //计算使用内存占最大内存的百分比
            double v = new BigDecimal(used).divide(new BigDecimal(max), 2, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
            jsonObject.put("heapUsed", FileUtil.readableFileSize(used));
            jsonObject.put("heapProportion", String.format("%.2f", v) + "%");
            jsonObject.put("heapCommitted", FileUtil.readableFileSize(memory.getCommitted()));
            long nonUsed = nonHeapMemoryUsage.getUsed();
            long nonMax = nonHeapMemoryUsage.getMax();
            long nonCommitted = nonHeapMemoryUsage.getCommitted();
            if (-1 == nonMax) {
                nonMax = nonCommitted;
            }
            jsonObject.put("nonHeapUsed", FileUtil.readableFileSize(nonUsed));
            double proportion = new BigDecimal(nonUsed).divide(new BigDecimal(nonMax), 2, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
            jsonObject.put("nonHeapProportion", String.format("%.2f", proportion) + "%");
            jsonObject.put("nonHeapCommitted", FileUtil.readableFileSize(nonCommitted));
            return jsonObject;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
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
                value = Convert.toLong(value) / 1024 + " MB";
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

    /**
     * 获取端口
     *
     * @param tag 项目id
     */
    private JSONArray getPort(String tag) {
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
                    cmd = "netstat -nao -p tcp | findstr /V \"CLOSE_WAIT\" | findstr " + pId;
                }
                String result = AbstractCommander.getInstance().execSystemCommand(cmd);
                return formatRam(isLinux, result);
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }

    private JSONArray formatRam(boolean isLinux, String result) {
        List<String> netList = StrSpliter.splitTrim(result, "\n", true);
        if (netList == null || netList.size() <= 0) {
            return null;
        }
        JSONArray array = new JSONArray();
        for (String str : netList) {
            List<String> list = StrSpliter.splitTrim(str, " ", true);
            if (list.size() < 5) {
                continue;
            }
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
