package cn.keepbx.jpom.service.system;

import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.AgentWhitelist;
import cn.keepbx.jpom.model.data.NodeModel;
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
