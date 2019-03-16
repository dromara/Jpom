import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class DTest {


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        String s1 = execSystemCommand("netstat -antp |grep  32936 | head -1 ");
        System.out.println(s1);
        long l = System.currentTimeMillis();
        System.out.println(l-start);

    }

    private static String execSystemCommand(String command) {
        String result = "error";
        try {
            //执行linux系统命令
            String[] cmd = new String[]{"/bin/sh", "-c", command};
            result = exec(cmd);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("执行命令异常", e);
            result += e.getMessage();
        }
        return result;
    }

    private static String exec(String[] cmd) throws IOException, InterruptedException {
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
        result = IoUtil.read(is, CharsetUtil.UTF_8);
        is.close();
        process.destroy();
        if (StrUtil.isEmpty(result) && wait != 0) {
            result = "没有返回任何执行信息";
        }
        return result;
    }
}
