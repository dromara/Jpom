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
package io.jpom.func.user.controller;

import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.func.user.model.UserLoginLogModel;
import io.jpom.func.user.server.UserLoginLogServer;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.jpom.model.PageResultDto;

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
    public JsonMessage<PageResultDto<UserLoginLogModel>> listData(HttpServletRequest request) {
        PageResultDto<UserLoginLogModel> pageResult = userLoginLogServer.listPage(request);
        return JsonMessage.success("", pageResult);
    }
}
