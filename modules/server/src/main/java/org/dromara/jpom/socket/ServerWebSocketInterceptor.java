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
package org.dromara.jpom.socket;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.interceptor.PermissionInterceptor;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.user.UserBindWorkspaceModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.user.UserBindWorkspaceService;
import org.dromara.jpom.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * socket 拦截器、鉴权
 *
 * @author jiangzeyin
 * @since 2019/4/19
 */
@Slf4j
@Configuration
public class ServerWebSocketInterceptor implements HandshakeInterceptor {

    private final UserService userService;
    private final NodeService nodeService;
    private final UserBindWorkspaceService userBindWorkspaceService;

    public ServerWebSocketInterceptor(UserService userService,
                                      NodeService nodeService,
                                      UserBindWorkspaceService userBindWorkspaceService) {
        this.userService = userService;
        this.nodeService = nodeService;
        this.userBindWorkspaceService = userBindWorkspaceService;
    }

    private boolean checkNode(HttpServletRequest httpServletRequest, Map<String, Object> attributes, UserModel userModel) {
        // 验证 node 权限
        String nodeId = httpServletRequest.getParameter("nodeId");
        if (!Const.SYSTEM_ID.equals(nodeId)) {
            NodeModel nodeModel = nodeService.getByKey(nodeId, userModel);
            if (nodeModel == null) {
                return false;
            }
            //
            attributes.put("nodeInfo", nodeModel);
        }
        return true;
    }

    private HandlerType fromType(HttpServletRequest httpServletRequest) {
        // 判断拦截类型
        String type = httpServletRequest.getParameter("type");
        HandlerType handlerType = EnumUtil.fromString(HandlerType.class, type, null);
        if (handlerType == null) {
            log.warn("传入的类型错误：{}", type);
        }
        return handlerType;
    }

    private boolean checkHandlerType(HandlerType handlerType, UserModel userModel, HttpServletRequest httpServletRequest, Map<String, Object> attributes) {
        switch (handlerType) {
            case console: {
                //控制台
                Object dataItem = this.checkData(handlerType, userModel, httpServletRequest);
                if (dataItem == null) {
                    return false;
                }
                attributes.put("copyId", httpServletRequest.getParameter("copyId"));
                attributes.put("projectId", BeanUtil.getProperty(dataItem, "projectId"));
                attributes.put("dataItem", dataItem);
                break;
            }
            case nodeScript: {
                // 节点脚本模板
                Object dataItem = this.checkData(handlerType, userModel, httpServletRequest);
                if (dataItem == null) {
                    return false;
                }
                attributes.put("dataItem", dataItem);
                attributes.put("scriptId", BeanUtil.getProperty(dataItem, "scriptId"));
                break;
            }
            case script: {
                // 脚本模板
                Object dataItem = this.checkData(handlerType, userModel, httpServletRequest);
                if (dataItem == null) {
                    return false;
                }
                attributes.put("dataItem", dataItem);
                attributes.put("scriptId", BeanUtil.getProperty(dataItem, "id"));
                break;
            }
            case tomcat:
                String tomcatId = httpServletRequest.getParameter("tomcatId");

                attributes.put("tomcatId", tomcatId);
                break;
            case dockerLog: {
                Tuple dataItem = this.checkAssetsData(handlerType, userModel, httpServletRequest);
                if (dataItem == null) {
                    return false;
                }
                attributes.put("dataItem", dataItem.get(2));
                attributes.put("isAssetsManager", dataItem.get(1));
                attributes.put("machineDocker", dataItem.get(0));
                break;
            }
            case ssh: {
                Tuple dataItem = this.checkAssetsData(handlerType, userModel, httpServletRequest);
                if (dataItem == null) {
                    return false;
                }
                attributes.put("dataItem", dataItem.get(2));
                attributes.put("isAssetsManager", dataItem.get(1));
                attributes.put("machineSsh", dataItem.get(0));
                break;
            }
            case docker:
                Tuple dataItem = this.checkAssetsData(handlerType, userModel, httpServletRequest);
                if (dataItem == null) {
                    return false;
                }
                attributes.put("dataItem", dataItem.get(2));
                attributes.put("isAssetsManager", dataItem.get(1));
                attributes.put("machineDocker", dataItem.get(0));
                attributes.put("containerId", httpServletRequest.getParameter("containerId"));
                break;
            case nodeUpdate:
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest serverHttpRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpServletRequest = serverHttpRequest.getServletRequest();
            // 判断用户
            String userId = httpServletRequest.getParameter("userId");
            String workspaceId = httpServletRequest.getParameter(ServerConst.WORKSPACE_ID_REQ_HEADER);
            attributes.put("workspaceId", workspaceId);
            UserModel userModel = userService.checkUser(userId);
            if (userModel == null) {
                attributes.put("permissionMsg", "用户不存在");
                return true;
            }
            boolean checkNode = this.checkNode(httpServletRequest, attributes, userModel);
            HandlerType handlerType = this.fromType(httpServletRequest);
            if (!checkNode || handlerType == null) {
                attributes.put("permissionMsg", "未匹配到合适的处理类型");
                return true;
            }
            if (!this.checkHandlerType(handlerType, userModel, httpServletRequest, attributes)) {
                attributes.put("permissionMsg", "未找到匹配的数据");
                return true;
            }
            // 判断权限
            String permissionMsg = this.checkPermission(userModel, attributes, handlerType);
            attributes.put("permissionMsg", permissionMsg);

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

    /**
     * 检查权限
     *
     * @param userInfo    用户
     * @param attributes  属性
     * @param handlerType 功能类型
     * @return 错误消息
     */
    private String checkPermission(UserModel userInfo, Map<String, Object> attributes, HandlerType handlerType) {
        Object dataItem = attributes.get("dataItem");
        Object nodeInfo = attributes.get("nodeInfo");
        Object optData = dataItem == null ? nodeInfo : dataItem;
        String workspaceId = BeanUtil.getProperty(optData, "workspaceId");
        //?  : BeanUtil.getProperty(dataItem, "workspaceId");
        String useWorkspaceId;
        if (StrUtil.equals(workspaceId, ServerConst.WORKSPACE_GLOBAL)) {
            // 操作工作空间
            useWorkspaceId = (String) attributes.get("workspaceId");
        } else {
            // 数据工作空间
            useWorkspaceId = workspaceId;
            if (optData instanceof BaseWorkspaceModel && !StrUtil.equals(workspaceId, (String) attributes.get("workspaceId"))) {
                return "数据工作空间和操作工作空间不一致";
            }
        }
        if (optData instanceof BaseWorkspaceModel) {
            if (StrUtil.isEmpty(useWorkspaceId)) {
                return "没有找到数据对应的工作空间,不能进行操作";
            }
            // 将数据的工作空间设置为当前操作的工作空间
            BeanUtil.setProperty(optData, "workspaceId", useWorkspaceId);
        }
        //
        if (userInfo.isSuperSystemUser()) {
            return StrUtil.EMPTY;
        }
        if (userInfo.isDemoUser()) {
            return PermissionInterceptor.DEMO_TIP;
        }
        boolean isAssetsManager = Convert.toBool(attributes.get("isAssetsManager"), false);
        if (isAssetsManager && !userInfo.isSystemUser()) {
            // 判断资产权限
            return "您没有资产管理权限";
        }
        if (handlerType == HandlerType.nodeUpdate) {
            return "您没有对应功能【" + ClassFeature.NODE_UPGRADE.getName() + "】管理权限";
        }
        Class<?> handlerClass = handlerType.getHandlerClass();
        SystemPermission systemPermission = handlerClass.getAnnotation(SystemPermission.class);
        if (systemPermission != null) {
            if (!userInfo.isSuperSystemUser()) {
                return "您没有对应功能【" + ClassFeature.NODE_UPGRADE.getName() + "】管理权限";
            }
        }
        Feature feature = handlerClass.getAnnotation(Feature.class);
        MethodFeature method = feature.method();
        ClassFeature cls = feature.cls();
        UserBindWorkspaceModel.PermissionResult permissionResult = userBindWorkspaceService.checkPermission(userInfo, useWorkspaceId + StrUtil.DASHED + method.name());
        if (permissionResult.isSuccess()) {
            return StrUtil.EMPTY;
        }
        return permissionResult.errorMsg("对应功能【" + cls.getName() + "-" + method.getName() + "】");
    }

    private BaseWorkspaceModel checkData(HandlerType handlerType, UserModel userModel, HttpServletRequest httpServletRequest) {
        String id = httpServletRequest.getParameter("id");
        BaseWorkspaceService<?> workspaceService = SpringUtil.getBean(handlerType.getServiceClass());
        return workspaceService.getByKey(id, userModel);
    }

    /**
     * 解析参数，获取对应的数据
     *
     * @param handlerType        操作类型
     * @param userModel          用户
     * @param httpServletRequest 请求信息
     * @return 数据
     */
    private Tuple checkAssetsData(HandlerType handlerType, UserModel userModel, HttpServletRequest httpServletRequest) {
        String id = httpServletRequest.getParameter("id");
        return Opt.ofBlankAble(id).map(s -> {
            BaseWorkspaceService<?> workspaceService = SpringUtil.getBean(handlerType.getServiceClass());
            BaseWorkspaceModel workspaceModel = workspaceService.getByKey(s, userModel);
            String assetsLinkDataId = BeanUtil.getProperty(workspaceModel, handlerType.getAssetsLinkDataId());
            BaseDbService<?> assetsServiceClass = SpringUtil.getBean(handlerType.getAssetsServiceClass());
            return new Tuple(assetsServiceClass.getByKey(assetsLinkDataId, false), false, workspaceModel);
        }).orElseGet(() -> {
            String assetsLinkDataId = httpServletRequest.getParameter(handlerType.getAssetsLinkDataId());
            if (StrUtil.isEmpty(assetsLinkDataId)) {
                return null;
            }
            BaseDbService<?> assetsServiceClass = SpringUtil.getBean(handlerType.getAssetsServiceClass());
            return new Tuple(assetsServiceClass.getByKey(assetsLinkDataId, false), true, null);
        });
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("afterHandshake", exception);
        }
    }
}
