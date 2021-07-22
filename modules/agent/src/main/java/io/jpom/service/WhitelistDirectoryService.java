package io.jpom.service;

import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseDataService;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.system.AgentConfigBean;
import io.jpom.util.JsonFileUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 白名单服务
 *
 * @author jiangzeyin
 * @date 2019/2/28
 */
@Service
public class WhitelistDirectoryService extends BaseDataService {

	public AgentWhitelist getWhitelist() {
		try {
			JSONObject jsonObject = getJSONObject(AgentConfigBean.WHITELIST_DIRECTORY);
			if (jsonObject == null) {
				return null;
			}
			return jsonObject.toJavaObject(AgentWhitelist.class);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * 单项添加白名单
	 *
	 * @param item 白名单
	 */
	public void addProjectWhiteList(String item) {
		AgentWhitelist agentWhitelist = getWhitelist();
		if (agentWhitelist == null) {
			agentWhitelist = new AgentWhitelist();
		}
		List<String> project = agentWhitelist.getProject();
		if (project == null) {
			project = new ArrayList<>();
		}
		project.add(item);
		saveWhitelistDirectory(agentWhitelist);
	}

	public boolean isInstalled() {
		AgentWhitelist agentWhitelist = getWhitelist();
		if (agentWhitelist == null) {
			return false;
		}
		List<String> project = agentWhitelist.getProject();
		return project != null && !project.isEmpty();
	}

	private List<String> getNgxDirectory() {
		AgentWhitelist agentWhitelist = getWhitelist();
		if (agentWhitelist == null) {
			return null;
		}
		return agentWhitelist.getNginx();
	}

	public boolean checkProjectDirectory(String path) {
		AgentWhitelist agentWhitelist = getWhitelist();
		if (agentWhitelist == null) {
			return false;
		}
		List<String> list = agentWhitelist.getProject();
		return AgentWhitelist.checkPath(list, path);
	}

	public boolean checkNgxDirectory(String path) {
		List<String> list = getNgxDirectory();
		return AgentWhitelist.checkPath(list, path);
	}

	private List<String> getCertificateDirectory() {
		AgentWhitelist agentWhitelist = getWhitelist();
		if (agentWhitelist == null) {
			return null;
		}
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
