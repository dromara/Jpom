/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.model.user.UserBindWorkspaceModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.model.user.UserPermissionGroupBean;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2021/12/4
 */
@Service
public class UserBindWorkspaceService extends BaseDbService<UserBindWorkspaceModel> {

    private final WorkspaceService workspaceService;
    private final UserPermissionGroupServer userPermissionGroupServer;

    /**
     * ssh 终端没有任何限制
     */
    public static final String SSH_COMMAND_NOT_LIMITED = "-sshCommandNotLimited";

    public UserBindWorkspaceService(WorkspaceService workspaceService,
                                    UserPermissionGroupServer userPermissionGroupServer) {
        this.workspaceService = workspaceService;
        this.userPermissionGroupServer = userPermissionGroupServer;
    }

    /**
     * 更新用户的工作空间信息
     *
     * @param userId    用户ID
     * @param workspace 工作空间信息
     */
    public void updateUserWorkspace(String userId, List<String> workspace) {
        Assert.notEmpty(workspace, I18nMessageUtil.get("i18n.no_workspace_info.75ae"));
        List<UserBindWorkspaceModel> list = new HashSet<>(workspace).stream()
            .filter(s -> {
                // 过滤
                s = StrUtil.removeSuffix(s, SSH_COMMAND_NOT_LIMITED);
                MethodFeature[] values = MethodFeature.values();
                for (MethodFeature value : values) {
                    s = StrUtil.removeSuffix(s, StrUtil.DASHED + value.name());
                }
                return workspaceService.exists(new WorkspaceModel(s));
            })
            .map(s -> {
                UserBindWorkspaceModel userBindWorkspaceModel = new UserBindWorkspaceModel();
                userBindWorkspaceModel.setWorkspaceId(s);
                userBindWorkspaceModel.setUserId(userId);
                userBindWorkspaceModel.setId(UserBindWorkspaceModel.getId(userId, s));
                return userBindWorkspaceModel;
            })
            .collect(Collectors.toList());
        // 删除之前的数据
        UserBindWorkspaceModel userBindWorkspaceModel = new UserBindWorkspaceModel();
        userBindWorkspaceModel.setUserId(userId);
        super.del(super.dataBeanToEntity(userBindWorkspaceModel));
        // 重新入库
        super.insert(list);
    }

    /**
     * 查询用户绑定的工作空间
     *
     * @param userId 用户ID
     * @return list
     */
    public List<UserBindWorkspaceModel> listUserWorkspace(String userId) {
        UserBindWorkspaceModel userBindWorkspaceModel = new UserBindWorkspaceModel();
        userBindWorkspaceModel.setUserId(userId);
        return super.listByBean(userBindWorkspaceModel);
    }

    /**
     * 判断对应的工作空间是否被用户绑定
     *
     * @param workspaceId 工作空间ID
     * @return true 有用户绑定
     */
    public boolean existsWorkspace(String workspaceId) {
        UserBindWorkspaceModel userBindWorkspaceModel = new UserBindWorkspaceModel();
        userBindWorkspaceModel.setWorkspaceId(workspaceId);
        return super.exists(userBindWorkspaceModel);
    }

    /**
     * 查询用户绑定的工作空间
     *
     * @param userModel 用户
     * @return list
     */
    public List<WorkspaceModel> listUserWorkspaceInfo(UserModel userModel) {
        if (userModel.isSuperSystemUser()) {
            // 超级管理员有所有工作空间权限
            return workspaceService.list();
        }
        String permissionGroup = userModel.getPermissionGroup();
        List<String> list = StrUtil.splitTrim(permissionGroup, StrUtil.AT);
        list = ObjectUtil.defaultIfNull(list, new ArrayList<>());
        // 兼容旧代码
        list.add(userModel.getId());
        Entity entity = Entity.create();
        entity.set("userId", list);
        List<UserBindWorkspaceModel> userBindWorkspaceModels = super.listByEntity(entity);
        Assert.notEmpty(userBindWorkspaceModels, I18nMessageUtil.get("i18n.no_workspace_info_contact_admin_for_authorization.825f"));
        List<String> collect = userBindWorkspaceModels.stream().map(UserBindWorkspaceModel::getWorkspaceId).collect(Collectors.toList());
        return workspaceService.listById(collect);
    }

    /**
     * 删除
     *
     * @param userId 用户ID
     */
    public void deleteByUserId(String userId) {
        UserBindWorkspaceModel bindWorkspaceModel = new UserBindWorkspaceModel();
        bindWorkspaceModel.setUserId(userId);
        Entity where = super.dataBeanToEntity(bindWorkspaceModel);
        super.del(where);
    }

    /**
     * 查询用户 是否存在工作空间权限
     *
     * @param userModel   用户
     * @param workspaceId 工作空间
     * @return list
     */
    private List<UserBindWorkspaceModel> existsList(UserModel userModel, String workspaceId) {
        String permissionGroup = userModel.getPermissionGroup();
        List<String> list = StrUtil.splitTrim(permissionGroup, StrUtil.AT);
        list = list.stream()
            .map(s -> UserBindWorkspaceModel.getId(s, workspaceId))
            .collect(Collectors.toList());
        // 兼容旧数据
        list.add(UserBindWorkspaceModel.getId(userModel.getId(), workspaceId));
        return this.listById(list);
    }

    /**
     * 查询用户 是否存在工作空间权限
     *
     * @param userModel   用户
     * @param workspaceId 工作空间
     * @return true 存在
     */
    public boolean exists(UserModel userModel, String workspaceId) {
        List<UserBindWorkspaceModel> workspaceModels = this.existsList(userModel, workspaceId);
        return CollUtil.isNotEmpty(workspaceModels);
    }

    /**
     * 判断是否可以执行，并且验证时间段
     *
     * @param userModel   用户
     * @param workspaceId 工作空间ID
     * @return Permission Result
     */
    public UserBindWorkspaceModel.PermissionResult checkPermission(UserModel userModel, String workspaceId) {
        List<UserBindWorkspaceModel> workspaceModels = this.existsList(userModel, workspaceId);
        if (CollUtil.isEmpty(workspaceModels)) {
            return UserBindWorkspaceModel.PermissionResult.builder()
                .state(UserBindWorkspaceModel.PermissionResultEnum.FAIL)
                .msg(I18nMessageUtil.get("i18n.no_management_permission2.35d4"))
                .build();
        }
        List<String> permissionGroupIds = workspaceModels.stream()
            .map(UserBindWorkspaceModel::getUserId)
            .collect(Collectors.toList());
        List<UserPermissionGroupBean> permissionGroups = userPermissionGroupServer.listById(permissionGroupIds);
        if (CollUtil.isEmpty(permissionGroups)) {
            return UserBindWorkspaceModel.PermissionResult.builder()
                .state(UserBindWorkspaceModel.PermissionResultEnum.FAIL)
                .msg(I18nMessageUtil.get("i18n.no_management_permission.fd25"))
                .build();
        }
        // 判断禁止执行
        Optional<JSONObject> prohibitExecuteRule = this.findProhibitExecuteRule(permissionGroups);
        if (prohibitExecuteRule.isPresent()) {
            String msg = prohibitExecuteRule.map(jsonObject -> {
                String reason = jsonObject.getString("reason");
                String startTime = jsonObject.getString("startTime");
                String endTime = jsonObject.getString("endTime");
                if (StrUtil.isEmpty(reason)) {
                    return StrUtil.format(I18nMessageUtil.get("i18n.forbidden_operation_time_range.92bf"), startTime, endTime);
                }
                return StrUtil.format(I18nMessageUtil.get("i18n.forbidden_operation_range.247f"), reason, startTime, endTime);
            }).orElse(I18nMessageUtil.get("i18n.forbidden_operation_time.d83d"));
            return UserBindWorkspaceModel.PermissionResult.builder()
                .state(UserBindWorkspaceModel.PermissionResultEnum.MISS_PROHIBIT)
                .msg(msg)
                .build();
        }
        // 判断允许执行
        return this.checkAllowExecute(permissionGroups);
    }

    /**
     * 匹配可以执行的时间段
     *
     * @param permissionGroups 权限组
     * @return 结果
     */
    private UserBindWorkspaceModel.PermissionResult checkAllowExecute(List<UserPermissionGroupBean> permissionGroups) {
        List<JSONObject> allowExecuteListRule = permissionGroups.stream()
            .map(UserPermissionGroupBean::getAllowExecute)
            .filter(Objects::nonNull)
            .map(JSONArray::parseArray)
            .filter(CollUtil::isNotEmpty)
            .flatMap(jsonArray -> jsonArray.stream().map(o -> (JSONObject) o))
            .collect(Collectors.toList());
        if (CollUtil.isEmpty(allowExecuteListRule)) {
            // 没有配置规则，直接放行
            return UserBindWorkspaceModel.PermissionResult.builder().state(UserBindWorkspaceModel.PermissionResultEnum.SUCCESS).build();
        }
        Optional<JSONObject> allowExecuteRule = allowExecuteListRule.stream()
            .filter(jsonObject -> {
                DateTime now = DateTime.now();
                Week nowWeek = now.dayOfWeekEnum();
                int nowWeekInt = nowWeek.getIso8601Value();
                JSONArray week = jsonObject.getJSONArray("week");
                if (CollUtil.isEmpty(week)) {
                    return false;
                }
                if (!CollUtil.contains(week, nowWeekInt)) {
                    return false;
                }
                String startTime = jsonObject.getString("startTime");
                String endTime = jsonObject.getString("endTime");
                DateTime startDate = DateUtil.parseTimeToday(startTime);
                DateTime endDate = DateUtil.parseTimeToday(endTime);
                return DateUtil.isIn(DateTime.now(), startDate, endDate);
            })
            .findAny();
        if (allowExecuteRule.isPresent()) {
            // 允许执行
            return UserBindWorkspaceModel.PermissionResult.builder().state(UserBindWorkspaceModel.PermissionResultEnum.SUCCESS).build();
        }
        // 拼接限制规则
        String ruleStr = allowExecuteListRule.stream()
            .map(jsonObject -> {
                JSONArray week = jsonObject.getJSONArray("week");
                String weekStr = week.stream()
                    .map(o -> Convert.toInt(o, 0))
                    .map(weekInt -> {
                        DayOfWeek dayOfWeek = DayOfWeek.of(weekInt);
                        return Week.of(dayOfWeek);
                    })
                    .map(week1 -> week1.toChinese(StrUtil.EMPTY))
                    .collect(Collectors.joining(StrUtil.COMMA));
                String startTime = jsonObject.getString("startTime");
                String endTime = jsonObject.getString("endTime");
                return StrUtil.format(I18nMessageUtil.get("i18n.week_day_range_format.ebec"), weekStr, startTime, endTime);
            })
            .collect(Collectors.joining(StrUtil.SPACE));
        return UserBindWorkspaceModel.PermissionResult.builder()
            .state(UserBindWorkspaceModel.PermissionResultEnum.MISS_PERIOD)
            .msg(I18nMessageUtil.get("i18n.forbidden_operation_time_period.86a3") + ruleStr)
            .build();
    }

    /**
     * 匹配禁止执行规则
     *
     * @param permissionGroups 权限组配置
     * @return 找到的第一个禁止规则
     */
    private Optional<JSONObject> findProhibitExecuteRule(List<UserPermissionGroupBean> permissionGroups) {
        return permissionGroups.stream()
            .map(UserPermissionGroupBean::getProhibitExecute)
            .filter(Objects::nonNull)
            .map(JSONArray::parseArray)
            .filter(CollUtil::isNotEmpty)
            .flatMap(jsonArray -> jsonArray.stream().map(o -> (JSONObject) o))
            .filter(jsonObject -> {
                String startTime = jsonObject.getString("startTime");
                String endTime = jsonObject.getString("endTime");
                if (StrUtil.hasEmpty(startTime, endTime)) {
                    return false;
                }
                DateTime startDate = DateUtil.parse(startTime);
                DateTime endDate = DateUtil.parse(endTime);
                return DateUtil.isIn(DateTime.now(), startDate, endDate);
            })
            .findFirst();
    }
}
