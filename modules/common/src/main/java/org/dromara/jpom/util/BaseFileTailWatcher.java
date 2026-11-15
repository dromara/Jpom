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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件跟随器工具
 *
 * <p>会话集合与生命周期均按并发场景实现：WebSocket 关闭回调线程、Tailer 定时线程、
 * 业务线程会同时调用 add/offline/close，旧实现使用 HashSet + 无状态 close 存在
 * ConcurrentModificationException、重复关闭 Tailer、以及“关闭竞态下会话被加入已移除的
 * watcher”导致 Tailer/会话永久泄漏的问题。修复后：
 * <ul>
 *     <li>会话使用 {@link ConcurrentHashMap} 保存，按会话 ID 去重，遍历时使用快照；</li>
 *     <li>add/start/restart/close 通过生命周期锁保证原子性，关闭后不再接受新会话；</li>
 *     <li>会话集合清空后只关闭一次 Tailer，并由子类在 {@link #onClosed()} 中原子摘除缓存。</li>
 * </ul>
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

    /**
     * 生命周期锁：保证 add/start/restart/close 的状态切换是原子的
     */
    private final Object lifecycleLock = new Object();
    private Tailer tailer;
    private volatile boolean closed;

    /**
     * 所有会话（会话ID -> 会话），并发安全
     */
    protected final Map<String, T> socketSessions = new ConcurrentHashMap<>();

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
        if (this.closed) {
            return;
        }
        // 使用快照遍历：add/offline 可能在 Tailer 发送过程中并发修改会话集合
        Collection<T> snapshot = new ArrayList<>(this.socketSessions.values());
        for (T socketSession : snapshot) {
            try {
                if (!this.send(socketSession, msg)) {
                    this.removeAndClose(socketSession);
                }
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.send_message_failure.9621"), e);
                this.removeAndClose(socketSession);
            }
        }
        this.closeIfEmpty();
    }

    private void removeAndClose(T socketSession) {
        String id = this.getIdQuietly(socketSession);
        if (id != null) {
            this.socketSessions.remove(id, socketSession);
        }
        this.errorAutoClose(socketSession);
    }

    private void errorAutoClose(T socketSession) {
        String id = this.getIdQuietly(socketSession);
        if (id != null) {
            log.warn(I18nMessageUtil.get("i18n.message_send_failed.4dbe"), id);
        } else {
            log.warn(I18nMessageUtil.get("i18n.message_send_failed.4dbe"), socketSession);
        }
        IoUtil.close(socketSession);
    }

    private String getIdQuietly(T session) {
        try {
            return this.getId(session);
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.no_get_id_method.2a65"), e);
            return null;
        }
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
     * @return true 表示新增成功；false 表示 watcher 已关闭，调用方应重新获取/创建 watcher
     * @throws IOException 发送欢迎消息失败
     */
    protected boolean add(T session, String name) throws IOException {
        String id = this.getId(session);
        synchronized (this.lifecycleLock) {
            if (this.closed) {
                return false;
            }
            if (this.socketSessions.containsKey(id)) {
                return false;
            }
            this.socketSessions.put(id, session);
            try {
                this.send(session, StrUtil.format(I18nMessageUtil.get("i18n.listen_log_success_currently_sessions_viewing.a74a"), name, this.socketSessions.size()));
                // 发送头信息
                for (String s : limitQueue) {
                    this.send(session, s);
                }
            } catch (IOException | RuntimeException e) {
                // 发送失败时不能把会话留在集合里，否则会话与 watcher 会一起泄漏
                this.socketSessions.remove(id, session);
                throw e;
            }
            this.startLocked();
            return true;
        }
    }

    /**
     * 移除会话（不关闭会话）
     *
     * @param session 会话
     * @return 是否移除成功
     */
    protected boolean removeSession(T session) {
        String id = this.getIdQuietly(session);
        if (id == null) {
            return false;
        }
        return this.socketSessions.remove(id, session);
    }

    public void start() {
        synchronized (this.lifecycleLock) {
            this.startLocked();
        }
    }

    private void startLocked() {
        if (this.closed || this.tailer != null) {
            return;
        }
        this.tailer = new Tailer(logFile, charset, line -> {
            limitQueue.offer(line);
            this.sendAll(line);
        }, initReadLine, DateUnit.SECOND.getMillis());
        this.tailer.start(true);
    }

    public void restart() {
        synchronized (this.lifecycleLock) {
            if (this.closed) {
                return;
            }
            this.stopTailerLocked();
        }
        this.sendAll("Relisten to the file............");
        this.start();
    }

    private void stopTailerLocked() {
        if (this.tailer == null) {
            return;
        }
        try {
            this.tailer.stop();
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.close_exception.5b86"), e);
        } finally {
            this.tailer = null;
        }
    }

    /**
     * 关闭
     */
    protected void close() {
        Collection<T> snapshot;
        synchronized (this.lifecycleLock) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            this.stopTailerLocked();
            snapshot = new ArrayList<>(this.socketSessions.values());
            this.socketSessions.clear();
        }
        for (T session : snapshot) {
            IoUtil.close(session);
        }
        this.onClosed();
    }

    /**
     * 会话全部离线后关闭 watcher；多个线程同时触发时只会关闭一次
     */
    protected void closeIfEmpty() {
        synchronized (this.lifecycleLock) {
            if (this.closed || !this.socketSessions.isEmpty()) {
                return;
            }
            this.closed = true;
            this.stopTailerLocked();
        }
        this.onClosed();
    }

    /**
     * watcher 关闭回调：子类用于从静态缓存中原子摘除当前实例
     */
    protected void onClosed() {
        // 默认无缓存
    }

    protected boolean isClosed() {
        return this.closed;
    }

    protected boolean isEmpty() {
        return this.socketSessions.isEmpty();
    }
}
