/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.exception.LogRecorderCloseException;
import org.springframework.util.Assert;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 * 日志记录
 *
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@Slf4j
public class LogRecorder implements ILogRecorder, AutoCloseable {

    private File file;
    private PrintWriter writer;

    public LogRecorder(File file, Charset charset) {
        if (file == null) {
            this.writer = null;
            this.file = null;
            return;
        }
        this.file = file;
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
        Assert.notNull(writer, "日志记录器未启用");
        return FileUtil.size(this.file);
    }
}
