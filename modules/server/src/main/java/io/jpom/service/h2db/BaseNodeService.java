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
package io.jpom.service.h2db;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.model.BaseNodeModel;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.service.node.NodeService;
import io.jpom.service.system.WorkspaceService;
import io.jpom.system.AgentException;
import io.jpom.system.AuthorizeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2021/12/5
 */
@Slf4j
public abstract class BaseNodeService<T extends BaseNodeModel> extends BaseWorkspaceService<T> {

    protected final NodeService nodeService;
    protected final WorkspaceService workspaceService;
    private final String dataName;

    protected BaseNodeService(NodeService nodeService,
                              WorkspaceService workspaceService,
                              String dataName) {
        this.nodeService = nodeService;
        this.workspaceService = workspaceService;
        this.dataName = dataName;
    }

    public PageResultDto<T> listPageNode(HttpServletRequest request) {
        // 验证工作空间权限
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        String workspaceId = this.getCheckUserWorkspace(request);
        paramMap.put("workspaceId", workspaceId);
        // 验证节点
        String nodeId = paramMap.get(BaseServerController.NODE_ID);
        Assert.notNull(nodeId, "没有选择节点ID");
        NodeService nodeService = SpringUtil.getBean(NodeService.class);
        NodeModel nodeModel = nodeService.getByKey(nodeId);
        Assert.notNull(nodeModel, "不存在对应的节点");
        paramMap.put("nodeId", nodeId);
        return super.listPage(paramMap);
    }


    /**
     * 同步所有节点的项目
     */
    public void syncAllNode() {
        ThreadUtil.execute(() -> {
            List<NodeModel> list = nodeService.list();
            if (CollUtil.isEmpty(list)) {
                log.debug("没有任何节点");
                return;
            }
            // 排序 避免项目被个节点绑定
            list.sort((o1, o2) -> {
                if (StrUtil.equals(o1.getWorkspaceId(), Const.WORKSPACE_DEFAULT_ID)) {
                    return 1;
                }
                if (StrUtil.equals(o2.getWorkspaceId(), Const.WORKSPACE_DEFAULT_ID)) {
                    return 1;
                }
                return 0;
            });
            for (NodeModel nodeModel : list) {
                this.syncNode(nodeModel);
            }
        });
    }


    /**
     * 同步节点的项目
     *
     * @param nodeModel 节点
     */
    public void syncNode(final NodeModel nodeModel) {
        ThreadUtil.execute(() -> this.syncExecuteNode(nodeModel));
    }

    /**
     * 同步执行 同步节点信息
     *
     * @param nodeModel 节点信息
     * @return json
     */
    public String syncExecuteNode(NodeModel nodeModel) {
        String nodeModelName = nodeModel.getName();
        if (!nodeModel.isOpenStatus()) {
            log.debug("{} 节点未启用", nodeModelName);
            return "节点未启用";
        }
        try {
            JSONArray jsonArray = this.getLitDataArray(nodeModel);
            if (CollUtil.isEmpty(jsonArray)) {
                Entity entity = Entity.create();
                entity.set("nodeId", nodeModel.getId());
                int del = super.del(entity);
                //
                log.debug("{} 节点没有拉取到任何{}", nodeModelName, dataName);
                return "节点没有拉取到任何" + dataName;
            }
            // 查询现在存在的项目
            T where = ReflectUtil.newInstance(this.tClass);
            where.setWorkspaceId(nodeModel.getWorkspaceId());
            where.setNodeId(nodeModel.getId());
            List<T> cacheAll = super.listByBean(where);
            cacheAll = ObjectUtil.defaultIfNull(cacheAll, Collections.EMPTY_LIST);
            Set<String> cacheIds = cacheAll.stream()
                .map(BaseNodeModel::dataId)
                .collect(Collectors.toSet());
            //
            List<T> projectInfoModels = jsonArray.toJavaList(this.tClass);
            List<T> models = projectInfoModels.stream()
                .peek(item -> this.fullData(item, nodeModel))
                .filter(item -> {
                    // 检查对应的工作空间 是否存在
                    return workspaceService.exists(new WorkspaceModel(item.getWorkspaceId()));
                })
                .filter(projectInfoModel -> {
                    // 避免重复同步
                    return StrUtil.equals(nodeModel.getWorkspaceId(), projectInfoModel.getWorkspaceId());
                })
                .peek(item -> cacheIds.remove(item.dataId()))
                .collect(Collectors.toList());
            // 设置 临时缓存，便于放行检查
            BaseServerController.resetInfo(UserModel.EMPTY);
            //
            models.forEach(BaseNodeService.super::upsert);
            // 删除项目
            Set<String> strings = cacheIds.stream()
                .map(s -> BaseNodeModel.fullId(nodeModel.getWorkspaceId(), nodeModel.getId(), s))
                .collect(Collectors.toSet());
            if (CollUtil.isNotEmpty(strings)) {
                super.delByKey(strings, null);
            }
            String format = StrUtil.format(
                "{} 节点拉取到 {} 个{},已经缓存 {} 个{},更新 {} 个{},删除 {} 个缓存",
                nodeModelName, CollUtil.size(jsonArray), dataName,
                CollUtil.size(cacheAll), dataName,
                CollUtil.size(models), dataName,
                CollUtil.size(strings));
            log.debug(format);
            return format;
        } catch (Exception e) {
            return this.checkException(e, nodeModelName);
        } finally {
            BaseServerController.removeEmpty();
        }
    }

    protected String checkException(Exception e, String nodeModelName) {
        if (e instanceof AgentException) {
            AgentException agentException = (AgentException) e;
            log.error("{} 同步失败 {}", nodeModelName, agentException.getMessage());
            return "同步失败" + agentException.getMessage();
        } else if (e instanceof AuthorizeException) {
            AuthorizeException authorizeException = (AuthorizeException) e;
            log.error("{} 同步失败 {}", nodeModelName, authorizeException.getMessage());
            return "同步失败" + authorizeException.getMessage();
        }
        log.error("同步节点" + dataName + "失败:" + nodeModelName, e);
        return "同步节点" + dataName + "失败" + e.getMessage();
    }

    /**
     * 同步节点的项目
     *
     * @param nodeModel 节点
     */
    public void syncNode(final NodeModel nodeModel, String id) {
        String nodeModelName = nodeModel.getName();
        if (!nodeModel.isOpenStatus()) {
            log.debug("{} 节点未启用", nodeModelName);
            return;
        }
        ThreadUtil.execute(() -> {
            try {
                JSONObject data = this.getItem(nodeModel, id);
                if (data == null) {
                    // 删除
                    String fullId = BaseNodeModel.fullId(nodeModel.getWorkspaceId(), nodeModel.getId(), id);
                    super.delByKey(fullId);
                    return;
                }
                T projectInfoModel = data.toJavaObject(this.tClass);
                this.fullData(projectInfoModel, nodeModel);
                // 设置 临时缓存，便于放行检查
                BaseServerController.resetInfo(UserModel.EMPTY);
                //
                super.upsert(projectInfoModel);
            } catch (Exception e) {
                this.checkException(e, nodeModelName);
            } finally {
                BaseServerController.removeEmpty();
            }
        });
    }

    /**
     * 填充数据ID
     *
     * @param item      对象
     * @param nodeModel 节点
     */
    private void fullData(T item, NodeModel nodeModel) {
        item.dataId(item.getId());
        item.setNodeId(nodeModel.getId());
        if (StrUtil.isEmpty(item.getWorkspaceId())) {
            item.setWorkspaceId(nodeModel.getWorkspaceId());
        }
        item.setId(item.fullId());
    }

    /**
     * 删除节点 工作空间缓存
     *
     * @param nodeId  节点
     * @param request 请求
     * @return 影响行数
     */
    public int delCache(String nodeId, HttpServletRequest request) {
        String checkUserWorkspace = this.getCheckUserWorkspace(request);
        Entity entity = Entity.create();
        entity.set("nodeId", nodeId);
        entity.set("workspaceId", checkUserWorkspace);
        return super.del(entity);
    }

    /**
     * 删除节点 工作空间缓存
     *
     * @param dataId  数据ID
     * @param nodeId  节点
     * @param request 请求
     * @return 影响行数
     */
    public int delCache(String dataId, String nodeId, HttpServletRequest request) {
        String checkUserWorkspace = this.getCheckUserWorkspace(request);
        T data = ReflectUtil.newInstance(this.tClass);
        data.setNodeId(nodeId);
        data.dataId(dataId);
        data.setWorkspaceId(checkUserWorkspace);
        Entity entity = super.dataBeanToEntity(data);
        return super.del(entity);
    }

    /**
     * 根据 节点和数据ID查询数据
     *
     * @param nodeId 节点ID
     * @param dataId 数据ID
     * @return data
     */
    @Override
    public T getData(String nodeId, String dataId) {
        T data = ReflectUtil.newInstance(this.tClass);
        data.setNodeId(nodeId);
        data.dataId(dataId);
        return super.queryByBean(data);
    }

    /**
     * 查询远端项目
     *
     * @param nodeModel 节点
     * @param id        项目ID
     * @return json
     */
    public abstract JSONObject getItem(NodeModel nodeModel, String id);

    /**
     * 查询列表数据
     *
     * @param nodeModel 节点
     * @return json
     */
    public abstract JSONArray getLitDataArray(NodeModel nodeModel);
}
