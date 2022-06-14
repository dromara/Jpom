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
package io.jpom.util;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.websocket.Session;
import java.io.IOException;

/**
 * socket 会话对象
 *
 * @author jiangzeyin
 * @since 2018/9/29
 */
@Slf4j
public class SocketSessionUtil {
	/**
	 * 锁
	 */
	private static final KeyLock<String> LOCK = new KeyLock<>();
	/**
	 * 错误尝试次数
	 */
	private static final int ERROR_TRY_COUNT = 10;

	/**
	 * 发送消息
	 *
	 * @param session 会话对象
	 * @param msg     消息
	 * @throws IOException 异常
	 */
	public static void send(final Session session, String msg) throws IOException {
		if (StrUtil.isEmpty(msg)) {
			return;
		}
		if (!session.isOpen()) {
			throw new RuntimeException("session close ");
		}
		try {
			LOCK.lock(session.getId());
			IOException exception = null;
			int tryCount = 0;
			do {
				tryCount++;
				if (exception != null) {
					// 上一次有异常、休眠 500
					ThreadUtil.sleep(500);
				}
				try {
					session.getBasicRemote().sendText(msg);
					exception = null;
					break;
				} catch (IOException e) {
					log.error("发送消息失败:" + tryCount, e);
					exception = e;
				}
			} while (tryCount <= ERROR_TRY_COUNT);
			if (exception != null) {
				throw exception;
			}
		} finally {
			LOCK.unlock(session.getId());
		}
	}

	public static void send(WebSocketSession session, String msg) throws IOException {
		if (StrUtil.isEmpty(msg)) {
			return;
		}
		if (!session.isOpen()) {
			throw new RuntimeException("session close ");
		}
		try {
			LOCK.lock(session.getId());
			IOException exception = null;
			int tryCount = 0;
			do {
				tryCount++;
				if (exception != null) {
					// 上一次有异常、休眠 500
					ThreadUtil.sleep(500);
				}
				try {
					session.sendMessage(new TextMessage(msg));
					exception = null;
					break;
				} catch (IOException e) {
					log.error("发送消息失败:" + tryCount, e);
					exception = e;
				}
			} while (tryCount <= ERROR_TRY_COUNT);
			if (exception != null) {
				throw exception;
			}
		} finally {
			LOCK.unlock(session.getId());
		}
	}
}
