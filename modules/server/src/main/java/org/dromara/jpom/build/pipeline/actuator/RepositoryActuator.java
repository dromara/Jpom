package org.dromara.jpom.build.pipeline.actuator;

import org.dromara.jpom.build.pipeline.ServerContext;
import org.dromara.jpom.build.pipeline.model.config.Repository;
import org.dromara.jpom.model.data.RepositoryModel;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public class RepositoryActuator extends BaseActuator {
    private final Repository repository;
    private final String tag;
    private boolean pull;

    public RepositoryActuator(String tag, Repository repository) {
        super(null);
        this.tag = tag;
        this.repository = repository;
    }

    @Override
    public void run() throws IOException {
        if (pull) {
            return;
        }
        String repositoryId = repository.getRepositoryId();
        ServerContext instance = ServerContext.getInstance();
        RepositoryModel repositoryModel = instance.getRepositoryService().getByKey(repositoryId);
        Assert.notNull(repositoryModel, "没有找到仓库信息");
    }
}
