package io.jpom.controller.system;

import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseJpomController;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.service.WhitelistDirectoryService;
import io.jpom.system.AgentExtConfigBean;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
@RequestMapping(value = "/system")
public class WhitelistDirectoryController extends BaseJpomController {

	@Resource
	private WhitelistDirectoryService whitelistDirectoryService;

	@RequestMapping(value = "whitelistDirectory_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String whiteListDirectoryData() {
		AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();
		return JsonMessage.getString(200, "", agentWhitelist);
	}

	private List<String> parseToList(String value, String errorMsg) {
		if (StrUtil.isEmpty(value)) {
			return null;
		}
		List<String> list = StrSplitter.splitTrim(value, StrUtil.LF, true);
		Assert.notEmpty(list, errorMsg);
		return list;
	}


	@RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String whitelistDirectorySubmit(String project, String certificate, String nginx, String allowEditSuffix) {
		if (StrUtil.isEmpty(project)) {
			return JsonMessage.getString(401, "项目路径白名单不能为空");
		}
		List<String> list = this.parseToList(project, "项目路径白名单不能为空");
		//
		List<String> certificateList = this.parseToList(certificate, "证书路径白名单不能为空");
		List<String> nList = this.parseToList(nginx, "nginx路径白名单不能为空");
		List<String> allowEditSuffixList = this.parseToList(allowEditSuffix, "运行编辑的文件后缀不能为空");
		return save(list, certificateList, nList, allowEditSuffixList).toString();
	}
//
//	private JsonMessage<String> save(String project, List<String> certificate, List<String> nginx, List<String> allowEditSuffixList) {
//
//		return save(list, certificate, nginx);
//	}


	private JsonMessage<String> save(List<String> projects, List<String> certificate, List<String> nginx, List<String> allowEditSuffixList) {
		List<String> projectArray;
		{
			projectArray = AgentWhitelist.covertToArray(projects);
			if (projectArray == null) {
				return new JsonMessage<>(401, "项目路径白名单不能位于Jpom目录下");
			}
			if (projectArray.isEmpty()) {
				return new JsonMessage<>(401, "项目路径白名单不能为空");
			}
			String error = findStartsWith(projectArray, 0);
			if (error != null) {
				return new JsonMessage<>(401, "白名单目录中不能存在包含关系：" + error);
			}
		}
		List<String> certificateArray = null;
		if (certificate != null && !certificate.isEmpty()) {
			certificateArray = AgentWhitelist.covertToArray(certificate);
			if (certificateArray == null) {
				return new JsonMessage<>(401, "证书路径白名单不能位于Jpom目录下");
			}
			if (certificateArray.isEmpty()) {
				return new JsonMessage<>(401, "证书路径白名单不能为空");
			}
			String error = findStartsWith(certificateArray, 0);
			if (error != null) {
				return new JsonMessage<>(401, "证书目录中不能存在包含关系：" + error);
			}
		}
		List<String> nginxArray = null;
		if (nginx != null && !nginx.isEmpty()) {
			nginxArray = AgentWhitelist.covertToArray(nginx);
			if (nginxArray == null) {
				return new JsonMessage<>(401, "nginx路径白名单不能位于Jpom目录下");
			}
			if (nginxArray.isEmpty()) {
				return new JsonMessage<>(401, "nginx路径白名单不能为空");
			}
			String error = findStartsWith(nginxArray, 0);
			if (error != null) {
				return new JsonMessage<>(401, "nginx目录中不能存在包含关系：" + error);
			}
		}
		AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();
		if (agentWhitelist == null) {
			agentWhitelist = new AgentWhitelist();
		}
		agentWhitelist.setProject(projectArray);
		agentWhitelist.setCertificate(certificateArray);
		agentWhitelist.setNginx(nginxArray);
		agentWhitelist.setAllowEditSuffix(allowEditSuffixList);
		whitelistDirectoryService.saveWhitelistDirectory(agentWhitelist);
		return new JsonMessage<>(200, "保存成功");
	}

	/**
	 * 检查白名单包含关系
	 *
	 * @param jsonArray 要检查的对象
	 * @param start     检查的坐标
	 * @return null 正常
	 */
	private String findStartsWith(List<String> jsonArray, int start) {
		if (!AgentExtConfigBean.getInstance().whitelistDirectoryCheckStartsWith) {
			return null;
		}
		String str = jsonArray.get(start);
		int len = jsonArray.size();
		for (int i = 0; i < len; i++) {
			if (i == start) {
				continue;
			}
			String findStr = jsonArray.get(i);
			if (findStr.startsWith(str)) {
				return str;
			}
		}
		if (start < len - 1) {
			return findStartsWith(jsonArray, start + 1);
		}
		return null;
	}
}
