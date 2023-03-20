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
package io.jpom.system.init;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.ILoadEvent;
import io.jpom.common.ISystemTask;
import io.jpom.common.JsonMessage;
import io.jpom.common.RemoteVersion;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.cron.CronUtils;
import io.jpom.model.data.NodeModel;
import io.jpom.script.BaseRunScript;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.script.NodeScriptExecuteLogServer;
import io.jpom.service.node.script.NodeScriptServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;

/**
 * 检查监控数据状态
 *
 * @author bwcx_jzy
 * @since 2019/7/14
 */
@Configuration
@Slf4j
public class ServerCheckMonitor implements ILoadEvent, ISystemTask {

    /**
     * 同步 节点的脚本模版日志
     *
     * @param nodeModel 节点
     */
    private void pullScriptLogItem(NodeModel nodeModel) {
        try {
            NodeScriptExecuteLogServer nodeScriptExecuteLogServer = SpringUtil.getBean(NodeScriptExecuteLogServer.class);
            Collection<String> strings = nodeScriptExecuteLogServer.syncExecuteNodeInc(nodeModel);
            if (strings == null) {
                return;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ids", strings);
            JsonMessage<Object> jsonMessage = NodeForward.requestBody(nodeModel, NodeUrl.SCRIPT_DEL_EXEC_LOG, jsonObject);
            if (!jsonMessage.success()) {
                log.error("删除脚本模版执行数据错误:{}", jsonMessage);
            }
        } catch (Exception e) {
            log.error("同步脚本异常", e);
        }
    }


    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        // 拉取 脚本模版日志
        CronUtils.upsert("pull_script_log", "0 0/1 * * * ?", () -> {
            NodeService nodeService = SpringUtil.getBean(NodeService.class);
            NodeScriptServer nodeScriptServer = SpringUtil.getBean(NodeScriptServer.class);
            List<String> nodeIds = nodeScriptServer.hasScriptNode();
            if (nodeIds == null) {
                return;
            }
            for (String nodeId : nodeIds) {
                NodeModel nodeModel = nodeService.getByKey(nodeId);
                if (nodeModel == null) {
                    continue;
                }
                ThreadUtil.execute(() -> this.pullScriptLogItem(nodeModel));
            }
        });
    }

    @Override
    public void executeTask() {
        // 尝试获取版本更新信息
        RemoteVersion.loadRemoteInfo();
        // 清空脚本缓存
        BaseRunScript.clearRunScript();
    }
}
