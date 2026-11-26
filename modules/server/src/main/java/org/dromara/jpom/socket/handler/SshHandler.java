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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.NioUtil;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.i18n.I18nThreadUtil;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.dblog.SshTerminalExecuteLogService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.socket.handler.record.AsciicastTerminalRecorder;
import org.dromara.jpom.socket.handler.record.OscMarkerScanner;
import org.dromara.jpom.socket.handler.record.TerminalRecorder;
import org.dromara.jpom.socket.handler.record.TerminalRecorderFactory;
import org.dromara.jpom.service.user.UserBindWorkspaceService;
import org.dromara.jpom.util.SocketSessionUtil;
import org.dromara.jpom.util.StringUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ssh 处理2
 *
 * @author bwcx_jzy
 * @since 2019/8/9
 */
@Feature(cls = ClassFeature.SSH_TERMINAL, method = MethodFeature.EXECUTE)
@Slf4j
public class SshHandler extends BaseTerminalHandler {

    private static final ConcurrentHashMap<String, HandlerItem> HANDLER_ITEM_CONCURRENT_HASH_MAP = new SafeConcurrentHashMap<>();
    private static SshTerminalExecuteLogService sshTerminalExecuteLogService;
    private static UserBindWorkspaceService userBindWorkspaceService;
    private static SshService sshService;
    private static TerminalRecorderFactory terminalRecorderFactory;

    /**
     * shell integration 注入脚本（OSC 133/633）：
     * 仅用于产出「命令原文 + 退出码」标记，不改变命令执行；非 bash/zsh 环境下 if 条件不成立，不产生副作用。
     */
    private static final String SHELL_INTEGRATION = "if [ -n \"${BASH_VERSION:-}\" ]; then __jpom_h=${HISTCMD:-0}; __jpom_mark(){ local e=$?; local h=${HISTCMD:-0}; if [ \"$h\" -le \"$__jpom_h\" ]; then return; fi; __jpom_h=$h; printf '\\033]633;E;%s\\007\\033]133;D;%s\\007\\033]133;A\\007' \"$(fc -ln -1 2>/dev/null)\" \"$e\"; }; PROMPT_COMMAND=\"__jpom_mark${PROMPT_COMMAND:+;$PROMPT_COMMAND}\"; __jpom_h=$((HISTCMD+1)); elif [ -n \"${ZSH_VERSION:-}\" ]; then __jpom_mark(){ local e=$?; printf '\\033]633;E;%s\\007\\033]133;D;%s\\007\\033]133;A\\007' \"$(fc -ln -1 2>/dev/null)\" \"$e\"; }; precmd_functions+=(__jpom_mark); fi";

    private static void init() {
        if (sshTerminalExecuteLogService == null) {
            sshTerminalExecuteLogService = SpringUtil.getBean(SshTerminalExecuteLogService.class);
        }
        if (userBindWorkspaceService == null) {
            userBindWorkspaceService = SpringUtil.getBean(UserBindWorkspaceService.class);
        }
        if (sshService == null) {
            sshService = SpringUtil.getBean(SshService.class);
        }
        if (terminalRecorderFactory == null) {
            terminalRecorderFactory = SpringUtil.getBean(TerminalRecorderFactory.class);
        }
    }

    @Override
    public void afterConnectionEstablishedImpl(WebSocketSession session) throws Exception {
        super.afterConnectionEstablishedImpl(session);
        init();
        Map<String, Object> attributes = session.getAttributes();
        MachineSshModel machineSshModel = (MachineSshModel) attributes.get("machineSsh");
        SshModel sshModel = (SshModel) attributes.get("dataItem");
        //
        UserModel userInfo = (UserModel) attributes.get("userInfo");
        if (sshModel != null) {
            // 判断是没有任何限制
            String workspaceId = sshModel.getWorkspaceId();
            boolean sshCommandNotLimited = userBindWorkspaceService.exists(userInfo, workspaceId + UserBindWorkspaceService.SSH_COMMAND_NOT_LIMITED);
            attributes.put("sshCommandNotLimited", sshCommandNotLimited);
        } else {
            // 通过资产管理方式进入
            attributes.put("sshCommandNotLimited", true);
        }
        //
        HandlerItem handlerItem;
        try {
            //
            handlerItem = new HandlerItem(session, machineSshModel, sshModel);
            handlerItem.startRead();
        } catch (Exception e) {
            // 输出超时日志 @author jzy
            log.error(I18nMessageUtil.get("i18n.ssh_console_connection_timeout.8eb3"), e);
            // 原来无论什么原因都只回「SSH console connection timed out」，用户看不出是资产没分配、
            // 认证失败还是网络不通（BUGS B-031）。这里把真实异常信息一并回显。
            String reason = StrUtil.emptyToDefault(e.getMessage(), e.getClass().getSimpleName());
            sendBinary(session, I18nMessageUtil.get("i18n.ssh_console_connection_timeout.8eb3")
                + I18nMessageUtil.get("i18n.ssh_console_connection_reason.6f2a") + reason);
            this.destroy(session);
            return;
        }
        HANDLER_ITEM_CONCURRENT_HASH_MAP.put(session.getId(), handlerItem);
        //
        Thread.sleep(1000);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            setLanguage(session);
            HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
            if (handlerItem == null) {
                sendBinary(session, I18nMessageUtil.get("i18n.already_offline.d3b5"));
                IoUtil.close(session);
                return;
            }
            String payload = message.getPayload();

            JSONValidator.Type type = StringUtil.validatorJson(payload);
            if (type == JSONValidator.Type.Object) {
                JSONObject jsonObject = JSONObject.parseObject(payload);
                String data = jsonObject.getString("data");
                if (StrUtil.equals(data, "jpom-heart")) {
                    // 心跳消息不转发
                    return;
                }
                if (StrUtil.equals(data, "resize")) {
                    // 缓存区大小
                    handlerItem.resize(jsonObject);
                    return;
                }
            }
            //
            Map<String, Object> attributes = session.getAttributes();
            UserModel userInfo = (UserModel) attributes.get("userInfo");
            boolean sshCommandNotLimited = (boolean) attributes.get("sshCommandNotLimited");
            try {
                this.sendCommand(handlerItem, payload, userInfo, sshCommandNotLimited);
            } catch (Exception e) {
                sendBinary(session, "Failure:" + e.getMessage());
                log.error(I18nMessageUtil.get("i18n.command_execution_exception.4ccd"), e);
            }
        } finally {
            clearLanguage();
        }
    }

    private void sendCommand(HandlerItem handlerItem, String data, UserModel userInfo, boolean sshCommandNotLimited) throws Exception {
        // 录制用户输入（关闭录制时是空实现）
        handlerItem.recordInput(data.getBytes());
        if (handlerItem.checkInput(data, userInfo, sshCommandNotLimited)) {
            handlerItem.outputStream.write(data.getBytes());
        } else {
            handlerItem.outputStream.write(I18nMessageUtil.get("i18n.no_permission_to_execute_command.04d4").getBytes());
            handlerItem.outputStream.flush();
            handlerItem.outputStream.write(new byte[]{3});
        }
        handlerItem.outputStream.flush();
    }

    /**
     * 记录终端执行记录
     *
     * @param session 回话
     * @param command 命令行
     * @param refuse  是否拒绝
     */
    private void logCommands(WebSocketSession session, String command, boolean refuse) {
        List<String> split = StrUtil.splitTrim(command, StrUtil.CR);
        // 最后一个是否为回车, 最后一个不是回车表示还未提交，还在缓存去待确认
        boolean all = StrUtil.endWith(command, StrUtil.CR);
        int size = split.size();
        split = CollUtil.sub(split, 0, all ? size : size - 1);
        if (CollUtil.isEmpty(split)) {
            return;
        }
        // 获取基础信息
        Map<String, Object> attributes = session.getAttributes();
        UserModel userInfo = (UserModel) attributes.get("userInfo");
        String ip = (String) attributes.get("ip");
        String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
        MachineSshModel machineSshModel = (MachineSshModel) attributes.get("machineSsh");
        SshModel sshItem = (SshModel) attributes.get("dataItem");
        //
        sshTerminalExecuteLogService.batch(userInfo, machineSshModel, sshItem, ip, userAgent, refuse, split);
    }

    private class HandlerItem implements Runnable, AutoCloseable {
        private final WebSocketSession session;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final Session openSession;
        private final ChannelShell channel;
        private final SshModel sshItem;
        private final MachineSshModel machineSshModel;
        private final StringBuilder nowLineInput = new StringBuilder();
        private final KeyEventCycle keyEventCycle = new KeyEventCycle();
        /**
         * 终端会话录制器（关闭录制时为空实现）
         */
        private TerminalRecorder recorder = TerminalRecorder.NOOP;
        /**
         * 录制器是否已尝试创建（首次拿到真实终端尺寸时创建）
         */
        private boolean recorderCreated;
        /**
         * OSC 133/633 标记扫描器
         */
        private final OscMarkerScanner oscScanner = new OscMarkerScanner();
        /**
         * 是否已拿到 shell integration 上报的命令（拿到后不再用按键推断做命令索引）
         */
        private volatile boolean oscSeen;
        private int markerSeq;
        /**
         * WebSocket 关闭码（用于区分「用户正常结束」与「链路异常中断」）
         */
        private volatile CloseStatus closeStatus;

        void setCloseStatus(CloseStatus closeStatus) {
            this.closeStatus = closeStatus;
        }

        HandlerItem(WebSocketSession session, MachineSshModel machineSshModel, SshModel sshModel) throws IOException {
            this.session = session;
            this.sshItem = sshModel;
            this.machineSshModel = machineSshModel;
            this.openSession = sshService.getSessionByModel(machineSshModel);
            this.channel = (ChannelShell) JschUtil.createChannel(openSession, ChannelType.SHELL);
            this.inputStream = channel.getInputStream();
            this.outputStream = channel.getOutputStream();
            keyEventCycle.setCharset(machineSshModel.charset());
        }

        void startRead() throws JSchException {
            this.channel.connect(machineSshModel.timeout());
            I18nThreadUtil.execute(this);
        }

        /**
         * 调整 缓存区大小
         *
         * @param jsonObject 参数
         */
        private void resize(JSONObject jsonObject) {
            Integer rows = Convert.toInt(jsonObject.getString("rows"), 10);
            Integer cols = Convert.toInt(jsonObject.getString("cols"), 10);
            Integer wp = Convert.toInt(jsonObject.getString("wp"), 10);
            Integer hp = Convert.toInt(jsonObject.getString("hp"), 10);
            this.channel.setPtySize(cols, rows, wp, hp);
            // 首次拿到真实终端尺寸时创建录制器（保证录制头部的宽高与用户终端一致）
            this.ensureRecorder(cols, rows);
            this.safeRecord(recorder -> recorder.resize(cols, rows));
        }

        /**
         * 创建录制器（只创建一次）
         *
         * @param cols 列
         * @param rows 行
         */
        private void ensureRecorder(int cols, int rows) {
            if (this.recorderCreated) {
                return;
            }
            this.recorderCreated = true;
            try {
                Map<String, Object> attributes = session.getAttributes();
                MachineSshModel machineSshModel = (MachineSshModel) attributes.get("machineSsh");
                SshModel sshItem = (SshModel) attributes.get("dataItem");
                UserModel userInfo = (UserModel) attributes.get("userInfo");
                String ip = (String) attributes.get("ip");
                String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
                Charset charset = machineSshModel == null ? Charset.forName("UTF-8") : machineSshModel.charset();
                this.recorder = terminalRecorderFactory.create(machineSshModel, sshItem, userInfo, ip, userAgent, cols, rows, charset);
                if (this.recorder != TerminalRecorder.NOOP) {
                    this.installShellIntegration();
                }
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.ssh_terminal_session_write_fail.9a5c"), e.getMessage());
            }
        }

        /**
         * 注入 shell integration（仅用于产出命令标记，失败不影响会话）
         */
        private void installShellIntegration() {
            try {
                // 必须以回车结束：否则 shell 会把后续用户输入拼到同一行，命令一直处于续行状态
                this.outputStream.write((SHELL_INTEGRATION + StrUtil.CR).getBytes());
                this.outputStream.flush();
            } catch (Exception e) {
                log.warn("install shell integration: {}", e.getMessage());
            }
        }

        /**
         * 录制用户输入
         *
         * @param data 输入字节
         */
        private void recordInput(byte[] data) {
            this.safeRecord(recorder -> recorder.input(data));
        }

        /**
         * 录制远端输出，并从输出里解析命令标记
         *
         * @param data 输出字节
         */
        private void recordOutput(byte[] data) {
            if (this.recorder == TerminalRecorder.NOOP) {
                return;
            }
            this.safeRecord(recorder -> recorder.output(data));
            try {
                String text = new String(data, machineSshModel.charset());
                this.oscScanner.scan(text, this::onOscCommand);
            } catch (Exception e) {
                log.warn("scan osc marker: {}", e.getMessage());
            }
        }

        /**
         * 命令标记回调（shell integration）
         *
         * @param command  命令原文
         * @param exitCode 退出码
         */
        private void onOscCommand(String command, Integer exitCode) {
            this.oscSeen = true;
            int seq = ++this.markerSeq;
            JSONObject marker = new JSONObject();
            marker.put("seq", seq);
            marker.put("exit", exitCode);
            marker.put("cmd", command);
            marker.put("source", "OSC133");
            this.safeRecord(recorder -> recorder.marker(marker.toJSONString()));
            if (StrUtil.isEmpty(command)) {
                return;
            }
            try {
                Map<String, Object> attributes = session.getAttributes();
                UserModel userInfo = (UserModel) attributes.get("userInfo");
                String ip = (String) attributes.get("ip");
                String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
                SshModel sshItem = (SshModel) attributes.get("dataItem");
                sshTerminalExecuteLogService.batch(userInfo, machineSshModel, sshItem, ip, userAgent, false,
                    Collections.singletonList(command), this.recorder.sessionId(), seq, "OSC133");
            } catch (Exception e) {
                log.warn("record osc command: {}", e.getMessage());
            }
        }

        /**
         * 录制调用兜底：录制异常绝不影响终端会话
         *
         * @param consumer 操作
         */
        private void safeRecord(Consumer<TerminalRecorder> consumer) {
            if (this.recorder == TerminalRecorder.NOOP) {
                return;
            }
            try {
                consumer.accept(this.recorder);
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.ssh_terminal_session_write_fail.9a5c"), e.getMessage());
            }
        }

        /**
         * 添加到命令队列
         *
         * @param msg 输入
         * @return 当前待确认待所有命令
         */
        private String append(String msg) {
            char[] x = msg.toCharArray();
            if (x.length == 1 && x[0] == 127) {
                // 退格键
                int length = nowLineInput.length();
                if (length > 0) {
                    nowLineInput.delete(length - 1, length);
                }
            } else {
                nowLineInput.append(msg);
            }
            return nowLineInput.toString();
        }

        /**
         * 检查输入是否包含禁止命令，记录执行记录
         *
         * @param msg                  输入
         * @param userInfo             用户
         * @param sshCommandNotLimited 是否解除限制
         * @return true 没有任何限制
         */
        public boolean checkInput(String msg, UserModel userInfo, boolean sshCommandNotLimited) {
            String allCommand = this.append(msg);
            boolean refuse;
            // 超级管理员不限制,有权限都不限制
            boolean systemUser = userInfo.isSuperSystemUser() || sshCommandNotLimited;
            if (StrUtil.equalsAny(msg, StrUtil.CR, StrUtil.TAB)) {
                String join = nowLineInput.toString();
                if (StrUtil.equals(msg, StrUtil.CR)) {
                    nowLineInput.setLength(0);
                }
                // sshItem 可能为空
                refuse = sshItem == null || SshModel.checkInputItem(sshItem, join);
            } else {
                // 复制输出
                refuse = sshItem == null || SshModel.checkInputItem(sshItem, msg);
            }
            // 执行命令行记录
            keyEventCycle.read(text -> {
                if (this.oscSeen) {
                    // shell integration 已提供可信命令与退出码，不再重复记录按键推断结果
                    return;
                }
                // 获取基础信息
                Map<String, Object> attributes = session.getAttributes();
                String ip = (String) attributes.get("ip");
                String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
                MachineSshModel machineSshModel = (MachineSshModel) attributes.get("machineSsh");
                SshModel sshItem = (SshModel) attributes.get("dataItem");
                sshTerminalExecuteLogService.batch(userInfo, machineSshModel, sshItem, ip, userAgent, refuse, Collections.singletonList(text),
                    this.recorder.sessionId(), null, "LEGACY");
            }, msg.getBytes(Charset.forName(machineSshModel.getCharset())));
            // 执行命令行记录
            // logCommands(session, allCommand, refuse);
            return systemUser || refuse;
        }


        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1024];
                int i;
                //如果没有数据来，线程会一直阻塞在这个地方等待数据。
                while ((i = inputStream.read(buffer)) != NioUtil.EOF) {
                    byte[] tempBytes = Arrays.copyOfRange(buffer, 0, i);
                    try {
                        keyEventCycle.receive(tempBytes);
                    } catch (Exception e) {
                        log.warn("keyEventCycle.receive", e);
                    }
                    // 录制远端输出（含 OSC 命令标记解析），关闭录制时为空操作
                    this.recordOutput(tempBytes);
                    sendBinary(session, new String(tempBytes, machineSshModel.charset()));
                }
            } catch (Exception e) {
                if (this.recorder instanceof AsciicastTerminalRecorder) {
                    ((AsciicastTerminalRecorder) this.recorder).markAbnormal();
                }
                if (!this.openSession.isConnected()) {
                    log.error(I18nMessageUtil.get("i18n.ssh_error_string.6bdb"), e.getMessage());
                    return;
                }
                log.error(I18nMessageUtil.get("i18n.read_error.7fa5"), e);
                if (this.recorder instanceof AsciicastTerminalRecorder) {
                    ((AsciicastTerminalRecorder) this.recorder).markAbnormal();
                }
                SshHandler.this.destroy(this.session);
            }
        }

        @Override
        public void close() throws Exception {
            // 先落录制文件与会话行，再关通道
            // 只有「浏览器正常关闭终端（1000/1001）且 SSH 会话仍然连通」才算正常结束，其余一律标记异常中断
            boolean cleanClose = this.closeStatus != null
                && (this.closeStatus.getCode() == 1000 || this.closeStatus.getCode() == 1001);
            boolean sessionBroken = this.openSession == null || !this.openSession.isConnected();
            if (!cleanClose || sessionBroken) {
                if (this.recorder instanceof AsciicastTerminalRecorder) {
                    ((AsciicastTerminalRecorder) this.recorder).markAbnormal();
                }
            }
            IoUtil.close(this.recorder);
            IoUtil.close(this.inputStream);
            IoUtil.close(this.outputStream);
            JschUtil.close(this.channel);
            JschUtil.close(this.openSession);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
        if (handlerItem != null) {
            handlerItem.setCloseStatus(status);
        }
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void destroy(WebSocketSession session) {
        HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
        IoUtil.close(handlerItem);
        IoUtil.close(session);
        HANDLER_ITEM_CONCURRENT_HASH_MAP.remove(session.getId());
        SocketSessionUtil.close(session);
    }
}
