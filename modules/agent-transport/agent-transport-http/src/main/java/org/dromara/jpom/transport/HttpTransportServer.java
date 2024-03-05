/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.resource.Resource;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.alibaba.fastjson2.JSONObject;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.encrypt.EncryptFactory;
import org.dromara.jpom.encrypt.Encryptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 插件端消息传输服务
 *
 * @author bwcx_jzy
 * @since 2022/12/18
 */
@Slf4j
public class HttpTransportServer implements TransportServer {


    private HttpRequest createRequest(INodeInfo nodeInfo, IUrlItem urlItem, Method method) {
        String url = StrUtil.format("{}://{}/", nodeInfo.scheme(), nodeInfo.url());
        UrlBuilder urlBuilder = UrlBuilder.of(url).addPath(urlItem.path());
        HttpRequest httpRequest = HttpRequest.of(urlBuilder);
        httpRequest.setMethod(method);

        Optional.ofNullable(urlItem.timeout()).ifPresent(integer -> httpRequest.timeout(integer * 1000));

        httpRequest.header(TRANSPORT_ENCRYPTION, nodeInfo.transportEncryption() + "");

        httpRequest.header(JPOM_AGENT_AUTHORIZE, nodeInfo.authorize());
        //
        httpRequest.header(WORKSPACE_ID_REQ_HEADER, urlItem.workspaceId());
        Optional.ofNullable(nodeInfo.proxy()).ifPresent(httpRequest::setProxy);
        return httpRequest;
    }

    private HttpRequest createRequest(INodeInfo nodeInfo, IUrlItem urlItem) {
        return createRequest(nodeInfo, urlItem, Method.POST);
    }

    @SuppressWarnings("unchecked")
    private void appendRequestData(HttpRequest httpRequest, IUrlItem urlItem, Object data, INodeInfo nodeInfo) {
        DataContentType dataContentType = urlItem.contentType();
        Optional.ofNullable(data).ifPresent(o -> {
            Encryptor encryptor;
            try {
                encryptor = EncryptFactory.createEncryptor(nodeInfo.transportEncryption());
                if (dataContentType == DataContentType.FORM_URLENCODED) {
                    if (o instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) o;
                        Map<String, Object> encryptedMap = new HashMap<>();
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            String encryptedKey = encryptor.encrypt(entry.getKey());
                            Object value = entry.getValue();
                            Object newValue;
                            if (value instanceof String[]) {
                                String[] valueStr = (String[]) value;
                                for (int i = 0; i < valueStr.length; i++) {
                                    valueStr[i] = encryptor.encrypt(valueStr[i]);
                                }
                                newValue = valueStr;
                            } else if (value instanceof Resource) {
                                newValue = value;
                            } else {
                                newValue = encryptor.encrypt(StrUtil.toStringOrNull(entry.getValue()));
                            }
                            encryptedMap.put(encryptedKey, newValue);
                        }
                        httpRequest.form(encryptedMap);
                    } else {
                        throw new IllegalArgumentException("不支持的类型:" + o.getClass());
                    }
                } else if (dataContentType == DataContentType.JSON) {
                    httpRequest.body(encryptor.encrypt(JSONObject.toJSONString(o)), ContentType.JSON.getValue());
                } else {
                    throw new IllegalArgumentException("不支持的 contentType");
                }
            } catch (Exception e) {
                log.error("编码异常", e);
                throw new TransportAgentException("节点传输信息编码异常:" + e.getMessage());
            }
        });
    }

    private String executeRequest(HttpRequest httpRequest, INodeInfo nodeInfo, IUrlItem urlItem) {
        //
        if (log.isDebugEnabled()) {
            log.debug("{}[{}] -> {} {}", nodeInfo.name(), httpRequest.getUrl(), urlItem.workspaceId(), Optional.ofNullable((Object) httpRequest.form()).orElse("-"));
        }
        return httpRequest.thenFunction(response -> {
            int status = response.getStatus();
            String body = response.body();
            log.debug("Completed {}", body);
            if (status != HttpStatus.HTTP_OK) {
                log.warn("{} 响应异常 状态码错误：{} {}", nodeInfo.name(), status, body);
                throw new TransportAgentException(nodeInfo.name() + " 节点响应异常,状态码错误：" + status);
            }
            return body;
        });
    }

    @Override
    public String execute(INodeInfo nodeInfo, IUrlItem urlItem, Object data) {
        HttpRequest httpRequest = this.createRequest(nodeInfo, urlItem);
        this.appendRequestData(httpRequest, urlItem, data, nodeInfo);
        try {
            return this.executeRequest(httpRequest, nodeInfo, urlItem);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(TransformServerFactory.get().transformException(e, nodeInfo));
        }
    }


    @Override
    public void download(INodeInfo nodeInfo, IUrlItem urlItem, Object data, Consumer<DownloadCallback> consumer) {
        HttpRequest httpRequest = this.createRequest(nodeInfo, urlItem, Method.GET);
        httpRequest.setFollowRedirects(true);
        this.appendRequestData(httpRequest, urlItem, data, nodeInfo);
        try (HttpResponse response1 = httpRequest.execute()) {
            String contentDisposition = response1.header(Header.CONTENT_DISPOSITION);
            String contentType = response1.header(Header.CONTENT_TYPE);
            DownloadCallback build = DownloadCallback.builder()
                .contentDisposition(contentDisposition).contentType(contentType).inputStream(response1.bodyStream())
                .build();
            consumer.accept(build);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(TransformServerFactory.get().transformException(e, nodeInfo));
        }
    }

    @Override
    public IProxyWebSocket websocket(INodeInfo nodeInfo, IUrlItem urlItem, Object... parameters) {
        String url = StrUtil.format("{}://{}/", nodeInfo.scheme(), nodeInfo.url());
        UrlBuilder urlBuilder = UrlBuilder.of(url).addPath(urlItem.path());
        //
        urlBuilder.addQuery(JPOM_AGENT_AUTHORIZE, nodeInfo.authorize());
        //
        urlBuilder.addQuery(WORKSPACE_ID_REQ_HEADER, urlItem.workspaceId());
        for (int i = 0; i < parameters.length; i += 2) {
            Object parameter = parameters[i + 1];
            String value = Convert.toStr(parameter, StrUtil.EMPTY);
            urlBuilder.addQuery(parameters[i].toString(), value);
        }
        urlBuilder.setWithEndTag(false);
        String uriTemplate = urlBuilder.build();
        uriTemplate = StrUtil.removePrefixIgnoreCase(uriTemplate, nodeInfo.scheme());
        String ws = "https".equalsIgnoreCase(nodeInfo.scheme()) ? "wss" : "ws";
        uriTemplate = StrUtil.format("{}{}", ws, uriTemplate);
        //
        if (log.isDebugEnabled()) {
            log.debug("{}[{}] -> {}", nodeInfo.name(), uriTemplate, urlItem.workspaceId());
        }
        Integer timeout = urlItem.timeout();
        return new ServletWebSocketClientHandler(uriTemplate, timeout);
    }
}
