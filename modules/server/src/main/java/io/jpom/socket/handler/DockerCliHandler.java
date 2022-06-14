/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.socket.handler;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.docker.DockerInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * docker cli
 *
 * @author bwcx_jzy
 * @since 2022/02/10
 */
@Feature(cls = ClassFeature.DOCKER, method = MethodFeature.EXECUTE)
@Slf4j
public class DockerCliHandler extends BaseTerminalHandler {

	private static final ConcurrentHashMap<String, HandlerItem> HANDLER_ITEM_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

	@Override
	public void afterConnectionEstablishedImpl(WebSocketSession session) throws Exception {
		Map<String, Object> attributes = session.getAttributes();
		DockerInfoModel dockerInfoModel = (DockerInfoModel) attributes.get("dataItem");
		String containerId = (String) attributes.get("containerId");
		super.logOpt(this.getClass(), attributes, attributes);
		//
		HandlerItem handlerItem;
		try {
			handlerItem = new HandlerItem(session, dockerInfoModel, containerId);
			handlerItem.startRead();
		} catch (Exception e) {
			// 输出超时日志 @author jzy
			log.error("docker 控制台连接超时", e);
			sendBinary(session, "docker 控制台连接超时");
			this.destroy(session);
			return;
		}
		HANDLER_ITEM_CONCURRENT_HASH_MAP.put(session.getId(), handlerItem);
		//
		Thread.sleep(1000);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
		if (handlerItem == null) {
			sendBinary(session, "已经离线啦");
			IoUtil.close(session);
			return;
		}
		String payload = message.getPayload();
		if (JSONValidator.from(payload).getType() == JSONValidator.Type.Object) {
			JSONObject jsonObject = JSONObject.parseObject(payload);
			String data = jsonObject.getString("data");
			if (StrUtil.equals(data, "jpom-heart")) {
				// 心跳消息不转发
				return;
			}
			if (StrUtil.equals(data, "resize")) {
				// 缓存区大小
				//handlerItem.resize(jsonObject);
				return;
			}
		}

		try {
			this.sendCommand(handlerItem, payload);
		} catch (Exception e) {
			sendBinary(session, "Failure:" + e.getMessage());
			log.error("执行命令异常", e);
		}
	}

	private void sendCommand(HandlerItem handlerItem, String data) throws Exception {
		handlerItem.outputStream.write(data.getBytes());
		handlerItem.outputStream.flush();
	}


	private class HandlerItem implements Runnable {
		private final WebSocketSession session;
		private final DockerInfoModel dockerInfoModel;
		private final PipedInputStream inputStream = new PipedInputStream();
		private final PipedOutputStream outputStream = new PipedOutputStream(inputStream);
		private final String containerId;

		HandlerItem(WebSocketSession session, DockerInfoModel dockerInfoModel, String containerId) throws IOException {
			this.session = session;
			this.dockerInfoModel = dockerInfoModel;
			this.containerId = containerId;
		}

		void startRead() {
			ThreadUtil.execute(this);
		}

		@Override
		public void run() {
			Map<String, Object> map = dockerInfoModel.toParameter();
			map.put("containerId", containerId);
			Consumer<String> logConsumer = s -> sendBinary(session, s);
			map.put("charset", CharsetUtil.CHARSET_UTF_8);
			map.put("stdin", inputStream);
			map.put("logConsumer", logConsumer);
			Consumer<String> errorConsumer = s -> {
				sendBinary(session, s);
				if (StrUtil.equals(s, "exit")) {
					// 退出
					destroy(session);
				}
			};
			map.put("errorConsumer", errorConsumer);
			IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
			try {
				plugin.execute("exec", map);
			} catch (Exception e) {
				log.error("执行容器命令异常", e);
				sendBinary(session, "执行异常:" + e.getMessage());
			}
		}
	}

	@Override
	public void destroy(WebSocketSession session) {
		HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
		if (handlerItem != null) {
			IoUtil.close(handlerItem.inputStream);
			IoUtil.close(handlerItem.outputStream);
		}
		IoUtil.close(session);
		HANDLER_ITEM_CONCURRENT_HASH_MAP.remove(session.getId());
	}
}
