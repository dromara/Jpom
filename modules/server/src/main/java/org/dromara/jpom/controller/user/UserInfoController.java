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
package org.dromara.jpom.controller.user;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.model.user.UserBindWorkspaceModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.user.UserBindWorkspaceService;
import org.dromara.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 用户管理
 *
 * @author bwcx_jzy
 * @since 2018/9/28
 */
@RestController
@RequestMapping(value = "/user")
@Slf4j
public class UserInfoController extends BaseServerController {

    private final UserService userService;
    private final UserBindWorkspaceService userBindWorkspaceService;

    public UserInfoController(UserService userService,
                              UserBindWorkspaceService userBindWorkspaceService) {
        this.userService = userService;
        this.userBindWorkspaceService = userBindWorkspaceService;
    }

    /**
     * 修改密码
     *
     * @param oldPwd 旧密码
     * @param newPwd 新密码
     * @return json
     */
    @RequestMapping(value = "updatePwd", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> updatePwd(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "密码不能为空") String oldPwd,
                                          @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "密码不能为空") String newPwd,
                                          HttpSession session) {
        Assert.state(!StrUtil.equals(oldPwd, newPwd), "新旧密码一致");
        UserModel userName = getUser();
        Assert.state(!userName.isDemoUser(), "当前账户为演示账号，不支持修改密码");
        try {
            UserModel userModel = userService.simpleLogin(userName.getId(), oldPwd);
            Assert.notNull(userModel, "旧密码不正确！");
            Assert.state(ObjectUtil.defaultIfNull(userModel.getPwdErrorCount(), 0) <= 0, "当前账号被锁定中，不能修改密码");

            userService.updatePwd(userName.getId(), newPwd);
            // 如果修改成功，则销毁会话
            session.invalidate();
            return JsonMessage.success("修改密码成功！");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new JsonMessage<>(500, "系统异常：" + e.getMessage());
        }
    }

    /**
     * 查询用户工作空间
     *
     * @return json
     */
    @GetMapping(value = "workspace_list", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<UserBindWorkspaceModel>> workspaceList(@ValidatorItem String userId) {
        List<UserBindWorkspaceModel> workspaceModels = userBindWorkspaceService.listUserWorkspace(userId);
        return JsonMessage.success("", workspaceModels);
    }
}
