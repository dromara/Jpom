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
package io.jpom.controller.openapi;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.build.BuildExecuteService;
import io.jpom.common.BaseJpomController;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.ServerOpenApi;
import io.jpom.common.interceptor.NotLogin;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.enums.BuildStatus;
import io.jpom.model.user.UserModel;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.user.TriggerTokenLogServer;
import io.jpom.system.JpomRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2019/9/4
 */
@RestController
@NotLogin
@Slf4j
public class BuildTriggerApiController extends BaseJpomController {

    private final BuildInfoService buildInfoService;
    private final BuildExecuteService buildExecuteService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public BuildTriggerApiController(BuildInfoService buildInfoService,
                                     BuildExecuteService buildExecuteService,
                                     TriggerTokenLogServer triggerTokenLogServer) {
        this.buildInfoService = buildInfoService;
        this.buildExecuteService = buildExecuteService;
        this.triggerTokenLogServer = triggerTokenLogServer;
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
    public String trigger2(@PathVariable String id, @PathVariable String token, String delay, String buildRemark) {
        BuildInfoModel item = buildInfoService.getByKey(id);
        Assert.notNull(item, "没有对应数据");
        UserModel userModel = this.triggerTokenLogServer.getUserByToken(token, buildInfoService.typeName());
        //
        Assert.notNull(userModel, "触发token错误,或者已经失效:-1");

        Assert.state(StrUtil.equals(token, item.getTriggerToken()), "触发token错误,或者已经失效");
        BaseServerController.resetInfo(userModel);
        JsonMessage<Integer> start = buildExecuteService.start(id, userModel, Convert.toInt(delay, 0), 1, buildRemark);
        return start.toString();
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
    public JsonMessage<List<Object>> triggerBatch() {
        try {
            String body = ServletUtil.getBody(getRequest());
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<Object> collect = jsonArray.stream().peek(o -> {
                JSONObject jsonObject = (JSONObject) o;
                String id = jsonObject.getString("id");
                String token = jsonObject.getString("token");
                Integer delay = jsonObject.getInteger("delay");
                String buildRemark = jsonObject.getString("buildRemark");
                BuildInfoModel item = buildInfoService.getByKey(id);
                if (item == null) {
                    jsonObject.put("msg", "没有对应数据");
                    return;
                }
                UserModel userModel = triggerTokenLogServer.getUserByToken(token, buildInfoService.typeName());
                if (userModel == null) {
                    jsonObject.put("msg", "对应的用户不存在,触发器已失效");
                    return;
                }
                //
                if (!StrUtil.equals(token, item.getTriggerToken())) {
                    jsonObject.put("msg", "触发token错误,或者已经失效");
                    return;
                }
                // 更新字段
                String updateItemErrorMsg = this.updateItem(jsonObject);
                if (updateItemErrorMsg != null) {
                    jsonObject.put("msg", updateItemErrorMsg);
                    return;
                }
                BaseServerController.resetInfo(userModel);
                //
                JsonMessage<Integer> start = buildExecuteService.start(id, userModel, delay, 1, buildRemark);
                jsonObject.put("msg", start.getMsg());
                jsonObject.put("buildId", start.getData());
            }).collect(Collectors.toList());
            return JsonMessage.success("触发成功", collect);
        } catch (Exception e) {
            throw new JpomRuntimeException("构建触发批量触发异常", e);
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
            item.setResultDirFile(resultDirFile);
        }
        if (StrUtil.isNotEmpty(webhook)) {
            if (!Validator.isMatchRegex(RegexPool.URL_HTTP, webhook)) {
                return "WebHooks 地址不合法";
            }
            item.setWebhook(webhook);
        }
        if (ObjectUtil.isNotEmpty(item)) {
            item.setId(id);
            buildInfoService.update(item);
        }
        return null;
    }


    /**
     * 批量获取构建状态
     *
     * @return json
     */
    @GetMapping(value = ServerOpenApi.BUILD_TRIGGER_STATUS, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> buildStatusGet(@ValidatorItem String id, @ValidatorItem String token) {
        JSONObject statusData = this.getStatusData(id, token);
        return JsonMessage.success("", statusData);
    }

    /**
     * 批量获取构建状态
     *
     * @return json
     */
    @PostMapping(value = ServerOpenApi.BUILD_TRIGGER_STATUS, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<List<JSONObject>> buildStatusPost() {
        try {
            String body = ServletUtil.getBody(getRequest());
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<JSONObject> collect = jsonArray.stream().map(o -> {
                JSONObject data = (JSONObject) o;
                String id = data.getString("id");
                String token = data.getString("token");
                return this.getStatusData(id, token);
            }).collect(Collectors.toList());
            return JsonMessage.success("", collect);
        } catch (Exception e) {
            log.error("获取构建状态异常", e);
            return new JsonMessage<>(500, "发生异常" + e.getMessage());
        }
    }

    private JSONObject getStatusData(String id, String token) {
        JSONObject jsonObject = new JSONObject();
        BuildInfoModel item = buildInfoService.getByKey(id);
        if (item == null) {
            jsonObject.put("msg", "没有对应数据");
            return jsonObject;
        }
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, buildInfoService.typeName());
        if (userModel == null) {
            jsonObject.put("msg", "对应的用户不存在,触发器已失效");
            return jsonObject;
        }
        //
        if (!StrUtil.equals(token, item.getTriggerToken())) {
            jsonObject.put("msg", "触发token错误,或者已经失效");
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

}
