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

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
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
import org.dromara.jpom.service.docker.DockerSwarmInfoService;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.util.FileUtils;
import org.dromara.jpom.util.LogRecorder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletResponse;
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
                log.debug(I18nMessageUtil.get("i18n.async_resource_expired.2ddc"), key, userIds);
                IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
                Map<String, Object> map = MapUtil.of("uuid", key);
                plugin.execute("closeAsyncResource", map);
                //
                for (String userId : userIds) {
                    File file = FileUtil.file(serverConfig.getUserTempPath(userId), "docker-swarm-log", key + ".log");
                    FileUtil.del(file);
                }
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.close_resource_failure.dc66"), e);
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
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.delete_service_success.4d73"));
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
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.modify_service_success.bd75"));
    }


    /**
     * @return json
     */
    @GetMapping(value = "start-log", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> pullImage(@ValidatorItem String id,
                                          @ValidatorItem String type,
                                          @ValidatorItem String dataId,
                                          Integer tail,
                                          String since,
                                          Boolean timestamps) {
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = this.toDockerParameter(id);
        parameter.put(StrUtil.equalsIgnoreCase(type, "service") ? "serviceId" : "taskId", dataId);
        //
        String uuid = IdUtil.fastSimpleUUID();
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-swarm-log", uuid + ".log");
        LogRecorder logRecorder = LogRecorder.builder().file(file).build();

        logRecorder.system("start pull {}", dataId);
        logRecorder.info("");
        Consumer<String> logConsumer = logRecorder::append;
        parameter.put("charset", CharsetUtil.CHARSET_UTF_8);
        parameter.put("consumer", logConsumer);
        //
        tail = ObjectUtil.defaultIfNull(tail, 50);
        tail = Math.max(tail, 1);
        parameter.put("tail", tail);
        //parameter.put("since", since);
        parameter.put("timestamps", timestamps);
        // 操作id
        parameter.put("uuid", uuid);
        I18nThreadUtil.execute(() -> {
            try {
                plugin.execute(StrUtil.equalsIgnoreCase(type, "service") ? "logService" : "logTask", parameter);
                logRecorder.system("pull end");
            } catch (Exception e) {
                logRecorder.error(I18nMessageUtil.get("i18n.pull_log_exception.cc3e"), e);
            } finally {
                IoUtil.close(logRecorder);
            }
        });
        // 添加到缓存中
        LOG_CACHE.put(uuid, CollUtil.newHashSet(getUser().getId()));
        return JsonMessage.success(I18nMessageUtil.get("i18n.start_pulling.57ab"), uuid);
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
    public IJsonMessage<JSONObject> getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                                              @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "i18n.line_number_error.c65d") int line) {
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-swarm-log", id + ".log");
        if (!file.exists()) {
            return new JsonMessage<>(201, I18nMessageUtil.get("i18n.no_log_file.bacf"));
        }
        JSONObject data = FileUtils.readLogFile(file, line);
        // 更新缓存，避免超时被清空
        synchronized (BaseDockerSwarmServiceController.class) {
            Set<String> userIds = ObjectUtil.defaultIfNull(LOG_CACHE.get(id), new HashSet<>());
            userIds.add(getUser().getId());
            LOG_CACHE.put(id, userIds);
        }
        return JsonMessage.success("", data);
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
        File file = FileUtil.file(serverConfig.getUserTempPath(), "docker-swarm-log", id + ".log");
        if (!file.exists()) {
            ServletUtil.write(response, new JsonMessage<>(201, I18nMessageUtil.get("i18n.no_log_file.bacf")).toString(), MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        ServletUtil.write(response, file);
    }
}
