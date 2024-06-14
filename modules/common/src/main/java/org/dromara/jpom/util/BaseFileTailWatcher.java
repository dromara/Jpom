/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.Tailer;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 文件跟随器工具
 *
 * @author bwcx_jzy
 * @since 2019/7/21
 */
@Slf4j
public abstract class BaseFileTailWatcher<T extends AutoCloseable> {

    private static int initReadLine = 10;

    public static void setInitReadLine(int initReadLine) {
        BaseFileTailWatcher.initReadLine = initReadLine;
    }

    protected File logFile;
    private final Charset charset;
    /**
     * 缓存近x条
     */
    private final LimitQueue<String> limitQueue = new LimitQueue<>(initReadLine);
    private Tailer tailer;

    /**
     * 所有会话
     */
    protected final Set<T> socketSessions = new HashSet<>();

    public BaseFileTailWatcher(File logFile, Charset charset) {
        this.logFile = logFile;
        this.charset = charset;
    }

    /**
     * 发生消息
     *
     * @param session 会话
     * @param msg     消息内容
     * @return 是否发送成功
     * @throws IOException io
     */
    protected abstract boolean send(T session, String msg) throws IOException;

    /**
     * 有新的日志
     *
     * @param msg 日志
     */
    private void sendAll(String msg) {
        Iterator<T> iterator = socketSessions.iterator();
        while (iterator.hasNext()) {
            T socketSession = iterator.next();
            try {
                boolean send = this.send(socketSession, msg);
                if (!send) {
                    //
                    this.errorAutoClose(socketSession);
                    iterator.remove();
                }
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.send_message_failure.9621"), e);
                this.errorAutoClose(socketSession);
                iterator.remove();
            }
        }
        if (this.socketSessions.isEmpty()) {
            this.close();
        }
    }

    private void errorAutoClose(T socketSession) {
        log.warn(I18nMessageUtil.get("i18n.message_send_failed.4dbe"), this.getId(socketSession));
        IoUtil.close(socketSession);
    }

    private String getId(T session) {
        Method byName = ReflectUtil.getMethodByName(session.getClass(), "getId");
        Assert.notNull(byName, I18nMessageUtil.get("i18n.no_get_id_method.2a65"));
        return ReflectUtil.invoke(session, byName);
    }

    /**
     * 添加监听会话
     *
     * @param name    文件名
     * @param session 会话
     */
    protected boolean add(T session, String name) throws IOException {
        String id = getId(session);
        Method byName = ReflectUtil.getMethodByName(session.getClass(), "getId");
        boolean match = this.socketSessions.stream()
            .anyMatch(t -> {
                String itemId = ReflectUtil.invoke(t, byName);
                return StrUtil.equals(id, itemId);
            });
        if (match) {
            return false;
        }
        if (this.socketSessions.add(session)) {
            this.send(session, StrUtil.format(I18nMessageUtil.get("i18n.listen_log_success_currently_sessions_viewing.a74a"), name, this.socketSessions.size()));
            // 开发发送头信息
            for (String s : limitQueue) {
                this.send(session, s);
            }
        }
        return true;
    }

    public void start() {
        //this.tailWatcherRun = new FileTailWatcherRun(logFile, this::sendAll);
        this.tailer = new Tailer(logFile, charset, line -> {
            limitQueue.offer(line);
            this.sendAll(line);
        }, initReadLine, DateUnit.SECOND.getMillis());
        this.tailer.start(true);
    }

    public void restart() {
        if (this.tailer != null) {
            this.tailer.stop();
        }
        this.sendAll("Relisten to the file............");
        this.start();
    }

    /**
     * 关闭
     */
    protected void close() {
        this.tailer.stop();
    }
}
