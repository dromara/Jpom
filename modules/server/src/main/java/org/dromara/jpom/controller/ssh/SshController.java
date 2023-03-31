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

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.model.log.SshTerminalExecuteLog;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.SshTerminalExecuteLogService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2019/8/9
 */
@RestController
@RequestMapping(value = "node/ssh")
@Feature(cls = ClassFeature.SSH)
@Slf4j
public class SshController extends BaseServerController {

    private final SshService sshService;
    private final SshTerminalExecuteLogService sshTerminalExecuteLogService;
    private final BuildInfoService buildInfoService;
    private final MachineSshServer machineSshServer;

    public SshController(SshService sshService,
                         SshTerminalExecuteLogService sshTerminalExecuteLogService,
                         BuildInfoService buildInfoService,
                         MachineSshServer machineSshServer) {
        this.sshService = sshService;
        this.sshTerminalExecuteLogService = sshTerminalExecuteLogService;
        this.buildInfoService = buildInfoService;
        this.machineSshServer = machineSshServer;
    }


    @PostMapping(value = "list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<SshModel>> listData(HttpServletRequest request) {
        PageResultDto<SshModel> pageResultDto = sshService.listPage(request);
        pageResultDto.each(sshModel -> {
            sshModel.setMachineSsh(machineSshServer.getByKey(sshModel.getMachineSshId()));
            List<NodeModel> nodeBySshId = nodeService.getNodeBySshId(sshModel.getId());
            sshModel.setLinkNode(CollUtil.getFirst(nodeBySshId));
        });
        return new JsonMessage<>(200, "", pageResultDto);
    }

    @GetMapping(value = "list_data_all.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<SshModel>> listDataAll(HttpServletRequest request) {
        List<SshModel> list = sshService.listByWorkspace(request);
        return new JsonMessage<>(200, "", list);
    }

    @GetMapping(value = "get-item.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<SshModel> getItem(@ValidatorItem String id, HttpServletRequest request) {
        SshModel byKey = sshService.getByKey(id, request);
        Assert.notNull(byKey, "对应的 ssh 不存在");
        return new JsonMessage<>(200, "", byKey);
    }

    /**
     * 查询所有的分组
     *
     * @return list
     */
    @GetMapping(value = "list-group-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<String>> listGroupAll(HttpServletRequest request) {
        List<String> listGroup = sshService.listGroup(request);
        return JsonMessage.success("", listGroup);
    }

    /**
     * 编辑
     *
     * @param name    名称
     * @param group   分组名
     * @param request 请求对象
     * @param id      ID
     * @return json
     */
    @PostMapping(value = "save.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "ssh名称不能为空") String name,
                                    String id,
                                    String group,
                                    HttpServletRequest request) {
        SshModel sshModel = new SshModel();
        sshModel.setName(name);
        sshModel.setGroup(group);
        sshModel.setId(id);
        sshService.updateById(sshModel, request);
        return JsonMessage.success("操作成功");
    }


    @PostMapping(value = "del.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<Object> del(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id, HttpServletRequest request) {
        boolean checkSsh = buildInfoService.checkReleaseMethodByLike(id, request, BuildReleaseMethod.Ssh);
        Assert.state(!checkSsh, "当前ssh存在构建项，不能删除");
        // 判断是否绑定节点
        List<NodeModel> nodeBySshId = nodeService.getNodeBySshId(id);
        Assert.state(CollUtil.isEmpty(nodeBySshId), "当前ssh被节点绑定，不能删除");

        sshService.delByKey(id, request);
        //
        int logCount = sshTerminalExecuteLogService.delByWorkspace(request, entity -> entity.set("sshId", id));
        return JsonMessage.success("操作成功");
    }

    @PostMapping(value = "del-fore", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission
    public JsonMessage<Object> delFore(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id) {
        boolean checkSsh = buildInfoService.checkReleaseMethodByLike(id, BuildReleaseMethod.Ssh);
        Assert.state(!checkSsh, "当前ssh存在构建项，不能删除");
        // 判断是否绑定节点
        List<NodeModel> nodeBySshId = nodeService.getNodeBySshId(id);
        Assert.state(CollUtil.isEmpty(nodeBySshId), "当前ssh被节点绑定，不能删除");

        sshService.delByKey(id);
        //
        int logCount = sshTerminalExecuteLogService.delByKey(null, entity -> entity.set("sshId", id));
        return JsonMessage.success("操作成功");
    }

    /**
     * 执行记录
     *
     * @return json
     */
    @PostMapping(value = "log_list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SSH_TERMINAL_LOG, method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<SshTerminalExecuteLog>> logListData(HttpServletRequest request) {
        PageResultDto<SshTerminalExecuteLog> pageResult = sshTerminalExecuteLogService.listPage(request);
        return JsonMessage.success("获取成功", pageResult);
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
    public JsonMessage<Object> syncToWorkspace(@ValidatorItem String ids,
                                               @ValidatorItem String toWorkspaceId,
                                               HttpServletRequest request) {
        String nowWorkspaceId = nodeService.getCheckUserWorkspace(request);
        //
        sshService.checkUserWorkspace(toWorkspaceId);
        sshService.syncToWorkspace(ids, nowWorkspaceId, toWorkspaceId);
        return JsonMessage.success("操作成功");
    }
}
