/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.repo;

import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.pipeline.model.PipelineSource;

import java.io.File;

/**
 * 仓库源拉取方式（变点在这里）
 * <p>
 * 流水线的源码获取有多种实现：本地 bare 缓存 + 系统 git、插件（系统 git / JGit）、svn 插件。
 * 调用方只按顺序挑第一个 {@link #supports(RepositoryModel)} 为真的实现，不再在业务里写
 * {@code if (repoType == ...)} 的类型分支；新增一种实现＝新增一个类 + 在
 * {@link PipelineSourceSync} 里注册一行。
 * </p>
 *
 * @author jpom
 * @since 2.11.15.3
 */
public interface PipelineSourceFetcher {

    /**
     * 实现标识（写日志用，不参与判定）
     *
     * @return 标识
     */
    String label();

    /**
     * 是否支持该仓库
     *
     * @param repository 仓库配置
     * @return true 支持
     */
    boolean supports(RepositoryModel repository);

    /**
     * 把源拉到挂载目录
     *
     * @param source     仓库源
     * @param repository 仓库配置
     * @param mountDir   挂载目录（本次运行的工作区，调用方已保证目录安全）
     * @param cacheRoot  缓存根目录（{@code data/pipeline/cache}）
     * @param log        日志出口
     * @return 拉取结果
     * @throws Exception 拉取失败（由调用方决定是否回退到下一种实现）
     */
    SourceFetchResult fetch(PipelineSource source, RepositoryModel repository, File mountDir, File cacheRoot,
                            PipelineSourceSync.LogSink log) throws Exception;
}
