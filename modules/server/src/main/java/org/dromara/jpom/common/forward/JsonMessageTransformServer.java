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

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.exception.AgentException;
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
            log.error(I18nMessageUtil.get("i18n.node_null_pointer_exception.76fe"), nodeModel.name(), exception);
            return new AgentException(nodeModel.name() + I18nMessageUtil.get("i18n.node_exception_null_pointer.d408"));
        }
        String message = exception.getMessage();
        log.error("node [{}] connect failed...message: [{}]", nodeModel.name(), message);
        List<Throwable> throwableList = ExceptionUtil.getThrowableList(exception);
        for (Throwable throwable : throwableList) {
            if (throwable instanceof ConnectException || throwable instanceof SocketTimeoutException) {
                return new AgentException(nodeModel.name() + I18nMessageUtil.get("i18n.node_network_connection_exception_or_timeout.5904") +
                    I18nMessageUtil.get("i18n.port_configuration_check.d888") +
                    I18nMessageUtil.get("i18n.cloud_server_network_issues.a865") + message);
            }
            if (throwable instanceof UnknownHostException) {
                return new AgentException(nodeModel.name() + I18nMessageUtil.get("i18n.unable_to_access_node_network.4e09") + message);
            }
            if (throwable instanceof NoRouteToHostException) {
                return new AgentException(nodeModel.name() + I18nMessageUtil.get("i18n.node_communication_failure_signal.5aae") + message);
            }
            if (throwable instanceof IOException && StrUtil.containsIgnoreCase(message, "Error writing to server")) {
                return new AgentException(nodeModel.name() + I18nMessageUtil.get("i18n.node_communication_failure.00fb") + message);
            }
        }
        return new AgentException(nodeModel.name() + I18nMessageUtil.get("i18n.node_exception.bca7") + message);
    }
}
