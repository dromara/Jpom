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
 * @author bwcx_jzy
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
        Assert.state(session.isOpen(), "session close ");
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
                    log.error("{}{}", I18nMessageUtil.get("i18n.send_message_failure_prefix.6f8c"), tryCount, e);
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
