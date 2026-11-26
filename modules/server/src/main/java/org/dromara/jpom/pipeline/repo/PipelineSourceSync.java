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

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.pipeline.engine.PipelineWorkspace;
import org.dromara.jpom.pipeline.model.PipelineSource;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 流水线仓库源同步：把每个源拉到工作区对应的挂载目录
 * <p>
 * 多仓库的核心就在这里：一个运行一个工作区，每个源一个子目录（mount），
 * 后面的构建阶段用相对路径（cwd）在各自目录里干活，互不干扰。
 * </p>
 * <p>
 * 拉取方式按 {@link PipelineSourceFetcher} 顺序挑选（缓存优先、插件兜底），本类只做
 * 「找仓库 → 选实现 → 记提交」，不再按仓库类型写分支；新增一种拉取方式＝新增实现类 + 在构造函数里注册一行。
 * </p>
 *
 * @author jpom
 * @since 2.11.15.1
 */
@Service
public class PipelineSourceSync {

    private final RepositoryService repositoryService;

    /**
     * 拉取实现（顺序即优先级：本地缓存 → 插件 git → 插件 svn）
     */
    private final List<PipelineSourceFetcher> fetchers;

    @Autowired
    public PipelineSourceSync(RepositoryService repositoryService) {
        this(repositoryService, Arrays.asList(
            new CachedGitSourceFetcher(),
            new PluginGitSourceFetcher(),
            new PluginSvnSourceFetcher()));
    }

    PipelineSourceSync(RepositoryService repositoryService, List<PipelineSourceFetcher> fetchers) {
        this.repositoryService = repositoryService;
        this.fetchers = Collections.unmodifiableList(fetchers);
    }

    /**
     * 同步全部源
     *
     * @param workspace  运行工作区（缓存目录也由它给出）
     * @param pipelineId 流水线
     * @param sources    源
     * @param dirs       与 sources 一一对应的挂载目录
     * @param logSink    日志
     * @return 变量：sourceId → commit/版本号
     */
    public Map<String, String> sync(PipelineWorkspace workspace, String pipelineId, List<PipelineSource> sources,
                                    List<File> dirs, LogSink logSink) throws Exception {
        Map<String, String> commits = new TreeMap<>();
        File cacheRoot = workspace.cacheDir();
        for (int i = 0; i < sources.size(); i++) {
            PipelineSource source = sources.get(i);
            SourceFetchResult result = this.syncOne(cacheRoot, source, dirs.get(i), logSink);
            commits.put(source.getId(), StrUtil.nullToEmpty(result.getCommitId()));
        }
        return commits;
    }

    /**
     * 同步单个源：按优先级挑第一个可用的实现，失败按顺序回退
     *
     * @param cacheRoot 缓存根目录
     * @param source    源
     * @param dir       挂载目录
     * @param logSink   日志
     * @return 结果
     * @throws Exception 全部实现都失败时抛出最后一个异常
     */
    SourceFetchResult syncOne(File cacheRoot, PipelineSource source, File dir, LogSink logSink) throws Exception {
        return this.syncOne(cacheRoot, this.findRepository(source), source, dir, logSink);
    }

    /**
     * 同步单个源（仓库已就绪；单测直接走这里，不需要数据库）
     *
     * @param cacheRoot  缓存根目录
     * @param repository 仓库配置
     * @param source     源
     * @param dir        挂载目录
     * @param logSink    日志
     * @return 结果
     * @throws Exception 全部实现都失败时抛出最后一个异常
     */
    SourceFetchResult syncOne(File cacheRoot, RepositoryModel repository, PipelineSource source, File dir,
                              LogSink logSink) throws Exception {
        Exception last = null;
        for (PipelineSourceFetcher fetcher : this.fetchers) {
            if (!fetcher.supports(repository)) {
                continue;
            }
            try {
                return fetcher.fetch(source, repository, dir, cacheRoot, logSink);
            } catch (Exception e) {
                last = e;
                // 回退必须留痕：否则用户会看到「缓存失效后每次又变回全量拉取」却不知道原因
                logSink.info(PipelineSourceMessages.text("i18n.pipeline_source_fetch_fallback.9d63")
                    + fetcher.label() + ": " + e.getMessage());
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException(PipelineSourceMessages.text("i18n.pipeline_source_repo_required.c5f2") + source.getRepoId());
    }

    /**
     * 读取仓库配置（不做填充：这里只需要地址与凭据）
     *
     * @param source 源
     * @return 仓库
     */
    private RepositoryModel findRepository(PipelineSource source) {
        RepositoryModel repository = repositoryService.getByKey(source.getRepoId(), false);
        if (repository == null) {
            throw new IllegalArgumentException(PipelineSourceMessages.text("i18n.pipeline_source_repo_required.c5f2") + source.getRepoId());
        }
        return repository;
    }

    /**
     * 日志出口（避免直接依赖 LogRecorder）
     */
    public interface LogSink {
        /**
         * 普通日志
         *
         * @param message 内容
         */
        void info(String message);

        /**
         * @return 供插件输出的 writer
         */
        PrintWriter writer();
    }
}
