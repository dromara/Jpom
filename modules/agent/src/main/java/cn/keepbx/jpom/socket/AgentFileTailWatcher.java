package cn.keepbx.jpom.socket;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.util.BaseFileTailWatcher;
import cn.keepbx.util.LimitQueue;
import cn.keepbx.util.SocketSessionUtil;

import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件跟随器
 *
 * @author jiangzeyin
 * @date 2019/3/16
 */
public class AgentFileTailWatcher<T> extends BaseFileTailWatcher<T> {
    private static final ConcurrentHashMap<File, AgentFileTailWatcher<Session>> CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();


    private AgentFileTailWatcher(File logFile) throws IOException {
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
    public static void addWatcher(File file, Session session) throws IOException {
        if (!file.exists() || file.isDirectory()) {
            throw new IOException("文件不存在或者是目录:" + file.getPath());
        }
        AgentFileTailWatcher<Session> agentFileTailWatcher = CONCURRENT_HASH_MAP.computeIfAbsent(file, s -> {
            try {
                return new AgentFileTailWatcher<>(file);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("创建文件监听失败", e);
                return null;
            }
        });
        if (agentFileTailWatcher == null) {
            throw new IOException("加载文件失败:" + file.getPath());
        }
        agentFileTailWatcher.add(session, FileUtil.getName(file));
        agentFileTailWatcher.tailWatcherRun.start();
    }

    /**
     * 有客户端离线
     *
     * @param session 会话
     */
    public static void offline(Session session) {
        Collection<AgentFileTailWatcher<Session>> collection = CONCURRENT_HASH_MAP.values();
        for (AgentFileTailWatcher<Session> agentFileTailWatcher : collection) {
            agentFileTailWatcher.socketSessions.removeIf(session::equals);
            if (agentFileTailWatcher.socketSessions.isEmpty()) {
                agentFileTailWatcher.close();
            }
        }
    }

    /**
     * 关闭文件读取流
     *
     * @param fileName 文件名
     */
    public static void offlineFile(File fileName) {
        AgentFileTailWatcher<Session> agentFileTailWatcher = CONCURRENT_HASH_MAP.get(fileName);
        if (null == agentFileTailWatcher) {
            return;
        }
        Set<Session> socketSessions = agentFileTailWatcher.socketSessions;
        for (Session socketSession : socketSessions) {
            offline(socketSession);
        }
        agentFileTailWatcher.close();
    }

    /**
     * 关闭文件读取流
     *
     * @param fileName 文件名
     */
    static void offlineFile(File fileName, Session session) {
        AgentFileTailWatcher<Session> agentFileTailWatcher = CONCURRENT_HASH_MAP.get(fileName);
        if (null == agentFileTailWatcher) {
            return;
        }
        Set<Session> socketSessions = agentFileTailWatcher.socketSessions;
        for (Session socketSession : socketSessions) {
            if (socketSession.equals(session)) {
                offline(socketSession);
                break;
            }
        }
        if (agentFileTailWatcher.socketSessions.isEmpty()) {
            agentFileTailWatcher.close();
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
