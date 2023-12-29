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
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.IllegalArgument2Exception;
import org.dromara.jpom.configuration.ProjectLogConfig;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.model.data.NodeScriptModel;
import org.dromara.jpom.service.script.NodeScriptServer;
import org.dromara.jpom.service.system.AgentWorkspaceEnvVarService;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.FileUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private EnvironmentMapBuilder environmentMapBuilder;

    private DslScriptBuilder(String action,
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
    public static Tuple syncRun(DslYmlDto.BaseProcess scriptProcess, NodeProjectInfoModel nodeProjectInfoModel, String action) {
        try (DslScriptBuilder builder = DslScriptBuilder.create(scriptProcess, nodeProjectInfoModel, action, null)) {
            return builder.syncExecute();
        }
    }

    /**
     * 构建 DSL 执行器
     *
     * @param scriptProcess        脚本流程
     * @param nodeProjectInfoModel 项目
     * @param log                  日志路径
     * @param action               具体操作
     */
    private static DslScriptBuilder create(DslYmlDto.BaseProcess scriptProcess, NodeProjectInfoModel nodeProjectInfoModel, String action, String log) {
        NodeScriptServer nodeScriptServer = SpringUtil.getBean(NodeScriptServer.class);
        AgentConfig agentConfig = SpringUtil.getBean(AgentConfig.class);
        ProjectLogConfig logConfig = agentConfig.getProject().getLog();
        String scriptId = scriptProcess.getScriptId();
        cn.hutool.core.lang.Assert.notBlank(scriptId, () -> new IllegalArgument2Exception("请填写脚本模板id"));

        NodeScriptModel item = nodeScriptServer.getItem(scriptId);
        EnvironmentMapBuilder environment = DslScriptBuilder.environment(nodeProjectInfoModel, scriptProcess);
        File scriptFile;
        boolean autoDelete = false;
        if (item == null) {
            scriptFile = FileUtil.file(nodeProjectInfoModel.allLib(), scriptId);
            cn.hutool.core.lang.Assert.isTrue(FileUtil.isFile(scriptFile), () -> new IllegalArgument2Exception("脚本模版不存在:" + scriptProcess.getScriptId()));
        } else {
            scriptFile = DslScriptBuilder.initScriptFile(item);
            // 系统生成的脚本需要自动删除
            autoDelete = true;
        }
        DslScriptBuilder builder = new DslScriptBuilder(action, environment, scriptProcess.getScriptArgs(), log, logConfig.getFileCharset());
        builder.setScriptFile(scriptFile);
        builder.setAutoDelete(autoDelete);
        return builder;
    }

    /**
     * 创建脚本文件
     *
     * @param scriptModel 脚本对象
     * @return file
     */
    private static File initScriptFile(NodeScriptModel scriptModel) {
        String dataPath = JpomApplication.getInstance().getDataPath();
        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, StrUtil.format("{}.{}", IdUtil.fastSimpleUUID(), CommandUtil.SUFFIX));
        // 替换内容
        String context = scriptModel.getContext();
        FileUtils.writeScript(context, scriptFile, ExtConfigBean.getConsoleLogCharset());
        return scriptFile;
    }

    private static EnvironmentMapBuilder environment(NodeProjectInfoModel nodeProjectInfoModel, DslYmlDto.BaseProcess scriptProcess) {
        //
        AgentWorkspaceEnvVarService workspaceService = SpringUtil.getBean(AgentWorkspaceEnvVarService.class);
        EnvironmentMapBuilder environmentMapBuilder = workspaceService.getEnv(nodeProjectInfoModel.getWorkspaceId());
        // 项目配置的环境变量
        String dslEnv = nodeProjectInfoModel.getDslEnv();
        Opt.ofBlankAble(dslEnv)
            .map(s -> UrlQuery.of(s, CharsetUtil.CHARSET_UTF_8))
            .map(UrlQuery::getQueryMap)
            .map(map -> {
                Map<String, String> map1 = MapUtil.newHashMap();
                for (Map.Entry<CharSequence, CharSequence> entry : map.entrySet()) {
                    map1.put(StrUtil.toString(entry.getKey()), StrUtil.toString(entry.getValue()));
                }
                return map1;
            })
            .ifPresent(environmentMapBuilder::putStr);
        //
        environmentMapBuilder
            .putStr(scriptProcess.getScriptEnv())
            .put("PROJECT_ID", nodeProjectInfoModel.getId())
            .put("PROJECT_NAME", nodeProjectInfoModel.getName())
            .put("PROJECT_PATH", nodeProjectInfoModel.allLib());
        return environmentMapBuilder;
    }
}
