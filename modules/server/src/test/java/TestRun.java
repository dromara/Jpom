import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;

import java.io.*;

public class TestRun {
    public static void main(String[] args) throws IOException, InterruptedException {
        testProcessBuilder("javaw  -jar D:\\sdfsdf\\test-jar\\springboot-test-jar-0.0.1-SNAPSHOT.jar -Dapplication=test -Dbasedir=D:\\sdfsdf\\test-jar   >> D:\\sdfsdf\\test.log");

    }

    public static void testProcessBuilder(String command) {
        boolean err = false;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            //初始化ProcessBuilder对象
            Process p = processBuilder.start();
            //用于存储执行命令的结果
            BufferedReader results = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String s;
            while ((s = results.readLine()) != null) {
                System.out.println(s);
            }
            //用于存储执行命令的错误信息
            BufferedReader errors = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((s = errors.readLine()) != null) {
                System.err.println(s);
                err = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (err) {
            throw new RuntimeException("Error executing " + command);
        }
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
