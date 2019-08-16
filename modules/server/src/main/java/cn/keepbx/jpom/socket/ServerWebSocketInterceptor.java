package cn.keepbx.jpom.socket;

import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.JpomApplication;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.SshModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.node.ssh.SshService;
import cn.keepbx.jpom.service.user.UserService;
import cn.keepbx.jpom.system.JpomRuntimeException;
import cn.keepbx.plugin.ClassFeature;
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
                if (nodeModel == null || !userService.checkUserPermission(userModel, ClassFeature.NODE, nodeId)) {
                    return false;
                }
            }
            // 判断拦截类型
            String type = httpServletRequest.getParameter("type");
            HandlerType handlerType;
            try {
                handlerType = HandlerType.valueOf(type);
            } catch (Exception e) {
                throw new JpomRuntimeException("type 错误：" + type);
            }
            switch (handlerType) {
                case console:
                    //控制台
                    String projectId = httpServletRequest.getParameter("projectId");
                    // 判断权限
                    if (nodeModel == null || !userService.checkUserPermission(userModel, ClassFeature.PROJECT, projectId)) {
                        return false;
                    }
                    attributes.put("projectId", projectId);
                    break;
                case script:
                    // 脚本模板
                    String scriptId = httpServletRequest.getParameter("scriptId");
                    if (!userService.checkUserPermission(userModel, ClassFeature.PROJECT, scriptId)) {
                        return false;
                    }
                    attributes.put("scriptId", scriptId);
                    break;
                case tomcat:
                    String tomcatId = httpServletRequest.getParameter("tomcatId");
                    attributes.put("tomcatId", tomcatId);
                    break;
                case ssh:
                    String sshId = httpServletRequest.getParameter("sshId");
                    SshService bean = SpringUtil.getBean(SshService.class);
                    SshModel sshModel = bean.getItem(sshId);
                    if (sshModel == null) {
                        return false;
                    }
                    Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();
                    attributes.put("parameterMap", parameterMap);
                    attributes.put("sshItem", sshModel);
                    break;
                default:
                    return false;
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
        if (exception != null) {
            DefaultSystemLog.ERROR().error("afterHandshake", exception);
        }
    }
}
