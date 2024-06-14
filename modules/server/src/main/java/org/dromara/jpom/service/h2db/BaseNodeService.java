/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.exception.AgentAuthorizeException;
import org.dromara.jpom.exception.AgentException;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.model.BaseNodeModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.system.WorkspaceService;

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

    @Override
    public List<T> listByWorkspace(HttpServletRequest request) {
        String workspaceId = this.getCheckUserWorkspace(request);
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        String nodeId = paramMap.get("nodeId");
        Entity entity = Entity.create();
        entity.set("workspaceId", CollUtil.newArrayList(workspaceId, ServerConst.WORKSPACE_GLOBAL));
        Opt.ofBlankAble(nodeId).ifPresent(s -> entity.set("nodeId", s));
        List<Entity> entities = super.queryList(entity);
        return super.entityToBeanList(entities);
    }

    /**
     * 同步所有节点的项目
     */
    public void syncAllNode() {
        ThreadUtil.execute(() -> {
            List<NodeModel> list = nodeService.list();
            if (CollUtil.isEmpty(list)) {
                log.debug(I18nMessageUtil.get("i18n.no_nodes.17b4"));
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
     * 检查孤独数据
     *
     * @param jsonArray 数据
     * @param machineId 机器 ID
     * @return list
     */
    protected List<T> checkLonelyDataArray(JSONArray jsonArray, String machineId) {
        if (CollUtil.isEmpty(jsonArray)) {
            return null;
        }
        // 分组
        Map<String, List<T>> map = jsonArray.stream().map(o -> {
            JSONObject jsonObject = (JSONObject) o;
            return jsonObject.to(tClass);
        }).collect(Collectors.groupingBy(
            t -> StrUtil.emptyToDefault(t.getNodeId(), StrUtil.EMPTY) + StrUtil.COMMA + t.getWorkspaceId(),
            Collectors.mapping(t -> t, Collectors.toList())
        ));
        // 查询不存在的节点
        Map<String, String> nodeIdMap = new HashMap<>();
        return map.entrySet()
            .stream()
            .filter(entry -> {
                String key = entry.getKey();
                if (StrUtil.startWith(key, StrUtil.COMMA)) {
                    // 旧数据没有节点 ID
                    List<String> list = StrUtil.splitTrim(key, StrUtil.COMMA);
                    String workspaceId = CollUtil.getLast(list);
                    NodeModel nodeModel = new NodeModel();
                    nodeModel.setMachineId(machineId);
                    nodeModel.setWorkspaceId(workspaceId);
                    // 更新推荐节点ID
                    NodeModel queryByBean = nodeService.queryByBean(nodeModel);
                    if (queryByBean != null) {
                        String beanId = queryByBean.getId();
                        String s = nodeIdMap.put(key, beanId);
                        if (StrUtil.isNotEmpty(s) && !StrUtil.equals(s, beanId)) {
                            // 对比已经存在的数据
                            log.error(I18nMessageUtil.get("i18n.project_data_workspace_id_inconsistency.7ed6"), key, s, beanId);
                        }
                    }
                    return true;
                }
                List<String> list = StrUtil.splitTrim(key, StrUtil.COMMA);
                if (CollUtil.size(list) != 2) {
                    return true;
                }
                String workspaceId = list.get(1);
                String id = list.get(0);
                if (StrUtil.equals(workspaceId, ServerConst.WORKSPACE_GLOBAL)) {
                    // 判断全局工作空间ID ,判断节点不存在
                    return !nodeService.exists(id);
                }
                NodeModel nodeModel = new NodeModel();
                nodeModel.setId(id);
                nodeModel.setWorkspaceId(workspaceId);
                return !nodeService.exists(nodeModel);

            })
            .peek(entry -> {
                String key = entry.getKey();
                String nodeId = nodeIdMap.get(key);
                if (nodeId != null) {
                    // 更新节点ID
                    List<T> value = entry.getValue();
                    for (T t : value) {
                        t.setNodeId(nodeId);
                    }
                }
            })
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toList());
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
            log.debug(I18nMessageUtil.get("i18n.node_not_enabled.10ef"), nodeModelName);
            return I18nMessageUtil.get("i18n.node_not_enabled.a14d");
        }
        try {
            JSONArray jsonArray = this.getLitDataArray(nodeModel);
            if (CollUtil.isEmpty(jsonArray)) {
                Entity entity = Entity.create();
                entity.set("nodeId", nodeModel.getId());
                int del = super.del(entity);
                //
                log.debug(I18nMessageUtil.get("i18n.node_no_data_pulled.0dae"), nodeModelName, dataName, del);
                return I18nMessageUtil.get("i18n.node_did_not_pull_anything.8af5") + dataName;
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
            List<T> projectInfoModels = jsonArray.stream()
                .map(o -> {
                    // modifyTime,createTime
                    JSONObject jsonObject = (JSONObject) o;
                    T t = jsonObject.to(tClass);
                    Opt.ofBlankAble(jsonObject.getString("createTime"))
                        .map(s -> {
                            try {
                                return DateUtil.parse(s);
                            } catch (Exception e) {
                                log.warn(I18nMessageUtil.get("i18n.data_creation_time_format_incorrect.7772"), s, jsonObject);
                                return null;
                            }
                        }).ifPresent(s -> t.setCreateTimeMillis(s.getTime()));
                    //
                    Opt.ofBlankAble(jsonObject.getString("modifyTime"))
                        .map(s -> {
                            try {
                                return DateUtil.parse(s);
                            } catch (Exception e) {
                                log.warn(I18nMessageUtil.get("i18n.data_modification_time_format_incorrect.7ffe"), s, jsonObject);
                                return null;
                            }
                        })
                        .ifPresent(s -> t.setModifyTimeMillis(s.getTime()));
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
                    item.setWorkspaceName(Optional.ofNullable(workspaceModel).map(WorkspaceModel::getName).orElse(I18nMessageUtil.get("i18n.data_does_not_exist.b201")));
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
            String template = I18nMessageUtil.get("i18n.physical_node_pull.874e");
            String format = StrUtil.format(
                template,
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
            log.error(I18nMessageUtil.get("i18n.synchronization_failed.091a"), nodeModelName, agentException.getMessage());
            return I18nMessageUtil.get("i18n.synchronization_failed.d610") + agentException.getMessage();
        } else if (e instanceof AgentAuthorizeException) {
            AgentAuthorizeException agentAuthorizeException = (AgentAuthorizeException) e;
            log.error(I18nMessageUtil.get("i18n.authorization_exception.acc0"), nodeModelName, agentAuthorizeException.getMessage());
            return I18nMessageUtil.get("i18n.auth_exception.27be") + agentAuthorizeException.getMessage();
        }
//        else if (e instanceof JSONException) {
//            log.error("{} 消息解析失败 {}", nodeModelName, e.getMessage());
//            return "消息解析失败" + e.getMessage();
//        }
        log.error(I18nMessageUtil.get("i18n.synchronization_node_failure_with_details.8660"), dataName, nodeModelName, e);
        return StrUtil.format(I18nMessageUtil.get("i18n.synchronization_node_failure.8a2c"), dataName, e.getMessage());
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
            log.debug(I18nMessageUtil.get("i18n.node_not_enabled.10ef"), nodeModelName);
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

    /**
     * 查询孤立的数据
     *
     * @param machineNodeModel 资产
     * @return json
     */
    public abstract List<T> lonelyDataArray(MachineNodeModel machineNodeModel);
}
