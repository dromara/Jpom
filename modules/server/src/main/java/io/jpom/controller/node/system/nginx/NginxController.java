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
package io.jpom.controller.node.system.nginx;

import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.system.WhitelistDirectoryService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * nginx 管理
 *
 * @author Arno
 */
@Controller
@RequestMapping("/node/system/nginx")
@Feature(cls = ClassFeature.NGINX)
@SystemPermission
public class NginxController extends BaseServerController {

	private final WhitelistDirectoryService whitelistDirectoryService;

	public NginxController(WhitelistDirectoryService whitelistDirectoryService) {
		this.whitelistDirectoryService = whitelistDirectoryService;
	}


	/**
	 * 配置列表
	 *
	 * @return json
	 */
	@RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String list() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_list_data).toString();
	}

	/**
	 * 配置列表
	 *
	 * @return json
	 */
	@RequestMapping(value = "tree.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String tree() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_Tree).toString();
	}


	/**
	 * @return
	 * @author Hotstrip
	 * load Nginx white list data
	 */
	@RequestMapping(value = "white-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String loadWhiteList() {
		List<String> list = whitelistDirectoryService.getNgxDirectory(getNode());
		return JsonMessage.getString(200, "success", list);
	}

	/**
	 * @return
	 * @author Hotstrip
	 * load Nginx config data
	 */
	@RequestMapping(value = "load-config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String loadConfig() {
		JSONObject data = NodeForward.requestData(getNode(), NodeUrl.System_Nginx_item_data, getRequest(), JSONObject.class);
		return JsonMessage.getString(200, "success", data);
	}

	@RequestMapping(value = "updateNgx", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.EDIT)
	public String updateNgx() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_updateNgx).toString();
	}


	@RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.DEL)
	public String delete() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_delete).toString();
	}

	/**
	 * 获取nginx状态
	 *
	 * @return json
	 */
	@RequestMapping(value = "status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String status() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_status).toString();
	}

	/**
	 * 获取nginx配置状态
	 *
	 * @return json
	 */
	@RequestMapping(value = "config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String config() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_config).toString();
	}

	/**
	 * 启动nginx
	 *
	 * @return json
	 */
	@RequestMapping(value = "open", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.EXECUTE)
	public String open() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_open).toString();
	}

	/**
	 * 关闭nginx
	 *
	 * @return json
	 */
	@RequestMapping(value = "close", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.EXECUTE)
	public String close() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_close).toString();
	}


	/**
	 * 修改nginx
	 *
	 * @return json
	 */
	@RequestMapping(value = "updateConf", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.EDIT)
	public String updateConf() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_updateConf).toString();
	}

	@RequestMapping(value = "reload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.EXECUTE)
	public String reload() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_reload).toString();
	}

}
