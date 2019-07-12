package cn.keepbx.jpom;

import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.ApplicationBuilder;
import cn.jiangzeyin.common.EnableCommonBoot;
import cn.keepbx.jpom.common.JpomApplicationEvent;
import cn.keepbx.jpom.common.Type;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.common.interceptor.PermissionInterceptor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.http.converter.StringHttpMessageConverter;

/**
 * jpom 启动类
 *
 * @author jiangzeyin
 * @date 2017/9/14
 */
@SpringBootApplication
@ServletComponentScan
@EnableCommonBoot
public class JpomServerApplication extends BaseJpomApplication {
    public JpomServerApplication() throws Exception {
        super(Type.Server, JpomServerApplication.class);
    }

    /**
     * 启动执行
     *
     * @param args 参数
     */
    public static void main(String[] args) throws Exception {
        JpomServerApplication.args = args;
        ApplicationBuilder.createBuilder(JpomServerApplication.class)
                .addHttpMessageConverter(new StringHttpMessageConverter(CharsetUtil.CHARSET_UTF_8))
                // 拦截器
                .addInterceptor(LoginInterceptor.class)
                .addInterceptor(PermissionInterceptor.class)
                //
                .addApplicationEventClient(new JpomApplicationEvent())
                .run(args);
    }


}
