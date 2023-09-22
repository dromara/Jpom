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
package org.dromara.jpom.common.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.model.data.NodeScriptModel;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.dromara.jpom.service.script.NodeScriptServer;
import org.dromara.jpom.transport.netty.service.ChannelService;
import org.dromara.jpom.transport.protocol.PushStatInfoMessage;
import org.dromara.jpom.util.JvmUtil;
import org.dromara.jpom.util.OshiUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableScheduling
public class StatInfoTask {

    private final ChannelService channelService;
    private final ProjectInfoService projectInfoService;
    private final NodeScriptServer nodeScriptServer;
    private long count = 0;

    public StatInfoTask(ChannelService channelService, ProjectInfoService projectInfoService, NodeScriptServer nodeScriptServer) {
        this.channelService = channelService;
        this.projectInfoService = projectInfoService;
        this.nodeScriptServer = nodeScriptServer;
    }

    @Scheduled(fixedDelay = 20, timeUnit = TimeUnit.SECONDS)
    public void pushStatInfo() {
        if (count <= 2) {
            // 启动后开始的前3次不执行，避免未注册指定后服务器断开连接
            return;
        }
        JSONObject jsonObject = new JSONObject();
        JSONObject topInfo = OshiUtils.getSimpleInfo();
        jsonObject.put("simpleStatus", topInfo);
        // 系统固定休眠时间
        jsonObject.put("systemSleep", OshiUtils.NET_STAT_SLEEP + OshiUtils.CPU_STAT_SLEEP);

        JSONObject systemInfo = OshiUtils.getSystemInfo();
        jsonObject.put("systemInfo", systemInfo);

        JSONObject jpomInfo = this.getJpomInfo();
        jsonObject.put("jpomInfo", jpomInfo);
        jsonObject.put("installId", jpomInfo.getJSONObject("jpomManifest").getString("installId"));
        channelService.writeAndFlush(new PushStatInfoMessage(new HashMap<>(0), jsonObject.toJSONString()));
        count++;
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
}
