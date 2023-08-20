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
package org.dromara.jpom.func.system.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.Method;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.func.system.model.ClusterInfoModel;
import org.dromara.jpom.func.system.service.ClusterInfoService;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private final MachineDockerServer machineDockerServer;
    private final MachineNodeServer machineNodeServer;
    private final MachineSshServer machineSshServer;

    public ClusterInfoController(ClusterInfoService clusterInfoService,
                                 MachineDockerServer dockerServer,
                                 MachineNodeServer machineNodeServer,
                                 MachineSshServer machineSshServer) {
        this.clusterInfoService = clusterInfoService;
        this.machineDockerServer = dockerServer;
        this.machineNodeServer = machineNodeServer;
        this.machineSshServer = machineSshServer;
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


    /**
     * 查询所有可以管理的分组名
     *
     * @return json
     */
    @GetMapping(value = "list-link-groups")
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<String>> listLinkGroups() {
        List<String> nodeGroup = machineNodeServer.listGroupName();
        List<String> sshGroup = machineSshServer.listGroupName();
        List<String> dockerGroup = machineDockerServer.listGroupName();
        //
        List<String> all = new ArrayList<>();
        CollUtil.addAll(all, nodeGroup);
        CollUtil.addAll(all, sshGroup);
        CollUtil.addAll(all, dockerGroup);
        //
        all = all.stream()
            .distinct()
            .collect(Collectors.toList());
        return JsonMessage.success("", all);
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
                                     @ValidatorItem(msg = "请填写集群访问地址") String url,
                                     @ValidatorItem(msg = "请选择关联分组") String linkGroup) {
        Validator.validateUrl(url, "请填写正确的 url");
        //
        UrlBuilder urlBuilder = UrlBuilder.ofHttp(url);
        urlBuilder.addPath(ServerConst.CHECK_SYSTEM);
        HttpRequest httpRequest = HttpRequest.of(urlBuilder).method(Method.GET);
        try {
            JSONObject jsonObject = httpRequest.thenFunction(httpResponse -> {
                String body = httpResponse.body();
                return JSONObject.parseObject(body);
            });
            int code = jsonObject.getIntValue(JsonMessage.CODE);
            Assert.state(code == JsonMessage.DEFAULT_SUCCESS_CODE, () -> {
                String msg = jsonObject.getString(JsonMessage.MSG);
                msg = StrUtil.emptyToDefault(msg, jsonObject.toString());
                return "集群状态码异常：" + code + " " + msg;
            });
            //
            JSONObject data = jsonObject.getJSONObject("data");
            Assert.notNull(data, "集群响应信息不正确,请确认集群地址是正确的服务端地址");
            Assert.state(data.containsKey("routerBase") && data.containsKey("extendPlugins"), "填写的集群地址不正确");
        } catch (Exception e) {
            log.error("检查集群信息异常", e);
            throw new IllegalArgumentException("填写的集群地址检查异常,请确认集群地址是正确的服务端地址," + e.getMessage());
        }
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
        clusterInfoService.delByKey(id);
        return JsonMessage.success("删除成功");
    }
}
