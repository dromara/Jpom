package io.jpom.socket.client;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.WebSocketMessageModel;
import io.jpom.model.data.NodeModel;
import io.jpom.system.init.OperateLogController;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 节点Client
 *
 * @author lf
 */
public class NodeClient extends WebSocketClient {
    private final WebSocketSession session;
    private final OperateLogController logController;
    private final NodeModel nodeModel;


    public NodeClient(String uri, NodeModel nodeModel, WebSocketSession session) throws URISyntaxException {
        super(new URI(uri));
        this.session = session;
        this.nodeModel = nodeModel;
        this.connect();
        logController = SpringUtil.getBean(OperateLogController.class);
    }

    /**
     * 等待连接成功
     */
    private void loopOpen() {
        int count = 0;
        while (!this.isOpen() && count < 20) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            count++;
        }
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        // 连接成功后获取版本信息
        WebSocketMessageModel command = new WebSocketMessageModel("getVersion", this.nodeModel.getId());
        send(command.toString());
    }

    @Override
    public void onMessage(String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            DefaultSystemLog.getLog().error("发送消息失败", e);
        }
        try {
            JSONObject jsonObject = JSONObject.parseObject(message);
            String reqId = jsonObject.getString("reqId");
            if (StrUtil.isNotEmpty(reqId)) {
                logController.updateLog(reqId, message);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception e) {
        DefaultSystemLog.getLog().error("发生异常", e);
    }
}
