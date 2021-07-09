package io.jpom.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 命令行工具
 *
 * @author jiangzeyin
 * @date 2019/4/15
 */
public class CommandUtil {
    /**
     * 系统命令
     */
    private static final List<String> COMMAND = new ArrayList<>();
    /**
     * 文件后缀
     */
    public static final String SUFFIX;

    static {
        if (SystemUtil.getOsInfo().isLinux()) {
            //执行linux系统命令
            COMMAND.add("/bin/sh");
            COMMAND.add("-c");
        } else if (SystemUtil.getOsInfo().isMac()) {
            COMMAND.add("/bin/sh");
            COMMAND.add("-c");
        } else {
            COMMAND.add("cmd");
            COMMAND.add("/c");
        }

        if (SystemUtil.getOsInfo().isWindows()) {
            SUFFIX = "bat";
        } else {
            SUFFIX = "sh";
        }
    }

    public static List<String> getCommand() {
        return ObjectUtil.clone(COMMAND);
    }

    /*public static String execCommand(String command) {
        String newCommand = StrUtil.replace(command, StrUtil.CRLF, StrUtil.SPACE);
        newCommand = StrUtil.replace(newCommand, StrUtil.LF, StrUtil.SPACE);
        String result = "error";
        try {
            result = exec(new String[]{newCommand}, null);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("执行命令异常", e);
            result += e.getMessage();
        }
        return result;
    }*/

    public static String execSystemCommand(String command) {
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
            DefaultSystemLog.getLog().error("执行命令异常", e);
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
        DefaultSystemLog.getLog().info(Arrays.toString(cmd));
        Process process = new ProcessBuilder(cmd).directory(file).redirectErrorStream(true).start();
        return RuntimeUtil.getResult(process);
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
        DefaultSystemLog.getLog().info(newCommand);
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
