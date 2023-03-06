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

import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.JsonMessage;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.docker.DockerSwarmInfoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/2/13
 */
public abstract class BaseDockerSwarmInfoController extends BaseDockerController {

    @PostMapping(value = "node-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<JSONObject>> nodeList(
        @ValidatorItem String id,
        String nodeId, String nodeName, String nodeRole) throws Exception {
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> map = this.toDockerParameter(id);
        map.put("id", nodeId);
        map.put("name", nodeName);
        map.put("role", nodeRole);
        List<JSONObject> listSwarmNodes = (List<JSONObject>) plugin.execute("listSwarmNodes", map);
        return new JsonMessage<>(200, "", listSwarmNodes);
    }


    /**
     * 修改节点信息
     *
     * @param id 集群ID
     * @return json
     */
    @PostMapping(value = "update", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<String> update(@ValidatorItem String id,
                                      @ValidatorItem String nodeId,
                                      @ValidatorItem String availability,
                                      @ValidatorItem String role) throws Exception {
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> map = this.toDockerParameter(id);
        map.put("nodeId", nodeId);
        map.put("availability", availability);
        map.put("role", role);
        plugin.execute("updateSwarmNode", map);
        return new JsonMessage<>(200, "修改成功");
    }
}
