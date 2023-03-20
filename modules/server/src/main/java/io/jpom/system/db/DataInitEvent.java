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
package io.jpom.system.db;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.spring.SpringUtil;
import io.jpom.common.ILoadEvent;
import io.jpom.common.ServerConst;
import io.jpom.model.BaseIdModel;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.service.IStatusRecover;
import io.jpom.service.h2db.BaseGroupService;
import io.jpom.service.h2db.BaseNodeGroupService;
import io.jpom.service.h2db.BaseNodeService;
import io.jpom.service.system.WorkspaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import top.jpom.h2db.TableName;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据库初始化完成后
 *
 * @author bwcx_jzy
 * @since 2023/2/18
 */
@Configuration
@Slf4j
public class DataInitEvent implements ILoadEvent {

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        //
        Map<String, BaseGroupService> groupServiceMap = SpringUtil.getApplicationContext().getBeansOfType(BaseGroupService.class);
        for (BaseGroupService<?> value : groupServiceMap.values()) {
            value.repairGroupFiled();
        }
        //
        Map<String, BaseNodeGroupService> nodeGroupServiceMap = SpringUtil.getApplicationContext().getBeansOfType(BaseNodeGroupService.class);
        for (BaseNodeGroupService<?> value : nodeGroupServiceMap.values()) {
            value.repairGroupFiled();
        }
        // 状态恢复的数据
        Map<String, IStatusRecover> statusRecoverMap = SpringUtil.getApplicationContext().getBeansOfType(IStatusRecover.class);
        statusRecoverMap.forEach((name, iCron) -> {
            int count = iCron.statusRecover();
            if (count > 0) {
                log.info("{} 恢复 {} 条异常数据", name, count);
            }
        });
        //  同步项目
        Map<String, BaseNodeService> beansOfType = SpringUtil.getApplicationContext().getBeansOfType(BaseNodeService.class);
        for (BaseNodeService<?> value : beansOfType.values()) {
            value.syncAllNode();
        }
        ThreadUtil.execute(() -> {
            try {
                checkErrorWorkspace();
            } catch (Exception e) {
                log.error("查询错误的工作空间失败", e);
            }
        });
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 2;
    }


    private void checkErrorWorkspace() {
        // 判断是否存在关联数据
        WorkspaceService workspaceService = SpringUtil.getBean(WorkspaceService.class);
        List<WorkspaceModel> list = workspaceService.list();
        Set<String> workspaceIds = Optional.ofNullable(list)
                .map(workspaceModels -> workspaceModels.stream()
                        .map(BaseIdModel::getId)
                        .collect(Collectors.toSet()))
                .orElse(new HashSet<>());
        // 添加默认的全局工作空间 id
        workspaceIds.add(ServerConst.WORKSPACE_GLOBAL);
        Set<Class<?>> classes = BaseWorkspaceModel.allClass();
        for (Class<?> aClass : classes) {
            TableName tableName = aClass.getAnnotation(TableName.class);
            if (tableName == null) {
                continue;
            }
            String sql = "select `workspaceId`,count(1) as allCount from " + tableName.value() + " group by `workspaceId`";
            List<Entity> query = workspaceService.query(sql);
            for (Entity entity : query) {
                String workspaceId = (String) entity.get("workspaceId");
                long allCount = (long) entity.get("allCount");
                if (workspaceIds.contains(workspaceId)) {
                    continue;
                }
                log.error("表 {}[{}] 存在 {} 条错误工作空间数据 -> {}", tableName.name(), tableName.value(), allCount, workspaceId);
            }
        }
    }
}
