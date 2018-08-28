package cn.jiangzeyin.common;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.MultipartConfigElement;

/**
 * @author ChenRan
 * create by 2017-10-24 20:07
 **/
@Configuration
public class InitBean {
    /**
     * 上传文件限制
     *
     * @return config
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize("500MB");
        factory.setMaxRequestSize("1000MB");
        return factory.createMultipartConfig();
    }
}
