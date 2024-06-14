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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
    public IJsonMessage<String> updatePwd(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.password_cannot_be_empty.89b5") String oldPwd,
                                          @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.password_cannot_be_empty.89b5") String newPwd,
                                          HttpSession session) {
        Assert.state(!StrUtil.equals(oldPwd, newPwd), I18nMessageUtil.get("i18n.old_and_new_passwords_match.55b4"));
        UserModel userName = getUser();
        Assert.state(!userName.isDemoUser(), I18nMessageUtil.get("i18n.demo_account_password_change_not_supported.91f4"));

        UserModel userModel = userService.simpleLogin(userName.getId(), oldPwd);
        Assert.notNull(userModel, I18nMessageUtil.get("i18n.old_password_incorrect.9cf6"));
        Assert.state(ObjectUtil.defaultIfNull(userModel.getPwdErrorCount(), 0) <= 0, I18nMessageUtil.get("i18n.account_locked_cannot_change_password.d6ab"));

        userService.updatePwd(userName.getId(), newPwd);
        // 如果修改成功，则销毁会话
        session.invalidate();
        return JsonMessage.success(I18nMessageUtil.get("i18n.password_change_success.8013"));
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
