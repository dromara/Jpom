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

import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.model.data.ProjectLogFileItem;

import java.util.List;

/**
 * 项目运行日志文件提供者
 * <p>
 * 不同运行方式的项目（当前为 DSL）可以提供自己在项目控制台中可直接查看的日志文件。
 * 新增一种运行方式只需新增一个实现并注册为 Spring Bean（{@link ProjectLogFileService} 自动装配），
 * 控制台、WebSocket、日志大小统计等调用方无需改动。
 *
 * @author bwcx_jzy
 * @since 2026/11/19
 */
public interface ProjectLogFileProvider {

    /**
     * 支持的运行方式
     *
     * @return runMode
     */
    RunMode runMode();

    /**
     * 该项目可查看的日志文件列表
     *
     * @param nodeProjectInfoModel 项目
     * @return 日志文件列表（未配置返回空集合）
     */
    List<ProjectLogFileItem> list(NodeProjectInfoModel nodeProjectInfoModel);

    /**
     * 按日志标识解析日志文件
     *
     * @param nodeProjectInfoModel 项目
     * @param key                  日志标识（{@link ProjectLogFileItem#getKey()}）
     * @return 日志文件，未配置或不存在对应标识返回 null
     */
    ProjectLogFileItem resolve(NodeProjectInfoModel nodeProjectInfoModel, String key);
}
