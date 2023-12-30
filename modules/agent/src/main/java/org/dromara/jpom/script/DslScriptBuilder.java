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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.CommandUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * dsl 执行脚本
 *
 * @author bwcx_jzy
 * @since 2022/1/15
 */
@Setter
@Slf4j
public class DslScriptBuilder extends BaseRunScript implements Runnable {


    private final String args;
    private String action;
    private File scriptFile;
    private boolean autoDelete;
    private EnvironmentMapBuilder environmentMapBuilder;

    public DslScriptBuilder(String action,
                            EnvironmentMapBuilder environmentMapBuilder,
                            String args,
                            String log,
                            Charset charset) {
        super(FileUtil.file(log), charset);
        this.action = action;
        this.environmentMapBuilder = environmentMapBuilder;
        this.args = args;
    }

    /**
     * 初始化
     */
    private ProcessBuilder init() {
        //
        String script = FileUtil.getAbsolutePath(scriptFile);
        ProcessBuilder processBuilder = new ProcessBuilder();
        List<String> command = StrUtil.splitTrim(args, StrUtil.SPACE);
        command.add(0, script);
        CommandUtil.paddingPrefix(command);
        log.debug(CollUtil.join(command, StrUtil.SPACE));
        processBuilder
            .environment()
            .putAll(environmentMapBuilder.environment());
        processBuilder.directory(FileUtil.getParent(scriptFile, 1));
        processBuilder.redirectErrorStream(true);
        processBuilder.command(command);
        return processBuilder;
    }

    @Override
    public void run() {
        try {
            ProcessBuilder processBuilder = this.init();
            environmentMapBuilder.eachStr(this::info);
            //
            this.system("开始执行: {}", this.action);
            process = processBuilder.start();
            inputStream = process.getInputStream();
            IoUtil.readLines(inputStream, ExtConfigBean.getConsoleLogCharset(), (LineHandler) line -> {
                String formatLine = formatLine(line);
                this.info(formatLine);
            });
            //
            int waitFor = process.waitFor();
            //
            this.system("执行结束: {} {}", this.action, waitFor);
        } catch (Exception e) {
            log.error("执行异常", e);
            this.systemError("执行异常：" + e.getMessage());
        } finally {
            this.close();
        }
    }


    private String formatLine(String line) {
        return StrUtil.format("{} [{}] - {}", DateTime.now().toString(DatePattern.NORM_DATETIME_MS_FORMAT), this.action, line);
    }

    /**
     * 执行
     * <p>
     * 0 退出码
     * 1 日志
     */
    public Tuple syncExecute() {
        ProcessBuilder processBuilder = this.init();
        List<String> result = new ArrayList<>();
        int waitFor = -100;
        try {
            //
            process = processBuilder.start();
            inputStream = process.getInputStream();

            IoUtil.readLines(inputStream, ExtConfigBean.getConsoleLogCharset(), (LineHandler) line -> result.add(this.formatLine(line)));
            //
            waitFor = process.waitFor();
            // 插入第一行
            result.add(0, this.formatLine(StrUtil.format("执行结束: {}", waitFor)));
            //
        } catch (Exception e) {
            log.error("执行异常", e);
            result.add(this.formatLine(StrUtil.format("执行异常：", e.getMessage())));
        } finally {
            this.close();
        }
        return new Tuple(waitFor, result);
    }

    @Override
    protected void end(String msg) {

    }

    @Override
    protected void msgCallback(String msg) {

    }

    @Override
    public void close() {
        super.close();
        //
        if (autoDelete) {
            try {
                FileUtil.del(this.scriptFile);
            } catch (Exception ignored) {
            }
        }
    }


}
