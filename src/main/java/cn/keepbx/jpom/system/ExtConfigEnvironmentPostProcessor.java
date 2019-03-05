package cn.keepbx.jpom.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;

/**
 * 动态读取外部配置文件
 *
 * @author jiangzeyin
 * @date 2019/3/5
 */
public class ExtConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File directory = ExtConfigBean.getFile();
        if (!directory.exists()) {
            return;
        }
        YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
        FileSystemResource fileSystemResource = new FileSystemResource(directory);
        try {
            PropertySource propertySource = yamlPropertySourceLoader.load(directory.getName(), fileSystemResource, null);
            environment.getPropertySources().addLast(propertySource);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
