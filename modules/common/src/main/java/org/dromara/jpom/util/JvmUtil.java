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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.i18n.I18nMessageUtil;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * jvm jmx 工具
 *
 * @author bwcx_jzy
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
        return I18nMessageUtil.get("i18n.server_jps_command_exception.e380");
    };

    /**
     * 支持的标签数组
     */
    private static final String[] JPOM_PID_TAG = new String[]{"DJpom.application", "Jpom.application"};

    /**
     * 检查 jps 命令是否正常
     */
    public static void checkJpsNormal() {
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
        String pidCommandInfo = list.stream()
            .filter(s -> {
                List<String> split = StrSplitter.splitTrim(s, StrUtil.SPACE, true);
                return StrUtil.equals(pid + "", CollUtil.getFirst(split));
            })
            .findAny()
            .orElse(null);
        return StrUtil.isNotEmpty(pidCommandInfo);
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
        return list.stream()
            .filter(s -> checkCommandLineIsJpom(s, tag))
            .map(s -> {
                List<String> split = StrUtil.split(s, StrUtil.SPACE);
                return CollUtil.getFirst(split);
            })
            .findAny()
            .map(Convert::toInt)
            .orElse(null);
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
}
