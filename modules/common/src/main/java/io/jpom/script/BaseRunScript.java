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
package io.jpom.script;

import cn.hutool.core.io.IoUtil;
import io.jpom.util.LogRecorder;

import java.io.File;
import java.io.InputStream;

/**
 * 脚本模版执行父类
 *
 * @author bwcx_jzy
 * @since 2022/1/15
 */
public abstract class BaseRunScript implements AutoCloseable {

    /**
     * 日志文件
     */
    protected final LogRecorder logRecorder;
    protected final File logFile;
    protected Process process;
    protected InputStream inputStream;

    protected BaseRunScript(File logFile) {
        this.logFile = logFile;
        this.logRecorder = LogRecorder.builder().file(logFile).build();
    }

    /**
     * 响应
     *
     * @param line 信息
     */
    protected void handle(String line) {
        logRecorder.info(line);
    }

    /**
     * 结束执行
     *
     * @param msg 异常方法
     */
    protected abstract void end(String msg);

    @Override
    public void close() {
        if (this.process != null) {
            // windows 中不能正常关闭
            IoUtil.close(inputStream);
            this.process.destroy();
        }
    }
}
