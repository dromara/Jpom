package cn.keepbx.jpom;

import cn.jiangzeyin.common.ApplicationBuilder;
import cn.jiangzeyin.common.EnableCommonBoot;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.nio.charset.Charset;

/**
 * jpom 启动类
 * Created by jiangzeyin on 2017/9/14.
 *
 * @author jiangzeyin
 */
@SpringBootApplication
@ServletComponentScan
@EnableCommonBoot
public class JpomApplication {

    /**
     * 启动执行
     *
     * @param args 参数
     */
    public static void main(String[] args) throws Exception {
        ApplicationBuilder.createBuilder(JpomApplication.class)
                .addHttpMessageConverter(new StringHttpMessageConverter(Charset.forName("UTF-8")))
                .addInterceptor(LoginInterceptor.class)
                .run(args);
    }
}
