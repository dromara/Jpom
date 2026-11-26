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
import org.dromara.jpom.plugin.PluginFactory;
import cn.keepbx.jpom.plugins.IPlugin;

import java.io.File;
import java.util.Map;

/**
 * 走现有 `svn-clone` 插件拉取
 * <p>
 * svn 没有浅克隆/本地镜像的概念，行为与改造前一致；抽成独立实现只是为了去掉
 * {@link PipelineSourceSync} 里的类型分支。
 * </p>
 *
 * @author jpom
 * @since 2.11.15.3
 */
public class PluginSvnSourceFetcher implements PipelineSourceFetcher {

    @Override
    public String label() {
        return "plugin-svn";
    }

    @Override
    public boolean supports(RepositoryModel repository) {
        return repository != null && !RepositorySupport.isGit(repository);
    }

    @Override
    public SourceFetchResult fetch(PipelineSource source, RepositoryModel repository, File mountDir, File cacheRoot,
                                   PipelineSourceSync.LogSink log) throws Exception {
        Map<String, Object> map = repository.toMap();
        map.put("logWriter", log.writer());
        log.info(PipelineSourceMessages.text("i18n.pipeline_source_sync_begin.7d21")
            + source.getId() + " " + repository.getName() + " → " + mountDir.getName());
        IPlugin plugin = PluginFactory.getPlugin("svn-clone");
        Object result = plugin.execute(mountDir, map);
        return SourceFetchResult.fromPluginResult(result);
    }
}
