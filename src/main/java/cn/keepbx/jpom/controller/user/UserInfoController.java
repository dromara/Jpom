package cn.keepbx.jpom.controller.user;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.controller.BaseController;
import cn.keepbx.jpom.service.UserService;
import cn.keepbx.jpom.system.ConfigBean;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户管理
 *
 * @author jiangzeyin
 * @date 2018/9/28
 */
@RestController
@RequestMapping(value = "/user")
public class UserInfoController extends BaseController {
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
        if (ConfigBean.getInstance().showDemo) {
            return JsonMessage.getString(401, "演示模式不允许修改自己的密码");
        }
        try {
            String result = userService.updatePwd(userName.getId(), oldPwd, newPwd);
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

    /**
     * 删除用户
     *
     * @param id 用户id
     * @return String
     */
    @RequestMapping(value = "deleteUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteUser(String id) {
        if (userName == null) {
            return JsonMessage.getString(401, "系统异常：不能删除");
        }
        if (userName.getId().equals(id)) {
            return JsonMessage.getString(400, "不能删除自己");
        }
        boolean b = userService.deleteUser(id);
        if (b) {
            return JsonMessage.getString(200, "删除成功");
        }
        return JsonMessage.getString(400, "删除失败");
    }

    /**
     * 新增用户
     *
     * @return String
     */
    @RequestMapping(value = "addUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String addUser(String id, String name, String manage, String password) {
        boolean manager = userService.isManager("", getUserName());
        if (!manager) {
            return JsonMessage.getString(400, "你还没有权限");
        }
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "登录名不能为空");
        }
        if (StrUtil.isEmpty(password)) {
            return JsonMessage.getString(400, "密码不能为空");
        }
        int length = password.length();
        if (length < 6) {
            return JsonMessage.getString(400, "密码长度为6-12位");
        }
        boolean b = userService.addUser(id, name, password, "true".equals(manage));
        if (b) {
            return JsonMessage.getString(200, "添加成功");
        }
        return JsonMessage.getString(400, "添加失败");
    }

    /**
     * 修改用户
     *
     * @return String
     */
    @RequestMapping(value = "updateUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updateUser(String id, String name, String manage, String password, String project) {
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "登录名不能为空");
        }
        if (!StrUtil.isEmpty(password)) {
            int length = password.length();
            if (length < 6) {
                return JsonMessage.getString(400, "密码长度为6-12位");
            }
        }
        JSONArray projects = null;
        if (StrUtil.isNotEmpty(project)) {
            projects = (JSONArray) JSONArray.toJSON(StrUtil.splitToArray(project, ','));
        }
        boolean b = userService.updateUser(id, name, password, manage, projects);
        if (b) {
            return JsonMessage.getString(200, "修改成功");
        }
        return JsonMessage.getString(400, "修改失败");
    }

}
