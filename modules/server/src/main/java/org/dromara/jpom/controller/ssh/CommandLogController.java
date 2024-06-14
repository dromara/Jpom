/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.ssh;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.CommandExecLogModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.node.ssh.CommandExecLogService;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;

/**
 * 命令执行日志
 *
 * @author bwcx_jzy
 * @since 2021/12/23
 */
@RestController
@RequestMapping(value = "/node/ssh_command_log")
@Feature(cls = ClassFeature.SSH_COMMAND_LOG)
public class CommandLogController extends BaseServerController {

    private final CommandExecLogService commandExecLogService;

    public CommandLogController(CommandExecLogService commandExecLogService) {
        this.commandExecLogService = commandExecLogService;
    }

    /**
     * 分页获取命令信息
     *
     * @return result
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<CommandExecLogModel>> page(HttpServletRequest request) {
        PageResultDto<CommandExecLogModel> page = commandExecLogService.listPage(request);
        return JsonMessage.success("", page);
    }

    /**
     * 删除日志记录
     *
     * @param id id
     * @return result
     * @api {POST} node/ssh_command_log/del 删除日志记录
     * @apiGroup node/ssh_command_log
     * @apiUse defResultJson
     * @apiParam {String} id 记录 id
     */
    @RequestMapping(value = "del", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> del(String id, HttpServletRequest request) {
        CommandExecLogModel execLogModel = commandExecLogService.getByKey(id, request);
        Assert.notNull(execLogModel, I18nMessageUtil.get("i18n.no_record.ff41"));
        File logFile = execLogModel.logFile();
        boolean fastDel = CommandUtil.systemFastDel(logFile);
        Assert.state(!fastDel, I18nMessageUtil.get("i18n.log_file_cleanup_failed.3a3b"));
        //
        commandExecLogService.delByKey(id);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    /**
     * 命令执行记录
     *
     * @param commandId 命令ID
     * @param batchId   批次ID
     * @return result
     * @api {GET}  node/ssh_command_log/batch_list 命令执行记录
     * @apiGroup node/ssh_command_log
     * @apiUse defResultJson
     * @apiParam {String} commandId 命令ID
     * @apiParam {String} batchId 批次ID
     * @apiSuccess {Object} commandExecLogModels 命令执行记录
     * @apiSuccess {String} commandExecLogModels.commandId 命令ID
     * @apiSuccess {String} commandExecLogModels.batchId 批次ID
     * @apiSuccess {String} commandExecLogModels.sshId ssh Id
     * @apiSuccess {Number} commandExecLogModels.status Status
     * @apiSuccess {String} commandExecLogModels.commandName 命令名称
     * @apiSuccess {String} commandExecLogModels.sshName ssh 名称
     * @apiSuccess {String} commandExecLogModels.params 参数
     * @apiSuccess {Number} commandExecLogModels.triggerExecType 触发类型 {0，手动，1 自动触发}
     * @apiSuccess {Boolean} commandExecLogModels.hasLog 日志文件是否存在
     */
    @GetMapping(value = "batch_list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<CommandExecLogModel>> batchList(@ValidatorItem String commandId, @ValidatorItem String batchId, HttpServletRequest request) {
        CommandExecLogModel commandExecLogModel = new CommandExecLogModel();
        String workspace = commandExecLogService.getCheckUserWorkspace(request);
        commandExecLogModel.setWorkspaceId(workspace);
        commandExecLogModel.setCommandId(commandId);
        commandExecLogModel.setBatchId(batchId);
        List<CommandExecLogModel> commandExecLogModels = commandExecLogService.listByBean(commandExecLogModel);

        return JsonMessage.success("", commandExecLogModels);
    }

    /**
     * 获取日志
     *
     * @param id   id
     * @param line 需要获取的行号
     * @return json
     * @api {POST} node/ssh_command_log/log 获取日志
     * @apiGroup node/ssh_command_log
     * @apiUse defResultJson
     * @apiParam {String} id 日志 id
     * @apiParam {Number} line 需要获取的行号
     * @apiSuccess {Boolean} run 运行状态
     */
    @RequestMapping(value = "log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> log(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                                        @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "i18n.line_number_error.c65d") int line, HttpServletRequest request) {
        CommandExecLogModel item = commandExecLogService.getByKey(id, request);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));

        File file = item.logFile();
        if (!FileUtil.exist(file)) {
            return JsonMessage.success(I18nMessageUtil.get("i18n.no_log_info.d551"));
        }
        Assert.state(FileUtil.isFile(file), I18nMessageUtil.get("i18n.log_file_error.473b"));

        JSONObject data = FileUtils.readLogFile(file, line);
        // 运行中
        Integer status = item.getStatus();
        data.put("run", status != null && status == CommandExecLogModel.Status.ING.getCode());

        return JsonMessage.success("", data);
    }

    /**
     * 下载日志
     *
     * @param logId 日志 id
     * @api {GET} node/ssh_command_log/download_log 下载日志
     * @apiGroup node/ssh_command_log
     * @apiUse defResultJson
     * @apiParam {String} logId 日志 id
     * @apiSuccess {File} file 日志文件
     */
    @RequestMapping(value = "download_log", method = RequestMethod.GET)
    @ResponseBody
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String logId, HttpServletRequest request, HttpServletResponse response) {
        CommandExecLogModel item = commandExecLogService.getByKey(logId, request);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        File logFile = item.logFile();
        if (!FileUtil.exist(logFile)) {
            return;
        }
        if (logFile.isFile()) {
            ServletUtil.write(response, logFile);
        }
    }
}
