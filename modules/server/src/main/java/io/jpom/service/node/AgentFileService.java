package io.jpom.service.node;

import io.jpom.common.BaseOperService;
import io.jpom.model.AgentFileModel;
import io.jpom.system.ServerConfigBean;
import org.springframework.stereotype.Service;

/**
 * @author lf
 */
@Service
public class AgentFileService  extends BaseOperService<AgentFileModel> {

    public AgentFileService() {
        super(ServerConfigBean.AGENT_FILE);
    }
}
