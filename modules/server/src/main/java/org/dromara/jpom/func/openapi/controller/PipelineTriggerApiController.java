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

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.text.UnicodeUtil;
import com.alibaba.fastjson2.JSONObject;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseJpomController;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.pipeline.PipelineRunner;
import org.dromara.jpom.pipeline.model.PipelineInfoModel;
import org.dromara.jpom.pipeline.service.PipelineInfoService;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流水线开放接口：触发运行与查询最近一次运行状态
 * <p>
 * 与构建触发器的契约保持一致（token 校验顺序、错误码、i18n 文案），差异在于：
 * 流线线一次运行是异步多阶段，接口返回运行记录 ID 而不是「已开始」；
 * 同一流水线并发上限为 1，重复触发如实返回「正在运行」而不是静默排队。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@RestController
@RequestMapping
@NotLogin
@Slf4j
public class PipelineTriggerApiController extends BaseJpomController {

    /**
     * 开放接口触发的运行记录标记（与 PipelineRunModel#triggerType 一致：0 手动 / 1 定时 / 2 系统 / 3 重跑）
     */
    private static final int TRIGGER_TYPE_API = 2;

    private final PipelineInfoService pipelineInfoService;
    private final PipelineRunner pipelineRunner;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public PipelineTriggerApiController(PipelineInfoService pipelineInfoService,
                                        PipelineRunner pipelineRunner,
                                        TriggerTokenLogServer triggerTokenLogServer) {
        this.pipelineInfoService = pipelineInfoService;
        this.pipelineRunner = pipelineRunner;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 触发流水线运行
     *
     * @param id     流水线 ID
     * @param token  触发器 token
     * @param remark 备注（可选，进运行记录的状态说明）
     * @return 运行记录 ID 与运行编号
     */
    @RequestMapping(value = ServerOpenApi.PIPELINE_TRIGGER_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> trigger(@PathVariable String id, @PathVariable String token, String remark) {
        PipelineInfoModel item = this.checkToken(id, token);
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, pipelineInfoService.typeName());
        String runId;
        try {
            runId = pipelineRunner.start(id, userModel, remark, TRIGGER_TYPE_API);
        } catch (IllegalArgumentException e) {
            // 正在运行 / 配置校验不通过：如实返回业务失败，不伪装成服务端异常
            return new JsonMessage<>(405, e.getMessage());
        }
        PipelineInfoModel latest = pipelineInfoService.getByKey(id);
        JSONObject json = new JSONObject();
        json.put("runId", runId);
        json.put("runNumber", latest == null ? null : latest.getRunNumber());
        json.put("pipelineId", item.getId());
        return JsonMessage.success("", json);
    }

    /**
     * 最近一次运行状态
     *
     * @param id    流水线 ID
     * @param token 触发器 token
     * @return 状态（与流水线列表同一套状态码）
     */
    @GetMapping(value = ServerOpenApi.PIPELINE_TRIGGER_STATUS, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> status(String id, String token) {
        PipelineInfoModel item = this.checkToken(id, token);
        Assert.notNull(triggerTokenLogServer.getUserByToken(token, pipelineInfoService.typeName()),
            I18nMessageUtil.get("i18n.trigger_token_error_or_expired_with_code.393b"));
        JSONObject json = new JSONObject();
        json.put("pipelineId", item.getId());
        json.put("status", item.getStatus());
        json.put("statusMsg", UnicodeUtil.toString(item.getStatusMsg()));
        json.put("runNumber", item.getRunNumber());
        json.put("lastRunId", item.getLastRunId());
        json.put("running", item.getStatus() != null && item.getStatus() == 1);
        return JsonMessage.success("", json);
    }

    /**
     * 与构建触发器一致的校验顺序：先查数据，再比对 token
     */
    private PipelineInfoModel checkToken(String id, String token) {
        // 缺参（例如用户直接打开地址没带 ?id=&token=）如实回「token 错误或失效」，不要含糊成「没有对应数据」
        Assert.state(StrUtil.isNotEmpty(id) && StrUtil.isNotEmpty(token), I18nMessageUtil.get("i18n.trigger_token_error_or_expired.8976"));
        PipelineInfoModel item = pipelineInfoService.getByKey(id);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        Assert.state(StrUtil.equals(token, item.getTriggerToken()), I18nMessageUtil.get("i18n.trigger_token_error_or_expired.8976"));
        return item;
    }
}
