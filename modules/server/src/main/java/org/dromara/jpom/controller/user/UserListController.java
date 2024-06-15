/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.dromara.jpom.service.user.UserBindWorkspaceService;
import org.dromara.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户列表
 *
 * @author bwcx_jzy
 */
@RestController
@RequestMapping(value = "/user")
@Feature(cls = ClassFeature.USER)
@SystemPermission
public class UserListController extends BaseServerController {

    private final UserService userService;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public UserListController(UserService userService,
                              UserBindWorkspaceService userBindWorkspaceService,
                              TriggerTokenLogServer triggerTokenLogServer) {
        this.userService = userService;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 查询所有用户
     *
     * @return json
     */
    @RequestMapping(value = "get_user_list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<UserModel>> getUserList(HttpServletRequest request) {
        PageResultDto<UserModel> userModelPageResultDto = userService.listPage(request);
        userModelPageResultDto.each(userModel -> {
            boolean bindMfa = userService.hasBindMfa(userModel.getId());
            if (bindMfa) {
                userModel.setTwoFactorAuthKey("true");
            }
        });
        return new JsonMessage<>(200, "", userModelPageResultDto);
    }

    /**
     * 获取所有管理员信息
     * get all admin user list
     *
     * @return json
     * @author Hotstrip
     */
    @RequestMapping(value = "get_user_list_all", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<UserModel>> getUserListAll() {
        List<UserModel> list = userService.list();
        return JsonMessage.success("", list);
    }

    /**
     * 编辑用户
     *
     * @param type 操作类型
     * @return String
     */
    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<JSONObject> addUser(String type) {
        //
        boolean create = StrUtil.equals(type, "add");
        UserModel userModel = this.parseUser(create);
        JSONObject result = new JSONObject();
        if (create) {
            String randomPwd = RandomUtil.randomString(UserModel.SALT_LEN);
            String sha1Pwd = SecureUtil.sha1(randomPwd);
            userModel.setSalt(userService.generateSalt());
            userModel.setPassword(SecureUtil.sha1(sha1Pwd + userModel.getSalt()));
            userModel.setSource("jpom");
            userService.insert(userModel);
            result.put("randomPwd", randomPwd);
        } else {
            UserModel model = userService.getByKey(userModel.getId());
            Assert.notNull(model, I18nMessageUtil.get("i18n.user_not_exist.5387"));
            boolean systemUser = userModel.isSystemUser();
            if (!systemUser) {
                Assert.state(!model.isSuperSystemUser(), I18nMessageUtil.get("i18n.cannot_cancel_super_admin_permissions.99b5"));
            }
            if (model.isSuperSystemUser()) {
                Assert.state(userModel.getStatus() == 1, I18nMessageUtil.get("i18n.cannot_disable_super_admin.6429"));
            }
            UserModel optUser = getUser();
            if (StrUtil.equals(model.getId(), optUser.getId())) {
                Assert.state(optUser.isSuperSystemUser(), I18nMessageUtil.get("i18n.cannot_modify_own_info.4036"));
            }
            userService.updateById(userModel);
            // 删除旧数据
            userBindWorkspaceService.deleteByUserId(userModel.getId());
        }
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.operation_succeeded.3313"), result);
    }

    private UserModel parseUser(boolean create) {
        String id = getParameter("id");
        boolean email = Validator.isEmail(id);
        if (email) {
            int length = id.length();
            Assert.state(length <= Const.ID_MAX_LEN && length >= UserModel.USER_NAME_MIN_LEN, StrUtil.format(I18nMessageUtil.get("i18n.login_name_email_format_length_range.25f3"), UserModel.USER_NAME_MIN_LEN, Const.ID_MAX_LEN));
        } else {
            String checkId = StrUtil.replace(id, "-", "_");
            Validator.validateGeneral(checkId, UserModel.USER_NAME_MIN_LEN, Const.ID_MAX_LEN, StrUtil.format(I18nMessageUtil.get("i18n.login_name_format_incorrect.f789"), UserModel.USER_NAME_MIN_LEN, Const.ID_MAX_LEN));
        }

        Assert.state(!StrUtil.equalsAnyIgnoreCase(id, UserModel.SYSTEM_OCCUPY_NAME.get(), UserModel.SYSTEM_ADMIN), I18nMessageUtil.get("i18n.login_name_already_taken.5b46"));

        UserModel userModel = new UserModel();
        UserModel optUser = getUser();
        if (create) {
            // 登录名重复
            boolean exists = userService.exists(new UserModel(id));
            Assert.state(!exists, I18nMessageUtil.get("i18n.login_name_already_exists.2511"));
            userModel.setParent(optUser.getId());
        }
        userModel.setId(id);
        //
        String name = getParameter("name");
        Assert.hasText(name, I18nMessageUtil.get("i18n.account_name_nickname_required.b757"));
        int len = name.length();
        Assert.state(len <= 10 && len >= 2, I18nMessageUtil.get("i18n.nickname_length_limit.6312"));

        userModel.setName(name);

//        String password = getParameter("password");
//        if (create || StrUtil.isNotEmpty(password)) {
//            Assert.hasText(password, "密码不能为空");
//            // 修改用户
//            Assert.state(create || optUser.isSystemUser(), "只有系统管理员才能重置用户密码");
//            userModel.setSalt(userService.generateSalt());
//            userModel.setPassword(SecureUtil.sha1(password + userModel.getSalt()));
//        }

        int systemUser = getParameterInt("systemUser", 0);
        userModel.setSystemUser(systemUser);
        //
        String permissionGroup = getParameter("permissionGroup");
        List<String> permissionGroupList = StrUtil.split(permissionGroup, StrUtil.AT);
        Assert.notEmpty(permissionGroupList, I18nMessageUtil.get("i18n.user_not_select_permission_group.1091"));
        userModel.setPermissionGroup(CollUtil.join(permissionGroupList, StrUtil.AT, StrUtil.AT, StrUtil.AT));
        //
        int status = getParameterInt("status", 1);
        Assert.state(status == 0 || status == 1, I18nMessageUtil.get("i18n.selected_user_status_abnormal.efcf"));
        userModel.setStatus(status);
        return userModel;
    }

    /**
     * 删除用户
     *
     * @param id 用户id
     * @return String
     */
    @RequestMapping(value = "deleteUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> deleteUser(String id) {
        UserModel userName = getUser();
        Assert.state(!StrUtil.equals(userName.getId(), id), I18nMessageUtil.get("i18n.cannot_delete_self.fec9"));

        UserModel userModel = userService.getByKey(id);
        Assert.notNull(userModel, I18nMessageUtil.get("i18n.illegal_access.c365"));
        if (userModel.isSystemUser()) {
            // 如果是系统管理员，判断个数
            Assert.state(userService.systemUserCount() > 1, I18nMessageUtil.get("i18n.admin_account_required.31e0"));
        }
        Assert.state(!userModel.isSuperSystemUser(), I18nMessageUtil.get("i18n.cannot_delete_super_admin.68e2"));
        // 非系统管理员不支持删除演示账号
        Assert.state(!userModel.isRealDemoUser(), I18nMessageUtil.get("i18n.demo_account_not_support_delete.f9a6"));
        userService.delByKey(id);
        // 删除工作空间
        userBindWorkspaceService.deleteByUserId(id);
        //
        triggerTokenLogServer.delByUserId(id);
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }

    /**
     * 解锁用户锁定状态
     *
     * @param id id
     * @return json
     */
    @GetMapping(value = "unlock", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Object> unlock(@ValidatorItem String id) {
        UserModel update = UserModel.unLock(id);
        userService.updateById(update);
        return JsonMessage.success(I18nMessageUtil.get("i18n.unlock_success.4cea"));
    }

    /**
     * 关闭用户 mfa
     *
     * @param id id
     * @return json
     */
    @GetMapping(value = "close_user_mfa", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission(superUser = true)
    public IJsonMessage<Object> closeMfa(@ValidatorItem String id) {
        UserModel update = new UserModel(id);
        update.setTwoFactorAuthKey(StrUtil.EMPTY);
        userService.updateById(update);
        return JsonMessage.success(I18nMessageUtil.get("i18n.close_success.8a31"));
    }

    /**
     * 重置用户密码
     *
     * @param id id
     * @return json
     */
    @GetMapping(value = "rest-user-pwd", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<JSONObject> restUserPwd(@ValidatorItem String id) {
        UserModel userModel = userService.getByKey(id);
        Assert.notNull(userModel, I18nMessageUtil.get("i18n.account_does_not_exist.8402"));
        Assert.state(!userModel.isSuperSystemUser(), I18nMessageUtil.get("i18n.super_admin_cannot_reset_password_this_way.0761"));
        //不支持重置演示账号
        Assert.state(!userModel.isRealDemoUser(), I18nMessageUtil.get("i18n.demo_account_not_support_reset_password.a595"));
        String randomPwd = RandomUtil.randomString(UserModel.SALT_LEN);
        String sha1Pwd = SecureUtil.sha1(randomPwd);
        userService.updatePwd(id, sha1Pwd);
        //
        JSONObject result = new JSONObject();
        result.put("randomPwd", randomPwd);
        return JsonMessage.success(I18nMessageUtil.get("i18n.reset_success.faa3"), result);
    }
}
