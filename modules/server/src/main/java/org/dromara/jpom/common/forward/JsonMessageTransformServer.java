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
package org.dromara.jpom.common.forward;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.system.AgentException;
import org.dromara.jpom.transport.INodeInfo;
import org.dromara.jpom.transport.TransformServer;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * json 消息转换
 *
 * @author bwcx_jzy
 * @since 2022/12/24
 */
@Slf4j
public class JsonMessageTransformServer implements TransformServer {

    @Override
    public <T> T transform(String data, TypeReference<T> tTypeReference) {
        return NodeForward.toJsonMessage(data, tTypeReference);
    }

    @Override
    public <T> T transformOnlyData(String data, Class<T> tClass) {
        JsonMessage<T> transform = this.transform(data, new TypeReference<JsonMessage<T>>() {
        });
        return transform.getData(tClass);
    }

    @Override
    public Exception transformException(Exception exception, INodeInfo nodeModel) {
        if (exception instanceof NullPointerException) {
            log.error("{}节点,程序空指针异常", nodeModel.name(), exception);
            return new AgentException(nodeModel.name() + "节点异常,空指针");
        }
        String message = exception.getMessage();
        log.error("node [{}] connect failed...message: [{}]", nodeModel.name(), message);
        List<Throwable> throwableList = ExceptionUtil.getThrowableList(exception);
        for (Throwable throwable : throwableList) {
            if (throwable instanceof ConnectException || throwable instanceof SocketTimeoutException) {
                return new AgentException(nodeModel.name() + "节点网络连接异常或超时,请优先检查插件端运行状态再检查 IP 地址、" +
                    "端口号是否配置正确,防火墙规则," +
                    "云服务器的安全组配置等网络相关问题排查定位。" + message);
            }
            if (throwable instanceof UnknownHostException) {
                return new AgentException(nodeModel.name() + "无法访问节点网络(未知的名称或服务),请检查主机名或者 DNS 是否可用。" + message);
            }
            if (throwable instanceof NoRouteToHostException) {
                return new AgentException(nodeModel.name() + "节点通讯失败,远程地址和端口时发生错误的信号。通常，由于中间的防火墙或中间路由器已关闭，无法访问远程主机。" + message);
            }
            if (throwable instanceof IOException && StrUtil.containsIgnoreCase(message, "Error writing to server")) {
                return new AgentException(nodeModel.name() + "节点通讯失败,请优先检查限制上传大小配置是否合理,或者网络连接是否被代理终端、防火墙终端等。" + message);
            }
        }
        return new AgentException(nodeModel.name() + "节点异常：" + message);
    }
}
