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
package io.jpom.service.monitor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.model.data.MonitorUserOptModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.h2db.BaseWorkspaceService;
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
