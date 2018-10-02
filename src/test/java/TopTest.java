import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.service.manage.CommandService;

import java.io.IOException;
import java.io.InputStream;

public class TopTest {
    public static void main(String[] args) {
        String result = execCommand("top -b -n 1 ");
        System.out.println(result);
    }

    private static String execCommand(String command) {
        String result = "error";
        try {
            Process process = Runtime.getRuntime().exec(command);
            InputStream is;
            int wait = process.waitFor();
            if (wait == 0) {
                is = process.getInputStream();
            } else {
                is = process.getErrorStream();
            }
            result = IoUtil.read(is, CharsetUtil.CHARSET_UTF_8);
            is.close();
            process.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
}
