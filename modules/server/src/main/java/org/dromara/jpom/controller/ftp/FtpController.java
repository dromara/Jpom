/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package org.dromara.jpom.controller.ftp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.func.assets.server.MachineFtpServer;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.FtpModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.node.ftp.FtpService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wxy
 * @since 2025/05/29
 */
@RestController
@RequestMapping(value = "node/ftp")
@Feature(cls = ClassFeature.FTP)
@Slf4j
public class FtpController extends BaseServerController {

    private final FtpService ftpService;
    private final MachineFtpServer machineFtpServer;


    private final BuildInfoService buildInfoService;

    public FtpController(FtpService ftpService, MachineFtpServer machineFtpServer,
                         BuildInfoService buildInfoService) {
        this.ftpService = ftpService;
        this.machineFtpServer = machineFtpServer;
        this.buildInfoService = buildInfoService;
    }


    @PostMapping(value = "list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<FtpModel>> listData(HttpServletRequest request) {
        PageResultDto<FtpModel> pageResultDto = ftpService.listPage(request);
        pageResultDto.each(ftpModel -> {
            ftpModel.setMachineFtp(machineFtpServer.getByKey(ftpModel.getMachineFtpId()));
           /* List<NodeModel> nodeBySshId = nodeService.getNodeBySshId(ftpModel.getId());
            ftpModel.setLinkNode(CollUtil.getFirst(nodeBySshId));*/
        });
        return new JsonMessage<>(200, "", pageResultDto);
    }

    @GetMapping(value = "list_data_all.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<FtpModel>> listDataAll(HttpServletRequest request) {
        List<FtpModel> list = ftpService.listByWorkspace(request);
        return new JsonMessage<>(200, "", list);
    }

    @GetMapping(value = "list-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<Tree<String>> listTree(HttpServletRequest request) {
        List<FtpModel> list = ftpService.listByWorkspace(request);
        Map<String, TreeNode<String>> groupNode = new HashMap<>(4);
        List<TreeNode<String>> treeNodes = list.stream()
            .map(ftpModel -> {
                String group = ftpModel.getGroup();
                String groupId = SecureUtil.sha1(StrUtil.emptyToDefault(group, StrUtil.EMPTY));
                String groupId2 = StrUtil.format("g_{}", groupId);
                groupNode.computeIfAbsent(groupId, s -> new TreeNode<>(groupId2, StrUtil.SLASH, group, ftpModel.getName()));
                //
                TreeNode<String> treeNode = new TreeNode<>(ftpModel.getId(), groupId2, ftpModel.getName(), ftpModel.getName());
                Map<String, Object> extra = new HashMap<>();
                extra.put("fileDirs", ftpModel.getFileDirs());
                extra.put("isLeaf", true);
                treeNode.setExtra(extra);
                return treeNode;
            })
            .collect(Collectors.toList());
        //
        treeNodes.addAll(groupNode.values());
        Tree<String> tree = TreeUtil.buildSingle(treeNodes, StrUtil.SLASH);
        return new JsonMessage<>(200, "", tree);
    }

    @GetMapping(value = "get-item.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<FtpModel> getItem(@ValidatorItem String id, HttpServletRequest request) {
        FtpModel byKey = ftpService.getByKey(id, request);
        Assert.notNull(byKey, I18nMessageUtil.get("i18n.ssh_does_not_exist_with_message.de6c"));
        return new JsonMessage<>(200, "", byKey);
    }

    /**
     * 查询所有的分组
     *
     * @return list
     */
    @GetMapping(value = "list-group-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<String>> listGroupAll(HttpServletRequest request) {
        List<String> listGroup = ftpService.listGroup(request);
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
    public IJsonMessage<String> save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.parameter_error_ssh_name_cannot_be_empty.ff4f") String name,
                                     String id,
                                     String group,
                                     HttpServletRequest request) {
        FtpModel ftpModel = new FtpModel();
        ftpModel.setName(name);
        ftpModel.setGroup(group);
        ftpModel.setId(id);
        ftpService.updateById(ftpModel, request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }


    @PostMapping(value = "del.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> del(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id, HttpServletRequest request) {
        boolean checkSsh = buildInfoService.checkReleaseMethodByLike(id, request, BuildReleaseMethod.Ssh);
        Assert.state(!checkSsh, I18nMessageUtil.get("i18n.ssh_with_build_items_message.0f6d"));
        // 判断是否绑定节点
        List<NodeModel> nodeBySshId = nodeService.getNodeBySshId(id);
        Assert.state(CollUtil.isEmpty(nodeBySshId), I18nMessageUtil.get("i18n.ssh_bound_to_node_message.7b64"));

        ftpService.delByKey(id, request);
        //
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    @PostMapping(value = "del-fore", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission
    public IJsonMessage<Object> delFore(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id) {
        boolean checkSsh = buildInfoService.checkReleaseMethodByLike(id, BuildReleaseMethod.Ssh);
        Assert.state(!checkSsh, I18nMessageUtil.get("i18n.ssh_with_build_items_message.0f6d"));
        // 判断是否绑定节点
        List<NodeModel> nodeBySshId = nodeService.getNodeBySshId(id);
        Assert.state(CollUtil.isEmpty(nodeBySshId), I18nMessageUtil.get("i18n.ssh_bound_to_node_message.7b64"));

        ftpService.delByKey(id);
        //
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
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
    public IJsonMessage<Object> syncToWorkspace(@ValidatorItem String ids,
                                                @ValidatorItem String toWorkspaceId,
                                                HttpServletRequest request) {
        String nowWorkspaceId = nodeService.getCheckUserWorkspace(request);
        //
        ftpService.checkUserWorkspace(toWorkspaceId);
        ftpService.syncToWorkspace(ids, nowWorkspaceId, toWorkspaceId);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }
}
