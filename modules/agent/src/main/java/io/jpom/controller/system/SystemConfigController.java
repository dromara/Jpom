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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.BaseAgentController;
import io.jpom.common.Const;
import io.jpom.common.JpomManifest;
import io.jpom.system.ExtConfigBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 系统配置
 *
 * @author bwcx_jzy
 * @since 2019/08/08
 */
@RestController
@RequestMapping(value = "system")
@Slf4j
public class SystemConfigController extends BaseAgentController {

	@RequestMapping(value = "getConfig.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String config() throws IOException {
		Resource resource = ExtConfigBean.getResource();
		String content = IoUtil.read(resource.getInputStream(), CharsetUtil.CHARSET_UTF_8);
		JSONObject json = new JSONObject();
		json.put("content", content);
		json.put("file", FileUtil.getAbsolutePath(resource.getFile()));
		return JsonMessage.getString(200, "ok", json);
	}

	@RequestMapping(value = "save_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String saveConfig(String content, String restart) {
		if (StrUtil.isEmpty(content)) {
			return JsonMessage.getString(405, "内容不能为空");
		}
		try {
			YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
			// @author hjk 前端编辑器允许使用tab键，并设定为2个空格，再转换为yml时要把tab键换成2个空格
			ByteArrayResource resource = new ByteArrayResource(content.replace("\t", "  ").getBytes(StandardCharsets.UTF_8));
			yamlPropertySourceLoader.load("test", resource);
		} catch (Exception e) {
			log.warn("内容格式错误，请检查修正", e);
			return JsonMessage.getString(500, "内容格式错误，请检查修正:" + e.getMessage());
		}
		if (JpomManifest.getInstance().isDebug()) {
			return JsonMessage.getString(405, "调试模式下不支持在线修改,请到resources目录下的bin目录修改extConfig.yml");
		}
		File resourceFile = ExtConfigBean.getResourceFile();
		FileUtil.writeString(content, resourceFile, CharsetUtil.CHARSET_UTF_8);

		if (Convert.toBool(restart, false)) {
			// 重启
			ThreadUtil.execute(() -> {
				ThreadUtil.sleep(2000);
				JpomApplication.restart();
			});
			return JsonMessage.getString(200, Const.UPGRADE_MSG);
		}
		return JsonMessage.getString(200, "修改成功");
	}
}
