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
package io.jpom.controller.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.user.UserPermissionGroupBean;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.user.UserBindWorkspaceService;
import io.jpom.service.user.UserPermissionGroupServer;
import io.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    public UserPermissionGroupController(UserPermissionGroupServer userPermissionGroupServer,
                                         UserBindWorkspaceService userBindWorkspaceService,
                                         UserService userService) {
        this.userPermissionGroupServer = userPermissionGroupServer;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.userService = userService;
    }

    /**
     * 分页查询权限组
     *
     * @return json
     */
    @RequestMapping(value = "get-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<UserPermissionGroupBean>> getUserList() {
        PageResultDto<UserPermissionGroupBean> userModelPageResultDto = userPermissionGroupServer.listPage(getRequest());
        return new JsonMessage<>(200, "", userModelPageResultDto);
    }

    /**
     * 查询所有权限组
     *
     * @return json
     */
    @GetMapping(value = "get-list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<UserPermissionGroupBean>> getListAll() {
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
    public JsonMessage<String> edit(String id,
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
            Assert.notNull(permissionGroupBean, "数据不存在");
            userPermissionGroupBean.setId(id);
            userPermissionGroupServer.update(userPermissionGroupBean);
        }
        //
        JSONArray jsonArray = JSONArray.parseArray(workspace);
        List<String> workspaceList = jsonArray.toJavaList(String.class);
        userBindWorkspaceService.updateUserWorkspace(userPermissionGroupBean.getId(), workspaceList);
        return new JsonMessage<>(200, "操作成功");
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
                Assert.state(week1 >= 1 && week1 <= 7, "选择的周几不正确");
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
    public String delete(String id) {
        UserPermissionGroupBean groupBean = userPermissionGroupServer.getByKey(id);
        Assert.notNull(groupBean, "数据不存在");
        // 判断是否绑定用户
        Entity entity = Entity.create();
        entity.set("permissionGroup", StrUtil.format(" like '%{}{}{}%'", StrUtil.AT, id, StrUtil.AT));
        long count = userService.count(entity);
        Assert.state(count == 0, "当前权限组还绑定用户,不能删除");
        //
        userPermissionGroupServer.delByKey(id);
        // 删除工作空间
        userBindWorkspaceService.deleteByUserId(id);
        return JsonMessage.getString(200, "删除成功");
    }
}
