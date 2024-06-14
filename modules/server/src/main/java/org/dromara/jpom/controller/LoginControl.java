/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.LFUCache;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.jwt.JWT;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.interceptor.LoginInterceptor;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.configuration.UserConfig;
import org.dromara.jpom.configuration.WebConfig;
import org.dromara.jpom.controller.user.UserWorkspaceModel;
import org.dromara.jpom.func.user.server.UserLoginLogServer;
import org.dromara.jpom.model.dto.UserLoginDto;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.oauth2.BaseOauth2Config;
import org.dromara.jpom.oauth2.Oauth2Factory;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.user.UserService;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.util.JwtUtil;
import org.dromara.jpom.util.StringUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 登录控制
 *
 * @author bwcx_jzy
 */
@RestController
@Slf4j
public class LoginControl extends BaseServerController implements InitializingBean {
    /**
     * ip 禁止缓存
     */
    public static final LFUCache<String, Integer> LFU_CACHE = new LFUCache<>(1000);
    /**
     * 登录需要两步验证
     */
    private static final TimedCache<String, String> MFA_TOKEN = CacheUtil.newTimedCache(TimeUnit.MINUTES.toMillis(10));

    private static final String LOGIN_CODE = "login_code";

    private final UserService userService;
    private final UserConfig userConfig;
    private final WebConfig webConfig;
    private final UserLoginLogServer userLoginLogServer;

    public LoginControl(UserService userService,
                        ServerConfig serverConfig,
                        UserLoginLogServer userLoginLogServer) {
        this.userService = userService;
        this.userConfig = serverConfig.getUser();
        this.webConfig = serverConfig.getWeb();
        this.userLoginLogServer = userLoginLogServer;
    }

    /**
     * 验证码
     */
    @RequestMapping(value = "rand-code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public IJsonMessage<String> randCode(String theme) {
        if (webConfig.isDisabledCaptcha()) {
            return new JsonMessage<>(400, I18nMessageUtil.get("i18n.verification_code_disabled.349b"));
        }
        CircleCaptcha captcha = this.createCaptcha(theme);
        setSessionAttribute(LOGIN_CODE, captcha.getCode());
        String base64Data = captcha.getImageBase64Data();
        return new JsonMessage<>(200, "", base64Data);
    }

    private CircleCaptcha createCaptcha(String theme) {
        int height = 50;
        CircleCaptcha circleCaptcha = new CircleCaptcha(100, height, 4, 8);
        if (StrUtil.equalsIgnoreCase(theme, "dark")) {
            circleCaptcha.setBackground(Color.darkGray);
        } else {
            circleCaptcha.setBackground(Color.white);
        }
        // 设置为默认字体
        circleCaptcha.setFont(new Font(null, Font.PLAIN, (int) (height * 0.75)));
        circleCaptcha.createCode();
        return circleCaptcha;
    }

    /**
     * 记录 ip 登录失败
     */
    private synchronized void ipError() {
        String ip = getIp();
        int count = ObjectUtil.defaultIfNull(LFU_CACHE.get(ip), 0) + 1;
        LFU_CACHE.put(ip, count, userConfig.getIpErrorLockTime().toMillis());
    }

    private synchronized void ipSuccess() {
        String ip = getIp();
        LFU_CACHE.remove(ip);
    }

    /**
     * 当登录的ip 错误次数达到配置以上锁定当前ip
     *
     * @return true
     */
    private boolean ipLock() {
        if (userConfig.getAlwaysIpLoginError() <= 0) {
            return false;
        }
        String ip = getIp();
        Integer count = LFU_CACHE.get(ip);
        if (count == null) {
            count = 0;
        }
        return count > userConfig.getAlwaysIpLoginError();
    }


    /**
     * 登录接口
     *
     * @param loginName 登录名
     * @param userPwd   登录密码
     * @param code      验证码
     * @return json
     */
    @PostMapping(value = "userLogin", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    @Feature(cls = ClassFeature.USER, method = MethodFeature.EXECUTE, logResponse = false)
    public IJsonMessage<Object> userLogin(@ValidatorItem(value = ValidatorRule.NOT_EMPTY, msg = "i18n.login_info_required.973b") String loginName,
                                          @ValidatorItem(value = ValidatorRule.NOT_EMPTY, msg = "i18n.login_info_required.973b") String userPwd,
                                          String code,
                                          HttpServletRequest request) {
        if (this.ipLock()) {
            return new JsonMessage<>(400, I18nMessageUtil.get("i18n.too_many_attempts.d88d"));
        }
        synchronized (loginName.intern()) {
            UserModel userModel = userService.getByKey(loginName);
            if (userModel == null) {
                this.ipError();
                return new JsonMessage<>(400, I18nMessageUtil.get("i18n.login_failed_please_enter_correct_password_and_account.03b2"));
            }
            if (userModel.getStatus() != null && userModel.getStatus() == 0) {
                userLoginLogServer.fail(userModel, 4, false, request);
                return new JsonMessage<>(ServerConst.ACCOUNT_LOCKED, ServerConst.ACCOUNT_LOCKED_TIP.get());
            }
            if (!webConfig.isDisabledCaptcha()) {
                // 获取验证码
                String sCode = getSessionAttribute(LOGIN_CODE);
                Assert.state(StrUtil.equalsIgnoreCase(code, sCode), I18nMessageUtil.get("i18n.correct_verification_code_required.ff0d"));
                removeSessionAttribute(LOGIN_CODE);
            }
            UserModel updateModel = null;
            try {
                long lockTime = userModel.overLockTime(userConfig.getAlwaysLoginError());
                if (lockTime > 0) {
                    String msg = DateUtil.formatBetween(lockTime * 1000, BetweenFormatter.Level.SECOND);
                    updateModel = userModel.errorLock(userConfig.getAlwaysLoginError());
                    this.ipError();
                    userLoginLogServer.fail(userModel, 2, false, request);
                    return new JsonMessage<>(400, StrUtil.format(I18nMessageUtil.get("i18n.account_login_failed_too_many_times_locked.23b2"), msg));
                }
                // 验证
                if (userService.simpleLogin(loginName, userPwd) != null) {
                    updateModel = UserModel.unLock(loginName);
                    this.ipSuccess();
                    // 判断是否开启 两步验证
                    boolean bindMfa = userService.hasBindMfa(loginName);
                    if (bindMfa) {
                        //
                        JSONObject jsonObject = new JSONObject();
                        String uuid = IdUtil.fastSimpleUUID();
                        MFA_TOKEN.put(uuid, loginName);
                        jsonObject.put("tempToken", uuid);
                        userLoginLogServer.success(userModel, 5, true, request);
                        return new JsonMessage<>(201, I18nMessageUtil.get("i18n.two_step_verification_code_required.7e86"), jsonObject);
                    }
                    UserLoginDto userLoginDto = this.createToken(userModel);
                    userLoginLogServer.success(userModel, 0, false, request);
                    return new JsonMessage<>(200, I18nMessageUtil.get("i18n.login_success.71fa"), userLoginDto);
                } else {
                    updateModel = userModel.errorLock(userConfig.getAlwaysLoginError());
                    this.ipError();
                    userLoginLogServer.fail(userModel, 1, false, request);
                    return new JsonMessage<>(501, I18nMessageUtil.get("i18n.login_failed_please_enter_correct_password_and_account.03b2"));
                }
            } finally {
                if (updateModel != null) {
                    userService.updateById(updateModel);
                }
            }
        }
    }

    /**
     * 跳转到认证中心登录
     *
     * @param request 请求对象
     * @return json
     */
    @GetMapping(value = "oauth2-url", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public IJsonMessage<JSONObject> oauth2LoginUrl(HttpServletRequest request, @ValidatorItem String provide) {
        AuthRequest authRequest = Oauth2Factory.get(provide);
        String authorize = authRequest.authorize(null);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("toUrl", authorize);
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 跳转到认证中心登录
     *
     * @param provide 平台
     */
    @GetMapping(value = "oauth2-render-{provide}")
    @NotLogin
    public void oauth2UrlRender(HttpServletResponse response, @PathVariable String provide) {
        try {
            AuthRequest authRequest = Oauth2Factory.get(provide);
            response.sendRedirect(authRequest.authorize(null));
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.oauth2_redirect_failed.6dcd"), provide, e.getMessage());
            ServletUtil.write(response, JsonMessage.getString(500, e.getMessage()), ContentType.JSON.toString());
        }
    }

    /**
     * oauth2 登录并获取token
     *
     * @param code    授权码
     * @param state   state
     * @param request 请求对象
     * @return json
     */
    @PostMapping(value = "oauth2/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    @Feature(cls = ClassFeature.USER, method = MethodFeature.EXECUTE, logResponse = false)
    public IJsonMessage<UserLoginDto> oauth2Callback(@ValidatorItem String code,
                                                     @ValidatorItem String provide,
                                                     String state,
                                                     HttpServletRequest request) {
        AuthRequest authRequest = Oauth2Factory.get(provide);
        AuthCallback authCallback = new AuthCallback();
        authCallback.setCode(code);
        authCallback.setState(state);

        AuthResponse<?> authResponse = authRequest.login(authCallback);
        if (authResponse.ok()) {
            AuthUser authUser = (AuthUser) authResponse.getData();
            String username = this.parseUsername(authUser);
            UserModel userModel = userService.getByKey(username);
            if (userModel == null) {
                BaseOauth2Config oauth2Config = Oauth2Factory.getConfig(provide);
                if (oauth2Config.autoCreteUser()) {
                    userModel = this.createUser(username, authUser, provide, oauth2Config.getPermissionGroup());
                } else {
                    return new JsonMessage<>(400, username + I18nMessageUtil.get("i18n.user_does_not_exist.8363"));
                }
            }
            if (userModel.getStatus() != null && userModel.getStatus() == 0) {
                userLoginLogServer.fail(userModel, 4, false, request);
                return new JsonMessage<>(ServerConst.ACCOUNT_LOCKED, ServerConst.ACCOUNT_LOCKED_TIP.get());
            }
            //
            UserModel updateModel = UserModel.unLock(userModel.getId());
            userService.updateById(updateModel);
            //
            UserLoginDto userLoginDto = this.createToken(userModel);
            userLoginLogServer.success(userModel, 6, false, request);
            return JsonMessage.success(I18nMessageUtil.get("i18n.login_success.71fa"), userLoginDto);
        }
        return new JsonMessage<>(400, I18nMessageUtil.get("i18n.login_failure_O_auth2_message.3e91") + authResponse.getMsg());
    }

    /**
     * 解析用户名
     *
     * @param authUser 平台账号信息
     * @return 用户名
     */
    private String parseUsername(AuthUser authUser) {
        String username = authUser.getUsername();
        String checkId = StrUtil.replace(username, "-", "_");
        if (Validator.isGeneral(checkId, UserModel.USER_NAME_MIN_LEN, Const.ID_MAX_LEN)) {
            return username;
        }
        if (StrUtil.isNotEmpty(authUser.getEmail())) {
            return authUser.getEmail();
        }
        String uuid = authUser.getUuid();
        if (Validator.isGeneral(uuid, UserModel.USER_NAME_MIN_LEN, Const.ID_MAX_LEN)) {
            return uuid;
        }
        throw new IllegalStateException(I18nMessageUtil.get("i18n.oauth2_login_failure.3841"));
    }

    /**
     * oauth2 创建用户账号
     *
     * @param username        用户名
     * @param authUser        平台信息
     * @param source          来源平台
     * @param permissionGroup 权限组
     * @return 用户
     */
    private UserModel createUser(String username, AuthUser authUser, String source, String permissionGroup) {
        // 创建用户
        UserModel where = new UserModel();
        where.setSystemUser(1);
        List<UserModel> userModels = userService.listByBean(where);
        UserModel first = CollUtil.getFirst(userModels);
        Assert.notNull(first, I18nMessageUtil.get("i18n.system_admin_not_found.6f6c"));
        UserModel userModel = new UserModel();
        userModel.setName(StrUtil.emptyToDefault(authUser.getNickname(), authUser.getUsername()));
        userModel.setId(username);
        userModel.setEmail(authUser.getEmail());
        userModel.setSalt(userService.generateSalt());
        String randomPwd = RandomUtil.randomString(UserModel.SALT_LEN);
        String sha1Pwd = SecureUtil.sha1(randomPwd);
        userModel.setPassword(SecureUtil.sha1(sha1Pwd + userModel.getSalt()));
        userModel.setSystemUser(0);
        userModel.setParent(first.getId());
        userModel.setSource(source);
        // 绑定权限组
        List<String> permissionGroupList = StrUtil.split(permissionGroup, StrUtil.AT, true, true);
        if (CollUtil.isNotEmpty(permissionGroupList)) {
            userModel.setPermissionGroup(CollUtil.join(permissionGroupList, StrUtil.AT, StrUtil.AT, StrUtil.AT));
        }
        BaseServerController.resetInfo(first);
        userService.insert(userModel);
        return userModel;
    }

    private UserLoginDto createToken(UserModel userModel) {
        // 判断工作空间
        List<UserWorkspaceModel> bindWorkspaceModels = userService.myWorkspace(userModel);
        Assert.notEmpty(bindWorkspaceModels, I18nMessageUtil.get("i18n.account_not_bound_to_any_workspace.fd61"));
        UserLoginDto userLoginDto = userService.getUserJwtId(userModel);
        // UserLoginDto userLoginDto = new UserLoginDto(JwtUtil.builder(userModel, jwtId), jwtId);
        userLoginDto.setBindWorkspaceModels(bindWorkspaceModels);
        //
        setSessionAttribute(LoginInterceptor.SESSION_NAME, userModel);
        return userLoginDto;
    }

    @GetMapping(value = "mfa_verify", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public IJsonMessage<UserLoginDto> mfaVerify(String token, String code, HttpServletRequest request) {
        String userId = MFA_TOKEN.get(token);
        if (StrUtil.isEmpty(userId)) {
            return new JsonMessage<>(201, I18nMessageUtil.get("i18n.login_info_expired_please_re_login.fbbc"));
        }
        boolean mfaCode = userService.verifyMfaCode(userId, code);
        Assert.state(mfaCode, I18nMessageUtil.get("i18n.verification_code_incorrect_retry.d88d"));
        UserModel userModel = userService.getByKey(userId);
        //
        UserLoginDto userLoginDto = this.createToken(userModel);
        MFA_TOKEN.remove(token);
        userLoginLogServer.success(userModel, 0, true, request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.login_success.71fa"), userLoginDto);
    }

    /**
     * 退出登录
     *
     * @return json
     */
    @RequestMapping(value = "logout2", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public IJsonMessage<Object> logout(HttpSession session) {
        session.invalidate();
        return JsonMessage.success(I18nMessageUtil.get("i18n.exit_successful.8150"));
    }

    /**
     * 刷新token
     *
     * @return json
     */
    @RequestMapping(value = "renewal", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public IJsonMessage<UserLoginDto> renewalToken(HttpServletRequest request) {
        String token = request.getHeader(ServerOpenApi.HTTP_HEAD_AUTHORIZATION);
        if (StrUtil.isEmpty(token)) {
            return new JsonMessage<>(ServerConst.AUTHORIZE_TIME_OUT_CODE, I18nMessageUtil.get("i18n.refresh_token_failure.de7f"));
        }
        JWT jwt = JwtUtil.readBody(token);
        if (JwtUtil.expired(jwt, 0)) {
            int renewal = userConfig.getTokenRenewal();
            if (jwt == null || renewal <= 0 || JwtUtil.expired(jwt, TimeUnit.MINUTES.toSeconds(renewal))) {
                return new JsonMessage<>(ServerConst.AUTHORIZE_TIME_OUT_CODE, I18nMessageUtil.get("i18n.refresh_token_timeout.3291"));
            }
        }
        UserModel userModel = userService.checkUser(JwtUtil.getId(jwt));
        if (userModel == null) {
            return new JsonMessage<>(ServerConst.AUTHORIZE_TIME_OUT_CODE, I18nMessageUtil.get("i18n.no_user.3b69"));
        }
        UserLoginDto userLoginDto = userService.getUserJwtId(userModel);
        userLoginLogServer.success(userModel, 3, false, request);
        return JsonMessage.success("", userLoginDto);
    }

    /**
     * 获取 demo 账号的信息
     */
    @GetMapping(value = "login-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public IJsonMessage<JSONObject> demoInfo() {
        String userDemoTip = userConfig.getDemoTip();
        userDemoTip = StringUtil.convertFileStr(userDemoTip, StrUtil.EMPTY);

        JSONObject jsonObject = new JSONObject();
        if (StrUtil.isNotEmpty(userDemoTip) && userService.hasDemoUser()) {
            JSONObject demo = new JSONObject();
            demo.put("msg", userDemoTip);
            demo.put("user", UserModel.DEMO_USER);
            jsonObject.put("demo", demo);
        }
        jsonObject.put("disabledCaptcha", webConfig.isDisabledCaptcha());
        Collection<String> provides = Oauth2Factory.provides();
        jsonObject.put("oauth2Provides", provides);
        return JsonMessage.success("", jsonObject);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            this.createCaptcha(null);
            log.debug(I18nMessageUtil.get("i18n.server_captcha_available.5570"));
        } catch (Throwable e) {
            log.warn(I18nMessageUtil.get("i18n.server_captcha_generation_exception.54d0"), e);
            webConfig.setDisabledCaptcha(true);
        }
    }
}
