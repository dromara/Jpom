package io.jpom.controller.user;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.LoginInterceptor;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.user.UserService;
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
@Feature(cls = ClassFeature.USER)
public class UserInfoController extends BaseServerController {
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
    public String updatePwd(
            @ValidatorConfig(value = {
                    @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "密码不能为空")
            }) String oldPwd,
            @ValidatorConfig(value = {
                    @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "密码不能为空")
            }) String newPwd) {
        if (oldPwd.equals(newPwd)) {
            return JsonMessage.getString(400, "新旧密码一致");
        }
        UserModel userName = getUser();
        if (userName.isDemoUser()) {
            return JsonMessage.getString(402, "当前账户为演示账号，不支持修改密码");
        }
        try {
            UserModel userModel = userService.simpleLogin(userName.getId(), oldPwd);
            if (userModel == null || userModel.getPwdErrorCount() > 0) {
                return JsonMessage.getString(500, "旧密码不正确！");
            }
            userModel.setPassword(newPwd);
            userService.updateItem(userModel);
            // 如果修改成功，则销毁会话
            getSession().invalidate();
            return JsonMessage.getString(200, "修改密码成功！");
        } catch (Exception e) {
            DefaultSystemLog.getLog().error(e.getMessage(), e);
            return JsonMessage.getString(500, "系统异常：" + e.getMessage());
        }
    }

    /**
     * 修改用户昵称
     *
     * @param name 新昵称
     * @return json
     */
    @RequestMapping(value = "updateName", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updateName(@ValidatorConfig(value = {
            @ValidatorItem(value = ValidatorRule.NOT_BLANK, range = "2:10", msg = "昵称长度只能是2-10")
    }) String name) {
        UserModel userModel = getUser();
        userModel = userService.getItem(userModel.getId());
        userModel.setName(name);
        userService.updateItem(userModel);
        setSessionAttribute(LoginInterceptor.SESSION_NAME, userModel);
        return JsonMessage.getString(200, "修改成功");
    }

    /**
     * 删除用户
     *
     * @param id 用户id
     * @return String
     */
    @RequestMapping(value = "deleteUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @OptLog(UserOperateLogV1.OptType.DelUer)
    @Feature(method = MethodFeature.DEL)
    public String deleteUser(String id) {
        UserModel userName = getUser();
        if (userName.getId().equals(id)) {
            return JsonMessage.getString(400, "不能删除自己");
        }
        UserModel userModel = userService.getItem(id);
        if (userModel == null) {
            return JsonMessage.getString(501, "非法访问");
        }
        // 非系统管理员不支持删除演示账号
        if (!userName.isSystemUser() && userModel.isDemoUser()) {
            return JsonMessage.getString(402, "演示账号不支持删除");
        }
        if (userModel.isSystemUser()) {
            return JsonMessage.getString(400, "非法访问:-5");
        }
        userService.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }

    /**
     * 解锁用户锁定状态
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "unlock", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @OptLog(UserOperateLogV1.OptType.UnlockUer)
    @Feature(method = MethodFeature.EDIT)
    public String unlock(String id) {
        UserModel userModel = userService.getItem(id);
        if (userModel == null) {
            return JsonMessage.getString(400, "修改失败:-1");
        }
        userModel.unLock();
        userService.updateItem(userModel);
        return JsonMessage.getString(200, "解锁成功");
    }
}
