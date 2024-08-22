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
import lombok.Setter;
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
import org.dromara.jpom.service.user.UserBindWorkspaceService;
import org.dromara.jpom.util.SocketSessionUtil;
import org.dromara.jpom.util.StringUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayOutputStream;
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
            sendBinary(session, I18nMessageUtil.get("i18n.ssh_console_connection_timeout.8eb3"));
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
                // 获取基础信息
                Map<String, Object> attributes = session.getAttributes();
                String ip = (String) attributes.get("ip");
                String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
                MachineSshModel machineSshModel = (MachineSshModel) attributes.get("machineSsh");
                SshModel sshItem = (SshModel) attributes.get("dataItem");
                sshTerminalExecuteLogService.batch(userInfo, machineSshModel, sshItem, ip, userAgent, refuse, Collections.singletonList(text));
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
                    keyEventCycle.receive(tempBytes);
                    sendBinary(session, new String(tempBytes, machineSshModel.charset()));
                }
            } catch (Exception e) {
                if (!this.openSession.isConnected()) {
                    log.error(I18nMessageUtil.get("i18n.ssh_error_string.6bdb"), e.getMessage());
                    return;
                }
                log.error(I18nMessageUtil.get("i18n.read_error.7fa5"), e);
                SshHandler.this.destroy(this.session);
            }
        }

        @Override
        public void close() throws Exception {
            IoUtil.close(this.inputStream);
            IoUtil.close(this.outputStream);
            JschUtil.close(this.channel);
            JschUtil.close(this.openSession);
        }
    }

    @Override
    public void destroy(WebSocketSession session) {
        HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
        IoUtil.close(handlerItem);
        IoUtil.close(session);
        HANDLER_ITEM_CONCURRENT_HASH_MAP.remove(session.getId());
        SocketSessionUtil.close(session);
    }

    /**
     * 控制台案件事件处理
     */
    public static class KeyEventCycle {

        // 输入缓存
        private StringBuffer buffer = new StringBuffer();
        // 输入后是否接收返回字符串
        private boolean inputReceive = false;
        // TAB 输入暂停（处理Y/N确认）
        private boolean tabInputPause = false;
        // 光标位置
        private int inputSelection = 0;
        @Setter
        private Charset charset;
        private KeyControl keyControl = KeyControl.KEY_END;

        /**
         * 从控制台读取输入按键进行处理
         *
         * @param consumer 完整命令后输入回调
         * @param bytes    输入按键
         */
        public void read(Consumer<String> consumer, byte... bytes) {
            String str = new String(bytes, charset);
            if (keyControl == KeyControl.KEY_TAB && tabInputPause) {
                if (str.equalsIgnoreCase("y") || str.equalsIgnoreCase("n")) {
                    tabInputPause = false;
                    return;
                }
            }
            keyControl = KeyControl.getKeyControl(bytes);
            if ((keyControl == KeyControl.KEY_INPUT || keyControl == KeyControl.KEY_FUNCTION) && !tabInputPause) {
                buffer.insert(inputSelection, str);
                inputSelection += str.length();
            } else if (keyControl == KeyControl.KEY_ENTER) {
                // 回车，结束当前输入周期
                if (buffer.length() > 0) {
                    consumer.accept(buffer.toString());
                }
                // 重置周期
                buffer = new StringBuffer();
                inputReceive = false;
                inputSelection = 0;
            } else if (keyControl == KeyControl.KEY_BACK) {
                buffer.delete(Math.max(inputSelection - 1, 0), inputSelection);
                inputSelection = Math.max(inputSelection - 1, 0);
            } else if (keyControl == KeyControl.KEY_DELETE) {
                buffer.delete(inputSelection, Math.min(inputSelection + 1, buffer.length()));
            } else if (keyControl == KeyControl.KEY_LEFT) {
                inputSelection = Math.max(inputSelection - 1, 0);
            } else if (keyControl == KeyControl.KEY_RIGHT) {
                inputSelection = Math.min(inputSelection + 1, buffer.length());
            } else if (keyControl == KeyControl.KEY_HOME) {
                inputSelection = 0;
            } else if (keyControl == KeyControl.KEY_END) {
                inputSelection = buffer.length();
            } else if (keyControl == KeyControl.KEY_TAB) {
                inputReceive = true;
            } else if (keyControl == KeyControl.KEY_UP || keyControl == KeyControl.KEY_DOWN) {
                // 清空命令缓冲
                inputSelection = 0;
                inputReceive = true;
            } else if (keyControl == KeyControl.KEY_ETX) {
                buffer = new StringBuffer();
                inputSelection = 0;
            }
        }

        /**
         * 从SSH服务端接收字节
         *
         * @param bytes 字节
         */
        public void receive(byte... bytes) {
            if (inputReceive) {
                String str = new String(bytes, charset);
                if (keyControl == KeyControl.KEY_UP || keyControl == KeyControl.KEY_DOWN) {
                    // 上下键只有第一条是正常的，后面的都是根据第一条进行退格删除再补充的。
                    // 8,8,8,99,100,32,47,112,114,50,111,99,47,
                    try {
                        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                            for (byte aByte : bytes) {
                                if (aByte == 8) {
                                    // 首位是退格键，就执行删除末尾值
                                    buffer.deleteCharAt(Math.max(buffer.length() - 1, 0));
                                } else if (aByte == 27) {
                                    // 遇到【逃离/取消】就跳出循环
                                    break;
                                } else if (aByte != 0) {
                                    outputStream.write(aByte);
                                }
                            }
                            buffer.append(new String(outputStream.toByteArray(), charset));
                        }
                        inputSelection = buffer.length();
                    } catch (Exception e) {
                        log.error("", e);
                    }
                } else {
                    if (keyControl == KeyControl.KEY_TAB) {
                        if (bytes[0] == 7) {
                            // 接收到终端响铃，就删除响铃
                            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
                        }
                        if (Arrays.equals(new byte[]{13, 10}, bytes)) {
                            inputReceive = false;
                            return;
                        }
                        // tab下文件很多
                        if (str.contains("y or n")) {
                            tabInputPause = true;
                            inputReceive = false;
                            return;
                        }
                        // cat 'hello word.txt'
                        // cat hello\ word.txt
                        if (str.split(" ").length > 1 && (!str.contains("'") && !str.contains("\\"))) {
                            inputReceive = false;
                            return;
                        }
                    }
                    // 非上下键输入输入中，如果接受到数据就执行插入数据，根据当前光标位置执行插入
                    // 存在退格，就从光标位置开始删除
                    int backCount = 0;
                    for (byte aByte : bytes) {
                        if (aByte == 8) {
                            buffer.deleteCharAt(inputSelection);
                            backCount++;
                        }
                    }
                    str = new String(Arrays.copyOfRange(bytes, 0, bytes.length - backCount), charset);
                    buffer.insert(inputSelection, str);
                    inputSelection += str.length();
                }
            }
            inputReceive = false;
        }

    }

    /**
     * 功能键枚举
     */
    public enum KeyControl {
        KEY_TAB((byte) 9), // TAB
        KEY_ETX((byte) 3), // Control + C
        KEY_ENTER((byte) 13), // Enter
        KEY_BACK((byte) 127), // 退格键
        KEY_DELETE(new byte[]{27, 91, 51, 126}), // DELETE键
        KEY_LEFT(new byte[]{27, 91, 68}), // 左
        KEY_RIGHT(new byte[]{27, 91, 67}), // 右
        KEY_UP(new byte[]{27, 91, 65}), // 上
        KEY_DOWN(new byte[]{27, 91, 66}), // 下
        KEY_HOME(new byte[]{27, 91, 72}),
        KEY_END(new byte[]{27, 91, 70}),
        KEY_FUNCTION(new byte[]{27, 91}), //其他功能键
        KEY_INPUT(new byte[]{-1}); // 正常输入

        private final byte[] control;

        KeyControl(byte... control) {
            this.control = control;
        }

        public static KeyControl getKeyControl(byte[] bytes) {
            for (KeyControl value : KeyControl.values()) {
                if (Arrays.equals(value.control, bytes)) {
                    return value;
                }
            }
            // 其他功能键
            if (Arrays.equals(KEY_FUNCTION.control, Arrays.copyOf(bytes, 2))) {
                return KEY_FUNCTION;
            }
            // 正常输入
            return KEY_INPUT;
        }
    }
}
