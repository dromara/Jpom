/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

import javax.websocket.Session;
import java.io.IOException;

/**
 * socket 会话对象
 *
 * <p>发送锁直接使用会话对象（所有发送统一走本工具类），避免 KeyLock 在锁释放与删除缓存
 * 并发时可能出现两个线程同时进入临界区的问题；同时把失败重试从 11 次 × 500ms 收敛到
 * 2 次 × 100ms，避免对已断开/半开连接长时间持锁阻塞日志 Tailer 线程。
 *
 * @author bwcx_jzy
 * @since 2018/9/29
 */
@Slf4j
public class SocketSessionUtil {
    /**
     * 失败重试次数（不含首次）
     */
    private static final int ERROR_TRY_COUNT = 2;
    /**
     * 失败重试休眠时间（毫秒）
     */
    private static final long ERROR_RETRY_SLEEP = 100;

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
        Assert.state(session.isOpen(), "session close ");
        synchronized (session) {
            IOException exception = null;
            for (int tryCount = 0; tryCount <= ERROR_TRY_COUNT; tryCount++) {
                if (!session.isOpen()) {
                    throw exception != null ? exception : new IOException("session close");
                }
                try {
                    session.getBasicRemote().sendText(msg);
                    return;
                } catch (IOException e) {
                    exception = e;
                    log.warn("{}{}", I18nMessageUtil.get("i18n.send_message_failure_prefix.6f8c"), tryCount + 1, e);
                    if (tryCount < ERROR_TRY_COUNT) {
                        ThreadUtil.sleep(ERROR_RETRY_SLEEP);
                    }
                }
            }
            throw exception != null ? exception : new IOException("session close");
        }
    }
}
