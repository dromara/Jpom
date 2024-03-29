/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.user.server;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.Header;
import org.dromara.jpom.func.user.model.UserLoginLogModel;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;

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
