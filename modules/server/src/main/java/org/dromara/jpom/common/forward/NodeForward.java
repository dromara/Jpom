/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.forward;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.resource.BytesResource;
import cn.hutool.core.io.unit.DataSize;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.model.BaseIdModel;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.NodeConfig;
import org.dromara.jpom.exception.AgentAuthorizeException;
import org.dromara.jpom.exception.AgentException;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.transport.*;
import org.dromara.jpom.util.StrictSyncFinisher;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 节点请求转发
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@Slf4j
public class NodeForward {

    /**
     * 创建代理
     *
     * @param type      代理类型
     * @param httpProxy 代理地址
     * @return proxy
     */
    public static Proxy crateProxy(String type, String httpProxy) {
        if (StrUtil.isNotEmpty(httpProxy)) {
            List<String> split = StrUtil.splitTrim(httpProxy, StrUtil.COLON);
            String host = CollUtil.getFirst(split);
            int port = Convert.toInt(CollUtil.getLast(split), 0);
            Proxy.Type type1 = EnumUtil.fromString(Proxy.Type.class, type, Proxy.Type.HTTP);
            return new Proxy(type1, new InetSocketAddress(host, port));
        }
        return null;
    }

    public static INodeInfo parseNodeInfo(NodeModel nodeModel) {
        Assert.hasText(nodeModel.getMachineId(), I18nMessageUtil.get("i18n.incomplete_node_info_missing_machine_id.1c9a"));
        MachineNodeServer machineNodeServer = SpringUtil.getBean(MachineNodeServer.class);
        MachineNodeModel model = machineNodeServer.getByKey(nodeModel.getMachineId(), false);
        Assert.notNull(model, I18nMessageUtil.get("i18n.machine_info_not_exist.3468"));
        return model;
    }

    public static INodeInfo coverNodeInfo(MachineNodeModel machineNodeModel) {
        if (StrUtil.isEmpty(machineNodeModel.getId())) {
            // 新增的情况
            return machineNodeModel;
        }
        MachineNodeServer machineNodeServer = SpringUtil.getBean(MachineNodeServer.class);
        MachineNodeModel model = machineNodeServer.getByKey(machineNodeModel.getId(), false);
        Optional.ofNullable(model)
            .ifPresent(exits -> {
                String password = Opt.ofBlankAble(machineNodeModel.getJpomPassword()).orElse(exits.getJpomPassword());
                machineNodeModel.setJpomPassword(password);
            });
        return machineNodeModel;
    }

    /**
     * 创建节点 url
     *
     * @param iNodeInfo       节点信息
     * @param nodeUrl         节点功能 url
     * @param dataContentType 传输的数据类型
     */
    public static IUrlItem parseUrlItem(INodeInfo iNodeInfo, String workspaceId, NodeUrl nodeUrl, DataContentType dataContentType) {
        //
        Map<String, String> header = NodeForward.createHeader();
        return new DefaultUrlItem(nodeUrl, iNodeInfo.timeout(), workspaceId, dataContentType, header);
    }

    /**
     * 创建节点 url
     *
     * @param iNodeInfo 节点信息
     * @param nodeUrl   节点功能 url
     */
    public static IUrlItem parseUrlItem(INodeInfo iNodeInfo, String workspaceId, NodeUrl nodeUrl) {
        //
        Map<String, String> header = NodeForward.createHeader();
        return new DefaultUrlItem(nodeUrl, iNodeInfo.timeout(), workspaceId, DataContentType.FORM_URLENCODED, header);
    }

    /**
     * 创建节点 url
     *
     * @param nodeModel       节点信息
     * @param nodeUrl         节点功能 url
     * @param dataContentType 传输的数据类型
     */
    public static <T> T createUrlItem(NodeModel nodeModel, NodeUrl nodeUrl, DataContentType dataContentType, BiFunction<INodeInfo, IUrlItem, T> consumer) {
        INodeInfo parseNodeInfo = parseNodeInfo(nodeModel);
        Map<String, String> header = NodeForward.createHeader();
        //
        IUrlItem iUrlItem = new DefaultUrlItem(nodeUrl, parseNodeInfo.timeout(), nodeModel.getWorkspaceId(), dataContentType, header);
        return consumer.apply(parseNodeInfo, iUrlItem);
    }

    private static <T> T createUrlItem(NodeModel nodeModel, NodeUrl nodeUrl, BiFunction<INodeInfo, IUrlItem, T> consumer) {
        return createUrlItem(nodeModel, nodeUrl, DataContentType.FORM_URLENCODED, consumer);
    }

    private static <T> T createUrlItem(INodeInfo nodeInfo, String workspaceId, NodeUrl nodeUrl, BiFunction<INodeInfo, IUrlItem, T> consumer) {
        return createUrlItem(nodeInfo, workspaceId, nodeUrl, DataContentType.FORM_URLENCODED, consumer);
    }

    private static <T> T createUrlItem(INodeInfo nodeInfo, String workspaceId, NodeUrl nodeUrl, DataContentType dataContentType, BiFunction<INodeInfo, IUrlItem, T> consumer) {
        //
        Map<String, String> header = NodeForward.createHeader();
        IUrlItem iUrlItem = new DefaultUrlItem(nodeUrl, nodeInfo.timeout(), workspaceId, dataContentType, header);
        return consumer.apply(nodeInfo, iUrlItem);
    }

    private static Map<String, String> createHeader() {
        Map<String, String> header = new HashMap<>();
        UserModel userByThreadLocal = BaseServerController.getUserByThreadLocal();
        header.put(Const.JPOM_SERVER_USER_NAME, Optional.ofNullable(userByThreadLocal).map(BaseIdModel::getId).orElse(StrUtil.EMPTY));
        // 语言
        header.put(HttpHeaders.ACCEPT_LANGUAGE, I18nMessageUtil.getLanguageByRequest());
        return header;
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
    public static <T> JsonMessage<T> request(NodeModel nodeModel, HttpServletRequest request, NodeUrl nodeUrl, String... removeKeys) {
        return request(nodeModel, request, nodeUrl, removeKeys, new String[]{});
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
    public static <T> JsonMessage<T> request(NodeModel nodeModel, HttpServletRequest request, NodeUrl nodeUrl, String[] removeKeys, String... appendData) {
        Map<String, String> map = Optional.ofNullable(request)
            .map(ServletUtil::getParamMap)
            .map(map1 -> MapUtil.removeAny(map1, removeKeys))
            .map(map2 -> {
                for (int i = 0; i < appendData.length; i += 2) {
                    map2.put(appendData[i], appendData[i + 1]);
                }
                return map2;
            })
            .orElse(null);

        TypeReference<JsonMessage<T>> tTypeReference = new TypeReference<JsonMessage<T>>() {
        };
        return createUrlItem(nodeModel, nodeUrl,
            (nodeInfo, urlItem) ->
                TransportServerFactory.get().executeToType(nodeInfo, urlItem, map, tTypeReference)
        );
    }

    /**
     * 普通消息转发
     *
     * @param machineNodeModel 机器
     * @param request          请求
     * @param nodeUrl          节点的url
     * @param <T>              泛型
     * @return JSON
     */
    public static <T> JsonMessage<T> request(MachineNodeModel machineNodeModel, HttpServletRequest request, NodeUrl nodeUrl, String... removeKeys) {
        return request(machineNodeModel, request, nodeUrl, removeKeys, new String[]{});
    }

    /**
     * 普通消息转发
     *
     * @param machineNodeModel 机器
     * @param request          请求
     * @param nodeUrl          节点的url
     * @param <T>              泛型
     * @return JSON
     */
    public static <T> JsonMessage<T> request(MachineNodeModel machineNodeModel, HttpServletRequest request, NodeUrl nodeUrl, String[] removeKeys, String... appendData) {
        Map<String, String> map = Optional.ofNullable(request)
            .map(ServletUtil::getParamMap)
            .map(map1 -> MapUtil.removeAny(map1, removeKeys))
            .map(map2 -> {
                for (int i = 0; i < appendData.length; i += 2) {
                    map2.put(appendData[i], appendData[i + 1]);
                }
                return map2;
            })
            .orElse(null);
        TypeReference<JsonMessage<T>> tTypeReference = new TypeReference<JsonMessage<T>>() {
        };
        INodeInfo nodeInfo1 = coverNodeInfo(machineNodeModel);
        return createUrlItem(nodeInfo1, StrUtil.EMPTY, nodeUrl,
            (nodeInfo, urlItem) ->
                TransportServerFactory.get().executeToType(nodeInfo, urlItem, map, tTypeReference)
        );
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
        TypeReference<JsonMessage<T>> tTypeReference = new TypeReference<JsonMessage<T>>() {
        };
        return createUrlItem(nodeModel, nodeUrl, (nodeInfo, urlItem) -> TransportServerFactory.get().executeToType(nodeInfo, urlItem, jsonObject, tTypeReference));
    }

    /**
     * 普通消息转发
     *
     * @param machineNodeModel 节点
     * @param nodeUrl          节点的url
     * @param jsonObject       数据
     * @return JSON
     */
    public static <T> JsonMessage<T> request(MachineNodeModel machineNodeModel, NodeUrl nodeUrl, JSONObject jsonObject) {
        TypeReference<JsonMessage<T>> typeReference = new TypeReference<JsonMessage<T>>() {
        };
        INodeInfo nodeInfo = coverNodeInfo(machineNodeModel);
        return createUrlItem(nodeInfo, StrUtil.EMPTY, nodeUrl, (nodeInfo1, urlItem) -> TransportServerFactory.get().executeToType(nodeInfo1, urlItem, jsonObject, typeReference));
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
        INodeInfo nodeInfo = parseNodeInfo(nodeModel);
        return requestSharding(nodeInfo, nodeModel.getWorkspaceId(), nodeUrl, jsonObject, file, File::getName, doneCallback, streamProgress);
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel  节点
     * @param nodeUrl    节点的url
     * @param jsonObject 数据
     * @return JSON
     */
    public static <T> JsonMessage<T> requestSharding(NodeModel nodeModel, NodeUrl nodeUrl, JSONObject jsonObject, File file, String fileName, Function<JSONObject, JsonMessage<T>> doneCallback, BiConsumer<Long, Long> streamProgress) throws IOException {
        INodeInfo nodeInfo = parseNodeInfo(nodeModel);
        return requestSharding(nodeInfo, nodeModel.getWorkspaceId(), nodeUrl, jsonObject, file, file1 -> fileName, doneCallback, streamProgress);
    }

    /**
     * 普通消息转发
     *
     * @param machineNodeModel 节点
     * @param nodeUrl          节点的url
     * @param jsonObject       数据
     * @return JSON
     */
    public static <T> JsonMessage<T> requestSharding(MachineNodeModel machineNodeModel, NodeUrl nodeUrl, JSONObject jsonObject, File file, Function<JSONObject, JsonMessage<T>> doneCallback, BiConsumer<Long, Long> streamProgress) throws IOException {
        INodeInfo nodeInfo = coverNodeInfo(machineNodeModel);
        return requestSharding(nodeInfo, StrUtil.EMPTY, nodeUrl, jsonObject, file, File::getName, doneCallback, streamProgress);
    }

    /**
     * 普通消息转发
     *
     * @param nodeInfo       节点
     * @param workspaceId    工作空间id
     * @param streamProgress 进度回调
     * @param nodeUrl        节点的url
     * @param jsonObject     数据
     * @return JSON
     */
    private static <T> JsonMessage<T> requestSharding(INodeInfo nodeInfo, String workspaceId, NodeUrl nodeUrl, JSONObject jsonObject, File file, Function<File, String> fileNameFn, Function<JSONObject, JsonMessage<T>> doneCallback, BiConsumer<Long, Long> streamProgress) throws IOException {
        IUrlItem urlItem = parseUrlItem(nodeInfo, workspaceId, nodeUrl, DataContentType.FORM_URLENCODED);
        ServerConfig serverConfig = SpringUtil.getBean(ServerConfig.class);
        NodeConfig nodeConfig = serverConfig.getNode();
        long length = file.length();
        String fileName = fileNameFn.apply(file);
        Assert.state(length > 0, I18nMessageUtil.get("i18n.empty_file_cannot_upload.88df") + file.getAbsolutePath());
        String md5 = SecureUtil.md5(file);
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
        int concurrent = nodeConfig.getUploadFileConcurrent();
        AtomicReference<JsonMessage<T>> failureMessage = new AtomicReference<>();
        AtomicReference<JsonMessage<T>> succeedMessage = new AtomicReference<>();
        AtomicLong atomicProgressSize = new AtomicLong(0);
        JSONObject sliceData = new JSONObject();
        sliceData.put("sliceId", IdUtil.fastSimpleUUID());
        sliceData.put("totalSlice", total);
        sliceData.put("fileSumMd5", md5);
        TransportServer transportServer = TransportServerFactory.get();
        TypeReference<JsonMessage<T>> typeReference = new TypeReference<JsonMessage<T>>() {
        };
        // 需要计算 并发数和最大任务数，如果任务数小于并发数则使用任务数
        try (StrictSyncFinisher syncFinisher = new StrictSyncFinisher(Math.min(concurrent, total), total)) {
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
                            uploadData.put("file", new BytesResource(array, fileName + StrUtil.DOT + currentChunk));
                            uploadData.put("nowSlice", currentChunk);
                            uploadData.putAll(sliceData);
                        }
                    }
                    // 上传
                    JsonMessage<T> message = transportServer.executeToType(nodeInfo, urlItem, uploadData, typeReference);
                    if (message.success()) {
                        // 使用成功的个数计算
                        success.add(currentChunk);
                        long end = Math.min(length, ((success.size() - 1) * chunkSize) + chunkSize);
                        // 保存线程安全顺序回调进度信息
                        atomicProgressSize.set(Math.max(end, atomicProgressSize.get()));
                        streamProgress.accept(length, atomicProgressSize.get());
                        succeedMessage.set(message);
                    } else {
                        log.warn(I18nMessageUtil.get("i18n.chunk_upload_exception.87c1"), nodeUrl, message);
                        // 终止上传
                        queueList.clear();
                        failureMessage.set(message);
                    }
                } catch (Exception e) {
                    log.error(I18nMessageUtil.get("i18n.chunk_upload_file_exception.0dc3"), e);
                    // 终止上传
                    queueList.clear();
                    failureMessage.set(new JsonMessage<>(500, I18nMessageUtil.get("i18n.upload_exception.cd6c") + e.getMessage()));
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
        Assert.state(success.size() == total, I18nMessageUtil.get("i18n.upload_exception_mismatch.0b25"));
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

        INodeInfo parseNodeInfo = parseNodeInfo(nodeModel);
        return request(parseNodeInfo, nodeModel.getWorkspaceId(), nodeUrl, pName, pVal, parameters);
    }

    /**
     * 普通消息转发
     *
     * @param machineNodeModel 节点
     * @param workspaceId      工作空间id
     * @param nodeUrl          节点的url
     * @param pName            主参数名
     * @param pVal             主参数值
     * @param parameters       其他参数
     * @return JSON
     */
    public static <T> JsonMessage<T> request(MachineNodeModel machineNodeModel, String workspaceId, NodeUrl nodeUrl, String pName, Object pVal, Object... parameters) {
        Map<String, Object> parametersMap = MapUtil.of(pName, pVal);
        for (int i = 0; i < parameters.length; i += 2) {
            parametersMap.put(parameters[i].toString(), parameters[i + 1]);
        }
        INodeInfo nodeInfo = coverNodeInfo(machineNodeModel);
        return request(nodeInfo, workspaceId, nodeUrl, pName, pVal, parameters);
    }

    /**
     * 普通消息转发
     *
     * @param nodeInfo    节点
     * @param workspaceId 工作空间id
     * @param nodeUrl     节点的url
     * @param pName       主参数名
     * @param pVal        主参数值
     * @param parameters  其他参数
     * @return JSON
     */
    private static <T> JsonMessage<T> request(INodeInfo nodeInfo, String workspaceId, NodeUrl nodeUrl, String pName, Object pVal, Object... parameters) {
        Map<String, Object> parametersMap = MapUtil.of(pName, pVal);
        for (int i = 0; i < parameters.length; i += 2) {
            parametersMap.put(parameters[i].toString(), parameters[i + 1]);
        }
        IUrlItem iUrlItem = parseUrlItem(nodeInfo, workspaceId, nodeUrl);
        return TransportServerFactory.get().executeToType(nodeInfo, iUrlItem, parametersMap, new TypeReference<JsonMessage<T>>() {
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
        TypeReference<JsonMessage<T>> tTypeReference = new TypeReference<JsonMessage<T>>() {
        };
        return createUrlItem(nodeModel, nodeUrl, DataContentType.JSON,
            (nodeInfo, urlItem) -> TransportServerFactory.get().executeToType(nodeInfo, urlItem, jsonData, tTypeReference));

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
        INodeInfo parseNodeInfo = parseNodeInfo(nodeModel);
        return requestData(parseNodeInfo, nodeModel.getWorkspaceId(), nodeUrl, request, tClass);
    }

    /**
     * 普通消息转发,并解析数据
     *
     * @param machineNodeModel 节点
     * @param request          请求
     * @param nodeUrl          节点的url
     * @param tClass           要解析的类
     * @param <T>              泛型
     * @return T
     */
    public static <T> T requestData(MachineNodeModel machineNodeModel, NodeUrl nodeUrl, HttpServletRequest request, Class<T> tClass) {
        INodeInfo nodeInfo = coverNodeInfo(machineNodeModel);
        return requestData(nodeInfo, StrUtil.EMPTY, nodeUrl, request, tClass);
    }

    /**
     * 普通消息转发,并解析数据
     *
     * @param nodeInfo1 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @param tClass    要解析的类
     * @param <T>       泛型
     * @return T
     */
    private static <T> T requestData(INodeInfo nodeInfo1, String workspaceId, NodeUrl nodeUrl, HttpServletRequest request, Class<T> tClass) {
        Map<String, String> map = Optional.ofNullable(request).map(ServletUtil::getParamMap).orElse(null);
        IUrlItem iUrlItem = parseUrlItem(nodeInfo1, workspaceId, nodeUrl);
        return TransportServerFactory.get().executeToTypeOnlyData(nodeInfo1, iUrlItem, map, tClass);
    }


    /**
     * 上传文件消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @return json
     */
    public static JsonMessage<String> requestMultipart(NodeModel nodeModel, MultipartHttpServletRequest request, NodeUrl nodeUrl) {
        INodeInfo parseNodeInfo = parseNodeInfo(nodeModel);
        return requestMultipart(parseNodeInfo, nodeModel.getWorkspaceId(), request, nodeUrl);
    }

    /**
     * 上传文件消息转发
     *
     * @param machineNodeModel 节点
     * @param request          请求
     * @param nodeUrl          节点的url
     * @return json
     */
    public static JsonMessage<String> requestMultipart(MachineNodeModel machineNodeModel, MultipartHttpServletRequest request, NodeUrl nodeUrl) {
        INodeInfo nodeInfo = coverNodeInfo(machineNodeModel);
        return requestMultipart(nodeInfo, StrUtil.EMPTY, request, nodeUrl);
    }

    /**
     * 上传文件消息转发
     *
     * @param nodeInfo 节点
     * @param request  请求
     * @param nodeUrl  节点的url
     * @return json
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static JsonMessage<String> requestMultipart(INodeInfo nodeInfo, String workspaceId, MultipartHttpServletRequest request, NodeUrl nodeUrl) {
        IUrlItem iUrlItem = parseUrlItem(nodeInfo, workspaceId, nodeUrl);
        //
        Map params = ServletUtil.getParamMap(request);
        //
        Map<String, MultipartFile> fileMap = request.getFileMap();
        fileMap.forEach((s, multipartFile) -> {
            try {
                params.put(s, new BytesResource(multipartFile.getBytes(), multipartFile.getOriginalFilename()));
            } catch (IOException e) {
                log.error(I18nMessageUtil.get("i18n.file_transfer_exception.bda6"), e);
                throw Lombok.sneakyThrow(e);
            }
        });
        TypeReference<JsonMessage<String>> tTypeReference = new TypeReference<JsonMessage<String>>() {
        };
        return TransportServerFactory.get().executeToType(nodeInfo, iUrlItem, params, tTypeReference);

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
        //
        Map<String, String> params = ServletUtil.getParamMap(request);
        createUrlItem(nodeModel, nodeUrl, (nodeInfo, urlItem) -> {
            TransportServerFactory.get().download(nodeInfo, urlItem, params, downloadCallback -> {
                Opt.ofBlankAble(downloadCallback.getContentDisposition())
                    .ifPresent(s -> response.setHeader(HttpHeaders.CONTENT_DISPOSITION, s));
                response.setContentType(downloadCallback.getContentType());
                ServletUtil.write(response, downloadCallback.getInputStream());
            });
            return null;
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
    public static void requestDownload(MachineNodeModel nodeModel, HttpServletRequest request, HttpServletResponse response, NodeUrl nodeUrl) {
        //
        Map<String, String> params = ServletUtil.getParamMap(request);
        INodeInfo nodeInfo = coverNodeInfo(nodeModel);
        IUrlItem iUrlItem = parseUrlItem(nodeInfo, StrUtil.EMPTY, nodeUrl);
        TransportServerFactory.get().download(nodeInfo, iUrlItem, params, downloadCallback -> {
            Opt.ofBlankAble(downloadCallback.getContentDisposition())
                .ifPresent(s -> response.setHeader(HttpHeaders.CONTENT_DISPOSITION, s));
            response.setContentType(downloadCallback.getContentType());
            ServletUtil.write(response, downloadCallback.getInputStream());
        });
    }

    public static <T> T toJsonMessage(String body, TypeReference<T> tTypeReference) {
        if (StrUtil.isEmpty(body)) {
            throw new AgentException(I18nMessageUtil.get("i18n.agent_response_empty.cc8e"));
        }
        T data = JSON.parseObject(body, tTypeReference);
        if (data instanceof JsonMessage) {
            JsonMessage<?> jsonMessage = (JsonMessage<?>) data;
            if (jsonMessage.getCode() == Const.AUTHORIZE_ERROR) {
                throw new AgentAuthorizeException(new JsonMessage<>(jsonMessage.getCode(), jsonMessage.getMsg()));
            }
        } else {
            throw new IllegalStateException(I18nMessageUtil.get("i18n.message_conversion_exception.cce8"));
        }
        return data;
    }
}
