/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dromara.jpom.util;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.io.file.Tailer;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
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
public abstract class BaseFileTailWatcher<T> {

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
     * @throws IOException io
     */
    protected abstract void send(T session, String msg) throws IOException;

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
                this.send(socketSession, msg);
            } catch (Exception e) {
                log.error("发送消息失败", e);
                iterator.remove();
            }
        }
        if (this.socketSessions.isEmpty()) {
            this.close();
        }
    }

    /**
     * 添加监听会话
     *
     * @param name    文件名
     * @param session 会话
     */
    protected boolean add(T session, String name) throws IOException {
        Method byName = ReflectUtil.getMethodByName(session.getClass(), "getId");
        Assert.notNull(byName, "没有  getId 方法");
        String id = ReflectUtil.invoke(session, byName);
        boolean match = this.socketSessions.stream().anyMatch(t -> {
            String itemId = ReflectUtil.invoke(t, byName);
            return StrUtil.equals(id, itemId);
        });
        if (match) {
            return false;
        }
        if (this.socketSessions.add(session)) {
            this.send(session, StrUtil.format("监听{}日志成功,目前共有{}人正在查看", name, this.socketSessions.size()));
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
