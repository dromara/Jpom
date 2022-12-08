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
package io.jpom;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.lang.Console;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.system.SystemUtil;
import io.jpom.common.JpomAppType;
import io.jpom.common.JpomManifest;
import io.jpom.common.Type;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.CommandUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Jpom
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@Slf4j
public class JpomApplication {

    /**
     *
     */
    public static final String SYSTEM_ID = "system";


//    private void checkEvent(String[] args) throws Exception {
//        new JpomClose().main(args);
//    }

//	/**
//	 * 获取当前系统编码
//	 *
//	 * @return charset
//	 */
//	public static Charset getCharset() {
//		if (charset == null) {
//			if (SystemUtil.getOsInfo().isLinux()) {
//				charset = CharsetUtil.CHARSET_UTF_8;
//			} else if (SystemUtil.getOsInfo().isMac()) {
//				charset = CharsetUtil.CHARSET_UTF_8;
//			} else {
//				charset = CharsetUtil.CHARSET_GBK;
//			}
//		}
//		return charset;
//	}

    /**
     * 获取当前程序的类型
     *
     * @return Agent 或者 Server
     */
    public static Type getAppType() {
        Map<String, Object> beansWithAnnotation = SpringUtil.getApplicationContext().getBeansWithAnnotation(JpomAppType.class);
        Class<?> jpomAppClass = Optional.of(beansWithAnnotation)
            .map(map -> CollUtil.getFirst(map.values()))
            .map(Object::getClass)
            .orElseThrow(() -> new RuntimeException("没有找到 Jpom 类型配置"));
        JpomAppType jpomAppType = jpomAppClass.getAnnotation(JpomAppType.class);
        return jpomAppType.value();
    }

    public static Class<?> getAppClass() {
        Map<String, Object> beansWithAnnotation = SpringUtil.getApplicationContext().getBeansWithAnnotation(SpringBootApplication.class);
        return Optional.of(beansWithAnnotation)
            .map(map -> CollUtil.getFirst(map.values()))
            .map(Object::getClass)
            .orElseThrow(() -> new RuntimeException("没有找到运行的主类"));
    }

    /**
     * 重启自身
     * 分发会延迟2秒执行正式升级 重启命令
     */
    public static void restart() {
        File scriptFile = JpomManifest.getScriptFile();
        ThreadUtil.execute(() -> {
            //
            prepareRestart(scriptFile);
            // Waiting for method caller,For example, the interface response
            ThreadUtil.sleep(2, TimeUnit.SECONDS);
            try {
                String command = CommandUtil.generateCommand(scriptFile, "restart upgrade");
                //CommandUtil.EXECUTE_PREFIX + StrUtil.SPACE + FileUtil.getAbsolutePath(scriptFile) + " ";
                if (SystemUtil.getOsInfo().isWindows()) {
                    //String result = CommandUtil.execSystemCommand(command, scriptFile.getParentFile());
                    //log.debug("windows restart {}", result);
                    CommandUtil.asyncExeLocalCommand(scriptFile.getParentFile(), "start /b" + command);
                } else {
                    CommandUtil.asyncExeLocalCommand(scriptFile.getParentFile(), command);
                }
            } catch (Exception e) {
                log.error("重启自身异常", e);
            }
        });
    }

    private static void prepareRestart(File scriptFile) {
        Charset charset = ExtConfigBean.getInstance().getConsoleLogCharset();
        String typeName = JpomApplication.getAppType().name().toLowerCase();
        final String[] oldName = new String[]{typeName + ".log"};
        List<String> newData = new LinkedList<>();
        FileUtil.readLines(scriptFile, charset, (LineHandler) line -> {
            if (!line.startsWith(String.valueOf(StrUtil.C_TAB)) &&
                !line.startsWith(String.valueOf(StrUtil.C_SPACE))) {
                if (SystemUtil.getOsInfo().isWindows()) {
                    // windows 控制台文件相关
                    if (StrUtil.containsAny(line, "set LogName=")) {
                        //
                        oldName[0] = CharSequenceUtil.splitToArray(line, "=")[0];
                        newData.add(StrUtil.format("set LogName={}_{}.log", typeName, System.currentTimeMillis()));
                    } else {
                        newData.add(line);
                    }
                } else {
                    newData.add(line);
                }
            } else {
                newData.add(line);
            }
        });
        FileUtil.writeLines(newData, scriptFile, charset);
    }

    /**
     * 控制台输出并结束程序
     *
     * @param status   终止码
     * @param template 输出消息
     * @param args     参数
     */
    public static void consoleExit(int status, String template, Object... args) {
        if (status == 0) {
            Console.log(template, args);
        } else {
            Console.error(template, args);
        }
        Console.log("has stopped running automatically，Need to log out manually: Ctrl+C/Control+C ");
        System.exit(status);
    }
}
