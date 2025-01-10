/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.files.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.controller.outgiving.OutGivingWhitelistService;
import org.dromara.jpom.func.files.model.FileReleaseTaskLogModel;
import org.dromara.jpom.func.files.model.FileReleaseTaskTemplate;
import org.dromara.jpom.func.files.model.IFileStorage;
import org.dromara.jpom.func.files.service.FileReleaseTaskService;
import org.dromara.jpom.func.files.service.FileReleaseTaskTemplateService;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.model.data.ServerWhitelist;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/3/18
 */
@RestController
@RequestMapping(value = "/file-storage/release-task")
@Feature(cls = ClassFeature.FILE_STORAGE_RELEASE)
public class FileReleaseTaskController extends BaseServerController {

    private final FileReleaseTaskService fileReleaseTaskService;
    private final OutGivingWhitelistService outGivingWhitelistService;
    private final ScriptServer scriptServer;
    private final FileReleaseTaskTemplateService fileReleaseTaskTemplateService;

    public FileReleaseTaskController(FileReleaseTaskService fileReleaseTaskService,
                                     OutGivingWhitelistService outGivingWhitelistService,
                                     NodeService nodeService,
                                     ScriptServer scriptServer,
                                     FileReleaseTaskTemplateService fileReleaseTaskTemplateService) {
        this.fileReleaseTaskService = fileReleaseTaskService;
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.scriptServer = scriptServer;
        this.fileReleaseTaskTemplateService = fileReleaseTaskTemplateService;
        this.nodeService = nodeService;
    }


    @PostMapping(value = "add-task", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> addTask(@ValidatorItem(msg = "i18n.file_id_missing.0e39") String fileId,
                                        @ValidatorItem(value = ValidatorRule.NUMBERS, msg = "i18n.select_file_type.064a") Integer fileType,
                                        @ValidatorItem(msg = "i18n.task_name.2ca5") String name,
                                        @ValidatorItem(value = ValidatorRule.NUMBERS, msg = "i18n.select_publish_method.29b4") int taskType,
                                        @ValidatorItem(msg = "i18n.select_related_data_id.7fba") String taskDataIds,
                                        @ValidatorItem(msg = "i18n.select_publish_directory.db60") String releasePathParent,
                                        @ValidatorItem(msg = "i18n.publish_subdirectory.dc0d") String releasePathSecondary,
                                        String beforeScript,
                                        String afterScript,
                                        String save2Template,
                                        HttpServletRequest request) {
        // 判断参数
        ServerWhitelist configDeNewInstance = outGivingWhitelistService.getServerWhitelistData(request);
        List<String> whitelistServerOutGiving = configDeNewInstance.getOutGiving();
        Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, releasePathParent), I18nMessageUtil.get("i18n.select_correct_project_path_or_no_auth_configured.366a"));
        Assert.hasText(releasePathSecondary, I18nMessageUtil.get("i18n.publish_file_second_level_directory_required.2f65"));

        if (StrUtil.startWith(beforeScript, ServerConst.REF_SCRIPT)) {
            String scriptId = StrUtil.removePrefix(beforeScript, ServerConst.REF_SCRIPT);
            ScriptModel keyAndGlobal = scriptServer.getByKeyAndGlobal(scriptId, request, I18nMessageUtil.get("i18n.select_correct_pre_publish_script.d230"));
            Assert.notNull(keyAndGlobal, I18nMessageUtil.get("i18n.select_correct_pre_publish_script.d230"));
        }
        if (StrUtil.startWith(afterScript, ServerConst.REF_SCRIPT)) {
            String scriptId = StrUtil.removePrefix(afterScript, ServerConst.REF_SCRIPT);
            ScriptModel keyAndGlobal = scriptServer.getByKeyAndGlobal(scriptId, request, I18nMessageUtil.get("i18n.select_correct_post_publish_script.49d2"));
            Assert.notNull(keyAndGlobal, I18nMessageUtil.get("i18n.select_correct_post_publish_script.49d2"));
        }

        String releasePath = FileUtil.normalize(releasePathParent + StrUtil.SLASH + releasePathSecondary);

        IFileStorage storageModel = fileReleaseTaskService.addTask(fileId, fileType, name, taskType, taskDataIds, releasePath, beforeScript, afterScript, null, request);
        // 判断是否需要存储为模板
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("releasePath", releasePath);
        jsonObject.put("save2Template", save2Template);
        jsonObject.put("fileType", fileType);
        jsonObject.put("fileId", fileId);
        jsonObject.put("taskType", taskType);
        jsonObject.put("taskDataIds", taskDataIds);
        jsonObject.put("releasePathParent", releasePathParent);
        jsonObject.put("releasePathSecondary", releasePathSecondary);
        jsonObject.put("beforeScript", beforeScript);
        jsonObject.put("afterScript", afterScript);
        String workspaceId = fileReleaseTaskTemplateService.getCheckUserWorkspace(request);
        fileReleaseTaskTemplateService.add(save2Template, workspaceId, storageModel, fileType, jsonObject);
        return JsonMessage.success(I18nMessageUtil.get("i18n.create_success.04a6"));
    }


    /**
     * 重建-重新发布
     *
     * @param fileId       文件id
     * @param name         任务名
     * @param taskType     任务类型
     * @param taskDataIds  任务关联数据id
     * @param parentTaskId 父级任务id
     * @param beforeScript 发布之前的脚步
     * @param afterScript  发布之后的脚步
     * @param request      请求
     * @return json
     */
    @PostMapping(value = "re-task", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> reTask(@ValidatorItem(msg = "i18n.file_id_missing.0e39") String fileId,
                                       @ValidatorItem(msg = "i18n.task_name.2ca5") String name,
                                       @ValidatorItem(value = ValidatorRule.NUMBERS, msg = "i18n.select_publish_method.29b4") int taskType,
                                       @ValidatorItem(msg = "i18n.select_related_data_id.7fba") String taskDataIds,
                                       @ValidatorItem(msg = "i18n.missing_parent_id.4331") String parentTaskId,
                                       String beforeScript,
                                       String afterScript,
                                       HttpServletRequest request) {
        FileReleaseTaskLogModel parentTask = fileReleaseTaskService.getByKey(parentTaskId, request);
        Assert.notNull(parentTask, I18nMessageUtil.get("i18n.parent_task_not_exist.ca1b"));
        Integer fileType = parentTask.getFileType();
        fileType = ObjectUtil.defaultIfNull(fileType, 1);
        fileReleaseTaskService.addTask(fileId, fileType, name, taskType, taskDataIds, parentTask.getReleasePath(), beforeScript, afterScript, null, request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.create_success.04a6"));
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<FileReleaseTaskLogModel>> list(HttpServletRequest request) {
        //
        PageResultDto<FileReleaseTaskLogModel> listPage = fileReleaseTaskService.listPage(request);
        return JsonMessage.success("", listPage);
    }


    /**
     * 发布模块列表
     *
     * @return json
     */
    @PostMapping(value = "list-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<FileReleaseTaskTemplate>> listTemplate(HttpServletRequest request) {
        //
        PageResultDto<FileReleaseTaskTemplate> listPage = fileReleaseTaskTemplateService.listPage(request);
        return JsonMessage.success("", listPage);
    }

    /**
     * 获取模板
     *
     * @param id 模板id
     * @return json
     */
    @GetMapping(value = "get-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<FileReleaseTaskTemplate> getTemplate(@ValidatorItem(msg = "i18n.id_not_empty.bf1f") String id,
                                                             String alias,
                                                             Integer fileType,
                                                             HttpServletRequest request) {
        Entity entity = new Entity();
        String workspaceId = fileReleaseTaskTemplateService.getCheckUserWorkspace(request);
        entity.set("workspaceId", workspaceId);
        if (fileType != null && (fileType == 1 || fileType == 2)) {
            entity.set("fileType", fileType);
        }
        if (StrUtil.isNotEmpty(alias)) {
            entity.set("templateTag", "alias:" + alias);
        } else {
            entity.set("templateTag", "id:" + id);
        }
        //
        List<FileReleaseTaskTemplate> templates = fileReleaseTaskTemplateService.queryList(entity, 1, new Order("modifyTimeMillis", Direction.DESC), new Order("createTimeMillis", Direction.DESC));
        if (CollUtil.isEmpty(templates) && StrUtil.isNotEmpty(alias)) {
            // 别名没有查询到，查询id
            entity.set("templateTag", "id:" + id);
            templates = fileReleaseTaskTemplateService.queryList(entity, 1, new Order("modifyTimeMillis", Direction.DESC), new Order("createTimeMillis", Direction.DESC));
        }
        return JsonMessage.success("", CollUtil.getFirst(templates));
    }

    /**
     * 删除模板
     *
     * @param id 模板id
     * @return json
     */
    @GetMapping(value = "delete-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<JSONObject> deleteTemplate(@ValidatorItem(msg = "i18n.id_not_empty.bf1f") String id, HttpServletRequest request) {
        //
        fileReleaseTaskTemplateService.delByKey(id, request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }

    /**
     * 取消任务
     *
     * @param id 任务id
     * @return json
     */
    @GetMapping(value = "cancel-task", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> hasFile(@ValidatorItem(msg = "i18n.missing_task_id.12a7") String id, HttpServletRequest request) {
        FileReleaseTaskLogModel taskLogModel = fileReleaseTaskService.getByKey(id, request);
        Assert.notNull(taskLogModel, I18nMessageUtil.get("i18n.task_not_exist.47e9"));
        fileReleaseTaskService.cancelTask(taskLogModel.getId());
        return JsonMessage.success(I18nMessageUtil.get("i18n.cancel_success.285f"));
    }

    /**
     * 查询任务
     *
     * @param id 任务id
     * @return json
     */
    @GetMapping(value = "details", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> details(@ValidatorItem(msg = "i18n.missing_task_id.12a7") String id, HttpServletRequest request) {
        FileReleaseTaskLogModel taskLogModel = fileReleaseTaskService.getByKey(id, request);
        Assert.notNull(taskLogModel, I18nMessageUtil.get("i18n.task_not_exist.47e9"));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("taskData", taskLogModel);
        FileReleaseTaskLogModel fileReleaseTaskLogModel = new FileReleaseTaskLogModel();
        fileReleaseTaskLogModel.setTaskId(taskLogModel.getId());
        List<FileReleaseTaskLogModel> logModels = fileReleaseTaskService.listByBean(fileReleaseTaskLogModel);
        jsonObject.put("taskList", logModels);
        return JsonMessage.success(I18nMessageUtil.get("i18n.cancel_success.285f"), jsonObject);
    }

    /**
     * 删除任务
     *
     * @param id 任务id
     * @return json
     */
    @GetMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<JSONObject> delete(@ValidatorItem(msg = "i18n.missing_task_id.12a7") String id, HttpServletRequest request) {
        FileReleaseTaskLogModel taskLogModel = fileReleaseTaskService.getByKey(id, request);
        Assert.notNull(taskLogModel, I18nMessageUtil.get("i18n.task_not_exist.47e9"));

        FileReleaseTaskLogModel fileReleaseTaskLogModel = new FileReleaseTaskLogModel();
        fileReleaseTaskLogModel.setTaskId(taskLogModel.getId());
        List<FileReleaseTaskLogModel> logModels = fileReleaseTaskService.listByBean(fileReleaseTaskLogModel);
        if (logModels != null) {
            List<String> ids = logModels.stream()
                .map(logModel -> {
                    File file = fileReleaseTaskService.logFile(logModel);
                    FileUtil.del(file);
                    return logModel.getId();
                })
                .collect(Collectors.toList());
            fileReleaseTaskService.delByKey(ids, null);
        }
        File taskDir = fileReleaseTaskService.logTaskDir(taskLogModel);
        FileUtil.del(taskDir);
        //
        fileReleaseTaskService.delByKey(taskLogModel.getId());
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }

    /**
     * 获取日志
     *
     * @param id   id
     * @param line 需要获取的行号
     * @return json
     */
    @GetMapping(value = "log-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> log(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                                        @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "i18n.line_number_error.c65d") int line,
                                        HttpServletRequest request) {
        FileReleaseTaskLogModel item = fileReleaseTaskService.getByKey(id, request);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        File file = fileReleaseTaskService.logFile(item);
        if (!FileUtil.isFile(file)) {
            return new JsonMessage<>(405, I18nMessageUtil.get("i18n.no_log_info_or_log_file_error.2c25"));
        }

        JSONObject data = FileUtils.readLogFile(file, line);
        // 运行中
        Integer status = item.getStatus();
        data.put("run", status != null && (status == 0 || status == 1));
        data.put("status", item.getStatus());
        return JsonMessage.success("", data);
    }

}
