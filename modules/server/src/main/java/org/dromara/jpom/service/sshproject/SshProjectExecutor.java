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
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.dromara.jpom.model.sshproject.SshProjectStatus;
import org.dromara.jpom.plugins.JschUtils;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SSH 项目执行器：把生命周期脚本送到目标机器执行，并回读状态。
 * <p>
 * 约定：
 * <ul>
 *     <li>脚本经 SFTP 上传到 `~/.jpom/scripts/{uuid}.sh`，执行后删除；</li>
 *     <li>执行统一套 `timeout -k 5 N`（退出码 124 表示超时），避免 SSH 通道永久阻塞；</li>
 *     <li>状态协议：status 脚本最后一行 `running:<pid>[:ports]` / `stopped`；</li>
 *     <li>脚本来源解析见 {@link SshProjectScriptResolver}，机器解析见 {@link SshProjectMachineResolver}。</li>
 * </ul>
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Slf4j
@Service
public class SshProjectExecutor {

    private static final int DEFAULT_TIMEOUT = 60;
    /**
     * 输出回传的最大字符数
     */
    private static final int MAX_OUTPUT = 8000;

    private final SshProjectMachineResolver machineResolver;
    private final SshProjectScriptResolver scriptResolver;
    private final MachineSshServer machineSshServer;

    public SshProjectExecutor(SshProjectMachineResolver machineResolver,
                              SshProjectScriptResolver scriptResolver,
                              MachineSshServer machineSshServer) {
        this.machineResolver = machineResolver;
        this.scriptResolver = scriptResolver;
        this.machineSshServer = machineSshServer;
    }

    /**
     * 查询项目状态
     *
     * @param project 项目
     * @return 状态
     */
    public SshProjectStatus status(SshProjectModel project) {
        DslYmlDto dsl = DslYmlDto.build(project.getDslContent());
        DslYmlDto.BaseProcess process = dsl.tryDslProcess(ConsoleCommandOp.status.name());
        String script;
        if (process != null) {
            SshProjectExecuteResult resolve = scriptResolver.resolve(project, process, ConsoleCommandOp.status);
            if (!resolve.isSuccess()) {
                return SshProjectStatus.stopped(resolve.getMessage());
            }
            script = resolve.getOutput();
        } else {
            script = this.pidFileStatusScript();
        }
        SshProjectExecuteResult result = this.run(project, dsl, ConsoleCommandOp.status, script, StrUtil.EMPTY);
        if (result.getExitCode() == -100) {
            return SshProjectStatus.stopped(result.getMessage());
        }
        SshProjectStatus status = SshProjectStatus.parse(result.getOutput());
        if (StrUtil.isEmpty(status.getPorts()) && status.getPid() != null && status.getPid() > 0) {
            status.setPorts(this.port(project, status.getPid()));
        }
        return status;
    }

    /**
     * 执行生命周期操作
     *
     * @param project 项目
     * @param op      操作
     * @param handler 输出回调（可为空）
     * @return 执行结果
     */
    public SshProjectExecuteResult operate(SshProjectModel project, ConsoleCommandOp op, LineHandler handler) {
        DslYmlDto dsl = DslYmlDto.build(project.getDslContent());
        if (op == ConsoleCommandOp.restart && dsl.tryDslProcess(ConsoleCommandOp.restart.name()) == null) {
            SshProjectExecuteResult stop = this.operate(project, ConsoleCommandOp.stop, handler);
            if (stop.getExitCode() != 0) {
                return stop;
            }
            return this.operate(project, ConsoleCommandOp.start, handler);
        }
        DslYmlDto.BaseProcess process = dsl.tryDslProcess(op.name());
        Assert.notNull(process, I18nMessageUtil.get("i18n.ssh_project_process_not_configured.8f21") + op.name());
        SshProjectExecuteResult resolve = scriptResolver.resolve(project, process, op);
        if (!resolve.isSuccess()) {
            return resolve;
        }
        SshProjectExecuteResult result = this.run(project, dsl, op, resolve.getOutput(), StrUtil.nullToEmpty(process.getScriptArgs()));
        if (handler != null && StrUtil.isNotEmpty(result.getOutput())) {
            for (String line : StrUtil.split(result.getOutput(), '\n', false, false)) {
                handler.handle(line);
            }
        }
        return result;
    }

    /**
     * 查询端口
     *
     * @param project 项目
     * @param pid     进程
     * @return 端口，多个用逗号分隔
     */
    public String port(SshProjectModel project, Integer pid) {
        if (pid == null || pid <= 0) {
            return StrUtil.DASHED;
        }
        String script = StrUtil.format("if command -v ss >/dev/null 2>&1; then ss -lntp 2>/dev/null | grep -F \"pid={},\" | awk '{{print $4}}' | awk -F: '{{print $NF}}' | sort -n | uniq; else netstat -lntp 2>/dev/null | grep -F \"{}/\" | awk '{{print $4}}' | awk -F: '{{print $NF}}' | sort -n | uniq; fi", pid, pid);
        SshProjectExecuteResult result = this.run(project, DslYmlDto.build(project.getDslContent()), ConsoleCommandOp.status, script, StrUtil.EMPTY);
        String output = StrUtil.trimToEmpty(result.getOutput());
        if (StrUtil.isEmpty(output)) {
            return StrUtil.DASHED;
        }
        return StrUtil.join(StrUtil.COMMA, StrUtil.splitTrim(output, '\n'));
    }

    /**
     * 执行一段自定义脚本（日志备份/清空等运维动作复用同一套超时与临时文件清理）
     *
     * @param project 项目
     * @param script  脚本
     * @param timeout 超时秒
     * @return 结果
     */
    public SshProjectExecuteResult runScript(SshProjectModel project, String script, int timeout) {
        DslYmlDto dsl = DslYmlDto.build(project.getDslContent());
        SshProjectExecuteResult result = new SshProjectExecuteResult();
        long start = System.currentTimeMillis();
        Session session = null;
        try {
            MachineSshModel machine = machineResolver.resolve(project);
            session = machineSshServer.getSessionByModel(machine);
            String content = this.buildScript(project, dsl, script);
            ExecOutput exec = this.uploadAndExec(session, machine, machine.charset(), content, StrUtil.EMPTY, Math.max(timeout, 1));
            result.setExitCode(exec.exitCode);
            result.setSuccess(exec.exitCode == 0);
            result.setOutput(StrUtil.maxLength(exec.output, MAX_OUTPUT));
            if (!result.isSuccess()) {
                result.setMessage(I18nMessageUtil.get("i18n.ssh_project_exec_fail.4b82") + exec.exitCode);
            }
        } catch (Exception e) {
            log.warn("ssh project runScript error: {}", project.getId(), e);
            result.setExitCode(-100);
            result.setMessage(I18nMessageUtil.get("i18n.ssh_project_exec_fail.4b82") + e.getMessage());
        } finally {
            JschUtil.close(session);
            result.setCost(System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * 远端用户 HOME 目录（结尾不带斜杠，取不到返回 null）
     * <p>
     * 控制台日志内部路径是 {@code $HOME/.jpom/logs/{id}.log}，但 {@code tail} 等命令里的
     * {@code $HOME} 必须由远端 shell 展开；统一在这里展开成绝对路径，调用方再做单引号引用即可。
     *
     * @param machine 资产
     * @return HOME 目录
     */
    public String homeDir(MachineSshModel machine) {
        Session session = null;
        try {
            session = machineSshServer.getSessionByModel(machine);
            try (Sftp sftp = new Sftp(session, machine.charset(), Math.max(machine.timeout(), 5000))) {
                return StrUtil.removeSuffix(sftp.home(), StrUtil.SLASH);
            }
        } catch (Exception e) {
            log.warn("ssh project resolve home dir fail: {}", machine.getId(), e);
            return null;
        } finally {
            JschUtil.close(session);
        }
    }

    /**
     * 下载项目控制台日志/备份到输出流（文件名已由调用方校验，只允许 {id}.log*）
     *
     * @param project      项目
     * @param fileName     文件名
     * @param outputStream 输出
     * @throws Exception 失败
     */
    public void downloadLog(SshProjectModel project, String fileName, java.io.OutputStream outputStream) throws Exception {
        MachineSshModel machine = machineResolver.resolve(project);
        Session session = null;
        Sftp sftp = null;
        try {
            session = machineSshServer.getSessionByModel(machine);
            sftp = new Sftp(session, machine.charset(), Math.max(machine.timeout(), 5000));
            String remote = sftp.home() + "/.jpom/logs/" + fileName;
            sftp.download(remote, outputStream);
        } finally {
            cn.hutool.core.io.IoUtil.close(sftp);
            JschUtil.close(session);
        }
    }

    private SshProjectExecuteResult run(SshProjectModel project, DslYmlDto dsl, ConsoleCommandOp op, String script, String args) {
        long start = System.currentTimeMillis();
        Session session = null;
        SshProjectExecuteResult result = new SshProjectExecuteResult();
        try {
            MachineSshModel machine = machineResolver.resolve(project);
            session = machineSshServer.getSessionByModel(machine);
            int timeout = this.resolveTimeout(dsl, op);
            String content = this.buildScript(project, dsl, script);
            Charset charset = machine.charset();
            ExecOutput exec = this.uploadAndExec(session, machine, charset, content, args, timeout);
            result.setOutput(StrUtil.maxLength(exec.output, MAX_OUTPUT));
            result.setExitCode(exec.exitCode);
            result.setSuccess(exec.exitCode == 0);
            if (exec.exitCode == 124) {
                result.setMessage(I18nMessageUtil.get("i18n.ssh_project_exec_timeout.9a35") + timeout);
            } else if (!result.isSuccess()) {
                result.setMessage(I18nMessageUtil.get("i18n.ssh_project_exec_fail.4b82") + exec.exitCode);
            }
        } catch (Exception e) {
            log.warn("ssh project exec error: {}", project.getId(), e);
            result.setExitCode(-100);
            result.setMessage(I18nMessageUtil.get("i18n.ssh_project_exec_fail.4b82") + e.getMessage());
        } finally {
            JschUtil.close(session);
            result.setCost(System.currentTimeMillis() - start);
        }
        return result;
    }

    private String buildScript(SshProjectModel project, DslYmlDto dsl, String script) {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("PROJECT_ID", project.getId());
        env.put("PROJECT_NAME", StrUtil.nullToEmpty(project.getName()));
        env.put("PROJECT_PATH", StrUtil.nullToEmpty(project.getPath()));
        env.put("JPOM_WORKSPACE_ID", StrUtil.nullToEmpty(project.getWorkspaceId()));
        env.put("JPOM_SSH_ID", StrUtil.nullToEmpty(project.getMachineSshId()));
        String execPath = this.execPath(project, dsl);
        env.put("JPOM_EXEC_PATH", execPath);
        StringBuilder builder = new StringBuilder();
        // 不注入 set -e：用户的 status/stop 脚本常用「测试 + 提前退出」写法，强制 -e 会在测试为假时中断
        builder.append("umask 022").append(StrUtil.LF);
        builder.append("mkdir -p \"$HOME/.jpom/logs\" \"$HOME/.jpom/run\" \"$HOME/.jpom/scripts\"").append(StrUtil.LF);
        builder.append("export JPOM_LOG_FILE=\"$HOME/.jpom/logs/").append(project.getId()).append(".log\"").append(StrUtil.LF);
        builder.append("export JPOM_PID_FILE=\"$HOME/.jpom/run/").append(project.getId()).append(".pid\"").append(StrUtil.LF);
        builder.append("touch \"$JPOM_LOG_FILE\"").append(StrUtil.LF);
        builder.append(SshProjectShell.envBlock(env));
        if (StrUtil.isNotEmpty(execPath)) {
            builder.append("cd ").append(SshProjectShell.quote(execPath)).append(StrUtil.LF);
        }
        builder.append(script).append(StrUtil.LF);
        return builder.toString();
    }

    private String execPath(SshProjectModel project, DslYmlDto dsl) {
        DslYmlDto.Run run = dsl.getRun();
        String execPath = run == null ? null : run.getExecPath();
        if (StrUtil.isEmpty(execPath)) {
            return StrUtil.nullToEmpty(project.getPath());
        }
        if (StrUtil.startWith(execPath, "/")) {
            return execPath;
        }
        return StrUtil.format("{}/{}", StrUtil.removeSuffix(StrUtil.nullToEmpty(project.getPath()), "/"), execPath);
    }

    private int resolveTimeout(DslYmlDto dsl, ConsoleCommandOp op) {
        DslYmlDto.BaseProcess process = dsl.tryDslProcess(op.name());
        Integer timeout = process == null ? null : process.getTimeout();
        if (timeout == null || timeout <= 0) {
            return DEFAULT_TIMEOUT;
        }
        return timeout;
    }

    private ExecOutput uploadAndExec(Session session, MachineSshModel machine, Charset charset, String content, String args, int timeout) throws Exception {
        String tempId = IdUtil.fastSimpleUUID();
        File local = FileUtil.file(FileUtil.getTmpDir(), "ssh_project", tempId + ".sh");
        FileUtil.writeString(content, local, CharsetUtil.CHARSET_UTF_8);
        try (Sftp sftp = new Sftp(session, charset, machine.timeout())) {
            String dir = sftp.home() + "/.jpom/scripts/";
            sftp.mkDirs(dir);
            String remote = dir + tempId + ".sh";
            sftp.upload(remote, local);
            try {
                return this.execRemote(session, charset, machine, SshProjectShell.execCommand(remote, args, timeout));
            } finally {
                try {
                    sftp.delFile(remote);
                } catch (Exception e) {
                    log.warn("delete remote ssh project script fail", e);
                }
            }
        } finally {
            FileUtil.del(local);
        }
    }

    private ExecOutput execRemote(Session session, Charset charset, MachineSshModel machine, String command) throws Exception {
        ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
        channel.setCommand(StrUtil.bytes(command, charset));
        channel.setInputStream(null);
        StringBuilder output = new StringBuilder();
        try (ByteArrayOutputStream errorStream = new ByteArrayOutputStream()) {
            channel.setErrStream(errorStream, true);
            try (InputStream in = channel.getInputStream()) {
                channel.connect(Math.max(machine.timeout(), 5000));
                IoUtil.readLines(in, charset, (LineHandler) line -> output.append(line).append(StrUtil.LF));
            }
            if (errorStream.size() > 0) {
                output.append(errorStream.toString(charset.name()));
            }
            // 等待通道关闭，否则可能拿到 -1：JSch 的退出码在收到 close 消息后才可用（收口在 JschUtils）
            JschUtils.waitClosed(channel, JschUtils.EXIT_STATUS_WAIT_MILLIS);
            return new ExecOutput(output.toString(), channel.getExitStatus());
        } finally {
            JschUtil.close(channel);
        }
    }

    private String pidFileStatusScript() {
        return String.join("\n",
            "if [ -f \"$JPOM_PID_FILE\" ]; then",
            "  PID=$(cat \"$JPOM_PID_FILE\" 2>/dev/null)",
            "  if [ -n \"$PID\" ] && kill -0 \"$PID\" 2>/dev/null; then echo \"running:$PID\"; exit 0; fi",
            "fi",
            "echo stopped");
    }

    /**
     * 远端执行结果
     */
    private static class ExecOutput {
        private final String output;
        private final int exitCode;

        private ExecOutput(String output, int exitCode) {
            this.output = output;
            this.exitCode = exitCode;
        }
    }
}
