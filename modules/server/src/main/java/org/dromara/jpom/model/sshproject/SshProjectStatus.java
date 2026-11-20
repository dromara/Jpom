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

import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 * SSH 项目状态解析结果
 * <p>
 * 契约与现有项目一致：status 脚本最后一行输出 `running:<pid>[:<ports>]` 或 `stopped`；
 * 也接受 `running` 与 `running:0`（容器/systemd 场景没有本机 pid）。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Data
public class SshProjectStatus {

    private boolean running;
    private Integer pid;
    private String ports;
    private String message;

    public static SshProjectStatus stopped(String message) {
        SshProjectStatus status = new SshProjectStatus();
        status.running = false;
        status.message = message;
        return status;
    }

    /**
     * 解析 status 脚本输出（取最后一行）
     *
     * @param output 输出
     * @return 状态
     */
    public static SshProjectStatus parse(String output) {
        String lastLine = StrUtil.EMPTY;
        for (String line : StrUtil.split(output, '\n', true, true)) {
            lastLine = line;
        }
        if (StrUtil.isEmpty(lastLine)) {
            return stopped("empty status output");
        }
        String line = lastLine.trim();
        if (StrUtil.equalsIgnoreCase(line, "stopped") || StrUtil.equalsIgnoreCase(line, "stop")) {
            SshProjectStatus status = stopped(line);
            return status;
        }
        if (!StrUtil.startWithIgnoreCase(line, "running")) {
            return stopped(line);
        }
        SshProjectStatus status = new SshProjectStatus();
        status.running = true;
        status.message = line;
        String[] parts = StrUtil.splitToArray(line, ':');
        if (parts.length > 1) {
            Integer pid = StrUtil.isNumeric(parts[1]) ? Integer.parseInt(parts[1]) : null;
            status.pid = pid;
        }
        if (parts.length > 2) {
            status.ports = parts[2];
        }
        return status;
    }
}
