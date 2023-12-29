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
package org.dromara.jpom.service.h2db;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.model.BaseNodeModel;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.dromara.jpom.system.AgentException;
import org.dromara.jpom.system.AuthorizeException;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bwcx_jzy
 * @since 2021/12/5
 */
@Slf4j
public abstract class BaseNodeService<T extends BaseNodeModel> extends BaseGlobalOrWorkspaceService<T> {

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
        // 验证节点
        String nodeId = paramMap.get(BaseServerController.NODE_ID);
        Assert.notNull(nodeId, "没有选择节点ID");
        NodeService nodeService = SpringUtil.getBean(NodeService.class);
        NodeModel nodeModel = nodeService.getByKey(nodeId);
        Assert.notNull(nodeModel, "不存在对应的节点");
        Assert.state(StrUtil.equals(workspaceId, nodeModel.getWorkspaceId()), "节点的工作空间和操作的工作空间补一致");
        paramMap.remove("workspaceId");
        paramMap.put("nodeId", nodeId);
        paramMap.put("workspaceId:in", workspaceId + StrUtil.COMMA + ServerConst.WORKSPACE_GLOBAL);

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
                log.debug("{} 节点没有拉取到任何 {},但是删除了数据：{}", nodeModelName, dataName, del);
                return "节点没有拉取到任何" + dataName;
            }
            // 查询现在存在的项目
            T where = ReflectUtil.newInstance(this.tClass);
            // where.setWorkspaceId(nodeModel.getWorkspaceId());
            where.setNodeId(nodeModel.getId());
            List<T> cacheAll = super.listByBean(where);
            cacheAll = ObjectUtil.defaultIfNull(cacheAll, Collections.emptyList());
            Set<String> needDelete = new HashSet<>();
            Set<String> cacheIds = cacheAll.stream()
                .map(BaseNodeModel::dataId)
                .collect(Collectors.toSet());
            // 转换数据修改时间
            List<T> projectInfoModels = jsonArray.stream().map(o -> {
                    // modifyTime,createTime
                    JSONObject jsonObject = (JSONObject) o;
                    T t = jsonObject.to(tClass);
                    Opt.ofBlankAble(jsonObject.getString("createTime")).map(s -> {
                        try {
                            return DateUtil.parse(s);
                        } catch (Exception e) {
                            log.warn("数据创建时间格式不正确 {} {}", s, jsonObject);
                            return null;
                        }
                    }).ifPresent(s -> t.setCreateTimeMillis(s.getTime()));
                    //
                    Opt.ofBlankAble(jsonObject.getString("modifyTime")).map(s -> {
                        try {
                            return DateUtil.parse(s);
                        } catch (Exception e) {
                            log.warn("数据修改时间格式不正确 {} {}", s, jsonObject);
                            return null;
                        }
                    }).ifPresent(s -> t.setModifyTimeMillis(s.getTime()));
                    return t;
                })
                .peek(item -> this.fullData(item, nodeModel))
                // 只保留自己节点的数据
                .filter(t -> StrUtil.equals(t.getNodeId(), nodeModel.getId()))
                .filter(item -> {
                    if (StrUtil.equals(item.getWorkspaceId(), ServerConst.WORKSPACE_GLOBAL)) {
                        return true;
                    }
                    // 检查对应的工作空间 是否存在
                    return workspaceService.exists(new WorkspaceModel(item.getWorkspaceId()));
                })
                .filter(item -> {
                    if (StrUtil.equals(item.getWorkspaceId(), ServerConst.WORKSPACE_GLOBAL)) {
                        return true;
                    }
                    // 避免重复同步
                    return StrUtil.equals(nodeModel.getWorkspaceId(), item.getWorkspaceId());
                })
                .peek(item -> {
                    item.setNodeName(nodeModel.getName());
                    WorkspaceModel workspaceModel = workspaceService.getByKey(nodeModel.getWorkspaceId());
                    item.setWorkspaceName(Optional.ofNullable(workspaceModel).map(WorkspaceModel::getName).orElse("数据不存在"));
                    cacheIds.remove(item.dataId());
                    // 需要删除相反的工作空间的数据（避免出现一个脚本同步出2条数据的问题）
                    if (StrUtil.equals(item.getWorkspaceId(), ServerConst.WORKSPACE_GLOBAL)) {
                        needDelete.add(BaseNodeModel.fullId(nodeModel.getWorkspaceId(), nodeModel.getId(), item.dataId()));
                    } else {
                        needDelete.add(BaseNodeModel.fullId(ServerConst.WORKSPACE_GLOBAL, nodeModel.getId(), item.dataId()));
                    }
                })
                .collect(Collectors.toList());
            // 设置 临时缓存，便于放行检查
            BaseServerController.resetInfo(UserModel.EMPTY);
            //
            projectInfoModels.forEach(BaseNodeService.super::upsert);
            // 删除项目
            int delCount = 0;
            Set<String> strings = cacheIds.stream()
                .flatMap((Function<String, Stream<String>>) s -> Stream.of(
                    BaseNodeModel.fullId(nodeModel.getWorkspaceId(), nodeModel.getId(), s),
                    BaseNodeModel.fullId(ServerConst.WORKSPACE_GLOBAL, nodeModel.getId(), s)))
                .collect(Collectors.toSet());
            //
            needDelete.addAll(strings);
            if (CollUtil.isNotEmpty(needDelete)) {
                delCount = super.delByKey(needDelete, null);
            }
            int size = CollUtil.size(projectInfoModels);
            String format = StrUtil.format(
                "{} 节点拉取到 {} 个{},已经缓存 {} 个{},更新 {} 个{},删除 {} 个缓存",
                nodeModelName, CollUtil.size(jsonArray), dataName,
                CollUtil.size(cacheAll), dataName,
                size, dataName,
                delCount);
            this.refreshCacheStat(nodeModel.getId(), size);
            log.debug(format);
            return format;
        } catch (Exception e) {
            return this.checkException(e, nodeModelName);
        } finally {
            BaseServerController.removeEmpty();
        }
    }

    /**
     * 刷新缓存统计
     *
     * @param nodeId    节点id
     * @param dataCount 数据总数
     */
    protected void refreshCacheStat(String nodeId, int dataCount) {

    }

    protected String checkException(Exception e, String nodeModelName) {
        if (e instanceof AgentException) {
            AgentException agentException = (AgentException) e;
            log.error("{} 同步失败 {}", nodeModelName, agentException.getMessage());
            return "同步失败" + agentException.getMessage();
        } else if (e instanceof AuthorizeException) {
            AuthorizeException authorizeException = (AuthorizeException) e;
            log.error("{} 授权异常 {}", nodeModelName, authorizeException.getMessage());
            return "授权异常" + authorizeException.getMessage();
        }
//        else if (e instanceof JSONException) {
//            log.error("{} 消息解析失败 {}", nodeModelName, e.getMessage());
//            return "消息解析失败" + e.getMessage();
//        }
        log.error("同步节点" + dataName + "失败:" + nodeModelName, e);
        return "同步节点" + dataName + "失败" + e.getMessage();
    }

    /**
     * 同步节点的项目
     *
     * @param nodeModel 节点
     * @param id        项目id
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
        if (StrUtil.isEmpty(item.getNodeId())) {
            item.setNodeId(nodeModel.getId());
        }
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
        return this.delByWorkspace(request, entity -> {
            T data = ReflectUtil.newInstance(this.tClass);
            data.setNodeId(nodeId);
            data.dataId(dataId);
            Entity entity1 = dataBeanToEntity(data);
            entity.putAll(entity1);
        });


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
