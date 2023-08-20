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

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.docker.DockerSwarmInfoService;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.util.FileUtils;
import org.dromara.jpom.util.LogRecorder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author bwcx_jzy
 * @since 2022/2/14
 */
@Slf4j
public abstract class BaseDockerSwarmServiceController extends BaseDockerController {
    private static final TimedCache<String, Set<String>> LOG_CACHE = new TimedCache<>(30 * 1000);
    protected final ServerConfig serverConfig;

    public BaseDockerSwarmServiceController(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        // 30 秒检查一次
        LOG_CACHE.schedulePrune(30 * 1000);
        // 监控过期
        LOG_CACHE.setListener((key, userIds) -> {
            try {
                log.debug("异步资源过期,需要主动关闭,{} {}", key, userIds);
                IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
                Map<String, Object> map = MapUtil.of("uuid", key);
                plugin.execute("closeAsyncResource", map);
                //
                for (String userId : userIds) {
                    File file = FileUtil.file(serverConfig.getUserTempPath(userId), "docker-swarm-log", key + ".log");
                    FileUtil.del(file);
                }
            } catch (Exception e) {
                log.error("关闭资源失败", e);
            }
        });
    }

    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<JSONObject>> list(
        @ValidatorItem String id,
        String serviceId, String serviceName) throws Exception {
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> map = this.toDockerParameter(id);
        map.put("id", serviceId);
        map.put("name", serviceName);
        List<JSONObject> listSwarmNodes = (List<JSONObject>) plugin.execute("listServices", map);
        return new JsonMessage<>(200, "", listSwarmNodes);
    }

    @PostMapping(value = "task-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<JSONObject>> taskList(
        @ValidatorItem String id,
        String serviceId, String taskId, String taskName, String taskNode, String taskState) throws Exception {
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> map = this.toDockerParameter(id);
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
    public IJsonMessage<List<JSONObject>> del(@ValidatorItem String id, @ValidatorItem String serviceId) throws Exception {
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> map = this.toDockerParameter(id);
        map.put("serviceId", serviceId);
        plugin.execute("removeService", map);
        return new JsonMessage<>(200, "删除服务成功");
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<List<JSONObject>> edit(@RequestBody JSONObject jsonObject) throws Exception {
        //
        String id = jsonObject.getString("id");
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> map = this.toDockerParameter(id);
        map.putAll(jsonObject);
        plugin.execute("updateService", map);
        return new JsonMessage<>(200, "修改服务成功");
    }


    /**
     * @return json
     */
    @GetMapping(value = "start-log", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> pullImage(@ValidatorItem String id, @ValidatorItem String type, @ValidatorItem String dataId) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put(StrUtil.equalsIgnoreCase(type, "service") ? "serviceId" : "taskId", dataId);
        //
        String uuid = IdUtil.fastSimpleUUID();
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-swarm-log", uuid + ".log");
        LogRecorder logRecorder = LogRecorder.builder().file(file).build();

        logRecorder.system("start pull {}", dataId);
        Consumer<String> logConsumer = logRecorder::append;
        parameter.put("charset", CharsetUtil.CHARSET_UTF_8);
        parameter.put("consumer", logConsumer);
        parameter.put("tail", 50);
        // 操作id
        parameter.put("uuid", uuid);
        ThreadUtil.execute(() -> {
            try {
                plugin.execute(StrUtil.equalsIgnoreCase(type, "service") ? "logService" : "logTask", parameter);
            } catch (Exception e) {
                logRecorder.error("拉取日志异常", e);
            }
            logRecorder.system("pull end");
        });
        // 添加到缓存中
        LOG_CACHE.put(uuid, CollUtil.newHashSet(getUser().getId()));
        return JsonMessage.success("开始拉取", uuid);
    }

    /**
     * 获取拉取的日志
     *
     * @param id   id
     * @param line 需要获取的行号
     * @return json
     */
    @GetMapping(value = "pull-log", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                                              @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "line") int line) {
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-swarm-log", id + ".log");
        if (!file.exists()) {
            return new JsonMessage<>(201, "还没有日志文件");
        }
        JSONObject data = FileUtils.readLogFile(file, line);
        // 更新缓存，避免超时被清空
        Set<String> userIds = ObjectUtil.defaultIfNull(LOG_CACHE.get(id), new HashSet<>());
        userIds.add(getUser().getId());
        LOG_CACHE.put(id, userIds);
        return JsonMessage.success("ok", data);
    }
}
