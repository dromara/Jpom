/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket.handler;

import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.socket.BaseHandler;
import org.dromara.jpom.util.SocketSessionUtil;
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
        if (msg == null) {
            return;
        }
        BinaryMessage byteBuffer = new BinaryMessage(msg.getBytes());
        try {
            SocketSessionUtil.send(session, byteBuffer);
        } catch (IOException e) {
            log.error(I18nMessageUtil.get("i18n.send_message_failure_prefix.6f8c") + msg, e);
        }
    }
}
