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
package io.jpom.controller.ssh;

import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.CommandExecLogModel;
import io.jpom.model.data.CommandModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.node.command.CommandExecLogService;
import io.jpom.service.node.command.CommandService;
import io.jpom.util.CommandUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 命令管理
 *
 * @author : Arno
 * @since : 2021/12/6 21:42
 */
@RestController
@RequestMapping(value = "/node/ssh_command")
@Feature(cls = ClassFeature.SSH_COMMAND)
public class CommandInfoController extends BaseServerController {

    private final CommandService commandService;
    private final CommandExecLogService commandExecLogService;

    public CommandInfoController(CommandService commandService,
                                 CommandExecLogService commandExecLogService) {
        this.commandService = commandService;
        this.commandExecLogService = commandExecLogService;
    }

    /**
     * 分页获取命令信息
     *
     * @return result
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String page() {
        PageResultDto<CommandModel> page = commandService.listPage(getRequest());
        return JsonMessage.getString(200, "", page);
    }

    /**
     * 新建/编辑命令
     *
     * @param data 命令信息
     * @return result
     * @api {POST} node/ssh_command/edit 新建/编辑命令
     * @apiGroup node/ssh_command
     * @apiUse defResultJson
     * @apiBody {String} name           命令名称
     * @apiBody {String} command        命令内容
     * @apiBody {String} [desc]         命令描述
     * @apiBody {String} defParams      默认参数
     * @apiBody {String} autoExecCron   定时构建表达式
     * @apiBody {String} id             命令主键 ID
     * @apiBody {String} [sshIds]       SSH 节点
     */
    @RequestMapping(value = "edit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String edit(@RequestBody JSONObject data) {
        String name = data.getString("name");
        String command = data.getString("command");
        String desc = data.getString("desc");
        String defParams = data.getString("defParams");
        Assert.hasText(name, "请输入命令名称");
        Assert.hasText(command, "请输入命令内容");
        String autoExecCron = data.getString("autoExecCron");
        String id = data.getString("id");
        //
        CommandModel commandModel = new CommandModel();
        commandModel.setName(name);
        commandModel.setCommand(command);
        commandModel.setDesc(desc);
        commandModel.setSshIds(data.getString("sshIds"));
        if (StrUtil.isNotEmpty(autoExecCron)) {
            try {
                new CronPattern(autoExecCron);
            } catch (Exception e) {
                throw new IllegalArgumentException("定时构建表达式格式不正确");
            }
        }
        commandModel.setAutoExecCron(autoExecCron);
        //
        if (StrUtil.isNotEmpty(defParams)) {
            List<CommandModel.CommandParam> params = CommandModel.params(defParams);
            if (params == null) {
                commandModel.setDefParams(StrUtil.EMPTY);
            } else {
                commandModel.setDefParams(JSONObject.toJSONString(params));
            }
        } else {
            commandModel.setDefParams(StrUtil.EMPTY);
        }

        if (StrUtil.isEmpty(id)) {
            commandService.insert(commandModel);
        } else {
            commandModel.setId(id);
            commandService.updateById(commandModel, getRequest());
        }
        return JsonMessage.getString(200, "操作成功");
    }

    /**
     * 删除命令
     *
     * @param id id
     * @return result
     * @api {DELETE} node/ssh_command/del 删除命令
     * @apiGroup node/ssh_command
     * @apiUse defResultJson
     * @apiParam {String} id 日志 id
     */
    @RequestMapping(value = "del", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public String del(String id) {
        File logFileDir = CommandExecLogModel.logFileDir(id);
        boolean fastDel = CommandUtil.systemFastDel(logFileDir);
        Assert.state(!fastDel, "清理日志文件失败");
        //
        HttpServletRequest request = getRequest();
        commandService.delByKey(id, request);
        commandExecLogService.delByWorkspace(request, entity -> entity.set("commandId", id));
        return JsonMessage.getString(200, "操作成功");
    }

    /**
     * 批量执行命令
     *
     * @return result
     * @api {POST} node/ssh_command/batch 批量执行命令
     * @apiGroup node/ssh_command
     * @apiUse defResultJson
     * @apiParam {String} id 命令 id
     * @apiParam {String} [params] 参数
     * @apiParam {String} nodes ssh节点
     * @apiSuccess {String} data batchId
     */
    @RequestMapping(value = "batch", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public String batch(String id,
                        String params,
                        @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "运行节点不能为空") String nodes) throws IOException {
        Assert.hasText(id, "请选择执行的命令");
        Assert.hasText(nodes, "请选择执行节点");
        String batchId = commandService.executeBatch(id, params, nodes);
        return JsonMessage.getString(200, "操作成功", batchId);
    }

    /**
     * 同步到指定工作空间
     *
     * @param ids         节点ID
     * @param workspaceId 分配到到工作空间ID
     * @return msg
     */
    @GetMapping(value = "sync-to-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission()
    public String syncToWorkspace(@ValidatorItem String ids, @ValidatorItem String workspaceId) {
        String nowWorkspaceId = nodeService.getCheckUserWorkspace(getRequest());
        //
        commandService.checkUserWorkspace(workspaceId);
        commandService.syncToWorkspace(ids, nowWorkspaceId, workspaceId);
        return JsonMessage.getString(200, "操作成功");
    }
}
