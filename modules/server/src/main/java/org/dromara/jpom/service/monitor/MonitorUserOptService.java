/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.monitor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.model.data.MonitorUserOptModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 监控用户操作Service
 *
 * @author bwcx_jzy
 */
@Service
public class MonitorUserOptService extends BaseWorkspaceService<MonitorUserOptModel> {

    /**
     * 查询 对应操作的监控信息
     *
     * @param workspaceId   工作空间ID
     * @param classFeature  功能
     * @param methodFeature 操作
     * @return list
     */
    public List<MonitorUserOptModel> listByType(String workspaceId, ClassFeature classFeature, MethodFeature methodFeature, String userId) {
        MonitorUserOptModel where = new MonitorUserOptModel();
        if (StrUtil.isNotEmpty(workspaceId)) {
            // 没有工作空间查询全部
            where.setWorkspaceId(workspaceId);
        }
        where.setStatus(true);
        List<MonitorUserOptModel> list = super.listByBean(where);
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        return list.stream().filter(monitorUserOptModel -> {
            List<ClassFeature> classFeatures = monitorUserOptModel.monitorFeature();
            List<MethodFeature> methodFeatures = monitorUserOptModel.monitorOpt();
            boolean b = CollUtil.contains(classFeatures, classFeature) && CollUtil.contains(methodFeatures, methodFeature);
            if (b) {
                List<String> monitorUser = monitorUserOptModel.monitorUser();
                return CollUtil.contains(monitorUser, userId);
            }
            return false;
        }).collect(Collectors.toList());
    }

//    public List<MonitorUserOptModel> listByType(UserOperateLogV1.OptType optType, String userId) {
//        List<MonitorUserOptModel> userOptModels = this.listByType(optType);
//        if (CollUtil.isEmpty(userOptModels)) {
//            return null;
//        }
//        return userOptModels.stream().filter(monitorUserOptModel -> {
//            List<String> monitorUser = monitorUserOptModel.getMonitorUser();
//            return CollUtil.contains(monitorUser, userId);
//        }).collect(Collectors.toList());
//    }
}
