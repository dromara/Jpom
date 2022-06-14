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

import cn.hutool.core.util.StrUtil;
import io.jpom.socket.BaseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * @author bwcx_jzy
 * @since 2022/2/10
 */
@Slf4j
public abstract class BaseTerminalHandler extends BaseHandler {

	protected void sendBinary(WebSocketSession session, String msg) {
		if (StrUtil.isEmpty(msg)) {
			return;
		}
		if (!session.isOpen()) {
			// 会话关闭不能发送消息 @author jzy 21-08-04
			log.warn("回话已经关闭啦，不能发送消息：{}", msg);
			return;
		}
		synchronized (session.getId()) {
			BinaryMessage byteBuffer = new BinaryMessage(msg.getBytes());
			try {
				session.sendMessage(byteBuffer);
			} catch (IOException e) {
				log.error("发送消息失败:" + msg, e);
			}
		}
	}
}
