/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.sshproject;

import cn.hutool.core.util.StrUtil;

import java.util.Map;

/**
 * SSH 项目脚本拼接工具（纯函数，便于单测）
 * <p>
 * 所有进入 shell 的值一律单引号包裹并转义，避免字段值注入。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
public final class SshProjectShell {

    private SshProjectShell() {
    }

    /**
     * shell 单引号转义
     *
     * @param value 值
     * @return 转义后的单引号字面量
     */
    public static String quote(String value) {
        String text = StrUtil.nullToEmpty(value);
        return "'" + StrUtil.replace(text, "'", "'\"'\"'") + "'";
    }

    /**
     * 生成环境变量导出语句
     *
     * @param env 环境变量
     * @return 多行 export
     */
    public static String envBlock(Map<String, String> env) {
        StringBuilder builder = new StringBuilder();
        if (env != null) {
            env.forEach((key, value) -> {
                if (StrUtil.isEmpty(key) || !key.matches("^[A-Za-z_][A-Za-z0-9_]*$")) {
                    return;
                }
                builder.append("export ").append(key).append("=").append(quote(value)).append(StrUtil.LF);
            });
        }
        return builder.toString();
    }

    /**
     * 生成执行命令（带超时，超时退出码 124）
     *
     * @param remoteScript 远端脚本绝对路径
     * @param args         参数
     * @param timeout      超时秒数
     * @return 命令
     */
    public static String execCommand(String remoteScript, String args, int timeout) {
        int seconds = Math.max(timeout, 1);
        return StrUtil.format("timeout -k 5 {} bash {} {}", seconds, quote(remoteScript), StrUtil.nullToEmpty(args));
    }
}
