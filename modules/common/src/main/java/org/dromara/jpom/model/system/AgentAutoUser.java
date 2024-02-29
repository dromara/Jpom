/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.system;

import cn.keepbx.jpom.model.BaseJsonModel;

/**
 * agent 端自动生成的密码实体
 *
 * @author bwcx_jzy
 * @since 2019/4/18
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
