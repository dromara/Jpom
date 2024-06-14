/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.log.ILogRecorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.exception.LogRecorderCloseException;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 * 日志记录
 *
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@Slf4j
@Getter
public class LogRecorder extends OutputStream implements ILogRecorder, AutoCloseable {

    private File file;
    private PrintWriter writer;
    private final Charset charset;

    private LogRecorder(File file, Charset charset) {
        if (file == null) {
            this.writer = null;
            this.file = null;
            this.charset = charset;
            return;
        }
        this.file = file;
        this.charset = charset;
        this.writer = FileWriter.create(file, charset).getPrintWriter(true);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private File file;
        private Charset charset;

        Builder() {
        }

        public Builder file(final File file) {
            this.file = file;
            return this;
        }

        public Builder charset(final Charset charset) {
            this.charset = charset;
            return this;
        }

        public LogRecorder build() {
            Charset charset1 = ObjectUtil.defaultIfNull(this.charset, CharsetUtil.CHARSET_UTF_8);
            return new LogRecorder(this.file, charset1);
        }

        public String toString() {
            return "LogRecorder.LogRecorderBuilder(file=" + this.file + ", charset=" + this.charset + ")";
        }
    }


    /**
     * 记录错误信息
     *
     * @param title     错误描述
     * @param throwable 堆栈信息
     */
    public void error(String title, Throwable throwable) {
        log.error(title, throwable);
        if (writer == null) {
            throw new LogRecorderCloseException();
        }
        writer.println(title);
        String s = ExceptionUtil.stacktraceToString(throwable);
        writer.println(s);
        writer.flush();
    }

    /**
     * 记录单行日志
     *
     * @param info 日志
     */
    public String info(String info, Object... vals) {
        if (writer == null) {
            throw new LogRecorderCloseException();
        }
        String format = StrUtil.format(info, vals);
        writer.println(format);
        writer.flush();
        return format;
    }

    /**
     * 记录单行日志
     *
     * @param info 日志
     */
    public String system(String info, Object... vals) {
        return this.info("[SYSTEM-INFO] " + info, vals);
    }

    /**
     * 记录单行日志
     *
     * @param info 日志
     */
    public String systemError(String info, Object... vals) {
        return this.info("[SYSTEM-ERROR] " + info, vals);
    }

    /**
     * 记录单行日志
     *
     * @param info 日志
     */
    public String systemWarning(String info, Object... vals) {
        return this.info("[SYSTEM-WARNING] " + info, vals);
    }

    /**
     * 记录单行日志 (不还行)
     *
     * @param info 日志
     */
    public void append(String info, Object... vals) {
        if (writer == null) {
            throw new LogRecorderCloseException();
        }
        writer.append(StrUtil.format(info, vals));
        writer.flush();

    }

    /**
     * 获取 文件输出流
     *
     * @return Writer
     */
    public PrintWriter getPrintWriter() {
        return writer;
    }

    @Override
    public void close() {
        IoUtil.close(writer);
        this.writer = null;
        this.file = null;
    }

    public long size() {
        Assert.notNull(writer, I18nMessageUtil.get("i18n.log_recorder_not_enabled.5a4e"));
        return FileUtil.size(this.file);
    }

    @Override
    public void write(int b) throws IOException {
        if (writer == null) {
            throw new LogRecorderCloseException();
        }
        writer.write((byte) b);
    }
}
