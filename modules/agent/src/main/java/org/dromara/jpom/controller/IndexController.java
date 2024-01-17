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
package org.dromara.jpom.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.RemoteVersion;
import org.dromara.jpom.common.commander.ProjectCommander;
import org.dromara.jpom.common.commander.SystemCommander;
import org.dromara.jpom.common.interceptor.NotAuthorize;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.model.data.NodeScriptModel;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.dromara.jpom.service.script.NodeScriptServer;
import org.dromara.jpom.util.JvmUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页
 *
 * @author bwcx_jzy
 * @since 2019/4/17
 */
@RestController
@Slf4j
public class IndexController extends BaseAgentController {

    private final ProjectInfoService projectInfoService;
    private final NodeScriptServer nodeScriptServer;
    private final SystemCommander systemCommander;
    private final ProjectCommander projectCommander;

    public IndexController(ProjectInfoService projectInfoService,
                           NodeScriptServer nodeScriptServer,
                           SystemCommander systemCommander,
                           ProjectCommander projectCommander) {
        this.projectInfoService = projectInfoService;
        this.nodeScriptServer = nodeScriptServer;
        this.systemCommander = systemCommander;
        this.projectCommander = projectCommander;
    }

    @RequestMapping(value = {"index", "", "index.html", "/"}, produces = MediaType.TEXT_PLAIN_VALUE)
    @NotAuthorize
    public String index() {
        return "Jpom-Agent,Can't access directly,Please configure it to JPOM server";
    }

    @RequestMapping(value = "info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> info() {

        JpomManifest instance = JpomManifest.getInstance();
        cn.keepbx.jpom.RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("manifest", instance);
        jsonObject.put("remoteVersion", remoteVersion);
        jsonObject.put("pluginSize", PluginFactory.size());
        jsonObject.put("joinBetaRelease", RemoteVersion.betaRelease());
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 获取节点统计信息
     *
     * @return json
     */
    @PostMapping(value = "get-stat-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> getDirectTop() {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONObject topInfo = org.dromara.jpom.util.OshiUtils.getSimpleInfo();
            jsonObject.put("simpleStatus", topInfo);
            // 系统固定休眠时间
            jsonObject.put("systemSleep", org.dromara.jpom.util.OshiUtils.NET_STAT_SLEEP + org.dromara.jpom.util.OshiUtils.CPU_STAT_SLEEP);

            JSONObject systemInfo = org.dromara.jpom.util.OshiUtils.getSystemInfo();
            jsonObject.put("systemInfo", systemInfo);
        } catch (Exception e) {
            log.error("oshi 系统监控异常", e);
            jsonObject.put("oshiError", e.getMessage());
        }

        JSONObject jpomInfo = this.getJpomInfo();
        jsonObject.put("jpomInfo", jpomInfo);
        return JsonMessage.success("", jsonObject);
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
        Map<String, JSONObject> workspaceMap = new HashMap<>(4);
        //
        {
            for (NodeProjectInfoModel model : nodeProjectInfoModels) {
                JSONObject jsonObject1 = workspaceMap.computeIfAbsent(model.getWorkspaceId(), s -> {
                    JSONObject jsonObject11 = new JSONObject();
                    jsonObject11.put("projectCount", 0);
                    jsonObject11.put("scriptCount", 0);
                    return jsonObject11;
                });
                jsonObject1.merge("projectCount", 1, (v1, v2) -> Integer.sum((Integer) v1, (Integer) v2));
            }
            jsonObject.put("projectCount", CollUtil.size(nodeProjectInfoModels));
        }
        {
            for (NodeScriptModel model : list) {
                JSONObject jsonObject1 = workspaceMap.computeIfAbsent(model.getWorkspaceId(), s -> {
                    JSONObject jsonObject11 = new JSONObject();
                    jsonObject11.put("projectCount", 0);
                    jsonObject11.put("scriptCount", 0);
                    return jsonObject11;
                });
                jsonObject1.merge("scriptCount", 1, (v1, v2) -> Integer.sum((Integer) v1, (Integer) v2));
            }
            jsonObject.put("scriptCount", CollUtil.size(list));
        }
        jsonObject.put("workspaceStat", workspaceMap);
        return jsonObject;
    }


    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> getProcessList(String processName, Integer count) {
        try {
            processName = StrUtil.emptyToDefault(processName, "java");
            List<JSONObject> processes = org.dromara.jpom.util.OshiUtils.getProcesses(processName, Convert.toInt(count, 20));
            processes = processes.stream()
                .peek(jsonObject -> {
                    int processId = jsonObject.getIntValue("processId");
                    String port = projectCommander.getMainPort(processId);
                    jsonObject.put("port", port);
                    //
                })
                .collect(Collectors.toList());
            return JsonMessage.success("", processes);
        } catch (Exception e) {
            log.error("oshi 系统进程监控异常", e);
            throw new IllegalStateException("系统进程监控异常：" + e.getMessage());
        }
    }


    @PostMapping(value = "kill.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> kill(int pid) {
        long jpomAgentId = JpomManifest.getInstance().getPid();
        Assert.state(!StrUtil.equals(StrUtil.toString(jpomAgentId), StrUtil.toString(pid)), "不支持在线关闭 Agent 进程");
        String result = systemCommander.kill(null, pid);
        if (StrUtil.isEmpty(result)) {
            result = "成功kill";
        }
        return JsonMessage.success(result);
    }

    @PostMapping(value = "disk-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> diskInfo() {
        try {
            List<JSONObject> list = org.dromara.jpom.util.OshiUtils.fileStores();
            return JsonMessage.success("", list);
        } catch (Exception e) {
            log.error("oshi 文件系统资源监控异常", e);
            throw new IllegalStateException("文件系统监控异常：" + e.getMessage());
        }
    }

    @PostMapping(value = "hw-disk--info", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> hwDiskInfo() {
        try {
            List<JSONObject> list = org.dromara.jpom.util.OshiUtils.diskStores();
            return JsonMessage.success("", list);
        } catch (Exception e) {
            log.error("oshi 硬盘资源监控异常", e);
            throw new IllegalStateException("硬盘资源监控异常：" + e.getMessage());
        }
    }

    @PostMapping(value = "network-interfaces", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> networkInterfaces() {
        try {
            List<JSONObject> list = org.dromara.jpom.util.OshiUtils.networkInterfaces();
            return JsonMessage.success("", list);
        } catch (Exception e) {
            log.error("oshi 网卡资源监控异常", e);
            throw new IllegalStateException("网卡资源监控异常：" + e.getMessage());
        }
    }
}
