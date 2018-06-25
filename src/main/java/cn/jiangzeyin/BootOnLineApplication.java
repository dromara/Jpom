package cn.jiangzeyin;

import cn.jiangzeyin.common.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.nio.charset.Charset;

/**
 * Created by jiangzeyin on 2017/9/14.
 */
@SpringBootApplication
@ServletComponentScan
@ComponentScan({"cn.jiangzeyin"})
public class BootOnLineApplication {

    /**
     * 启动执行
     *
     * @param args 参数
     */
    public static void main(String[] args) throws Exception {
        SpringApplicationBuilder.createBuilder(BootOnLineApplication.class)
                .addHttpMessageConverter(new StringHttpMessageConverter(Charset.forName("UTF-8")))
                .run(args);
    }
}
