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
package io.jpom.socket;

import cn.hutool.core.io.FileUtil;
import io.jpom.util.BaseFileTailWatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件跟随器
 *
 * @author jiangzeyin
 * @since 2019/07/21
 */
@Slf4j
public class ServiceFileTailWatcher<T> extends BaseFileTailWatcher<T> {
    private static final ConcurrentHashMap<File, ServiceFileTailWatcher<WebSocketSession>> CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    private ServiceFileTailWatcher(File logFile) {
        super(logFile);
    }

    public static int getOneLineCount() {
        return CONCURRENT_HASH_MAP.size();
    }

    /**
     * 添加文件监听
     *
     * @param file    文件
     * @param session 会话
     * @throws IOException 异常
     */
    public static void addWatcher(File file, WebSocketSession session) throws IOException {
        if (!file.exists() || file.isDirectory()) {
            throw new IOException("文件不存在或者是目录:" + file.getPath());
        }
        ServiceFileTailWatcher<WebSocketSession> agentFileTailWatcher = CONCURRENT_HASH_MAP.computeIfAbsent(file, s -> {
            try {
                return new ServiceFileTailWatcher<>(file);
            } catch (Exception e) {
                log.error("创建文件监听失败", e);
                return null;
            }
        });
        if (agentFileTailWatcher == null) {
            throw new IOException("加载文件失败:" + file.getPath());
        }
        agentFileTailWatcher.add(session, FileUtil.getName(file));
        agentFileTailWatcher.start();
    }

    /**
     * 有客户端离线
     *
     * @param session 会话
     */
    public static void offline(WebSocketSession session) {
        Collection<ServiceFileTailWatcher<WebSocketSession>> collection = CONCURRENT_HASH_MAP.values();
        for (ServiceFileTailWatcher<WebSocketSession> agentFileTailWatcher : collection) {
            agentFileTailWatcher.socketSessions.removeIf(session::equals);
            if (agentFileTailWatcher.socketSessions.isEmpty()) {
                agentFileTailWatcher.close();
            }
        }
    }

    /**
     * 关闭文件
     *
     * @param fileName 文件
     */
    public static void offlineFile(File fileName) {
        ServiceFileTailWatcher<WebSocketSession> agentFileTailWatcher = CONCURRENT_HASH_MAP.get(fileName);
        if (null == agentFileTailWatcher) {
            return;
        }
        Set<WebSocketSession> socketSessions = agentFileTailWatcher.socketSessions;
        for (WebSocketSession socketSession : socketSessions) {
            offline(socketSession);
        }
        agentFileTailWatcher.close();
    }

    /**
     * 关闭文件读取流
     *
     * @param fileName 文件名
     * @param session  回话
     */
    public static void offlineFile(File fileName, WebSocketSession session) {
        ServiceFileTailWatcher<WebSocketSession> serviceFileTailWatcher = CONCURRENT_HASH_MAP.get(fileName);
        if (null == serviceFileTailWatcher) {
            return;
        }
        Set<WebSocketSession> socketSessions = serviceFileTailWatcher.socketSessions;
        for (WebSocketSession socketSession : socketSessions) {
            if (socketSession.equals(session)) {
                offline(socketSession);
                break;
            }
        }
        if (serviceFileTailWatcher.socketSessions.isEmpty()) {
            serviceFileTailWatcher.close();
        }
    }

    /**
     * 关闭
     */
    @Override
    protected void close() {
        super.close();
        // 清理线程记录
        CONCURRENT_HASH_MAP.remove(this.logFile);
    }
}
