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

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.UrlRedirectUtil;
import io.jpom.common.interceptor.BaseJpomInterceptor;
import io.jpom.common.interceptor.NotLogin;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.service.user.UserService;
import io.jpom.system.ConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.ServerExtConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 首页
 *
 * @author Administrator
 */
@RestController
@RequestMapping(value = "/")
public class IndexControl extends BaseServerController {

	private final UserService userService;

	public IndexControl(UserService userService) {
		this.userService = userService;
	}

	/**
	 * 加载首页
	 */
	@GetMapping(value = {"index", "", "/"}, produces = MediaType.TEXT_HTML_VALUE)
	@NotLogin
	public void index(HttpServletResponse response) {
		InputStream inputStream = ResourceUtil.getStream("classpath:/dist/index.html");
		String html = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
		//<div id="jpomCommonJs"></div>
		String path = ExtConfigBean.getInstance().getPath();
		File file = FileUtil.file(String.format("%s/script/common.js", path));
		String jsCommonContext = StrUtil.EMPTY;
		if (file.exists()) {
			jsCommonContext = FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
		}
		html = StrUtil.replace(html, "<div id=\"jpomCommonJs\"></div>", jsCommonContext);
		// <routerBase>
		String proxyPath = UrlRedirectUtil.getHeaderProxyPath(getRequest(), BaseJpomInterceptor.PROXY_PATH);
		html = StrUtil.replace(html, "<routerBase>", proxyPath);
		// <apiTimeOut>
		int webApiTimeout = ServerExtConfigBean.getInstance().getWebApiTimeout();
		html = StrUtil.replace(html, "<apiTimeout>", TimeUnit.SECONDS.toMillis(webApiTimeout) + "");
		// 修改网页标题
		String title = ReUtil.get("<title>.*?</title>", html, 0);
		if (StrUtil.isNotEmpty(title)) {
			html = StrUtil.replace(html, title, "<title>" + ServerExtConfigBean.getInstance().getName() + "</title>");
		}
		ServletUtil.write(response, html, ContentType.TEXT_HTML.getValue());
	}

	/**
	 * logo 图片
	 */
	@RequestMapping(value = "logo_image", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
	@NotLogin
	public void logoImage(HttpServletResponse response) {
		ServerExtConfigBean instance = ServerExtConfigBean.getInstance();
		String logoFile = instance.getLogoFile();
		if (StrUtil.isNotEmpty(logoFile)) {
			File file = FileUtil.file(logoFile);
			if (FileUtil.isFile(file)) {
				String type = FileTypeUtil.getType(file);
				if (StrUtil.equalsAnyIgnoreCase(type, "jpg", "png", "gif")) {
					ServletUtil.write(response, file);
					return;
				}
			}
		}
		// 默认logo
		InputStream inputStream = ResourceUtil.getStream("classpath:/logo/jpom.png");
		ServletUtil.write(response, inputStream, MediaType.IMAGE_PNG_VALUE);
	}


	/**
	 * @return json
	 * @author Hotstrip
	 * 检查是否需要初始化系统
	 * check if need to init system
	 */
	@NotLogin
	@RequestMapping(value = "check-system", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String checkSystem() {
		JSONObject data = new JSONObject();
		data.put("routerBase", UrlRedirectUtil.getHeaderProxyPath(getRequest(), BaseJpomInterceptor.PROXY_PATH));
		//
		ServerExtConfigBean instance = ServerExtConfigBean.getInstance();
		data.put("name", instance.getName());
		data.put("subTitle", instance.getSubTitle());
		data.put("loginTitle", instance.getLoginTitle());
		if (userService.canUse()) {
			return JsonMessage.getString(200, "success", data);
		}
		return JsonMessage.getString(500, "需要初始化系统", data);
	}

	@RequestMapping(value = "menus_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String menusData() {
		NodeModel nodeModel = tryGetNode();
		UserModel userModel = getUserModel();
		// 菜单
		InputStream inputStream;
		if (nodeModel == null) {
			inputStream = ResourceUtil.getStream("classpath:/menus/index.json");
		} else {
			inputStream = ResourceUtil.getStream("classpath:/menus/node-index.json");
		}

		String json = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
		JSONArray jsonArray = JSONArray.parseArray(json);
		List<Object> collect1 = jsonArray.stream().filter(o -> {
			JSONObject jsonObject = (JSONObject) o;
			if (!testMenus(jsonObject, userModel)) {
				return false;
			}
			JSONArray childs = jsonObject.getJSONArray("childs");
			if (childs != null) {
				List<Object> collect = childs.stream().filter(o1 -> {
					JSONObject jsonObject1 = (JSONObject) o1;
					return testMenus(jsonObject1, userModel);
				}).collect(Collectors.toList());
				if (collect.isEmpty()) {
					return false;
				}
				jsonObject.put("childs", collect);
			}
			return true;
		}).collect(Collectors.toList());
		return JsonMessage.getString(200, "", collect1);
	}

	private boolean testMenus(JSONObject jsonObject, UserModel userModel) {
		String active = jsonObject.getString("active");
		if (StrUtil.isNotEmpty(active)) {
			String active1 = ConfigBean.getInstance().getActive();
			if (!StrUtil.equals(active1, active1)) {
				return false;
			}
		}
		String role = jsonObject.getString("role");
		if (StrUtil.equals(role, UserModel.SYSTEM_ADMIN) && !userModel.isSuperSystemUser()) {
			// 超级理员权限
			return false;
		}
		// 系统管理员权限
		return !StrUtil.equals(role, "system") || userModel.isSystemUser();
	}
}
