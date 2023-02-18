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
package io.jpom.func.assets.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import io.jpom.common.JsonMessage;
import io.jpom.func.assets.model.MachineNodeModel;
import io.jpom.func.assets.server.MachineNodeServer;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/2/18
 */
@RestController
@RequestMapping(value = "/system/assets/machine")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE)
@SystemPermission
public class MachineNodeController {

    private final MachineNodeServer machineNodeServer;

    public MachineNodeController(MachineNodeServer machineNodeServer) {
        this.machineNodeServer = machineNodeServer;
    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<MachineNodeModel>> listJson(HttpServletRequest request) {
        PageResultDto<MachineNodeModel> pageResultDto = machineNodeServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }

    @GetMapping(value = "list-group", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<String>> listGroup() {
        String sql = "select `groupName` from " + machineNodeServer.getTableName() + " group by `groupName`";
        List<Entity> list = machineNodeServer.query(sql);
        // 筛选字段
        List<String> collect = list.stream()
            .map(entity -> {
                Object obj = entity.get("groupName");
                return StrUtil.toStringOrNull(obj);
            })
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        return JsonMessage.success("", collect);
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> save(HttpServletRequest request) {
        machineNodeServer.update(request);
        return JsonMessage.success("操作成功");
    }
}
