import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TestRun {
    public static void main(String[] args) throws IOException, InterruptedException {
        String[] c = new String[]{"/jpom/command/run_boot.sh restart test no cn.keepbx.jpom.JpomApplication /jpom/p/jpom /jpom/p/test.log [][--server.port=2024  --jpom.path=/jpom/p/jpom/]"};

        System.out.println(exec(c));
    }

    private static String exec(String[] cmd) throws IOException, InterruptedException {
//        DefaultSystemLog.LOG().info(Arrays.toString(cmd));
        String result;
        Process process;
        process = Runtime.getRuntime().exec(cmd[0]);
        InputStream is;

        process.waitFor();
        OutputStream outputStream = process.getOutputStream();

        outputStream.write(1);

        is = process.getInputStream();
        result = IoUtil.read(is, CharsetUtil.CHARSET_UTF_8);
        if (StrUtil.isEmpty(result)) {
            System.out.println("321");
            is = process.getErrorStream();
            result = IoUtil.read(is, CharsetUtil.CHARSET_UTF_8);
        }

        is.close();
        process.destroy();
        if (StrUtil.isEmpty(result)) {
            result = "没有返回任何执行信息";
        }
        return result;
    }


}
