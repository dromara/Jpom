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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.Session;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.model.ScriptLibraryModel;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.func.assets.server.ScriptLibraryServer;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.Charset;

/**
 * 解析流程脚本来源
 * <ul>
 *     <li>内联 {@code script}：直接使用</li>
 *     <li>{@code G@<id>}：服务端脚本库（可见即可引用）</li>
 *     <li>其他：项目目录内的脚本文件（SFTP 读取）</li>
 * </ul>
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Component
public class SshProjectScriptResolver {

    private final SshProjectMachineResolver machineResolver;
    private final MachineSshServer machineSshServer;
    private final ScriptLibraryServer scriptLibraryServer;

    public SshProjectScriptResolver(SshProjectMachineResolver machineResolver,
                                    MachineSshServer machineSshServer,
                                    ScriptLibraryServer scriptLibraryServer) {
        this.machineResolver = machineResolver;
        this.machineSshServer = machineSshServer;
        this.scriptLibraryServer = scriptLibraryServer;
    }

    /**
     * 解析脚本文本（结果放在 {@link SshProjectExecuteResult#getOutput()}）
     *
     * @param project 项目
     * @param process 流程
     * @param op      操作
     * @return 结果
     */
    public SshProjectExecuteResult resolve(SshProjectModel project, DslYmlDto.BaseProcess process, ConsoleCommandOp op) {
        if (StrUtil.isNotEmpty(process.getScript())) {
            return ok(process.getScript());
        }
        String scriptId = process.getScriptId();
        if (StrUtil.isEmpty(scriptId)) {
            return SshProjectExecuteResult.fail(I18nMessageUtil.get("i18n.ssh_project_script_missing.2c63") + op.name());
        }
        if (StrUtil.startWithIgnoreCase(scriptId, "G@")) {
            ScriptLibraryModel libraryModel = scriptLibraryServer.getByKey(scriptId.substring(2));
            if (libraryModel == null) {
                return SshProjectExecuteResult.fail(I18nMessageUtil.get("i18n.ssh_project_script_library_missing.5d74") + scriptId);
            }
            return ok(libraryModel.getScript());
        }
        String path = StrUtil.format("{}/{}", StrUtil.removeSuffix(StrUtil.nullToEmpty(project.getPath()), "/"), scriptId);
        Session session = null;
        try {
            MachineSshModel machine = machineResolver.resolve(project);
            session = machineSshServer.getSessionByModel(machine);
            return ok(this.readFile(session, machine.charset(), path));
        } catch (Exception e) {
            return SshProjectExecuteResult.fail(I18nMessageUtil.get("i18n.ssh_project_script_read_fail.7e17") + e.getMessage());
        } finally {
            cn.hutool.extra.ssh.JschUtil.close(session);
        }
    }

    private String readFile(Session session, Charset charset, String path) throws Exception {
        File temp = FileUtil.createTempFile("jpom-ssh-project-script", ".sh", true);
        try (Sftp sftp = new Sftp(session, charset, 5000)) {
            sftp.download(path, temp);
            return FileUtil.readString(temp, charset);
        } finally {
            FileUtil.del(temp);
        }
    }

    private SshProjectExecuteResult ok(String script) {
        SshProjectExecuteResult result = new SshProjectExecuteResult();
        result.setExitCode(0);
        result.setSuccess(true);
        result.setOutput(script);
        return result;
    }
}
