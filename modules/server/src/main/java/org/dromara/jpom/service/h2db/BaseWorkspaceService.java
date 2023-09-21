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
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.user.UserBindWorkspaceService;
import org.springframework.util.Assert;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 工作空间 通用 service
 *
 * @author bwcx_jzy
 * @since 2021/8/13
 */
public abstract class BaseWorkspaceService<T extends BaseWorkspaceModel> extends BaseDbService<T> {
    /**
     * 是否可以进行排序
     */
    private final boolean canSort;
    /**
     * 有排序字段的默认排序规则
     */
    private static final Order[] SORT_DEFAULT_ORDERS = new Order[]{
        new Order("sortValue", Direction.DESC),
        new Order("createTimeMillis", Direction.DESC),
        new Order("modifyTimeMillis", Direction.DESC)
    };

    public BaseWorkspaceService() {
        super();
        this.canSort = ReflectUtil.hasField(this.tClass, "sortValue");
    }

    /**
     * 根据工作空间查询
     *
     * @param request 请求
     * @return list
     */
    public List<T> listByWorkspace(HttpServletRequest request) {
        String workspaceId = this.getCheckUserWorkspace(request);
        Entity entity = Entity.create();
        entity.set("workspaceId", workspaceId);
        List<Entity> entities = super.queryList(entity);
        return super.entityToBeanList(entities);
    }

    /**
     * 根据主键ID + 请信息查询
     *
     * @param keyValue ID
     * @param request  请求
     * @return data
     */
    public T getByKey(String keyValue, HttpServletRequest request) {
        String workspace = this.getCheckUserWorkspace(request);
        return super.getByKey(keyValue, true, entity -> entity.set("workspaceId", workspace));
    }

    /**
     * 根据主键ID + 请信息查询
     *
     * @param keyValue ID
     * @param request  请求
     * @param fill     是否填充数据
     * @return data
     */
    public T getByKey(String keyValue, HttpServletRequest request, boolean fill) {
        String workspace = this.getCheckUserWorkspace(request);
        return super.getByKey(keyValue, fill, entity -> entity.set("workspaceId", workspace));
    }

    /**
     * 根据主键ID + 用户ID
     *
     * @param keyValue  ID
     * @param userModel 用户
     * @return data
     */
    public T getByKey(String keyValue, UserModel userModel) {
        T byKey = super.getByKey(keyValue, false);
        if (byKey == null) {
            return null;
        }
        this.checkUserWorkspace(byKey.getWorkspaceId(), userModel);
        return byKey;
    }

    @Override
    protected void fillInsert(T t) {
        super.fillInsert(t);
        if (StrUtil.isEmpty(t.getWorkspaceId())) {
            // 自动绑定 工作空间ID
            ServletRequestAttributes servletRequestAttributes = BaseServerController.tryGetRequestAttributes();
            if (servletRequestAttributes != null) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                String workspaceId = this.getCheckUserWorkspace(request);
                t.setWorkspaceId(workspaceId);
            } else {
                t.setWorkspaceId(Const.WORKSPACE_DEFAULT_ID);
            }
        } else {
            // 检查权限
            String modifyUser = t.getModifyUser();
            if (!StrUtil.equals(modifyUser, UserModel.SYSTEM_ADMIN)) {
                this.checkUserWorkspace(t.getWorkspaceId());
            }
        }
    }

    /**
     * 获取我所有的空间
     *
     * @param request 请求对象
     * @return page
     */
    @Override
    public PageResultDto<T> listPage(HttpServletRequest request) {
        // 验证工作空间权限
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        String workspaceId = this.getCheckUserWorkspace(request);
        //Assert.hasText(workspaceId, "此接口需要传workspaceId！");
        paramMap.put("workspaceId", workspaceId);
        return super.listPage(paramMap);
    }

//    /**
//     * 根据 workspaceId获取空间变量列表
//     *
//     * @param workspaceId
//     * @return
//     */
//    public List<T> listByWorkspaceId(String workspaceId) {
//        Entity entity = Entity.create();
//        entity.set("workspaceId", workspaceId);
//        List<Entity> entities = super.queryList(entity);
//        return super.entityToBeanList(entities);
//    }

    /**
     * 删除
     *
     * @param keyValue 主键
     * @param request  请求信息
     * @return 影响行数
     */
    public int delByKey(String keyValue, HttpServletRequest request) {
        String workspace = this.getCheckUserWorkspace(request);
        return super.delByKey(keyValue, entity -> entity.set("workspaceId", workspace));
    }

    /**
     * 删除,根据工作空间删除
     *
     * @param consumer 回调
     * @param request  请求信息
     * @return 影响行数
     */
    public int delByWorkspace(HttpServletRequest request, Consumer<Entity> consumer) {
        String workspace = this.getCheckUserWorkspace(request);
        return super.delByKey(null, entity -> {
            entity.set("workspaceId", workspace);
            consumer.accept(entity);
            int size = entity.size();
            Assert.state(size > 1, "没有添加任何参数");
        });
    }

    public static String getWorkspaceId(HttpServletRequest request) {
        String workspaceId = request.getParameter(Const.WORKSPACE_ID_REQ_HEADER);
        if (StrUtil.isEmpty(workspaceId)) {
            workspaceId = ServletUtil.getHeader(request, Const.WORKSPACE_ID_REQ_HEADER, CharsetUtil.CHARSET_UTF_8);
        }
        return StrUtil.emptyToDefault(workspaceId, Const.WORKSPACE_DEFAULT_ID);
    }

    /**
     * 获取 工作空间ID 并判断是否有权限
     *
     * @param request 请求对象
     * @return 工作空间ID
     */
    public String getCheckUserWorkspace(HttpServletRequest request) {
        String workspaceId = getWorkspaceId(request);
        Assert.hasText(workspaceId, "请选择工作空间");
        //
        this.checkUserWorkspace(workspaceId);
        return workspaceId;
    }

    /**
     * 判断用户是否有对应工作空间权限
     *
     * @param workspaceId 工作空间ID
     */
    public void checkUserWorkspace(String workspaceId) {
        UserModel userModel = BaseServerController.getUserByThreadLocal();
        this.checkUserWorkspace(workspaceId, userModel);
    }

    /**
     * 判断用户是否有对应工作空间权限
     *
     * @param workspaceId 工作空间ID
     */
    protected void checkUserWorkspace(String workspaceId, UserModel userModel) {
        Assert.notNull(userModel, "没有对应的用户");
        if (StrUtil.equals(userModel.getId(), UserModel.SYSTEM_ADMIN)) {
            // 系统执行，发行检查
            return;
        }
        if (userModel.isSuperSystemUser()) {
            // 超级管理员
            return;
        }
        if (StrUtil.equals(workspaceId, ServerConst.WORKSPACE_GLOBAL)) {
            // 全局 ID 忽略
            return;
        }
        // 查询绑定的权限
        UserBindWorkspaceService userBindWorkspaceService = SpringUtil.getBean(UserBindWorkspaceService.class);
        boolean exists = userBindWorkspaceService.exists(userModel, workspaceId);
        Assert.state(exists, "没有对应的工作空间权限");
    }


    public List<T> listById(Collection<String> ids, HttpServletRequest request) {
        String workspaceId = this.getCheckUserWorkspace(request);
        return super.listById(ids, entity -> entity.set("workspaceId", workspaceId));
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
        return super.updateById(info, entity -> entity.set("workspaceId", workspaceId));
    }

    // 排序相关方法 ---------


    @Override
    protected Order[] defaultOrders() {
        if (canSort) {
            return SORT_DEFAULT_ORDERS;
        }
        return super.defaultOrders();
    }

    /**
     * 置顶
     *
     * @param id      数据ID
     * @param request 请求
     */
    public void sortToTop(String id, HttpServletRequest request) {
        Assert.state(canSort, "当前数据不支持排序");
        String workspaceId = this.getCheckUserWorkspace(request);
        String maxSql = "select max(sortValue) as sortValue from " + this.tableName + " where workspaceId=?";
        List<Entity> query = this.query(maxSql, workspaceId);
        float maxSortValue = Opt.ofEmptyAble(query)
            .map(CollUtil::getFirst)
            .map(entity -> entity.getFloat("sortValue"))
            .orElse(1f);
        //
        String updateSql = "update " + this.tableName + " set sortValue=? where id=? and workspaceId=?";
        this.execute(updateSql, maxSortValue + 0.0001f, id, workspaceId);
    }

    /**
     * 向上移
     *
     * @param id       数据ID
     * @param beforeId 前面一个数据的ID
     * @param request  请求
     */
    public void sortMoveUp(String id, String beforeId, HttpServletRequest request) {
        this.sortMove(id, beforeId, request, true);
    }

    /**
     * 向下移
     *
     * @param id      数据ID
     * @param afterId 后面一个数据的ID
     * @param request 请求
     */
    public void sortMoveDown(String id, String afterId, HttpServletRequest request) {
        this.sortMove(id, afterId, request, false);
    }

    /**
     * 移动排序方法
     *
     * @param id        数据ID
     * @param compareId 比较的数据ID
     * @param request   请求
     * @param up        true 上移
     */
    private void sortMove(String id, String compareId, HttpServletRequest request, boolean up) {
        Assert.state(canSort, "当前数据不支持排序");
        Assert.hasText(id, "数据id 不存在");
        Assert.hasText(compareId, "比较 id 不存在");
        String workspaceId = this.getCheckUserWorkspace(request);
        String sql = "select sortValue as sortValue from " + this.tableName + " where id=? and workspaceId=?";
        List<Entity> query = this.query(sql, compareId, workspaceId);
        float compareSortValue = Opt.ofEmptyAble(query)
            .map(CollUtil::getFirst)
            .map(entity -> entity.getFloat("sortValue"))
            .orElse(1f);
        String updateSql = "update " + this.tableName + " set sortValue=? where id=? and workspaceId=?";
        this.execute(updateSql, up ? compareSortValue + 0.0001f : compareSortValue - 0.0001f, id, workspaceId);
    }

    /**
     * load date group by group name
     *
     * @return list
     */
    public List<String> listGroup(HttpServletRequest request) {
        String workspaceId = getCheckUserWorkspace(request);
        String sql = "select `GROUP` from " + getTableName() + " where workspaceId=? group by `GROUP`";
        return super.listGroupByName(sql, "GROUP", workspaceId);
    }
}
