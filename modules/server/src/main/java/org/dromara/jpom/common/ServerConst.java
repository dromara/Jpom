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
package org.dromara.jpom.common;

/**
 * @author bwcx_jzy
 * @since 2022/8/30
 */
public class ServerConst extends Const {

    public static final String GROUP_STR = "group";

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
    public static final String LOGIN_TIP = "登录信息已失效,重新登录";
    public static final String ACCOUNT_LOCKED_TIP = "账号已经被禁用,不能使用";
}
