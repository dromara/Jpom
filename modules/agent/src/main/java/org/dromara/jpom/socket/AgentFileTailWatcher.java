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

import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件跟随器
 *
 * <p>插件端控制台/系统日志的文件监听缓存。修复点：
 * <ul>
 *     <li>关闭与新增会话的竞态：watcher 关闭后会话不能再被加入（否则会话与 Tailer 线程
 *     会挂在已从缓存移除的 watcher 上永久泄漏）；</li>
 *     <li>缓存摘除使用 {@code computeIfPresent + 实例比对}，避免误删并发重建的新实例；</li>
 *     <li>离线/关闭只遍历快照并幂等清理，避免并发修改异常。</li>
 * </ul>
 *
 * @author bwcx_jzy
 * @since 2019/3/16
 */
@Slf4j
public class AgentFileTailWatcher<T extends AutoCloseable> extends BaseFileTailWatcher<T> {
    private static final ConcurrentHashMap<File, AgentFileTailWatcher<Session>> CONCURRENT_HASH_MAP = new SafeConcurrentHashMap<>();

    private AgentFileTailWatcher(File logFile, Charset charset) {
        super(logFile, charset);
    }

    public static int getOneLineCount() {
        return CONCURRENT_HASH_MAP.size();
    }

    /**
     * 添加文件监听
     *
     * @param file    文件
     * @param charset 编码格式
     * @param session 会话
     * @throws IOException 异常
     */
    public static boolean addWatcher(File file, Charset charset, Session session) throws IOException {
        if (!FileUtil.isFile(file)) {
            log.warn("{}{}", I18nMessageUtil.get("i18n.file_or_directory_not_found.f03e"), file.getPath());
            return false;
        }
        while (true) {
            AgentFileTailWatcher<Session> agentFileTailWatcher = CONCURRENT_HASH_MAP.computeIfAbsent(file, s -> {
                try {
                    return new AgentFileTailWatcher<>(file, charset);
                } catch (Exception e) {
                    log.error(I18nMessageUtil.get("i18n.create_file_watch_failure.bc1a"), e);
                    return null;
                }
            });
            if (agentFileTailWatcher == null) {
                throw new IOException(I18nMessageUtil.get("i18n.load_file_failure.86cc") + file.getPath());
            }
            if (agentFileTailWatcher.add(session, FileUtil.getName(file))) {
                return true;
            }
            if (!agentFileTailWatcher.isClosed()) {
                // 同一会话已经在监听该文件
                return true;
            }
            // 与 closeIfEmpty/close 并发：该实例已关闭并从缓存移除，重新获取/创建 watcher 后重试
            CONCURRENT_HASH_MAP.remove(file, agentFileTailWatcher);
        }
    }

    /**
     * 有客户端离线
     *
     * @param session 会话
     */
    public static void offline(Session session) {
        for (AgentFileTailWatcher<Session> agentFileTailWatcher : new ArrayList<>(CONCURRENT_HASH_MAP.values())) {
            if (agentFileTailWatcher.removeSession(session)) {
                agentFileTailWatcher.closeIfEmpty();
            }
        }
    }

    /**
     * 关闭文件读取流
     *
     * @param fileName 文件名
     */
    public static void offlineFile(File fileName) {
        AgentFileTailWatcher<Session> agentFileTailWatcher = CONCURRENT_HASH_MAP.remove(fileName);
        if (agentFileTailWatcher != null) {
            agentFileTailWatcher.close();
        }
    }

    /**
     * 重新监听
     *
     * @param fileName 文件名
     */
    public static void reWatcher(File fileName) {
        AgentFileTailWatcher<Session> agentFileTailWatcher = CONCURRENT_HASH_MAP.get(fileName);
        if (agentFileTailWatcher != null) {
            agentFileTailWatcher.restart();
        }
    }

    /**
     * 关闭指定会话对指定文件的监听
     *
     * @param fileName 文件名
     * @param session  会话
     */
    static void offlineFile(File fileName, Session session) {
        AgentFileTailWatcher<Session> agentFileTailWatcher = CONCURRENT_HASH_MAP.get(fileName);
        if (agentFileTailWatcher == null) {
            return;
        }
        if (agentFileTailWatcher.removeSession(session)) {
            agentFileTailWatcher.closeIfEmpty();
        }
    }

    @Override
    protected boolean send(T session, String msg) throws IOException {
        SocketSessionUtil.send((Session) session, msg);
        return true;
    }

    @Override
    protected void onClosed() {
        // 只摘除当前实例，避免与并发重建的新 watcher 混淆
        CONCURRENT_HASH_MAP.computeIfPresent(this.logFile, (file, watcher) -> watcher == this ? null : watcher);
    }
}
