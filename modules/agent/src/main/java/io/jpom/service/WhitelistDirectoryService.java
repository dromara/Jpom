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
package io.jpom.service;

import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.system.AgentConfigBean;
import io.jpom.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 白名单服务
 *
 * @author jiangzeyin
 * @since 2019/2/28
 */
@Service
@Slf4j
public class WhitelistDirectoryService extends BaseDataService {

	/**
	 * 获取白名单信息配置、如何没有配置或者配置错误将返回新对象
	 *
	 * @return AgentWhitelist
	 */
	public AgentWhitelist getWhitelist() {
		try {
			JSONObject jsonObject = getJSONObject(AgentConfigBean.WHITELIST_DIRECTORY);
			if (jsonObject == null) {
				return new AgentWhitelist();
			}
			return jsonObject.toJavaObject(AgentWhitelist.class);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return new AgentWhitelist();
	}

	/**
	 * 单项添加白名单
	 *
	 * @param item 白名单
	 */
	public void addProjectWhiteList(String item) {
		AgentWhitelist agentWhitelist = getWhitelist();
		List<String> project = agentWhitelist.getProject();
		if (project == null) {
			project = new ArrayList<>();
		}
		project.add(item);
		saveWhitelistDirectory(agentWhitelist);
	}

	public boolean isInstalled() {
		AgentWhitelist agentWhitelist = getWhitelist();
		List<String> project = agentWhitelist.getProject();
		return project != null && !project.isEmpty();
	}

	private List<String> getNgxDirectory() {
		AgentWhitelist agentWhitelist = getWhitelist();
		return agentWhitelist.getNginx();
	}

	public boolean checkProjectDirectory(String path) {
		AgentWhitelist agentWhitelist = getWhitelist();

		List<String> list = agentWhitelist.getProject();
		return AgentWhitelist.checkPath(list, path);
	}

	public boolean checkNgxDirectory(String path) {
		List<String> list = getNgxDirectory();
		return AgentWhitelist.checkPath(list, path);
	}

	private List<String> getCertificateDirectory() {
		AgentWhitelist agentWhitelist = getWhitelist();

		return agentWhitelist.getCertificate();
	}

	public boolean checkCertificateDirectory(String path) {
		List<String> list = getCertificateDirectory();
		if (list == null) {
			return false;
		}
		return AgentWhitelist.checkPath(list, path);
	}

	/**
	 * 保存白名单
	 *
	 * @param jsonObject 实体
	 */
	public void saveWhitelistDirectory(AgentWhitelist jsonObject) {
		String path = getDataFilePath(AgentConfigBean.WHITELIST_DIRECTORY);
		JsonFileUtil.saveJson(path, jsonObject.toJson());
	}
}
