package io.jpom.socket;

import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.JpomApplication;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.user.RoleService;
import io.jpom.service.user.UserService;
import io.jpom.system.JpomRuntimeException;
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
            RoleService roleService = SpringUtil.getBean(RoleService.class);
            UserModel userModel = userService.checkUser(userId);
            if (userModel == null) {
                return false;
            }
            String nodeId = httpServletRequest.getParameter("nodeId");
            if (!JpomApplication.SYSTEM_ID.equals(nodeId)) {
                NodeService nodeService = SpringUtil.getBean(NodeService.class);
                NodeModel nodeModel = nodeService.getItem(nodeId);
                if (nodeModel == null || roleService.errorDynamicPermission(userModel, ClassFeature.NODE, nodeId)) {
                    return false;
                }
                //
                attributes.put("nodeInfo", nodeModel);
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
                    if (roleService.errorDynamicPermission(userModel, ClassFeature.PROJECT, projectId)) {
                        return false;
                    }
                    attributes.put("projectId", projectId);
                    break;
                case script:
                    // 脚本模板
                    String scriptId = httpServletRequest.getParameter("scriptId");
                    if (roleService.errorDynamicPermission(userModel, ClassFeature.SCRIPT, scriptId)) {
                        return false;
                    }
                    attributes.put("scriptId", scriptId);
                    break;
                case tomcat:
                    String tomcatId = httpServletRequest.getParameter("tomcatId");
                    if (roleService.errorDynamicPermission(userModel, ClassFeature.TOMCAT, tomcatId)) {
                        return false;
                    }
                    attributes.put("tomcatId", tomcatId);
                    break;
                case ssh:
                    String sshId = httpServletRequest.getParameter("sshId");
                    if (roleService.errorDynamicPermission(userModel, ClassFeature.SSH, sshId)) {
                        return false;
                    }
                    SshService bean = SpringUtil.getBean(SshService.class);
                    SshModel sshModel = bean.getItem(sshId);
                    if (sshModel == null) {
                        return false;
                    }
                    Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();
                    attributes.put("parameterMap", parameterMap);
                    attributes.put("sshItem", sshModel);
                    break;
                case nodeUpdate:
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
