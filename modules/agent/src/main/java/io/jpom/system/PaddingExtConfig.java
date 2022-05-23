package io.jpom.system;

import cn.hutool.core.lang.Console;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * 兼容一些全局默认配置属性
 *
 * @author bwcx_jzy
 * @see MultipartProperties
 * @since 2022/5/23
 */
@Order()
public class PaddingExtConfig implements EnvironmentPostProcessor {


    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
        Resource resource = new ClassPathResource("/bin/extConfigDefault.yml");
        try {
            List<PropertySource<?>> propertySources = yamlPropertySourceLoader.load("extConfigDefault.yml", resource);
            propertySources.forEach(propertySource -> environment.getPropertySources().addLast(propertySource));
        } catch (Exception e) {
            Console.error(e.getMessage());
        }
    }
}
