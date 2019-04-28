package cn.keepbx.jpom.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.BaseJpomApplication;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 命令行工具
 *
 * @author jiangzeyin
 * @date 2019/4/15
 */
public class CommandUtil {

    public static String execCommand(String command) {
        String result = "error";
        try {
            result = exec(new String[]{command});
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("执行命令异常", e);
            result += e.getMessage();
        }
        return result;
    }

    public static String execSystemCommand(String command) {
        String result = "error";
        try {
            String[] cmd;
            if (BaseJpomApplication.OS_INFO.isLinux()) {
                //执行linux系统命令
                cmd = new String[]{"/bin/sh", "-c", command};
            } else if (BaseJpomApplication.OS_INFO.isMac()) {
                cmd = new String[]{"/bin/sh", "-c", command};
            } else {
                cmd = new String[]{"cmd", "/c", command};
            }
            result = exec(cmd);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("执行命令异常", e);
            result += e.getMessage();
        }
        return result;
    }

    /**
     * 执行命令
     *
     * @param cmd 命令行
     * @return 结果
     * @throws IOException          IO
     * @throws InterruptedException 等待超时
     */
    private static String exec(String[] cmd) throws IOException, InterruptedException {
        DefaultSystemLog.LOG().info(Arrays.toString(cmd));
        String result;
        Process process;
        if (cmd.length == 1) {
            process = Runtime.getRuntime().exec(cmd[0]);
        } else {
            process = Runtime.getRuntime().exec(cmd);
        }
        InputStream is;
        int wait = process.waitFor();
        if (wait == 0) {
            is = process.getInputStream();
        } else {
            is = process.getErrorStream();
        }
        result = IoUtil.read(is, BaseJpomApplication.getCharset());
        is.close();
        process.destroy();
        if (StrUtil.isEmpty(result) && wait != 0) {
            result = "没有返回任何执行信息:" + wait;
        }
        return result;
    }
}
