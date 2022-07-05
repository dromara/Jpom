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
package io.jpom.controller.docker;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.docker.DockerInfoService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

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
        DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = dockerInfoModel.toParameter();
        parameter.put("name", getParameter("name"));
        parameter.put("containerId", getParameter("containerId"));
        parameter.put("imageId", getParameter("imageId"));
        parameter.put("showAll", getParameter("showAll"));
        List<JSONObject> listContainer = (List<JSONObject>) plugin.execute("listContainer", parameter);
        return JsonMessage.getString(200, "", listContainer);
    }

    /**
     * @return json
     */
    @GetMapping(value = "remove", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public String del(@ValidatorItem String id, String containerId) throws Exception {
        DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
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
        DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
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
        DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = dockerInfoModel.toParameter();
        parameter.put("containerId", containerId);
        plugin.execute("restartContainer", parameter);
        return JsonMessage.getString(200, "执行成功");
    }

    /**
     * @return json
     */
    @GetMapping(value = "stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public String stats(@ValidatorItem String id, String containerId) throws Exception {
        DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = dockerInfoModel.toParameter();
        parameter.put("containerId", containerId);
        Map<String, JSONObject> stats = (Map<String, JSONObject>) plugin.execute("stats", parameter);
        return JsonMessage.getString(200, "执行成功", stats);
    }

    /**
     * @return json
     */
    @GetMapping(value = "inspect-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public String inspectContainer(@ValidatorItem String id, String containerId) throws Exception {
        DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = dockerInfoModel.toParameter();
        parameter.put("containerId", containerId);
        JSONObject results = (JSONObject) plugin.execute("inspectContainer", parameter);
        return JsonMessage.getString(200, "执行成功", results);
    }

    /**
     * 修改容器配置
     *
     * @return json
     */
    @PostMapping(value = "update-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public String updateContainer(@RequestBody JSONObject jsonObject) throws Exception {
        // @ValidatorItem String id, String containerId
        String id = jsonObject.getString("id");
        Assert.hasText(id, "id 不能为空");
        jsonObject.remove("id");

        DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = dockerInfoModel.toParameter();
        parameter.putAll(jsonObject);
        JSONObject results = (JSONObject) plugin.execute("updateContainer", parameter);
        return JsonMessage.getString(200, "执行成功", results);
    }
}
