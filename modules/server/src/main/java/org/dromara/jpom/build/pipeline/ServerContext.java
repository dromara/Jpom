package org.dromara.jpom.build.pipeline;

import cn.hutool.extra.spring.SpringUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
@Service
@Getter
@AllArgsConstructor
public class ServerContext {

    private final RepositoryService repositoryService;
    private final JpomApplication jpomApplication;

    /**
     * 获取服务的上下文
     *
     * @return 服务的上下文
     */
    public static ServerContext getInstance() {
        return SpringUtil.getBean(ServerContext.class);
    }
}
