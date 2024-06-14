/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.user.UserPermissionGroupBean;
import org.dromara.jpom.oauth2.BaseOauth2Config;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.dromara.jpom.service.user.UserBindWorkspaceService;
import org.dromara.jpom.service.user.UserPermissionGroupServer;
import org.dromara.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2022/8/3
 */
@RestController
@RequestMapping(value = "/user-permission-group")
@Feature(cls = ClassFeature.USER_PERMISSION_GROUP)
@SystemPermission
public class UserPermissionGroupController extends BaseServerController {

    private final UserPermissionGroupServer userPermissionGroupServer;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final UserService userService;
    private final SystemParametersServer systemParametersServer;

    public UserPermissionGroupController(UserPermissionGroupServer userPermissionGroupServer,
                                         UserBindWorkspaceService userBindWorkspaceService,
                                         UserService userService,
                                         SystemParametersServer systemParametersServer) {
        this.userPermissionGroupServer = userPermissionGroupServer;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.userService = userService;
        this.systemParametersServer = systemParametersServer;
    }

    /**
     * 分页查询权限组
     *
     * @return json
     */
    @RequestMapping(value = "get-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<UserPermissionGroupBean>> getUserList(HttpServletRequest request) {
        PageResultDto<UserPermissionGroupBean> userModelPageResultDto = userPermissionGroupServer.listPage(request);
        return new JsonMessage<>(200, "", userModelPageResultDto);
    }

    /**
     * 查询所有权限组
     *
     * @return json
     */
    @GetMapping(value = "get-list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<UserPermissionGroupBean>> getListAll() {
        List<UserPermissionGroupBean> list = userPermissionGroupServer.list();
        return new JsonMessage<>(200, "", list);
    }

    /**
     * 编辑权限组
     *
     * @return String
     */
    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> edit(String id,
                                     @ValidatorItem String name,
                                     String description,
                                     String prohibitExecute,
                                     String allowExecute,
                                     @ValidatorItem String workspace) {
        UserPermissionGroupBean userPermissionGroupBean = new UserPermissionGroupBean();
        userPermissionGroupBean.setName(name);
        userPermissionGroupBean.setDescription(description);
        //
        userPermissionGroupBean.setProhibitExecute(this.resolveProhibitExecute(prohibitExecute));
        userPermissionGroupBean.setAllowExecute(this.resolveAllowExecute(allowExecute));
        if (StrUtil.isEmpty(id)) {
            userPermissionGroupServer.insert(userPermissionGroupBean);
        } else {
            UserPermissionGroupBean permissionGroupBean = userPermissionGroupServer.getByKey(id);
            Assert.notNull(permissionGroupBean, I18nMessageUtil.get("i18n.data_does_not_exist.b201"));
            userPermissionGroupBean.setId(id);
            userPermissionGroupServer.updateById(userPermissionGroupBean);
        }
        //
        JSONArray jsonArray = JSONArray.parseArray(workspace);
        List<String> workspaceList = jsonArray.toJavaList(String.class);
        userBindWorkspaceService.updateUserWorkspace(userPermissionGroupBean.getId(), workspaceList);
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    private String resolveAllowExecute(String allowExecute) {
        if (StrUtil.isEmpty(allowExecute)) {
            return StrUtil.EMPTY;
        }
        JSONArray jsonArray = JSONArray.parseArray(allowExecute);
        return JSON.toJSONString(jsonArray.stream().map(o -> {
            JSONObject jsonObject = (JSONObject) o;
            String startTime = jsonObject.getString("startTime");
            String endTime = jsonObject.getString("endTime");
            if (StrUtil.hasEmpty(startTime, endTime)) {
                return null;
            }
            JSONArray week = jsonObject.getJSONArray("week");
            if (CollUtil.isEmpty(week)) {
                return null;
            }
            int[] weeks = week.stream().mapToInt(value -> {
                int week1 = Convert.toInt(value, 0);
                Assert.state(week1 >= 1 && week1 <= 7, I18nMessageUtil.get("i18n.selected_weekday_incorrect.4cd4"));
                return week1;
            }).toArray();
            //
            JSONObject result = new JSONObject();
            result.put("week", weeks);
            result.put("startTime", DateUtil.parseTimeToday(startTime).toString("HH:mm:ss"));
            result.put("endTime", DateUtil.parseTimeToday(endTime).toString("HH:mm:ss"));
            return result;
        }).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    private String resolveProhibitExecute(String prohibitExecute) {
        if (StrUtil.isEmpty(prohibitExecute)) {
            return StrUtil.EMPTY;
        }
        JSONArray jsonArray = JSONArray.parseArray(prohibitExecute);
        return JSON.toJSONString(jsonArray.stream().map(o -> {
            JSONObject jsonObject = (JSONObject) o;
            String startTime = jsonObject.getString("startTime");
            String endTime = jsonObject.getString("endTime");
            if (StrUtil.hasEmpty(startTime, endTime)) {
                return null;
            }
            JSONObject result = new JSONObject();
            result.put("startTime", DateUtil.parse(startTime).toString(DatePattern.NORM_DATETIME_FORMAT));
            result.put("endTime", DateUtil.parse(endTime).toString(DatePattern.NORM_DATETIME_FORMAT));
            result.put("reason", jsonObject.getString("reason"));
            return result;
        }).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    /**
     * 删除
     *
     * @param id 权限组
     * @return String
     */
    @GetMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> delete(String id) {
        UserPermissionGroupBean groupBean = userPermissionGroupServer.getByKey(id);
        Assert.notNull(groupBean, I18nMessageUtil.get("i18n.data_does_not_exist.b201"));
        // 判断是否绑定用户
        Entity entity = Entity.create();
        entity.set("permissionGroup", StrUtil.format(" like '%{}{}{}%'", StrUtil.AT, id, StrUtil.AT));
        long count = userService.count(entity);
        Assert.state(count == 0, I18nMessageUtil.get("i18n.user_binding_warning.16b0"));
        // 判断是否被 oauth2 绑定
        for (Map.Entry<String, Tuple> entry : BaseOauth2Config.DB_KEYS.entrySet()) {
            Tuple value = entry.getValue();
            String dbKey = value.get(0);
            BaseOauth2Config baseOauth2Config = systemParametersServer.getConfigDefNewInstance(dbKey, value.get(1));
            String permissionGroup = baseOauth2Config.getPermissionGroup();
            List<String> permissionGroupList = StrUtil.split(permissionGroup, StrUtil.AT, true, true);
            Assert.state(!CollUtil.contains(permissionGroupList, groupBean.getId()), StrUtil.format(I18nMessageUtil.get("i18n.oauth2_binding_warning.d8f0"), baseOauth2Config.provide()));
        }
        //
        userPermissionGroupServer.delByKey(id);
        // 删除工作空间
        userBindWorkspaceService.deleteByUserId(id);
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }
}
