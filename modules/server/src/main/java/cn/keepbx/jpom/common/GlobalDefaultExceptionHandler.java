package cn.keepbx.jpom.common;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.interceptor.BaseJpomInterceptor;
import cn.keepbx.jpom.system.AgentException;
import cn.keepbx.jpom.system.AuthorizeException;
import cn.keepbx.jpom.system.JpomRuntimeException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 全局异常处理
 *
 * @author jiangzeyin
 * <p>
 * createTime 2019/04/17
 */
@ControllerAdvice
public class GlobalDefaultExceptionHandler {
    private static final TimedCache<String, String> TIMED_CACHE = new TimedCache<>(TimeUnit.MINUTES.toMillis(1));

    public static String getErrorMsg(String id) {
        return TIMED_CACHE.get(id);
    }

    /**
     * 声明要捕获的异常
     */
    @ExceptionHandler({AgentException.class, AuthorizeException.class, RuntimeException.class, Exception.class})
    public void paramExceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception e) {
        DefaultSystemLog.ERROR().error("controller " + request.getRequestURI(), e);
        if (BaseJpomInterceptor.isPage(request)) {
            try {
                String id = IdUtil.fastUUID();
                TIMED_CACHE.put(id, getErrorMsg(e));
                response.sendRedirect(BaseJpomInterceptor.getHeaderProxyPath(request) + "/error.html?id=" + id);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            if (e instanceof AuthorizeException) {
                AuthorizeException authorizeException = (AuthorizeException) e;
                ServletUtil.write(response, authorizeException.getJsonMessage().toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
            } else if (e instanceof AgentException) {
                AgentException agentException = (AgentException) e;
                ServletUtil.write(response, JsonMessage.getString(500, agentException.getMessage()), MediaType.APPLICATION_JSON_UTF8_VALUE);
            } else if (e instanceof JpomRuntimeException) {
                ServletUtil.write(response, JsonMessage.getString(500, e.getMessage()), MediaType.APPLICATION_JSON_UTF8_VALUE);
            } else {
                ServletUtil.write(response, JsonMessage.getString(500, "服务异常：" + e.getMessage()), MediaType.APPLICATION_JSON_UTF8_VALUE);
            }
        }
    }

    private String getErrorMsg(Exception e) {
        if (e instanceof AuthorizeException) {
            AuthorizeException authorizeException = (AuthorizeException) e;
            return authorizeException.getMessage();
        } else if (e instanceof AgentException) {
            AgentException agentException = (AgentException) e;
            return agentException.getMessage();
        } else {
            return "服务异常：" + e.getMessage();
        }
    }
}
