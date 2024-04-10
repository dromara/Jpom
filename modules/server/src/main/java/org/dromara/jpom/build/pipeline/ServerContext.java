package org.dromara.jpom.build.pipeline;

import cn.hutool.extra.spring.SpringUtil;
import lombok.Getter;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
@Service
@Getter
public class ServerContext {

    private final RepositoryService repositoryService;

    public ServerContext(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * 获取服务的上下文
     *
     * @return 服务的上下文
     */
    public static ServerContext getInstance() {
        return SpringUtil.getBean(ServerContext.class);
    }
}
