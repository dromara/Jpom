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
package io.jpom.common.forward;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.resource.BytesResource;
import cn.hutool.core.io.unit.DataSize;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.thread.SyncFinisher;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import io.jpom.common.Const;
import io.jpom.common.JsonMessage;
import io.jpom.model.data.NodeModel;
import io.jpom.model.user.UserModel;
import io.jpom.service.node.NodeService;
import io.jpom.system.AgentException;
import io.jpom.system.AuthorizeException;
import io.jpom.system.ServerConfig;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import top.jpom.transport.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 节点请求转发
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@Slf4j
public class NodeForward {

    /**
     * 创建节点 url
     *
     * @param nodeModel       节点信息
     * @param nodeUrl         节点功能 url
     * @param dataContentType 传输的数据类型
     * @return item
     */
    public static IUrlItem createUrlItem(NodeModel nodeModel, NodeUrl nodeUrl, DataContentType dataContentType) {
        // 修正节点密码
        if (StrUtil.isEmpty(nodeModel.getLoginPwd())) {
            NodeService nodeService = SpringUtil.getBean(NodeService.class);
            NodeModel model = nodeService.getByKey(nodeModel.getId(), false);
            nodeModel.setLoginPwd(model.getLoginPwd());
            nodeModel.setLoginName(model.getLoginName());
        }
        //
        return new IUrlItem() {
            @Override
            public String path() {
                return nodeUrl.getUrl();
            }

            @Override
            public Integer timeout() {
                if (nodeUrl.isFileTimeout()) {
                    ServerConfig serverConfig = SpringUtil.getBean(ServerConfig.class);
                    ServerConfig.NodeConfig configNode = serverConfig.getNode();
                    return configNode.getUploadFileTimeout();
                } else {
                    return Optional.of(nodeUrl.getTimeout())
                        .flatMap(timeOut -> {
                            if (timeOut == 0) {
                                // 读取节点配置的超时时间
                                return Optional.ofNullable(nodeModel.getTimeOut());
                            }
                            // 值 < 0  url 指定不超时
                            return timeOut > 0 ? Optional.of(timeOut) : Optional.empty();
                        })
                        .map(timeOut -> {
                            if (timeOut <= 0) {
                                return null;
                            }
                            // 超时时间不能小于 2 秒
                            return Math.max(timeOut, 2);
                        })
                        .orElse(null);
                }
            }

            @Override
            public String workspaceId() {
                return Optional.ofNullable(nodeModel.getWorkspaceId()).orElse(Const.WORKSPACE_DEFAULT_ID);
            }

            @Override
            public DataContentType contentType() {
                return dataContentType;
            }
        };
    }

    private static IUrlItem createUrlItem(NodeModel nodeModel, NodeUrl nodeUrl) {
        return createUrlItem(nodeModel, nodeUrl, DataContentType.FORM_URLENCODED);
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @param <T>       泛型
     * @return JSON
     */
    public static <T> JsonMessage<T> request(NodeModel nodeModel, HttpServletRequest request, NodeUrl nodeUrl) {
        Map<String, String> map = Optional.ofNullable(request).map(ServletUtil::getParamMap).orElse(null);
        IUrlItem urlItem = createUrlItem(nodeModel, nodeUrl);
        return TransportServerFactory.get().executeToType(nodeModel, urlItem, map, new TypeReference<JsonMessage<T>>() {
        });
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel  节点
     * @param nodeUrl    节点的url
     * @param jsonObject 数据
     * @return JSON
     */
    public static <T> JsonMessage<T> request(NodeModel nodeModel, NodeUrl nodeUrl, JSONObject jsonObject) {
        IUrlItem urlItem = createUrlItem(nodeModel, nodeUrl);
        return TransportServerFactory.get().executeToType(nodeModel, urlItem, jsonObject, new TypeReference<JsonMessage<T>>() {
        });
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel  节点
     * @param nodeUrl    节点的url
     * @param jsonObject 数据
     * @return JSON
     */
    public static <T> JsonMessage<T> requestSharding(NodeModel nodeModel, NodeUrl nodeUrl, JSONObject jsonObject, File file, Function<JSONObject, JsonMessage<T>> doneCallback, BiConsumer<Long, Long> streamProgress) throws IOException {
        IUrlItem urlItem = createUrlItem(nodeModel, nodeUrl);
        ServerConfig serverConfig = SpringUtil.getBean(ServerConfig.class);
        ServerConfig.NodeConfig nodeConfig = serverConfig.getNode();
        long length = file.length();
        String fileName = file.getName();
        Assert.state(length > 0, "空文件不能上传");
        String sha1 = SecureUtil.sha1(file);
        int fileSliceSize = nodeConfig.getUploadFileSliceSize();
        //如果小数点大于1，整数加一 例如4.1 =》5
        long chunkSize = DataSize.ofMegabytes(fileSliceSize).toBytes();
        int total = (int) Math.ceil((double) length / chunkSize);
        Queue<Integer> queueList = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < total; i++) {
            queueList.offer(i);
        }
        List<Integer> success = Collections.synchronizedList(new ArrayList<>(total));

        // 并发数
        int concurrent = 2;
        AtomicReference<JsonMessage<T>> failureMessage = new AtomicReference<>();
        AtomicReference<JsonMessage<T>> succeedMessage = new AtomicReference<>();
        AtomicLong atomicProgressSize = new AtomicLong(0);
        JSONObject sliceData = new JSONObject();
        sliceData.put("sliceId", IdUtil.fastSimpleUUID());
        sliceData.put("totalSlice", total);
        sliceData.put("fileSumSha1", sha1);
        TransportServer transportServer = TransportServerFactory.get();
        TypeReference<JsonMessage<T>> typeReference = new TypeReference<JsonMessage<T>>() {
        };
        // 需要计算 并发数和最大任务数，如果任务数小于并发数则使用任务数
        try (SyncFinisher syncFinisher = new SyncFinisher(Math.min(concurrent, total))) {
            Runnable runnable = () -> {
                // 取出任务
                Integer currentChunk = queueList.poll();
                if (currentChunk == null) {
                    return;
                }
                JSONObject uploadData = jsonObject.clone();
                try {
                    try (FileInputStream inputStream = new FileInputStream(file)) {
                        try (FileChannel inputChannel = inputStream.getChannel()) {
                            //分配缓冲区，设定每次读的字节数
                            ByteBuffer byteBuffer = ByteBuffer.allocate((int) chunkSize);
                            // 移动到指定位置开始读取
                            inputChannel.position(currentChunk * chunkSize);
                            inputChannel.read(byteBuffer);
                            //上面把数据写入到了buffer，所以可知上面的buffer是写模式，调用flip把buffer切换到读模式，读取数据
                            byteBuffer.flip();
                            byte[] array = new byte[byteBuffer.remaining()];
                            byteBuffer.get(array, 0, array.length);
                            byteBuffer.clear();
                            uploadData.put("file", new BytesResource(array, fileName));
                            uploadData.put("nowSlice", currentChunk);
                            uploadData.putAll(sliceData);
                        }
                    }
                    // 上传
                    JsonMessage<T> message = transportServer.executeToType(nodeModel, urlItem, uploadData, typeReference);
                    if (message.success()) {
                        // 使用成功的个数计算
                        success.add(currentChunk);
                        long end = Math.min(length, ((success.size() - 1) * chunkSize) + chunkSize);
                        // 保存线程安全顺序回调进度信息
                        atomicProgressSize.set(Math.max(end, atomicProgressSize.get()));
                        streamProgress.accept(length, atomicProgressSize.get());
                        succeedMessage.set(message);
                    } else {
                        log.warn("分片上传异常：{} {}", nodeUrl, message);
                        // 终止上传
                        queueList.clear();
                        failureMessage.set(message);
                    }
                } catch (Exception e) {
                    log.error("分片上传文件异常", e);
                    // 终止上传
                    queueList.clear();
                    failureMessage.set(new JsonMessage<>(500, "上传异常：" + e.getMessage()));
                }
            };
            for (int i = 0; i < total; i++) {
                syncFinisher.addWorker(runnable);
            }
            syncFinisher.start();
        }
        JsonMessage<T> message = failureMessage.get();
        if (message != null) {
            return message;
        }
        // 判断是否都成功
        Assert.state(success.size() == total, "上传异常,完成数量不匹配");
        //
        return Optional.ofNullable(doneCallback)
            .map(function -> function.apply(sliceData))
            .orElseGet(succeedMessage::get);
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel  节点
     * @param nodeUrl    节点的url
     * @param pName      主参数名
     * @param pVal       主参数值
     * @param parameters 其他参数
     * @return JSON
     */
    public static <T> JsonMessage<T> request(NodeModel nodeModel, NodeUrl nodeUrl, String pName, Object pVal, Object... parameters) {
        Map<String, Object> parametersMap = MapUtil.of(pName, pVal);
        for (int i = 0; i < parameters.length; i += 2) {
            parametersMap.put(parameters[i].toString(), parameters[i + 1]);
        }
        IUrlItem urlItem = createUrlItem(nodeModel, nodeUrl);

        return TransportServerFactory.get().executeToType(nodeModel, urlItem, parametersMap, new TypeReference<JsonMessage<T>>() {
        });
    }

    /**
     * post body 消息转发
     *
     * @param nodeModel 节点
     * @param nodeUrl   节点的url
     * @param jsonData  数据
     * @param <T>       泛型
     * @return JSON
     */
    public static <T> JsonMessage<T> requestBody(NodeModel nodeModel, NodeUrl nodeUrl, JSONObject jsonData) {

        IUrlItem urlItem = createUrlItem(nodeModel, nodeUrl, DataContentType.JSON);
        return TransportServerFactory.get().executeToType(nodeModel, urlItem, jsonData, new TypeReference<JsonMessage<T>>() {
        });
    }

    /**
     * 插件端 异常类型判断
     *
     * @param exception 异常
     * @param nodeModel 插件端
     */
    public static AgentException responseException(Exception exception, INodeInfo nodeModel) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        log.error("node [{}] connect failed...message: [{}]", nodeModel.name(), message);
        if (exception instanceof IORuntimeException) {
            if (cause instanceof java.net.ConnectException || cause instanceof java.net.SocketTimeoutException) {
                return new AgentException(nodeModel.name() + "节点网络连接异常或超时,请优先检查插件端运行状态再检查 IP 地址、" +
                    "端口号是否配置正确,防火墙规则," +
                    "云服务器的安全组配置等网络相关问题排查定位。" + message);
            }
        } else if (exception instanceof cn.hutool.http.HttpException) {
            if (cause instanceof java.net.SocketTimeoutException) {
                return new AgentException(nodeModel.name() + "节点网络连接超时,请优先检查插件端运行状态再检查节点超时时间配置是否合理,上传文件超时时间配置是否合理。" + message);
            }
            if (cause instanceof IOException && StrUtil.containsIgnoreCase(message, "Error writing to server")) {
                return new AgentException(nodeModel.name() + "节点上传失败,请优先检查限制上传大小配置是否合理。" + message);
            }
        }
        return new AgentException(nodeModel.name() + "节点异常：" + message);
    }

    /**
     * 普通消息转发,并解析数据
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @param tClass    要解析的类
     * @param <T>       泛型
     * @return T
     */
    public static <T> T requestData(NodeModel nodeModel, NodeUrl nodeUrl, HttpServletRequest request, Class<T> tClass) {
        Map<String, String> map = Optional.ofNullable(request).map(ServletUtil::getParamMap).orElse(null);
        IUrlItem urlItem = createUrlItem(nodeModel, nodeUrl);
        return TransportServerFactory.get().executeToTypeOnlyData(nodeModel, urlItem, map, tClass);
//        JsonMessage<T> jsonMessage = request(nodeModel, request, nodeUrl);
//        return jsonMessage.getData(tClass);
    }


    /**
     * 上传文件消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @return json
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static JsonMessage<String> requestMultipart(NodeModel nodeModel, MultipartHttpServletRequest request, NodeUrl nodeUrl) {
        IUrlItem urlItem = createUrlItem(nodeModel, nodeUrl);
        //
        Map params = ServletUtil.getParamMap(request);
        //
        Map<String, MultipartFile> fileMap = request.getFileMap();
        fileMap.forEach((s, multipartFile) -> {
            try {
                params.put(s, new BytesResource(multipartFile.getBytes(), multipartFile.getOriginalFilename()));
            } catch (IOException e) {
                log.error("转发文件异常", e);
                throw Lombok.sneakyThrow(e);
            }
        });
        return TransportServerFactory.get().executeToType(nodeModel, urlItem, params, new TypeReference<JsonMessage<String>>() {
        });
    }

    /**
     * 下载文件消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param response  响应
     * @param nodeUrl   节点的url
     */
    public static void requestDownload(NodeModel nodeModel, HttpServletRequest request, HttpServletResponse response, NodeUrl nodeUrl) {
        IUrlItem urlItem = createUrlItem(nodeModel, nodeUrl);
        //
        Map<String, String> params = ServletUtil.getParamMap(request);
        TransportServerFactory.get().download(nodeModel, urlItem, params, downloadCallback -> {
            Opt.ofBlankAble(downloadCallback.getContentDisposition())
                .ifPresent(s -> response.setHeader(HttpHeaders.CONTENT_DISPOSITION, s));
            response.setContentType(downloadCallback.getContentType());
            ServletUtil.write(response, downloadCallback.getInputStream());
        });
    }

    /**
     * 获取节点socket 信息
     *
     * @param nodeModel 节点信息
     * @param nodeUrl   url
     * @return url
     */
    public static String getSocketUrl(NodeModel nodeModel, NodeUrl nodeUrl, UserModel userInfo, Object... parameters) {
        String ws;
        if ("https".equalsIgnoreCase(nodeModel.getProtocol())) {
            ws = "wss";
        } else {
            ws = "ws";
        }
        if (StrUtil.isEmpty(nodeModel.getLoginPwd())) {
            NodeService nodeService = SpringUtil.getBean(NodeService.class);
            NodeModel model = nodeService.getByKey(nodeModel.getId(), false);
            nodeModel.setLoginPwd(model.getLoginPwd());
            nodeModel.setLoginName(model.getLoginName());
        }
        UrlQuery urlQuery = new UrlQuery();
        urlQuery.add(Const.JPOM_AGENT_AUTHORIZE, nodeModel.toAuthorize());
        //
        String optUser = userInfo.getId();
        optUser = URLUtil.encode(optUser);
        urlQuery.add("optUser", optUser);
        if (ArrayUtil.isNotEmpty(parameters)) {
            for (int i = 0; i < parameters.length; i += 2) {
                Object parameter = parameters[i + 1];
                String value = Convert.toStr(parameter, StrUtil.EMPTY);
                urlQuery.add(parameters[i].toString(), URLUtil.encode(value));
            }
        }
        // 兼容旧版本-节点升级 @author jzy
        //urlQuery.add("name", URLUtil.encode(nodeModel.getLoginName()));
        //urlQuery.add("password", URLUtil.encode(nodeModel.getLoginPwd()));
        String format = StrUtil.format("{}://{}{}?{}", ws, nodeModel.getUrl(), nodeUrl.getUrl(), urlQuery.toString());
        log.debug("web socket url:{}", format);
        return format;
    }

    public static <T> T toJsonMessage(String body, TypeReference<T> tTypeReference) {
        if (StrUtil.isEmpty(body)) {
            throw new AgentException("agent 端响应内容为空");
        }
        T data = JSON.parseObject(body, tTypeReference);
        if (data instanceof JsonMessage) {
            JsonMessage<?> jsonMessage = (JsonMessage<?>) data;
            if (jsonMessage.getCode() == Const.AUTHORIZE_ERROR) {
                throw new AuthorizeException(new JsonMessage<>(jsonMessage.getCode(), jsonMessage.getMsg()));
            }
        } else {
            throw new IllegalStateException("消息转换异常");
        }
        return data;
    }
}
