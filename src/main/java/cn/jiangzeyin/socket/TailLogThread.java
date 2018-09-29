package cn.jiangzeyin.socket;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;

import javax.websocket.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.sun.org.apache.xml.internal.security.keys.keyresolver.KeyResolver.iterator;

/**
 * 线程处理
 *
 * @author jiangzeyin
 * date 2017/9/8
 */
public class TailLogThread implements Runnable {


    private static final ConcurrentHashMap<String, List<TailLogThread>> TAIL_LOG_THREAD_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    private final Session session;
    private Evn evn;
    private int errorCount;
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

    public static TailLogThread createThread(Session session, String log, Evn evn) throws IOException {
        List<TailLogThread> logThreads = TAIL_LOG_THREAD_CONCURRENT_HASH_MAP.computeIfAbsent(log, s -> new ArrayList<>());
        for (TailLogThread logThread : logThreads) {
            //  停止上一次的读取
            logThread.process.destroy();
            // 标记需要重新执行
            logThread.reload = true;
        }
        TailLogThread tailLogThread = new TailLogThread(log, session, evn);
        logThreads.add(tailLogThread);
        return tailLogThread;
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

    public void send(String msg) {
        if (msg == null) {
            return;
        }
        if (!session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.getBasicRemote().sendText(msg);
            }
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("发送消息失败", e);
            errorCount++;
            if (errorCount >= 10) {
                DefaultSystemLog.LOG().info("失败次数超过大于10次，结束本次事件");
                throw new RuntimeException(e);
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

    public static void main(String[] args) throws IOException {
        Process process = Runtime.getRuntime().exec(String.format("tail -f %s", "/boot-line/boot/online/run.log"));
        InputStream inputStream = process.getInputStream();
        BufferedReader bufferedReader = IoUtil.getReader(inputStream, CharsetUtil.CHARSET_UTF_8);
        String line;
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("关闭");
                process.destroy();
            }
        }.start();
        while ((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
        }
    }
}