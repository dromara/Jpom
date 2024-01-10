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
package org.dromara.jpom.script;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.keepbx.jpom.log.ILogRecorder;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.LogRecorder;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Optional;

/**
 * 脚本模版执行父类
 *
 * @author bwcx_jzy
 * @since 2022/1/15
 */
public abstract class BaseRunScript implements AutoCloseable, ILogRecorder {

    /**
     * 日志文件
     */
    protected final LogRecorder logRecorder;
    protected final File logFile;
    protected Process process;
    protected InputStream inputStream;

    protected BaseRunScript(File logFile, Charset charset) {
        if (logFile == null) {
            this.logFile = null;
            this.logRecorder = null;
        } else {
            this.logFile = logFile;
            this.logRecorder = LogRecorder.builder().file(logFile).charset(charset).build();
        }
    }

    @Override
    public String info(String info, Object... vals) {
        String msg = logRecorder.info(info, vals);
        this.msgCallback(msg);
        return msg;
    }

    @Override
    public String system(String info, Object... vals) {
        String msg = logRecorder.system(info, vals);
        this.msgCallback(msg);
        return msg;
    }

    @Override
    public String systemError(String info, Object... vals) {
        String msg = logRecorder.systemError(info, vals);
        this.msgCallback(msg);
        return msg;
    }

    @Override
    public String systemWarning(String info, Object... vals) {
        String msg = logRecorder.systemWarning(info, vals);
        this.msgCallback(msg);
        return msg;
    }

    /**
     * 输出消息后的回调
     *
     * @param msg 消息
     */
    protected abstract void msgCallback(String msg);

    /**
     * 结束执行
     *
     * @param msg 异常方法
     */
    protected abstract void end(String msg);

    @Override
    public void close() {
        // windows 中不能正常关闭
        IoUtil.close(inputStream);
        CommandUtil.kill(process);
        IoUtil.close(logRecorder);
    }

    /**
     * 清理 脚本文件执行缓存
     */
    public static void clearRunScript() {
        String dataPath = JpomApplication.getInstance().getDataPath();
        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY);
        if (!FileUtil.isDirectory(scriptFile)) {
            return;
        }
        File[] files = scriptFile.listFiles(pathname -> {
            Date lastModifiedTime = FileUtil.lastModifiedTime(pathname);
            DateTime now = DateTime.now();
            long between = DateUtil.between(lastModifiedTime, now, DateUnit.HOUR);
            // 文件大于一个小时才能被删除
            return between > 1;
        });
        Optional.ofNullable(files).ifPresent(files1 -> {
            for (File file : files1) {
                try {
                    FileUtil.del(file);
                } catch (Exception ignored) {
                }
            }
        });
    }
}
