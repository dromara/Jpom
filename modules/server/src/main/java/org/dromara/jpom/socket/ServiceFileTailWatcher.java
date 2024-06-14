/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.SafeConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.util.BaseFileTailWatcher;
import org.dromara.jpom.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件跟随器
 *
 * @author bwcx_jzy
 * @since 2019/07/21
 */
@Slf4j
public class ServiceFileTailWatcher<T extends AutoCloseable> extends BaseFileTailWatcher<T> {
    private static final ConcurrentHashMap<File, ServiceFileTailWatcher<WebSocketSession>> CONCURRENT_HASH_MAP = new SafeConcurrentHashMap<>();

    private static Charset charset;

    public static void setCharset(Charset charset) {
        ServiceFileTailWatcher.charset = charset;
    }

    private ServiceFileTailWatcher(File logFile) {
        super(logFile, charset);
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
            throw new IOException(I18nMessageUtil.get("i18n.file_or_directory_not_found.f03e") + file.getPath());
        }
        ServiceFileTailWatcher<WebSocketSession> agentFileTailWatcher = CONCURRENT_HASH_MAP.computeIfAbsent(file, s -> {
            try {
                return new ServiceFileTailWatcher<>(file);
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.create_file_watch_failure.bc1a"), e);
                return null;
            }
        });
        if (agentFileTailWatcher == null) {
            throw new IOException(I18nMessageUtil.get("i18n.load_file_failure.86cc") + file.getPath());
        }
        if (agentFileTailWatcher.add(session, FileUtil.getName(file))) {
            agentFileTailWatcher.start();
        }
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

    @Override
    protected boolean send(T session, String msg) throws IOException {
        return SocketSessionUtil.send((WebSocketSession) session, msg);
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
