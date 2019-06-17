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
        UserOperateLogV1.OptType type = null;
        switch (consoleCommandOp) {
            case start:
                type = UserOperateLogV1.OptType.Script_Start;
                break;
            case stop:
                type = UserOperateLogV1.OptType.Script_Stop;
                break;
            default:
                break;
        }
        if (type != null) {
            // 记录操作日志
            UserModel userInfo = (UserModel) attributes.get("userInfo");
            //
            String tomcatId = (String) attributes.get("tomcatId");
            OperateLogController.CacheInfo cacheInfo = cacheInfo(attributes, json, type, tomcatId);
            try {
                operateLogController.log(userInfo, "文件读取中...", cacheInfo);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("记录操作日志异常", e);
            }
        }
        proxySession.send(json.toString());
    }
}
