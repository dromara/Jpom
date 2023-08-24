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
package org.dromara.jpom.controller.ssh;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
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
    public IJsonMessage<PageResultDto<CommandExecLogModel>> page() {
        PageResultDto<CommandExecLogModel> page = commandExecLogService.listPage(getRequest());
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
    public IJsonMessage<String> del(String id) {
        CommandExecLogModel execLogModel = commandExecLogService.getByKey(id);
        Assert.notNull(execLogModel, "没有对应的记录");
        File logFile = execLogModel.logFile();
        boolean fastDel = CommandUtil.systemFastDel(logFile);
        Assert.state(!fastDel, "清理日志文件失败");
        //
        commandExecLogService.delByKey(id);
        return JsonMessage.success("操作成功");
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
    public IJsonMessage<List<CommandExecLogModel>> batchList(@ValidatorItem String commandId, @ValidatorItem String batchId) {
        CommandExecLogModel commandExecLogModel = new CommandExecLogModel();
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
    public IJsonMessage<JSONObject> log(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                                       @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "line") int line) {
        CommandExecLogModel item = commandExecLogService.getByKey(id, getRequest());
        Assert.notNull(item, "没有对应数据");

        File file = item.logFile();
        if (!FileUtil.exist(file)) {
            return JsonMessage.success("还没有日志信息");
        }
        Assert.state(FileUtil.isFile(file), "日志文件错误");

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
    public void downloadLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String logId, HttpServletResponse response) {
        CommandExecLogModel item = commandExecLogService.getByKey(logId);
        Assert.notNull(item, "没有对应数据");
        File logFile = item.logFile();
        if (!FileUtil.exist(logFile)) {
            return;
        }
        if (logFile.isFile()) {
            ServletUtil.write(response, logFile);
        }
    }
}
