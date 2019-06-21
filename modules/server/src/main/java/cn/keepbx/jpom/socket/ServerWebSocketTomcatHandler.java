package cn.keepbx.jpom.socket;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.system.init.OperateLogController;
import com.alibaba.fastjson.JSONObject;

import java.util.Map;

/**
 * 脚本模板消息控制器
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
public class ServerWebSocketTomcatHandler extends BaseServerWebSocketHandler {

    public ServerWebSocketTomcatHandler() {
        super(NodeUrl.Tomcat_Socket, "tomcatId");
    }

    @Override
    protected void handleTextMessage(Map<String, Object> attributes, ProxySession proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) {
        proxySession.send(json.toString());
    }
}
