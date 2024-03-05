/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.plugins;

import java.nio.charset.Charset;

/**
 * @author bwcx_jzy
 * @since 2023/4/6
 */
public interface ISshInfo {

    int timeout();

    String host();

    ConnectType connectType();

    Charset charset();

    int port();

    String user();

    String password();

    /**
     * 私钥
     *
     * @return 私钥
     */
    String privateKey();

    /**
     * id
     *
     * @return 数据id
     */
    String id();

    enum ConnectType {
        /**
         * 账号密码
         */
        PASS,
        /**
         * 密钥
         */
        PUBKEY
    }
}
