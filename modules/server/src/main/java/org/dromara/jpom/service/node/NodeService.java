/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.node;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2021/12/4
 */
@Service
@Slf4j
public class NodeService extends BaseWorkspaceService<NodeModel> {

    private final SshService sshService;

    @Resource
    @Lazy
    private ProjectInfoCacheService projectInfoCacheService;

    public NodeService(SshService sshService) {
        this.sshService = sshService;
    }

    @Override
    protected void fillSelectResult(NodeModel data) {
        if (data != null) {
            data.setLoginPwd(null);
        }
    }


//    public boolean existsByUrl(String url, String workspaceId, String id) {
//
//        // 可能出现错误
//        NodeModel nodeModel1 = new NodeModel();
//        nodeModel1.setUrl(url);
//        nodeModel1.setWorkspaceId(workspaceId);
//        List<NodeModel> nodeModels = ObjectUtil.defaultIfNull(super.listByBean(nodeModel1), Collections.EMPTY_LIST);
//        Optional<NodeModel> any = nodeModels.stream().filter(nodeModel2 -> !StrUtil.equals(id, nodeModel2.getId())).findAny();
//        return any.isPresent();
//    }

    private NodeModel resolveNode(HttpServletRequest request) {
        // 创建对象
        NodeModel nodeModel = ServletUtil.toBean(request, NodeModel.class, true);
        String id = nodeModel.getId();
        Assert.hasText(id, I18nMessageUtil.get("i18n.node_id_not_found.2f9e"));
        Assert.hasText(nodeModel.getName(), I18nMessageUtil.get("i18n.node_name_required.5bdf"));
        // 兼容就数据判断
        String checkId = StrUtil.replace(id, StrUtil.DASHED, StrUtil.UNDERLINE);
        Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, I18nMessageUtil.get("i18n.node_id_required_and_format.5926"));

        Assert.hasText(nodeModel.getName(), I18nMessageUtil.get("i18n.node_name_required.ac0f"));
        String workspaceId = this.getCheckUserWorkspace(request);
        nodeModel.setWorkspaceId(workspaceId);

        // 判断 ssh
        String sshId = nodeModel.getSshId();
        if (StrUtil.isNotEmpty(sshId)) {
            SshModel byKey = sshService.getByKey(sshId, request);
            Assert.notNull(byKey, I18nMessageUtil.get("i18n.ssh_does_not_exist.5bec"));
            Entity entity = Entity.create();
            entity.set("sshId", sshId);
            entity.set("workspaceId", workspaceId);
            if (StrUtil.isNotEmpty(id)) {
                entity.set("id", StrUtil.format(" <> {}", id));
            }
            boolean exists = super.exists(entity);
            Assert.state(!exists, I18nMessageUtil.get("i18n.ssh_already_bound_to_other_node.2d4e"));
        }
        NodeModel update = new NodeModel();
        update.setId(id);
        update.setName(nodeModel.getName());
        update.setGroup(nodeModel.getGroup());
        update.setSshId(nodeModel.getSshId());
        update.setOpenStatus(nodeModel.getOpenStatus());
        return update;
    }

    /**
     * 修改 节点
     *
     * @param request 请求对象
     */
    public void update(HttpServletRequest request) {
        NodeModel nodeModel = this.resolveNode(request);
        this.updateById(nodeModel);
        // 同步项目
        projectInfoCacheService.syncNode(nodeModel);
    }


    public void existsNode(String workspaceId, String machineId) {
        //
        NodeModel where = new NodeModel();
        where.setWorkspaceId(workspaceId);
        where.setMachineId(machineId);
        NodeModel nodeModel = this.queryByBean(where);
        Assert.isNull(nodeModel, () -> I18nMessageUtil.get("i18n.node_already_exists_in_workspace.9499") + nodeModel.getName());
    }

    public boolean existsNode2(String workspaceId, String machineId) {
        //
        NodeModel where = new NodeModel();
        where.setWorkspaceId(workspaceId);
        where.setMachineId(machineId);
        return this.exists(where);
    }

    /**
     * 将节点信息同步到其他工作空间
     *
     * @param ids            多给节点ID
     * @param nowWorkspaceId 当前的工作空间ID
     * @param workspaceId    同步到哪个工作空间
     */
    public void syncToWorkspace(String ids, String nowWorkspaceId, String workspaceId) {
        StrUtil.splitTrim(ids, StrUtil.COMMA).forEach(id -> {
            NodeModel data = this.getByKey(id, false, entity -> entity.set("workspaceId", nowWorkspaceId));
            Assert.notNull(data, I18nMessageUtil.get("i18n.no_corresponding_node_info.cd24"));
            this.existsNode(workspaceId, data.getMachineId());
            // 不存在则添加节点
            data.setId(null);
            data.setWorkspaceId(workspaceId);
            data.setCreateTimeMillis(null);
            data.setModifyTimeMillis(null);
            data.setModifyUser(null);
            data.setLoginName(null);
            data.setUrl(null);
            data.setLoginPwd(null);
            data.setProtocol(null);
            data.setHttpProxy(null);
            data.setHttpProxyType(null);
            // ssh 不同步
            data.setSshId(null);
            this.insert(data);
        });
    }

    @Override
    protected void fillInsert(NodeModel nodeModel) {
        super.fillInsert(nodeModel);
        // 表中字段不能为空，设置为空字符串
        nodeModel.setLoginName(StrUtil.EMPTY);
        nodeModel.setLoginPwd(StrUtil.EMPTY);
        nodeModel.setProtocol(StrUtil.EMPTY);
        nodeModel.setUrl(StrUtil.EMPTY);
    }

    //    @Override
//    public void insertNotFill(NodeModel nodeModel) {
//        nodeModel.setWorkspaceId(StrUtil.emptyToDefault(nodeModel.getWorkspaceId(), Const.WORKSPACE_DEFAULT_ID));
//        this.fillNodeInfo(nodeModel);
//        super.insertNotFill(nodeModel);
//    }

//    /**
//     * 填充默认字段
//     *
//     * @param nodeModel 节点
//     */
//    private void fillNodeInfo(NodeModel nodeModel) {
//        nodeModel.setProtocol(StrUtil.emptyToDefault(nodeModel.getProtocol(), "http"));
//        nodeModel.setOpenStatus(ObjectUtil.defaultIfNull(nodeModel.getOpenStatus(), 0));
//    }


    public List<NodeModel> getNodeBySshId(String sshId) {
        NodeModel nodeModel = new NodeModel();
        nodeModel.setSshId(sshId);
        return super.listByBean(nodeModel);
    }

    @Override
    public NodeModel getData(String nodeId, String dataId) {
        return Opt.ofBlankAble(nodeId)
            .map(super::getByKey)
            .orElseGet(() -> Opt.ofBlankAble(dataId)
                .map(NodeService.super::getByKey)
                .orElse(null)
            );
    }

    public long countByMachine(String machineId) {
        NodeModel nodeModel = new NodeModel();
        nodeModel.setMachineId(machineId);
        return this.count(nodeModel);
    }
}
