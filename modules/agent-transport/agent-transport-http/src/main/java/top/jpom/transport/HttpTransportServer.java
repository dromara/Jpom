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
package top.jpom.transport;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.encrypt.EncryptFactory;
import io.jpom.encrypt.Encryptor;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import top.jpom.transform.TransformServerFactory;

import java.util.*;
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

        httpRequest.header(TransportServer.TRANSPORT_ENCRYPTION, nodeInfo.transportEncryption().toString());

        httpRequest.header(TransportServer.JPOM_AGENT_AUTHORIZE, nodeInfo.authorize());
        //
        httpRequest.header(TransportServer.WORKSPACE_ID_REQ_HEADER, urlItem.workspaceId());
        Optional.ofNullable(nodeInfo.proxy()).ifPresent(httpRequest::setProxy);
        return httpRequest;
    }

    private HttpRequest createRequest(INodeInfo nodeInfo, IUrlItem urlItem) {
        return createRequest(nodeInfo, urlItem, Method.POST);
    }

    @SuppressWarnings("unchecked")
    private void appendRequestData(HttpRequest httpRequest, IUrlItem urlItem, Object data,INodeInfo nodeInfo) {
        DataContentType dataContentType = urlItem.contentType();
        Optional.ofNullable(data).ifPresent(o -> {
            Encryptor encryptor;
            try {
                encryptor = EncryptFactory.createEncryptor(nodeInfo.transportEncryption());
                if (dataContentType == DataContentType.FORM_URLENCODED) {
                    if (o instanceof Map) {
                        Map<String, Object> map=(Map<String, Object>) o;
                        Map<String, Object> encryptedMap = new HashMap<>();
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            String encryptedKey =  encryptor.encrypt(String.valueOf(entry.getKey()));
                            String encryptedValue = encryptor.encrypt(String.valueOf(entry.getValue()));
                            encryptedMap.put(encryptedKey, encryptedValue);
                        }
                        httpRequest.form(encryptedMap);
                    } else {
                        throw new IllegalArgumentException("不支持的类型:" + o.getClass());
                    }
                } else if (dataContentType == DataContentType.JSON) {
                    httpRequest.body(encryptor.encrypt(JSONObject.toJSONString(o)), cn.hutool.http.ContentType.JSON.getValue());
                } else {
                    throw new IllegalArgumentException("不支持的 contentType");
                }
            }catch (Exception e) {
                throw Lombok.sneakyThrow(TransformServerFactory.get().transformException(e,nodeInfo));
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
                throw new AgentException(nodeInfo.name() + " 节点响应异常,状态码错误：" + status);
            }
            return body;
        });
    }

    @Override
    public String execute(INodeInfo nodeInfo, IUrlItem urlItem, Object data) {
        HttpRequest httpRequest = this.createRequest(nodeInfo, urlItem);
        this.appendRequestData(httpRequest, urlItem, data,nodeInfo);
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
        this.appendRequestData(httpRequest, urlItem, data,nodeInfo);
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
        urlBuilder.addQuery(TransportServer.JPOM_AGENT_AUTHORIZE, nodeInfo.authorize());
        //
        urlBuilder.addQuery(TransportServer.WORKSPACE_ID_REQ_HEADER, urlItem.workspaceId());
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
