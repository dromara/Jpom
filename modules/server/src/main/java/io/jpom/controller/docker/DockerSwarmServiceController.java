package io.jpom.controller.docker;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.plugin.*;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.service.docker.DockerSwarmInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/2/14
 */
@RestController
@Feature(cls = ClassFeature.DOCKER_SWARM)
@RequestMapping(value = "/docker-swarm-service")
@Slf4j
public class DockerSwarmServiceController extends BaseServerController {

	private final DockerInfoService dockerInfoService;

	public DockerSwarmServiceController(DockerInfoService dockerInfoService) {
		this.dockerInfoService = dockerInfoService;
	}

	@PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public JsonMessage<List<JSONObject>> list(
			@ValidatorItem String id,
			String serviceId, String serviceName) throws Exception {
		//
		IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> map = dockerInfoService.getBySwarmPluginMap(id, getRequest());
		map.put("id", serviceId);
		map.put("name", serviceName);
		List<JSONObject> listSwarmNodes = (List<JSONObject>) plugin.execute("listServices", map);
		return new JsonMessage<>(200, "", listSwarmNodes);
	}

	@PostMapping(value = "task-list", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public JsonMessage<List<JSONObject>> taskList(
			@ValidatorItem String id,
			String serviceId, String taskId, String taskName, String taskNode, String taskState) throws Exception {
		//
		IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> map = dockerInfoService.getBySwarmPluginMap(id, getRequest());
		map.put("id", taskId);
		map.put("serviceId", serviceId);
		map.put("name", taskName);
		map.put("node", taskNode);
		map.put("state", taskState);
		List<JSONObject> listSwarmNodes = (List<JSONObject>) plugin.execute("listTasks", map);
		return new JsonMessage<>(200, "", listSwarmNodes);
	}

	@GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public JsonMessage<List<JSONObject>> del(@ValidatorItem String id, @ValidatorItem String serviceId) throws Exception {
		//
		IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> map = dockerInfoService.getBySwarmPluginMap(id, getRequest());
		map.put("serviceId", serviceId);
		plugin.execute("removeService", map);
		return new JsonMessage<>(200, "删除服务成功");
	}

	@PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public JsonMessage<List<JSONObject>> edit(@RequestBody JSONObject jsonObject) throws Exception {
		//
		String id = jsonObject.getString("id");
		IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> map = dockerInfoService.getBySwarmPluginMap(id, getRequest());
		map.putAll(jsonObject);
		plugin.execute("updateService", map);
		return new JsonMessage<>(200, "修改服务成功");
	}
}
