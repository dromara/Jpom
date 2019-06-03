package cn.keepbx.jpom.controller.monitor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.common.commander.AbstractProjectCommander;
import cn.keepbx.jpom.common.commander.AbstractSystemCommander;
import cn.keepbx.jpom.model.system.NetstatModel;
import cn.keepbx.jpom.model.system.ProcessModel;
import cn.keepbx.jpom.system.AgentConfigBean;
import cn.keepbx.jpom.util.CommandUtil;
import cn.keepbx.jpom.util.JvmUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.tools.attach.VirtualMachine;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.util.List;

/**
 * 内存查看
 *
 * @author Administrator
 */
@RestController
@RequestMapping(value = "/manage/")
public class InternalController extends BaseAgentController {

    /**
     * 获取内存信息
     */
    @RequestMapping(value = "internal_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getInternal(String tag) throws Exception {
        int pid = AbstractProjectCommander.getInstance().getPid(tag);
        if (pid <= 0) {
            return JsonMessage.getString(400, "");
        }
        JSONObject jsonObject = new JSONObject();
        ProcessModel item = AbstractSystemCommander.getInstance().getPidInfo(pid);
        jsonObject.put("process", item);
        JSONObject beanMem = getBeanMem(tag);
        jsonObject.put("beanMem", beanMem);
        //获取端口信息
        List<NetstatModel> netstatModels = AbstractProjectCommander.getInstance().listNetstat(pid, false);
        jsonObject.put("netstat", netstatModels);
        return JsonMessage.getString(200, "", jsonObject);
    }

    /**
     * 查询监控线程列表
     */
    @RequestMapping(value = "threadInfos", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getThreadInfos(String tag) throws Exception {
        int limit = getParameterInt("limit", 10);
        int page = getParameterInt("page", 1);
        VirtualMachine virtualMachine = JvmUtil.getVirtualMachine(tag);
        ThreadMXBean threadMXBean = JvmUtil.getThreadMXBean(virtualMachine);
        if (threadMXBean == null) {
            return JsonMessage.getString(200, "");
        }
        //启用线程争用监视
        threadMXBean.setThreadContentionMonitoringEnabled(true);
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        if (threadInfos == null || threadInfos.length <= 0) {
            return JsonMessage.getString(200, "");
        }
        JSONArray array = new JSONArray();
        int index = (page - 1) * limit;
        int len = limit + index;
        int length = threadInfos.length;
        if (len > length) {
            len = length;
        }
        for (int i = index; i < len; i++) {
            ThreadInfo threadInfo = threadInfos[i];
            Thread.State threadState = threadInfo.getThreadState();
            JSONObject object = new JSONObject();
            object.put("id", threadInfo.getThreadId());
            object.put("name", threadInfo.getThreadName());
            object.put("status", threadState);
            object.put("waitedCount", threadInfo.getWaitedCount());
            object.put("waitedTime", threadInfo.getWaitedTime());
            object.put("blockedCount", threadInfo.getBlockedCount());
            object.put("blockedTime", threadInfo.getBlockedTime());
            object.put("isInNative", threadInfo.isInNative());
            object.put("isSuspended", threadInfo.isSuspended());
            array.add(object);
        }
        JSONObject object = new JSONObject();
        object.put("count", length);
        object.put("data", array);
        return JsonMessage.getString(200, "", object);
    }

    /**
     * 获取jvm内存
     *
     * @param tag tag
     * @return JSONObject
     */
    private JSONObject getBeanMem(String tag) {
        try {
            VirtualMachine virtualMachine = JvmUtil.getVirtualMachine(tag);
            MemoryMXBean memoryMXBean = JvmUtil.getMemoryMXBean(virtualMachine);
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


    /**
     * 导出堆栈信息
     */
    @RequestMapping(value = "internal_stack", method = RequestMethod.GET)
    @ResponseBody
    public String stack(String tag) throws Exception {
        String fileName = AgentConfigBean.getInstance().getTempPathName() + "/" + tag + "_java_cpu.txt";
        fileName = FileUtil.normalize(fileName);
        try {
            int pid = AbstractProjectCommander.getInstance().getPid(tag);
            if (pid <= 0) {
                return JsonMessage.getString(400, "未运行");
            }
            String command = String.format("jstack %s >> %s ", pid, fileName);
            CommandUtil.execSystemCommand(command);
            downLoad(getResponse(), fileName);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
//            getResponse().sendRedirect("internal?tag=" + tag);
        }
        return JsonMessage.getString(200, "");
    }

    /**
     * 导出内存信息
     */
    @RequestMapping(value = "internal_ram", method = RequestMethod.GET)
    @ResponseBody
    public String ram(String tag) throws Exception {
        String fileName = AgentConfigBean.getInstance().getTempPathName() + "/" + tag + "_java_ram.txt";
        fileName = FileUtil.normalize(fileName);
        try {
            int pid = AbstractProjectCommander.getInstance().getPid(tag);
            if (pid <= 0) {
                return JsonMessage.getString(400, "未运行");
            }
            String command = String.format("jmap -histo:live %s >> %s", pid, fileName);
            CommandUtil.execSystemCommand(command);
            downLoad(getResponse(), fileName);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
//            getResponse().sendRedirect("internal?tag=" + tag);
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
}
