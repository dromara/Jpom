package org.dromara.jpom.build.pipeline.actuator;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.EnumUtil;
import cn.keepbx.jpom.plugins.IPlugin;
import org.dromara.jpom.build.pipeline.ServerContext;
import org.dromara.jpom.build.pipeline.config.EmptyStage;
import org.dromara.jpom.build.pipeline.config.Repository;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.plugin.PluginFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public class RepositoryActuator extends BaseActuator<EmptyStage> {
    private final Repository repository;
    private final String tag;
    private final String concurrentTag;

    public RepositoryActuator(String concurrentTag, String tag, Repository repository) {
        this.tag = tag;
        this.concurrentTag = concurrentTag;
        this.repository = repository;
    }

    private File buildWorkDir() {
        return FileUtil.file(dataDir, "source", concurrentTag + "_" + tag);
    }

    @Override
    public void run() throws Exception {
        String repositoryId = repository.getRepositoryId();
        ServerContext instance = ServerContext.getInstance();
        RepositoryModel repositoryModel = instance.getRepositoryService().getByKey(repositoryId);
        Assert.notNull(repositoryModel, "没有找到仓库信息");
        //
        Integer repoTypeCode = repositoryModel.getRepoType();
        RepositoryModel.RepoType repoType = EnumUtil.likeValueOf(RepositoryModel.RepoType.class, repoTypeCode);
        //
        // 指定 clone 深度
        Integer cloneDepth = repository.getCloneDepth();
        //
    }

    private void cloneByGit(RepositoryModel repositoryModel) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin("git-clone");
        Map<String, Object> map = repositoryModel.toMap();
        //
        Tuple tuple = (Tuple) plugin.execute("branchAndTagList", map);
    }

    private void cloneBySvn(RepositoryModel repositoryModel) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin("svn-clone");
        Map<String, Object> map = repositoryModel.toMap();
        File workDir = this.buildWorkDir();
        String[] result = (String[]) plugin.execute(workDir, map);
    }
}
