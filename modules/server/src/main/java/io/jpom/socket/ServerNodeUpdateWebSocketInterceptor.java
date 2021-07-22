package io.jpom.socket;

import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.model.data.UserModel;
import io.jpom.service.user.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 节点升级 websocket 拦截器
 *
 * @author lf
 */
public class ServerNodeUpdateWebSocketInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest serverHttpRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpServletRequest = serverHttpRequest.getServletRequest();
            // 判断用户
            String userId = httpServletRequest.getParameter("userId");
            UserService userService = SpringUtil.getBean(UserService.class);
            UserModel userModel = userService.checkUser(userId);
            if (userModel == null) {
                return false;
            }

            String userAgent = ServletUtil.getHeaderIgnoreCase(httpServletRequest, HttpHeaders.USER_AGENT);
            attributes.put(HttpHeaders.USER_AGENT, userAgent);
            attributes.put("userInfo", userModel);
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            DefaultSystemLog.getLog().error("afterHandshake", exception);
        }
    }
}
