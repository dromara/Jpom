/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.sshproject;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.dromara.jpom.model.sshproject.SshProjectStatus;
import org.dromara.jpom.model.sshproject.SshProjectTemplate;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.dromara.jpom.service.sshproject.SshProjectLogService;
import org.dromara.jpom.service.sshproject.SshProjectService;
import org.dromara.jpom.service.sshproject.SshProjectTemplateService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * SSH 项目（不依赖插件端）
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@RestController
@RequestMapping(value = "/node/ssh-project")
@Feature(cls = ClassFeature.SSH_PROJECT)
public class SshProjectController extends BaseServerController {

    private final SshProjectService sshProjectService;
    private final SshProjectTemplateService sshProjectTemplateService;
    private final SshProjectLogService sshProjectLogService;

    public SshProjectController(SshProjectService sshProjectService,
                                SshProjectTemplateService sshProjectTemplateService,
                                SshProjectLogService sshProjectLogService) {
        this.sshProjectService = sshProjectService;
        this.sshProjectTemplateService = sshProjectTemplateService;
        this.sshProjectLogService = sshProjectLogService;
    }

    /**
     * 项目列表
     *
     * @param request 请求
     * @return 分页
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<SshProjectModel>> list(HttpServletRequest request) {
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        // 兼容前端表格组件的 current/pageSize 命名
        if (!paramMap.containsKey("page") && paramMap.containsKey("current")) {
            paramMap.put("page", paramMap.get("current"));
        }
        if (!paramMap.containsKey("limit") && paramMap.containsKey("pageSize")) {
            paramMap.put("limit", paramMap.get("pageSize"));
        }
        String workspaceId = sshProjectService.getCheckUserWorkspace(request);
        BaseDbService.putForcedFilter(paramMap, "workspaceId", workspaceId);
        PageResultDto<SshProjectModel> page = sshProjectService.listPage(paramMap);
        return JsonMessage.success("", page);
    }

    /**
     * 项目详情
     *
     * @param id      id
     * @param request 请求
     * @return 项目
     */
    @PostMapping(value = "item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<SshProjectModel> item(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.ssh_project_not_exist.5d18") String id,
                                              HttpServletRequest request) {
        SshProjectModel item = sshProjectService.getItem(id, request);
        return JsonMessage.success("", item);
    }

    /**
     * 保存项目
     *
     * @param model   项目
     * @param request 请求
     * @return 操作结果
     */
    @PostMapping(value = "save", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> save(SshProjectModel model, HttpServletRequest request) {
        sshProjectService.saveItem(model, request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    /**
     * 删除项目
     *
     * @param id      id
     * @param request 请求
     * @return 操作结果
     */
    @PostMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.ssh_project_not_exist.5d18") String id,
                                       HttpServletRequest request) {
        sshProjectService.deleteItem(id, request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }


    /**
     * 启动/停止/重启/重载
     *
     * @param id      id
     * @param opt     操作（start/stop/restart/reload）
     * @param request 请求
     * @return 执行结果
     */
    @PostMapping(value = "operate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<JSONObject> operate(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.ssh_project_not_exist.5d18") String id,
                                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.ssh_project_operate_unsupported.3d18") String opt,
                                            HttpServletRequest request) {
        SshProjectExecuteResult result = sshProjectService.operate(id, opt, null, request);
        SshProjectModel project = sshProjectService.getItem(id, request);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success", result.isSuccess());
        jsonObject.put("exitCode", result.getExitCode());
        jsonObject.put("message", result.getMessage());
        jsonObject.put("output", result.getOutput());
        jsonObject.put("cost", result.getCost());
        jsonObject.put("status", project.getLastStatus());
        jsonObject.put("pid", project.getLastPid());
        jsonObject.put("port", project.getLastPort());
        if (!result.isSuccess()) {
            return new JsonMessage<>(406, StrUtil.emptyToDefault(result.getMessage(), I18nMessageUtil.get("i18n.ssh_project_exec_fail.4b82")), jsonObject);
        }
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 实时状态（单条或批量）
     *
     * @param id      id
     * @param ids     ids（JSON 数组字符串）
     * @param request 请求
     * @return 状态
     */
    @PostMapping(value = "status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> status(String id, String ids, HttpServletRequest request) {
        JSONObject result = new JSONObject();
        if (StrUtil.isNotEmpty(ids)) {
            JSONArray jsonArray = JSONArray.parseArray(ids);
            for (Object item : jsonArray) {
                this.fillStatus(result, item.toString(), request);
            }
            return JsonMessage.success("", result);
        }
        Assert.hasText(id, I18nMessageUtil.get("i18n.ssh_project_not_exist.5d18"));
        this.fillStatus(result, id, request);
        return JsonMessage.success("", result);
    }

    private void fillStatus(JSONObject result, String id, HttpServletRequest request) {
        SshProjectModel project = sshProjectService.getItem(id, request);
        SshProjectStatus status = sshProjectService.refreshStatus(project, true);
        JSONObject item = new JSONObject();
        item.put("status", project.getLastStatus());
        item.put("pid", status.getPid());
        item.put("port", status.getPorts());
        item.put("message", status.getMessage());
        result.put(id, item);
    }

    /**
     * 端口
     *
     * @param id      id
     * @param request 请求
     * @return 端口
     */
    @PostMapping(value = "port", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> port(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.ssh_project_not_exist.5d18") String id,
                                         HttpServletRequest request) {
        SshProjectModel project = sshProjectService.getItem(id, request);
        SshProjectStatus status = sshProjectService.refreshStatus(project, true);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("pid", status.getPid());
        jsonObject.put("port", status.getPorts());
        return JsonMessage.success("", jsonObject);
    }


    /**
     * 可查看的日志文件列表（控制台日志 + dsl 声明的应用日志）
     *
     * @param id      id
     * @param request 请求
     * @return 列表
     */
    @PostMapping(value = "log-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<java.util.List<JSONObject>> logList(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.ssh_project_not_exist.5d18") String id,
                                                            HttpServletRequest request) {
        SshProjectModel project = sshProjectService.getItem(id, request);
        java.util.List<JSONObject> list = new java.util.ArrayList<>();
        JSONObject console = new JSONObject();
        console.put("name", "console");
        console.put("label", I18nMessageUtil.get("i18n.ssh_project_console_log.2e81"));
        list.add(console);
        org.dromara.jpom.model.data.DslYmlDto dsl = org.dromara.jpom.model.data.DslYmlDto.build(project.getDslContent());
        if (dsl.getConfig() != null && dsl.getConfig().getLog() != null) {
            for (org.dromara.jpom.model.data.DslYmlDto.LogFile logFile : dsl.getConfig().getLog()) {
                JSONObject item = new JSONObject();
                item.put("name", logFile.getName());
                item.put("label", logFile.getName());
                item.put("path", logFile.getPath());
                list.add(item);
            }
        }
        return JsonMessage.success("", list);
    }


    /**
     * 日志备份列表
     *
     * @param id      id
     * @param request 请求
     * @return 列表
     */
    @PostMapping(value = "log/list-backup", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<java.util.List<JSONObject>> logBackupList(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.ssh_project_not_exist.5d18") String id,
                                                                  HttpServletRequest request) {
        SshProjectModel project = sshProjectService.getItem(id, request);
        return JsonMessage.success("", sshProjectLogService.listBackup(project));
    }

    /**
     * 备份当前控制台日志
     *
     * @param id      id
     * @param request 请求
     * @return 备份文件
     */
    @PostMapping(value = "log/backup", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> logBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.ssh_project_not_exist.5d18") String id,
                                         HttpServletRequest request) {
        SshProjectModel project = sshProjectService.getItem(id, request);
        return JsonMessage.success("", sshProjectLogService.backup(project));
    }

    /**
     * 清空控制台日志
     *
     * @param id      id
     * @param request 请求
     * @return 操作结果
     */
    @PostMapping(value = "log/reset", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> logReset(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.ssh_project_not_exist.5d18") String id,
                                        HttpServletRequest request) {
        SshProjectModel project = sshProjectService.getItem(id, request);
        sshProjectLogService.reset(project);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    /**
     * 下载控制台日志或备份
     *
     * @param id       id
     * @param name     console 或备份文件名
     * @param response 响应
     * @param request  请求
     */
    @GetMapping(value = "log/download")
    @Feature(method = MethodFeature.DOWNLOAD)
    public void logDownload(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.ssh_project_not_exist.5d18") String id,
                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.file_not_found.d952") String name,
                            HttpServletResponse response,
                            HttpServletRequest request) throws Exception {
        SshProjectModel project = sshProjectService.getItem(id, request);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(name, "UTF-8"));
        sshProjectLogService.download(project, name, response.getOutputStream());
    }


    /**
     * 生成/查看触发器地址
     *
     * @param id      id
     * @param request 请求
     * @return 地址
     */
    @PostMapping(value = "trigger-url", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<JSONObject> triggerUrl(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.ssh_project_not_exist.5d18") String id,
                                               HttpServletRequest request) {
        SshProjectModel project = sshProjectService.getItem(id, request);
        String token = project.getToken();
        if (StrUtil.isEmpty(token)) {
            token = cn.hutool.core.util.IdUtil.fastSimpleUUID();
            SshProjectModel update = new SshProjectModel();
            update.setId(project.getId());
            update.setToken(token);
            sshProjectService.updateById(update);
            project.setToken(token);
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", token);
        jsonObject.put("url", StrUtil.format("/openapi/ssh-project/{}/{}/", project.getId(), token));
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 内置模板列表
     *
     * @return 模板
     */
    @PostMapping(value = "template-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<SshProjectTemplate>> templateList() {
        return JsonMessage.success("", sshProjectTemplateService.list());
    }

    /**
     * 渲染模板为生命周期 yml
     *
     * @param templateKey 模板 key
     * @param fieldsJson  字段值 JSON
     * @return yml
     */
    @PostMapping(value = "template-render", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> templateRender(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.ssh_project_template_not_found.c2a7") String templateKey,
                                                   String fieldsJson) {
        String dslContent = sshProjectTemplateService.render(templateKey, fieldsJson);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("dslContent", dslContent);
        return JsonMessage.success("", jsonObject);
    }
}
