package io.jpom.common;

import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.system.JpomRuntimeException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 全局异常处理
 *
 * @author jiangzeyin
 * @date 2019/04/17
 */
@ControllerAdvice
public class AgentExceptionHandler {

    /**
     * 声明要捕获的异常
     *
     * @param request  请求
     * @param response 响应
     * @param e        异常
     */
    @ExceptionHandler({JpomRuntimeException.class, RuntimeException.class, Exception.class})
    public void paramExceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception e) {
        DefaultSystemLog.getLog().error("controller " + request.getRequestURI(), e);
        if (e instanceof JpomRuntimeException) {
            ServletUtil.write(response, JsonMessage.getString(500, e.getMessage()), MediaType.APPLICATION_JSON_UTF8_VALUE);
        } else {
            ServletUtil.write(response, JsonMessage.getString(500, "服务异常：" + e.getMessage()), MediaType.APPLICATION_JSON_UTF8_VALUE);
        }
    }
}
