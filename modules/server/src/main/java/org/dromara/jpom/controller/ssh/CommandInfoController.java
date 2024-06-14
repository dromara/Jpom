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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.UrlRedirectUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.CommandExecLogModel;
import org.dromara.jpom.model.data.CommandModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.script.CommandParam;
import org.dromara.jpom.service.node.ssh.CommandExecLogService;
import org.dromara.jpom.service.node.ssh.SshCommandService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.dromara.jpom.util.CommandUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final SshCommandService sshCommandService;
    private final CommandExecLogService commandExecLogService;
    private final TriggerTokenLogServer triggerTokenLogServer;
    private final SshService sshService;

    public CommandInfoController(SshCommandService sshCommandService,
                                 CommandExecLogService commandExecLogService,
                                 TriggerTokenLogServer triggerTokenLogServer,
                                 SshService sshService) {
        this.sshCommandService = sshCommandService;
        this.commandExecLogService = commandExecLogService;
        this.triggerTokenLogServer = triggerTokenLogServer;
        this.sshService = sshService;
    }

    /**
     * 分页获取命令信息
     *
     * @return result
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<CommandModel>> page(HttpServletRequest request) {
        PageResultDto<CommandModel> page = sshCommandService.listPage(request);
        return JsonMessage.success("", page);
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
    public IJsonMessage<Object> edit(@RequestBody JSONObject data, HttpServletRequest request) {
        String name = data.getString("name");
        String command = data.getString("command");
        String desc = data.getString("desc");
        String defParams = data.getString("defParams");
        Assert.hasText(name, I18nMessageUtil.get("i18n.command_name_required.49fa"));
        Assert.hasText(command, I18nMessageUtil.get("i18n.command_content_required.6005"));
        String autoExecCron = this.checkCron(data.getString("autoExecCron"));
        String id = data.getString("id");
        //
        CommandModel commandModel = new CommandModel();
        commandModel.setName(name);
        commandModel.setCommand(command);
        commandModel.setDesc(desc);
        String sshIds = data.getString("sshIds");
        List<String> sshIdList = StrUtil.split(sshIds, StrUtil.COMMA, true, true);
        if (CollUtil.isNotEmpty(sshIdList)) {
            List<SshModel> commandModels = sshService.getByKey(sshIdList, request);
            Assert.state(CollUtil.size(sshIdList) == CollUtil.size(commandModels), I18nMessageUtil.get("i18n.associated_ssh_node_contains_nonexistent_node.c7f5"));
        }
        commandModel.setSshIds(sshIds);
        commandModel.setAutoExecCron(autoExecCron);
        //
        commandModel.setDefParams(CommandParam.checkStr(defParams));

        if (StrUtil.isEmpty(id)) {
            sshCommandService.insert(commandModel);
        } else {
            commandModel.setId(id);
            sshCommandService.updateById(commandModel, request);
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
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
    public IJsonMessage<Object> del(String id, HttpServletRequest request) {
        File logFileDir = CommandExecLogModel.logFileDir(id);
        boolean fastDel = CommandUtil.systemFastDel(logFileDir);
        Assert.state(!fastDel, I18nMessageUtil.get("i18n.log_file_cleanup_failed.3a3b"));
        //

        sshCommandService.delByKey(id, request);
        commandExecLogService.delByWorkspace(request, entity -> entity.set("commandId", id));
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
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
    public IJsonMessage<String> batch(String id,
                                      String params,
                                      @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.running_node_cannot_be_empty.ffc6") String nodes) throws IOException {
        Assert.hasText(id, I18nMessageUtil.get("i18n.execution_command_required.1cf3"));
        Assert.hasText(nodes, I18nMessageUtil.get("i18n.execution_node_required.d747"));
        String batchId = sshCommandService.executeBatch(id, params, nodes);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"), batchId);
    }

    /**
     * 同步到指定工作空间
     *
     * @param ids           节点ID
     * @param toWorkspaceId 分配到到工作空间ID
     * @return msg
     */
    @GetMapping(value = "sync-to-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission()
    public IJsonMessage<Object> syncToWorkspace(@ValidatorItem String ids, @ValidatorItem String toWorkspaceId, HttpServletRequest request) {
        String nowWorkspaceId = nodeService.getCheckUserWorkspace(request);
        //
        sshCommandService.checkUserWorkspace(toWorkspaceId);
        sshCommandService.syncToWorkspace(ids, nowWorkspaceId, toWorkspaceId);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    /**
     * get a trigger url
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "trigger-url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Map<String, String>> getTriggerUrl(String id, String rest, HttpServletRequest request) {
        CommandModel item = sshCommandService.getByKey(id, request);
        UserModel user = getUser();
        CommandModel updateInfo;
        if (StrUtil.isEmpty(item.getTriggerToken()) || StrUtil.isNotEmpty(rest)) {
            updateInfo = new CommandModel();
            updateInfo.setId(id);
            updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), sshCommandService.typeName(),
                item.getId(), user.getId()));
            sshCommandService.updateById(updateInfo);
        } else {
            updateInfo = item;
        }
        Map<String, String> map = this.getBuildToken(updateInfo, request);
        String string = I18nMessageUtil.get("i18n.reset_success.faa3");
        return JsonMessage.success(StrUtil.isEmpty(rest) ? "ok" : string, map);
    }

    private Map<String, String> getBuildToken(CommandModel item, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        String url = ServerOpenApi.SSH_COMMAND_TRIGGER_URL.
            replace("{id}", item.getId()).
            replace("{token}", item.getTriggerToken());
        String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
        Map<String, String> map = new HashMap<>(10);
        map.put("triggerUrl", FileUtil.normalize(triggerBuildUrl));
        String batchTriggerBuildUrl = String.format("/%s/%s", contextPath, ServerOpenApi.SSH_COMMAND_TRIGGER_BATCH);
        map.put("batchTriggerUrl", FileUtil.normalize(batchTriggerBuildUrl));

        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }
}
