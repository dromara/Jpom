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
import cn.hutool.core.thread.GlobalThreadPool;
import cn.hutool.core.util.CharsetUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class TopTest {
    public static void main(String[] args) {
//        String result = execCommand("/boot-line/command/java_cpu.sh 4257 1 java_cpu22340.txt");
//        System.out.println(result);

        GlobalThreadPool.execute(() -> {
            String[] cmd = {"cmd", "/c", "javaw  -classpath D:\\ssss\\a\\test\\aliyun-sdk-oss-2.8.3.jar;D:\\ssss\\a\\test\\antlr-2.7.7.jar;D:\\ssss\\a\\test\\antlr4-runtime-4.5.3.jar;D:\\ssss\\a\\test\\aspectjweaver-1.8.10.jar;D:\\ssss\\a\\test\\classmate-1.3.3.jar;D:\\ssss\\a\\test\\common-boot-1.2.27.jar;D:\\ssss\\a\\test\\commons-beanutils-1.9.3.jar;D:\\ssss\\a\\test\\commons-chain-1.1.jar;D:\\ssss\\a\\test\\commons-codec-1.10.jar;D:\\ssss\\a\\test\\commons-collections-3.2.2.jar;D:\\ssss\\a\\test\\commons-digester-2.1.jar;D:\\ssss\\a\\test\\commons-lang-2.4.jar;D:\\ssss\\a\\test\\commons-validator-1.3.1.jar;D:\\ssss\\a\\test\\dom4j-1.6.1.jar;D:\\ssss\\a\\test\\fastjson-1.2.49.jar;D:\\ssss\\a\\test\\hibernate-validator-5.2.5.Final.jar;D:\\ssss\\a\\test\\httpclient-4.5.3.jar;D:\\ssss\\a\\test\\httpcore-4.4.6.jar;D:\\ssss\\a\\test\\hutool-all-4.4.5.jar;D:\\ssss\\a\\test\\jackson-annotations-2.8.8.jar;D:\\ssss\\a\\test\\jackson-core-2.8.8.jar;D:\\ssss\\a\\test\\jackson-databind-2.8.8.jar;D:\\ssss\\a\\test\\jboss-logging-3.3.1.Final.jar;D:\\ssss\\a\\test\\jcl-over-slf4j-1.7.25.jar;D:\\ssss\\a\\test\\jdom-1.1.jar;D:\\ssss\\a\\test\\jpom-1.1.jar;D:\\ssss\\a\\test\\jul-to-slf4j-1.7.25.jar;D:\\ssss\\a\\test\\log4j-over-slf4j-1.7.25.jar;D:\\ssss\\a\\test\\logback-classic-1.1.11.jar;D:\\ssss\\a\\test\\logback-core-1.1.11.jar;D:\\ssss\\a\\test\\nginxparser-0.9.6.jar;D:\\ssss\\a\\test\\oro-2.0.8.jar;D:\\ssss\\a\\test\\slf4j-api-1.7.25.jar;D:\\ssss\\a\\test\\snakeyaml-1.17.jar;D:\\ssss\\a\\test\\spring-aop-4.3.9.RELEASE.jar;D:\\ssss\\a\\test\\spring-beans-4.3.9.RELEASE.jar;D:\\ssss\\a\\test\\spring-boot-1.4.7.RELEASE.jar;D:\\ssss\\a\\test\\spring-boot-autoconfigure-1.4.7.RELEASE.jar;D:\\ssss\\a\\test\\spring-boot-starter-1.4.7.RELEASE.jar;D:\\ssss\\a\\test\\spring-boot-starter-aop-1.4.7.RELEASE.jar;D:\\ssss\\a\\test\\spring-boot-starter-logging-1.4.7.RELEASE.jar;D:\\ssss\\a\\test\\spring-boot-starter-tomcat-1.4.7.RELEASE.jar;D:\\ssss\\a\\test\\spring-boot-starter-velocity-1.4.7.RELEASE.jar;D:\\ssss\\a\\test\\spring-boot-starter-web-1.4.7.RELEASE.jar;D:\\ssss\\a\\test\\spring-boot-starter-websocket-1.4.7.RELEASE.jar;D:\\ssss\\a\\test\\spring-context-4.3.9.RELEASE.jar;D:\\ssss\\a\\test\\spring-context-support-4.3.9.RELEASE.jar;D:\\ssss\\a\\test\\spring-core-4.3.9.RELEASE.jar;D:\\ssss\\a\\test\\spring-expression-4.3.9.RELEASE.jar;D:\\ssss\\a\\test\\spring-messaging-4.3.9.RELEASE.jar;D:\\ssss\\a\\test\\spring-web-4.3.9.RELEASE.jar;D:\\ssss\\a\\test\\spring-webmvc-4.3.9.RELEASE.jar;D:\\ssss\\a\\test\\spring-websocket-4.3.9.RELEASE.jar;D:\\ssss\\a\\test\\sslext-1.2-0.jar;D:\\ssss\\a\\test\\struts-core-1.3.8.jar;D:\\ssss\\a\\test\\struts-taglib-1.3.8.jar;D:\\ssss\\a\\test\\struts-tiles-1.3.8.jar;D:\\ssss\\a\\test\\tomcat-embed-core-8.5.15.jar;D:\\ssss\\a\\test\\tomcat-embed-el-8.5.15.jar;D:\\ssss\\a\\test\\tomcat-embed-websocket-8.5.15.jar;D:\\ssss\\a\\test\\validation-api-1.1.0.Final.jar;D:\\ssss\\a\\test\\velocity-1.7.jar;D:\\ssss\\a\\test\\velocity-tools-2.0.jar;D:\\ssss\\a\\test\\xml-apis-1.4.01.jar; -Dapplication=tset -Dbasedir=D:\\ssss\\a\\test cn.keepbx.jpom.JpomApplication --server.port=2123 >> D:\\ssss\\a\\tset.log"};
            String s = execCommand(cmd);

            System.out.println(s);
            System.out.println("结束");
        });


    }

    private static String execCommand(String[] command) {
        System.out.println(Arrays.toString(command));
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
            result = IoUtil.read(is, CharsetUtil.GBK);
            is.close();
            process.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
}
