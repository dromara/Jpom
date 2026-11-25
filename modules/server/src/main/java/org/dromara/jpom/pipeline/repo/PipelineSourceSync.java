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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.pipeline.model.PipelineSource;
import cn.keepbx.jpom.plugins.IPlugin;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 流水线仓库源同步：把每个源拉到工作区对应的挂载目录
 * <p>
 * 多仓库的核心就在这里：一个运行一个工作区，每个源一个子目录（mount），
 * 后面的构建阶段用相对路径（cwd）在各自目录里干活，互不干扰。
 * 拉取复用「仓库管理」里的 git/svn 插件，不新写一套传输。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Service
public class PipelineSourceSync {

    private final RepositoryService repositoryService;

    public PipelineSourceSync(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * 同步全部源
     *
     * @param pipelineId 流水线
     * @param sources    源
     * @param dirs       与 sources 一一对应的挂载目录
     * @param logSink    日志
     * @return 变量：sourceId → commit/版本号
     */
    public Map<String, String> sync(String pipelineId, List<PipelineSource> sources, List<File> dirs, LogSink logSink) throws Exception {
        Map<String, String> commits = new TreeMap<>();
        for (int i = 0; i < sources.size(); i++) {
            PipelineSource source = sources.get(i);
            File dir = dirs.get(i);
            String revision = this.syncOne(pipelineId, source, dir, logSink);
            commits.put(source.getId(), StrUtil.nullToEmpty(revision));
        }
        return commits;
    }

    private String syncOne(String pipelineId, PipelineSource source, File dir, LogSink logSink) throws Exception {
        RepositoryModel repository = repositoryService.getByKey(source.getRepoId(), false);
        if (repository == null) {
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.pipeline_source_repo_required.c5f2") + source.getRepoId());
        }
        Integer repoType = repository.getRepoType();
        boolean git = repoType == null || repoType == RepositoryModel.RepoType.Git.ordinal();
        Map<String, Object> map = repository.toMap();
        // 空目录会让 git 插件走 reClone 分支（实测 NPE）；首次拉取前先删掉空目录，让它全新 clone
        if (dir.isDirectory() && FileUtil.isDirEmpty(dir)) {
            FileUtil.del(dir);
        }
        map.put("savePath", dir);
        // git 插件会把该值拆箱成 int：不传就 NPE（实测 reClone 行 143），这里给默认进度比
        map.put("reduceProgressRatio", 100);
        if (source.getCloneDepth() != null) {
            map.put("depth", source.getCloneDepth());
        }
        map.put("logWriter", logSink.writer());
        logSink.info("同步仓库源 [" + source.getId() + "] " + repository.getName() + " → " + dir.getName());
        if (git) {
            IPlugin plugin = PluginFactory.getPlugin("git-clone");
            String ref = StrUtil.trimToEmpty(source.getRef());
            if (StrUtil.startWith(ref, "tag:")) {
                map.put("tagName", StrUtil.removePrefix(ref, "tag:"));
                return extractCommit(plugin.execute("pullByTag", map));
            }
            map.put("branchName", StrUtil.removePrefix(ref, "branch:"));
            return extractCommit(plugin.execute("pull", map));
        }
        IPlugin plugin = PluginFactory.getPlugin("svn-clone");
        Object result = plugin.execute(dir, map);
        return extractCommit(result);
    }

    private String extractCommit(Object result) {
        if (result instanceof String[]) {
            String[] array = (String[]) result;
            return array.length > 0 ? array[0] : null;
        }
        return null;
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