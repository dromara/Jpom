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
import cn.keepbx.jpom.service.user.RoleService;
import cn.keepbx.jpom.system.AgentException;
import cn.keepbx.permission.DynamicData;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 权限拦截器
 *
 * @author jiangzeyin
 * @date 2019/03/16.
 */
@InterceptorPattens(sort = 1)
public class PermissionInterceptor extends BaseJpomInterceptor {

    private NodeService nodeService;
    private RoleService roleService;

    private void init() {
        if (nodeService == null) {
            nodeService = SpringUtil.getBean(NodeService.class);
        }
        if (roleService == null) {
            roleService = SpringUtil.getBean(RoleService.class);
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
        this.addNode(request);
        UserModel userModel = BaseServerController.getUserModel();
        if (userModel == null || userModel.isSystemUser()) {
            return true;
        }
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
        MethodFeature method = feature.method();
        // 判断方法
        if (roleService.errorMethodPermission(userModel, classFeature, method)) {
            this.errorMsg(request, response);
            return false;
        }
        // 判断动态权限
        Map<ClassFeature, DynamicData> dynamicDataMap = DynamicData.getDynamicDataMap();
        DynamicData dynamicData = dynamicDataMap.get(classFeature);
        if (dynamicData != null) {
            // 排除的方法
            MethodFeature[] excludeMethod = dynamicData.getExcludeMethod();
            if (excludeMethod != null) {
                for (MethodFeature methodFeature : excludeMethod) {
                    if (methodFeature == method) {
                        // 排除方法
                        return true;
                    }
                }
            }
            // 动态参数
            String parameterName = dynamicData.getParameterName();
            String parameter = request.getParameter(parameterName);
            if (StrUtil.isEmpty(parameter)) {
                this.errorMsg(request, response);
                return false;
            }
            //
            if (roleService.errorDynamicPermission(userModel, classFeature, parameter)) {
                this.errorMsg(request, response);
                return false;
            }
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

    private void errorMsg(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (BaseJpomInterceptor.isPage(request)) {
            String url = getHeaderProxyPath(request) + "/authorize.html";
            response.sendRedirect(url);
        } else {
            JsonMessage jsonMessage = new JsonMessage(302, "你没有权限:-2");
            ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_UTF8_VALUE);
        }
    }
}
