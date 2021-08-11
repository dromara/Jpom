package io.jpom.controller.outgiving;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.model.data.ServerWhitelist;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.service.system.ServerWhitelistServer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点白名单
 *
 * @author jiangzeyin
 * @date 2019/4/22
 */
@Controller
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
public class OutGivingWhitelistController extends BaseServerController {
	@Resource
	private ServerWhitelistServer serverWhitelistServer;


	/**
	 * get whiteList data
	 * 白名单数据接口
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@RequestMapping(value = "white-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@SystemPermission
	@ResponseBody
	public String whiteList() {
		ServerWhitelist serverWhitelist = serverWhitelistServer.getWhitelist();
		Field[] fields = ReflectUtil.getFields(ServerWhitelist.class);
		Map<String, String> map = new HashMap<>(8);
		for (Field field : fields) {
			Collection<String> fieldValue = (Collection<String>) ReflectUtil.getFieldValue(serverWhitelist, field);
			map.put(field.getName(), AgentWhitelist.convertToLine(fieldValue));
		}
		return JsonMessage.getString(200, "ok", map);
	}

	/**
	 * 保存节点白名单
	 *
	 * @param outGiving 数据
	 * @return json
	 */
	@RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.SaveOutgivingWhitelist)
	@SystemPermission
	public String whitelistDirectorySubmit(String outGiving, String allowRemoteDownloadHost) {
		List<String> list = AgentWhitelist.parseToList(outGiving, true, "项目路径白名单不能为空");
		list = AgentWhitelist.covertToArray(list, "项目路径白名单不能位于Jpom目录下");

		ServerWhitelist serverWhitelist = serverWhitelistServer.getWhitelist();
		serverWhitelist.setOutGiving(list);
		//
		List<String> allowRemoteDownloadHostList = AgentWhitelist.parseToList(allowRemoteDownloadHost, "运行远程下载的 host 不能配置为空");
		//
		if (CollUtil.isNotEmpty(allowRemoteDownloadHostList)) {
			for (String s : allowRemoteDownloadHostList) {
				Assert.state(ReUtil.isMatch(RegexPool.URL_HTTP, s), "配置的远程地址不规范,请重新填写：" + s);
			}
		}
		serverWhitelist.setAllowRemoteDownloadHost(allowRemoteDownloadHostList == null ? null : CollUtil.newHashSet(allowRemoteDownloadHostList));
		serverWhitelistServer.saveWhitelistDirectory(serverWhitelist);

		String resultData = AgentWhitelist.convertToLine(list);
		return JsonMessage.getString(200, "保存成功", resultData);
	}
}
