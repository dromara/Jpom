/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import org.dromara.jpom.system.ExtConfigBean;

import java.io.*;

public class TestRun {
	public static void main(String[] args) throws IOException, InterruptedException {
		testProcessBuilder("D:\\jpom\\agent\\script\\test\\script.bat");

	}

	public static void testProcessBuilder(String... command) {
		boolean err = false;
		try {
			final int[] count = {0};
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			//初始化ProcessBuilder对象
//            processBuilder.inheritIO()
			Process p = processBuilder.start();
			OutputStream outputStream = p.getOutputStream();
			InputStream inputStream = p.getInputStream();
			ThreadUtil.execute(() -> {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, CharsetUtil.CHARSET_GBK);
                //用于存储执行命令的结果
                BufferedReader results = new BufferedReader(inputStreamReader);
                IoUtil.readLines(results, (LineHandler) line -> {
//                    String result = CharsetUtil.convert(line, CharsetUtil.CHARSET_ISO_8859_1, CharsetUtil.CHARSET_GBK);
                    System.out.println(line);
                    count[0]++;
                    if (count[0] == 10) {
                        processBuilder.inheritIO();
                        try {
                            outputStream.write(2);
                            outputStream.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            results.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            inputStreamReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("3*****");
                        try {
                            p.waitFor();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            p.getErrorStream().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println(123456);
                        p.destroy();
                    }
                });
                System.out.println("123");
                //用于存储执行命令的错误信息
                BufferedReader errors = new BufferedReader(new InputStreamReader(p.getErrorStream(), ExtConfigBean.getConsoleLogCharset()));
                IoUtil.readLines(errors, (LineHandler) line -> {

                    String result = CharsetUtil.convert(line, null, ExtConfigBean.getConsoleLogCharset());
                    System.out.println("error:" + result);
                });
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

			System.out.println("end");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
