package cn.keepbx.jpom.common.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.node.manage.ProjectInfoService;
import cn.keepbx.jpom.service.user.UserService;
import cn.keepbx.jpom.system.AgentException;
import cn.keepbx.jpom.system.JpomRuntimeException;
import cn.keepbx.permission.DynamicData;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 权限拦截器
 *
 * @author jiangzeyin
 * @date 2019/03/16.
 */
@InterceptorPattens(sort = 1)
public class PermissionInterceptor extends BaseJpomInterceptor {

    private ProjectInfoService projectInfoService;
    private NodeService nodeService;
    private UserService userService;

    private void init() {
        if (projectInfoService == null) {
            projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
        }
        if (nodeService == null) {
            nodeService = SpringUtil.getBean(NodeService.class);
        }
        if (userService == null) {
            userService = SpringUtil.getBean(UserService.class);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        this.init();
        super.preHandle(request, response, handler);
        HandlerMethod handlerMethod = getHandlerMethod();
        if (handlerMethod == null) {
            return true;
        }
        UserModel userModel = BaseServerController.getUserModel();
        if (userModel == null || userModel.isSystemUser()) {
            return true;
        }
        this.addNode(request);
        //
        Feature feature = handlerMethod.getBeanType().getAnnotation(Feature.class);
        if (feature == null || feature.cls() == ClassFeature.NULL) {
            return true;
        }
        ClassFeature classFeature = feature.cls();
        feature = handlerMethod.getMethodAnnotation(Feature.class);
        if (feature == null || feature.method() == MethodFeature.NULL) {
            return true;
        }
        // 判断动态权限
        MethodFeature method = feature.method();
        Map<ClassFeature, DynamicData> dynamicDataMap = DynamicData.getDynamicDataMap();
        DynamicData dynamicData = dynamicDataMap.get(classFeature);
        if (dynamicData != null) {
            String parameterName = dynamicData.getParameterName();
            String parameter = request.getParameter(parameterName);
            if (StrUtil.isEmpty(parameter)) {
                this.errorMsg(request, response);
                return false;
            }
            MethodFeature[] excludeMethod = dynamicData.getExcludeMethod();
            if (excludeMethod != null) {
                for (MethodFeature methodFeature : excludeMethod) {
                    if (methodFeature == method) {
                        // 排除方法
                        return true;
                    }
                }
            }
            //
            if (userService.errorDynamicPermission(userModel, classFeature, parameter)) {
                this.errorMsg(request, response);
                return false;
            }
        }
        // 判断方法
        if (userService.errorMethodPermission(userModel, classFeature, method)) {
            this.errorMsg(request, response);
            return false;
        }
        ClassFeature parent = classFeature.getParent();
        if (parent != null) {

        }

        return true;
    }

    private void addNode(HttpServletRequest request) {
        String nodeId = request.getParameter("nodeId");
        if (!StrUtil.isBlankOrUndefined(nodeId)) {
            // 节点信息
            NodeModel nodeModel = nodeService.getItem(nodeId);
            if (nodeModel != null && !nodeModel.isOpenStatus()) {
                throw new AgentException(nodeModel.getName() + "节点未启用");
            }
            request.setAttribute("node", nodeModel);
        }
    }

    private void errorMsg(HttpServletRequest request, HttpServletResponse response) {
        if (BaseJpomInterceptor.isPage(request)) {
            throw new JpomRuntimeException("没有权限");
        } else {
            JsonMessage jsonMessage = new JsonMessage(302, "你没有权限:-2");
            ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
        }
    }
}
