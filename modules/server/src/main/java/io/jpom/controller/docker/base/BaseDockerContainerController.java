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
package io.jpom.controller.docker.base;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.JsonMessage;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.docker.DockerInfoService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/2/7
 */
public abstract class BaseDockerContainerController extends BaseDockerController {


    @GetMapping(value = "info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<JSONObject> info(@ValidatorItem String id) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        JSONObject info = plugin.execute("info", parameter, JSONObject.class);
        return JsonMessage.success("", info);
    }

    @PostMapping(value = "prune", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission
    public JsonMessage<Object> prune(@ValidatorItem String id, @ValidatorItem String pruneType, String labels, String until, String dangling) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("pruneType", pruneType);
        parameter.put("labels", labels);
        parameter.put("until", until);
        parameter.put("dangling", dangling);
        //
        Long spaceReclaimed = plugin.execute("prune", parameter, Long.class);
        spaceReclaimed = ObjectUtil.defaultIfNull(spaceReclaimed, 0L);
        return JsonMessage.success("修剪完成,总回收空间：" + FileUtil.readableFileSize(spaceReclaimed));
    }

    /**
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<JSONObject>> list(@ValidatorItem String id) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("name", getParameter("name"));
        parameter.put("containerId", getParameter("containerId"));
        parameter.put("imageId", getParameter("imageId"));
        parameter.put("showAll", getParameter("showAll"));
        List<JSONObject> listContainer = (List<JSONObject>) plugin.execute("listContainer", parameter);
        return JsonMessage.success("", listContainer);
    }

    /**
     * @return json
     */
    @GetMapping(value = "remove", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<Object> del(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        plugin.execute("removeContainer", parameter);
        return JsonMessage.success("执行成功");
    }

    /**
     * @return json
     */
    @GetMapping(value = "start", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<Object> start(@ValidatorItem String id, String containerId) throws Exception {

        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        plugin.execute("startContainer", parameter);
        return JsonMessage.success("执行成功");
    }


    /**
     * @return json
     */
    @GetMapping(value = "stop", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<Object> stop(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        plugin.execute("stopContainer", parameter);
        return JsonMessage.success("执行成功");
    }


    /**
     * @return json
     */
    @GetMapping(value = "restart", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<Object> restart(@ValidatorItem String id, String containerId) throws Exception {

        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        plugin.execute("restartContainer", parameter);
        return JsonMessage.success("执行成功");
    }

    /**
     * @return json
     */
    @GetMapping(value = "stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<Map<String, JSONObject>> stats(@ValidatorItem String id, String containerId) throws Exception {

        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        Map<String, JSONObject> stats = (Map<String, JSONObject>) plugin.execute("stats", parameter);
        return JsonMessage.success("执行成功", stats);
    }

    /**
     * @return json
     */
    @GetMapping(value = "inspect-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<JSONObject> inspectContainer(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        JSONObject results = (JSONObject) plugin.execute("inspectContainer", parameter);
        return JsonMessage.success("执行成功", results);
    }

    /**
     * 修改容器配置
     *
     * @return json
     */
    @PostMapping(value = "update-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<JSONObject> updateContainer(@RequestBody JSONObject jsonObject) throws Exception {
        // @ValidatorItem String id, String containerId
        String id = jsonObject.getString("id");
        Assert.hasText(id, "id 不能为空");
        jsonObject.remove("id");
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.putAll(jsonObject);
        JSONObject results = (JSONObject) plugin.execute("updateContainer", parameter);
        return JsonMessage.success("执行成功", results);
    }
}
