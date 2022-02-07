package io.jpom.controller.docker;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
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
@RequestMapping(value = "/docker/container")
public class DockerContainerController extends BaseServerController {

	private final DockerInfoService dockerInfoService;

	public DockerContainerController(DockerInfoService dockerInfoService) {
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
		parameter.put("containerId", getParameter("containerId"));
		parameter.put("showAll", getParameter("showAll"));
		List<Object> listContainer = (List<Object>) plugin.execute("listContainer", parameter);
		return JsonMessage.getString(200, "", listContainer);
	}

	/**
	 * @return json
	 */
	@GetMapping(value = "remove", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String del(@ValidatorItem String id, String containerId) throws Exception {
		DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id);
		IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> parameter = dockerInfoModel.toParameter();
		parameter.put("containerId", containerId);
		plugin.execute("removeContainer", parameter);
		return JsonMessage.getString(200, "执行成功");
	}

	/**
	 * @return json
	 */
	@GetMapping(value = "start", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String start(@ValidatorItem String id, String containerId) throws Exception {
		DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id);
		IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> parameter = dockerInfoModel.toParameter();
		parameter.put("containerId", containerId);
		plugin.execute("startContainer", parameter);
		return JsonMessage.getString(200, "执行成功");
	}


	/**
	 * @return json
	 */
	@GetMapping(value = "stop", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String stop(@ValidatorItem String id, String containerId) throws Exception {
		DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id);
		IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> parameter = dockerInfoModel.toParameter();
		parameter.put("containerId", containerId);
		plugin.execute("stopContainer", parameter);
		return JsonMessage.getString(200, "执行成功");
	}


	/**
	 * @return json
	 */
	@GetMapping(value = "restart", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String restart(@ValidatorItem String id, String containerId) throws Exception {
		DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id);
		IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> parameter = dockerInfoModel.toParameter();
		parameter.put("containerId", containerId);
		plugin.execute("restartContainer", parameter);
		return JsonMessage.getString(200, "执行成功");
	}
}
