import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.*;

import java.io.*;
import java.nio.charset.Charset;

/**
 * @author bwcx_jzy
 * @date 2019/8/8
 */
public class TestSSh {
    private static String charset = "UTF-8"; // 设置编码格式

    public static void main(String[] args) throws Exception {
        Session session = JschUtil.getSession("39.1.109", 23, "root", "kee");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String command;
        BufferedReader reader = null;
        ChannelShell channel = (ChannelShell) JschUtil.createChannel(session, ChannelType.SHELL);
        channel.setPty(true);
        channel.connect();
        InputStream in = channel.getInputStream();
        ThreadUtil.execute(() -> {
            IoUtil.readLines(in, CharsetUtil.CHARSET_UTF_8, (LineHandler) System.out::println);
        });
        ThreadUtil.execute(() -> {
            try {
                IoUtil.readLines(channel.getExtInputStream(), CharsetUtil.CHARSET_UTF_8, (LineHandler) System.out::println);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        OutputStream outputStream = channel.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream);
        try {
            while ((command = br.readLine()) != null) {
                if ("*".equals(command)) {
                    command = "0x08";
                    printWriter.write("0x08".toCharArray());
                    printWriter.flush();
                    continue;
                }
                printWriter.println(command);
                printWriter.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            channel.disconnect();
        }
    }

    /**
     * 执行一条命令
     */
    public static void execCmd(String command, Session session) throws Exception {
        Channel channel = JschUtil.createChannel(session, ChannelType.EXEC);
        ((ChannelExec) channel).setCommand(command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);


        System.out.println("执行");
//        IoUtil.readUtf8Lines(channel.getExtInputStream(), new LineHandler() {
//            @Override
//            public void handle(String line) {
//                System.out.println(line);
//            }
//        });
        InputStream inputStream = channel.getInputStream();
        channel.connect();
        IoUtil.readLines(inputStream, CharsetUtil.CHARSET_UTF_8, new LineHandler() {
            @Override
            public void handle(String line) {
                System.out.println(line);
            }
        });
        int exitStatus = channel.getExitStatus();
        System.out.println(exitStatus);
        channel.disconnect();
        session.disconnect();
    }
}
