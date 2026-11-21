/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.sshproject;

import lombok.Data;

/**
 * SSH 项目脚本执行结果
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Data
public class SshProjectExecuteResult {

    /**
     * 脚本退出码，-100 表示未执行（如缺少流程配置）
     */
    private int exitCode = -100;
    /**
     * 输出（截断后的尾部）
     */
    private String output;
    /**
     * 是否执行成功（退出码 0）
     */
    private boolean success;
    /**
     * 失败原因
     */
    private String message;
    /**
     * 执行耗时毫秒
     */
    private long cost;

    public static SshProjectExecuteResult fail(String message) {
        SshProjectExecuteResult result = new SshProjectExecuteResult();
        result.message = message;
        return result;
    }

    public boolean timeout() {
        return this.exitCode == 124;
    }
}
