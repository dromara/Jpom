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

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.PermissionInterceptor;
import io.jpom.model.data.MailAccountModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.monitor.EmailUtil;
import io.jpom.service.system.SystemParametersServer;
import io.jpom.service.user.UserBindWorkspaceService;
import io.jpom.service.user.UserService;
import io.jpom.util.TwoFactorAuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author bwcx_jzy
 * @since 2019/8/10
 */
@RestController
@RequestMapping(value = "/user")
@Slf4j
public class UserBasicInfoController extends BaseServerController {

	private static final TimedCache<String, Integer> CACHE = new TimedCache<>(TimeUnit.MINUTES.toMillis(30));

	private final SystemParametersServer systemParametersServer;
	private final UserBindWorkspaceService userBindWorkspaceService;
	private final UserService userService;

	public UserBasicInfoController(SystemParametersServer systemParametersServer,
								   UserBindWorkspaceService userBindWorkspaceService,
								   UserService userService) {
		this.systemParametersServer = systemParametersServer;
		this.userBindWorkspaceService = userBindWorkspaceService;
		this.userService = userService;
	}


	/**
	 * get user basic info
	 * 获取管理员基本信息接口
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@RequestMapping(value = "user-basic-info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getUserBasicInfo() {
		UserModel userModel = getUser();
		userModel = userService.getByKey(userModel.getId(), false);
		// return basic info
		Map<String, Object> map = new HashMap<>(10);
		map.put("id", userModel.getId());
		map.put("name", userModel.getName());
		map.put("systemUser", userModel.isSystemUser());
		map.put("email", userModel.getEmail());
		map.put("dingDing", userModel.getDingDing());
		map.put("workWx", userModel.getWorkWx());
		map.put("md5Token", userModel.getPassword());
		boolean bindMfa = userService.hasBindMfa(userModel.getId());
		map.put("bindMfa", bindMfa);
		return JsonMessage.getString(200, "success", map);
	}

	@RequestMapping(value = "save_basicInfo.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String saveBasicInfo(@ValidatorItem(value = ValidatorRule.EMAIL, msg = "邮箱格式不正确") String email,
								String dingDing, String workWx, String code) {
		UserModel user = getUser();
		UserModel userModel = userService.getByKey(user.getId());
		// 判断是否一样
		if (!StrUtil.equals(email, userModel.getEmail())) {
			Integer cacheCode = CACHE.get(email);
			if (cacheCode == null || !Objects.equals(cacheCode.toString(), code)) {
				return JsonMessage.getString(405, "请输入正确验证码");
			}
		}
		UserModel updateModel = new UserModel(user.getId());
		updateModel.setEmail(email);
		//
		if (StrUtil.isNotEmpty(dingDing) && !Validator.isUrl(dingDing)) {
			Validator.validateMatchRegex(RegexPool.URL_HTTP, dingDing, "请输入正确钉钉地址");
		}
		updateModel.setDingDing(dingDing);
		if (StrUtil.isNotEmpty(workWx)) {
			Validator.validateMatchRegex(RegexPool.URL_HTTP, workWx, "请输入正确企业微信地址");
		}
		updateModel.setWorkWx(workWx);
		userService.update(updateModel);
		return JsonMessage.getString(200, "修改成功");
	}

	/**
	 * 发送邮箱验证
	 *
	 * @param email 邮箱
	 * @return msg
	 */
	@RequestMapping(value = "sendCode.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String sendCode(@ValidatorItem(value = ValidatorRule.EMAIL, msg = "邮箱格式不正确") String email) {
		MailAccountModel config = systemParametersServer.getConfig(MailAccountModel.ID, MailAccountModel.class);
		Assert.notNull(config, "管理员还没有配置系统邮箱,请联系管理配置发件信息");
		int randomInt = RandomUtil.randomInt(1000, 9999);
		try {
			EmailUtil.send(email, "Jpom 验证码", "验证码是：" + randomInt);
		} catch (Exception e) {
			log.error("发送失败", e);
			return JsonMessage.getString(500, "发送邮件失败：" + e.getMessage());
		}
		CACHE.put(email, randomInt);
		return JsonMessage.getString(200, "发送成功");
	}

	/**
	 * 查询用户自己的工作空间
	 *
	 * @return msg
	 */
	@GetMapping(value = "my_workspace", produces = MediaType.APPLICATION_JSON_VALUE)
	public String myWorkspace() {
		UserModel user = getUser();
		List<WorkspaceModel> models = userBindWorkspaceService.listUserWorkspaceInfo(user);
		Assert.notEmpty(models, "当前账号没有绑定任何工作空间，请联系管理员处理");
		return JsonMessage.getString(200, "", models);
	}

	/**
	 * 关闭自己到 mfa 相关信息
	 *
	 * @return json
	 */
	@GetMapping(value = "close_mfa", produces = MediaType.APPLICATION_JSON_VALUE)
	public String closeMfa(@ValidatorItem String code) {
		UserModel user = getUser();
		boolean mfaCode = userService.verifyMfaCode(user.getId(), code);
		Assert.state(mfaCode, "验证码不正确");
		UserModel userModel = new UserModel(user.getId());
		userModel.setTwoFactorAuthKey(StrUtil.EMPTY);
		userService.update(userModel);
		return JsonMessage.getString(200, "关闭成功");
	}

	@GetMapping(value = "generate_mfa", produces = MediaType.APPLICATION_JSON_VALUE)
	public String generateMfa() {
		UserModel user = getUser();
		JSONObject jsonObject = new JSONObject();
		String tfaKey = TwoFactorAuthUtils.generateTFAKey();
		jsonObject.put("mfaKey", tfaKey);
		jsonObject.put("url", TwoFactorAuthUtils.generateOtpAuthUrl(user.getId(), tfaKey));
		return JsonMessage.getString(200, "", jsonObject);
	}

	/**
	 * 绑定 mfa
	 *
	 * @param mfa     mfa key
	 * @param twoCode 验证码
	 * @return json
	 */
	@GetMapping(value = "bind_mfa", produces = MediaType.APPLICATION_JSON_VALUE)
	public String bindMfa(String mfa, String twoCode) {
		//
		UserModel user = getUser();
		boolean bindMfa = userService.hasBindMfa(user.getId());
		Assert.state(!bindMfa, "当前账号已经绑定 mfa 啦");
		// demo
		Assert.state(!user.isDemoUser(), PermissionInterceptor.DEMO_TIP);
		//
		boolean tfaCode = TwoFactorAuthUtils.validateTFACode(mfa, twoCode);
		Assert.state(tfaCode, " mfa 验证码不正确");
		userService.bindMfa(user.getId(), mfa);
		return JsonMessage.getString(200, "绑定成功");
	}
}
