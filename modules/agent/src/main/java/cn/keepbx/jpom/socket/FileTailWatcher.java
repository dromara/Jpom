package cn.keepbx.jpom.socket;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileMode;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.model.data.TomcatInfoModel;
import cn.keepbx.jpom.system.AgentExtConfigBean;
import cn.keepbx.jpom.util.CharsetDetector;
import cn.keepbx.jpom.util.LimitQueue;
import cn.keepbx.jpom.util.SocketSessionUtil;
import com.alibaba.fastjson.JSONObject;

import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
public class FileTailWatcher implements Runnable {
    private static final ConcurrentHashMap<String, FileTailWatcher> CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();
    /**
     * 缓存近10条
     */
    private LimitQueue limitQueue = new LimitQueue(10);
    private final RandomAccessFile randomFile;
    /**
     * 所有会话
     */
    private Set<Session> socketSessions = new HashSet<>();
    private final String log;
    /**
     * 是否已经开始执行
     */
    private boolean start = false;
    private final Charset charset;

    /**
     * 添加文件监听
     *
     * @param projectInfoModel 项目
     * @param session          会话
     * @throws IOException 异常
     */
    public static void addWatcher(ProjectInfoModel projectInfoModel, Session session) throws IOException {
        //        日志文件路径
        String log = projectInfoModel.getLog();
        File file = new File(log);
        if (!file.exists()) {
            throw new IOException("文件不存在:" + file.getPath());
        }
        FileTailWatcher fileTailWatcher = CONCURRENT_HASH_MAP.computeIfAbsent(log, s -> {
            try {
                return new FileTailWatcher(file, s);
            } catch (IOException e) {
                DefaultSystemLog.ERROR().error("创建文件监听失败", e);
                return null;
            }
        });
        if (fileTailWatcher == null) {
            throw new IOException("加载文件异常:" + file.getPath());
        }
        fileTailWatcher.add(session, projectInfoModel.getName());
        //
        fileTailWatcher.start();
    }

    /**
     * 添加文件监听
     *
     * @param tomcatInfoModel tomcat
     * @param name            日志文件名称
     * @param session         会话
     * @throws IOException 异常
     */
    public static void addWatcher(TomcatInfoModel tomcatInfoModel, String name, Session session) throws IOException {
        String path = tomcatInfoModel.getPath() + "/logs/" + name;
        File file = FileUtil.file(path);
        if (!file.exists()) {
            throw new IOException("文件不存在:" + file.getPath());
        }
        //发送文件名  标记返回日志
        JSONObject object = new JSONObject();
        object.put("fileName", name);
        SocketSessionUtil.send(session, object.toString());
        FileTailWatcher fileTailWatcher = CONCURRENT_HASH_MAP.computeIfAbsent(name, s -> {
            try {
                return new FileTailWatcher(file, s);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("创建文件监听失败", e);
                return null;
            }
        });
        if (fileTailWatcher == null) {
            throw new IOException("加载文件失败:" + file.getPath());
        }
        fileTailWatcher.add(session, name);
        fileTailWatcher.start();
    }

    /**
     * 有客户端离线
     *
     * @param session 会话
     */
    public static void offline(Session session) {
        Collection<FileTailWatcher> collection = CONCURRENT_HASH_MAP.values();
        for (FileTailWatcher fileTailWatcher : collection) {
            fileTailWatcher.socketSessions.removeIf(session::equals);
        }
    }

    private FileTailWatcher(File file, String log) throws IOException {
        this.log = log;
        this.randomFile = new RandomAccessFile(file, FileMode.r.name());
        Charset detSet = AgentExtConfigBean.getInstance().getLogFileCharset();
        if (detSet == null) {
            detSet = CharsetUtil.charset(new CharsetDetector().detectChineseCharset(file));
        }
        this.charset = (detSet == StandardCharsets.US_ASCII) ? CharsetUtil.CHARSET_UTF_8 : detSet;
        if (file.length() > 0) {
            // 开始读取
            this.startRead();
        }
    }

    /**
     * 添加监听会话
     *
     * @param session 会话
     */
    private void add(Session session, String name) {
        if (this.socketSessions.add(session) || this.socketSessions.contains(session)) {
            if (this.limitQueue.size() <= 0) {
                this.send(session, "日志文件为空");
                return;
            }
            this.send(session, StrUtil.format("监听{}日志成功,目前共有{}人正在查看", name, this.socketSessions.size()));
            // 开发发送头信息
            for (String s : this.limitQueue) {
                this.send(session, s);
            }
        }
        //        else {
        //            this.send(session, "添加日志监听失败");
        //        }
    }

    private void send(Session session, String msg) {
        try {
            SocketSessionUtil.send(session, msg);
        } catch (IOException ignored) {
        }
    }

    private void startRead() throws IOException {
        long len = randomFile.length();
        long start = randomFile.getFilePointer();
        long nextEnd = start + len - 1;
        randomFile.seek(nextEnd);
        int c;
        while (nextEnd > start) {
            // 满
            if (limitQueue.full()) {
                break;
            }
            c = randomFile.read();
            if (c == '\n' || c == '\r') {
                this.readLine();
                nextEnd--;
            }
            nextEnd--;
            randomFile.seek(nextEnd);
            if (nextEnd == 0) {
                // 当文件指针退至文件开始处，输出第一行
                this.readLine();
                break;
            }
        }
        // 移动到尾部
        randomFile.seek(len);
    }

    private void readLine() throws IOException {
        String line = randomFile.readLine();
        if (line != null) {
            line = CharsetUtil.convert(line, CharsetUtil.CHARSET_ISO_8859_1, charset);
            limitQueue.offerFirst(line);
        }
    }

    /**
     * 读取文件内容
     *
     * @throws IOException IO
     */
    private void read() throws IOException {
        final long currentLength = randomFile.length();
        final long position = randomFile.getFilePointer();
        if (0 == currentLength || currentLength == position) {
            // 内容长度不变时忽略此次
            return;
        } else if (currentLength < position) {
            // 如果内容变短，说明文件做了删改，回到内容末尾
            randomFile.seek(currentLength);
            return;
        }
        String tmp;
        while ((tmp = randomFile.readLine()) != null) {
            tmp = CharsetUtil.convert(tmp, CharsetUtil.CHARSET_ISO_8859_1, charset);
            limitQueue.offer(tmp);
            sendAll(tmp);
        }
        // 记录当前读到的位置
        this.randomFile.seek(currentLength);
    }

    private void sendAll(String msg) {
        Iterator<Session> iterator = socketSessions.iterator();
        while (iterator.hasNext()) {
            Session socketSession = iterator.next();
            try {
                SocketSessionUtil.send(socketSession, msg);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("发送消息失败", e);
                iterator.remove();
            }
        }
    }

    /**
     * 开始监听
     */
    private void start() {
        if (this.start) {
            return;
        }
        ThreadUtil.execute(this);
        this.start = true;
    }

    @Override
    public void run() {
        while (socketSessions.size() > 0) {
            try {
                this.read();
            } catch (IOException e) {
                DefaultSystemLog.ERROR().error("读取文件发送异常", e);
                this.sendAll("读取文件发生异常：" + e.getMessage());
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
        // 通知客户端
        if (socketSessions.size() > 0) {
            this.sendAll("服务主动关闭日志文件");
        }
        IoUtil.close(this.randomFile);
        // 清理线程记录
        CONCURRENT_HASH_MAP.remove(this.log);
    }

    /**
     * 关闭文件读取流
     *
     * @param fileName 文件名
     */
    public static void offTomcatFileListen(String fileName) {
        FileTailWatcher fileTailWatcher = CONCURRENT_HASH_MAP.get(fileName);
        if (null == fileTailWatcher) {
            return;
        }
        IoUtil.close(fileTailWatcher.randomFile);
        Set<Session> socketSessions = fileTailWatcher.socketSessions;
        for (Session socketSession : socketSessions) {
            offline(socketSession);
        }
        CONCURRENT_HASH_MAP.remove(fileName);
    }
}