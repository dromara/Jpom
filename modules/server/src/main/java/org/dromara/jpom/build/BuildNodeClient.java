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
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.transport.DataContentType;
import org.dromara.jpom.transport.DownloadCallback;
import org.dromara.jpom.transport.INodeInfo;
import org.dromara.jpom.transport.IProxyWebSocket;
import org.dromara.jpom.transport.IUrlItem;
import org.dromara.jpom.transport.TransportServer;
import org.dromara.jpom.transport.TransportServerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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

    /**
     * 全量响应
     */
    private static final int STATUS_OK = 200;
    /**
     * 分片（断点续传）响应
     */
    private static final int STATUS_PARTIAL_CONTENT = 206;

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
     * 拉取产物 zip（支持断点续传）
     * <p>
     * 断点续传靠 HTTP {@code Range: bytes=N-}：节点支持时返回 206 并只发送剩余字节，服务端追加写入；
     * 老版本节点忽略 Range 返回 200 全量，由调用方清空重来（见 {@link ArtifactDownloader}）。
     *
     * @param jobId         任务 ID
     * @param buildModelId  构建配置 ID（jobId 丢失时节点按构建 id + 构建号兜底定位）
     * @param buildNumberId 构建号
     * @param target        落地文件（父目录需已存在）
     * @param offset        已落盘字节数，大于 0 时声明 Range 并追加写入
     * @return 本次拉取结果（状态码、响应声明长度、实际写入字节）
     */
    public ArtifactDownloader.Chunk download(String jobId, String buildModelId, int buildNumberId, File target, long offset) {
        Map<String, Object> params = new HashMap<>(4);
        params.put("jobId", jobId);
        // 兜底定位：jobId 丢失/丢失查询参数时，节点也能按构建 id + 构建号直接找到产物 zip
        params.put("buildModelId", buildModelId);
        params.put("buildNumberId", String.valueOf(buildNumberId));
        IUrlItem urlItem = this.urlItem(NodeUrl.Build_Download, DataContentType.FORM_URLENCODED);
        if (offset > 0) {
            Map<String, String> header = urlItem.header();
            if (header != null) {
                header.put("Range", "bytes=" + offset + "-");
            }
        }
        return fetch(TransportServerFactory.get(), this.nodeInfo, urlItem, params, target, offset);
    }

    /**
     * 通过指定传输实现拉取一片产物（把「写文件 + 字节对账 + 状态校验」收敛在一处，便于换实现与单测）
     *
     * @param transport 传输实现（正向 HTTP / 反向隧道或其路由门面）
     * @param nodeInfo  节点信息
     * @param urlItem   请求地址（调用方需已按 offset 附加 {@code Range} 头）
     * @param params    请求参数
     * @param target    落地文件
     * @param offset    已落盘字节数，大于 0 时追加写入
     * @return 本次拉取结果
     */
    static ArtifactDownloader.Chunk fetch(TransportServer transport,
                                          INodeInfo nodeInfo, IUrlItem urlItem, Object params,
                                          File target, long offset) {
        AtomicReference<ArtifactDownloader.Chunk> chunkRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        transport.download(nodeInfo, urlItem, params, (DownloadCallback callback) -> {
            int status = callback.getStatus();
            if (status != STATUS_OK && status != STATUS_PARTIAL_CONTENT) {
                errorRef.set(new IllegalStateException(StrUtil.format(
                    I18nMessageUtil.get("i18n.build_node_download_status.7f21"), status)));
                return;
            }
            long received = 0;
            try (InputStream inputStream = callback.getInputStream()) {
                received = copy(inputStream, target, offset > 0);
            } catch (Exception e) {
                // 传输中途失败：已写入的字节保留，由下载器从断点继续
                errorRef.set(e);
            }
            chunkRef.set(new ArtifactDownloader.Chunk(status, callback.getContentLength(), received));
        });
        Exception error = errorRef.get();
        if (error != null) {
            throw new IllegalStateException(error.getMessage(), error);
        }
        ArtifactDownloader.Chunk chunk = chunkRef.get();
        if (chunk == null) {
            throw new IllegalStateException(I18nMessageUtil.get("i18n.build_node_result_missing.9e0f"));
        }
        return chunk;
    }

    /**
     * 把响应流写入产物文件
     *
     * @param inputStream 响应流
     * @param target      目标文件
     * @param append      是否追加（断点续传）
     * @return 本次写入字节数
     * @throws IOException 读写失败
     */
    private static long copy(InputStream inputStream, File target, boolean append) throws IOException {
        StandardOpenOption[] options = append
            ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE}
            : new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};
        long total = 0;
        try (OutputStream outputStream = Files.newOutputStream(target.toPath(), options)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                if (read == 0) {
                    continue;
                }
                outputStream.write(buffer, 0, read);
                total += read;
            }
        }
        return total;
    }
}
