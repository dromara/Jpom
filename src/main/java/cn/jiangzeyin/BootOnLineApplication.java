package cn.jiangzeyin;

import cn.jiangzeyin.common.BaseApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by jiangzeyin on 2017/9/14.
 */
@SpringBootApplication
@ServletComponentScan
@ComponentScan({"cn.jiangzeyin"})
public class BootOnLineApplication extends WebMvcConfigurerAdapter {

    /**
     * 启动执行
     *
     * @param args 参数
     */
    public static void main(String[] args) {
        BaseApplication application = new BaseApplication(BootOnLineApplication.class);
        application.run(args);
    }

    @Bean
    public HttpMessageConverter<String> responseBodyConverter() {
        StringHttpMessageConverter converter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        return converter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.configureMessageConverters(converters);
        converters.add(responseBodyConverter());
    }
}
