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
import cn.hutool.core.thread.SyncFinisher;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author bwcx_jzy
 * @since 2021/7/29
 */
@Slf4j
public class TestJschExec {

	//private static final String cmd = "java -Dappliction=jpom-test-jar -jar /home/data/test/springboot-test-jar-0.0.1-SNAPSHOT.jar";

	//private static final String cmd = "a + source /etc/profile && source ~/.bash_profile && source ~/.bashrc && nohup java -Dappliction=jpom-test-jar -jar /home/data/test/springboot-test-jar-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &";

	//private static final String cmd = "ping baidu.com";

	private static final String cmd = "ps -ef | grep jpom-test-jar | awk '{print $2}' | xargs kill -9";

	@Test
	public void testShell() {
		Session session = JschUtil.createSession("192.168.1.8", 22, "root", "123456+");
		System.out.println(JschUtil.execByShell(session, "source /etc/profile && source ~/.bash_profile && source ~/.bashrc && nohup java -Dappliction=jpom-test-jar -jar /home/data/test/springboot-test-jar-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &", CharsetUtil.CHARSET_UTF_8));
	}

	@Test
	public void testShell2() throws Exception {

		String[] command = new String[]{cmd};
		Session session = null;
		Channel channel = null;
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			session = JschUtil.createSession("192.168.1.8", 22, "root", "123456+");
			channel = JschUtil.createChannel(session, ChannelType.SHELL);
			channel.connect();
			inputStream = channel.getInputStream();
			outputStream = channel.getOutputStream();
			SyncFinisher syncFinisher = new SyncFinisher(2);
			PrintWriter printWriter = new PrintWriter(outputStream);
			InputStream finalInputStream = inputStream;
			StringBuffer stringBuilder = new StringBuffer();
			syncFinisher.addRepeatWorker(() -> {
				for (String s : command) {
					try {
						printWriter.println(s);
						printWriter.println("exit");
						//把缓冲区的数据强行输出
						printWriter.flush();
						//System.out.println(s + "  " + command.length);
					} catch (Exception e) {
						e.printStackTrace();
						log.error("写入错误", e);
					}
				}
			}).addRepeatWorker(() -> {
				try {
					byte[] buffer = new byte[1024];
					int i;
					//如果没有数据来，线程会一直阻塞在这个地方等待数据。
					while ((i = finalInputStream.read(buffer)) != -1) {
						//System.out.println(i);
						stringBuilder.append(new String(Arrays.copyOfRange(buffer, 0, i), CharsetUtil.CHARSET_UTF_8));
					}
				} catch (Exception e) {
					log.error("读取错误", e);
				}
			}).start();
			System.out.println(stringBuilder);
		} finally {
			IoUtil.close(inputStream);
			IoUtil.close(outputStream);
			JschUtil.close(channel);
			JschUtil.close(session);
		}
	}

	@Test
	public void test() throws IOException, JSchException {
		Charset charset = CharsetUtil.CHARSET_UTF_8;
		Session session = JschUtil.createSession("192.168.1.8", 22, "root", "123456+");
		ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);

		// 添加环境变量
		channel.setCommand(cmd);
		InputStream inputStream = channel.getInputStream();
		InputStream errStream = channel.getErrStream();
		channel.connect((int) TimeUnit.SECONDS.toMillis(5));
		//
		SyncFinisher syncFinisher = new SyncFinisher(2);
		final String[] error = new String[1];
		final String[] result = new String[1];
		//
		syncFinisher.addRepeatWorker(() -> {
			try {
				error[0] = IoUtil.read(errStream, charset);
			} catch (Exception e) {
				e.printStackTrace();
				if (!StrUtil.contains(e.getMessage(), "Pipe closed")) {
					log.error("读取 exec err 流发生异常", e);
					error[0] = "读取 exec err 流发生异常" + e.getMessage();
				}
			} finally {
				syncFinisher.stop();
			}
		}).addRepeatWorker(() -> {
			try {
				result[0] = IoUtil.read(inputStream, charset);
			} catch (Exception e) {
				e.printStackTrace();
				if (!StrUtil.contains(e.getMessage(), "Pipe closed")) {
					log.error("读取 exec 流发生异常", e);
					result[0] = "读取 exec 流发生异常" + e.getMessage();
				}
			} finally {
				syncFinisher.stop();
			}
		}).start();
		System.out.println(result[0] + "  " + error[0]);
	}


	@Test
	public void test1() throws IOException, JSchException {
		Charset charset = CharsetUtil.CHARSET_UTF_8;
		Session session = JschUtil.createSession("192.168.1.8", 22, "root", "123456+");
		//session.connect();
		//ChannelExec channel = (ChannelExec) session.openChannel("exec");
		ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);

		// 添加环境变量
		channel.setCommand(cmd);
		InputStream inputStream = channel.getInputStream();
		InputStream errStream = channel.getErrStream();
//		channel.connect((int) TimeUnit.SECONDS.toMillis(5));
		channel.connect();
		//

		final String[] error = new String[1];
		final String[] result = new String[1];
		//

		try {
			System.out.println(error[0] = IoUtil.read(errStream, charset));
		} catch (Exception e) {
			e.printStackTrace();
			if (!StrUtil.contains(e.getMessage(), "Pipe closed")) {
				log.error("读取 exec err 流发生异常", e);
				error[0] = "读取 exec err 流发生异常" + e.getMessage();
			}
		}

		try {
			result[0] = IoUtil.read(inputStream, charset);
		} catch (Exception e) {
			e.printStackTrace();
			if (!StrUtil.contains(e.getMessage(), "Pipe closed")) {
				log.error("读取 exec 流发生异常", e);
				result[0] = "读取 exec 流发生异常" + e.getMessage();
			}
		}
		System.out.println("结束 " + result[0] + "  " + error[0]);
	}

	@Test
	public void test2() throws IOException, JSchException {
		Charset charset = CharsetUtil.CHARSET_UTF_8;
		Session session = JschUtil.createSession("192.168.1.8", 22, "root", "123456+");
		ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);

		// 添加环境变量
		channel.setCommand(cmd);
		channel.setErrStream(System.err);
		channel.setInputStream(System.in);
		InputStream inputStream = channel.getInputStream();
		InputStream errStream = channel.getErrStream();
		channel.connect((int) TimeUnit.SECONDS.toMillis(5));
		//

		String error = null;
		String result = null;
		//

		try {
			result = IoUtil.read(inputStream, charset);
		} catch (Exception e) {
			e.printStackTrace();
			if (!StrUtil.contains(e.getMessage(), "Pipe closed")) {
				log.error("读取 exec 流发生异常", e);
				result = "读取 exec 流发生异常" + e.getMessage();
			}
		}

		try {
			error = IoUtil.read(errStream, charset);
		} catch (Exception e) {
			e.printStackTrace();
			if (!StrUtil.contains(e.getMessage(), "Pipe closed")) {
				log.error("读取 exec err 流发生异常", e);
				error = "读取 exec err 流发生异常" + e.getMessage();
			}
		}
		System.out.println("结束 " + result + "  " + error);
	}
}
