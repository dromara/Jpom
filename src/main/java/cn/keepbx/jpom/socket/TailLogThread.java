//package cn.keepbx.jpom.socket;
//
//import cn.hutool.core.io.IoUtil;
//import cn.jiangzeyin.common.DefaultSystemLog;
//import cn.keepbx.jpom.common.commander.AbstractCommander;
//
//import javax.websocket.Session;
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * 线程处理
// *
// * @author jiangzeyin
// * @date 2017/9/8
// */
//public class TailLogThread implements Runnable {
//    /**
//     * 记录日志被哪些打开过
//     */
//    private static final ConcurrentHashMap<String, List<TailLogThread>> TAIL_LOG_THREAD_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();
//
//
//    private final Session session;
//    private Evn evn;
//    private String log;
//    private InputStream inputStream;
//    private Process process;
//    private volatile boolean reload = false;
//    private BufferedReader bufferedReader;
//
//    private TailLogThread(String log, Session session, Evn evn) {
//        this.session = session;
//        this.evn = evn;
//        this.log = log;
//    }
//
//
//
//    private void loadInputStream() throws IOException {
//        if (inputStream != null) {
//            inputStream.close();
//        }
//        if (bufferedReader != null) {
//            bufferedReader.close();
//        }
////        final RandomAccessFile randomFile = new RandomAccessFile(this.log, "rw");
//        // 执行tail -f命令
//        process = Runtime.getRuntime().exec(String.format("tail -f %s", this.log));
//        inputStream = process.getInputStream();
//        bufferedReader = IoUtil.getReader(inputStream, AbstractCommander.getInstance().getCharset());
//        // 已经重新加载
//        reload = false;
//    }
//
//    void stop() {
//        if (process != null) {
//            process.destroy();
//        }
//        List<TailLogThread> logThreads = TAIL_LOG_THREAD_CONCURRENT_HASH_MAP.computeIfAbsent(log, s -> new ArrayList<>());
//        logThreads.remove(this);
//    }
//
//    private void send(String msg) {
//        try {
//            SocketSessionUtil.send(session, msg);
//        } catch (IOException e) {
//            stop();
//            if (evn != null) {
//                evn.onError(session);
//            }
//        }
//    }
//
//    @Override
//    public void run() {
//        do {
//            if (reload) {
//                send("需要重新开始读取流");
//            }
//            try {
//                loadInputStream();
//                String line;
//                // 会阻塞读取
//                while ((line = bufferedReader.readLine()) != null) {
//                    send(line);
//                }
//            } catch (Exception e) {
//                stop();
//                if (evn != null) {
//                    evn.onError(session);
//                }
//            }
//            // 延迟判断是否有新的流需要读取
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException ignored) {
//            }
//        } while (reload);
//        // 关闭流
//        IoUtil.close(inputStream);
//        IoUtil.close(bufferedReader);
//        send("结束本次读取地址事件");
//        DefaultSystemLog.LOG().info("结束本次读取地址事件");
//    }
//
//    public interface Evn {
//        /**
//         * 尝试次数过多
//         *
//         * @param session 会话
//         */
//        void onError(Session session);
//
//    }
//}