package io.jpom.socket.handler;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.socket.BaseHandler;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * @author bwcx_jzy
 * @since 2022/2/10
 */
public abstract class BaseTerminalHandler extends BaseHandler {

	protected void sendBinary(WebSocketSession session, String msg) {
		if (StrUtil.isEmpty(msg)) {
			return;
		}
		if (!session.isOpen()) {
			// 会话关闭不能发送消息 @author jzy 21-08-04
			DefaultSystemLog.getLog().warn("回话已经关闭啦，不能发送消息：{}", msg);
			return;
		}
		synchronized (session.getId()) {
			BinaryMessage byteBuffer = new BinaryMessage(msg.getBytes());
			try {
				session.sendMessage(byteBuffer);
			} catch (IOException e) {
				DefaultSystemLog.getLog().error("发送消息失败:" + msg, e);
			}
		}
	}
}
