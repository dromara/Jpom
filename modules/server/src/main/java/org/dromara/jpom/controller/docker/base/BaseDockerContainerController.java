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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/2/7
 */
@Slf4j
public abstract class BaseDockerContainerController extends BaseDockerController {
    protected final ServerConfig serverConfig;

    protected BaseDockerContainerController(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @GetMapping(value = "info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> info(@ValidatorItem String id) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        JSONObject info = plugin.execute("info", parameter, JSONObject.class);
        return JsonMessage.success("", info);
    }

    @PostMapping(value = "prune", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission
    public IJsonMessage<Object> prune(@ValidatorItem String id, @ValidatorItem String pruneType, String labels, String until, String dangling) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("pruneType", pruneType);
        parameter.put("labels", labels);
        parameter.put("until", until);
        parameter.put("dangling", dangling);
        //
        Long spaceReclaimed = plugin.execute("prune", parameter, Long.class);
        spaceReclaimed = ObjectUtil.defaultIfNull(spaceReclaimed, 0L);
        return JsonMessage.success(I18nMessageUtil.get("i18n.trim_completed_with_recovered_space.0463") + FileUtil.readableFileSize(spaceReclaimed));
    }

    /**
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<JSONObject>> list(@ValidatorItem String id) throws Exception {
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
    @PostMapping(value = "list-compose", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<JSONObject>> listCompose(@ValidatorItem String id) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("name", getParameter("name"));
        parameter.put("containerId", getParameter("containerId"));
        parameter.put("imageId", getParameter("imageId"));
        parameter.put("showAll", getParameter("showAll"));
        List<JSONObject> listContainer = (List<JSONObject>) plugin.execute("listComposeContainer", parameter);
        return JsonMessage.success("", listContainer);
    }

    /**
     * @return json
     */
    @GetMapping(value = "remove", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> del(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        plugin.execute("removeContainer", parameter);
        return JsonMessage.success(I18nMessageUtil.get("i18n.execution_succeeded.f56c"));
    }

    /**
     * @return json
     */
    @GetMapping(value = "start", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<Object> start(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        plugin.execute("startContainer", parameter);
        return JsonMessage.success(I18nMessageUtil.get("i18n.execution_succeeded.f56c"));
    }


    /**
     * @return json
     */
    @GetMapping(value = "stop", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<Object> stop(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        plugin.execute("stopContainer", parameter);
        return JsonMessage.success(I18nMessageUtil.get("i18n.execution_succeeded.f56c"));
    }


    /**
     * @return json
     */
    @GetMapping(value = "restart", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<Object> restart(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        plugin.execute("restartContainer", parameter);
        return JsonMessage.success(I18nMessageUtil.get("i18n.execution_succeeded.f56c"));
    }

    /**
     * @return json
     */
    @GetMapping(value = "stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<Map<String, JSONObject>> stats(@ValidatorItem String id, String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        Map<String, JSONObject> stats = (Map<String, JSONObject>) plugin.execute("stats", parameter);
        return JsonMessage.success(I18nMessageUtil.get("i18n.execution_succeeded.f56c"), stats);
    }

    /**
     * @return json
     */
    @GetMapping(value = "inspect-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<JSONObject> inspectContainer(@ValidatorItem String id, @ValidatorItem String containerId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("containerId", containerId);
        JSONObject results = (JSONObject) plugin.execute("inspectContainer", parameter);
        return JsonMessage.success(I18nMessageUtil.get("i18n.execution_succeeded.f56c"), results);
    }

    /**
     * 修改容器配置
     * NanoCpus：以 10-9 (十億分之一) 個 CPU 為單位的 CPU 配額。 例如，250000000 nanocpus = 0.25 CPU。
     *
     * @return json
     */
    @PostMapping(value = "update-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<JSONObject> updateContainer(@RequestBody JSONObject jsonObject) throws Exception {
        // @ValidatorItem String id, String containerId
        String id = jsonObject.getString("id");
        Assert.hasText(id, I18nMessageUtil.get("i18n.id_cannot_be_empty.8f2c"));
        jsonObject.remove("id");
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.putAll(jsonObject);
        JSONObject results = (JSONObject) plugin.execute("updateContainer", parameter);
        return JsonMessage.success(I18nMessageUtil.get("i18n.execution_succeeded.f56c"), results);
    }

    /**
     * drop old container and create new container
     *
     * @return json
     */
    @PostMapping(value = "rebuild-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<Object> reBuildContainer(@RequestBody JSONObject jsonObject) throws Exception {
        String id = jsonObject.getString("id");
        String containerId = jsonObject.getString("containerId");
        Assert.hasText(id, I18nMessageUtil.get("i18n.id_cannot_be_empty.8f2c"));
        String imageId = jsonObject.getString("imageId");
        Assert.hasText(imageId, I18nMessageUtil.get("i18n.image_cannot_be_empty.1600"));
        String name = jsonObject.getString("name");
        Assert.hasText(name, I18nMessageUtil.get("i18n.container_name_cannot_be_empty.14b1"));

        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);

        // drop old container
        if (StrUtil.isNotEmpty(containerId)) {
            parameter.put("containerId", containerId);
            try {
                plugin.execute("removeContainer", parameter);
            } catch (com.github.dockerjava.api.exception.NotFoundException notFoundException) {
                log.warn(notFoundException.getMessage());
            }
        }

        // create new container
        parameter.putAll(jsonObject);
        plugin.execute("createContainer", parameter);
        return JsonMessage.success(I18nMessageUtil.get("i18n.rebuild_success.5938"));
    }

    /**
     * 下载拉取的日志
     *
     * @param id id
     */
    @GetMapping(value = "download-log")
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                            HttpServletResponse response) {
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-log", id + ".log");
        if (!file.exists()) {
            ServletUtil.write(response, new JsonMessage<>(201, I18nMessageUtil.get("i18n.no_log_file.bacf")).toString(), MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        ServletUtil.write(response, file);
    }
}
