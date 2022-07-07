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

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.ApplicationBuilder;
import cn.jiangzeyin.common.validator.ParameterInterceptor;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.jpom.common.Const;
import io.jpom.common.JpomApplicationEvent;
import io.jpom.common.JpomManifest;
import io.jpom.common.Type;
import io.jpom.plugin.PluginFactory;
import io.jpom.util.CommandUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * Jpom
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@Slf4j
public class JpomApplication extends ApplicationBuilder {

    /**
     *
     */
    public static final String SYSTEM_ID = "system";

    protected static String[] args;
    /**
     * 应用类型
     */
    private static Type appType;
    private static Charset charset;

    private static Class<?> appClass;

    /**
     * 获取程序命令行参数
     *
     * @return 数组
     */
    public static String[] getArgs() {
        return args;
    }

    public JpomApplication(Type appType, Class<?> appClass, String[] args) throws Exception {
        super(appClass);
        //
        checkEvent(args);
        JpomApplication.appType = appType;
        JpomApplication.appClass = appClass;
        JpomApplication.args = args;
        // 检查 type 中的 applicationClass 配置是否正确
        String applicationClass = appType.getApplicationClass();
        Assert.state(StrUtil.equals(applicationClass, appClass.getName()), "The currently allowed classes are inconsistent with the configured class names：io.jpom.common.Type#getApplicationClass()");

        addHttpMessageConverter(new StringHttpMessageConverter(CharsetUtil.CHARSET_UTF_8));

        //
        ObjectMapper build = createJackson();
        addHttpMessageConverter(new MappingJackson2HttpMessageConverter(build));

        // 参数拦截器
        addInterceptor(ParameterInterceptor.class);
//        addInterceptor(PluginFeatureInterceptor.class);
        //
        addApplicationEventClient(new JpomApplicationEvent());
        // 添加初始化监听
        PluginFactory pluginFactory = new PluginFactory();
        SpringApplication application = this.application();
        application.addListeners(pluginFactory);
        application.addInitializers(pluginFactory);
    }

    /**
     * jackson 配置
     *
     * @return mapper
     */
    private static ObjectMapper createJackson() {
        Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder = Jackson2ObjectMapperBuilder.json();
        jackson2ObjectMapperBuilder.simpleDateFormat(DatePattern.NORM_DATETIME_PATTERN);
        ObjectMapper build = jackson2ObjectMapperBuilder.build();
        // 忽略空
        build.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 驼峰转下划线
        //        build.setPropertyNamingStrategy(new PropertyNamingStrategy.SnakeCaseStrategy());
        // long to String
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        build.registerModule(simpleModule);
        //
        build.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        build.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);

        return build;
    }

    private void checkEvent(String[] args) throws Exception {
        new JpomClose().main(args);
    }

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
        if (appType == null) {
            // 从配置文件中获取
            Environment environment = JpomApplication.getEnvironment();
            String property = environment.getProperty(Const.APPLICATION_NAME);
            property = StrUtil.removeAll(property, "jpom");
            Assert.hasLength(property, "Please configure the program type：" + Const.APPLICATION_NAME);
            appType = Type.valueOf(property);
        }
        return appType;
    }

    public static Class<?> getAppClass() {
        if (appClass == null) {
            return JpomApplication.class;
        }
        return appClass;
    }

    /**
     * 重启自身
     * 分发会延迟2秒执行正式升级 重启命令
     */
    public static void restart() {
        File scriptFile = JpomManifest.getScriptFile();
        ThreadUtil.execute(() -> {
            // Waiting for method caller,For example, the interface response
            ThreadUtil.sleep(2, TimeUnit.SECONDS);
            try {
                String command = CommandUtil.generateCommand(scriptFile, "restart upgrade");
                //CommandUtil.EXECUTE_PREFIX + StrUtil.SPACE + FileUtil.getAbsolutePath(scriptFile) + " ";
                if (SystemUtil.getOsInfo().isWindows()) {
                    CommandUtil.execSystemCommand(command, scriptFile.getParentFile());
                } else {
                    CommandUtil.asyncExeLocalCommand(scriptFile.getParentFile(), command);
                }
            } catch (Exception e) {
                log.error("重启自身异常", e);
            }
        });
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
