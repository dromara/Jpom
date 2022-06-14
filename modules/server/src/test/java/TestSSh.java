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
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.Session;

import java.io.*;

/**
 * @author bwcx_jzy
 * @since 2019/8/8
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
