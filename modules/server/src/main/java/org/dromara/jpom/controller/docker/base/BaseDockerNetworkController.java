/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.docker.base;

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/2/15
 */
public abstract class BaseDockerNetworkController extends BaseDockerController {

    /**
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<JSONObject>> list(@ValidatorItem String id, String name, String networkId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("name", name);
        parameter.put("id", networkId);
        List<JSONObject> listContainer = (List<JSONObject>) plugin.execute("listNetworks", parameter);
        return JsonMessage.success("", listContainer);
    }
}
