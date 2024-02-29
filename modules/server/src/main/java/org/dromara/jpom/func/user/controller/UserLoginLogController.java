/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.user.controller;

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.func.user.model.UserLoginLogModel;
import org.dromara.jpom.func.user.server.UserLoginLogServer;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author bwcx_jzy
 * @since 2023/3/9
 */
@RestController
@RequestMapping(value = "/user/login-log")
@Feature(cls = ClassFeature.USER_LOGIN_LOG)
@SystemPermission
public class UserLoginLogController extends BaseServerController {

    private final UserLoginLogServer userLoginLogServer;

    public UserLoginLogController(UserLoginLogServer userLoginLogServer) {
        this.userLoginLogServer = userLoginLogServer;
    }

    /**
     * 登录日志列表
     *
     * @return json
     */
    @RequestMapping(value = "list-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<UserLoginLogModel>> listData(HttpServletRequest request) {
        PageResultDto<UserLoginLogModel> pageResult = userLoginLogServer.listPage(request);
        return JsonMessage.success("", pageResult);
    }
}
