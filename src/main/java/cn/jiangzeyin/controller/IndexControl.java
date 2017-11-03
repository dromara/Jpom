package cn.jiangzeyin.controller;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.base.AbstractBaseControl;
import cn.jiangzeyin.common.interceptor.LoginInterceptor;
import cn.jiangzeyin.service.IndexService;
import cn.jiangzeyin.service.UserService;
import cn.jiangzeyin.system.log.SystemLog;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Controller
@RequestMapping(value = "/")
public class IndexControl extends AbstractBaseControl {

    @Resource
    IndexService indexService;
    @Resource
    UserService userService;

    /**
     * 加载首页
     * @return
     */
    @RequestMapping(value = "index")
    public String index() {

        return "index";
    }

    @RequestMapping(value = "welcome")
    public String welcome() {

        return "welcome";
    }

    /**
     * 退出登录
     * @return
     */
    @RequestMapping(value = "logout")
    public String logout() {
        session.invalidate();
        return "login";
    }

    /**
     * 修改密码
     * @param oldPwd 旧密码
     * @param newPwd 新密码
     * @return
     */
    @RequestMapping(value = "updatePwd")
    @ResponseBody
    public String updatePwd(String oldPwd, String newPwd) {

        try {
            String result = userService.updatePwd(session.getAttribute(LoginInterceptor.SESSION_NAME).toString(), oldPwd, newPwd);

            // 用户不存在
            if ("notexist".equals(result)) {
                return  JsonMessage.getString(500, "用户不存在！");
            }

            // 旧密码不正确
            if ("olderror".equals(result)) {
                return  JsonMessage.getString(500, "旧密码不正确！");
            }

            // 如果修改成功，则销毁会话
            session.invalidate();
            return JsonMessage.getString(200, "修改密码成功！");
        } catch (Exception e) {
            SystemLog.ERROR().info(e.getMessage());
            return JsonMessage.getString(500, e.getMessage());
        }
    }
}
