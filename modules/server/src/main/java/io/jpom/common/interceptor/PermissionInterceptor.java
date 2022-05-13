/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.common.interceptor;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.model.BaseNodeModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.permission.*;
import io.jpom.service.h2db.BaseNodeService;
import io.jpom.service.node.NodeService;
import io.jpom.service.user.UserBindWorkspaceService;
import io.jpom.system.AgentException;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 权限拦截器
 *
 * @author jiangzeyin
 * @since 2019/03/16.
 */
@InterceptorPattens(sort = 1)
public class PermissionInterceptor extends BaseJpomInterceptor {

    private NodeService nodeService;
    private UserBindWorkspaceService userBindWorkspaceService;
    public static final String DEMO_TIP = "演示账号不能使用该功能";
    /**
     * demo 账号不能使用的功能
     */
    private static final MethodFeature[] DEMO = new MethodFeature[]{
        MethodFeature.DEL,
        MethodFeature.UPLOAD,
        MethodFeature.REMOTE_DOWNLOAD,
        MethodFeature.EXECUTE};


    private void init() {
        if (nodeService == null) {
            nodeService = SpringUtil.getBean(NodeService.class);
        }
        if (userBindWorkspaceService == null) {
            userBindWorkspaceService = SpringUtil.getBean(UserBindWorkspaceService.class);
        }
    }

    private SystemPermission getSystemPermission(HandlerMethod handlerMethod) {
        SystemPermission systemPermission = handlerMethod.getMethodAnnotation(SystemPermission.class);
        if (systemPermission == null) {
            systemPermission = handlerMethod.getBeanType().getAnnotation(SystemPermission.class);
        }
        return systemPermission;
    }

    private NodeDataPermission getNodeDataPermission(HandlerMethod handlerMethod) {
        NodeDataPermission nodeDataPermission = handlerMethod.getMethodAnnotation(NodeDataPermission.class);
        if (nodeDataPermission == null) {
            nodeDataPermission = handlerMethod.getBeanType().getAnnotation(NodeDataPermission.class);
        }
        return nodeDataPermission;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        this.init();
        this.addNode(request);
        UserModel userModel = BaseServerController.getUserModel();
        if (userModel == null || userModel.isSuperSystemUser()) {
            // 没有登录、或者超级管理直接放过
            return true;
        }
        //
        boolean permission = this.checkSystemPermission(userModel, request, response, handlerMethod);
        if (!permission) {
            return false;
        }
        permission = this.checkNodeDataPermission(userModel, request, response, handlerMethod);
        if (!permission) {
            return false;
        }
        Feature feature = handlerMethod.getMethodAnnotation(Feature.class);
        if (feature == null) {
            return true;
        }
        MethodFeature method = feature.method();
        if (ArrayUtil.contains(DEMO, method) && userModel.isDemoUser()) {
            this.errorMsg(response, DEMO_TIP);
            return false;
        }
        ClassFeature classFeature = feature.cls();
        if (classFeature == ClassFeature.NULL) {
            Feature feature1 = handlerMethod.getBeanType().getAnnotation(Feature.class);
            if (feature1 != null && feature1.cls() != ClassFeature.NULL) {
                classFeature = feature1.cls();
            }
        }
        // 判断功能权限
        if (method != MethodFeature.LIST) {
            String workspaceId = ServletUtil.getHeader(request, Const.WORKSPACEID_REQ_HEADER, CharsetUtil.CHARSET_UTF_8);
            if (StrUtil.isEmpty(workspaceId)) {
                workspaceId = request.getParameter(Const.WORKSPACEID_REQ_HEADER);
            }
            boolean exists = userBindWorkspaceService.exists(userModel.getId(), workspaceId + StrUtil.DASHED + method.name());
            if (!exists) {
                this.errorMsg(response, "您没有对应功能【" + classFeature.getName() + StrUtil.DASHED + method.getName() + "】管理权限");
                return false;
            }
        }
        return true;
    }

    /**
     * 检查管理员权限
     *
     * @param userModel     用户
     * @param response      响应
     * @param handlerMethod 拦截到到方法
     * @return true 有权限
     */
    private boolean checkNodeDataPermission(UserModel userModel, HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) {
        NodeDataPermission nodeDataPermission = this.getNodeDataPermission(handlerMethod);
        if (nodeDataPermission == null || userModel.isSuperSystemUser()) {
            return true;
        }
        NodeModel node = (NodeModel) request.getAttribute("node");
        if (node != null) {
            String parameterName = nodeDataPermission.parameterName();
            BaseNodeService<?> baseNodeService = SpringUtil.getBean(nodeDataPermission.cls());
            String dataId = request.getParameter(parameterName);
            if (StrUtil.isNotEmpty(dataId)) {
                BaseNodeModel data = baseNodeService.getData(node.getId(), dataId);
                if (data != null) {
                    boolean exists = userBindWorkspaceService.exists(userModel.getId(), data.getWorkspaceId());
                    if (!exists) {
                        this.errorMsg(response, "您没有对应到数据权限:-3");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 检查管理员权限
     *
     * @param userModel     用户
     * @param response      响应
     * @param handlerMethod 拦截到到方法
     * @return true 有权限
     */
    private boolean checkSystemPermission(UserModel userModel, HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) {
        SystemPermission systemPermission = this.getSystemPermission(handlerMethod);
        if (systemPermission == null) {
            return true;
        }
        if (systemPermission.superUser() && !userModel.isSuperSystemUser()) {
            this.errorMsg(response, "您不是超级管理员没有权限:-2");
            return false;
        }
        NodeModel node = (NodeModel) request.getAttribute("node");
        if (node == null) {
            // 服务端
            if (!userModel.isSystemUser()) {
                this.errorMsg(response, "您没有服务端管理权限:-2");
                return false;
            }
        } else {
            // 判断节点管理权限
            String workspaceId = ServletUtil.getHeader(request, Const.WORKSPACEID_REQ_HEADER, CharsetUtil.CHARSET_UTF_8);
            boolean exists = userBindWorkspaceService.exists(userModel.getId(), workspaceId + UserBindWorkspaceService.SYSTEM_USER);
            if (!exists) {
                this.errorMsg(response, "您没有对应节点管理权限:-3");
                return false;
            }
        }
        return true;
    }

    private void addNode(HttpServletRequest request) {
        String nodeId = request.getParameter("nodeId");
        if (!StrUtil.isBlankOrUndefined(nodeId)) {
            // 节点信息
            NodeModel nodeModel = nodeService.getByKey(nodeId);
            if (nodeModel != null && !nodeModel.isOpenStatus()) {
                throw new AgentException(nodeModel.getName() + "节点未启用");
            }
            request.setAttribute("node", nodeModel);
        }
    }

    private void errorMsg(HttpServletResponse response, String msg) {
        JsonMessage<String> jsonMessage = new JsonMessage<>(302, msg);
        ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_VALUE);
    }
}
