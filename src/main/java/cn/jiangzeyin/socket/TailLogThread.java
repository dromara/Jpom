package cn.jiangzeyin.socket;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;

import javax.websocket.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程处理
 *
 * @author jiangzeyin
 * date 2017/9/8
 */
public class TailLogThread implements Runnable {
    /**
     * 记录日志被哪些打开过
     */
    private static final ConcurrentHashMap<String, List<TailLogThread>> TAIL_LOG_THREAD_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();
    /**
     * 记录日志被修改过,需要重新打开
     */
    private static final ConcurrentHashMap<String, Boolean> LOG_CHANGE = new ConcurrentHashMap<>();


    private final Session session;
    private Evn evn;
    private String log;
    private InputStream inputStream;
    private Process process;
    private volatile boolean reload = false;
    private BufferedReader bufferedReader;

    private TailLogThread(String log, Session session, Evn evn) {
        this.session = session;
        this.evn = evn;
        this.log = log;
    }

    /**
     * 创建新的
     *
     * @param session
     * @param log
     * @param evn
     * @return
     */
    static TailLogThread createThread(Session session, String log, Evn evn) {
        List<TailLogThread> logThreads = TAIL_LOG_THREAD_CONCURRENT_HASH_MAP.computeIfAbsent(log, s -> new ArrayList<>());
        Boolean change = LOG_CHANGE.get(log);
        if (change != null) {
            for (TailLogThread logThread : logThreads) {
                //  停止上一次的读取
                logThread.process.destroy();
                // 标记需要重新执行
                logThread.reload = true;
            }
            LOG_CHANGE.remove(log);
        }
        TailLogThread tailLogThread = new TailLogThread(log, session, evn);
        logThreads.add(tailLogThread);
        return tailLogThread;
    }

    /**
     * 通知日志需要重新加载
     *
     * @param log log
     */
    public static void logChange(String log) {
        LOG_CHANGE.put(log, true);
    }

    private void loadInputStream() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
        if (bufferedReader != null) {
            bufferedReader.close();
        }
        // 执行tail -f命令
        process = Runtime.getRuntime().exec(String.format("tail -f %s", this.log));
        inputStream = process.getInputStream();
        bufferedReader = IoUtil.getReader(inputStream, CharsetUtil.CHARSET_UTF_8);
        // 已经重新加载
        reload = false;
    }

    void stop() {
        if (process != null) {
            process.destroy();
        }
        List<TailLogThread> logThreads = TAIL_LOG_THREAD_CONCURRENT_HASH_MAP.computeIfAbsent(log, s -> new ArrayList<>());
        logThreads.remove(this);
    }

    private void send(String msg) {
        try {
            SocketSession.send(session, msg);
        } catch (IOException e) {
            stop();
            if (evn != null) {
                evn.onError(session);
            }
        }
    }

    @Override
    public void run() {
        do {
            if (reload) {
                send("需要重新开始读取流");
            }
            try {
                loadInputStream();
                String line;
                // 会阻塞读取
                while ((line = bufferedReader.readLine()) != null) {
                    send(line);
                }
            } catch (Exception e) {
                stop();
                if (evn != null) {
                    evn.onError(session);
                }
            }
        } while (reload);

        try {
            inputStream.close();
            bufferedReader.close();
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("关闭流异常", e);
            send("关闭流异常:" + e.getMessage());
        }
        send("结束本次读取地址事件");
        DefaultSystemLog.LOG().info("结束本次读取地址事件");
    }

    public interface Evn {
        /**
         * 尝试次数过多
         *
         * @param session 会话
         */
        void onError(Session session);

    }
}