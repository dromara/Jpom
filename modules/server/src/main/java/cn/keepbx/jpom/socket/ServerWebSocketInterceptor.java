package cn.keepbx.jpom.socket;

import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.JpomApplication;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.user.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * socket 拦截器
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
public class ServerWebSocketInterceptor implements HandshakeInterceptor {

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
            String nodeId = httpServletRequest.getParameter("nodeId");
            NodeModel nodeModel = null;
            if (!JpomApplication.SYSTEM_ID.equals(nodeId)) {
                NodeService nodeService = SpringUtil.getBean(NodeService.class);
                nodeModel = nodeService.getItem(nodeId);
                if (nodeModel == null) {
                    return false;
                }
            }
            // 判断拦截类型
            String type = httpServletRequest.getParameter("type");
            if ("script".equalsIgnoreCase(type)) {
                // 脚本模板
                String scriptId = httpServletRequest.getParameter("scriptId");
                if (!userModel.isManage(nodeId)) {
                    return false;
                }
                attributes.put("scriptId", scriptId);
            } else if ("tomcat".equalsIgnoreCase(type)) {
                String tomcatId = httpServletRequest.getParameter("tomcatId");
                attributes.put("tomcatId", tomcatId);
            } else {
                //控制台
                String projectId = httpServletRequest.getParameter("projectId");
                // 判断权限
                if (nodeModel == null || !userModel.isProject(nodeModel.getId(), projectId)) {
                    return false;
                }
                attributes.put("projectId", projectId);
            }
            //
            String ip = ServletUtil.getClientIP(httpServletRequest);
            attributes.put("ip", ip);
            //
            String userAgent = ServletUtil.getHeaderIgnoreCase(httpServletRequest, HttpHeaders.USER_AGENT);
            attributes.put(HttpHeaders.USER_AGENT, userAgent);
            if (nodeModel != null) {
                attributes.put("nodeInfo", nodeModel);
            }
            attributes.put("userInfo", userModel);
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
