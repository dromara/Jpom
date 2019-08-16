package cn.keepbx.jpom.common.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.BaseInterceptor;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.controller.node.tomcat.TomcatManageController;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.node.manage.ProjectInfoService;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.system.AgentException;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 权限拦截器
 *
 * @author jiangzeyin
 * @date 2019/03/16.
 */
@InterceptorPattens(sort = 1)
public class PermissionInterceptor extends BaseInterceptor {

    private ProjectInfoService projectInfoService;
    private NodeService nodeService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (projectInfoService == null) {
            projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
        }
        if (nodeService == null) {
            nodeService = SpringUtil.getBean(NodeService.class);
        }
        super.preHandle(request, response, handler);
        UserModel userModel = BaseServerController.getUserModel();
        if (handler instanceof HandlerMethod && userModel != null) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            String nodeId = request.getParameter("nodeId");
            if (!StrUtil.isBlankOrUndefined(nodeId)) {
                // 节点信息
                NodeModel nodeModel = nodeService.getItem(nodeId);
                if (nodeModel != null && !nodeModel.isOpenStatus()) {
                    throw new AgentException(nodeModel.getName() + "节点未启用");
                }
                request.setAttribute("node", nodeModel);
            }

//            UrlPermission urlPermission = handlerMethod.getMethodAnnotation(UrlPermission.class);
//            if (urlPermission != null) {
//                Role role = urlPermission.value();
//                if (role == Role.System && !userModel.isSystemUser()) {
//                    JsonMessage jsonMessage = new JsonMessage(302, "你没有权限:-1");
//                    ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
//                    return false;
//                }
//                //
//                if (role == Role.ServerManager && !userModel.isServerManager()) {
//                    JsonMessage jsonMessage = new JsonMessage(302, "你没有权限:-3");
//                    ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
//                    return false;
//                }
//                if (role == Role.NodeManage && !userModel.isManage(nodeId)) {
//                    JsonMessage jsonMessage = new JsonMessage(302, "你没有权限:-2");
//                    ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
//                    return false;
//                }
//            }
        }
        return true;
    }
}
