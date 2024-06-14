/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.openapi.controller;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.event.IAsyncLoad;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.build.BuildExecuteService;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.build.ResultDirFileAction;
import org.dromara.jpom.common.BaseJpomController;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.enums.BuildStatus;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.dromara.jpom.system.JpomRuntimeException;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2019/9/4
 */
@RestController
@NotLogin
@Slf4j
public class BuildTriggerApiController extends BaseJpomController implements IAsyncLoad, Runnable {

    private final BuildInfoService buildInfoService;
    private final BuildExecuteService buildExecuteService;
    private final TriggerTokenLogServer triggerTokenLogServer;
    /**
     * 等待执行构建的队列
     */
    private final Map<String, Queue<BuildCache>> waitQueue = new SafeConcurrentHashMap<>();

    public BuildTriggerApiController(BuildInfoService buildInfoService,
                                     BuildExecuteService buildExecuteService,
                                     TriggerTokenLogServer triggerTokenLogServer) {
        this.buildInfoService = buildInfoService;
        this.buildExecuteService = buildExecuteService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }


    private Object[] buildParametersEnv(HttpServletRequest request, String body) {
        String contentType = request.getContentType();
        Object[] parametersEnv = new Object[4];
        parametersEnv[0] = "triggerContentType";
        parametersEnv[1] = contentType;
        parametersEnv[2] = "triggerBodyData";
        if (ContentType.isDefault(contentType)) {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            parametersEnv[3] = JSONObject.toJSONString(paramMap);
        } else {
            // github issues 48
            parametersEnv[3] = body == null ? ServletUtil.getBody(request) : body;
        }
        return parametersEnv;
    }

    /**
     * 构建触发器
     *
     * @param id    构建ID
     * @param token 构建的token
     * @param delay 延迟时间（单位秒）
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.BUILD_TRIGGER_BUILD2, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Integer> trigger2(@PathVariable String id, @PathVariable String token,
                                          HttpServletRequest request,
                                          String delay,
                                          String buildRemark, String useQueue) {
        BuildInfoModel item = buildInfoService.getByKey(id);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        UserModel userModel = this.triggerTokenLogServer.getUserByToken(token, buildInfoService.typeName());
        //
        Assert.notNull(userModel, I18nMessageUtil.get("i18n.trigger_token_error_or_expired_with_code.393b"));

        Assert.state(StrUtil.equals(token, item.getTriggerToken()), I18nMessageUtil.get("i18n.trigger_token_error_or_expired.8976"));
        // 构建外部参数
        Object[] parametersEnv = this.buildParametersEnv(request, null);
        Integer delay1 = Convert.toInt(delay, 0);
        if (Convert.toBool(useQueue, false)) {
            // 提交到队列暂存
            BuildCache buildCache = new BuildCache();
            buildCache.setId(id);
            buildCache.setUserModel(userModel);
            buildCache.setDelay(delay1);
            buildCache.setBuildRemark(buildRemark);
            buildCache.setParametersEnv(parametersEnv);
            //
            Queue<BuildCache> buildCaches = waitQueue.computeIfAbsent(id, s -> new ConcurrentLinkedDeque<>());
            buildCaches.add(buildCache);
            return JsonMessage.success(I18nMessageUtil.get("i18n.submit_task_queue_success.5f5b") + buildCaches.size());
        }

        BaseServerController.resetInfo(userModel);
        return buildExecuteService.start(id, userModel, delay1, 1, buildRemark, parametersEnv);
    }

    /**
     * 构建触发器
     * <p>
     * 参数 <code>[
     * {
     * "id":"1",
     * "token":"a",
     * "delay":"0"
     * }
     * ]</code>
     * <p>
     * 响应 <code>[
     * {
     * "id":"1",
     * "token":"a",
     * "delay":"0",
     * "msg":"开始构建",
     * "data":1
     * }
     * ]</code>
     *
     * @return json
     */
    @PostMapping(value = ServerOpenApi.BUILD_TRIGGER_BUILD_BATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<Object>> triggerBatch(HttpServletRequest request) {
        String body = ServletUtil.getBody(request);
        if (StrUtil.isEmpty(body)) {
            return new JsonMessage<>(405, I18nMessageUtil.get("i18n.please_pass_body_parameter.4e5c"));
        }
        try {
            // 构建外部参数
            Object[] parametersEnv = this.buildParametersEnv(request, body);
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<Object> collect = jsonArray.stream().peek(o -> {
                JSONObject jsonObject = (JSONObject) o;
                String id = jsonObject.getString("id");
                String token = jsonObject.getString("token");
                Integer delay = jsonObject.getInteger("delay");
                String buildRemark = jsonObject.getString("buildRemark");
                String useQueue = jsonObject.getString("useQueue");
                BuildInfoModel item = buildInfoService.getByKey(id);
                if (item == null) {
                    String value = I18nMessageUtil.get("i18n.no_data_found.4ffb");
                    jsonObject.put("msg", value);
                    return;
                }
                UserModel userModel = triggerTokenLogServer.getUserByToken(token, buildInfoService.typeName());
                if (userModel == null) {
                    String value = I18nMessageUtil.get("i18n.user_not_exist_trigger_invalid.f375");
                    jsonObject.put("msg", value);
                    return;
                }
                //
                if (!StrUtil.equals(token, item.getTriggerToken())) {
                    String value = I18nMessageUtil.get("i18n.trigger_token_error_or_expired.8976");
                    jsonObject.put("msg", value);
                    return;
                }
                // 更新字段
                String updateItemErrorMsg = this.updateItem(jsonObject);
                if (updateItemErrorMsg != null) {
                    jsonObject.put("msg", updateItemErrorMsg);
                    return;
                }
                if (Convert.toBool(useQueue, false)) {
                    // 提交到队列暂存
                    BuildCache buildCache = new BuildCache();
                    buildCache.setId(id);
                    buildCache.setUserModel(userModel);
                    buildCache.setDelay(delay);
                    buildCache.setBuildRemark(buildRemark);
                    buildCache.setParametersEnv(parametersEnv);
                    //
                    Queue<BuildCache> buildCaches = waitQueue.computeIfAbsent(id, s -> new ConcurrentLinkedDeque<>());
                    buildCaches.add(buildCache);
                    jsonObject.put("msg", I18nMessageUtil.get("i18n.submit_task_queue_success.5f5b") + buildCaches.size());
                } else {
                    BaseServerController.resetInfo(userModel);
                    //
                    IJsonMessage<Integer> start = buildExecuteService.start(id, userModel, delay, 1, buildRemark, parametersEnv);
                    jsonObject.put("msg", start.getMsg());
                    jsonObject.put("buildId", start.getData());
                }
            }).collect(Collectors.toList());
            return JsonMessage.success(I18nMessageUtil.get("i18n.trigger_success.f9d1"), collect);
        } catch (Exception e) {
            throw new JpomRuntimeException(I18nMessageUtil.get("i18n.build_trigger_batch_exception.47d5"), e);
            //log.error("构建触发批量触发异常", e);
            //return JsonMessage.getString(500, "触发异常", e.getMessage());
        }
    }

    /**
     * 接收参数,修改
     *
     * @param jsonObject 参数
     * @return 错误消息
     */
    private String updateItem(JSONObject jsonObject) {
        String id = jsonObject.getString("id");
        String branchName = jsonObject.getString("branchName");
        String branchTagName = jsonObject.getString("branchTagName");
        String script = jsonObject.getString("script");
        String resultDirFile = jsonObject.getString("resultDirFile");
        String webhook = jsonObject.getString("webhook");
        //
        BuildInfoModel item = new BuildInfoModel();
        if (StrUtil.isNotEmpty(branchName)) {
            item.setBranchName(branchName);
        }
        if (StrUtil.isNotEmpty(branchTagName)) {
            item.setBranchTagName(branchTagName);
        }
        if (StrUtil.isNotEmpty(script)) {
            item.setScript(script);
        }
        if (StrUtil.isNotEmpty(resultDirFile)) {
            ResultDirFileAction parse = ResultDirFileAction.parse(resultDirFile);
            parse.check();
            item.setResultDirFile(resultDirFile);
        }
        if (StrUtil.isNotEmpty(webhook)) {
            if (!Validator.isMatchRegex(RegexPool.URL_HTTP, webhook)) {
                return I18nMessageUtil.get("i18n.invalid_webhooks_address.d836");
            }
            item.setWebhook(webhook);
        }
        if (ObjectUtil.isNotEmpty(item)) {
            item.setId(id);
            buildInfoService.updateById(item);
        }
        return null;
    }


    /**
     * 批量获取构建状态
     *
     * @return json
     */
    @GetMapping(value = ServerOpenApi.BUILD_TRIGGER_STATUS, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> buildStatusGet(@ValidatorItem String id, @ValidatorItem String token) {
        JSONObject statusData = this.getStatusData(id, token);
        return JsonMessage.success("", statusData);
    }

    /**
     * 批量获取构建状态
     */
    @GetMapping(value = ServerOpenApi.BUILD_TRIGGER_LOG, produces = MediaType.APPLICATION_JSON_VALUE)
    public void buildLogGet(@ValidatorItem String id,
                            @ValidatorItem String token,
                            @ValidatorItem(ValidatorRule.NUMBERS) Integer buildNumId,
                            HttpServletResponse response) throws IOException {
        BuildInfoModel item = buildInfoService.getByKey(id);
        if (item == null) {
            ServletUtil.write(response, I18nMessageUtil.get("i18n.no_data_found.4ffb"), ContentType.TEXT_PLAIN.getValue());
            return;
        }
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, buildInfoService.typeName());
        if (userModel == null) {
            ServletUtil.write(response, I18nMessageUtil.get("i18n.user_not_exist_trigger_invalid.f375"), ContentType.TEXT_PLAIN.getValue());
            return;
        }
        //
        if (!StrUtil.equals(token, item.getTriggerToken())) {
            ServletUtil.write(response, I18nMessageUtil.get("i18n.trigger_token_error_or_expired.8976"), ContentType.TEXT_PLAIN.getValue());
            return;
        }
        File file = BuildUtil.getLogFile(item.getId(), buildNumId);
        if (!FileUtil.isFile(file)) {
            ServletUtil.write(response, I18nMessageUtil.get("i18n.log_file_error.473b"), ContentType.TEXT_PLAIN.getValue());
            return;
        }
        try (BufferedInputStream inputStream = FileUtil.getInputStream(file)) {
            ServletUtil.write(response, inputStream, ContentType.TEXT_PLAIN.getValue());
        }
    }

    /**
     * 批量获取构建状态
     *
     * @return json
     */
    @PostMapping(value = ServerOpenApi.BUILD_TRIGGER_STATUS, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> buildStatusPost(HttpServletRequest request) {
        try {
            String body = ServletUtil.getBody(request);
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<JSONObject> collect = jsonArray.stream().map(o -> {
                JSONObject data = (JSONObject) o;
                String id = data.getString("id");
                String token = data.getString("token");
                return this.getStatusData(id, token);
            }).collect(Collectors.toList());
            return JsonMessage.success("", collect);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.get_build_status_exception.914e"), e);
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.unexpected_exception.2b52") + e.getMessage());
        }
    }

    private JSONObject getStatusData(String id, String token) {
        JSONObject jsonObject = new JSONObject();
        BuildInfoModel item = buildInfoService.getByKey(id);
        if (item == null) {
            String value = I18nMessageUtil.get("i18n.no_data_found.4ffb");
            jsonObject.put("msg", value);
            return jsonObject;
        }
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, buildInfoService.typeName());
        if (userModel == null) {
            String value = I18nMessageUtil.get("i18n.user_not_exist_trigger_invalid.f375");
            jsonObject.put("msg", value);
            return jsonObject;
        }
        //
        if (!StrUtil.equals(token, item.getTriggerToken())) {
            String value = I18nMessageUtil.get("i18n.trigger_token_error_or_expired.8976");
            jsonObject.put("msg", value);
            return jsonObject;
        }
        // 更新字段
        Integer status = item.getStatus();
        BuildStatus buildStatus = BaseEnum.getEnum(BuildStatus.class, status);
        if (buildStatus == null) {
            jsonObject.put("msg", "status code error");
        } else {
            jsonObject.put("msg", buildStatus.getDesc());
            jsonObject.put("statusCode", buildStatus.getCode());
            jsonObject.put("status", buildStatus.name());
        }
        jsonObject.put("buildNumberId", item.getBuildId());
        return jsonObject;
    }

    @Data
    private static class BuildCache {
        private UserModel userModel;
        // 构建外部参数
        private Object[] parametersEnv;

        private Integer delay;

        private String buildRemark;
        private String id;

        private Long taskTime;

        public BuildCache() {
            this.taskTime = SystemClock.now();
        }

        @Override
        public String toString() {
            return JSONObject.toJSONString(this);
        }
    }

    @Override
    public void startLoad() {
        ScheduledExecutorService scheduler = JpomApplication.getScheduledExecutorService();
        scheduler.scheduleWithFixedDelay(this, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        String id = "build_trigger_queue";
        int heartSecond = 5;
        try {
            CronUtils.TaskStat taskStat = CronUtils.getTaskStat(id, StrUtil.format(I18nMessageUtil.get("i18n.execution_frequency.d014"), heartSecond));
            taskStat.onStart();
            //
            this.runQueue();
            taskStat.onSucceeded();
        } catch (Throwable throwable) {
            CronUtils.TaskStat taskStat = CronUtils.getTaskStat(id, StrUtil.format(I18nMessageUtil.get("i18n.execution_frequency.d014"), heartSecond));
            taskStat.onFailed(id, throwable);
        }
    }

    private void runQueue() {
        // 先删除空队列
        Set<Map.Entry<String, Queue<BuildCache>>> entries = waitQueue.entrySet();
        Iterator<Map.Entry<String, Queue<BuildCache>>> entryIterator = entries.iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<String, Queue<BuildCache>> next = entryIterator.next();
            Queue<BuildCache> queue = next.getValue();
            if (queue.isEmpty()) {
                entryIterator.remove();
            }
        }
        int size = waitQueue.size();
        if (size > 0) {
            log.debug(I18nMessageUtil.get("i18n.need_handle_build_micro_queue_count.3010"), size);
            // 遍历队列中的数据
            waitQueue.forEach((buildId, buildCaches) -> {
                synchronized (buildId.intern()) {
                    log.debug(I18nMessageUtil.get("i18n.need_handle_build_queue_count.c01e"), buildId, buildCaches.size());
                    BuildInfoModel item = buildInfoService.getByKey(buildId);
                    if (item == null) {
                        log.error(I18nMessageUtil.get("i18n.build_data_not_exist.0225"), buildId, buildCaches.poll());
                        return;
                    }
                    String statusMsg = buildExecuteService.checkStatus(item);
                    if (statusMsg != null) {
                        log.debug(I18nMessageUtil.get("i18n.build_task_waiting.e303"), buildId, statusMsg);
                        return;
                    }
                    BuildCache cache = buildCaches.poll();
                    if (cache == null) {
                        return;
                    }
                    try {
                        BaseServerController.resetInfo(cache.userModel);
                        IJsonMessage<Integer> message = buildExecuteService.start(cache.id, cache.userModel, cache.delay, 1, cache.buildRemark, cache.parametersEnv);
                        log.info(I18nMessageUtil.get("i18n.build_trigger_queue_result.a1fe"), message);
                    } catch (Exception e) {
                        log.error(I18nMessageUtil.get("i18n.create_build_task_exception.06f1"), e);
                        // 重新添加任务
                        buildCaches.add(cache);
                    }
                }
            });
        }
    }
}
