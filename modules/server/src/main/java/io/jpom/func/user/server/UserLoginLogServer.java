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
    public void success(UserModel userModel, boolean useMfa, HttpServletRequest request) {
        this.success(userModel, 0, useMfa, request);
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
