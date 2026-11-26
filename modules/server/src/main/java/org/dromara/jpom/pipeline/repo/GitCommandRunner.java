/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.repo;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.util.CommandUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 系统 git 命令执行器
 * <p>
 * 只做三件事：拼参数、把输出实时写进运行日志、返回退出码。调用方必须带上
 * {@code GIT_TERMINAL_PROMPT=0}，否则私有仓库认证失败时会卡在交互式输入上把构建挂住。
 * </p>
 *
 * @author jpom
 * @since 2.11.15.3
 */
class GitCommandRunner {

    private final File workDir;
    private final Map<String, String> env;
    private final PipelineSourceSync.LogSink log;

    GitCommandRunner(File workDir, Map<String, String> env, PipelineSourceSync.LogSink log) {
        this.workDir = workDir;
        this.env = env;
        this.log = log;
    }

    /**
     * 执行命令并实时输出日志
     *
     * @param args 参数（不含 git 本身）
     * @return 退出码
     * @throws IOException IO
     */
    int run(String... args) throws IOException {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        return CommandUtil.exec(this.workDir, this.env, line -> {
            if (StrUtil.isNotEmpty(line)) {
                this.log.info(line);
            }
        }, command);
    }

    /**
     * 执行命令并返回输出（不写运行日志，用于 rev-parse 这类取值）
     *
     * @param args 参数
     * @return 输出（已 trim）
     * @throws IOException 执行失败（非 0 退出码）
     */
    String output(String... args) throws IOException {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        StringBuilder builder = new StringBuilder();
        int code = CommandUtil.exec(this.workDir, this.env, line -> {
            if (StrUtil.isNotEmpty(line)) {
                builder.append(line).append(StrUtil.LF);
            }
        }, command);
        if (code != 0) {
            throw new IOException("git " + ArrayUtil.join(args, StrUtil.SPACE) + " exit " + code);
        }
        return builder.toString().trim();
    }
}
