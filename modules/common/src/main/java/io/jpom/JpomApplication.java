package io.jpom;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.ApplicationBuilder;
import cn.jiangzeyin.common.validator.ParameterInterceptor;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.jpom.common.JpomApplicationEvent;
import io.jpom.common.JpomManifest;
import io.jpom.common.Type;
import io.jpom.common.interceptor.PluginFeatureInterceptor;
import io.jpom.plugin.PluginFactory;
import io.jpom.util.CommandUtil;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.File;
import java.nio.charset.Charset;

/**
 * Jpom
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
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

        addHttpMessageConverter(new StringHttpMessageConverter(CharsetUtil.CHARSET_UTF_8));

        //
        ObjectMapper build = createJackson();
        addHttpMessageConverter(new MappingJackson2HttpMessageConverter(build));

        // 参数拦截器
        addInterceptor(ParameterInterceptor.class);
        addInterceptor(PluginFeatureInterceptor.class);
        //
        addApplicationEventClient(new JpomApplicationEvent());
        // 添加初始化监听
        this.application().addInitializers(new PluginFactory());
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

    /**
     * 获取当前系统编码
     *
     * @return charset
     */
    public static Charset getCharset() {
        if (charset == null) {
            if (SystemUtil.getOsInfo().isLinux()) {
                charset = CharsetUtil.CHARSET_UTF_8;
            } else if (SystemUtil.getOsInfo().isMac()) {
                charset = CharsetUtil.CHARSET_UTF_8;
            } else {
                charset = CharsetUtil.CHARSET_GBK;
            }
        }
        return charset;
    }

    public static Type getAppType() {
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
     */
    public static void restart() {
        File scriptFile = JpomManifest.getScriptFile();
        ThreadUtil.execute(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
            try {
                String command = "";
                if (SystemUtil.getOsInfo().isLinux()) {
                    command = CommandUtil.SUFFIX;
                }
                command += " " + FileUtil.getAbsolutePath(scriptFile) + " restart upgrade";
                if (SystemUtil.getOsInfo().isWindows()) {
                    CommandUtil.execSystemCommand(command, scriptFile.getParentFile());
                } else {
                    CommandUtil.asyncExeLocalCommand(scriptFile.getParentFile(), command);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
