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
package io.jpom.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.JpomManifest;
import io.jpom.common.JsonMessage;
import io.jpom.common.RemoteVersion;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.common.interceptor.NotAuthorize;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.model.data.NodeScriptModel;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.manage.ProjectInfoService;
import io.jpom.service.script.NodeScriptServer;
import io.jpom.util.JvmUtil;
import io.jpom.util.OshiUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 首页
 *
 * @author jiangzeyin
 * @since 2019/4/17
 */
@RestController
@Slf4j
public class IndexController extends BaseAgentController {

    private final ProjectInfoService projectInfoService;
    private final NodeScriptServer nodeScriptServer;

    public IndexController(ProjectInfoService projectInfoService,
                           NodeScriptServer nodeScriptServer) {
        this.projectInfoService = projectInfoService;
        this.nodeScriptServer = nodeScriptServer;
    }

    @RequestMapping(value = {"index", "", "index.html", "/"}, produces = MediaType.TEXT_PLAIN_VALUE)
    @NotAuthorize
    public String index() {
        return "Jpom-Agent,Can't access directly,Please configure it to JPOM server";
    }

    @RequestMapping(value = "info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> info() {

        JpomManifest instance = JpomManifest.getInstance();
        RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("manifest", instance);
        jsonObject.put("remoteVersion", remoteVersion);
        jsonObject.put("pluginSize", PluginFactory.size());
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 获取节点统计信息
     *
     * @return json
     */
    @PostMapping(value = "get-stat-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> getDirectTop() {
        JSONObject jsonObject = new JSONObject();
        JSONObject topInfo = OshiUtils.getSimpleInfo();
        jsonObject.put("simpleStatus", topInfo);

        JSONObject systemInfo = OshiUtils.getSystemInfo();
        jsonObject.put("systemInfo", systemInfo);

        JSONObject jpomInfo = this.getJpomInfo();
        jsonObject.put("jpomInfo", jpomInfo);
        return JsonMessage.success("ok", jsonObject);
    }

    private JSONObject getJpomInfo() {
        List<NodeProjectInfoModel> nodeProjectInfoModels = projectInfoService.list();
        List<NodeScriptModel> list = nodeScriptServer.list();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("javaVirtualCount", JvmUtil.getJavaVirtualCount());
        JpomManifest instance = JpomManifest.getInstance();
        jsonObject.put("jpomManifest", instance);
        jsonObject.put("javaVersion", SystemUtil.getJavaRuntimeInfo().getVersion());
        //  获取JVM中内存总大小
        jsonObject.put("totalMemory", SystemUtil.getTotalMemory());
        //
        jsonObject.put("freeMemory", SystemUtil.getFreeMemory());
        //
        jsonObject.put("projectCount", CollUtil.size(nodeProjectInfoModels));
        jsonObject.put("scriptCount", CollUtil.size(list));
        return jsonObject;
    }


    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<List<JSONObject>> getProcessList(String processName, Integer count) {
        processName = StrUtil.emptyToDefault(processName, "java");
        List<JSONObject> processes = OshiUtils.getProcesses(processName, Convert.toInt(count, 20));
        processes = processes.stream()
            .peek(jsonObject -> {
                int processId = jsonObject.getIntValue("processId");
                String port = AbstractProjectCommander.getInstance().getMainPort(processId);
                jsonObject.put("port", port);
                //
                try {
                    String jpomName = AbstractProjectCommander.getInstance().getJpomNameByPid(processId);
                    jsonObject.put("jpomName", jpomName);
                } catch (IOException e) {
                    log.error("解析进程失败", e);
                }
            })
            .collect(Collectors.toList());
        return JsonMessage.success("ok", processes);
    }


    @RequestMapping(value = "kill.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<Object> kill(int pid) {
        long jpomAgentId = JpomManifest.getInstance().getPid();
        Assert.state(!StrUtil.equals(StrUtil.toString(jpomAgentId), StrUtil.toString(pid)), "不支持在线关闭 Agent 进程");
        String result = AbstractSystemCommander.getInstance().kill(null, pid);
        if (StrUtil.isEmpty(result)) {
            result = "成功kill";
        }
        return JsonMessage.success(result);
    }
}
