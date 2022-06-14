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
package io.jpom.service.dblog;

import cn.hutool.core.bean.BeanPath;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.Const;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.MonitorModel;
import io.jpom.model.data.MonitorUserOptModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.monitor.NotifyUtil;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.h2db.BaseDbService;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.service.monitor.MonitorUserOptService;
import io.jpom.service.system.WorkspaceService;
import io.jpom.service.user.UserService;
import io.jpom.system.init.OperateLogController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@Service
@Slf4j
public class DbUserOperateLogService extends BaseWorkspaceService<UserOperateLogV1> {

    private final MonitorUserOptService monitorUserOptService;
    private final UserService userService;
    private final WorkspaceService workspaceService;
    /**
     * 通用 bean 的名称字段 bean-path
     */
    private static final BeanPath[] NAME_BEAN_PATHS = new BeanPath[]{BeanPath.create("name"), BeanPath.create("title")};

    public DbUserOperateLogService(MonitorUserOptService monitorUserOptService,
                                   UserService userService,
                                   WorkspaceService workspaceService) {
        this.monitorUserOptService = monitorUserOptService;
        this.userService = userService;
        this.workspaceService = workspaceService;
    }

    /**
     * 根据 数据ID 和 节点ID 查询相关数据名称
     *
     * @param classFeature     功能
     * @param cacheInfo        操作缓存
     * @param userOperateLogV1 操作日志
     * @return map
     */
    private Map<String, Object> buildDataMsg(ClassFeature classFeature, OperateLogController.CacheInfo cacheInfo, UserOperateLogV1 userOperateLogV1) {
        Map<String, Object> optDataNameMap = cacheInfo.getOptDataNameMap();
        if (optDataNameMap != null) {
            return optDataNameMap;
        }
        return this.buildDataMsg(classFeature, userOperateLogV1.getDataId(), userOperateLogV1.getNodeId());
    }

    /**
     * 根据 数据ID 和 节点ID 查询相关数据名称
     *
     * @param classFeature 功能
     * @param dataId       数据ID
     * @param nodeId       节点ID
     * @return map
     */
    public Map<String, Object> buildDataMsg(ClassFeature classFeature, String dataId, String nodeId) {
        if (classFeature == null) {
            return null;
        }
        Class<? extends BaseDbService<?>> dbService = classFeature.getDbService();
        if (dbService == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("数据id", dataId);
        BaseDbService<?> baseDbCommonService = SpringUtil.getBean(dbService);
        Object data = baseDbCommonService.getData(nodeId, dataId);
        map.put("数据名称", this.tryGetBeanName(data));
        //
        map.put("节点id", nodeId);
        ClassFeature parent = classFeature.getParent();
        if (parent == ClassFeature.NODE) {
            Class<? extends BaseDbService<?>> dbServiceParent = parent.getDbService();
            BaseDbService<?> baseDbCommonServiceParent = SpringUtil.getBean(dbServiceParent);
            Object dataParent = baseDbCommonServiceParent.getData(nodeId, dataId);
            map.put("节点名称", this.tryGetBeanName(dataParent));
        }
        return map;
    }

    private Object tryGetBeanName(Object data) {
        for (BeanPath beanPath : NAME_BEAN_PATHS) {
            Object o = beanPath.get(data);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    private String buildContent(UserModel optUserItem, Map<String, Object> dataMap, WorkspaceModel workspaceModel, String optTypeMsg, UserOperateLogV1 userOperateLogV1) {
        Map<String, Object> map = new LinkedHashMap<>(10);
        map.put("操作用户", optUserItem.getName());
        map.put("操作状态码", userOperateLogV1.getOptStatus());
        map.put("操作类型", optTypeMsg);
        if (workspaceModel != null) {
            map.put("所属工作空间", workspaceModel.getName());
        }
        map.put("操作IP", userOperateLogV1.getIp());
        if (dataMap != null) {
            map.putAll(dataMap);
        }
        List<String> list = map.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> entry.getKey() + "：" + entry.getValue())
                .collect(Collectors.toList());
        //
        return CollUtil.join(list, StrUtil.LF);
    }

    /**
     * 判断当前操作是否需要报警
     *
     * @param userOperateLogV1 操作信息
     * @param cacheInfo        操作缓存相关
     */
    private void checkMonitor(UserOperateLogV1 userOperateLogV1, OperateLogController.CacheInfo cacheInfo) {
        ClassFeature classFeature = EnumUtil.fromString(ClassFeature.class, userOperateLogV1.getClassFeature(), null);
        MethodFeature methodFeature = EnumUtil.fromString(MethodFeature.class, userOperateLogV1.getMethodFeature(), null);
        UserModel optUserItem = userService.getByKey(userOperateLogV1.getUserId());
        if (classFeature == null || methodFeature == null || optUserItem == null) {
            return;
        }
        Map<String, Object> dataMap = this.buildDataMsg(classFeature, cacheInfo, userOperateLogV1);
        WorkspaceModel workspaceModel = workspaceService.getByKey(userOperateLogV1.getWorkspaceId());

        String optTypeMsg = StrUtil.format(" 【{}】->【{}】", classFeature.getName(), methodFeature.getName());
        List<MonitorUserOptModel> monitorUserOptModels = monitorUserOptService.listByType(userOperateLogV1.getWorkspaceId(),
                classFeature,
                methodFeature,
                userOperateLogV1.getUserId());
        if (CollUtil.isEmpty(monitorUserOptModels)) {
            return;
        }
        String context = this.buildContent(optUserItem, dataMap, workspaceModel, optTypeMsg, userOperateLogV1);
        for (MonitorUserOptModel monitorUserOptModel : monitorUserOptModels) {
            List<String> notifyUser = monitorUserOptModel.notifyUser();
            if (CollUtil.isEmpty(notifyUser)) {
                continue;
            }
            for (String userId : notifyUser) {
                UserModel item = userService.getByKey(userId);
                if (item == null) {
                    continue;
                }
                // 邮箱
                String email = item.getEmail();
                if (StrUtil.isNotEmpty(email)) {
                    MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.mail, email);
                    ThreadUtil.execute(() -> {
                        try {
                            NotifyUtil.send(notify1, "用户操作报警", context);
                        } catch (Exception e) {
                            log.error("发送报警信息错误", e);
                        }
                    });

                }
                // dingding
                String dingDing = item.getDingDing();
                if (StrUtil.isNotEmpty(dingDing)) {
                    MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.dingding, dingDing);
                    ThreadUtil.execute(() -> {
                        try {
                            NotifyUtil.send(notify1, "用户操作报警", context);
                        } catch (Exception e) {
                            log.error("发送报警信息错误", e);
                        }
                    });
                }
                // 企业微信
                String workWx = item.getWorkWx();
                if (StrUtil.isNotEmpty(workWx)) {
                    MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.workWx, workWx);
                    ThreadUtil.execute(() -> {
                        try {
                            NotifyUtil.send(notify1, "用户操作报警", context);
                        } catch (Exception e) {
                            log.error("发送报警信息错误", e);
                        }
                    });
                }
            }
        }
    }

    /**
     * 插入操作日志
     *
     * @param userOperateLogV1 日志信息
     * @param cacheInfo        当前操作相关信息
     */
    public void insert(UserOperateLogV1 userOperateLogV1, OperateLogController.CacheInfo cacheInfo) {
        super.insert(userOperateLogV1);
        ThreadUtil.execute(() -> {
            try {
                this.checkMonitor(userOperateLogV1, cacheInfo);
            } catch (Exception e) {
                log.error("执行操作监控错误", e);
            }
        });
    }

    @Override
    public PageResultDto<UserOperateLogV1> listPage(HttpServletRequest request) {
        // 验证工作空间权限
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        String workspaceId = this.getCheckUserWorkspace(request);
        paramMap.put("workspaceId:in", workspaceId + StrUtil.COMMA + StrUtil.EMPTY);
        return super.listPage(paramMap);
    }

    @Override
    public String getCheckUserWorkspace(HttpServletRequest request) {
        // 忽略检查
        String header = ServletUtil.getHeader(request, Const.WORKSPACEID_REQ_HEADER, CharsetUtil.CHARSET_UTF_8);
        return ObjectUtil.defaultIfNull(header, StrUtil.EMPTY);
    }

    @Override
    protected void checkUserWorkspace(String workspaceId, UserModel userModel) {
        // 忽略检查
    }

    @Override
    protected String[] clearTimeColumns() {
        return new String[]{"optTime", "createTimeMillis"};
    }
}
