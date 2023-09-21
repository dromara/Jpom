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

    public StatInfoTask(ChannelService channelService, ProjectInfoService projectInfoService, NodeScriptServer nodeScriptServer) {
        this.channelService = channelService;
        this.projectInfoService = projectInfoService;
        this.nodeScriptServer = nodeScriptServer;
    }

    @Scheduled(fixedDelay = 20, timeUnit = TimeUnit.SECONDS)
    public void pushStatInfo() {
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
