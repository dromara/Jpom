package io.jpom.socket.spring.interceptor;

import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.system.AgentAuthorize;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author lf
 */
public class NodeUpdateInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler webSocketHandler, Map<String, Object> map) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest serverHttpRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpServletRequest = serverHttpRequest.getServletRequest();
            // 判断用户
            String name = httpServletRequest.getParameter("name");
            String password = httpServletRequest.getParameter("password");

            AgentAuthorize authorize = AgentAuthorize.getInstance();
            return authorize.getAgentName().equals(name) && authorize.getAgentPwd().equals(password);
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Exception exception) {
        if (exception != null) {
            DefaultSystemLog.getLog().error("afterHandshake", exception);
        }
    }
}
