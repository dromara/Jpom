/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.interceptor.PermissionInterceptor;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.function.Supplier;

/**
 * socket 拦截器、鉴权
 *
 * @author bwcx_jzy
 * @since 2019/4/19
 */
@Slf4j
@Configuration
public class ServerWebSocketInterceptor implements HandshakeInterceptor {

    private final UserService userService;
    private final NodeService nodeService;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final MachineNodeServer machineNodeServer;

    public ServerWebSocketInterceptor(UserService userService,
                                      NodeService nodeService,
                                      UserBindWorkspaceService userBindWorkspaceService,
                                      MachineNodeServer machineNodeServer) {
        this.userService = userService;
        this.nodeService = nodeService;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.machineNodeServer = machineNodeServer;
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
        // 验证机器权限
        String machineId = httpServletRequest.getParameter("machineId");
        if (StringUtils.isNotEmpty(machineId)) {
            if (!userModel.isSystemUser()) {
                // 没有权限
                return false;
            }
            MachineNodeModel machine = machineNodeServer.getByKey(machineId);
            if (machine == null) {
                return false;
            }
            attributes.put("machine", machine);
        }
        return true;
    }

    private HandlerType fromType(HttpServletRequest httpServletRequest) {
        // 判断拦截类型
        String type = httpServletRequest.getParameter("type");
        HandlerType handlerType = EnumUtil.fromString(HandlerType.class, type, null);
        if (handlerType == null) {
            log.warn(I18nMessageUtil.get("i18n.incorrect_type_passed.d42e"), type);
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
            case systemLog:
            case agentLog:
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
            case freeScript:
                //
                MachineNodeModel machine = (MachineNodeModel) attributes.get("machine");
                if (machine == null) {
                    return false;
                }
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
            attributes.put("lang", httpServletRequest.getParameter("lang"));
            UserModel userModel = userService.checkUser(userId);
            if (userModel == null) {
                String string = I18nMessageUtil.get("i18n.user_not_exist.4892");
                attributes.put("permissionMsg", string);
                return true;
            }
            HandlerType handlerType = this.fromType(httpServletRequest);
            if (handlerType == null) {
                String string = I18nMessageUtil.get("i18n.no_matching_process_type.b468");
                attributes.put("permissionMsg", string);
                return true;
            }
            boolean checkNode = this.checkNode(httpServletRequest, attributes, userModel);
            if (!checkNode) {
                String string = I18nMessageUtil.get("i18n.no_matching_permission.09cf");
                attributes.put("permissionMsg", string);
                return true;
            }
            if (!this.checkHandlerType(handlerType, userModel, httpServletRequest, attributes)) {
                String string = I18nMessageUtil.get("i18n.no_matching_data_found.fe9d");
                attributes.put("permissionMsg", string);
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
                return I18nMessageUtil.get("i18n.data_workspace_mismatch.ae1d");
            }
        }
        if (optData instanceof BaseWorkspaceModel) {
            if (StrUtil.isEmpty(useWorkspaceId)) {
                return I18nMessageUtil.get("i18n.no_workspace_found_for_data.ac0f");
            }
            // 将数据的工作空间设置为当前操作的工作空间
            BeanUtil.setProperty(optData, "workspaceId", useWorkspaceId);
        }
        //
        if (userInfo.isSuperSystemUser()) {
            return StrUtil.EMPTY;
        }
        if (userInfo.isDemoUser()) {
            return PermissionInterceptor.DEMO_TIP.get();
        }
        boolean isAssetsManager = Convert.toBool(attributes.get("isAssetsManager"), false);
        if (isAssetsManager && !userInfo.isSystemUser()) {
            // 判断资产权限
            return I18nMessageUtil.get("i18n.no_asset_management_permission.739e");
        }
        Supplier<String> nodeUpgradeName = ClassFeature.NODE_UPGRADE.getName();
        if (handlerType == HandlerType.nodeUpdate) {
            return StrUtil.format(I18nMessageUtil.get("i18n.no_permission_for_function.b63d"), I18nMessageUtil.get(nodeUpgradeName.get()));
        }
        Class<?> handlerClass = handlerType.getHandlerClass();
        SystemPermission systemPermission = handlerClass.getAnnotation(SystemPermission.class);
        if (systemPermission != null) {
            if (!userInfo.isSuperSystemUser()) {
                return StrUtil.format(I18nMessageUtil.get("i18n.no_permission_for_function.b63d"), I18nMessageUtil.get(nodeUpgradeName.get()));
            }
        }
        Feature feature = handlerClass.getAnnotation(Feature.class);
        MethodFeature method = feature.method();
        ClassFeature cls = feature.cls();
        UserBindWorkspaceModel.PermissionResult permissionResult = userBindWorkspaceService.checkPermission(userInfo, useWorkspaceId + StrUtil.DASHED + method.name());
        if (permissionResult.isSuccess()) {
            return StrUtil.EMPTY;
        }
        return permissionResult.errorMsg(StrUtil.format(I18nMessageUtil.get("i18n.corresponding_function.5bb5"), I18nMessageUtil.get(cls.getName().get()), I18nMessageUtil.get(method.getName().get())));
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
