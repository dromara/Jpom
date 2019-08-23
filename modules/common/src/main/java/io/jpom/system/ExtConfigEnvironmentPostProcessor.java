package io.jpom.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

/**
 * 动态读取外部配置文件
 *
 * @author jiangzeyin
 * @date 2019/3/5
 */
public class ExtConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
        Resource resource = ExtConfigBean.getResource();
        try {
            List<PropertySource<?>> propertySources = yamlPropertySourceLoader.load(ExtConfigBean.FILE_NAME, resource);
            propertySources.forEach(propertySource -> environment.getPropertySources().addLast(propertySource));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
