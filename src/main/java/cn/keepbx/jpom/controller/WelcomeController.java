package cn.keepbx.jpom.controller;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.commander.AbstractCommander;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.socket.top.TopManager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.management.OperatingSystemMXBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.management.ManagementFactory;

/**
 * 欢迎页
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/")
public class WelcomeController extends BaseController {

    @RequestMapping(value = "welcome", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String welcome() {
        UserModel userName = getUser();
        setAttribute("userInfo", userName.getUserMd5Key());
        return "welcome";
    }

    @RequestMapping(value = "getTop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getTop() {
        String topInfo = "";
        try {
            if (AbstractCommander.OS_INFO.isLinux()) {
                String s = AbstractCommander.getInstance().execCommand("top -b -n 1");
                topInfo = TopManager.getTopInfo(s);
            } else {
//                https://docs.oracle.com/javase/7/docs/jre/api/management/extension/com/sun/management/OperatingSystemMXBean.html
                OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                long totalPhysicalMemorySize = operatingSystemMXBean.getTotalPhysicalMemorySize();
                long freePhysicalMemorySize = operatingSystemMXBean.getFreePhysicalMemorySize();
                //最近系统cpu使用量
                double systemCpuLoad = operatingSystemMXBean.getSystemCpuLoad();
                if (systemCpuLoad <= 0) {
                    systemCpuLoad = 0;
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("top", true);
                JSONArray memory = new JSONArray();
                JSONObject mem1 = new JSONObject();
                mem1.put("name", "占用内存");
                mem1.put("value", totalPhysicalMemorySize - freePhysicalMemorySize);
                JSONObject mem2 = new JSONObject();
                mem2.put("name", "空闲内存");
                mem2.put("value", freePhysicalMemorySize);
                memory.add(mem1);
                memory.add(mem2);
                JSONObject cpu1 = new JSONObject();
                cpu1.put("name", "占用cpu");
                cpu1.put("value", systemCpuLoad);
                JSONObject cpu2 = new JSONObject();
                cpu2.put("name", "空闲cpu");
                cpu2.put("value", 1 - systemCpuLoad);
                JSONArray cpus = new JSONArray();
                cpus.add(cpu1);
                cpus.add(cpu2);
                jsonObject.put("memory", memory);
                jsonObject.put("cpu", cpus);
                topInfo = jsonObject.toJSONString();
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return JsonMessage.getString(200, "", topInfo);
    }
}
