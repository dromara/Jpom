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
package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.json.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.util.StringUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 魏宏斌
 * @since 2022/7/25
 */
@RestController
@RequestMapping(value = "build-config")
@Feature(cls = ClassFeature.BUILD_CONFIG)
@SystemPermission
@Slf4j
public class BuildConfigController extends BaseServerController {

    @GetMapping("runs")
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<JSONObject>> getRuns() throws Exception {
        // runs/%s/Dockerfile
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        Map<String, URI> runs = (Map<String, URI>) plugin.execute("listRuns");

        List<JSONObject> collect = runs.entrySet()
            .stream()
            .map(e -> {
                    URI value = e.getValue();
                    String content;
                    try {
                        content = FileUtil.readString(value.toURL(), CharsetUtil.CHARSET_UTF_8);
                    } catch (MalformedURLException ex) {
                        throw Lombok.sneakyThrow(ex);
                    }
                    return new JSONObject()
                        .set("name", e.getKey())
                        .set("path", value)
                        .set("content", content);
                }
            ).collect(Collectors.toList());
        return JsonMessage.success("ok", collect);
    }

    @PostMapping("runs")
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<Object> updateRuns(@ValidatorItem String name, @ValidatorItem String content) throws Exception {
        Validator.validateMatchRegex(StringUtil.GENERAL_STR, name, "镜像名称不合法");
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        plugin.execute("updateRuns", "name", name, "content", content);
        return JsonMessage.success("更新成功");
    }

    @DeleteMapping("runs/{name}")
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<Object> deleteRuns(@PathVariable String name) throws Exception {
        Validator.validateMatchRegex(StringUtil.GENERAL_STR, name, "镜像名称不合法");
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        plugin.execute("deleteRuns", "name", name);
        return JsonMessage.success("删除成功");
    }
}
