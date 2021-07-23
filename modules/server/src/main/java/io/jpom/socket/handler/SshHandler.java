package io.jpom.socket.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.jpom.model.data.SshModel;
import io.jpom.service.node.ssh.SshService;
import io.jpom.socket.BaseHandler;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ssh 处理2
 *
 * @author bwcx_jzy
 * @date 2019/8/9
 */
public class SshHandler extends BaseHandler {

	private static final ConcurrentHashMap<String, HandlerItem> HANDLER_ITEM_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		SshModel sshItem = (SshModel) session.getAttributes().get("sshItem");
		Map<String, String[]> parameterMap = (Map<String, String[]>) session.getAttributes().get("parameterMap");
//		String[] fileDirAlls;
//		//判断url是何操作请求
//		if (parameterMap.containsKey("tail")) {
//			fileDirAlls = parameterMap.get("tail");
//		} else if (parameterMap.containsKey("gz")) {
//			fileDirAlls = parameterMap.get("gz");
//		} else {
//			fileDirAlls = parameterMap.get("zip");
//		}
//		//检查文件路径
//		String fileDirAll = null;
//		if (fileDirAlls != null && fileDirAlls.length > 0 && !StrUtil.isEmptyOrUndefined(fileDirAlls[0])) {
//			fileDirAll = fileDirAlls[0];
//			List<String> fileDirs = sshItem.getFileDirs();
//			if (fileDirs == null) {
//				sendBinary(session, "没有配置路径");
//				return;
//			}
//			File file = FileUtil.file(fileDirAll);
//			boolean find = false;
//			for (String fileDir : fileDirs) {
//				if (FileUtil.isSub(FileUtil.file(fileDir), file)) {
//					find = true;
//					break;
//				}
//			}
//			if (!find) {
//				sendBinary(session, "非法路径");
//				return;
//			}
//		}
		Session openSession = SshService.getSession(sshItem);
		//JschUtil.openSession(sshItem.getHost(), sshItem.getPort(), sshItem.getUser(), sshItem.getPassword());
		Channel channel = JschUtil.createChannel(openSession, ChannelType.SHELL);
		InputStream inputStream = channel.getInputStream();
		OutputStream outputStream = channel.getOutputStream();
		//
		HandlerItem handlerItem = new HandlerItem(session, inputStream, outputStream, openSession, channel, sshItem);
		handlerItem.startRead();
		HANDLER_ITEM_CONCURRENT_HASH_MAP.put(session.getId(), handlerItem);
		//
		Thread.sleep(1000);
//		//截取当前操作文件父路径
//		String fileLocalPath = null;
//		if (fileDirAll != null && fileDirAll.lastIndexOf("/") > -1) {
//			fileLocalPath = fileDirAll.substring(0, fileDirAll.lastIndexOf("/"));
//		}
//		if (fileDirAll == null) {
//			this.call(session, StrUtil.CR);
//		} else if (parameterMap.containsKey("tail")) {
//			// 查看文件
//			fileDirAll = FileUtil.normalize(fileDirAll);
//			this.call(session, StrUtil.format("tail -f {}", fileDirAll));
//			this.call(session, StrUtil.CR);
//		} else if (parameterMap.containsKey("zip")) {
//			//解压zip
//			fileDirAll = FileUtil.normalize(fileDirAll);
//			this.call(session, StrUtil.format("unzip -o {} -d " + "{}", fileDirAll, fileLocalPath));
//			this.call(session, StrUtil.CR);
//		} else {
//			//解压 tar和tar.gz
//			fileDirAll = FileUtil.normalize(fileDirAll);
//			this.call(session, StrUtil.format("tar -xzvf {} -C " + "{}", fileDirAll, fileLocalPath));
//			this.call(session, StrUtil.CR);
//		}
	}

//	private void call(WebSocketSession session, String msg) throws Exception {
//		JSONObject first = new JSONObject();
//		first.put("data", msg);
//		// 触发消息
//		//this.handleTextMessage(session, new TextMessage(first.toJSONString()));
//	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
		String payload = message.getPayload();
		if (JSONValidator.from(payload).getType() == JSONValidator.Type.Object) {
			JSONObject jsonObject = JSONObject.parseObject(payload);
			String data = jsonObject.getString("data");
			if (StrUtil.equals(data, "jpom-heart")) {
				// 心跳消息不转发
				return;
			}
		}
		this.sendCommand(handlerItem, payload);
	}

	private void sendCommand(HandlerItem handlerItem, String data) throws IOException {
		if (handlerItem.addMsg(data)) {
			handlerItem.outputStream.write(data.getBytes());
		} else {
			handlerItem.outputStream.write("没有执行相关命令权限".getBytes());
		}
		handlerItem.outputStream.flush();
	}


	private class HandlerItem implements Runnable {
		private final WebSocketSession session;
		private final InputStream inputStream;
		private final OutputStream outputStream;
		private final Session openSession;
		private final Channel channel;
		private final SshModel sshItem;
		private final List<String> nowLineInput = new ArrayList<>();

		HandlerItem(WebSocketSession session,
					InputStream inputStream,
					OutputStream outputStream,
					Session openSession,
					Channel channel,
					SshModel sshItem) {
			this.session = session;
			this.inputStream = inputStream;
			this.outputStream = outputStream;
			this.openSession = openSession;
			this.channel = channel;
			this.sshItem = sshItem;
		}

		void startRead() throws JSchException {
			this.channel.connect();
			ThreadUtil.execute(this);
		}

		public boolean addMsg(String msg) {
			nowLineInput.add(msg);
			if (StrUtil.equals(msg, StrUtil.CR)) {
				String join = CollUtil.join(nowLineInput, "");
				nowLineInput.clear();
				// 检查禁止执行的命令
				String notAllowedCommand = sshItem.getNotAllowedCommand();
				if (StrUtil.isNotEmpty(notAllowedCommand)) {
					List<String> split = StrUtil.split(notAllowedCommand, StrUtil.COMMA);
					for (String s : split) {
						s = s.toLowerCase();
						if (StrUtil.startWithAny(join.toLowerCase(), s + StrUtil.SPACE, ("&" + s + StrUtil.SPACE), StrUtil.SPACE + s + StrUtil.SPACE)) {
							return false;
						}
					}
				}
			}
			return true;
		}

		@Override
		public void run() {
			try {
				byte[] buffer = new byte[1024];
				int i;
				//如果没有数据来，线程会一直阻塞在这个地方等待数据。
				while ((i = inputStream.read(buffer)) != -1) {
					sendBinary(session, new String(Arrays.copyOfRange(buffer, 0, i), sshItem.getCharsetT()));
				}
			} catch (Exception e) {
				if (!this.openSession.isConnected()) {
					return;
				}
				DefaultSystemLog.getLog().error("读取错误", e);
				SshHandler.this.destroy(this.session);
			}
		}
	}

	@Override
	public void destroy(WebSocketSession session) {
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (IOException ignored) {
		}
		HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
		IoUtil.close(handlerItem.inputStream);
		IoUtil.close(handlerItem.outputStream);
		JschUtil.close(handlerItem.channel);
		JschUtil.close(handlerItem.openSession);
	}

	private static void sendBinary(WebSocketSession session, String msg) {
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
