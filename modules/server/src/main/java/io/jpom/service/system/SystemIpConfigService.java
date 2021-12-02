///*
// * The MIT License (MIT)
// *
// * Copyright (c) 2019 码之科技工作室
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of
// * this software and associated documentation files (the "Software"), to deal in
// * the Software without restriction, including without limitation the rights to
// * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// * the Software, and to permit persons to whom the Software is furnished to do so,
// * subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// */
//package io.jpom.service.system;
//
//import com.alibaba.fastjson.JSONObject;
//import io.jpom.common.BaseDataService;
//import io.jpom.model.data.SystemIpConfigModel;
//import io.jpom.system.ServerConfigBean;
//import io.jpom.util.JsonFileUtil;
//import org.springframework.stereotype.Service;
//
///**
// * 系统ip 白名单
// *
// * @author bwcx_jzy
// */
//@Service
//@Deprecated
//public class SystemIpConfigService extends BaseDataService {
//
//	/**
//	 * 获取配置
//	 *
//	 * @return config
//	 */
//	public SystemIpConfigModel getConfig() {
//		JSONObject config = getJSONObject(ServerConfigBean.IP_CONFIG);
//		if (config == null) {
//			return null;
//		}
//		return config.toJavaObject(SystemIpConfigModel.class);
//	}
//
//	/**
//	 * 保存配置
//	 *
//	 * @param configModel config
//	 */
//	public void save(SystemIpConfigModel configModel) {
//		String path = getDataFilePath(ServerConfigBean.IP_CONFIG);
//		JsonFileUtil.saveJson(path, configModel.toJson());
//	}
//
//	public String filePath() {
//		return getDataFilePath(ServerConfigBean.IP_CONFIG);
//	}
//}
