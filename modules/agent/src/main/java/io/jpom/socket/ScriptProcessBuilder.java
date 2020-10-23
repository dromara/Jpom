package io.jpom.socket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.model.data.ScriptModel;
import io.jpom.util.CommandUtil;
import io.jpom.util.SocketSessionUtil;

import javax.websocket.Session;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脚本执行
 *
 * @author jiangzeyin
 * @date 2019/4/25
 */
public class ScriptProcessBuilder implements Runnable {
    private static final ConcurrentHashMap<File, ScriptProcessBuilder> FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    private final ProcessBuilder processBuilder;
    private final Set<Session> sessions = new HashSet<>();
    private final File logFile;
    private final File scriptFile;
    private Process process;
    private InputStream inputStream;
    private InputStream errorInputStream;

    private ScriptProcessBuilder(ScriptModel scriptModel, String args) {
        this.logFile = scriptModel.logFile();
        this.scriptFile = scriptModel.getFile(true);
        //
        String script = FileUtil.getAbsolutePath(scriptFile);
        processBuilder = new ProcessBuilder();
        List<String> command = StrUtil.splitTrim(args, StrUtil.SPACE);
        command.add(0, script);
        if (SystemUtil.getOsInfo().isLinux()) {
            command.add(0, CommandUtil.SUFFIX);
        }
        DefaultSystemLog.getLog().info(CollUtil.join(command, StrUtil.SPACE));
        processBuilder.command(command);
    }

    public static void addWatcher(ScriptModel scriptModel, String args, Session session) {
        File file = scriptModel.getFile(true);
        ScriptProcessBuilder scriptProcessBuilder = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.computeIfAbsent(file, file1 -> {
            ScriptProcessBuilder scriptProcessBuilder1 = new ScriptProcessBuilder(scriptModel, args);
            ThreadUtil.execute(scriptProcessBuilder1);
            return scriptProcessBuilder1;
        });
        if (scriptProcessBuilder.sessions.add(session)) {
            if (FileUtil.exist(scriptProcessBuilder.logFile)) {
                // 读取之前的信息并发送
                FileUtil.readLines(scriptProcessBuilder.logFile, CharsetUtil.CHARSET_UTF_8, (LineHandler) line -> {
                    try {
                        SocketSessionUtil.send(session, line);
                    } catch (IOException e) {
                        DefaultSystemLog.getLog().error("发送消息失败", e);
                    }
                });
            }
        }
    }

    public static void stopWatcher(Session session) {
        Collection<ScriptProcessBuilder> scriptProcessBuilders = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.values();
        for (ScriptProcessBuilder scriptProcessBuilder : scriptProcessBuilders) {
            Set<Session> sessions = scriptProcessBuilder.sessions;
            sessions.removeIf(session1 -> session1.getId().equals(session.getId()));
        }
    }

    public static void stopRun(ScriptModel scriptModel) {
        File file = scriptModel.getFile(true);
        ScriptProcessBuilder scriptProcessBuilder = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.get(file);
        if (scriptProcessBuilder != null) {
            scriptProcessBuilder.end("停止运行");
        }
    }

    @Override
    public void run() {
        //初始化ProcessBuilder对象
        try {
            process = processBuilder.start();
            {
                inputStream = process.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, JpomApplication.getCharset());
                BufferedReader results = new BufferedReader(inputStreamReader);
                IoUtil.readLines(results, (LineHandler) ScriptProcessBuilder.this::handle);
            }
            {
                errorInputStream = process.getErrorStream();
                InputStreamReader inputStreamReader = new InputStreamReader(errorInputStream, JpomApplication.getCharset());
                BufferedReader results = new BufferedReader(inputStreamReader);
                IoUtil.readLines(results, (LineHandler) line -> ScriptProcessBuilder.this.handle("ERROR:" + line));
            }
            JsonMessage<String> jsonMessage = new JsonMessage<>(200, "执行完毕");
            JSONObject jsonObject = jsonMessage.toJson();
            jsonObject.put("op", ConsoleCommandOp.stop.name());
            this.end(jsonObject.toString());
        } catch (IORuntimeException ignored) {

        } catch (Exception e) {
            DefaultSystemLog.getLog().error("执行异常", e);
            this.end("执行异常：" + e.getMessage());
        }
    }

    /**
     * 结束执行
     *
     * @param msg 响应的消息
     */
    private void end(String msg) {
        if (this.process != null) {
            // windows 中不能正常关闭
            this.process.destroy();
            IoUtil.close(inputStream);
            IoUtil.close(errorInputStream);
        }
        Iterator<Session> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            try {
                SocketSessionUtil.send(session, msg);
            } catch (IOException e) {
                DefaultSystemLog.getLog().error("发送消息失败", e);
            }
            iterator.remove();
        }
        FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.remove(this.scriptFile);
    }

    /**
     * 响应
     *
     * @param line 信息
     */
    private void handle(String line) {
        // 写入文件
        List<String> fileLine = new ArrayList<>();
        fileLine.add(line);
        FileUtil.appendLines(fileLine, logFile, CharsetUtil.CHARSET_UTF_8);
        Iterator<Session> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            try {
                SocketSessionUtil.send(session, line);
            } catch (IOException e) {
                DefaultSystemLog.getLog().error("发送消息失败", e);
                iterator.remove();
            }
        }
    }
}
