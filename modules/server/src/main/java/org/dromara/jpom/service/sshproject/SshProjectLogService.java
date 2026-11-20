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
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * SSH 项目日志运维：备份、清空、下载、备份列表
 * <p>
 * 只操作 Jpom 自己维护的控制台日志（`~/.jpom/logs/{id}.log*`），不接受任意远端路径。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Service
public class SshProjectLogService {

    private final SshProjectExecutor sshProjectExecutor;

    public SshProjectLogService(SshProjectExecutor sshProjectExecutor) {
        this.sshProjectExecutor = sshProjectExecutor;
    }

    /**
     * 备份当前控制台日志
     *
     * @param project 项目
     * @return 备份文件名
     */
    public String backup(SshProjectModel project) {
        String script = "if [ -f \"$JPOM_LOG_FILE\" ]; then BACKUP=\"$JPOM_LOG_FILE.$(date +%Y%m%d%H%M%S)\"; cp -f \"$JPOM_LOG_FILE\" \"$BACKUP\"; basename \"$BACKUP\"; else echo \"\"; fi";
        SshProjectExecuteResult result = sshProjectExecutor.runScript(project, script, 30);
        Assert.state(result.isSuccess(), I18nMessageUtil.get("i18n.ssh_project_log_backup_fail.8c14") + StrUtil.nullToEmpty(result.getMessage()));
        return StrUtil.trimToEmpty(result.getOutput());
    }

    /**
     * 清空控制台日志
     *
     * @param project 项目
     */
    public void reset(SshProjectModel project) {
        SshProjectExecuteResult result = sshProjectExecutor.runScript(project, "mkdir -p \"$HOME/.jpom/logs\"; : > \"$JPOM_LOG_FILE\"", 15);
        Assert.state(result.isSuccess(), I18nMessageUtil.get("i18n.ssh_project_log_reset_fail.1d62") + StrUtil.nullToEmpty(result.getMessage()));
    }

    /**
     * 备份文件列表（按修改时间倒序）
     *
     * @param project 项目
     * @return 列表
     */
    public List<JSONObject> listBackup(SshProjectModel project) {
        SshProjectExecuteResult result = sshProjectExecutor.runScript(project,
            "ls -1t \"$HOME/.jpom/logs\" 2>/dev/null | grep -F \"" + project.getId() + ".log.\" || true", 20);
        List<JSONObject> list = new ArrayList<>();
        if (!result.isSuccess()) {
            return list;
        }
        for (String name : StrUtil.splitTrim(result.getOutput(), '\n')) {
            if (StrUtil.isEmpty(name) || StrUtil.contains(name, "/")) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("name", name);
            list.add(item);
        }
        return list;
    }

    /**
     * 下载控制台日志或备份
     *
     * @param project 项目
     * @param name    console 表示当前日志，其他为备份文件名
     * @param output  输出流
     * @throws Exception 失败
     */
    public void download(SshProjectModel project, String name, OutputStream output) throws Exception {
        String fileName = StrUtil.equals(name, "console")
            ? project.getId() + ".log"
            : name;
        Assert.state(StrUtil.startWith(fileName, project.getId() + ".log"), I18nMessageUtil.get("i18n.file_not_found.d952"));
        Assert.state(!StrUtil.containsAny(fileName, "/", "\\", ".."), I18nMessageUtil.get("i18n.file_not_found.d952"));
        sshProjectExecutor.downloadLog(project, fileName, output);
    }
}
