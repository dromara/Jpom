package io.jpom.service.system;

import io.jpom.model.system.WorkspaceModel;
import io.jpom.service.BaseOperService;
import io.jpom.system.AgentConfigBean;
import org.springframework.stereotype.Service;

/**
 * @ClassName AgentWorkspaceEnvVarService
 * @Description
 * @Author lidaofu
 * @Date 2022/3/8 9:16
 * @Version V1.0
 **/
@Service
public class AgentWorkspaceService extends BaseOperService<WorkspaceModel> {

    public AgentWorkspaceService() {
        super(AgentConfigBean.WORKSPACE_ENV_VAR);
    }
}
