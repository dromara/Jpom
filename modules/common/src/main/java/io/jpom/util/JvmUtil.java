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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.JpomManifest;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * jvm jmx 工具
 *
 * @author jiangzeyin
 * @since 2019/4/13
 */
public class JvmUtil {

    /**
     * 状态服务器 jps 命令执行是否正常
     */
    public static boolean jpsNormal = false;

    /**
     * Jps 异常消息回调
     */
    public final static Supplier<String> JPS_ERROR_MSG = () -> {
        checkJpsNormal();
        return "当前服务器 jps 命令异常,请检查 jdk 是否完整,以及 java 环境变量是否配置正确";
    };

    /**
     * 主持的标签数组
     */
    private static final String[] JPOM_PID_TAG = new String[]{"DJpom.application", "Jpom.application", "Dapplication"};

    /**
     * 检查 jps 命令是否正常
     */
    public static void checkJpsNormal() {
        //        JvmUtil.setJpsNormal(exist);
        JvmUtil.jpsNormal = JvmUtil.exist(JpomManifest.getInstance().getPid());
    }

    /**
     * 获取进程标识
     *
     * @param id   i
     * @param path 路径
     * @return str
     */
    public static String getJpomPidTag(String id, String path) {
        return String.format("-%s=%s -DJpom.basedir=%s", JPOM_PID_TAG[0], id, path);
    }


    /**
     * 获取当前系统运行的java 程序个数
     *
     * @return 如果发生异常则返回0
     */
    public static int getJavaVirtualCount() {
        String execSystemCommand = CommandUtil.execSystemCommand("jps -l");
        List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
        return Math.max(CollUtil.size(list) - 1, 0);
    }

    /**
     * 执行 jps 判断是否存在 对应的进程
     *
     * @return true 存在
     */
    public static boolean exist(long pid) {
        String execSystemCommand = CommandUtil.execSystemCommand("jps -l");
        List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
        String pidCommandInfo = list.stream().filter(s -> {
            List<String> split = StrSplitter.splitTrim(s, StrUtil.SPACE, true);
            return StrUtil.equals(pid + "", CollUtil.getFirst(split));
        }).findAny().orElse(null);
        return StrUtil.isNotEmpty(pidCommandInfo);
    }

    /**
     * 根据pid 获取jvm
     *
     * @param pid 进程id
     * @return command line info
     */
    public static String getPidJpsInfoInfo(int pid) {
        String execSystemCommand = CommandUtil.execSystemCommand("jps -mv");
        List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
        Optional<String> any = list.stream().filter(s -> {
            List<String> split = StrSplitter.splitTrim(s, StrUtil.SPACE, true);
            return StrUtil.equals(pid + "", CollUtil.getFirst(split));
        }).findAny();
        return any.orElse(null);
    }

    /**
     * 工具Jpom运行项目的id 获取进程ID
     *
     * @param tag 项目id
     * @return 进程ID
     */
    public static Integer getPidByTag(String tag) {
        String execSystemCommand = CommandUtil.execSystemCommand("jps -mv");
        List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
        Optional<String> any = list.stream().filter(s -> checkCommandLineIsJpom(s, tag)).map(s -> {
            List<String> split = StrUtil.split(s, StrUtil.SPACE);
            return CollUtil.getFirst(split);
        }).findAny();
        return any.map(Convert::toInt).orElse(null);
    }

    /**
     * 判断命令行是否为jpom 标识
     *
     * @param commandLine 命令行
     * @param tag         标识
     * @return true
     */
    public static boolean checkCommandLineIsJpom(String commandLine, String tag) {
        if (StrUtil.isEmpty(commandLine)) {
            return false;
        }
        String[] split = StrUtil.splitToArray(commandLine, StrUtil.SPACE);
        String[] tags = Arrays.stream(JPOM_PID_TAG)
            .map(s -> String.format("-%s=%s", s, tag))
            .collect(Collectors.toList())
            .toArray(new String[]{});
        for (String item : split) {
            if (StrUtil.equalsAnyIgnoreCase(item, tags)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析命令行的 tag 信息
     *
     * @param commandLine 命令行
     * @return tag         标识
     */
    public static String parseCommandJpomTag(String commandLine) {
        if (StrUtil.isEmpty(commandLine)) {
            return null;
        }
        String[] split = StrUtil.splitToArray(commandLine, StrUtil.SPACE);
        String[] tags = Arrays.stream(JPOM_PID_TAG)
            .map(s -> String.format("-%s=", s))
            .collect(Collectors.toList())
            .toArray(new String[]{});
        for (String item : split) {
            if (StrUtil.startWithAny(item, tags)) {
                List<String> split1 = StrUtil.split(item, "=");
                return CollUtil.get(split1, 1);
            }
        }
        return null;
    }

    /**
     * 工具指定的 mainClass 获取对应所有的的  MonitoredVm对象
     *
     * @param mainClass 程序运行主类
     * @return pid
     */
    public static Integer findMainClassPid(String mainClass) {
        String execSystemCommand = CommandUtil.execSystemCommand("jps -l");
        List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
        Optional<Tuple> any = list.stream().map(s -> {
            List<String> split = StrUtil.split(s, StrUtil.SPACE);
            return new Tuple(CollUtil.getFirst(split), CollUtil.getLast(split));
        }).filter(tuple -> {
            String fileName = tuple.get(1);
            return StrUtil.equals(mainClass, fileName) || checkFile(fileName, mainClass);
        }).findAny();
        return any.map(tuple -> Convert.toInt(tuple.get(0))).orElse(null);
    }

    private static boolean checkFile(String fileName, String mainClass) {
        try {
            File file = FileUtil.file(fileName);
            if (!file.exists() || file.isDirectory()) {
                return false;
            }
            try (JarFile jarFile1 = new JarFile(file)) {
                Manifest manifest = jarFile1.getManifest();
                Attributes attributes = manifest.getMainAttributes();
                String jarMainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
                String jarStartClass = attributes.getValue("Start-Class");
                return StrUtil.equals(mainClass, jarMainClass) || StrUtil.equals(mainClass, jarStartClass);
            }
        } catch (Exception e) {
            return false;
        }
    }
}
