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
package io.jpom.controller;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.LFUCache;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.jwt.JWT;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.ServerConst;
import io.jpom.common.ServerOpenApi;
import io.jpom.common.interceptor.LoginInterceptor;
import io.jpom.common.interceptor.NotLogin;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.common.validator.ValidatorRule;
import io.jpom.func.user.server.UserLoginLogServer;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.model.dto.UserLoginDto;
import io.jpom.model.user.UserModel;
import io.jpom.oauth2.BaseOauth2Config;
import io.jpom.oauth2.Oauth2Factory;
import io.jpom.service.user.UserBindWorkspaceService;
import io.jpom.service.user.UserService;
import io.jpom.system.ServerConfig;
import io.jpom.util.JwtUtil;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 登录控制
 *
 * @author Administrator
 */
@RestController
@Slf4j
public class LoginControl extends BaseServerController {
    /**
     * ip 黑名单
     */
    public static final LFUCache<String, Integer> LFU_CACHE = new LFUCache<>(1000);
    /**
     * 登录需要两步验证
     */
    private static final TimedCache<String, String> MFA_TOKEN = CacheUtil.newTimedCache(TimeUnit.MINUTES.toMillis(10));

    private static final String LOGIN_CODE = "login_code";

    private final UserService userService;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final ServerConfig.UserConfig userConfig;
    private final ServerConfig.WebConfig webConfig;
    private final UserLoginLogServer userLoginLogServer;

    public LoginControl(UserService userService,
                        UserBindWorkspaceService userBindWorkspaceService,
                        ServerConfig serverConfig,
                        UserLoginLogServer userLoginLogServer) {
        this.userService = userService;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.userConfig = serverConfig.getUser();
        this.webConfig = serverConfig.getWeb();
        this.userLoginLogServer = userLoginLogServer;
    }

    /**
     * 验证码
     *
     * @throws IOException IO
     */
    @RequestMapping(value = "randCode.png", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @NotLogin
    public void randCode(HttpServletResponse response) throws IOException {
        if (webConfig.isDisabledCaptcha()) {
            ServletUtil.write(response, JsonMessage.success("验证码已禁用").toString(), MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        int height = 50;
        CircleCaptcha circleCaptcha = new CircleCaptcha(100, height, 4, 8);
        // 设置为默认字体
        circleCaptcha.setFont(new Font(null, Font.PLAIN, (int) (height * 0.75)));
        circleCaptcha.createCode();
        circleCaptcha.write(response.getOutputStream());
        String code = circleCaptcha.getCode();
        setSessionAttribute(LOGIN_CODE, code);
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
     * 当登录的ip 错误次数达到配置的10倍以上锁定当前ip
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
    public JsonMessage<Object> userLogin(@ValidatorItem(value = ValidatorRule.NOT_EMPTY, msg = "请输入登录信息") String loginName,
                                         @ValidatorItem(value = ValidatorRule.NOT_EMPTY, msg = "请输入登录信息") String userPwd,
                                         String code,
                                         HttpServletRequest request) {
        if (this.ipLock()) {
            return new JsonMessage<>(400, "尝试次数太多，请稍后再来");
        }
        synchronized (loginName.intern()) {
            UserModel userModel = userService.getByKey(loginName);
            if (userModel == null) {
                this.ipError();
                return new JsonMessage<>(400, "登录失败，请输入正确的密码和账号,多次失败将锁定账号");
            }
            if (userModel.getStatus() != null && userModel.getStatus() == 0) {
                userLoginLogServer.fail(userModel, 4, false, request);
                return new JsonMessage<>(ServerConst.ACCOUNT_LOCKED, ServerConst.ACCOUNT_LOCKED_TIP);
            }
            if (!webConfig.isDisabledGuide()) {
                // 获取验证码
                String sCode = getSessionAttribute(LOGIN_CODE);
                Assert.state(StrUtil.equalsIgnoreCase(code, sCode), "请输入正确的验证码");
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
                    return new JsonMessage<>(400, "该账户登录失败次数过多，已被锁定" + msg + ",请不要再次尝试");
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
                        return new JsonMessage<>(201, "请输入两步验证码", jsonObject);
                    }
                    UserLoginDto userLoginDto = this.createToken(userModel);
                    userLoginLogServer.success(userModel, 0, false, request);
                    return new JsonMessage<>(200, "登录成功", userLoginDto);
                } else {
                    updateModel = userModel.errorLock(userConfig.getAlwaysLoginError());
                    this.ipError();
                    userLoginLogServer.fail(userModel, 1, false, request);
                    return new JsonMessage<>(501, "登录失败，请输入正确的密码和账号,多次失败将锁定账号");
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
    public JsonMessage<JSONObject> oauth2LoginUrl(HttpServletRequest request, @ValidatorItem String provide) {
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
            log.warn("跳转 oauth2 失败，{} {}", provide, e.getMessage());
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
    public JsonMessage<Object> oauth2Callback(@ValidatorItem String code,
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
            String username = authUser.getUsername();
            UserModel userModel = userService.getByKey(username);
            if (userModel == null) {
                BaseOauth2Config oauth2Config = Oauth2Factory.getConfig(provide);
                if (oauth2Config.autoCreteUser()) {
                    userModel = this.createUser(authUser);
                } else {
                    return new JsonMessage<>(400, username + " 用户不存在请联系管理创建");
                }
            }
            //
            UserModel updateModel = UserModel.unLock(userModel.getId());
            userService.updateById(updateModel);
            //
            UserLoginDto userLoginDto = this.createToken(userModel);
            userLoginLogServer.success(userModel, 6, false, request);
            return JsonMessage.success("登录成功", userLoginDto);
        }
        return new JsonMessage<>(400, "OAuth 2 登录失败,请联系管理员！" + authResponse.getMsg());
    }

    private UserModel createUser(AuthUser authUser) {
        // 创建用户
        UserModel where = new UserModel();
        where.setSystemUser(1);
        List<UserModel> userModels = userService.listByBean(where);
        UserModel first = CollUtil.getFirst(userModels);
        Assert.notNull(first, "没有找到系统管理员");
        UserModel userModel = new UserModel();
        userModel.setName(StrUtil.emptyToDefault(authUser.getNickname(), authUser.getUsername()));
        userModel.setId(authUser.getUsername());
        userModel.setEmail(authUser.getEmail());
        userModel.setSalt(userService.generateSalt());
        String randomPwd = RandomUtil.randomString(UserModel.SALT_LEN);
        String sha1Pwd = SecureUtil.sha1(randomPwd);
        userModel.setPassword(SecureUtil.sha1(sha1Pwd + userModel.getSalt()));
        userModel.setSystemUser(0);
        userModel.setParent(first.getId());
        BaseServerController.resetInfo(first);
        userService.insert(userModel);
        return userModel;
    }

    private UserLoginDto createToken(UserModel userModel) {
        // 判断工作空间
        List<WorkspaceModel> bindWorkspaceModels = userBindWorkspaceService.listUserWorkspaceInfo(userModel);
        Assert.notEmpty(bindWorkspaceModels, "当前账号没有绑定任何工作空间，请联系管理员处理");
        UserLoginDto userLoginDto = userService.getUserJwtId(userModel);
        //					UserLoginDto userLoginDto = new UserLoginDto(JwtUtil.builder(userModel, jwtId), jwtId);
        userLoginDto.setBindWorkspaceModels(bindWorkspaceModels);
        //
        setSessionAttribute(LoginInterceptor.SESSION_NAME, userModel);
        return userLoginDto;
    }

    @GetMapping(value = "mfa_verify", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public JsonMessage<UserLoginDto> mfaVerify(String token, String code, HttpServletRequest request) {
        String userId = MFA_TOKEN.get(token);
        if (StrUtil.isEmpty(userId)) {
            return new JsonMessage<>(201, "登录信息已经过期请重新登录");
        }
        boolean mfaCode = userService.verifyMfaCode(userId, code);
        Assert.state(mfaCode, "验证码不正确,请重新输入");
        UserModel userModel = userService.getByKey(userId);
        //
        UserLoginDto userLoginDto = this.createToken(userModel);
        MFA_TOKEN.remove(token);
        userLoginLogServer.success(userModel, 0, true, request);
        return JsonMessage.success("登录成功", userLoginDto);
    }

    /**
     * 退出登录
     *
     * @return json
     */
    @RequestMapping(value = "logout2", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public JsonMessage<Object> logout(HttpSession session) {
        session.invalidate();
        return JsonMessage.success("退出成功");
    }

    /**
     * 刷新token
     *
     * @return json
     */
    @RequestMapping(value = "renewal", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public JsonMessage<UserLoginDto> renewalToken(HttpServletRequest request) {
        String token = request.getHeader(ServerOpenApi.HTTP_HEAD_AUTHORIZATION);
        if (StrUtil.isEmpty(token)) {
            return new JsonMessage<>(ServerConst.AUTHORIZE_TIME_OUT_CODE, "刷新token失败");
        }
        JWT jwt = JwtUtil.readBody(token);
        if (JwtUtil.expired(jwt, 0)) {
            int renewal = userConfig.getTokenRenewal();
            if (jwt == null || renewal <= 0 || JwtUtil.expired(jwt, TimeUnit.MINUTES.toSeconds(renewal))) {
                return new JsonMessage<>(ServerConst.AUTHORIZE_TIME_OUT_CODE, "刷新token超时");
            }
        }
        UserModel userModel = userService.checkUser(JwtUtil.getId(jwt));
        if (userModel == null) {
            return new JsonMessage<>(ServerConst.AUTHORIZE_TIME_OUT_CODE, "没有对应的用户");
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
    public JsonMessage<JSONObject> demoInfo() {
        String userDemoTip = userConfig.getDemoTip();
        userDemoTip = StringUtil.convertFileStr(userDemoTip, StrUtil.EMPTY);

        JSONObject jsonObject = new JSONObject();
        if (StrUtil.isNotEmpty(userDemoTip) && userService.hasDemoUser()) {
            JSONObject demo = new JSONObject();
            demo.put("msg", userDemoTip);
            demo.put("user", UserModel.DEMO_USER);
            jsonObject.put("demo", demo);
        }
        Collection<String> provides = Oauth2Factory.provides();
        jsonObject.put("oauth2Provides", provides);
        return JsonMessage.success("", jsonObject);
    }
}
