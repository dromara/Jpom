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
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.pipeline.model.PipelineSource;
import org.dromara.jpom.plugin.PluginFactory;
import cn.keepbx.jpom.plugins.IPlugin;
import org.dromara.jpom.plugin.GitProcessFactory;

import java.io.File;
import java.util.Map;

/**
 * 走现有 `git-clone` 插件拉取（兜底实现）
 * <p>
 * 与「在线构建」的 {@code BuildExecuteManage#pullGit} 保持同一口径：设置了克隆深度就必须切到系统 git，
 * 否则 JGit 的实现根本不认 depth 参数（参数被静默丢弃）。系统没有安装 git 时插件会退回 JGit，
 * 这里明确在运行日志里说明「深度不生效」，不允许再出现「配了没反应也没提示」。
 * </p>
 *
 * @author jpom
 * @since 2.11.15.3
 */
public class PluginGitSourceFetcher implements PipelineSourceFetcher {

    @Override
    public String label() {
        return "plugin-git";
    }

    @Override
    public boolean supports(RepositoryModel repository) {
        return RepositorySupport.isGit(repository);
    }

    @Override
    public SourceFetchResult fetch(PipelineSource source, RepositoryModel repository, File mountDir, File cacheRoot,
                                   PipelineSourceSync.LogSink log) throws Exception {
        Map<String, Object> map = buildParameter(source, repository);
        Integer depth = source.effectiveCloneDepth();
        if (depth != null && !GitProcessFactory.existsSystemGit()) {
            log.info(PipelineSourceMessages.text("i18n.pipeline_source_depth_unsupported.f3c1"));
        }
        // 空目录会让 git 插件走 reClone 分支（实测 NPE）；首次拉取前先删掉空目录，让它全新 clone
        if (mountDir.isDirectory() && FileUtil.isDirEmpty(mountDir)) {
            FileUtil.del(mountDir);
        }
        map.put("savePath", mountDir);
        // git 插件会把该值拆箱成 int：不传就 NPE（实测 reClone 行 143），这里给默认进度比
        map.put("reduceProgressRatio", 100);
        map.put("logWriter", log.writer());
        log.info(PipelineSourceMessages.text("i18n.pipeline_source_sync_begin.7d21")
            + source.getId() + " " + repository.getName() + " → " + mountDir.getName());
        IPlugin plugin = PluginFactory.getPlugin("git-clone");
        Object result;
        if (source.isTagRef()) {
            map.put("tagName", source.refName());
            result = plugin.execute("pullByTag", map);
        } else {
            String branchName = source.refName();
            if (StrUtil.isNotEmpty(branchName)) {
                map.put("branchName", branchName);
            }
            result = plugin.execute("pull", map);
        }
        return SourceFetchResult.fromPluginResult(result);
    }

    /**
     * 构造 git 插件参数（可单测：克隆深度必须同时带 {@code gitProcessType=SystemGit}）
     *
     * @param source     仓库源
     * @param repository 仓库配置
     * @return 插件参数
     */
    static Map<String, Object> buildParameter(PipelineSource source, RepositoryModel repository) {
        Map<String, Object> map = repository.toMap();
        Integer depth = source.effectiveCloneDepth();
        if (depth != null) {
            map.put("depth", depth);
            // 只有系统 git 实现会消费 depth（JGit 5.x 没有浅克隆能力），与在线构建保持一致
            map.put("gitProcessType", "SystemGit");
        }
        return map;
    }
}
