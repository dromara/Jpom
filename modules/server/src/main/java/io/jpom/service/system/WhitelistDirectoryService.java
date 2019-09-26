package io.jpom.service.system;

import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.model.data.NodeModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 白名单
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Service
public class WhitelistDirectoryService {

    public AgentWhitelist getData(NodeModel model) {
        return NodeForward.requestData(model, NodeUrl.WhitelistDirectory_data, null, AgentWhitelist.class);
    }

    /**
     * 获取项目路径白名单
     *
     * @param model 实体
     * @return project
     */
    public List<String> getProjectDirectory(NodeModel model) {
        AgentWhitelist agentWhitelist = getData(model);
        if (agentWhitelist == null) {
            return null;
        }
        return agentWhitelist.getProject();
    }

    public List<String> getNgxDirectory(NodeModel model) {
        AgentWhitelist agentWhitelist = getData(model);
        if (agentWhitelist == null) {
            return null;
        }
        return agentWhitelist.getNginx();
    }

    public List<String> getCertificateDirectory(NodeModel model) {
        AgentWhitelist agentWhitelist = getData(model);
        if (agentWhitelist == null) {
            return null;
        }
        return agentWhitelist.getCertificate();
    }


}
