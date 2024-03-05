/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common;

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.exception.AgentAuthorizeException;
import org.dromara.jpom.exception.AgentException;
import org.dromara.jpom.exception.BaseExceptionHandler;
import org.dromara.jpom.exception.PermissionException;
import org.dromara.jpom.transport.TransportAgentException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理
 *
 * @author bwcx_jzy
 * @since 2019/04/17
 */
@RestControllerAdvice
@Slf4j
public class GlobalDefaultExceptionHandler extends BaseExceptionHandler {

    /**
     * 声明要捕获的异常
     *
     * @param e 异常
     */
    @ExceptionHandler({AgentAuthorizeException.class})
    public IJsonMessage<String> delExceptionHandler(AgentAuthorizeException e) {
        return e.getJsonMessage();
    }

    /**
     * 插件端异常
     * <p>
     * 避免重复记录堆栈
     *
     * @param request 请求
     * @param e       异常
     * @author jzy
     * @since 2021-08-01
     */
    @ExceptionHandler({AgentException.class, TransportAgentException.class})
    public IJsonMessage<String> agentExceptionHandler(HttpServletRequest request, AgentException e) {
        Throwable cause = e.getCause();
        if (cause != null) {
            log.error("controller {}", request.getRequestURI(), cause);
        }
        return new JsonMessage<>(405, e.getMessage());
    }

    /**
     * 权限异常 需要退出登录
     *
     * @param e 异常
     * @return json
     */
    @ExceptionHandler({PermissionException.class})
    public IJsonMessage<String> doPermissionException(PermissionException e) {
        return new JsonMessage<>(ServerConst.AUTHORIZE_TIME_OUT_CODE, e.getMessage());
    }
}
