/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common;

import org.dromara.jpom.common.i18n.I18nMessageUtil;

import java.util.function.Supplier;

/**
 * @author bwcx_jzy
 * @since 2022/8/30
 */
public class ServerConst extends Const {

    /**
     * h2 数据库表名字段
     */
    public static final String TABLE_NAME = "TABLE_NAME";

    /**
     * id_rsa
     */
    public static final String ID_RSA = "_id_rsa";
    /**
     * sshkey
     */
    public static final String SSH_KEY = "sshkey";
    /**
     * 引用工作空间环境变量的前缀
     */
    public static final String REF_WORKSPACE_ENV = "$ref.wEnv.";
    /**
     * 引用工作脚本模板的前缀
     */
    public static final String REF_SCRIPT = "$ref.script.";

    public static final String PROXY_PATH = "Jpom-ProxyPath";

    /**
     * 分发包存储路径
     */
    public static final String OUTGIVING_FILE = "outgiving";
    /**
     * token自动续签状态码
     */
    public static final int RENEWAL_AUTHORIZE_CODE = 801;

    /**
     * token 失效
     */
    public static final int AUTHORIZE_TIME_OUT_CODE = 800;

    /**
     * 账号被锁定
     */
    public static final int ACCOUNT_LOCKED = 802;
    public static final Supplier<String> LOGIN_TIP = () -> I18nMessageUtil.get("i18n.login_info_expired_re_login.6bc4");
    public static final Supplier<String> ACCOUNT_LOCKED_TIP = () -> I18nMessageUtil.get("i18n.account_disabled.9361");

    public static final String CHECK_SYSTEM = "check-system";

    public static final String RSA = "RSA";

    public static final String EC = "EC";
}
