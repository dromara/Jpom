/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.controller.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.common.JsonMessage;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.model.user.UserModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.user.UserBindWorkspaceService;
import io.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import top.jpom.model.PageResultDto;

import java.util.List;

/**
 * 用户列表
 *
 * @author Administrator
 */
@RestController
@RequestMapping(value = "/user")
@Feature(cls = ClassFeature.USER)
@SystemPermission
public class UserListController extends BaseServerController {

    private final UserService userService;
    private final UserBindWorkspaceService userBindWorkspaceService;

    public UserListController(UserService userService,
                              UserBindWorkspaceService userBindWorkspaceService) {
        this.userService = userService;
        this.userBindWorkspaceService = userBindWorkspaceService;
    }

    /**
     * 查询所有用户
     *
     * @return json
     */
    @RequestMapping(value = "get_user_list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<UserModel>> getUserList() {
        PageResultDto<UserModel> userModelPageResultDto = userService.listPage(getRequest());
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
    public JsonMessage<List<UserModel>> getUserListAll() {
        List<UserModel> list = userService.list();
        return JsonMessage.success("success", list);
    }

    /**
     * 编辑用户
     *
     * @param type 操作类型
     * @return String
     */
    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<JSONObject> addUser(String type) {
        //
        boolean create = StrUtil.equals(type, "add");
        UserModel userModel = this.parseUser(create);
        JSONObject result = new JSONObject();
        if (create) {
            String randomPwd = RandomUtil.randomString(UserModel.SALT_LEN);
            String sha1Pwd = SecureUtil.sha1(randomPwd);
            userModel.setSalt(userService.generateSalt());
            userModel.setPassword(SecureUtil.sha1(sha1Pwd + userModel.getSalt()));
            userService.insert(userModel);
            result.put("randomPwd", randomPwd);
        } else {
            UserModel model = userService.getByKey(userModel.getId());
            Assert.notNull(model, "不存在对应的用户");
            boolean systemUser = userModel.isSystemUser();
            if (!systemUser) {
                Assert.state(!model.isSuperSystemUser(), "不能取消超级管理员的权限");
            }
            if (model.isSuperSystemUser()) {
                Assert.state(userModel.getStatus() == 1, "不能禁用超级管理员");
            }
            UserModel optUser = getUser();
            if (StrUtil.equals(model.getId(), optUser.getId())) {
                Assert.state(optUser.isSuperSystemUser(), "不能修改自己的信息");
            }
            userService.updateById(userModel);
            // 删除旧数据
            userBindWorkspaceService.deleteByUserId(userModel.getId());
        }
        return new JsonMessage<>(200, "操作成功", result);
    }

    private UserModel parseUser(boolean create) {
        String id = getParameter("id");
        boolean email = Validator.isEmail(id);
        if (email) {
            int length = id.length();
            Assert.state(length <= Const.ID_MAX_LEN && length >= UserModel.USER_NAME_MIN_LEN, "登录名如果为邮箱格式,长度必须" + UserModel.USER_NAME_MIN_LEN + "-" + Const.ID_MAX_LEN);
        } else {
            Validator.validateGeneral(id, UserModel.USER_NAME_MIN_LEN, Const.ID_MAX_LEN, "登录名格式不正确（英文字母 、数字和下划线）,并且长度必须" + UserModel.USER_NAME_MIN_LEN + "-" + Const.ID_MAX_LEN);
        }

        Assert.state(!StrUtil.equalsAnyIgnoreCase(id, UserModel.SYSTEM_OCCUPY_NAME, UserModel.SYSTEM_ADMIN), "当前登录名已经被系统占用");

        UserModel userModel = new UserModel();
        UserModel optUser = getUser();
        if (create) {
            // 登录名重复
            boolean exists = userService.exists(new UserModel(id));
            Assert.state(!exists, "登录名已经存在");
            userModel.setParent(optUser.getId());
        }
        userModel.setId(id);
        //
        String name = getParameter("name");
        Assert.hasText(name, "请输入账户昵称");
        int len = name.length();
        Assert.state(len <= 10 && len >= 2, "昵称长度只能是2-10");

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
        Assert.notEmpty(permissionGroupList, "用户未选择权限组");
        userModel.setPermissionGroup(CollUtil.join(permissionGroupList, StrUtil.AT, StrUtil.AT, StrUtil.AT));
        //
        int status = getParameterInt("status", 1);
        Assert.state(status == 0 || status == 1, "选择的用户状态异常");
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
    public JsonMessage<Object> deleteUser(String id) {
        UserModel userName = getUser();
        Assert.state(!StrUtil.equals(userName.getId(), id), "不能删除自己");

        UserModel userModel = userService.getByKey(id);
        Assert.notNull(userModel, "非法访问");
        if (userModel.isSystemUser()) {
            // 如果是系统管理员，判断个数
            Assert.state(userService.systemUserCount() > 1, "系统中的系统管理员账号数量必须存在一个以上");
        }
        // 非系统管理员不支持删除演示账号
        Assert.state(!userModel.isRealDemoUser(), "演示账号不支持删除");
        userService.delByKey(id);
        // 删除工作空间
        userBindWorkspaceService.deleteByUserId(id);
        return JsonMessage.success("删除成功");
    }

    /**
     * 解锁用户锁定状态
     *
     * @param id id
     * @return json
     */
    @GetMapping(value = "unlock", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<Object> unlock(@ValidatorItem String id) {
        UserModel update = UserModel.unLock(id);
        userService.updateById(update);
        return JsonMessage.success("解锁成功");
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
    public JsonMessage<Object> closeMfa(@ValidatorItem String id) {
        UserModel update = new UserModel(id);
        update.setTwoFactorAuthKey(StrUtil.EMPTY);
        userService.updateById(update);
        return JsonMessage.success("关闭成功");
    }

    /**
     * 重置用户密码
     *
     * @param id id
     * @return json
     */
    @GetMapping(value = "rest-user-pwd", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<JSONObject> restUserPwd(@ValidatorItem String id) {
        UserModel userModel = userService.getByKey(id);
        Assert.notNull(userModel, "账号不存在");
        Assert.state(!userModel.isSuperSystemUser(), "超级管理员不能通过此方式重置密码");
        //不支持重置演示账号
        Assert.state(!userModel.isRealDemoUser(), "演示账号不支持重置密码");
        String randomPwd = RandomUtil.randomString(UserModel.SALT_LEN);
        String sha1Pwd = SecureUtil.sha1(randomPwd);
        userService.updatePwd(id, sha1Pwd);
        //
        JSONObject result = new JSONObject();
        result.put("randomPwd", randomPwd);
        return JsonMessage.success("重置成功", result);
    }
}
