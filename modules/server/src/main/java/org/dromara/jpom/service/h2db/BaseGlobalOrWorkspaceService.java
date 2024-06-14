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
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.user.UserModel;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 支持全局贡献的数据
 *
 * @author bwcx_jzy
 * @since 2023/03/23
 */
public abstract class BaseGlobalOrWorkspaceService<T extends BaseWorkspaceModel> extends BaseWorkspaceService<T> {

    @Override
    public T getByKey(String keyValue, HttpServletRequest request) {
        String workspace = this.getCheckUserWorkspace(request);
        return super.getByKey(keyValue, true, entity -> entity.set("workspaceId", CollUtil.newArrayList(workspace, ServerConst.WORKSPACE_GLOBAL)));
    }

    /**
     * 获取列表
     *
     * @param request 请求对象
     * @return page
     */
    @Override
    public PageResultDto<T> listPage(HttpServletRequest request) {
        // 验证工作空间权限
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        String workspaceId = this.getCheckUserWorkspace(request);
        paramMap.put("workspaceId:in", workspaceId + StrUtil.COMMA + ServerConst.WORKSPACE_GLOBAL);
        return super.listPage(paramMap);
    }

//    public PageResultDto<T> listPage(HttpServletRequest request, Map<String, String> paramMap) {
//        String workspaceId = this.getCheckUserWorkspace(request);
//        paramMap.put("workspaceId:in", workspaceId + StrUtil.COMMA + ServerConst.WORKSPACE_GLOBAL);
//        return super.listPage(paramMap, false);
//    }

    @Override
    public List<T> listByWorkspace(HttpServletRequest request) {
        String workspaceId = this.getCheckUserWorkspace(request);
        Entity entity = Entity.create();
        entity.set("workspaceId", CollUtil.newArrayList(workspaceId, ServerConst.WORKSPACE_GLOBAL));
        List<Entity> entities = super.queryList(entity);
        return super.entityToBeanList(entities);
    }

    /**
     * 更新数据,根据ID+工作空间ID
     *
     * @param info    更新的数据
     * @param request 请求
     * @return 影响行数
     */
    public int updateById(T info, HttpServletRequest request) {
        String workspaceId = this.getCheckUserWorkspace(request);
        return super.updateById(info, entity -> entity.set("workspaceId", CollUtil.newArrayList(workspaceId, ServerConst.WORKSPACE_GLOBAL)));
    }

    @Override
    public int delByKey(String keyValue, HttpServletRequest request) {
        String workspaceId = this.getCheckUserWorkspace(request);
        return super.delByKey(keyValue, entity -> entity.set("workspaceId", CollUtil.newArrayList(workspaceId, ServerConst.WORKSPACE_GLOBAL)));
    }

    @Override
    public int delByWorkspace(HttpServletRequest request, Consumer<Entity> consumer) {
        String workspaceId = this.getCheckUserWorkspace(request);
        return super.delByKey(null, entity -> {
            entity.set("workspaceId", CollUtil.newArrayList(workspaceId, ServerConst.WORKSPACE_GLOBAL));
            consumer.accept(entity);
            int size = entity.size();
            Assert.state(size > 1, I18nMessageUtil.get("i18n.no_parameters_added.1721"));
        });
    }

    @Override
    public T getByKey(String keyValue, UserModel userModel) {
        return super.getByKey(keyValue, userModel);
    }

    /**
     * 查询数据，并判断管理和创建人
     *
     * @param keyValue 主机id
     * @param request  请求信息
     * @return data
     */
    public T getByKeyAndGlobal(String keyValue, HttpServletRequest request) {
        return this.getByKeyAndGlobal(keyValue, request, I18nMessageUtil.get("i18n.data_does_not_exist.b201"));
    }

    /**
     * 查询数据，并判断管理和创建人
     *
     * @param keyValue 主机id
     * @param request  请求信息
     * @return data
     */
    public T getByKeyAndGlobal(String keyValue, HttpServletRequest request, String errorMsg) {
        T byKey = this.getByKey(keyValue, request);
        Assert.notNull(byKey, errorMsg);
        UserModel userModel = BaseServerController.getUserByThreadLocal();
        Assert.notNull(userModel, I18nMessageUtil.get("i18n.not_logged_in.c89f"));
        if (StrUtil.equals(byKey.getWorkspaceId(), ServerConst.WORKSPACE_GLOBAL) && !userModel.isSystemUser()) {
            // 是全局共享数据，并且不是管理员
            Assert.state(StrUtil.equals(userModel.getId(), byKey.getCreateUser()), I18nMessageUtil.get("i18n.no_current_data_permission.17d7"));
        }
        return byKey;
    }

    public String covertGlobalWorkspace(HttpServletRequest request) {
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        boolean global = Convert.toBool(paramMap.get("global"), false);
        // 判断是否为全局模式
        return global ? ServerConst.WORKSPACE_GLOBAL : this.getCheckUserWorkspace(request);
    }

}
