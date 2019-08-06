package cn.keepbx.jpom.system.init;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.keepbx.jpom.common.ServerOpenApi;
import cn.keepbx.jpom.system.AgentExtConfigBean;

/**
 * 自动注册server 节点
 *
 * @author bwcx_jzy
 * @date 2019/8/6
 */
@PreLoadClass
public class AutoRegSeverNode {

    @PreLoadMethod
    private static void reg() {
        System.out.println(AgentExtConfigBean.getInstance().getAgentUrl());
        String agentId = AgentExtConfigBean.getInstance().getAgentId();
        String serverUrl = AgentExtConfigBean.getInstance().getServerUrl();
        if (StrUtil.isEmpty(agentId) || StrUtil.isEmpty(serverUrl)) {
            //  如果二者缺一不注册
            return;
        }
        HttpRequest serverRequest = AgentExtConfigBean.getInstance().createServerRequest(ServerOpenApi.UPDATE_NODE_INFO);
    }
}
