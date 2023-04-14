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
package org.dromara.jpom.controller.docker.base;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.JsonMessage;
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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author bwcx_jzy
 * @since 2022/2/7
 */
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
    public JsonMessage<List<JSONObject>> list(@ValidatorItem String id) throws Exception {

        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("name", getParameter("name"));
        parameter.put("showAll", getParameter("showAll"));
        parameter.put("dangling", getParameter("dangling"));
        List<JSONObject> listContainer = (List<JSONObject>) plugin.execute("listImages", parameter);
        return JsonMessage.success("", listContainer);
    }


    /**
     * @return json
     */
    @GetMapping(value = "remove", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<Object> del(@ValidatorItem String id, String imageId) throws Exception {

        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("imageId", imageId);
        plugin.execute("removeImage", parameter);
        return JsonMessage.success("执行成功");
    }

    /**
     * @return json
     */
    @GetMapping(value = "inspect", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<JSONObject> inspect(@ValidatorItem String id, String imageId) throws Exception {

        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("imageId", imageId);
        JSONObject inspectImage = (JSONObject) plugin.execute("inspectImage", parameter);
        return JsonMessage.success("", inspectImage);
    }

    /**
     * @return json
     */
    @GetMapping(value = "pull-image", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<String> pullImage(@ValidatorItem String id, String repository) {

        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put("repository", repository);
        //
        String uuid = IdUtil.fastSimpleUUID();
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-log", uuid + ".log");
        LogRecorder logRecorder = LogRecorder.builder().file(file).build();
        logRecorder.system("start pull {}", repository);
        Consumer<String> logConsumer = logRecorder::info;
        parameter.put("logConsumer", logConsumer);
        ThreadUtil.execute(() -> {
            try {
                plugin.execute("pullImage", parameter);
            } catch (Exception e) {
                logRecorder.error("拉取异常", e);
            }
            logRecorder.system("pull end");
        });
        return JsonMessage.success("开始拉取", uuid);
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
    public JsonMessage<JSONObject> getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                                             @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "line") int line) {
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-log", id + ".log");
        if (!file.exists()) {
            return new JsonMessage<>(201, "还没有日志文件");
        }
        JSONObject data = FileUtils.readLogFile(file, line);
        return JsonMessage.success("ok", data);
    }

    /**
     * @return json
     */
    @PostMapping(value = "create-container", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<Object> createContainer(@RequestBody JSONObject jsonObject) throws Exception {
        String id = jsonObject.getString("id");
        Assert.hasText(id, "id 不能为空");
        Assert.hasText(jsonObject.getString("imageId"), "镜像不能为空");
        Assert.hasText(jsonObject.getString("name"), "容器名称不能为空");

        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.putAll(jsonObject);
        plugin.execute("createContainer", parameter);
        return JsonMessage.success("创建成功");
    }
}
