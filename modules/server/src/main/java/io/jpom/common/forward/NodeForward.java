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
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.common.JsonMessage;
import io.jpom.model.data.NodeModel;
import io.jpom.model.user.UserModel;
import io.jpom.service.node.NodeService;
import io.jpom.system.AgentException;
import io.jpom.system.AuthorizeException;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerExtConfigBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
     * 普通消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @param <T>       泛型
     * @return JSON
     */
    public static <T> JsonMessage<T> request(NodeModel nodeModel, HttpServletRequest request, NodeUrl nodeUrl) {
        return request(nodeModel, request, nodeUrl, true, null, null, null, null);
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel  节点
     * @param nodeUrl    节点的url
     * @param jsonObject 数据
     * @param userModel  user
     * @return JSON
     */
    public static JsonMessage<String> request(NodeModel nodeModel, NodeUrl nodeUrl, UserModel userModel, JSONObject jsonObject) {
        return request(nodeModel, null, nodeUrl, true, userModel, jsonObject, null, null);
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel 节点
     * @param nodeUrl   节点的url
     * @param pName     主参数名
     * @param pVal      主参数值
     * @param val       其他参数
     * @return JSON
     */
    public static <T> JsonMessage<T> requestBySys(NodeModel nodeModel, NodeUrl nodeUrl, String pName, Object pVal, Object... val) {
        return request(nodeModel, null, nodeUrl, false, null, null, pName, pVal, val);
    }

    /**
     * post body 消息转发
     *
     * @param nodeModel 节点
     * @param nodeUrl   节点的url
     * @param userModel 用户
     * @param jsonData  数据
     * @param <T>       泛型
     * @return JSON
     */
    public static <T> JsonMessage<T> requestBody(NodeModel nodeModel,
                                                 NodeUrl nodeUrl,
                                                 UserModel userModel,
                                                 JSONObject jsonData) {
        String url = nodeModel.getRealUrl(nodeUrl);
        HttpRequest httpRequest = HttpUtil.createPost(url);

        addUser(httpRequest, nodeModel, nodeUrl, userModel);

        httpRequest.body(jsonData.toString(), ContentType.JSON.getValue());

        try (HttpResponse response = httpRequest.execute()) {
            //
            return parseBody(httpRequest, response, nodeModel);
        } catch (Exception e) {
            throw NodeForward.responseException(e, nodeModel);
        }

    }

    /**
     * 普通消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @param pVal      主参数值
     * @param pName     主参数名
     * @param userModel 用户
     * @param jsonData  数据
     * @param mustUser  是否必须需要user
     * @param val       其他参数
     * @param <T>       泛型
     * @return JSON
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> JsonMessage<T> request(NodeModel nodeModel,
                                              HttpServletRequest request,
                                              NodeUrl nodeUrl,
                                              boolean mustUser,
                                              UserModel userModel,
                                              JSONObject jsonData,
                                              String pName,
                                              Object pVal,
                                              Object... val) {
        String url = nodeModel.getRealUrl(nodeUrl);
        HttpRequest httpRequest = HttpUtil.createPost(url);
        //
        if (mustUser) {
            if (userModel == null) {
                userModel = BaseServerController.getUserModel();
            }
        }
        //
        addUser(httpRequest, nodeModel, nodeUrl, userModel);
        Optional.ofNullable(request).map((Function<HttpServletRequest, Map>) ServletRequest::getParameterMap).ifPresent(httpRequest::form);
        httpRequest.form(pName, pVal, val);
        //
        if (jsonData != null) {
            boolean hasFile = false;
            // 参数 URL 编码，避免 特殊符号 不生效
            Set<Map.Entry<String, Object>> entries = jsonData.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                Object value = entry.getValue();
                if (value instanceof File) {
                    // 标记上传文件
                    hasFile = true;
                    break;
                }
            }
            httpRequest.form(jsonData);
            if (hasFile) {
                httpRequest.timeout(ServerExtConfigBean.getInstance().getUploadFileTimeOut());
            }
        }

        try (HttpResponse response = httpRequest.execute()) {
            //
            return parseBody(httpRequest, response, nodeModel);
        } catch (Exception e) {
            throw NodeForward.responseException(e, nodeModel);
        }
    }

    /**
     * 插件端 异常类型判断
     *
     * @param exception 异常
     * @param nodeModel 插件端
     */
    private static AgentException responseException(Exception exception, NodeModel nodeModel) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        log.error("node [{}] connect failed...message: [{}]", nodeModel.getName(), message);
        if (exception instanceof IORuntimeException) {
            if (cause instanceof java.net.ConnectException || cause instanceof java.net.SocketTimeoutException) {
                return new AgentException(nodeModel.getName() + "节点网络连接异常或超时,请优先检查插件端运行状态再检查 IP 地址、" +
                    "端口号是否配置正确,防火墙规则," +
                    "云服务器的安全组配置等网络相关问题排查定位。" + message);
            }
        } else if (exception instanceof cn.hutool.http.HttpException) {
            if (cause instanceof java.net.SocketTimeoutException) {
                return new AgentException(nodeModel.getName() + "节点网络连接超时,请优先检查插件端运行状态再检查节点超时时间配置是否合理,上传文件超时时间配置是否合理。" + message);
            }
            if (cause instanceof IOException && StrUtil.containsIgnoreCase(message, "Error writing to server")) {
                return new AgentException(nodeModel.getName() + "节点上传失败,请优先检查限制上传大小配置是否合理。" + message);
            }
        }
        return new AgentException(nodeModel.getName() + "节点异常：" + message);
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
        JsonMessage<T> jsonMessage = request(nodeModel, request, nodeUrl);
        return jsonMessage.getData(tClass);
    }

    /**
     * 普通消息转发,并解析数据
     *
     * @param nodeModel  节点
     * @param nodeUrl    节点的url
     * @param tClass     要解析的类
     * @param <T>        泛型
     * @param name       参数名
     * @param parameters 其他参数
     * @param value      值
     * @return T
     */
    public static <T> T requestData(NodeModel nodeModel, NodeUrl nodeUrl, Class<T> tClass, String name, Object value, Object... parameters) {
        String url = nodeModel.getRealUrl(nodeUrl);
        //
        HttpRequest httpRequest = HttpUtil.createPost(url);
        if (name != null && value != null) {
            httpRequest.form(name, value, parameters);
        }
        //
        addUser(httpRequest, nodeModel, nodeUrl);
        try (HttpResponse response = httpRequest.execute();) {
            //
            JsonMessage<T> jsonMessage = parseBody(httpRequest, response, nodeModel);
            return jsonMessage.getData(tClass);
        } catch (Exception e) {
            throw NodeForward.responseException(e, nodeModel);
        }
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
        String url = nodeModel.getRealUrl(nodeUrl);
        HttpRequest httpRequest = HttpUtil.createPost(url);
        addUser(httpRequest, nodeModel, nodeUrl);
        //
        Map params = ServletUtil.getParams(request);
        httpRequest.form(params);
        //
        Map<String, MultipartFile> fileMap = request.getFileMap();
        fileMap.forEach((s, multipartFile) -> {
            try {
                httpRequest.form(s, multipartFile.getBytes(), multipartFile.getOriginalFilename());
            } catch (IOException e) {
                log.error("转发文件异常", e);
            }
        });
        // @author jzy add  timeout
        httpRequest.timeout(ServerExtConfigBean.getInstance().getUploadFileTimeOut());
        try (HttpResponse response = httpRequest.execute()) {
            return parseBody(httpRequest, response, nodeModel);
        } catch (Exception e) {
            throw NodeForward.responseException(e, nodeModel);
        }

    }

    /**
     * 上传文件消息转发
     *
     * @param nodeModel 节点
     * @param fileName  文件字段名
     * @param file      上传的文件
     * @param nodeUrl   节点的url
     * @return json
     */
    public static JsonMessage<String> requestMultipart(NodeModel nodeModel, String fileName, File file, NodeUrl nodeUrl) {
        String url = nodeModel.getRealUrl(nodeUrl);
        HttpRequest httpRequest = HttpUtil.createPost(url);
        addUser(httpRequest, nodeModel, nodeUrl);
        //
        httpRequest.form(fileName, file);
        // @author jzy add  timeout
        httpRequest.timeout(ServerExtConfigBean.getInstance().getUploadFileTimeOut());
        try (HttpResponse response = httpRequest.execute()) {
            return parseBody(httpRequest, response, nodeModel);
        } catch (Exception e) {
            throw NodeForward.responseException(e, nodeModel);
        }

    }

    /**
     * 下载文件消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param response  响应
     * @param nodeUrl   节点的url
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void requestDownload(NodeModel nodeModel, HttpServletRequest request, HttpServletResponse response, NodeUrl nodeUrl) {
        String url = nodeModel.getRealUrl(nodeUrl);
        HttpRequest httpRequest = HttpUtil.createGet(url, true);
        addUser(httpRequest, nodeModel, nodeUrl);
        //
        Map params = ServletUtil.getParams(request);
        httpRequest.form(params);
        // @author jzy add  timeout
        httpRequest.timeout(ServerExtConfigBean.getInstance().getUploadFileTimeOut());
        //
        try (HttpResponse response1 = httpRequest.execute()) {
            String contentDisposition = response1.header("Content-Disposition");
            response.setHeader("Content-Disposition", contentDisposition);
            String contentType = response1.header("Content-Type");
            response.setContentType(contentType);
            ServletUtil.write(response, response1.bodyStream());
        } catch (Exception e) {
            throw NodeForward.responseException(e, nodeModel);
        }
    }

    private static void addUser(HttpRequest httpRequest, NodeModel nodeModel, NodeUrl nodeUrl) {
        UserModel userModel = BaseServerController.getUserModel();
        addUser(httpRequest, nodeModel, nodeUrl, userModel);
    }

    /**
     * 添加agent 授权信息header
     *
     * @param httpRequest request
     * @param nodeModel   节点
     * @param userModel   用户
     */
    private static void addUser(HttpRequest httpRequest, NodeModel nodeModel, NodeUrl nodeUrl, UserModel userModel) {
        // 判断开启状态
        if (!nodeModel.isOpenStatus()) {
            throw new AgentException(nodeModel.getName() + "节点未启用");
        }
        if (userModel != null) {
            httpRequest.header(ConfigBean.JPOM_SERVER_USER_NAME, URLUtil.encode(userModel.getId()));
//            httpRequest.header(ConfigBean.JPOM_SERVER_SYSTEM_USER_ROLE, userModel.getUserRole(nodeModel).name());
        }
        if (StrUtil.isEmpty(nodeModel.getLoginPwd())) {
            NodeService nodeService = SpringUtil.getBean(NodeService.class);
            NodeModel model = nodeService.getByKey(nodeModel.getId(), false);
            nodeModel.setLoginPwd(model.getLoginPwd());
            nodeModel.setLoginName(model.getLoginName());
        }
        httpRequest.header(ConfigBean.JPOM_AGENT_AUTHORIZE, nodeModel.toAuthorize());
        httpRequest.header(Const.WORKSPACEID_REQ_HEADER, nodeModel.getWorkspaceId());
        // 取最大的超时时间
        Optional.of(nodeUrl.getTimeOut())
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
            .ifPresent(timeOut -> httpRequest.timeout(timeOut * 1000));
        // 添加 http proxy
        Proxy proxy = nodeModel.proxy();
        httpRequest.setProxy(proxy);
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
        urlQuery.add(ConfigBean.JPOM_AGENT_AUTHORIZE, nodeModel.toAuthorize());
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
        urlQuery.add("name", URLUtil.encode(nodeModel.getLoginName()));
        urlQuery.add("password", URLUtil.encode(nodeModel.getLoginPwd()));
        String format = StrUtil.format("{}://{}{}?{}", ws, nodeModel.getUrl(), nodeUrl.getUrl(), urlQuery.toString());
        log.debug("web socket url:{}", format);
        return format;
    }

    /**
     * 解析结果
     *
     * @param response 响应
     * @return json
     */
    private static <T> JsonMessage<T> parseBody(HttpRequest httpRequest, HttpResponse response, NodeModel nodeModel) {
        int status = response.getStatus();
        String body = response.body();
        if (log.isDebugEnabled()) {
            log.debug("{} -> {} {} {} {}", nodeModel.getName(), httpRequest.getUrl(), httpRequest.getMethod(), Optional.ofNullable((Object) httpRequest.form()).orElse("-"), body);
        }
        if (status != HttpStatus.HTTP_OK) {
            log.warn("{} 响应异常 状态码错误：{} {}", nodeModel.getName(), status, body);
            throw new AgentException(nodeModel.getName() + " 节点响应异常,状态码错误：" + status);
        }
        return toJsonMessage(body);
    }

    private static <T> JsonMessage<T> toJsonMessage(String body) {
        if (StrUtil.isEmpty(body)) {
            throw new AgentException("agent 端响应内容为空");
        }
        JsonMessage<T> jsonMessage = JSON.parseObject(body, new TypeReference<JsonMessage<T>>() {
        });
        if (jsonMessage.getCode() == ConfigBean.AUTHORIZE_ERROR) {
            throw new AuthorizeException(jsonMessage, jsonMessage.getMsg());
        }
        return jsonMessage;
    }
}
