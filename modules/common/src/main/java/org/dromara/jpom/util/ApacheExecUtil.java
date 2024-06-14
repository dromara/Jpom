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

import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.*;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.system.ExtConfigBean;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 24/1/17 017
 */
@Slf4j
public class ApacheExecUtil {

    private static final ShutdownHookProcessDestroyer shutdownHookProcessDestroyer = new ShutdownHookProcessDestroyer();
    private static final Map<String, Process> processMap = new SafeConcurrentHashMap<>();

    public static void addProcess(Process process) {
        shutdownHookProcessDestroyer.add(process);
    }

    /**
     * 关闭 Process
     *
     * @param execId 执行Id
     */
    public static void kill(String execId) {
        Process process = processMap.remove(execId);
        if (process == null) {
            return;
        }
        CommandUtil.kill(process);
    }

    /**
     * 执行脚本
     *
     * @param scriptFile  脚本文件
     * @param baseDir     基础目录
     * @param env         环境变量
     * @param args        参数
     * @param logRecorder 日志记录
     * @return 退出码
     * @throws IOException io
     */
    public static int exec(String execId, File scriptFile, File baseDir, Map<String, String> env, String args, LogRecorder logRecorder) throws IOException {
        List<String> build = CommandUtil.build(scriptFile, args);
        String join = String.join(StrUtil.SPACE, build);
        CommandLine commandLine = CommandLine.parse(join);
        log.debug(join);
        Charset charset;
        try {
            charset = ExtConfigBean.getConsoleLogCharset();
        } catch (Exception e) {
            // 直接执行，使用默认编码格式
            charset = CharsetUtil.systemCharset();
        }
        Map<String, String> procEnvironment = EnvironmentUtils.getProcEnvironment();
        procEnvironment.putAll(env);
        final LogOutputStream logOutputStream = new LogOutputStream(1, charset) {
            @Override
            protected void processLine(String line, int logLevel) {
                logRecorder.info(line);
            }
        };
        // 重定向stdout和stderr到文件
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(logOutputStream, logOutputStream);

        // 创建执行器
        DefaultExecutor executor = DefaultExecutor.builder()
            .setExecuteStreamHandler(pumpStreamHandler)
            .setWorkingDirectory(baseDir)
            .get();
        //
        executor.setProcessDestroyer(new ProcessDestroyer() {
            private int size = 0;

            @Override
            public boolean add(Process process) {
                processMap.put(execId, process);
                size++;
                return shutdownHookProcessDestroyer.add(process);
            }

            @Override
            public boolean remove(Process process) {
                processMap.remove(execId);
                size--;
                return shutdownHookProcessDestroyer.remove(process);
            }

            @Override
            public int size() {
                return size;
            }
        });
        pumpStreamHandler.stop();
        // 执行，打印退出码
        try {
            return executor.execute(commandLine, procEnvironment);
        } catch (ExecuteException executeException) {
            logRecorder.systemWarning(I18nMessageUtil.get("i18n.execution_exception_with_detail.c142"), executeException.getMessage());
            return executeException.getExitValue();
        }
    }
}
