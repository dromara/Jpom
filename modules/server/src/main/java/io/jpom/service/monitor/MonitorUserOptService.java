/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
import io.jpom.model.data.MonitorUserOptModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.MethodFeature;
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

	public List<MonitorUserOptModel> listByType(String workspaceId, ClassFeature classFeature, MethodFeature methodFeature) {
		MonitorUserOptModel where = new MonitorUserOptModel();
		where.setWorkspaceId(workspaceId);
		where.setStatus(true);
		List<MonitorUserOptModel> list = super.listByBean(where);
		if (CollUtil.isEmpty(list)) {
			return null;
		}
		return list.stream().filter(monitorUserOptModel -> {
			List<ClassFeature> classFeatures = monitorUserOptModel.monitorFeature();
			List<MethodFeature> methodFeatures = monitorUserOptModel.monitorOpt();
			return CollUtil.contains(classFeatures, classFeature) && CollUtil.contains(methodFeatures, methodFeature);
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
