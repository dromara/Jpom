package cn.jiangzeyin.controller.user;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.model.UserInfoMode;
import cn.jiangzeyin.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author jiangzeyin
 * @date 2018/9/28
 */
@RestController
@RequestMapping(value = "/")
public class UpdatePwdController extends BaseController {
    @Resource
    private UserService userService;

    /**
     * 修改密码
     *
     * @param oldPwd 旧密码
     * @param newPwd 新密码
     * @return json
     */
    @RequestMapping(value = "updatePwd", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updatePwd(String oldPwd, String newPwd) {
        try {
            String result = userService.updatePwd(userName, oldPwd, newPwd);
            // 用户不存在
            if ("notexist".equals(result)) {
                return JsonMessage.getString(500, "用户不存在！");
            }
            // 旧密码不正确
            if ("olderror".equals(result)) {
                return JsonMessage.getString(500, "旧密码不正确！");
            }
            // 如果修改成功，则销毁会话
            getSession().invalidate();
            return JsonMessage.getString(200, "修改密码成功！");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }





}
