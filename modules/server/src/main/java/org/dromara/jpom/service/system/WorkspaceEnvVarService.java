/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.system;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.WorkspaceEnvVarModel;
import org.dromara.jpom.service.ITriggerToken;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2021/12/10
 */
@Service
public class WorkspaceEnvVarService extends BaseWorkspaceService<WorkspaceEnvVarModel> implements ITriggerToken {


    /**
     * 获取我所有的空间
     *
     * @param request 请求对象
     * @return page
     */
    @Override
    public PageResultDto<WorkspaceEnvVarModel> listPage(HttpServletRequest request) {
        // 验证工作空间权限
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        String workspaceIds = BaseWorkspaceService.getWorkspaceId(request);
        List<String> split = StrUtil.split(workspaceIds, StrUtil.COMMA);
        for (String workspaceId : split) {
            checkUserWorkspace(workspaceId);
        }
        paramMap.remove("workspaceId");
        paramMap.put("workspaceId:in", workspaceIds);
        return super.listPage(paramMap);
    }

    /**
     * 获取工作空间下面的所有环境变量
     *
     * @param workspaceId 工作空间ID
     * @return map
     */
    public EnvironmentMapBuilder getEnv(String workspaceId) {
        Entity entity = Entity.create();
        entity.set("workspaceId", CollUtil.newArrayList(workspaceId, ServerConst.WORKSPACE_GLOBAL));
        List<WorkspaceEnvVarModel> list = super.listByEntity(entity);
        Map<String, EnvironmentMapBuilder.Item> map = CollStreamUtil.toMap(list, WorkspaceEnvVarModel::getName, workspaceEnvVarModel -> {
            Integer privacy = workspaceEnvVarModel.getPrivacy();
            return new EnvironmentMapBuilder.Item(workspaceEnvVarModel.getValue(), privacy != null && privacy == 1, false);
        });
        // java.lang.UnsupportedOperationException
        return EnvironmentMapBuilder.builder(map);
    }

    /**
     * 转化 工作空间环境变量
     *
     * @param workspaceId 工作空间
     * @param value       值
     * @return 如果存在值，则返回环境变量值。不存在则返回原始值
     */
    public String convertRefEnvValue(String workspaceId, String value) {
        //  "$ref.wEnv."
        if (StrUtil.isEmpty(value) || !StrUtil.startWithIgnoreCase(value, ServerConst.REF_WORKSPACE_ENV)) {
            return value;
        }
        Entity entity = Entity.create();
        entity.set("name", StrUtil.removePrefixIgnoreCase(value, ServerConst.REF_WORKSPACE_ENV));
        entity.set("workspaceId", CollUtil.newArrayList(workspaceId, ServerConst.WORKSPACE_GLOBAL));
        List<WorkspaceEnvVarModel> modelList = super.listByEntity(entity);
        WorkspaceEnvVarModel workspaceEnvVarModel = CollUtil.getFirst(modelList);
        if (workspaceEnvVarModel == null) {
            return value;
        }
        return workspaceEnvVarModel.getValue();
    }

    @Override
    public List<WorkspaceEnvVarModel> listByWorkspace(HttpServletRequest request) {
        String workspaceIds = BaseWorkspaceService.getWorkspaceId(request);
        List<String> split = StrUtil.splitTrim(workspaceIds, StrUtil.COMMA);
        for (String workspaceId : split) {
            checkUserWorkspace(workspaceId);
        }
        Entity entity = Entity.create();
        entity.set("workspaceId", split);
        List<Entity> entities = super.queryList(entity);
        return super.entityToBeanList(entities);
    }

    @Override
    public String typeName() {
        return getTableName();
    }
}
