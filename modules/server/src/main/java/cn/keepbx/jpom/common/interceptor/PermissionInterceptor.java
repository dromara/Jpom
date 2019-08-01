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
import cn.keepbx.jpom.service.manage.ProjectInfoService;
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

    private boolean isTomcat(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return StrUtil.startWith(uri, TomcatManageController.TOMCAT_URL);
    }

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
            ProjectPermission projectPermission = handlerMethod.getMethodAnnotation(ProjectPermission.class);
            if (projectPermission != null) {
                String val = request.getParameter(projectPermission.value());
                if (StrUtil.isEmpty(val)) {
                    JsonMessage jsonMessage = new JsonMessage(300, "请传入合法参数：" + projectPermission.value());
                    ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
                    return false;
                }
                boolean tomcat = isTomcat(request);
                if (tomcat) {
                    if (!userModel.isTomcat(nodeId, val)) {
                        JsonMessage jsonMessage = new JsonMessage(300, "你没有改tomcat的权限");
                        ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
                        return false;
                    }

                    if (projectPermission.checkDelete() && !userModel.isDeleteTomcatFile(nodeId)) {
                        // 没有删除文件权限
                        JsonMessage jsonMessage = new JsonMessage(301, "你没有删除文件权限");
                        ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
                        return false;
                    }

                    if (projectPermission.checkUpload() && !userModel.isDeleteTomcatFile(nodeId)) {
                        // 没有上传文件权限
                        JsonMessage jsonMessage = new JsonMessage(302, "你没有上传文件权限");
                        ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
                        return false;
                    }
                } else {
                    if (!userModel.isProject(nodeId, val)) {
                        JsonMessage jsonMessage = new JsonMessage(300, "你没有改项目的权限");
                        ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
                        return false;
                    }

                    if (projectPermission.checkDelete() && !userModel.isDeleteFile(nodeId)) {
                        // 没有删除文件权限
                        JsonMessage jsonMessage = new JsonMessage(301, "你没有删除文件权限");
                        ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
                        return false;
                    }

                    if (projectPermission.checkUpload() && !userModel.isUploadFile(nodeId)) {
                        // 没有上传文件权限
                        JsonMessage jsonMessage = new JsonMessage(302, "你没有上传文件权限");
                        ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
                        return false;
                    }
                }
            }

            UrlPermission urlPermission = handlerMethod.getMethodAnnotation(UrlPermission.class);
            if (urlPermission != null) {
                Role role = urlPermission.value();
                if (role == Role.System && !userModel.isSystemUser()) {
                    JsonMessage jsonMessage = new JsonMessage(302, "你没有权限:-1");
                    ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
                    return false;
                }
                //
                if (role == Role.ServerManager && !userModel.isServerManager()) {
                    JsonMessage jsonMessage = new JsonMessage(302, "你没有权限:-3");
                    ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
                    return false;
                }
                if (role == Role.NodeManage && !userModel.isManage(nodeId)) {
                    JsonMessage jsonMessage = new JsonMessage(302, "你没有权限:-2");
                    ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
                    return false;
                }
            }
        }
        return true;
    }
}
