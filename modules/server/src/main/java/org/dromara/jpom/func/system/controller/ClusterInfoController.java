/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.system.controller;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.stream.CollectorUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.func.system.model.ClusterInfoModel;
import org.dromara.jpom.func.system.service.ClusterInfoService;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.system.WorkspaceService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bwcx_jzy
 * @since 2023/8/20
 */
@RestController
@RequestMapping(value = "/cluster/")
@Feature(cls = ClassFeature.CLUSTER_INFO)
@SystemPermission()
@Slf4j
public class ClusterInfoController {

    private final ClusterInfoService clusterInfoService;
    private final WorkspaceService workspaceService;

    public ClusterInfoController(ClusterInfoService clusterInfoService,
                                 WorkspaceService workspaceService) {
        this.clusterInfoService = clusterInfoService;
        this.workspaceService = workspaceService;
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<ClusterInfoModel>> list(HttpServletRequest request) {
        PageResultDto<ClusterInfoModel> listPage = clusterInfoService.listPage(request);
        return JsonMessage.success("", listPage);
    }

    @GetMapping(value = "list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<ClusterInfoModel>> listAll() {
        List<ClusterInfoModel> list = clusterInfoService.list();
        return JsonMessage.success("", list);
    }

    /**
     * 查询所有可以管理的分组名
     *
     * @return json
     */
    @GetMapping(value = "list-link-groups")
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> listLinkGroups() {
        //
        List<String> all = clusterInfoService.listLinkGroups();
        // 查询集群已经绑定的分组
        List<ClusterInfoModel> list = clusterInfoService.list();
        Map<String, List<JSONObject>> map = list.stream()
            .map(clusterInfoModel -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", clusterInfoModel.getName());
                jsonObject.put("id", clusterInfoModel.getId());
                jsonObject.put("linkGroup", clusterInfoModel.getLinkGroup());
                return jsonObject;
            })
            .flatMap((Function<JSONObject, Stream<JSONObject>>) jsonObject -> {
                String string = jsonObject.getString("linkGroup");
                List<String> list1 = StrUtil.splitTrim(string, StrUtil.COMMA);
                return list1.stream()
                    .map(s -> {
                        JSONObject clone = jsonObject.clone();
                        clone.remove("linkGroup");
                        clone.put("group", s);
                        return clone;
                    });
            })
            .collect(CollectorUtil.groupingBy(o -> o.getString("group"), Collectors.toList()));
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("linkGroups", all);
        jsonObject.put("groupMap", map);

        return JsonMessage.success("", jsonObject);
    }

    /**
     * 修改集群
     *
     * @param id ID
     * @return json
     */
    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> edit(@ValidatorItem(msg = "数据 id 不能为空") String id,
                                     @ValidatorItem(msg = "请填写集群名称") String name,
                                     String url,
                                     @ValidatorItem(msg = "请选择关联分组") String linkGroup) {
        Opt.ofBlankAble(url).ifPresent(s -> Validator.validateUrl(s, "请填写正确的 url"));
        //
        List<String> list = StrUtil.splitTrim(linkGroup, StrUtil.COMMA);
        Assert.notEmpty(list, "请选择关联的分组");
        //
        ClusterInfoModel infoModel = new ClusterInfoModel();
        infoModel.setId(id);
        infoModel.setName(name);
        infoModel.setLinkGroup(linkGroup);
        infoModel.setUrl(url);
        clusterInfoService.updateById(infoModel);
        return JsonMessage.success("修改成功");
    }


    /**
     * 删除集群
     *
     * @param id ID
     * @return json
     */
    @GetMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission(superUser = true)
    public IJsonMessage<String> delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
        //
        ClusterInfoModel infoModel = clusterInfoService.getByKey(id);
        Assert.notNull(infoModel, "对应的集群不存在");
        Assert.state(!clusterInfoService.online(infoModel), "不能删除在线的集群");
        // 如果还有工作空间绑定,不能删除集群
        WorkspaceModel workspaceModel = new WorkspaceModel();
        workspaceModel.setClusterInfoId(infoModel.getId());
        long count = workspaceService.count(workspaceModel);
        Assert.state(count == 0, "当前集群还被工作空间绑定不能删除");
        //
        clusterInfoService.delByKey(id);
        return JsonMessage.success("删除成功");
    }
}
