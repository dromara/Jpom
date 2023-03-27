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
package io.jpom.func.user.server;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.Header;
import io.jpom.func.user.model.UserLoginLogModel;
import io.jpom.model.user.UserModel;
import io.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;
import top.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2023/3/9
 */
@Service
public class UserLoginLogServer extends BaseDbService<UserLoginLogModel> {


    /**
     * 查询指定用户的登录日志
     *
     * @param request 请求信息
     * @param userId  用户id
     * @return page
     */
    public PageResultDto<UserLoginLogModel> listPageByUserId(HttpServletRequest request, String userId) {
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        paramMap.put("modifyUser", userId);
        return super.listPage(paramMap);
    }

    /**
     * 记录登录日志
     *
     * @param userModel 用户
     * @param success   是否成功
     * @param useMfa    是否使用 mfa
     * @param request   请求信息
     */
    public void log(UserModel userModel, boolean success, boolean useMfa, int operateCode, HttpServletRequest request) {
        UserLoginLogModel userLoginLogModel = new UserLoginLogModel();
        userLoginLogModel.setModifyUser(userModel.getId());
        userLoginLogModel.setUsername(userModel.getName());
        userLoginLogModel.setSuccess(success);
        userLoginLogModel.setUseMfa(useMfa);
        userLoginLogModel.setOperateCode(operateCode);
        userLoginLogModel.setIp(ServletUtil.getClientIP(request));
        userLoginLogModel.setUserAgent(ServletUtil.getHeader(request, Header.USER_AGENT.getValue(), CharsetUtil.CHARSET_UTF_8));
        this.insert(userLoginLogModel);
    }

    /**
     * 记录登录日志
     *
     * @param userModel 用户
     * @param useMfa    是否使用 mfa
     * @param request   请求信息
     */
    public void success(UserModel userModel, int code, boolean useMfa, HttpServletRequest request) {
        this.log(userModel, true, useMfa, code, request);
    }

    /**
     * 记录登录日志
     *
     * @param userModel 用户
     * @param useMfa    是否使用 mfa
     * @param code      错误码
     * @param request   请求信息
     */
    public void fail(UserModel userModel, int code, boolean useMfa, HttpServletRequest request) {
        this.log(userModel, false, useMfa, code, request);
    }
}
