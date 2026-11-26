/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket.handler.record;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.log.SshTerminalSessionModel;
import org.dromara.jpom.service.dblog.SshTerminalSessionService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * asciicast v2 终端录制器
 * <p>
 * 存储格式：ndjson，首行 header，其后每行 `[时间(秒), 事件码, 数据]`；事件码 o 输出、i 输入、m 标记、r 尺寸。
 * 规范：https://docs.asciinema.org/manual/asciicast/v2/
 * <p>
 * 约定：单会话单文件，超过 {@link #MAX_CAST_BYTES} 后写一条截断标记并停止记录；
 * 任何写入异常只置为失败状态并记日志，绝不影响终端会话本身。
 *
 * @author bwcx_jzy
 * @since 2026/11/26
 */
@Slf4j
public class AsciicastTerminalRecorder implements TerminalRecorder {

    /**
     * 录制文件根目录（位于 data 目录下）
     */
    public static final String ROOT_DIR = "ssh_terminal_record";
    /**
     * 单会话文件上限 64MB
     */
    private static final long MAX_CAST_BYTES = 64L * 1024 * 1024;
    /**
     * 攒够多少字符就落盘
     */
    private static final int FLUSH_THRESHOLD = 8 * 1024;
    /**
     * 录制器版本
     */
    public static final int RECORD_VERSION = 1;

    private final TerminalRecordContext context;
    private final Charset charset;
    private final File file;
    private final String castFile;
    private final SshTerminalSessionService sessionService;
    private final Writer writer;
    private final Object lock = new Object();
    private final long startNanos = System.nanoTime();
    private final long startMillis = System.currentTimeMillis();
    private long pending;
    private long lastFlush = System.currentTimeMillis();
    private long bytes;
    private int commandCount;
    private volatile boolean truncated;
    private volatile boolean abnormal;
    private volatile boolean broken;
    private volatile boolean closed;

    private AsciicastTerminalRecorder(TerminalRecordContext context, Charset charset, File file, String castFile,
                                       SshTerminalSessionService sessionService, Writer writer) {
        this.context = context;
        this.charset = charset;
        this.file = file;
        this.castFile = castFile;
        this.sessionService = sessionService;
        this.writer = writer;
    }

    /**
     * 开始录制
     *
     * @param context  上下文
     * @param charset  终端编码
     * @param service  会话服务
     * @return 录制器
     * @throws IOException 创建文件失败
     */
    public static TerminalRecorder start(TerminalRecordContext context, Charset charset, SshTerminalSessionService service) throws IOException {
        File rootDir = FileUtil.file(JpomApplication.getInstance().getDataPath(), ROOT_DIR);
        return startAt(rootDir, context, charset, service);
    }

    /**
     * 在指定根目录开始录制（单元测试用；service 可为空表示只写文件不落库）
     *
     * @param rootDir 录制根目录
     * @param context 上下文
     * @param charset 编码
     * @param service 会话服务（可为空）
     * @return 录制器
     * @throws IOException 创建失败
     */
    static TerminalRecorder startAt(File rootDir, TerminalRecordContext context, Charset charset, SshTerminalSessionService service) throws IOException {
        String dateDir = DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN);
        String castFile = dateDir + StrUtil.SLASH + context.getSessionId() + ".cast";
        File file = FileUtil.file(rootDir, dateDir, context.getSessionId() + ".cast");
        FileUtil.mkParentDirs(file);
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8), 8192);
        AsciicastTerminalRecorder recorder = new AsciicastTerminalRecorder(context, charset, file, castFile, service, writer);
        try {
            recorder.writeHeader();
            // 先落会话行，保证列表能立刻看到「录制中」
            if (service != null) {
                service.open(context, castFile, RECORD_VERSION);
            }
        } catch (Exception e) {
            IoUtil.close(writer);
            FileUtil.del(file);
            throw e;
        }
        return recorder;
    }

    private void writeHeader() throws IOException {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("version", 2);
        header.put("width", Math.max(context.getCols(), 1));
        header.put("height", Math.max(context.getRows(), 1));
        header.put("timestamp", startMillis / 1000L);
        header.put("title", StrUtil.format("{}@{} {}", StrUtil.nullToEmpty(context.getUserId()), StrUtil.nullToEmpty(context.getMachineSshName()), DateUtil.formatDateTime(new Date(startMillis))));
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("SHELL", StrUtil.EMPTY);
        env.put("TERM", "xterm-256color");
        header.put("env", env);
        String line = JSON.toJSONString(header);
        writer.write(line);
        writer.write("\n");
        writer.flush();
        bytes += line.getBytes(StandardCharsets.UTF_8).length + 1;
    }

    @Override
    public void input(byte[] data) {
        this.append("i", this.decode(data));
    }

    @Override
    public void output(byte[] data) {
        this.append("o", this.decode(data));
    }

    @Override
    public void resize(int cols, int rows) {
        if (cols <= 0 || rows <= 0) {
            return;
        }
        this.append("r", cols + "x" + rows);
    }

    @Override
    public void marker(String label) {
        synchronized (lock) {
            this.commandCount++;
        }
        this.append("m", StrUtil.nullToEmpty(label));
    }

    @Override
    public String sessionId() {
        return context.getSessionId();
    }

    /**
     * 已记录的命令条数
     *
     * @return 条数
     */
    public int getCommandCount() {
        return commandCount;
    }

    /**
     * 录制文件
     *
     * @return 文件
     */
    public File getFile() {
        return file;
    }

    /**
     * 标记异常结束（连接被强杀等）
     */
    public void markAbnormal() {
        this.abnormal = true;
    }

    private String decode(byte[] data) {
        if (data == null || data.length == 0) {
            return StrUtil.EMPTY;
        }
        // new String 会对非法字节做替换，保证写入的是合法 UTF-8（与前端看到的字符一致）
        return new String(data, charset);
    }

    private void append(String code, String data) {
        if (closed || broken || StrUtil.isEmpty(data)) {
            return;
        }
        synchronized (lock) {
            if (closed || broken) {
                return;
            }
            if (truncated) {
                return;
            }
            try {
                String line = JSON.toJSONString(Arrays.asList(this.now(), code, data));
                writer.write(line);
                writer.write("\n");
                pending += line.length() + 1;
                bytes += line.getBytes(StandardCharsets.UTF_8).length + 1;
                if (bytes >= MAX_CAST_BYTES) {
                    this.truncated = true;
                    String tip = JSON.toJSONString(Arrays.asList(this.now(), "m", "truncated"));
                    writer.write(tip);
                    writer.write("\n");
                    writer.flush();
                    log.warn(I18nMessageUtil.get("i18n.ssh_terminal_session_truncated.a2ad"), file.getAbsolutePath());
                    return;
                }
                long now = System.currentTimeMillis();
                if (pending >= FLUSH_THRESHOLD || now - lastFlush >= 1000L) {
                    writer.flush();
                    pending = 0;
                    lastFlush = now;
                }
            } catch (Exception e) {
                this.broken = true;
                log.warn(I18nMessageUtil.get("i18n.ssh_terminal_session_write_fail.9a5c"), e.getMessage());
            }
        }
    }

    private double now() {
        double value = (System.nanoTime() - startNanos) / 1_000_000_000.0D;
        return Math.round(value * 1_000_000.0D) / 1_000_000.0D;
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            try {
                writer.flush();
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.ssh_terminal_session_write_fail.9a5c"), e.getMessage());
            }
            IoUtil.close(writer);
        }
        if (sessionService == null) {
            return;
        }
        int status = this.abnormal || this.broken ? SshTerminalSessionModel.STATUS_ABORTED : SshTerminalSessionModel.STATUS_CLOSED;
        try {
            long endTime = System.currentTimeMillis();
            sessionService.finish(context.getSessionId(), endTime, endTime - startMillis, file.length(), commandCount, status);
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.ssh_terminal_session_write_fail.9a5c"), e.getMessage());
        }
    }
}
