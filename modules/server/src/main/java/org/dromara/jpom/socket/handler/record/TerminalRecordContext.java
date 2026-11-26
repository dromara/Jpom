/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket.handler.record;

import lombok.Data;
import org.dromara.jpom.model.user.UserModel;

/**
 * 终端录制上下文
 *
 * @author bwcx_jzy
 * @since 2026/11/26
 */
@Data
public class TerminalRecordContext {
    /**
     * 会话 id
     */
    private String sessionId;
    /**
     * 工作空间
     */
    private String workspaceId;
    /**
     * 操作人
     */
    private String userId;
    /**
     * 老 SSH id（工作空间内资产，可为空）
     */
    private String sshId;
    private String sshName;
    /**
     * SSH 资产
     */
    private String machineSshId;
    private String machineSshName;
    /**
     * 来源 ip
     */
    private String ip;
    /**
     * 浏览器标识
     */
    private String userAgent;
    private int cols = 80;
    private int rows = 24;
    /**
     * 操作人对象（落库时用于填充修改人/校验空间，不参与序列化）
     */
    private UserModel userModel;
}
