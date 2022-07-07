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
package io.jpom.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import io.jpom.system.ExtConfigBean;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令行工具
 *
 * @author jiangzeyin
 * @since 2019/4/15
 */
@Slf4j
public class CommandUtil {
    /**
     * 系统命令
     */
    private static final List<String> COMMAND = new ArrayList<>();
    /**
     * 文件后缀
     */
    public static final String SUFFIX;
    /**
     * 执行前缀
     */
    private static final String EXECUTE_PREFIX;
    /**
     * 是否缓存执行结果
     */
    private static final ThreadLocal<Boolean> CACHE_COMMAND_RESULT_TAG = new ThreadLocal<>();
    /**
     * 缓存执行结果
     */
    private static final ThreadLocal<Map<String, String>> CACHE_COMMAND_RESULT = new ThreadLocal<>();

    static {
        if (SystemUtil.getOsInfo().isLinux()) {
            //执行linux系统命令
            COMMAND.add("/bin/bash");
            COMMAND.add("-c");
        } else if (SystemUtil.getOsInfo().isMac()) {
            COMMAND.add("/bin/bash");
            COMMAND.add("-c");
        } else {
            COMMAND.add("cmd");
            COMMAND.add("/c");
        }
        //
        if (SystemUtil.getOsInfo().isWindows()) {
            SUFFIX = "bat";
            EXECUTE_PREFIX = StrUtil.EMPTY;
        } else {
            SUFFIX = "sh";
            EXECUTE_PREFIX = "bash";
        }
    }

    /**
     * 填充执行命令的前缀
     *
     * @param command 命令
     */
    public static void paddingPrefix(List<String> command) {
        if (EXECUTE_PREFIX.isEmpty()) {
            return;
        }
        command.add(0, CommandUtil.EXECUTE_PREFIX);
    }

    public static String generateCommand(File file, String args) {
        String path = FileUtil.getAbsolutePath(file);
        return generateCommand(path, args);
    }

    public static String generateCommand(String file, String args) {
        return StrUtil.format("{} {} {}", CommandUtil.EXECUTE_PREFIX, file, args);
        //String command = CommandUtil.EXECUTE_PREFIX + StrUtil.SPACE + FileUtil.getAbsolutePath(scriptFile) + " restart upgrade";
    }

    /**
     * 开启缓存执行结果
     */
    public static void openCache() {
        CACHE_COMMAND_RESULT_TAG.set(true);
        CACHE_COMMAND_RESULT.set(new ConcurrentHashMap<>(16));
    }

    /**
     * 关闭缓存执行结果
     */
    public static void closeCache() {
        CACHE_COMMAND_RESULT_TAG.remove();
        CACHE_COMMAND_RESULT.remove();
    }

    /**
     * 获取执行命令的 前缀
     *
     * @return list
     */
    public static List<String> getCommand() {
        return ObjectUtil.clone(COMMAND);
    }

    /**
     * 执行命令
     *
     * @param command 命令
     * @return 结果
     */
    public static String execSystemCommand(String command) {
        Boolean cache = CACHE_COMMAND_RESULT_TAG.get();
        if (cache != null && cache) {
            // 开启缓存
            Map<String, String> cacheMap = CACHE_COMMAND_RESULT.get();
            return cacheMap.computeIfAbsent(command, key -> execSystemCommand(key, null));
        }
        // 直接执行
        return execSystemCommand(command, null);
    }

    /**
     * 在指定文件夹下执行命令
     *
     * @param command 命令
     * @param file    文件夹
     * @return msg
     */
    public static String execSystemCommand(String command, File file) {
        String newCommand = StrUtil.replace(command, StrUtil.CRLF, StrUtil.SPACE);
        newCommand = StrUtil.replace(newCommand, StrUtil.LF, StrUtil.SPACE);
        String result = "error";
        try {
            List<String> commands = getCommand();
            commands.add(newCommand);
            String[] cmd = commands.toArray(new String[]{});
            result = exec(cmd, file);
        } catch (Exception e) {
            log.error("执行命令异常", e);
            result += e.getMessage();
        }
        return result;
    }

    /**
     * 执行命令
     *
     * @param cmd 命令行
     * @return 结果
     * @throws IOException IO
     */
    private static String exec(String[] cmd, File file) throws IOException {
        Process process = new ProcessBuilder(cmd).directory(file).redirectErrorStream(true).start();
        Charset charset;
        boolean isLog;
        try {
            charset = ExtConfigBean.getInstance().getConsoleLogCharset();
            isLog = true;
        } catch (Exception e) {
            // 直接执行，使用默认编码格式
            charset = CharsetUtil.systemCharset();
            // 不记录日志
            isLog = false;
        }
        charset = ObjectUtil.defaultIfNull(charset, CharsetUtil.defaultCharset());
        String result = RuntimeUtil.getResult(process, charset);
        if (isLog) {
            log.debug("exec {} {} {}", charset.name(), Arrays.toString(cmd), result);
        }
        return result;
    }

    /**
     * 异步执行命令
     *
     * @param file    文件夹
     * @param command 命令
     * @throws IOException 异常
     */
    public static void asyncExeLocalCommand(File file, String command) throws Exception {
        String newCommand = StrUtil.replace(command, StrUtil.CRLF, StrUtil.SPACE);
        newCommand = StrUtil.replace(newCommand, StrUtil.LF, StrUtil.SPACE);
        //
        log.debug(newCommand);
        List<String> commands = getCommand();
        commands.add(newCommand);
        ProcessBuilder pb = new ProcessBuilder(commands);
        if (file != null) {
            pb.directory(file);
        }
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb.start();
    }

    /**
     * 判断是否包含删除命令
     *
     * @param script 命令行
     * @return true 包含
     */
    public static boolean checkContainsDel(String script) {
        // 判断删除
        String[] commands = StrUtil.splitToArray(script, StrUtil.LF);
        for (String commandItem : commands) {
            if (checkContainsDelItem(commandItem)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行系统命令 快速删除.
     * 执行删除后再检查文件是否存在
     *
     * @param file 文件或者文件夹
     * @return true 文件还存在
     */
    public static boolean systemFastDel(File file) {
        String path = FileUtil.getAbsolutePath(file);
        String command;
        if (SystemUtil.getOsInfo().isWindows()) {
            // Windows
            command = StrUtil.format("rd /s/q \"{}\"", path);
        } else {
            // Linux MacOS
            command = StrUtil.format("rm -rf '{}'", path);
        }
        CommandUtil.execSystemCommand(command);
        // 再次尝试
        boolean del = FileUtil.del(file);
        if (!del) {
            FileUtil.del(file.toPath());
        }
        return FileUtil.exist(file);
    }

    private static boolean checkContainsDelItem(String script) {
        String[] split = StrUtil.splitToArray(script, StrUtil.SPACE);
        if (SystemUtil.getOsInfo().isWindows()) {
            for (String s : split) {
                if (StrUtil.startWithAny(s, "rd", "del")) {
                    return true;
                }
                if (StrUtil.containsAnyIgnoreCase(s, " rd", " del")) {
                    return true;
                }
            }
        } else {
            for (String s : split) {
                if (StrUtil.startWithAny(s, "rm", "\\rm")) {
                    return true;
                }
                if (StrUtil.containsAnyIgnoreCase(s, " rm", " \\rm", "&rm", "&\\rm")) {
                    return true;
                }
            }
        }
        return false;
    }
}
