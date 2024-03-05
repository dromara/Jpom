/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.exception;

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;

/**
 * 授权错误
 *
 * @author bwcx_jzy
 * @since 2019/4/17
 */
public class AgentAuthorizeException extends RuntimeException {
    private final JsonMessage<String> jsonMessage;

    public AgentAuthorizeException(JsonMessage<String> jsonMessage) {
        super(jsonMessage.getMsg());
        this.jsonMessage = jsonMessage;
    }

    public IJsonMessage<String> getJsonMessage() {
        return jsonMessage;
    }
}
