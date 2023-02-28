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
package io.jpom.service.node;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import io.jpom.common.Const;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.service.h2db.BaseGroupService;
import io.jpom.service.node.ssh.SshService;
import lombok.extern.slf4j.Slf4j;
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
public class NodeService extends BaseGroupService<NodeModel> {

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
        Assert.hasText(id, "没有节点id");
        Assert.hasText(nodeModel.getName(), "请填写节点名称");
        // 兼容就数据判断
        String checkId = StrUtil.replace(id, StrUtil.DASHED, StrUtil.UNDERLINE);
        Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, "节点id不能为空并且2-50（英文字母 、数字和下划线）");

        Assert.hasText(nodeModel.getName(), "节点名称 不能为空");
        String workspaceId = this.getCheckUserWorkspace(request);
        nodeModel.setWorkspaceId(workspaceId);

        // 判断 ssh
        String sshId = nodeModel.getSshId();
        if (StrUtil.isNotEmpty(sshId)) {
            SshModel byKey = sshService.getByKey(sshId, request);
            Assert.notNull(byKey, "对应的 SSH 不存在");
            Entity entity = Entity.create();
            entity.set("sshId", sshId);
            entity.set("workspaceId", workspaceId);
            if (StrUtil.isNotEmpty(id)) {
                entity.set("id", StrUtil.format(" <> {}", id));
            }
            boolean exists = super.exists(entity);
            Assert.state(!exists, "对应的SSH已经被其他节点绑定啦");
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
        this.update(nodeModel);
        // 同步项目
        projectInfoCacheService.syncNode(nodeModel);
    }


    public void existsNode(String workspaceId, String machineId) {
        //
        NodeModel where = new NodeModel();
        where.setWorkspaceId(workspaceId);
        where.setMachineId(machineId);
        NodeModel nodeModel = this.queryByBean(where);
        Assert.isNull(nodeModel, () -> "对应工作空间已经存在该节点啦:" + nodeModel.getName());
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
            Assert.notNull(data, "没有对应到节点信息");
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
