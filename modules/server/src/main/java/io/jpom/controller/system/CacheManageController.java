/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.controller.LoginControl;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.socket.ServiceFileTailWatcher;
import io.jpom.system.ConfigBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存管理
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Controller
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM)
public class CacheManageController extends BaseServerController {

	/**
	 * @return
	 * @author Hotstrip
	 * get server's cache data
	 * 获取 Server 的缓存数据
	 */
	@RequestMapping(value = "server-cache", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String serverCache() {
		Map<String, Object> map = new HashMap<>();
		String fileSize = FileUtil.readableFileSize(BuildUtil.tempFileCacheSize);
		map.put("cacheFileSize", fileSize);

		int size = LoginControl.LFU_CACHE.size();
		map.put("ipSize", size);
		int oneLineCount = ServiceFileTailWatcher.getOneLineCount();
		map.put("readFileOnLineCount", oneLineCount);


		fileSize = FileUtil.readableFileSize(BuildUtil.buildCacheSize);
		map.put("cacheBuildFileSize", fileSize);

		return JsonMessage.getString(200, "ok", map);
	}

	/**
	 * 获取节点中的缓存
	 *
	 * @return json
	 */
	@RequestMapping(value = "node_cache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.CACHE)
	public String nodeCache() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Cache).toString();
	}

	/**
	 * 清空缓存
	 *
	 * @param type 类型
	 * @return json
	 */
	@RequestMapping(value = "clearCache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.ClearCache)
	@Feature(method = MethodFeature.CACHE)
	public String clearCache(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "类型错误") String type) {
		switch (type) {
			case "serviceCacheFileSize":
				boolean clean = FileUtil.clean(ConfigBean.getInstance().getTempPath());
				if (!clean) {
					return JsonMessage.getString(504, "清空文件缓存失败");
				}
				break;
			case "serviceIpSize":
				LoginControl.LFU_CACHE.clear();
				break;
			default:
				return NodeForward.request(getNode(), getRequest(), NodeUrl.ClearCache).toString();

		}
		return JsonMessage.getString(200, "清空成功");
	}
}
