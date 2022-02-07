package io.jpom.controller.docker;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.plugin.*;
import io.jpom.service.docker.DockerInfoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/2/7
 */
@RestController
@Feature(cls = ClassFeature.DOCKER)
@RequestMapping(value = "/docker/volumes")
public class DockerVolumeController extends BaseServerController {

	private final DockerInfoService dockerInfoService;

	public DockerVolumeController(DockerInfoService dockerInfoService) {
		this.dockerInfoService = dockerInfoService;
	}


	/**
	 * @return json
	 */
	@PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String list(@ValidatorItem String id) throws Exception {
		DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id);
		IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> parameter = dockerInfoModel.toParameter();
		parameter.put("name", getParameter("name"));
		parameter.put("dangling", getParameter("dangling"));
		List<JSONObject> listContainer = (List<JSONObject>) plugin.execute("listVolumes", parameter);
		return JsonMessage.getString(200, "", listContainer);
	}


	/**
	 * @return json
	 */
	@GetMapping(value = "remove", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String del(@ValidatorItem String id, String volumeName) throws Exception {
		DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id);
		IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> parameter = dockerInfoModel.toParameter();
		parameter.put("volumeName", volumeName);
		plugin.execute("removeVolume", parameter);
		return JsonMessage.getString(200, "执行成功");
	}
}
