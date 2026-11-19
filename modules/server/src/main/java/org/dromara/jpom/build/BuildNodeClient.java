/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.Getter;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.transport.DataContentType;
import org.dromara.jpom.transport.DownloadCallback;
import org.dromara.jpom.transport.INodeInfo;
import org.dromara.jpom.transport.IProxyWebSocket;
import org.dromara.jpom.transport.IUrlItem;
import org.dromara.jpom.transport.TransportServerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 构建节点（Go 插件端）请求客户端。
 * <p>
 * 只负责「怎么把请求发到节点」，不做业务判断；协议字段见
 * {@code archive/2026-11-build-node-on-go-agent/PROTOCOL.md}。
 *
 * @author jpom
 * @since 2.11.15
 */
@Getter
public class BuildNodeClient {

    private final NodeModel nodeModel;
    private final INodeInfo nodeInfo;

    public BuildNodeClient(NodeModel nodeModel) {
        Assert.notNull(nodeModel, "nodeModel");
        this.nodeModel = nodeModel;
        this.nodeInfo = NodeForward.parseNodeInfo(nodeModel);
    }

    private IUrlItem urlItem(NodeUrl nodeUrl, DataContentType dataContentType) {
        return NodeForward.parseUrlItem(this.nodeInfo, this.nodeModel.getWorkspaceId(), nodeUrl, dataContentType);
    }

    /**
     * 读取节点信息（/info），失败抛异常
     *
     * @return /info 的 data
     */
    public JSONObject info() {
        String body = TransportServerFactory.get().execute(this.nodeInfo,
            this.urlItem(NodeUrl.Info, DataContentType.FORM_URLENCODED), null);
        return NodeForward.toJsonMessage(body, new TypeReference<cn.keepbx.jpom.model.JsonMessage<JSONObject>>() {
        }).getData();
    }

    /**
     * 提交构建任务
     *
     * @param job 任务体
     * @return 响应（code 可能是 409 节点繁忙 / 501 不支持）
     */
    public cn.keepbx.jpom.model.JsonMessage<JSONObject> start(JSONObject job) {
        String body = TransportServerFactory.get().execute(this.nodeInfo,
            this.urlItem(NodeUrl.Build_Start, DataContentType.JSON), job);
        return NodeForward.toJsonMessage(body, new TypeReference<cn.keepbx.jpom.model.JsonMessage<JSONObject>>() {
        });
    }

    /**
     * 查询任务状态
     *
     * @param jobId 任务 ID
     * @return 响应
     */
    public cn.keepbx.jpom.model.JsonMessage<JSONObject> status(String jobId) {
        JSONObject body = new JSONObject();
        body.put("jobId", jobId);
        String response = TransportServerFactory.get().execute(this.nodeInfo,
            this.urlItem(NodeUrl.Build_Status, DataContentType.JSON), body);
        return NodeForward.toJsonMessage(response, new TypeReference<cn.keepbx.jpom.model.JsonMessage<JSONObject>>() {
        });
    }

    /**
     * 取消任务
     *
     * @param jobId 任务 ID
     */
    public void cancel(String jobId) {
        JSONObject body = new JSONObject();
        body.put("jobId", jobId);
        String response = TransportServerFactory.get().execute(this.nodeInfo,
            this.urlItem(NodeUrl.Build_Cancel, DataContentType.JSON), body);
        NodeForward.toJsonMessage(response, new TypeReference<cn.keepbx.jpom.model.JsonMessage<JSONObject>>() {
        });
    }

    /**
     * 批量探测构建源码目录是否存在（远程构建时 Server 本地目录没有意义）
     *
     * @param buildModelIds 构建 ID 列表
     * @return 构建 ID -&gt; 是否存在
     */
    public Map<String, Boolean> sourceExists(List<String> buildModelIds) {
        Map<String, Boolean> result = new HashMap<>(CollUtil.size(buildModelIds));
        if (CollUtil.isEmpty(buildModelIds)) {
            return result;
        }
        JSONObject body = new JSONObject();
        body.put("buildModelIds", buildModelIds);
        String response = TransportServerFactory.get().execute(this.nodeInfo,
            this.urlItem(NodeUrl.Build_SourceExists, DataContentType.JSON), body);
        cn.keepbx.jpom.model.JsonMessage<JSONObject> message = NodeForward.toJsonMessage(response,
            new TypeReference<cn.keepbx.jpom.model.JsonMessage<JSONObject>>() {
            });
        JSONObject data = message.getData();
        if (data != null) {
            data.forEach((key, value) -> result.put(key, Boolean.parseBoolean(StrUtil.toStringOrNull(value))));
        }
        return result;
    }

    /**
     * 打开日志事件流
     *
     * @param jobId   任务 ID
     * @param fromSeq 从哪个序号开始补发
     * @return websocket 客户端（未连接）
     */
    public IProxyWebSocket openLog(String jobId, int fromSeq) {
        IUrlItem urlItem = this.urlItem(NodeUrl.Build_Log, DataContentType.FORM_URLENCODED);
        return TransportServerFactory.get().websocket(this.nodeInfo, urlItem, "jobId", jobId, "fromSeq", fromSeq);
    }

    /**
     * 拉取产物 zip
     *
     * @param jobId  任务 ID
     * @param target 落地文件（父目录需已存在）
     */
    public void download(String jobId, String buildModelId, int buildNumberId, File target) {
        Map<String, Object> params = new HashMap<>(4);
        params.put("jobId", jobId);
        // 兜底定位：jobId 丢失/丢失查询参数时，节点也能按构建 id + 构建号直接找到产物 zip
        params.put("buildModelId", buildModelId);
        params.put("buildNumberId", String.valueOf(buildNumberId));
        TransportServerFactory.get().download(this.nodeInfo,
            this.urlItem(NodeUrl.Build_Download, DataContentType.FORM_URLENCODED), params, (DownloadCallback callback) -> {
                try (java.io.InputStream inputStream = callback.getInputStream()) {
                    cn.hutool.core.io.FileUtil.writeFromStream(inputStream, target, true);
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            });
    }
}
