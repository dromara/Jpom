package cn.keepbx.jpom.controller;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.commander.AbstractCommander;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.socket.top.TopManager;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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
                topInfo = TopManager.getTopMonitor(s);
            } else {
                topInfo = TopManager.getWindowsMonitor();
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return JsonMessage.getString(200, "", topInfo);
    }

    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getProcessList() {
        JSONArray array = null;
        try {
            if (AbstractCommander.OS_INFO.isLinux()) {
                String head = AbstractCommander.getInstance().execSystemCommand("top -b -n 1 | head -7");
                String s = AbstractCommander.getInstance().execSystemCommand("top -b -n 1 | grep java");
                array = TopManager.formatLinuxTop(head + s);
            } else {
                String s = AbstractCommander.getInstance().execSystemCommand("tasklist /V | findstr java");
                array = TopManager.formatWindowsProcess(s);
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return JsonMessage.getString(200, "", array);
    }
}
