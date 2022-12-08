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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import io.jpom.model.data.DslYmlDto;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.model.data.NodeScriptModel;
import io.jpom.service.script.NodeScriptServer;
import io.jpom.system.ConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.CommandUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.util.*;
import java.util.concurrent.Future;

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
    private Map<String, String> environment;

    private DslScriptBuilder(String action, Map<String, String> environment, String args, String log) {
        super(FileUtil.file(log));
        this.action = action;
        this.environment = environment;
        this.args = args;
    }

    /**
     * 添加环境变量
     *
     * @param environment 环境变量
     */
    public void putEnvironment(Map<String, String> environment) {
        if (environment == null) {
            return;
        }
        if (this.environment == null) {
            this.environment = new HashMap<>(10);
        }
        this.environment.putAll(environment);
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
        Optional.ofNullable(environment).ifPresent(map -> processBuilder.environment().putAll(map));
        processBuilder.directory(FileUtil.getParent(scriptFile, 1));
        processBuilder.redirectErrorStream(true);
        processBuilder.command(command);
        return processBuilder;
    }

    @Override
    public void run() {
        try {
            ProcessBuilder processBuilder = this.init();
            //
            process = processBuilder.start();
            inputStream = process.getInputStream();
            IoUtil.readLines(inputStream, ExtConfigBean.getInstance().getConsoleLogCharset(), (LineHandler) DslScriptBuilder.this::handle);
            //
            int waitFor = process.waitFor();
            //
            if (waitFor != 0) {
                this.handle(StrUtil.format("execute done: {}", waitFor));
            }
        } catch (Exception e) {
            log.error("执行异常", e);
            this.handle("执行异常：" + e.getMessage());
        } finally {
            this.close();
        }
    }

    @Override
    protected void handle(String line) {
        super.handle(this.formatLine(line));
    }

    private String formatLine(String line) {
        return StrUtil.format("{} [{}] - {}", DateTime.now().toString(DatePattern.NORM_DATETIME_MS_FORMAT), this.action, line);
    }

    /**
     * 执行
     */
    public List<String> syncExecute() {
        ProcessBuilder processBuilder = this.init();
        List<String> result = new ArrayList<>();
        try {
            //
            process = processBuilder.start();
            inputStream = process.getInputStream();

            IoUtil.readLines(inputStream, ExtConfigBean.getInstance().getConsoleLogCharset(), (LineHandler) line -> result.add(this.formatLine(line)));
            //
            int waitFor = process.waitFor();
            if (waitFor != 0) {
                // 插入第一行
                result.add(0, this.formatLine(StrUtil.format("execute done: {}", waitFor)));
            }
            //
            return result;
        } catch (Exception e) {
            log.error("执行异常", e);
            result.add(this.formatLine(StrUtil.format("执行异常：", e.getMessage())));
        } finally {
            this.close();
        }
        return result;
    }

    @Override
    protected void end(String msg) {

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

    /**
     * 异步执行
     *
     * @param scriptProcess 脚本流程
     * @param log           日志
     */
    public static void run(DslYmlDto.BaseProcess scriptProcess, NodeProjectInfoModel nodeProjectInfoModel, String action, String log, boolean sync) throws Exception {
        DslScriptBuilder builder = DslScriptBuilder.create(scriptProcess, nodeProjectInfoModel, action, log);
        Future<?> execute = ThreadUtil.execAsync(builder);
        if (sync) {
            execute.get();
        }
    }

    /**
     * 同步执行
     *
     * @param scriptProcess 脚本流程
     */
    public static List<String> syncRun(DslYmlDto.BaseProcess scriptProcess, NodeProjectInfoModel nodeProjectInfoModel, String action) {
        try (DslScriptBuilder builder = DslScriptBuilder.create(scriptProcess, nodeProjectInfoModel, action, null)) {
            return builder.syncExecute();
        }
    }

    private static DslScriptBuilder create(DslYmlDto.BaseProcess scriptProcess, NodeProjectInfoModel nodeProjectInfoModel, String action, String log) {
        NodeScriptServer nodeScriptServer = SpringUtil.getBean(NodeScriptServer.class);
        String scriptId = scriptProcess.getScriptId();
        NodeScriptModel item = nodeScriptServer.getItem(scriptId);
        Map<String, String> environment = DslScriptBuilder.environment(nodeProjectInfoModel);
        File scriptFile;
        boolean autoDelete = false;
        if (item == null) {
            scriptFile = FileUtil.file(nodeProjectInfoModel.allLib(), scriptId);
            Assert.state(FileUtil.isFile(scriptFile), "脚本模版不存在:" + scriptProcess.getScriptId());
        } else {
            scriptFile = DslScriptBuilder.initScriptFile(item, environment);
            autoDelete = true;
        }
        DslScriptBuilder builder = new DslScriptBuilder(action, scriptProcess.getScriptEnv(), scriptProcess.getScriptArgs(), log);
        builder.putEnvironment(environment);
        builder.setScriptFile(scriptFile);
        builder.setAutoDelete(autoDelete);
        return builder;
    }

    /**
     * 创建脚本文件
     *
     * @param scriptModel 脚本对象
     * @param dslEnv      环境变量
     * @return file
     */
    private static File initScriptFile(NodeScriptModel scriptModel, Map<String, String> dslEnv) {
        String dataPath = ConfigBean.getInstance().getDataPath();
        File scriptFile = FileUtil.file(dataPath, ConfigBean.SCRIPT_RUN_CACHE_DIRECTORY, StrUtil.format("{}.{}", IdUtil.fastSimpleUUID(), CommandUtil.SUFFIX));
        // 替换内容
        String context = scriptModel.getContext();
        for (Map.Entry<String, String> envEntry : dslEnv.entrySet()) {
            String envValue = envEntry.getValue();
            context = StrUtil.replace(context, "#{" + envEntry.getKey() + "}", envValue);
        }
        FileUtil.writeString(context, scriptFile, ExtConfigBean.getInstance().getConsoleLogCharset());
        return scriptFile;
    }

    private static Map<String, String> environment(NodeProjectInfoModel nodeProjectInfoModel) {
        Map<String, String> dslEnv = new HashMap<>(10);
        dslEnv.put("PROJECT_ID", nodeProjectInfoModel.getId());
        dslEnv.put("PROJECT_NAME", nodeProjectInfoModel.getName());
        dslEnv.put("PROJECT_PATH", nodeProjectInfoModel.allLib());
        return dslEnv;
    }
}
