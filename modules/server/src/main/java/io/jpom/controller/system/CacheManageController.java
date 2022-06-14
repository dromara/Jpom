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
package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.controller.LoginControl;
import io.jpom.cron.CronUtils;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.PluginFactory;
import io.jpom.socket.ServiceFileTailWatcher;
import io.jpom.system.ConfigBean;
import io.jpom.util.CommandUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存管理
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_CACHE)
@SystemPermission
public class CacheManageController extends BaseServerController {

	/**
	 * get server's cache data
	 * 获取 Server 的缓存数据
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@PostMapping(value = "server-cache", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String serverCache() {
		Map<String, Object> map = new HashMap<>(10);
		String fileSize = FileUtil.readableFileSize(BuildUtil.tempFileCacheSize);
		map.put("cacheFileSize", fileSize);
		map.put("dataSize", FileUtil.readableFileSize(ConfigBean.getInstance().getDataSizeCache()));
		int size = LoginControl.LFU_CACHE.size();
		File oldJarsPath = JpomManifest.getOldJarsPath();
		map.put("oldJarsSize", FileUtil.readableFileSize(FileUtil.size(oldJarsPath)));
		map.put("ipSize", size);
		int oneLineCount = ServiceFileTailWatcher.getOneLineCount();
		map.put("readFileOnLineCount", oneLineCount);


		fileSize = FileUtil.readableFileSize(BuildUtil.buildCacheSize);
		map.put("cacheBuildFileSize", fileSize);

		map.put("taskList", CronUtils.list());
		map.put("pluginSize", PluginFactory.size());

		return JsonMessage.getString(200, "ok", map);
	}

	/**
	 * 获取节点中的缓存
	 *
	 * @return json
	 */
	@RequestMapping(value = "node_cache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
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
	@Feature(method = MethodFeature.DEL)
	public String clearCache(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "类型错误") String type) {
		switch (type) {
			case "serviceCacheFileSize": {
				File tempPath = ConfigBean.getInstance().getTempPath();
				boolean clean = CommandUtil.systemFastDel(tempPath);
				Assert.state(!clean, "清空文件缓存失败");
				break;
			}
			case "serviceIpSize":
				LoginControl.LFU_CACHE.clear();
				break;
			case "serviceOldJarsSize": {
				File oldJarsPath = JpomManifest.getOldJarsPath();
				boolean clean = CommandUtil.systemFastDel(oldJarsPath);
				Assert.state(!clean, "清空旧版本重新包失败");
				break;
			}
			default:
				return NodeForward.request(getNode(), getRequest(), NodeUrl.ClearCache).toString();

		}
		return JsonMessage.getString(200, "清空成功");
	}
}
