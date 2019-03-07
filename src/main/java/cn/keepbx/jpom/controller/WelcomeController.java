package cn.keepbx.jpom.controller;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.manage.CommandService;
import cn.keepbx.jpom.socket.top.TopManager;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 欢迎页
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/")
public class WelcomeController extends BaseController {
    @Resource
    private CommandService commandService;

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
            String s = commandService.execCommand(CommandService.CommandOp.top, null, null);
            topInfo = TopManager.getTopInfo(s);
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return JsonMessage.getString(200, "", topInfo);
    }
}
