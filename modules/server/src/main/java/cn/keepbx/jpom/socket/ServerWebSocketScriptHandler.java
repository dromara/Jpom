package cn.keepbx.jpom.socket;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.system.init.OperateLogController;
import com.alibaba.fastjson.JSONObject;

import java.util.Map;

/**
 * 脚本模板消息控制器
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
public class ServerWebSocketScriptHandler extends BaseServerWebSocketHandler {

    public ServerWebSocketScriptHandler() {
        super(NodeUrl.Script_Run, "scriptId");
    }

    @Override
    protected void handleTextMessage(Map<String, Object> attributes, ProxySession proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) {
        UserOperateLogV1.OptType type = null;
        switch (consoleCommandOp) {
            case stop:
                type = UserOperateLogV1.OptType.Script_Stop;
                break;
            case start:
                type = UserOperateLogV1.OptType.Script_Start;
                break;
            default:
                break;
        }
        if (type != null) {
            // 记录操作日志
            UserModel userInfo = (UserModel) attributes.get("userInfo");
            //
            String scriptId = (String) attributes.get("scriptId");
            OperateLogController.CacheInfo cacheInfo = cacheInfo(attributes, json, type, scriptId);
            try {
                operateLogController.log(userInfo, "脚本模板执行...", cacheInfo);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("记录操作日志异常", e);
            }
        }
        proxySession.send(json.toString());
    }
}
