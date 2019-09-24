package io.jpom.socket.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.socket.BaseHandler;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ssh 处理
 *
 * @author bwcx_jzy
 * @date 2019/8/9
 */
public class SshHandler extends BaseHandler {

    private static final ConcurrentHashMap<String, HandlerItem> HANDLER_ITEM_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SshModel sshItem = (SshModel) session.getAttributes().get("sshItem");
        Map<String, String[]> parameterMap = (Map<String, String[]>) session.getAttributes().get("parameterMap");
        String[] tails = parameterMap.get("tail");
        //
        String tail = null;
        if (tails != null && tails.length > 0 && !StrUtil.isEmptyOrUndefined(tails[0])) {
            tail = tails[0];
            List<String> fileDirs = sshItem.getFileDirs();
            if (fileDirs == null) {
                sendBinary(session, "没有配置路径");
                return;
            }
            File file = FileUtil.file(tail);
            boolean find = false;
            for (String fileDir : fileDirs) {
                if (FileUtil.isSub(FileUtil.file(fileDir), file)) {
                    find = true;
                    break;
                }
            }
            if (!find) {
                sendBinary(session, "非法路径");
                return;
            }
        }
        Session openSession = JschUtil.openSession(sshItem.getHost(), sshItem.getPort(), sshItem.getUser(), sshItem.getPassword());
        Channel channel = JschUtil.createChannel(openSession, ChannelType.SHELL);
        InputStream inputStream = channel.getInputStream();
        OutputStream outputStream = channel.getOutputStream();
        //
        Charset charset = sshItem.getCharsetT();
        HandlerItem handlerItem = new HandlerItem(session, inputStream, outputStream, openSession, channel, charset);
        handlerItem.startRead();
        HANDLER_ITEM_CONCURRENT_HASH_MAP.put(session.getId(), handlerItem);
        //
        Thread.sleep(1000);
        if (tail == null) {
            this.call(session, StrUtil.CR);
        } else {
            // 查看文件
            tail = FileUtil.normalize(tail);
            this.call(session, StrUtil.format("tail -f {}", tail));
            this.call(session, StrUtil.CR);
        }
    }

    private void call(WebSocketSession session, String msg) throws Exception {
        JSONObject first = new JSONObject();
        first.put("data", msg);
        // 触发消息
        this.handleTextMessage(session, new TextMessage(first.toJSONString()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JSONObject jsonObject = JSONObject.parseObject(message.getPayload());
        if (jsonObject.containsKey("resize")) {
            return;
        }
        String data = jsonObject.getString("data");
        if (StrUtil.isEmpty(data)) {
            return;
        }
        HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
        // 判断权限
        if (this.checkCommand(handlerItem, data)) {
            return;
        }
        if ("\t".equals(data)) {
            this.sendCommand(handlerItem, data);
            return;
        }
        if (StrUtil.CR.equals(data)) {
            if (handlerItem.dataToDst.length() > 0) {
                data = StrUtil.CRLF;
            }
        } else {
            handlerItem.dataToDst.append(data);
            // 判断权限
            if (this.checkCommand(handlerItem, null)) {
                return;
            }
        }
        this.sendCommand(handlerItem, data);
        if (!StrUtil.CRLF.equals(data) && !StrUtil.CR.equals(data)) {
            sendBinary(handlerItem.session, data);
        }
    }

    private void sendCommand(HandlerItem handlerItem, String data) throws IOException {
        handlerItem.outputStream.write(data.getBytes());
        handlerItem.outputStream.flush();
    }

    private boolean checkCommand(HandlerItem handlerItem, String data) throws Exception {
        UserModel userInfo = (UserModel) handlerItem.session.getAttributes().get("userInfo");
        if (!userInfo.isDemoUser()) {
            // demo user判断
            return false;
        }
        if (data == null) {
            data = handlerItem.dataToDst.toString();
        }
        if (StrUtil.containsAnyIgnoreCase(data, "rm ")) {
            //
            handlerItem.dataToDst.setLength(0);
            this.call(handlerItem.session, "没有权限");
            this.call(handlerItem.session, "\\u0016");
            this.call(handlerItem.session, StrUtil.CR);
            return true;
        }
        return false;
    }


    private class HandlerItem implements Runnable {
        private WebSocketSession session;
        private StringBuilder dataToDst = new StringBuilder();
        private InputStream inputStream;
        private OutputStream outputStream;
        private Session openSession;
        private Channel channel;
        private Charset charset;

        HandlerItem(WebSocketSession session,
                    InputStream inputStream,
                    OutputStream outputStream,
                    Session openSession,
                    Channel channel,
                    Charset charset) {
            this.session = session;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.openSession = openSession;
            this.channel = channel;
            this.charset = charset;
        }

        void startRead() throws JSchException {
            this.channel.connect();
            ThreadUtil.execute(this);
        }

        @Override
        public void run() {
            final String[] preMsg = {""};
            try {
                IoUtil.readLines(inputStream, charset, (LineHandler) msg -> {
                    msg = StrUtil.CRLF + msg;
                    if (preMsg[0].equals(msg)) {
                        sendBinary(session, msg);
                        return;
                    }
                    //
                    if (msg.equals(preMsg[0] + dataToDst.toString())) {
                        return;
                    }
                    if (StrUtil.isEmpty(msg) || StrUtil.CRLF.equals(msg)) {
                        return;
                    }
                    preMsg[0] = msg;
                    sendBinary(session, msg);
                    dataToDst.setLength(0);
                });
            } catch (Exception e) {
                if (!this.openSession.isConnected()) {
                    return;
                }
                DefaultSystemLog.getLog().error("读取错误", e);
                SshHandler.this.destroy(this.session);
            }
        }
    }

    @Override
    public void destroy(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException ignored) {
        }
        HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
        IoUtil.close(handlerItem.inputStream);
        IoUtil.close(handlerItem.outputStream);
        JschUtil.close(handlerItem.channel);
        JschUtil.close(handlerItem.openSession);
    }

    private static void sendBinary(WebSocketSession session, String msg) {
        // 判断退格键
        char[] chars = msg.toCharArray();
        if (chars.length == 1 && chars[0] == 127) {
            return;
        }
        synchronized (session.getId()) {
            BinaryMessage byteBuffer = new BinaryMessage(msg.getBytes());
            try {
                session.sendMessage(byteBuffer);
            } catch (IOException e) {
                DefaultSystemLog.getLog().error("发送消息失败:" + msg, e);
            }
        }
    }
}
