package io.jpom.controller.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.common.ServerOpenApi;
import io.jpom.common.UrlRedirectUtil;
import io.jpom.common.interceptor.BaseJpomInterceptor;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * new trigger controller for build
 *
 * @author Hotstrip
 * @since 2021-08-23
 */
@RestController
@Feature(cls = ClassFeature.BUILD)
public class BuildInfoTriggerController extends BaseServerController {

	@Resource
	private BuildInfoService buildInfoService;

	/**
	 * get a trigger url
	 *
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/build/trigger/url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTriggerUrl(String id) {
		BuildInfoModel item = buildInfoService.getByKey(id);
		if (StrUtil.isEmpty(item.getTriggerToken())) {
			item.setTriggerToken(RandomUtil.randomString(10));
			buildInfoService.update(item);
		}
		String contextPath = UrlRedirectUtil.getHeaderProxyPath(getRequest(), BaseJpomInterceptor.PROXY_PATH);
		String url = ServerOpenApi.BUILD_TRIGGER_BUILD2.
			replace("{id}", item.getId()).
			replace("{token}", item.getTriggerToken());
		String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
		Map<String, String> map = new HashMap<>();
		map.put("triggerBuildUrl", FileUtil.normalize(triggerBuildUrl));
		return JsonMessage.getString(200, "ok", map);
	}


	/**
	 * reset new trigger url
	 *
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/build/trigger/rest", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.EDIT)
	public String triggerRest(String id) {
		BuildInfoModel item = buildInfoService.getByKey(id);
		// new trigger url
		item.setTriggerToken(RandomUtil.randomString(10));
		buildInfoService.update(item);
		return JsonMessage.getString(200, "ok");
	}

}
