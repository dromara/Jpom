package io.jpom.model.system;

import io.jpom.model.BaseJsonModel;

/**
 * agent 端自动生成的密码实体
 *
 * @author jiangzeyin
 * @date 2019/4/18
 */
public class AgentAutoUser extends BaseJsonModel {

    private String agentName;
    private String agentPwd;

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentPwd() {
        return agentPwd;
    }

    public void setAgentPwd(String agentPwd) {
        this.agentPwd = agentPwd;
    }
}
