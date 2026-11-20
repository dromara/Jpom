/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket.handler;

import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.dromara.jpom.model.sshproject.SshProjectStatus;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.sshproject.SshProjectExecutor;
import org.dromara.jpom.service.sshproject.SshProjectLogTailer;
import org.dromara.jpom.service.sshproject.SshProjectMachineResolver;
import org.dromara.jpom.service.sshproject.SshProjectService;
import org.dromara.jpom.service.sshproject.SshProjectShell;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.socket.BaseHandler;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH 项目控制台
 * <p>
 * 协议与现有项目控制台保持一致：客户端发 {@code {op, projectId, logKey}}，
 * 服务端每条消息带 {@code JPOM_MSG} 标记与相同 op；流式输出用 {@code msg} 字段。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Feature(cls = ClassFeature.SSH_PROJECT_CONSOLE, method = MethodFeature.EXECUTE)
@Slf4j
public class SshProjectConsoleHandler extends BaseHandler {

    private static final Map<String, SshProjectLogTailer> TAILERS = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        JSONObject json;
        try {
            json = JSONObject.parseObject(message.getPayload());
        } catch (Exception e) {
            this.sendMsg(session, I18nMessageUtil.get("i18n.ssh_project_console_bad_message.7a51"));
            return;
        }
        if (json == null) {
            return;
        }
        ConsoleCommandOp op = EnumUtil.fromStringQuietly(ConsoleCommandOp.class, json.getString("op"));
        if (op == null) {
            return;
        }
        SshProjectModel project = (SshProjectModel) session.getAttributes().get("dataItem");
        if (project == null) {
            this.reply(session, json, op, false, I18nMessageUtil.get("i18n.ssh_project_not_exist.5d18"));
            return;
        }
        try {
            switch (op) {
                case heart:
                    this.reply(session, json, op, true, "pong");
                    break;
                case status: {
                    SshProjectExecutor executor = SpringUtil.getBean(SshProjectExecutor.class);
                    SshProjectStatus status = executor.status(project);
                    JSONObject data = new JSONObject();
                    data.put("status", status.isRunning() ? "running" : "stopped");
                    data.put("pid", status.getPid());
                    data.put("port", status.getPorts());
                    data.put("statusMsg", status.getMessage());
                    this.reply(session, json, op, status.isRunning(), null, data);
                    break;
                }
                case showlog:
                    this.showLog(session, json, project);
                    break;
                case start:
                case stop:
                case restart:
                case reload: {
                    super.logOpt(this.getClass(), session.getAttributes(), json);
                    SshProjectService service = SpringUtil.getBean(SshProjectService.class);
                    SshProjectExecuteResult result = service.operate(project.getId(), op.name(), line -> {
                        JSONObject lineJson = new JSONObject();
                        lineJson.put("op", op.name());
                        lineJson.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
                        lineJson.put("msg", line);
                        this.sendMsg(session, lineJson.toJSONString());
                    }, null);
                    SshProjectModel updated = service.getByKey(project.getId());
                    JSONObject data = new JSONObject();
                    data.put("status", updated == null ? null : updated.getLastStatus());
                    data.put("pid", updated == null ? null : updated.getLastPid());
                    data.put("port", updated == null ? null : updated.getLastPort());
                    this.reply(session, json, op, result.isSuccess(), result.getMessage(), data);
                    break;
                }
                default:
                    this.reply(session, json, op, false, I18nMessageUtil.get("i18n.ssh_project_operate_unsupported.3d18") + op);
                    break;
            }
        } catch (Exception e) {
            log.warn("ssh project console error: {}", op, e);
            this.reply(session, json, op, false, e.getMessage());
        }
    }

    private void showLog(WebSocketSession session, JSONObject json, SshProjectModel project) {
        this.closeTailer(session);
        String logKey = json.getString("logKey");
        SshProjectMachineResolver resolver = SpringUtil.getBean(SshProjectMachineResolver.class);
        SshProjectExecutor executor = SpringUtil.getBean(SshProjectExecutor.class);
        MachineSshModel machine = resolver.resolve(project);
        String path = this.resolveLogPath(project, logKey, machine, executor);
        if (path == null) {
            this.sendMsg(session, I18nMessageUtil.get("i18n.file_not_found.d952"));
            return;
        }
        // 文件还不存在 / 为空时先给一条提示：否则控制台只剩欢迎语，用户会以为日志坏了
        this.notifyLogState(session, project, path, executor);
        try {
            int initLines = SpringUtil.getBean(ServerConfig.class).getInitReadLine();
            SshProjectLogTailer tailer = SshProjectLogTailer.start(machine, path, initLines, line -> {
                JSONObject lineJson = new JSONObject();
                lineJson.put("op", ConsoleCommandOp.showlog.name());
                lineJson.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
                lineJson.put("msg", line);
                this.sendMsg(session, lineJson.toJSONString());
            });
            TAILERS.put(session.getId(), tailer);
        } catch (Exception e) {
            log.warn("start ssh project log tail fail", e);
            this.sendMsg(session, e.getMessage());
        }
    }

    /**
     * 探测日志文件状态，为空/不存在时在控制台给出一条可读提示
     */
    private void notifyLogState(WebSocketSession session, SshProjectModel project, String path, SshProjectExecutor executor) {
        String quoted = SshProjectShell.quote(path);
        String script = StrUtil.format("if [ -s {} ]; then echo exists; elif [ -f {} ]; then echo empty; else echo missing; fi", quoted, quoted);
        SshProjectExecuteResult result = executor.runScript(project, script, 15);
        String state = StrUtil.trimToEmpty(result.getOutput());
        if (StrUtil.equals(state, "missing")) {
            this.sendMsg(session, I18nMessageUtil.get("i18n.ssh_project_log_missing.4c7d"));
        } else if (StrUtil.equals(state, "empty")) {
            this.sendMsg(session, I18nMessageUtil.get("i18n.ssh_project_log_empty.9a52"));
        }
    }

    /**
     * 解析要跟随的日志文件：只能来自控制台日志或项目 dsl 声明的日志
     * <p>
     * 返回的必须是远端绝对路径：{@code tail} 里用单引号引用文件路径，{@code $HOME} 不会展开。
     */
    private String resolveLogPath(SshProjectModel project, String logKey, MachineSshModel machine, SshProjectExecutor executor) {
        if (StrUtil.isEmpty(logKey) || "console".equals(logKey)) {
            String home = executor.homeDir(machine);
            if (StrUtil.isEmpty(home)) {
                return null;
            }
            return StrUtil.removeSuffix(home, "/") + "/.jpom/logs/" + project.getId() + ".log";
        }
        DslYmlDto dsl = DslYmlDto.build(project.getDslContent());
        if (dsl.getConfig() == null || dsl.getConfig().getLog() == null) {
            return null;
        }
        return dsl.getConfig().getLog().stream()
            .filter(logFile -> StrUtil.equals(logFile.getName(), logKey))
            .findFirst()
            .map(logFile -> {
                String path = StrUtil.nullToEmpty(logFile.getPath());
                if (StrUtil.isEmpty(path) || StrUtil.contains(path, "..")) {
                    return null;
                }
                if (StrUtil.startWith(path, "/")) {
                    return path;
                }
                return StrUtil.format("{}/{}", StrUtil.removeSuffix(StrUtil.nullToEmpty(project.getPath()), "/"), path);
            })
            .orElse(null);
    }

    private void reply(WebSocketSession session, JSONObject request, ConsoleCommandOp op, boolean success, String message) {
        this.reply(session, request, op, success, message, null);
    }

    private void reply(WebSocketSession session, JSONObject request, ConsoleCommandOp op, boolean success, String message, JSONObject data) {
        JSONObject json = new JSONObject();
        json.put("op", op.name());
        json.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
        json.put("success", success);
        if (StrUtil.isNotEmpty(message)) {
            json.put("msg", message);
        }
        if (data != null) {
            json.putAll(data);
        }
        json.put("projectId", request.getString("projectId"));
        this.sendMsg(session, json.toJSONString());
    }

    private void closeTailer(WebSocketSession session) {
        SshProjectLogTailer tailer = TAILERS.remove(session.getId());
        if (tailer != null) {
            tailer.close();
        }
    }

    @Override
    public void afterConnectionEstablishedImpl(WebSocketSession session) {
        // 建立即回一次心跳，前端可据此确认通道可用
        JSONObject json = new JSONObject();
        json.put("op", "heart");
        json.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
        json.put("msg", "connected");
        this.sendMsg(session, json.toJSONString());
    }

    @Override
    public void destroy(WebSocketSession session) {
        this.closeTailer(session);
    }
}
