package cn.keepbx.jpom;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.ApplicationBuilder;
import cn.jiangzeyin.common.validator.ParameterInterceptor;
import cn.keepbx.jpom.common.JpomApplicationEvent;
import cn.keepbx.jpom.common.Type;
import cn.keepbx.jpom.common.interceptor.PluginFeatureInterceptor;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.plugin.PluginFactory;
import cn.keepbx.util.CommandUtil;
import org.springframework.http.converter.StringHttpMessageConverter;

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

    private static Class appClass;

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

        // 参数拦截器
        addInterceptor(ParameterInterceptor.class);
        addInterceptor(PluginFeatureInterceptor.class);
        //
        addApplicationEventClient(new JpomApplicationEvent());
        // 添加初始化监听
        this.application().addInitializers(new PluginFactory());
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

    public static Class getAppClass() {
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
                CommandUtil.asyncExeLocalCommand(scriptFile.getParentFile(), FileUtil.getAbsolutePath(scriptFile) + " restart upgrade");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
