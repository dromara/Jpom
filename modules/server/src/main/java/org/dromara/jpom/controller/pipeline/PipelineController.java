/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.pipeline;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.build.ExecutionRegistry;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.pipeline.PipelineRunner;
import org.dromara.jpom.pipeline.model.PipelineDefinition;
import org.dromara.jpom.pipeline.model.PipelineInfoModel;
import org.dromara.jpom.pipeline.model.PipelineValidateException;
import org.dromara.jpom.pipeline.hook.PipelineEvent;
import org.dromara.jpom.pipeline.hook.PipelineHookDef;
import org.dromara.jpom.pipeline.service.PipelineInfoService;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.service.user.TriggerUrlAssembler;
import org.dromara.jpom.service.user.TriggerUrls;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水线（新模块）：配置的增删改查
 * <p>
 * 与「在线构建」完全隔离：自己的表（PIPELINE_INFO）、自己的权限点（ClassFeature.PIPELINE）、
 * 自己的数据目录；不读写 BUILD_INFO，老构建因此零影响。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@RestController
@RequestMapping(value = "/pipeline")
@Feature(cls = ClassFeature.PIPELINE)
public class PipelineController extends BaseServerController {

    private final PipelineInfoService pipelineInfoService;
    private final PipelineRunner pipelineRunner;
    private final TriggerUrlAssembler triggerUrlAssembler;
    private final ScriptServer scriptServer;

    public PipelineController(PipelineInfoService pipelineInfoService,
                              PipelineRunner pipelineRunner,
                              TriggerUrlAssembler triggerUrlAssembler,
                              ScriptServer scriptServer) {
        this.pipelineInfoService = pipelineInfoService;
        this.pipelineRunner = pipelineRunner;
        this.triggerUrlAssembler = triggerUrlAssembler;
        this.scriptServer = scriptServer;
    }

    /**
     * 事件清单（事件状态切换机制的自描述）
     * <p>
     * 前端选项由接口返回，不在前端复制一份事件码：新增事件只改 {@link PipelineEvent}，
     * 前端下拉自动出现，避免「界面有选项、后端不认识」。
     * </p>
     *
     * @return 事件码与本地化描述
     */
    @RequestMapping(value = "/hook-events", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<Map<String, String>>> hookEvents() {
        List<Map<String, String>> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : PipelineEvent.descriptions().entrySet()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", entry.getKey());
            item.put("desc", entry.getValue());
            list.add(item);
        }
        return JsonMessage.success("", list);
    }

    /**
     * 触发器地址（生成/重置）
     * <p>
     * 与构建、脚本、SSH 命令等触发器走同一个收口点（{@link TriggerUrlAssembler}）：查不到统一 405、
     * token 生成/重置与审计落库同一段代码；流水线因此自动出现在「系统管理 → 触发器」的类型与记录列表里。
     * </p>
     *
     * @param id      流水线 ID
     * @param rest    非空表示重置
     * @param request 请求
     * @return 触发地址集合
     */
    @RequestMapping(value = "/trigger-url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Map<String, String>> getTriggerUrl(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                                                           String rest,
                                                           HttpServletRequest request) {
        PipelineInfoModel item = triggerUrlAssembler.require(() -> pipelineInfoService.getByKey(id, request),
            I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        Map<String, String> map = triggerUrlAssembler.issue(pipelineInfoService, item.getId(), item.getTriggerToken(), rest, getUser().getId(),
            TriggerUrls.builder()
                .add("triggerUrl", ServerOpenApi.PIPELINE_TRIGGER_URL)
                .add("statusUrl", ServerOpenApi.PIPELINE_TRIGGER_STATUS)
                .build(), request);
        return JsonMessage.success(TriggerUrlAssembler.successMessage(rest), map);
    }

    /**
     * 列表（分页）
     * <p>
     * 与在线构建列表统一：走 {@code listPage(request)}，支持 {@code page/limit/%name%/status/order_field}，
     * 工作空间由服务层强制注入（客户端传同名参数会被剔除）。
     * </p>
     *
     * @param request 请求（工作空间隔离由服务层强制）
     * @return 分页结果
     */
    @RequestMapping(value = "/list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<PipelineInfoModel>> list(HttpServletRequest request) {
        return JsonMessage.success("", pipelineInfoService.listPage(request));
    }

    /**
     * 分组列表（虚拟逻辑分组）
     * <p>
     * 与在线构建 {@code /build/list_group_all}、SSH、FTP、仓库、项目同一套语义：返回当前工作空间已存在的分组名，
     * 供编辑页定制下拉框（{@code custom-select}）作为候选项；分组不是独立实体，新增分组随保存自动出现。
     * </p>
     *
     * @param request 请求（工作空间由服务层强制）
     * @return 分组名称列表
     */
    @RequestMapping(value = "/list_group_all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<String>> listGroupAll(HttpServletRequest request) {
        return JsonMessage.success("", pipelineInfoService.listGroup(request));
    }

    /**
     * 详情
     *
     * @param id      流水线 ID
     * @param request 请求
     * @return 详情
     */
    @RequestMapping(value = "/get", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PipelineInfoModel> get(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                                 HttpServletRequest request) {
        PipelineInfoModel item = pipelineInfoService.getByKey(id, request);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        return JsonMessage.success("", item);
    }

    /**
     * 保存（新建/更新）
     *
     * @param id         流水线 ID（更新时必填）
     * @param name       名称
     * @param group      分组
     * @param sourcesJson 仓库源 JSON
     * @param stagesJson  阶段 JSON
     * @param request    请求
     * @return 保存后的记录
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<PipelineInfoModel> save(String id,
                                  @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.pipeline_name_required.5b7c") String name,
                                  String group,
                                  String sourcesJson,
                                  @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.pipeline_stage_required.4c18") String stagesJson,
                                  String hooksJson,
                                  HttpServletRequest request) {
        // 服务端必须完整校验：前端禁用/隐藏只是交互提示，直接调接口仍可绕过；校验异常带 i18n key，这里翻译成用户可读文案
        PipelineDefinition definition = PipelineDefinition.of(sourcesJson, stagesJson, hooksJson);
        try {
            definition.check();
        } catch (PipelineValidateException e) {
            throw new IllegalArgumentException(I18nMessageUtil.get(e.getKey()) + StrUtil.nullToEmpty(e.getDetail()), e);
        }
        // 事件脚本存在性：保存时拦住，避免运行到 notify 事件才发现脚本已被删除
        for (PipelineHookDef hook : definition.getHooks()) {
            if (hook == null || !"script".equals(hook.getType())) {
                continue;
            }
            ScriptModel scriptModel = scriptServer.getByKey(hook.getScriptId(), request);
            Assert.notNull(scriptModel, StrUtil.format(I18nMessageUtil.get("i18n.pipeline_hook_script_not_exists.8c02"),
                hook.getScriptId()));
        }
        PipelineInfoModel model = new PipelineInfoModel();
        model.setName(name);
        model.setGroupName(group);
        model.setSourcesJson(JSON.toJSONString(definition.getSources()));
        model.setStagesJson(JSON.toJSONString(definition.getStages()));
        model.setHooksJson(JSON.toJSONString(definition.getHooks()));
        model.setSchemaVersion(PipelineDefinition.CURRENT_SCHEMA_VERSION);
        if (StrUtil.isEmpty(id)) {
            model.setWorkspaceId(getWorkspaceId());
            model.setRunNumber(0);
            pipelineInfoService.insert(model);
        } else {
            PipelineInfoModel exists = pipelineInfoService.getByKey(id, request);
            Assert.notNull(exists, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
            model.setId(id);
            pipelineInfoService.updateById(model);
        }
        return JsonMessage.success("", pipelineInfoService.getByKey(model.getId()));
    }

    /**
     * 运行流水线（异步，返回运行记录 ID）
     *
     * @param id      流水线 ID
     * @param remark  备注
     * @param request 请求
     * @return 运行记录 ID
     */
    @RequestMapping(value = "/run", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> run(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                      String remark,
                      HttpServletRequest request) {
        PipelineInfoModel item = pipelineInfoService.getByKey(id, request);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        return JsonMessage.success("", pipelineRunner.start(id, getUser(), remark));
    }

    /**
     * 取消运行
     * <p>
     * 与在线构建同一套语义：先登记取消意图，执行侧确认进程退出后再落「已取消」；
     * 找不到执行体时如实返回，不会假装取消成功。
     * </p>
     *
     * @param id      流水线 ID
     * @param request 请求
     * @return 取消受理结果
     */
    @RequestMapping(value = "/cancel", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> cancel(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                         HttpServletRequest request) {
        PipelineInfoModel item = pipelineInfoService.getByKey(id, request);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        ExecutionRegistry.CancelOutcome outcome = pipelineRunner.cancel(id);
        // 直接把枚举名（REQUESTED/NOT_FOUND）显示给用户既不可读也不便本地化，这里按三态出文案
        String key;
        switch (outcome) {
            case REQUESTED:
                key = "i18n.pipeline_cancel_requested.3f01";
                break;
            case ALREADY_REQUESTED:
                key = "i18n.pipeline_cancel_already_requested.3f02";
                break;
            default:
                key = "i18n.pipeline_cancel_not_found.3f03";
                break;
        }
        return JsonMessage.success("", I18nMessageUtil.get(key));
    }

    /**
     * 删除
     *
     * @param id      流水线 ID
     * @param request 请求
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                       HttpServletRequest request) {
        PipelineInfoModel item = pipelineInfoService.getByKey(id, request);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        pipelineInfoService.delByKey(id, request);
        return JsonMessage.success("删除成功");
    }
}