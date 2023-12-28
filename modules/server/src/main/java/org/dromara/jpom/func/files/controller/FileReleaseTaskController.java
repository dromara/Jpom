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
package org.dromara.jpom.func.files.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.controller.outgiving.OutGivingWhitelistService;
import org.dromara.jpom.func.files.model.FileReleaseTaskLogModel;
import org.dromara.jpom.func.files.service.FileReleaseTaskService;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.model.data.ServerWhitelist;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.node.NodeService;
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

    public FileReleaseTaskController(FileReleaseTaskService fileReleaseTaskService,
                                     OutGivingWhitelistService outGivingWhitelistService,
                                     NodeService nodeService) {
        this.fileReleaseTaskService = fileReleaseTaskService;
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.nodeService = nodeService;
    }


    @PostMapping(value = "add-task", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> addTask(@ValidatorItem String fileId,
                                       @ValidatorItem String name,
                                       @ValidatorItem(value = ValidatorRule.NUMBERS) int taskType,
                                       @ValidatorItem String taskDataIds,
                                       @ValidatorItem String releasePathParent,
                                       @ValidatorItem String releasePathSecondary,
                                       String beforeScript,
                                       String afterScript,
                                       HttpServletRequest request) {
        // 判断参数
        ServerWhitelist configDeNewInstance = outGivingWhitelistService.getServerWhitelistData(request);
        List<String> whitelistServerOutGiving = configDeNewInstance.outGiving();
        Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, releasePathParent), "请选择正确的项目路径,或者还没有配置授权");
        Assert.hasText(releasePathSecondary, "请填写发布文件的二级目录");

        String releasePath = FileUtil.normalize(releasePathParent + StrUtil.SLASH + releasePathSecondary);

        return fileReleaseTaskService.addTask(fileId, name, taskType, taskDataIds, releasePath, beforeScript, afterScript, null, request);
    }


    @PostMapping(value = "re-task", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> reTask(@ValidatorItem String fileId,
                                       @ValidatorItem String name,
                                       @ValidatorItem(value = ValidatorRule.NUMBERS) int taskType,
                                       @ValidatorItem String taskDataIds,
                                       @ValidatorItem String parentTaskId,
                                       String beforeScript,
                                       String afterScript,
                                       HttpServletRequest request) {
        FileReleaseTaskLogModel parentTask = fileReleaseTaskService.getByKey(parentTaskId, request);
        Assert.notNull(parentTask, "父任务不存在");
        return fileReleaseTaskService.addTask(fileId, name, taskType, taskDataIds, parentTask.getReleasePath(), beforeScript, afterScript, null, request);
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
     * 取消任务
     *
     * @param id 任务id
     * @return json
     */
    @GetMapping(value = "cancel-task", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> hasFile(@ValidatorItem String id, HttpServletRequest request) {
        FileReleaseTaskLogModel taskLogModel = fileReleaseTaskService.getByKey(id, request);
        Assert.notNull(taskLogModel, "不存在对应的任务");
        fileReleaseTaskService.cancelTask(taskLogModel.getId());
        return JsonMessage.success("取消成功");
    }

    /**
     * 查询任务
     *
     * @param id 任务id
     * @return json
     */
    @GetMapping(value = "details", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> details(@ValidatorItem String id, HttpServletRequest request) {
        FileReleaseTaskLogModel taskLogModel = fileReleaseTaskService.getByKey(id, request);
        Assert.notNull(taskLogModel, "不存在对应的任务");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("taskData", taskLogModel);
        FileReleaseTaskLogModel fileReleaseTaskLogModel = new FileReleaseTaskLogModel();
        fileReleaseTaskLogModel.setTaskId(taskLogModel.getId());
        List<FileReleaseTaskLogModel> logModels = fileReleaseTaskService.listByBean(fileReleaseTaskLogModel);
        jsonObject.put("taskList", logModels);
        return JsonMessage.success("取消成功", jsonObject);
    }

    /**
     * 删除任务
     *
     * @param id 任务id
     * @return json
     */
    @GetMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> delete(@ValidatorItem String id, HttpServletRequest request) {
        FileReleaseTaskLogModel taskLogModel = fileReleaseTaskService.getByKey(id, request);
        Assert.notNull(taskLogModel, "不存在对应的任务");

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
        return JsonMessage.success("删除成功");
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
    public IJsonMessage<JSONObject> log(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                                       @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "line") int line,
                                       HttpServletRequest request) {
        FileReleaseTaskLogModel item = fileReleaseTaskService.getByKey(id, request);
        Assert.notNull(item, "没有对应数据");
        File file = fileReleaseTaskService.logFile(item);
        if (!FileUtil.isFile(file)) {
            return new JsonMessage<>(405, "还没有日志信息或者日志文件错误");
        }

        JSONObject data = FileUtils.readLogFile(file, line);
        // 运行中
        Integer status = item.getStatus();
        data.put("run", status != null && (status == 0 || status == 1));
        data.put("status", item.getStatus());
        return JsonMessage.success("", data);
    }

}
