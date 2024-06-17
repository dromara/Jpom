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
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.i18n.I18nThreadUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.util.FileUtils;
import org.dromara.jpom.util.LogRecorder;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author bwcx_jzy
 * @since 2022/2/7
 */
@Slf4j
public abstract class BaseDockerImagesController extends BaseDockerController {

    protected final ServerConfig serverConfig;

    public BaseDockerImagesController(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
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
        parameter.put("showAll", getParameter("showAll"));
        parameter.put("dangling", getParameter("dangling"));
        parameter.put("workspaceId", getWorkspaceId());
        List<JSONObject> listContainer = (List<JSONObject>) plugin.execute("listImages", parameter);
        return JsonMessage.success("", listContainer);
    }


    /**
     * @return json
     */
    @GetMapping(value = "remove", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> del(@ValidatorItem String id, String imageId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("imageId", imageId);
        parameter.put("workspaceId", getWorkspaceId());
        plugin.execute("removeImage", parameter);
        return JsonMessage.success(I18nMessageUtil.get("i18n.execution_succeeded.f56c"));
    }


    /**
     * @return json
     */
    @GetMapping(value = "batchRemove", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> batchRemove(@ValidatorItem String id, String[] imagesIds) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("imagesIds", imagesIds);
        parameter.put("workspaceId", getWorkspaceId());
        plugin.execute("batchRemove", parameter);
        return JsonMessage.success(I18nMessageUtil.get("i18n.execution_succeeded.f56c"));
    }

    /**
     * @return json
     */
    @GetMapping(value = "inspect", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> inspect(@ValidatorItem String id, String imageId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("imageId", imageId);
        parameter.put("workspaceId", getWorkspaceId());
        JSONObject inspectImage = (JSONObject) plugin.execute("inspectImage", parameter);
        return JsonMessage.success("", inspectImage);
    }

    /**
     * @return json
     */
    @GetMapping(value = "pull-image", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> pullImage(@ValidatorItem String id, String repository) {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("repository", repository);
        parameter.put("workspaceId", getWorkspaceId());
        //
        String uuid = IdUtil.fastSimpleUUID();
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-log", uuid + ".log");
        LogRecorder logRecorder = LogRecorder.builder().file(file).build();
        logRecorder.system("start pull {}", repository);
        Consumer<String> logConsumer = logRecorder::info;
        parameter.put("logConsumer", logConsumer);
        I18nThreadUtil.execute(() -> {
            try {
                plugin.execute("pullImage", parameter);
                logRecorder.system("pull end");
            } catch (Exception e) {
                logRecorder.error(I18nMessageUtil.get("i18n.pull_exception.b38d"), e);
            } finally {
                IoUtil.close(logRecorder);
            }

        });
        return JsonMessage.success(I18nMessageUtil.get("i18n.start_pulling.57ab"), uuid);
    }

    /**
     *
     */
    @GetMapping(value = "save-image", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public void saveImage(@ValidatorItem String id, String imageId, HttpServletResponse response) {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("imageId", imageId);
        //
        try {
            Tuple saveImage = (Tuple) plugin.execute("saveImage", parameter);
            if (saveImage == null) {
                ServletUtil.write(response, new JsonMessage<>(405, I18nMessageUtil.get("i18n.image_not_exist.ee17")).toString(), MediaType.APPLICATION_JSON_VALUE);
                return;
            }
            InputStream inputStream = saveImage.get(0);
            String name = saveImage.get(1);
            ServletUtil.write(response, inputStream, MediaType.APPLICATION_OCTET_STREAM_VALUE, name);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.export_image_exception.cb1c"), e);
            ServletUtil.write(response, new JsonMessage<>(500, I18nMessageUtil.get("i18n.export_image_exception.cb1c")).toString(), MediaType.APPLICATION_JSON_VALUE);
        }
    }

    /**
     *
     */
    @PostMapping(value = "load-image", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> loadImage(@ValidatorItem String id,
                                          MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        boolean expression = StrUtil.equalsIgnoreCase(extName, "tar");
        Assert.state(expression, I18nMessageUtil.get("i18n.only_tar_files_supported.dcc4"));
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("stream", file.getInputStream());
        //
        plugin.execute("loadImage", parameter);
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.import_success.b6d1"));
    }

    /**
     * 获取拉取的日志
     *
     * @param id   id
     * @param line 需要获取的行号
     * @return json
     */
    @GetMapping(value = "pull-image-log", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                                              @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "i18n.line_number_error.c65d") int line) {
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-log", id + ".log");
        if (!file.exists()) {
            return new JsonMessage<>(201, I18nMessageUtil.get("i18n.no_log_file.bacf"));
        }
        JSONObject data = FileUtils.readLogFile(file, line);
        return JsonMessage.success("", data);
    }

    /**
     * @return json
     */
    @PostMapping(value = "create-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<Object> createContainer(@RequestBody JSONObject jsonObject) throws Exception {
        String id = jsonObject.getString("id");
        Assert.hasText(id, I18nMessageUtil.get("i18n.id_cannot_be_empty.8f2c"));
        String imageId = jsonObject.getString("imageId");
        Assert.hasText(imageId, I18nMessageUtil.get("i18n.image_cannot_be_empty.1600"));
        String name = jsonObject.getString("name");
        Assert.hasText(name, I18nMessageUtil.get("i18n.container_name_cannot_be_empty.14b1"));

        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.putAll(jsonObject);
        parameter.put("workspaceId", getWorkspaceId());
        plugin.execute("createContainer", parameter);
        return JsonMessage.success(I18nMessageUtil.get("i18n.create_success.04a6"));
    }
}
