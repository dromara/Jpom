/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.log;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.model.data.ProjectLogFileItem;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 项目运行日志文件统一入口
 * <p>
 * 按项目运行方式选择对应的 {@link ProjectLogFileProvider}，未注册对应实现的运行方式返回空列表。
 *
 * @author bwcx_jzy
 * @since 2026/11/19
 */
@Service
public class ProjectLogFileService {

    private final Map<RunMode, ProjectLogFileProvider> providerMap;

    public ProjectLogFileService(List<ProjectLogFileProvider> providers) {
        Map<RunMode, ProjectLogFileProvider> map = new EnumMap<>(RunMode.class);
        if (CollUtil.isNotEmpty(providers)) {
            for (ProjectLogFileProvider provider : providers) {
                map.put(provider.runMode(), provider);
            }
        }
        this.providerMap = Collections.unmodifiableMap(map);
    }

    /**
     * 获取项目可查看的日志文件列表
     *
     * @param nodeProjectInfoModel 项目
     * @return 日志文件列表
     */
    public List<ProjectLogFileItem> list(NodeProjectInfoModel nodeProjectInfoModel) {
        ProjectLogFileProvider provider = this.provider(nodeProjectInfoModel);
        if (provider == null) {
            return Collections.emptyList();
        }
        return CollUtil.emptyIfNull(provider.list(nodeProjectInfoModel));
    }

    /**
     * 按日志标识解析日志文件
     *
     * @param nodeProjectInfoModel 项目
     * @param key                  日志标识
     * @return 日志文件，未找到返回 null
     */
    public ProjectLogFileItem resolve(NodeProjectInfoModel nodeProjectInfoModel, String key) {
        if (StrUtil.isEmpty(key)) {
            return null;
        }
        ProjectLogFileProvider provider = this.provider(nodeProjectInfoModel);
        if (provider == null) {
            return null;
        }
        return provider.resolve(nodeProjectInfoModel, key);
    }

    /**
     * 当前项目对应的日志文件提供者
     *
     * @param nodeProjectInfoModel 项目
     * @return provider or null
     */
    public ProjectLogFileProvider provider(NodeProjectInfoModel nodeProjectInfoModel) {
        if (nodeProjectInfoModel == null || nodeProjectInfoModel.getRunMode() == null) {
            return null;
        }
        return providerMap.get(nodeProjectInfoModel.getRunMode());
    }
}
